-- HU-17 - Migración manual obligatoria antes de usar centros municipales.
--
-- Contexto:
--   Las bases legacy (compartida, producción, backups viejos) tienen el
--   check ck_logs_entity_type con solo 15 valores, sin los 6 tipos nuevos:
--   municipal_center, municipal_service, center_service, center_opening_hour,
--   professional_assignment, professional_availability.
--   La entidad JPA Log no declara ningún @Check y el deploy usa
--   ddl-auto=update, que nunca crea, modifica ni borra ese check. Por eso,
--   sin este drop, la primera operación de centros falla con
--   409 DATA_INTEGRITY_VIOLATION (ck_logs_entity_type) y rollback total.
--
-- Decisión del equipo:
--   Drop definitivo del check. La validez de logs.entity_type se controla
--   en la app (enum LogEntityType + LogEntityTypeConverter). Las DBs nuevas
--   ya operan sin este constraint. NO recrearlo con ADD CONSTRAINT.
--
-- Uso:
--   Correr UNA vez por entorno (compartida, producción) con un usuario con
--   permiso ALTER TABLE, fuera de pico (toma lock ACCESS EXCLUSIVE sobre
--   logs por un instante). Es idempotente: si no hay nada que dropear,
--   no hace nada y no falla.
--   Ejemplo: psql -U <admin> -d <db> -f 2026-09-19-drop-ck-logs-entity-type.sql

-- Verificación previa (solo lectura): debe mostrar el check legacy.
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'public.logs'::regclass
  AND contype = 'c'
  AND pg_get_constraintdef(oid) ILIKE '%entity_type%';

BEGIN;

DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    FOR constraint_record IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'public.logs'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%entity_type%'
    LOOP
        EXECUTE format(
            'ALTER TABLE public.logs DROP CONSTRAINT %I',
            constraint_record.conname
        );
    END LOOP;
END
$$;

COMMIT;

-- Verificación posterior (solo lectura): debe volver vacío.
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'public.logs'::regclass
  AND contype = 'c'
  AND pg_get_constraintdef(oid) ILIKE '%entity_type%';
