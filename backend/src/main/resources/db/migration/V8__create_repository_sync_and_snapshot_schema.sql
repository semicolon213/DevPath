CREATE TABLE repository_sync_jobs (
    job_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    repository_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    job_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    phase VARCHAR(32) NOT NULL,
    progress_percent INTEGER NOT NULL,
    attempt_count INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    result_snapshot_id UUID,
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT repository_sync_jobs_user_fk FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT repository_sync_jobs_repository_fk FOREIGN KEY (repository_id) REFERENCES repositories (repository_id) ON DELETE CASCADE,
    CONSTRAINT repository_sync_jobs_type_check CHECK (job_type IN ('REPOSITORY_SYNC')),
    CONSTRAINT repository_sync_jobs_status_check CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT repository_sync_jobs_progress_check CHECK (progress_percent BETWEEN 0 AND 100),
    CONSTRAINT repository_sync_jobs_attempt_check CHECK (attempt_count BETWEEN 0 AND max_attempts),
    CONSTRAINT repository_sync_jobs_owner_key_uk UNIQUE (user_id, idempotency_key)
);

CREATE UNIQUE INDEX repository_sync_jobs_active_repository_uk
    ON repository_sync_jobs (repository_id) WHERE status IN ('QUEUED', 'RUNNING');
CREATE INDEX repository_sync_jobs_claim_idx
    ON repository_sync_jobs (status, next_attempt_at, submitted_at);
CREATE INDEX repository_sync_jobs_owner_idx
    ON repository_sync_jobs (user_id, submitted_at DESC);

CREATE TABLE repository_snapshots (
    snapshot_id UUID PRIMARY KEY,
    repository_id UUID NOT NULL,
    user_id UUID NOT NULL,
    source_revision VARCHAR(64) NOT NULL,
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL,
    snapshot_status VARCHAR(24) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    branch_count INTEGER NOT NULL,
    commit_count INTEGER NOT NULL,
    retention_status VARCHAR(24) NOT NULL,
    CONSTRAINT repository_snapshots_repository_fk FOREIGN KEY (repository_id) REFERENCES repositories (repository_id) ON DELETE CASCADE,
    CONSTRAINT repository_snapshots_user_fk FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT repository_snapshots_status_check CHECK (snapshot_status IN ('READY', 'FAILED', 'SUPERSEDED', 'DELETED_BY_POLICY')),
    CONSTRAINT repository_snapshots_retention_check CHECK (retention_status IN ('ACTIVE', 'DELETED_BY_POLICY')),
    CONSTRAINT repository_snapshots_counts_check CHECK (branch_count >= 0 AND commit_count >= 0)
);

CREATE INDEX repository_snapshots_owner_repository_idx
    ON repository_snapshots (user_id, repository_id, captured_at DESC);

CREATE TABLE repository_branches (
    branch_record_id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    default_branch BOOLEAN NOT NULL,
    head_commit_sha VARCHAR(64) NOT NULL,
    CONSTRAINT repository_branches_snapshot_fk FOREIGN KEY (snapshot_id) REFERENCES repository_snapshots (snapshot_id) ON DELETE CASCADE,
    CONSTRAINT repository_branches_snapshot_name_uk UNIQUE (snapshot_id, branch_name)
);

CREATE TABLE repository_commits (
    commit_record_id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    commit_sha VARCHAR(64) NOT NULL,
    author_login VARCHAR(255),
    committed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    message_summary VARCHAR(500) NOT NULL,
    CONSTRAINT repository_commits_snapshot_fk FOREIGN KEY (snapshot_id) REFERENCES repository_snapshots (snapshot_id) ON DELETE CASCADE,
    CONSTRAINT repository_commits_snapshot_sha_uk UNIQUE (snapshot_id, commit_sha)
);

CREATE INDEX repository_commits_snapshot_time_idx
    ON repository_commits (snapshot_id, committed_at DESC);

CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempt_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX outbox_events_unpublished_idx
    ON outbox_events (occurred_at) WHERE published_at IS NULL;

ALTER TABLE repositories ADD COLUMN current_snapshot_id UUID;
ALTER TABLE repositories
    ADD CONSTRAINT repositories_current_snapshot_fk
    FOREIGN KEY (current_snapshot_id) REFERENCES repository_snapshots (snapshot_id) ON DELETE SET NULL;
ALTER TABLE repository_sync_jobs
    ADD CONSTRAINT repository_sync_jobs_result_snapshot_fk
    FOREIGN KEY (result_snapshot_id) REFERENCES repository_snapshots (snapshot_id) ON DELETE SET NULL;
