-- Aplicar deliberadamente sobre la base propia; no ejecuta seeds ni toca tablas de negocio.
CREATE TABLE IF NOT EXISTS core_outbox (
    event_id uuid PRIMARY KEY,
    event_type varchar(120) NOT NULL,
    event_version varchar(20) NOT NULL,
    correlation_id uuid,
    occurred_at timestamptz NOT NULL,
    payload_json text NOT NULL,
    payload_hash varchar(64) NOT NULL,
    trace_id varchar(100) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL,
    lease_token uuid,
    lease_until timestamptz,
    last_error varchar(100),
    created_at timestamptz NOT NULL,
    confirmed_at timestamptz,
    CONSTRAINT ck_core_outbox_status CHECK (status IN ('PENDING','IN_FLIGHT','BROKER_CONFIRMED')),
    CONSTRAINT ck_core_outbox_attempts CHECK (attempts >= 0),
    CONSTRAINT ck_core_outbox_lease CHECK (
        (status='IN_FLIGHT' AND lease_token IS NOT NULL AND lease_until IS NOT NULL)
        OR (status<>'IN_FLIGHT' AND lease_token IS NULL AND lease_until IS NULL))
);
CREATE INDEX IF NOT EXISTS ix_core_outbox_pending ON core_outbox(status, next_attempt_at);

CREATE TABLE IF NOT EXISTS core_inbox (
    event_id uuid PRIMARY KEY,
    event_type varchar(120) NOT NULL,
    event_version varchar(20) NOT NULL,
    source_module varchar(60) NOT NULL,
    payload_hash varchar(64) NOT NULL,
    correlation_id uuid,
    occurred_at timestamptz NOT NULL,
    received_at timestamptz NOT NULL,
    processed_at timestamptz NOT NULL,
    message_id varchar(100),
    trace_id varchar(100),
    target_module varchar(60),
    delivery_attempt integer
);
