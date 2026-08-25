CREATE TABLE audit_records (
    audit_record_id UUID PRIMARY KEY,
    actor_user_id UUID,
    action_type VARCHAR(96) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128),
    privacy_class VARCHAR(32) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT audit_records_actor_fk
        FOREIGN KEY (actor_user_id) REFERENCES users (user_id) ON DELETE SET NULL,
    CONSTRAINT audit_records_privacy_check CHECK (privacy_class IN ('AUDIT_RESTRICTED')),
    CONSTRAINT audit_records_outcome_check CHECK (outcome IN ('SUCCEEDED', 'FAILED', 'DENIED'))
);

CREATE INDEX audit_records_actor_occurred_idx
    ON audit_records (actor_user_id, occurred_at DESC);

CREATE INDEX audit_records_action_occurred_idx
    ON audit_records (action_type, occurred_at DESC);
