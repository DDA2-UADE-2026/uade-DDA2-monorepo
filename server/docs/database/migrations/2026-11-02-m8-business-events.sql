-- Integración de negocio M8. Ejecutar con backup y CORE_BUSINESS_ENABLED=false,
-- después de las migraciones de solicitudes, beneficios, visitas y core-integration.sql.
-- No modifica recursos RabbitMQ ni registra contratos/suscripciones en Core.
BEGIN;

ALTER TABLE public.citizen_snapshot
    ADD COLUMN IF NOT EXISTS blocked boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS block_reason varchar(2000),
    ADD COLUMN IF NOT EXISTS review_required boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS deceased_at date,
    ADD COLUMN IF NOT EXISTS last_registered_at timestamptz,
    ADD COLUMN IF NOT EXISTS last_address_at timestamptz,
    ADD COLUMN IF NOT EXISTS last_state_at timestamptz;
ALTER TABLE public.application
    ADD COLUMN IF NOT EXISTS closed_reason varchar(1000),
    ADD COLUMN IF NOT EXISTS closed_at timestamptz,
    ADD COLUMN IF NOT EXISTS closed_by_event_id uuid;
ALTER TABLE public.benefit_grant_change
    ADD COLUMN IF NOT EXISTS causation_event_id uuid,
    ADD COLUMN IF NOT EXISTS effective_end_date date;
ALTER TABLE public.benefit_grant_change DROP CONSTRAINT IF EXISTS ck_benefit_grant_change_responsible;
ALTER TABLE public.benefit_grant_change ADD CONSTRAINT ck_benefit_grant_change_responsible CHECK (
    (action = 'EXPIRED' AND responsible_user_id IS NULL) OR
    (action = 'FINALIZED' AND responsible_user_id IS NULL AND causation_event_id IS NOT NULL) OR
    (action <> 'EXPIRED' AND responsible_user_id IS NOT NULL)
);
ALTER TABLE public.application_visit ADD COLUMN IF NOT EXISTS cancelled_by_event_id uuid;
ALTER TABLE public.application_visit DROP CONSTRAINT IF EXISTS ck_application_visit_lifecycle_data;
ALTER TABLE public.application_visit ADD CONSTRAINT ck_application_visit_lifecycle_data CHECK (
    (status = 'SCHEDULED' and cancellation_reason is null and cancelled_at is null and cancelled_by_user_id is null and result_observations is null and result_conclusions is null and result_recorded_at is null and result_recorded_by_user_id is null) or (status = 'CANCELLED' and cancellation_reason is not null and length(trim(cancellation_reason)) between 1 and 1000 and cancelled_at is not null and (cancelled_by_user_id is not null or cancelled_by_event_id is not null) and result_observations is null and result_conclusions is null and result_recorded_at is null and result_recorded_by_user_id is null) or (status in ('REALIZED', 'NOT_REALIZED') and cancellation_reason is null and cancelled_at is null and cancelled_by_user_id is null and result_recorded_at is not null and result_recorded_by_user_id = responsible_user_id and result_recorded_at >= ends_at and (result_observations is null or length(trim(result_observations)) between 1 and 2000) and (result_conclusions is null or length(trim(result_conclusions)) between 1 and 2000))
);

CREATE TABLE IF NOT EXISTS public.social_ticket_cursor (
    id uuid PRIMARY KEY,
    source_updated_at timestamptz NOT NULL,
    last_event_id uuid NOT NULL
);
CREATE TABLE IF NOT EXISTS public.social_ticket (
    id uuid PRIMARY KEY,
    public_id varchar(120) NOT NULL,
    citizen_id uuid,
    is_anonymous boolean NOT NULL,
    responsible_area_id varchar(60) NOT NULL,
    current_status varchar(60) NOT NULL,
    current_priority varchar(60) NOT NULL,
    state varchar(30) NOT NULL,
    correlation_id uuid,
    last_event_id uuid NOT NULL,
    response_causation_id uuid NOT NULL,
    response_trigger varchar(60) NOT NULL,
    route_event_id uuid NOT NULL,
    started_route_event_id uuid,
    source_updated_at timestamptz NOT NULL,
    snapshot_json text NOT NULL,
    attachments_json text NOT NULL,
    application_id uuid,
    assigned_worker_user_id bigint REFERENCES public.users(id),
    information_pending boolean NOT NULL,
    information_required_by timestamptz,
    information_response_json text,
    requires_attention boolean NOT NULL,
    reopen_reason varchar(2000),
    closed_reason varchar(1000),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_social_ticket_state_updated ON public.social_ticket(state, updated_at);
CREATE INDEX IF NOT EXISTS ix_social_ticket_application ON public.social_ticket(application_id);
CREATE INDEX IF NOT EXISTS ix_social_ticket_citizen ON public.social_ticket(citizen_id);
CREATE TABLE IF NOT EXISTS public.social_revenue_debt (
    id uuid PRIMARY KEY,
    citizen_id uuid NOT NULL,
    concept_code varchar(120) NOT NULL,
    outstanding_amount numeric(19,2) NOT NULL,
    due_date date NOT NULL,
    settled boolean NOT NULL,
    settled_at timestamptz,
    remaining_balance numeric(19,2),
    outstanding_balance numeric(19,2),
    last_event_id uuid NOT NULL,
    source_updated_at timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_social_debt_citizen ON public.social_revenue_debt(citizen_id);
CREATE TABLE IF NOT EXISTS public.social_exemption_request (
    id uuid PRIMARY KEY,
    citizen_id uuid NOT NULL,
    concept_code varchar(120) NOT NULL,
    request_reason varchar(2000),
    requested_percentage numeric(7,2),
    requested_from date,
    requested_until date,
    benefit_grant_id uuid,
    ambiguous_benefit boolean NOT NULL,
    status varchar(30) NOT NULL,
    exemption_id uuid,
    percentage numeric(7,2),
    valid_from date,
    valid_until date,
    resolution_reason varchar(2000),
    last_event_id uuid NOT NULL,
    requested_event_at timestamptz,
    resolution_event_at timestamptz
);
CREATE INDEX IF NOT EXISTS ix_social_exemption_citizen ON public.social_exemption_request(citizen_id);
CREATE TABLE IF NOT EXISTS public.social_external_documentation (
    request_id uuid PRIMARY KEY,
    application_id uuid NOT NULL,
    requirement_id uuid NOT NULL,
    file_id uuid NOT NULL,
    file_number varchar(120) NOT NULL,
    document_id uuid,
    status varchar(30) NOT NULL,
    last_event_id uuid NOT NULL,
    source_updated_at timestamptz NOT NULL,
    review_status varchar(20) NOT NULL,
    observation varchar(1000),
    reviewed_by_user_id bigint REFERENCES public.users(id),
    reviewed_at timestamptz
);
CREATE INDEX IF NOT EXISTS ix_social_external_docs_application ON public.social_external_documentation(application_id);

INSERT INTO public.permissions(name) VALUES ('tickets:management:view'), ('tickets:management:manage'),
    ('revenue:management:view'), ('revenue:management:manage') ON CONFLICT(name) DO NOTHING;
INSERT INTO public.role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM public.roles r CROSS JOIN public.permissions p
WHERE r.name = 'ADMIN' AND p.name IN ('tickets:management:view', 'tickets:management:manage',
    'revenue:management:view', 'revenue:management:manage') ON CONFLICT DO NOTHING;
COMMIT;
