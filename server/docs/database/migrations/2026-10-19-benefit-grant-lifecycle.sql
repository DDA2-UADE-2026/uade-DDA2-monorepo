-- Ciclo de vida de otorgamientos. Ejecutar despues de 2026-10-19-benefit-grants.sql, antes del despliegue.
-- Idempotente: conserva datos y reconstruye solamente la asignacion inicial conocida.
BEGIN;
LOCK TABLE public.benefit_grant IN ACCESS EXCLUSIVE MODE;

-- Sustituye TODOS los CHECK que restringen exclusivamente status (incluido el generado por Hibernate).
-- No elimina restricciones de fechas, monto, tipo ni claves foraneas.
DO $migration$
DECLARE status_constraint record;
BEGIN
    FOR status_constraint IN
        SELECT c.conname FROM pg_constraint c
        WHERE c.conrelid = 'public.benefit_grant'::regclass AND c.contype = 'c'
          AND c.conkey = ARRAY[(SELECT a.attnum FROM pg_attribute a
              WHERE a.attrelid = 'public.benefit_grant'::regclass AND a.attname = 'status')]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE public.benefit_grant DROP CONSTRAINT %I', status_constraint.conname);
    END LOOP;
END $migration$;
ALTER TABLE public.benefit_grant ADD CONSTRAINT ck_benefit_grant_status
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'FINALIZED'));

CREATE TABLE IF NOT EXISTS public.benefit_grant_change (
    id uuid PRIMARY KEY,
    grant_id uuid NOT NULL REFERENCES public.benefit_grant(id),
    sequence_number integer NOT NULL,
    action varchar(20) NOT NULL,
    previous_status varchar(20),
    status varchar(20) NOT NULL,
    previous_start_date date,
    previous_end_date date,
    start_date date NOT NULL,
    end_date date NOT NULL,
    reason varchar(1000),
    responsible_user_id bigint NOT NULL REFERENCES public.users(id),
    changed_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_benefit_grant_change_sequence UNIQUE (grant_id, sequence_number),
    CONSTRAINT ck_benefit_grant_change_reason CHECK
        ((action = 'ASSIGNED' AND reason IS NULL) OR
         (action <> 'ASSIGNED' AND reason IS NOT NULL AND length(trim(reason)) > 0)),
    CONSTRAINT ck_benefit_grant_change_dates CHECK (start_date <= end_date),
    CONSTRAINT ck_benefit_grant_change_transition CHECK (
        (action = 'ASSIGNED' AND sequence_number = 1 AND previous_status IS NULL AND status = 'ACTIVE'
            AND previous_start_date IS NULL AND previous_end_date IS NULL) OR
        (sequence_number > 1 AND previous_status IS NOT NULL AND previous_start_date IS NOT NULL
            AND previous_end_date IS NOT NULL AND start_date = previous_start_date AND (
            (action = 'RENEWED' AND previous_status IN ('ACTIVE', 'SUSPENDED') AND status = 'ACTIVE' AND end_date > previous_end_date) OR
            (action = 'SUSPENDED' AND previous_status = 'ACTIVE' AND status = 'SUSPENDED' AND end_date = previous_end_date) OR
            (action = 'FINALIZED' AND previous_status IN ('ACTIVE', 'SUSPENDED') AND status = 'FINALIZED' AND end_date = previous_end_date)
        ))
    )
);
CREATE INDEX IF NOT EXISTS ix_benefit_grant_change_responsible ON public.benefit_grant_change (responsible_user_id);

CREATE TABLE IF NOT EXISTS public.benefit_grant_change_requirement (
    id uuid PRIMARY KEY,
    change_id uuid NOT NULL REFERENCES public.benefit_grant_change(id),
    requirement_id uuid NOT NULL, -- identificador historico, sin FK al catalogo mutable
    type varchar(50) NOT NULL,
    requirement_value varchar(255) NOT NULL,
    description varchar(500),
    fulfilled boolean NOT NULL,
    CONSTRAINT uk_benefit_grant_change_requirement UNIQUE (change_id, requirement_id),
    CONSTRAINT ck_benefit_grant_change_requirement_type CHECK
        (type IN ('MIN_AGE', 'MAX_INCOME', 'RESIDENCY_YEARS', 'HAS_CHILDREN'))
);

-- UUID determinista en otra tabla: no requiere extensiones ni genera cambios historicos inexistentes.
INSERT INTO public.benefit_grant_change
    (id, grant_id, sequence_number, action, previous_status, status, previous_start_date, previous_end_date,
     start_date, end_date, reason, responsible_user_id, changed_at)
SELECT g.id, g.id, 1, 'ASSIGNED', NULL, 'ACTIVE', NULL, NULL,
    g.start_date, g.end_date, NULL, g.assigned_by_user_id, g.assigned_at
FROM public.benefit_grant g
WHERE NOT EXISTS (SELECT 1 FROM public.benefit_grant_change c WHERE c.grant_id = g.id);

INSERT INTO public.permissions (name) VALUES ('applications:management:benefits:manage') ON CONFLICT (name) DO NOTHING;
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r
JOIN public.permissions p ON p.name = 'applications:management:benefits:manage'
WHERE r.name = 'ADMIN' ON CONFLICT DO NOTHING;
COMMIT;
