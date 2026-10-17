-- Resultado final y trazable de visitas y entrevistas sociales.
-- Ejecutar despues de 2026-10-14-application-visit-management.sql. Es idempotente.
BEGIN;

ALTER TABLE public.application_visit
    ADD COLUMN IF NOT EXISTS result_observations varchar(2000),
    ADD COLUMN IF NOT EXISTS result_conclusions varchar(2000),
    ADD COLUMN IF NOT EXISTS result_recorded_at timestamp(6) with time zone,
    ADD COLUMN IF NOT EXISTS result_recorded_by_user_id bigint;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.application_visit'::regclass
          AND conname = 'fk_application_visit_result_recorded_by'
    ) THEN
        ALTER TABLE public.application_visit
            ADD CONSTRAINT fk_application_visit_result_recorded_by
            FOREIGN KEY (result_recorded_by_user_id) REFERENCES public.users(id);
    END IF;
END
$$;

-- Eliminar checks previos del estado creados por Hibernate o por migraciones anteriores.
DO $$
DECLARE
    old_check record;
BEGIN
    FOR old_check IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'public.application_visit'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%status%'
          AND pg_get_constraintdef(oid) ILIKE '%SCHEDULED%'
          AND pg_get_constraintdef(oid) NOT ILIKE '%REALIZED%'
    LOOP
        EXECUTE format('ALTER TABLE public.application_visit DROP CONSTRAINT %I', old_check.conname);
    END LOOP;
END
$$;

ALTER TABLE public.application_visit DROP CONSTRAINT IF EXISTS ck_application_visit_status;
ALTER TABLE public.application_visit ADD CONSTRAINT ck_application_visit_status
    CHECK (status IN ('SCHEDULED', 'CANCELLED', 'REALIZED', 'NOT_REALIZED'));

ALTER TABLE public.application_visit DROP CONSTRAINT IF EXISTS ck_application_visit_cancellation;
ALTER TABLE public.application_visit DROP CONSTRAINT IF EXISTS ck_application_visit_lifecycle_data;
ALTER TABLE public.application_visit ADD CONSTRAINT ck_application_visit_lifecycle_data CHECK (
    (status = 'SCHEDULED'
        AND cancellation_reason IS NULL
        AND cancelled_at IS NULL
        AND cancelled_by_user_id IS NULL
        AND result_observations IS NULL
        AND result_conclusions IS NULL
        AND result_recorded_at IS NULL
        AND result_recorded_by_user_id IS NULL)
    OR
    (status = 'CANCELLED'
        AND cancellation_reason IS NOT NULL
        AND length(btrim(cancellation_reason)) BETWEEN 1 AND 1000
        AND cancelled_at IS NOT NULL
        AND cancelled_by_user_id IS NOT NULL
        AND result_observations IS NULL
        AND result_conclusions IS NULL
        AND result_recorded_at IS NULL
        AND result_recorded_by_user_id IS NULL)
    OR
    (status IN ('REALIZED', 'NOT_REALIZED')
        AND cancellation_reason IS NULL
        AND cancelled_at IS NULL
        AND cancelled_by_user_id IS NULL
        AND result_recorded_at IS NOT NULL
        AND result_recorded_by_user_id = responsible_user_id
        AND result_recorded_at >= ends_at
        AND (result_observations IS NULL OR length(btrim(result_observations)) BETWEEN 1 AND 2000)
        AND (result_conclusions IS NULL OR length(btrim(result_conclusions)) BETWEEN 1 AND 2000))
);

CREATE INDEX IF NOT EXISTS ix_application_visit_result_recorded_by
    ON public.application_visit (result_recorded_by_user_id);

INSERT INTO public.permissions (name)
VALUES ('applications:visits:results:create')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name = 'applications:visits:results:create'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
