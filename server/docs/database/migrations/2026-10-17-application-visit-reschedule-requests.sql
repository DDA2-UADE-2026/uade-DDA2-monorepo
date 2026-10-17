-- Solicitudes ciudadanas de reprogramacion de visitas y entrevistas sociales.
-- Ejecutar despues de 2026-10-17-citizen-application-visits.sql. Es idempotente.
BEGIN;

CREATE TABLE IF NOT EXISTS public.application_visit_reschedule_request (
    id uuid PRIMARY KEY,
    visit_id uuid NOT NULL REFERENCES public.application_visit(id),
    requested_by_user_id bigint NOT NULL REFERENCES public.users(id),
    reason varchar(1000) NOT NULL,
    status varchar(20) NOT NULL,
    requested_at timestamp(6) with time zone NOT NULL,
    original_starts_at timestamp(6) with time zone NOT NULL,
    original_ends_at timestamp(6) with time zone NOT NULL,
    original_location varchar(300) NOT NULL,
    reviewed_by_user_id bigint REFERENCES public.users(id),
    reviewed_at timestamp(6) with time zone,
    review_comment varchar(1000),
    approved_starts_at timestamp(6) with time zone,
    approved_ends_at timestamp(6) with time zone,
    approved_location varchar(300)
);

ALTER TABLE public.application_visit_reschedule_request
    DROP CONSTRAINT IF EXISTS ck_visit_reschedule_request_status;
ALTER TABLE public.application_visit_reschedule_request
    ADD CONSTRAINT ck_visit_reschedule_request_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));

ALTER TABLE public.application_visit_reschedule_request
    DROP CONSTRAINT IF EXISTS ck_visit_reschedule_request_reason;
ALTER TABLE public.application_visit_reschedule_request
    ADD CONSTRAINT ck_visit_reschedule_request_reason
    CHECK (length(btrim(reason)) BETWEEN 1 AND 1000);

ALTER TABLE public.application_visit_reschedule_request
    DROP CONSTRAINT IF EXISTS ck_visit_reschedule_request_original_schedule;
ALTER TABLE public.application_visit_reschedule_request
    ADD CONSTRAINT ck_visit_reschedule_request_original_schedule
    CHECK (
        original_starts_at < original_ends_at
        AND length(btrim(original_location)) BETWEEN 1 AND 300
    );

ALTER TABLE public.application_visit_reschedule_request
    DROP CONSTRAINT IF EXISTS ck_visit_reschedule_request_lifecycle;
ALTER TABLE public.application_visit_reschedule_request
    ADD CONSTRAINT ck_visit_reschedule_request_lifecycle
    CHECK (
        (status = 'PENDING'
            AND reviewed_by_user_id IS NULL
            AND reviewed_at IS NULL
            AND review_comment IS NULL
            AND approved_starts_at IS NULL
            AND approved_ends_at IS NULL
            AND approved_location IS NULL)
        OR
        (status = 'APPROVED'
            AND reviewed_by_user_id IS NOT NULL
            AND reviewed_at IS NOT NULL
            AND reviewed_at >= requested_at
            AND approved_starts_at IS NOT NULL
            AND approved_ends_at IS NOT NULL
            AND approved_starts_at < approved_ends_at
            AND approved_location IS NOT NULL
            AND length(btrim(approved_location)) BETWEEN 1 AND 300
            AND (review_comment IS NULL OR length(btrim(review_comment)) BETWEEN 1 AND 1000))
        OR
        (status = 'REJECTED'
            AND reviewed_by_user_id IS NOT NULL
            AND reviewed_at IS NOT NULL
            AND reviewed_at >= requested_at
            AND review_comment IS NOT NULL
            AND length(btrim(review_comment)) BETWEEN 1 AND 1000
            AND approved_starts_at IS NULL
            AND approved_ends_at IS NULL
            AND approved_location IS NULL)
    );

CREATE INDEX IF NOT EXISTS ix_visit_reschedule_request_visit_time
    ON public.application_visit_reschedule_request (visit_id, requested_at);

CREATE INDEX IF NOT EXISTS ix_visit_reschedule_request_status_time
    ON public.application_visit_reschedule_request (status, requested_at);

CREATE INDEX IF NOT EXISTS ix_visit_reschedule_request_requested_by
    ON public.application_visit_reschedule_request (requested_by_user_id);

CREATE INDEX IF NOT EXISTS ix_visit_reschedule_request_reviewed_by
    ON public.application_visit_reschedule_request (reviewed_by_user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_visit_reschedule_request_pending
    ON public.application_visit_reschedule_request (visit_id)
    WHERE status = 'PENDING';

INSERT INTO public.permissions (name)
VALUES ('applications:own:visits:reschedule-requests:create'),
       ('applications:visits:reschedule-requests:view'),
       ('applications:visits:reschedule-requests:review')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM public.roles role
JOIN public.permissions permission
    ON permission.name = 'applications:own:visits:reschedule-requests:create'
WHERE role.name IN ('ADMIN', 'CIUDADANO')
ON CONFLICT DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM public.roles role
JOIN public.permissions permission
    ON permission.name IN (
        'applications:visits:reschedule-requests:view',
        'applications:visits:reschedule-requests:review'
    )
WHERE role.name = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
