-- Otorgamientos individuales. Idempotente, sin generar datos para aprobaciones anteriores.
-- Ejecutar antes del despliegue; no modifica solicitudes, resoluciones ni cupos.
BEGIN;
CREATE TABLE IF NOT EXISTS public.benefit_grant (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES public.application(id),
    program_benefit_id uuid NOT NULL REFERENCES public.program_benefit(id),
    benefit_type varchar(50) NOT NULL,
    description varchar(500),
    amount numeric(19,2),
    start_date date NOT NULL,
    end_date date NOT NULL,
    status varchar(20) NOT NULL,
    assigned_by_user_id bigint NOT NULL REFERENCES public.users(id),
    assigned_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_benefit_grant_application_benefit UNIQUE (application_id, program_benefit_id),
    CONSTRAINT ck_benefit_grant_dates CHECK (start_date <= end_date),
    CONSTRAINT ck_benefit_grant_status CHECK (status = 'ACTIVE'),
    CONSTRAINT ck_benefit_grant_amount CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT ck_benefit_grant_type CHECK
        (benefit_type IN ('TAX_EXEMPTION', 'HOUSING_SUBSIDY', 'FOOD_ASSISTANCE', 'UTILITY_SUBSIDY'))
);
CREATE INDEX IF NOT EXISTS ix_benefit_grant_application_date ON public.benefit_grant (application_id, assigned_at, id);
CREATE INDEX IF NOT EXISTS ix_benefit_grant_benefit ON public.benefit_grant (program_benefit_id);
CREATE INDEX IF NOT EXISTS ix_benefit_grant_assigned_by ON public.benefit_grant (assigned_by_user_id);

INSERT INTO public.permissions (name) VALUES ('applications:management:benefits:assign') ON CONFLICT (name) DO NOTHING;
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r
JOIN public.permissions p ON p.name = 'applications:management:benefits:assign'
WHERE r.name = 'ADMIN' ON CONFLICT DO NOTHING;
COMMIT;
