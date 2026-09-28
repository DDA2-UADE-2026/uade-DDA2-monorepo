-- HU-19: a cancelled appointment keeps its original interval and identifier.
-- A replacement appointment may use the same assignment and start after manual release.
-- PostgreSQL ddl-auto=update does not drop obsolete constraints automatically.
-- Run once per existing environment before enabling administrative cancellation.
-- Idempotent: retains appointments and all existing permissions.
BEGIN;

ALTER TABLE public.appointment ADD COLUMN IF NOT EXISTS slot_released_at timestamp(6) with time zone;
ALTER TABLE public.appointment DROP CONSTRAINT IF EXISTS uk_appointment_assignment_start;

-- Hibernate may have generated an enum check limited to CONFIRMED in old schemas.
DO $$
DECLARE
    old_check record;
BEGIN
    FOR old_check IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'public.appointment'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%status%'
          AND pg_get_constraintdef(oid) ILIKE '%CONFIRMED%'
          AND pg_get_constraintdef(oid) NOT ILIKE '%CANCELLED%'
    LOOP
        EXECUTE format('ALTER TABLE public.appointment DROP CONSTRAINT %I', old_check.conname);
    END LOOP;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.appointment'::regclass AND conname = 'ck_appointment_status'
    ) THEN
        ALTER TABLE public.appointment ADD CONSTRAINT ck_appointment_status
            CHECK (status IN ('CONFIRMED', 'CANCELLED'));
    END IF;
END
$$;

INSERT INTO public.permissions (name)
VALUES ('appointments:management:view'), ('appointments:management:manage')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name IN ('appointments:management:view', 'appointments:management:manage')
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
