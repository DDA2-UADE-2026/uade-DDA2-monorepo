-- HU-20: habilita constancias de asistencia y permisos profesionales en bases PostgreSQL existentes.
-- Ejecutar antes del despliegue si el esquema no es creado automáticamente. Es idempotente.
BEGIN;

CREATE TABLE IF NOT EXISTS public.community_attention (
    id uuid PRIMARY KEY,
    appointment_id uuid NOT NULL CONSTRAINT uk_community_attention_appointment UNIQUE
        REFERENCES public.appointment(id),
    result varchar(20) NOT NULL,
    attended_on date,
    description varchar(500),
    version bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT ck_community_attention_result_data CHECK (
        (result = 'ATENDIDO' AND attended_on IS NOT NULL AND description IS NOT NULL)
        OR (result = 'AUSENTE' AND attended_on IS NULL AND description IS NULL)
    )
);

INSERT INTO public.permissions (name)
VALUES ('appointments:professional:view'), ('appointments:professional:manage')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name IN ('appointments:professional:view', 'appointments:professional:manage')
WHERE r.name = 'PROFESIONAL_CENTRO'
ON CONFLICT DO NOTHING;

COMMIT;
