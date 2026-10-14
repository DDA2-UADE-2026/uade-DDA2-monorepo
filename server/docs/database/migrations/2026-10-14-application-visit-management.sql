-- Reprogramacion y cancelacion trazable de visitas y entrevistas sociales.
-- Ejecutar antes del despliegue en bases existentes. Es idempotente y conserva las actuaciones actuales.
BEGIN;

ALTER TABLE public.application_visit
    ADD COLUMN IF NOT EXISTS cancellation_reason varchar(1000),
    ADD COLUMN IF NOT EXISTS cancelled_at timestamp(6) with time zone,
    ADD COLUMN IF NOT EXISTS cancelled_by_user_id bigint;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.application_visit'::regclass
          AND conname = 'fk_application_visit_cancelled_by'
    ) THEN
        ALTER TABLE public.application_visit
            ADD CONSTRAINT fk_application_visit_cancelled_by
            FOREIGN KEY (cancelled_by_user_id) REFERENCES public.users(id);
    END IF;
END
$$;

-- Hibernate puede haber creado un check anterior que solo admita SCHEDULED.
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
          AND pg_get_constraintdef(oid) NOT ILIKE '%CANCELLED%'
    LOOP
        EXECUTE format('ALTER TABLE public.application_visit DROP CONSTRAINT %I', old_check.conname);
    END LOOP;
END
$$;

ALTER TABLE public.application_visit DROP CONSTRAINT IF EXISTS ck_application_visit_status;
ALTER TABLE public.application_visit ADD CONSTRAINT ck_application_visit_status
    CHECK (status IN ('SCHEDULED', 'CANCELLED'));

ALTER TABLE public.application_visit DROP CONSTRAINT IF EXISTS ck_application_visit_cancellation;
ALTER TABLE public.application_visit ADD CONSTRAINT ck_application_visit_cancellation CHECK (
    (status = 'SCHEDULED'
        AND cancellation_reason IS NULL
        AND cancelled_at IS NULL
        AND cancelled_by_user_id IS NULL)
    OR
    (status = 'CANCELLED'
        AND cancellation_reason IS NOT NULL
        AND length(btrim(cancellation_reason)) BETWEEN 1 AND 1000
        AND cancelled_at IS NOT NULL
        AND cancelled_by_user_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS ix_application_visit_application_status
    ON public.application_visit (application_id, status);

INSERT INTO public.permissions (name)
VALUES ('applications:visits:manage')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name = 'applications:visits:manage'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
