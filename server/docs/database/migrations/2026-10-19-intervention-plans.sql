-- Planes de intervencion por solicitud. Ejecutar antes del despliegue sobre el esquema existente.
-- Idempotente. No fabrica planes, objetivos, acciones ni seguimientos para casos anteriores.
BEGIN;
CREATE TABLE IF NOT EXISTS public.intervention_plan (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES public.application(id),
    responsible_user_id bigint NOT NULL REFERENCES public.users(id),
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_intervention_plan_application UNIQUE (application_id)
);
CREATE INDEX IF NOT EXISTS ix_intervention_plan_responsible ON public.intervention_plan (responsible_user_id);

CREATE TABLE IF NOT EXISTS public.intervention_objective (
    id uuid PRIMARY KEY,
    plan_id uuid NOT NULL REFERENCES public.intervention_plan(id),
    position integer NOT NULL,
    description varchar(1000) NOT NULL,
    CONSTRAINT uk_intervention_objective_position UNIQUE (plan_id, position),
    CONSTRAINT ck_intervention_objective_position CHECK (position >= 0),
    CONSTRAINT ck_intervention_objective_description CHECK (length(trim(description)) > 0)
);

CREATE TABLE IF NOT EXISTS public.intervention_action (
    id uuid PRIMARY KEY,
    objective_id uuid NOT NULL REFERENCES public.intervention_objective(id),
    position integer NOT NULL,
    description varchar(1000) NOT NULL,
    start_date date NOT NULL,
    due_date date NOT NULL,
    status varchar(20) NOT NULL,
    CONSTRAINT uk_intervention_action_position UNIQUE (objective_id, position),
    CONSTRAINT ck_intervention_action_position CHECK (position >= 0),
    CONSTRAINT ck_intervention_action_description CHECK (length(trim(description)) > 0),
    CONSTRAINT ck_intervention_action_dates CHECK (start_date <= due_date),
    CONSTRAINT ck_intervention_action_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED'))
);

CREATE TABLE IF NOT EXISTS public.intervention_follow_up (
    id uuid PRIMARY KEY,
    action_id uuid NOT NULL REFERENCES public.intervention_action(id),
    sequence_number integer NOT NULL,
    previous_status varchar(20) NOT NULL,
    status varchar(20) NOT NULL,
    occurred_on date NOT NULL,
    notes varchar(2000) NOT NULL,
    result varchar(2000),
    author_user_id bigint NOT NULL REFERENCES public.users(id),
    recorded_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_intervention_follow_up_sequence UNIQUE (action_id, sequence_number),
    CONSTRAINT ck_intervention_follow_up_sequence CHECK (sequence_number > 0),
    CONSTRAINT ck_intervention_follow_up_notes CHECK (length(trim(notes)) > 0),
    CONSTRAINT ck_intervention_follow_up_result CHECK
        ((result IS NULL OR length(trim(result)) > 0) AND (status <> 'COMPLETED' OR result IS NOT NULL)),
    CONSTRAINT ck_intervention_follow_up_transition CHECK
        (previous_status IN ('PENDING', 'IN_PROGRESS') AND status IN ('IN_PROGRESS', 'COMPLETED'))
);
CREATE INDEX IF NOT EXISTS ix_intervention_follow_up_author ON public.intervention_follow_up (author_user_id);

INSERT INTO public.permissions (name) VALUES
    ('applications:interventions:create'),
    ('applications:interventions:follow-ups:create'),
    ('applications:interventions:view')
ON CONFLICT (name) DO NOTHING;
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r
JOIN public.permissions p ON p.name IN
    ('applications:interventions:create', 'applications:interventions:follow-ups:create', 'applications:interventions:view')
WHERE r.name = 'ADMIN' ON CONFLICT DO NOTHING;
COMMIT;
