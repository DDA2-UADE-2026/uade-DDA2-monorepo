-- Resoluciones administrativas e historial inmutable. Ejecutar antes del despliegue.
-- Idempotente; no crea resoluciones ni modifica cupos o datos historicos existentes.
BEGIN;

ALTER TABLE public.application ADD COLUMN IF NOT EXISTS resolved_by_user_id bigint;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_application_resolved_by'
                   AND conrelid = 'public.application'::regclass) THEN
        ALTER TABLE public.application ADD CONSTRAINT fk_application_resolved_by
            FOREIGN KEY (resolved_by_user_id) REFERENCES public.users(id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.application_resolution (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES public.application(id),
    previous_status varchar(30) NOT NULL,
    decision varchar(30) NOT NULL,
    reason varchar(1000) NOT NULL,
    resolved_by_user_id bigint NOT NULL REFERENCES public.users(id),
    resolved_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_application_resolution_stage UNIQUE (application_id, previous_status),
    CONSTRAINT ck_application_resolution_transition CHECK (
        (previous_status = 'IN_EVALUATION' AND decision IN ('APPROVED', 'REJECTED', 'WAITLISTED')) OR
        (previous_status = 'WAITLISTED' AND decision IN ('APPROVED', 'REJECTED'))),
    CONSTRAINT ck_application_resolution_reason CHECK (length(trim(reason)) > 0)
);
CREATE INDEX IF NOT EXISTS ix_application_resolution_application
    ON public.application_resolution (application_id, resolved_at);
CREATE INDEX IF NOT EXISTS ix_application_resolution_responsible
    ON public.application_resolution (resolved_by_user_id);

CREATE TABLE IF NOT EXISTS public.application_resolution_requirement (
    id uuid PRIMARY KEY,
    resolution_id uuid NOT NULL REFERENCES public.application_resolution(id),
    -- Identificador historico sin FK al catalogo editable.
    requirement_id uuid NOT NULL,
    type varchar(50) NOT NULL,
    requirement_value varchar(255) NOT NULL,
    description varchar(500),
    fulfilled boolean NOT NULL,
    CONSTRAINT uk_application_resolution_requirement UNIQUE (resolution_id, requirement_id),
    CONSTRAINT ck_application_resolution_requirement_type CHECK
        (type IN ('MIN_AGE', 'MAX_INCOME', 'RESIDENCY_YEARS', 'HAS_CHILDREN'))
);

INSERT INTO public.permissions (name) VALUES ('applications:management:resolve') ON CONFLICT (name) DO NOTHING;
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r
JOIN public.permissions p ON p.name = 'applications:management:resolve'
WHERE r.name = 'ADMIN' ON CONFLICT DO NOTHING;

COMMIT;
