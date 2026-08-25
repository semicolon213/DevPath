ALTER TABLE repository_snapshots
    ADD CONSTRAINT uq_repository_snapshots_owner UNIQUE (snapshot_id, user_id);

CREATE TABLE evaluations (
    evaluation_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    snapshot_id UUID NOT NULL,
    rule_set_version_id UUID NOT NULL REFERENCES rule_set_versions(rule_set_version_id),
    status VARCHAR(24) NOT NULL CHECK (status IN ('REQUESTED', 'RUNNING', 'COMPLETED', 'FAILED', 'SUPERSEDED')),
    input_hash CHAR(64) NOT NULL CHECK (input_hash ~ '^[a-f0-9]{64}$'),
    rule_set_version_label VARCHAR(32) NOT NULL,
    formula_library_version VARCHAR(32) NOT NULL,
    extractor_version VARCHAR(64) NOT NULL,
    overall_score NUMERIC(5, 2) NOT NULL CHECK (overall_score BETWEEN 0 AND 100),
    confidence NUMERIC(5, 2) NOT NULL CHECK (confidence BETWEEN 0 AND 100),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (snapshot_id, user_id) REFERENCES repository_snapshots(snapshot_id, user_id) ON DELETE CASCADE,
    CONSTRAINT chk_evaluation_completion CHECK (completed_at >= started_at),
    CONSTRAINT uq_evaluation_basis UNIQUE (user_id, snapshot_id, rule_set_version_id, input_hash)
);

CREATE INDEX idx_evaluations_owner_completed
    ON evaluations(user_id, completed_at DESC, evaluation_id DESC);

CREATE TABLE evaluation_warnings (
    evaluation_warning_id UUID PRIMARY KEY,
    evaluation_id UUID NOT NULL REFERENCES evaluations(evaluation_id) ON DELETE CASCADE,
    warning_order INTEGER NOT NULL CHECK (warning_order >= 0),
    warning_message VARCHAR(500) NOT NULL,
    UNIQUE (evaluation_id, warning_order)
);

CREATE TABLE category_evaluations (
    category_evaluation_id UUID PRIMARY KEY,
    evaluation_id UUID NOT NULL REFERENCES evaluations(evaluation_id) ON DELETE CASCADE,
    category VARCHAR(32) NOT NULL,
    score NUMERIC(5, 2) NOT NULL CHECK (score BETWEEN 0 AND 100),
    weight NUMERIC(9, 6) NOT NULL CHECK (weight > 0 AND weight <= 1),
    confidence NUMERIC(5, 2) NOT NULL CHECK (confidence BETWEEN 0 AND 100),
    evidence_count INTEGER NOT NULL CHECK (evidence_count >= 0),
    UNIQUE (evaluation_id, category)
);

CREATE TABLE category_missing_evidence (
    category_missing_evidence_id UUID PRIMARY KEY,
    category_evaluation_id UUID NOT NULL REFERENCES category_evaluations(category_evaluation_id) ON DELETE CASCADE,
    missing_order INTEGER NOT NULL CHECK (missing_order >= 0),
    evidence_key VARCHAR(128) NOT NULL,
    UNIQUE (category_evaluation_id, missing_order)
);

CREATE TABLE rule_execution_results (
    rule_execution_result_id UUID PRIMARY KEY,
    evaluation_id UUID NOT NULL REFERENCES evaluations(evaluation_id) ON DELETE CASCADE,
    rule_set_version_id UUID NOT NULL,
    rule_id VARCHAR(64) NOT NULL,
    rule_version VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    outcome_status VARCHAR(16) NOT NULL CHECK (outcome_status IN ('PASSED', 'FAILED', 'PARTIAL', 'SKIPPED', 'ERROR')),
    raw_value NUMERIC(18, 6) NOT NULL CHECK (raw_value >= 0),
    score NUMERIC(5, 2) NOT NULL CHECK (score BETWEEN 0 AND 100),
    weight NUMERIC(9, 6) NOT NULL CHECK (weight > 0 AND weight <= 1),
    formula_id VARCHAR(80) NOT NULL,
    calculation_trace VARCHAR(1000) NOT NULL,
    evidence_count INTEGER NOT NULL CHECK (evidence_count >= 0),
    FOREIGN KEY (rule_set_version_id, rule_id) REFERENCES rules(rule_set_version_id, rule_id),
    UNIQUE (evaluation_id, rule_id),
    UNIQUE (rule_execution_result_id, evaluation_id)
);

CREATE INDEX idx_rule_execution_results_evaluation_category
    ON rule_execution_results(evaluation_id, category, rule_id);

CREATE TABLE evidence_records (
    evidence_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    snapshot_id UUID NOT NULL,
    evidence_type VARCHAR(32) NOT NULL,
    source_reference VARCHAR(1200) NOT NULL,
    source_reference_hash CHAR(64) NOT NULL CHECK (source_reference_hash ~ '^[a-f0-9]{64}$'),
    observed_fact_summary VARCHAR(500) NOT NULL,
    confidence NUMERIC(5, 2) NOT NULL CHECK (confidence BETWEEN 0 AND 100),
    created_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (snapshot_id, user_id) REFERENCES repository_snapshots(snapshot_id, user_id) ON DELETE CASCADE,
    UNIQUE (user_id, snapshot_id, source_reference_hash)
);

CREATE INDEX idx_evidence_records_owner_snapshot
    ON evidence_records(user_id, snapshot_id, evidence_id);

CREATE TABLE score_evidence_links (
    score_evidence_link_id UUID PRIMARY KEY,
    evidence_id UUID NOT NULL REFERENCES evidence_records(evidence_id) ON DELETE CASCADE,
    evaluation_id UUID NOT NULL REFERENCES evaluations(evaluation_id) ON DELETE CASCADE,
    rule_execution_result_id UUID NOT NULL,
    contribution_role VARCHAR(32) NOT NULL CHECK (contribution_role IN ('DIRECT', 'SUPPORTING', 'MISSING')),
    FOREIGN KEY (rule_execution_result_id, evaluation_id)
        REFERENCES rule_execution_results(rule_execution_result_id, evaluation_id) ON DELETE CASCADE,
    UNIQUE (rule_execution_result_id, evidence_id)
);

CREATE INDEX idx_score_evidence_links_evaluation
    ON score_evidence_links(evaluation_id, rule_execution_result_id, evidence_id);
