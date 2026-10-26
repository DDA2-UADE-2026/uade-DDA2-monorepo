-- Ejecutar despues de 2026-10-19-intervention-plans.sql, antes del despliegue.
-- Idempotente. No modifica planes/seguimientos ni publica automaticamente registros anteriores.
BEGIN;
CREATE TABLE IF NOT EXISTS public.intervention_follow_up_publication (
    id uuid PRIMARY KEY,
    follow_up_id uuid NOT NULL REFERENCES public.intervention_follow_up(id),
    summary varchar(2000) NOT NULL,
    visible boolean NOT NULL,
    published_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by_user_id bigint NOT NULL REFERENCES public.users(id),
    CONSTRAINT uk_intervention_publication_follow_up UNIQUE (follow_up_id),
    CONSTRAINT ck_intervention_publication_summary CHECK (length(trim(summary)) > 0),
    CONSTRAINT ck_intervention_publication_dates CHECK (published_at <= updated_at)
);
CREATE INDEX IF NOT EXISTS ix_intervention_publication_updated_by ON public.intervention_follow_up_publication(updated_by_user_id);
INSERT INTO public.permissions(name) VALUES
    ('applications:own:interventions:view'), ('applications:interventions:follow-ups:publish') ON CONFLICT(name) DO NOTHING;
INSERT INTO public.role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM public.roles r JOIN public.permissions p ON p.name = 'applications:own:interventions:view'
WHERE r.name IN ('ADMIN', 'CIUDADANO') ON CONFLICT DO NOTHING;
INSERT INTO public.role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM public.roles r JOIN public.permissions p ON p.name = 'applications:interventions:follow-ups:publish'
WHERE r.name = 'ADMIN' ON CONFLICT DO NOTHING;
COMMIT;
