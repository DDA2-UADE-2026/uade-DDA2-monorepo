-- Evaluacion social unica por solicitud y permisos de registro/consulta para bases PostgreSQL existentes.
-- Ejecutar antes del despliegue si el esquema no es creado automaticamente. Es idempotente.
BEGIN;

CREATE TABLE IF NOT EXISTS public.application_evaluation (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL CONSTRAINT uk_application_evaluation_application UNIQUE
        REFERENCES public.application(id),
    vulnerability varchar(20) NOT NULL,
    priority varchar(20) NOT NULL,
    recommendation varchar(1000) NOT NULL,
    professional_user_id bigint NOT NULL REFERENCES public.users(id),
    evaluated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT ck_application_evaluation_vulnerability
        CHECK (vulnerability IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_application_evaluation_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

CREATE INDEX IF NOT EXISTS ix_application_evaluation_professional
    ON public.application_evaluation (professional_user_id);

CREATE INDEX IF NOT EXISTS ix_application_evaluation_evaluated_at
    ON public.application_evaluation (evaluated_at DESC);

INSERT INTO public.permissions (name)
VALUES ('applications:evaluations:create'),
       ('applications:evaluations:view')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name IN ('applications:evaluations:create', 'applications:evaluations:view')
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
