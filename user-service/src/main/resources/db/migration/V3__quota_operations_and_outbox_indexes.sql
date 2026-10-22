CREATE UNIQUE INDEX IF NOT EXISTS ux_outbox_aggregate_event ON outbox_events (aggregate_id, event_type);
CREATE INDEX IF NOT EXISTS ix_outbox_status_available ON outbox_events (status, available_at);

CREATE TABLE quota_operations (
    operation_id UUID PRIMARY KEY,
    user_id      UUID         NOT NULL,
    amount       INTEGER      NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_quota_operations_user ON quota_operations (user_id);
