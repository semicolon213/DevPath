CREATE TABLE skills (
    skill_id UUID PRIMARY KEY,
    stable_key VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE', 'DEPRECATED', 'MERGED')),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE skill_matrix_policies (
    skill_matrix_policy_id UUID PRIMARY KEY,
    rule_set_version_id UUID NOT NULL UNIQUE REFERENCES rule_set_versions(rule_set_version_id),
    version_label VARCHAR(32) NOT NULL UNIQUE,
    beginner_minimum NUMERIC(5,2) NOT NULL,
    developing_minimum NUMERIC(5,2) NOT NULL,
    competent_minimum NUMERIC(5,2) NOT NULL,
    strong_minimum NUMERIC(5,2) NOT NULL,
    weakness_maximum NUMERIC(5,2) NOT NULL,
    strength_minimum NUMERIC(5,2) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'DEPRECATED')),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_skill_matrix_policy_thresholds CHECK (
        beginner_minimum = 1 AND developing_minimum > beginner_minimum
        AND competent_minimum > developing_minimum AND strong_minimum > competent_minimum
        AND strong_minimum <= 100 AND weakness_maximum >= 0 AND weakness_maximum < strength_minimum
        AND strength_minimum <= 100
    )
);

CREATE TABLE skill_policy_mappings (
    skill_policy_mapping_id UUID PRIMARY KEY,
    skill_matrix_policy_id UUID NOT NULL REFERENCES skill_matrix_policies(skill_matrix_policy_id),
    skill_id UUID NOT NULL REFERENCES skills(skill_id),
    source_category VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    UNIQUE (skill_matrix_policy_id, skill_id),
    UNIQUE (skill_matrix_policy_id, source_category)
);

CREATE TABLE skill_matrices (
    skill_matrix_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    evaluation_id UUID NOT NULL REFERENCES evaluations(evaluation_id) ON DELETE CASCADE,
    skill_matrix_policy_id UUID NOT NULL REFERENCES skill_matrix_policies(skill_matrix_policy_id),
    policy_version VARCHAR(32) NOT NULL,
    rule_set_version VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('CURRENT', 'SUPERSEDED', 'ARCHIVED')),
    generated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (evaluation_id)
);

CREATE UNIQUE INDEX uq_skill_matrices_current_owner
    ON skill_matrices(user_id) WHERE status = 'CURRENT';
CREATE INDEX idx_skill_matrices_owner_history
    ON skill_matrices(user_id, generated_at DESC, skill_matrix_id DESC);

CREATE TABLE skill_assessments (
    skill_assessment_id UUID PRIMARY KEY,
    skill_matrix_id UUID NOT NULL REFERENCES skill_matrices(skill_matrix_id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(skill_id),
    score NUMERIC(5,2) NOT NULL CHECK (score BETWEEN 0 AND 100),
    skill_level VARCHAR(24) NOT NULL CHECK (skill_level IN ('NONE', 'BEGINNER', 'DEVELOPING', 'COMPETENT', 'STRONG')),
    confidence NUMERIC(5,2) NOT NULL CHECK (confidence BETWEEN 0 AND 100),
    strength_flag BOOLEAN NOT NULL,
    weakness_flag BOOLEAN NOT NULL,
    growth_trend VARCHAR(24) NOT NULL CHECK (growth_trend IN ('UNAVAILABLE', 'IMPROVING', 'STABLE', 'DECLINING')),
    aggregate_rule_result_reference VARCHAR(160) NOT NULL,
    rule_set_version VARCHAR(32) NOT NULL,
    CONSTRAINT chk_skill_assessment_flags CHECK (NOT (strength_flag AND weakness_flag)),
    UNIQUE (skill_matrix_id, skill_id)
);

CREATE TABLE skill_evidence_links (
    skill_evidence_link_id UUID PRIMARY KEY,
    skill_assessment_id UUID NOT NULL REFERENCES skill_assessments(skill_assessment_id) ON DELETE CASCADE,
    evidence_id UUID NOT NULL REFERENCES evidence_records(evidence_id) ON DELETE CASCADE,
    evidence_strength NUMERIC(5,2) NOT NULL CHECK (evidence_strength BETWEEN 0 AND 100),
    source_role VARCHAR(32) NOT NULL CHECK (source_role IN ('DIRECT', 'SUPPORTING')),
    UNIQUE (skill_assessment_id, evidence_id)
);

CREATE TABLE skill_repository_links (
    skill_repository_link_id UUID PRIMARY KEY,
    skill_assessment_id UUID NOT NULL REFERENCES skill_assessments(skill_assessment_id) ON DELETE CASCADE,
    repository_id UUID NOT NULL REFERENCES repositories(repository_id) ON DELETE CASCADE,
    UNIQUE (skill_assessment_id, repository_id)
);

CREATE TABLE skill_assessment_facts (
    skill_assessment_fact_id UUID PRIMARY KEY,
    skill_assessment_id UUID NOT NULL REFERENCES skill_assessments(skill_assessment_id) ON DELETE CASCADE,
    fact_order INTEGER NOT NULL CHECK (fact_order >= 0),
    fact_value VARCHAR(160) NOT NULL,
    UNIQUE (skill_assessment_id, fact_order)
);

INSERT INTO skills (skill_id, stable_key, name, category, status, created_at) VALUES
('20000000-0000-0000-0000-000000000001', 'language-engineering', 'Language Engineering', 'LANGUAGE', 'ACTIVE', '2026-08-11T00:00:00Z'),
('20000000-0000-0000-0000-000000000002', 'framework-application', 'Framework Application', 'FRAMEWORK', 'ACTIVE', '2026-08-11T00:00:00Z'),
('20000000-0000-0000-0000-000000000003', 'testing-discipline', 'Testing Discipline', 'TESTING', 'ACTIVE', '2026-08-11T00:00:00Z'),
('20000000-0000-0000-0000-000000000004', 'technical-documentation', 'Technical Documentation', 'DOCUMENTATION', 'ACTIVE', '2026-08-11T00:00:00Z'),
('20000000-0000-0000-0000-000000000005', 'development-activity', 'Development Activity', 'ACTIVITY', 'ACTIVE', '2026-08-11T00:00:00Z');

INSERT INTO skill_matrix_policies (
    skill_matrix_policy_id, rule_set_version_id, version_label, beginner_minimum, developing_minimum,
    competent_minimum, strong_minimum, weakness_maximum, strength_minimum, status, created_at
) VALUES (
    '21000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001',
    'skill-matrix-v1', 1, 40, 60, 80, 39.99, 80, 'ACTIVE', '2026-08-11T00:00:00Z'
);

INSERT INTO skill_policy_mappings (skill_policy_mapping_id, skill_matrix_policy_id, skill_id, source_category, enabled) VALUES
('22000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'LANGUAGE', TRUE),
('22000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'FRAMEWORK', TRUE),
('22000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 'TESTING', TRUE),
('22000000-0000-0000-0000-000000000004', '21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000004', 'DOCUMENTATION', TRUE),
('22000000-0000-0000-0000-000000000005', '21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', 'ACTIVITY', TRUE);
