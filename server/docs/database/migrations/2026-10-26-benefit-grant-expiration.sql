-- Ejecutar despues de 2026-10-19-benefit-grant-lifecycle.sql y antes del despliegue.
-- Idempotente. Solo cambia estructura: los vencimientos reales se registran por el servicio al arrancar.
BEGIN;
LOCK TABLE public.benefit_grant, public.benefit_grant_change IN ACCESS EXCLUSIVE MODE;

DO $migration$
DECLARE existing_check record;
BEGIN
    FOR existing_check IN
        SELECT c.conname FROM pg_constraint c
        WHERE c.conrelid = 'public.benefit_grant'::regclass AND c.contype = 'c'
          AND EXISTS (SELECT 1 FROM pg_attribute a WHERE a.attrelid = c.conrelid
              AND a.attname = 'status' AND c.conkey = ARRAY[a.attnum]::smallint[])
    LOOP
        EXECUTE format('ALTER TABLE public.benefit_grant DROP CONSTRAINT %I', existing_check.conname);
    END LOOP;
    FOR existing_check IN
        SELECT c.conname FROM pg_constraint c
        WHERE c.conrelid = 'public.benefit_grant_change'::regclass AND c.contype = 'c'
          AND (c.conname IN ('ck_benefit_grant_change_transition', 'ck_benefit_grant_change_responsible')
            OR EXISTS (SELECT 1 FROM pg_attribute a WHERE a.attrelid = c.conrelid
                AND a.attname IN ('action', 'status', 'previous_status')
                AND c.conkey = ARRAY[a.attnum]::smallint[]))
    LOOP
        EXECUTE format('ALTER TABLE public.benefit_grant_change DROP CONSTRAINT %I', existing_check.conname);
    END LOOP;
END $migration$;

ALTER TABLE public.benefit_grant ADD CONSTRAINT ck_benefit_grant_status
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'FINALIZED', 'EXPIRED'));
CREATE INDEX IF NOT EXISTS ix_benefit_grant_expiration ON public.benefit_grant (status, end_date, application_id);

ALTER TABLE public.benefit_grant_change ALTER COLUMN responsible_user_id DROP NOT NULL;
ALTER TABLE public.benefit_grant_change ADD CONSTRAINT ck_benefit_grant_change_status
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'FINALIZED', 'EXPIRED'));
ALTER TABLE public.benefit_grant_change ADD CONSTRAINT ck_benefit_grant_change_previous_status
    CHECK (previous_status IS NULL OR previous_status IN ('ACTIVE', 'SUSPENDED', 'FINALIZED', 'EXPIRED'));
ALTER TABLE public.benefit_grant_change ADD CONSTRAINT ck_benefit_grant_change_action
    CHECK (action IN ('ASSIGNED', 'RENEWED', 'SUSPENDED', 'FINALIZED', 'EXPIRED'));
ALTER TABLE public.benefit_grant_change ADD CONSTRAINT ck_benefit_grant_change_responsible
    CHECK ((action = 'EXPIRED' AND responsible_user_id IS NULL)
        OR (action <> 'EXPIRED' AND responsible_user_id IS NOT NULL));
ALTER TABLE public.benefit_grant_change ADD CONSTRAINT ck_benefit_grant_change_transition CHECK (
    (action = 'ASSIGNED' AND sequence_number = 1 AND previous_status IS NULL AND status = 'ACTIVE'
        AND previous_start_date IS NULL AND previous_end_date IS NULL)
    OR (sequence_number > 1 AND previous_status IS NOT NULL AND previous_start_date IS NOT NULL
        AND previous_end_date IS NOT NULL AND start_date = previous_start_date AND (
        (action = 'RENEWED' AND previous_status IN ('ACTIVE', 'SUSPENDED') AND status = 'ACTIVE' AND end_date > previous_end_date)
        OR (action = 'SUSPENDED' AND previous_status = 'ACTIVE' AND status = 'SUSPENDED' AND end_date = previous_end_date)
        OR (action = 'FINALIZED' AND previous_status IN ('ACTIVE', 'SUSPENDED') AND status = 'FINALIZED' AND end_date = previous_end_date)
        OR (action = 'EXPIRED' AND previous_status IN ('ACTIVE', 'SUSPENDED') AND status = 'EXPIRED' AND end_date = previous_end_date)
    ))
);
COMMIT;
