ALTER TABLE repositories
    ADD CONSTRAINT uq_repositories_owner UNIQUE (repository_id, user_id);
ALTER TABLE evaluations
    ADD CONSTRAINT uq_evaluations_owner UNIQUE (evaluation_id, user_id);
ALTER TABLE skill_matrices
    ADD CONSTRAINT uq_skill_matrices_owner UNIQUE (skill_matrix_id, user_id);
ALTER TABLE repository_snapshots
    ADD CONSTRAINT uq_repository_snapshots_owner_repository UNIQUE (snapshot_id, repository_id, user_id);
ALTER TABLE evaluations
    ADD CONSTRAINT uq_evaluations_snapshot_owner UNIQUE (evaluation_id, snapshot_id, user_id);
ALTER TABLE skill_matrices
    ADD CONSTRAINT uq_skill_matrices_evaluation_owner UNIQUE (skill_matrix_id, evaluation_id, user_id);

CREATE TABLE analysis_jobs (
    job_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    repository_id UUID NOT NULL,
    snapshot_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    analysis_scope VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    phase VARCHAR(32) NOT NULL,
    progress_percent INTEGER NOT NULL,
    attempt_count INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    result_analysis_id UUID,
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (repository_id, user_id) REFERENCES repositories(repository_id, user_id) ON DELETE CASCADE,
    FOREIGN KEY (snapshot_id, repository_id, user_id)
        REFERENCES repository_snapshots(snapshot_id, repository_id, user_id) ON DELETE CASCADE,
    CONSTRAINT analysis_jobs_scope_check CHECK (analysis_scope IN ('REPOSITORY_BASELINE')),
    CONSTRAINT analysis_jobs_status_check CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT analysis_jobs_progress_check CHECK (progress_percent BETWEEN 0 AND 100),
    CONSTRAINT analysis_jobs_attempt_check CHECK (attempt_count BETWEEN 0 AND max_attempts),
    CONSTRAINT analysis_jobs_owner_key_uk UNIQUE (user_id, idempotency_key)
);

CREATE UNIQUE INDEX analysis_jobs_active_basis_uk
    ON analysis_jobs(user_id, snapshot_id, analysis_scope) WHERE status IN ('QUEUED', 'RUNNING');
CREATE INDEX analysis_jobs_claim_idx
    ON analysis_jobs(status, next_attempt_at, submitted_at);
CREATE INDEX analysis_jobs_owner_idx
    ON analysis_jobs(user_id, submitted_at DESC);

CREATE TABLE analysis_results (
    analysis_id UUID PRIMARY KEY,
    job_id UUID NOT NULL UNIQUE REFERENCES analysis_jobs(job_id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    repository_id UUID NOT NULL,
    snapshot_id UUID NOT NULL,
    evaluation_id UUID NOT NULL,
    skill_matrix_id UUID NOT NULL,
    analysis_scope VARCHAR(32) NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (repository_id, user_id) REFERENCES repositories(repository_id, user_id) ON DELETE CASCADE,
    FOREIGN KEY (snapshot_id, repository_id, user_id)
        REFERENCES repository_snapshots(snapshot_id, repository_id, user_id) ON DELETE CASCADE,
    FOREIGN KEY (evaluation_id, snapshot_id, user_id)
        REFERENCES evaluations(evaluation_id, snapshot_id, user_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_matrix_id, evaluation_id, user_id)
        REFERENCES skill_matrices(skill_matrix_id, evaluation_id, user_id) ON DELETE CASCADE,
    CONSTRAINT analysis_results_scope_check CHECK (analysis_scope IN ('REPOSITORY_BASELINE'))
);

CREATE INDEX analysis_results_owner_completed_idx
    ON analysis_results(user_id, completed_at DESC, analysis_id DESC);

ALTER TABLE analysis_jobs
    ADD CONSTRAINT analysis_jobs_result_fk
    FOREIGN KEY (result_analysis_id) REFERENCES analysis_results(analysis_id) ON DELETE SET NULL;
