-- Consulta ciudadana de visitas propias e indicador persistente de reprogramacion.
-- Ejecutar despues de las migraciones de visitas del 2026-10-14. Es idempotente.
BEGIN;

ALTER TABLE public.application_visit
    ADD COLUMN IF NOT EXISTS last_rescheduled_at timestamp(6) with time zone;

-- Recupera la ultima reprogramacion conocida sin convertir la auditoria en fuente de lectura de la API.
UPDATE public.application_visit visit
SET last_rescheduled_at = history.last_rescheduled_at
FROM (
    SELECT entity_id, max(created_at) AS last_rescheduled_at
    FROM public.logs
    WHERE entity_type = 'application_visit'
      AND new_values ->> 'operation' = 'RESCHEDULE'
    GROUP BY entity_id
) history
WHERE history.entity_id = visit.id::text
  AND visit.last_rescheduled_at IS NULL
  AND history.last_rescheduled_at >= visit.scheduled_at
  AND history.last_rescheduled_at < visit.starts_at;

ALTER TABLE public.application_visit
    DROP CONSTRAINT IF EXISTS ck_application_visit_reschedule_data;
ALTER TABLE public.application_visit
    ADD CONSTRAINT ck_application_visit_reschedule_data CHECK (
        last_rescheduled_at IS NULL
        OR (last_rescheduled_at >= scheduled_at AND last_rescheduled_at < starts_at)
    );

INSERT INTO public.permissions (name)
VALUES ('applications:own:visits:view')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM public.roles role
JOIN public.permissions permission ON permission.name = 'applications:own:visits:view'
WHERE role.name IN ('ADMIN', 'CIUDADANO')
ON CONFLICT DO NOTHING;

COMMIT;
