-- Visitas y entrevistas sociales programadas, con permisos de alta y consulta.
-- Ejecutar antes del despliegue si el esquema no es creado automaticamente. Es idempotente.
BEGIN;

CREATE TABLE IF NOT EXISTS public.application_visit (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES public.application(id),
    type varchar(20) NOT NULL,
    starts_at timestamp(6) with time zone NOT NULL,
    ends_at timestamp(6) with time zone NOT NULL,
    location varchar(300) NOT NULL,
    responsible_user_id bigint NOT NULL REFERENCES public.users(id),
    status varchar(20) NOT NULL,
    scheduled_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT ck_application_visit_type CHECK (type IN ('VISIT', 'INTERVIEW')),
    CONSTRAINT ck_application_visit_status CHECK (status IN ('SCHEDULED')),
    CONSTRAINT ck_application_visit_time_range CHECK (starts_at < ends_at)
);

CREATE INDEX IF NOT EXISTS ix_application_visit_application
    ON public.application_visit (application_id);

CREATE INDEX IF NOT EXISTS ix_application_visit_responsible_time
    ON public.application_visit (responsible_user_id, starts_at, ends_at, status);

CREATE INDEX IF NOT EXISTS ix_application_visit_starts_at
    ON public.application_visit (starts_at);

INSERT INTO public.permissions (name)
VALUES ('applications:visits:create'),
       ('applications:visits:view')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name IN ('applications:visits:create', 'applications:visits:view')
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
