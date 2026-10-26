-- Resolucion de solicitudes (POST /api/admin/applications/{id}/resolution):
-- aprobar, rechazar o dejar en lista de espera, registrando quien resolvio.
-- Ejecutar antes del despliegue en bases existentes. Es idempotente y conserva las solicitudes actuales.
BEGIN;

ALTER TABLE public.application
    ADD COLUMN IF NOT EXISTS resolved_by_user_id bigint;

-- Hibernate (ddl-auto: update) puede haber creado ya una FK con nombre generado: no se duplica.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.application'::regclass
          AND contype = 'f'
          AND pg_get_constraintdef(oid) LIKE 'FOREIGN KEY (resolved_by_user_id)%'
    ) THEN
        ALTER TABLE public.application
            ADD CONSTRAINT fk_application_resolved_by
            FOREIGN KEY (resolved_by_user_id) REFERENCES public.users(id);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS ix_application_resolved_by
    ON public.application (resolved_by_user_id);

INSERT INTO public.permissions (name)
VALUES ('applications:resolutions:create')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name = 'applications:resolutions:create'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
