CREATE TABLE rule_sets (
    rule_set_id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    scope VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(24) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'DEPRECATED')),
    active_rule_set_version_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE rule_set_versions (
    rule_set_version_id UUID PRIMARY KEY,
    rule_set_id UUID NOT NULL REFERENCES rule_sets(rule_set_id),
    version_label VARCHAR(32) NOT NULL,
    formula_library_version VARCHAR(32) NOT NULL,
    required_extractor_version VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'SUPERSEDED', 'DEPRECATED')),
    validation_status VARCHAR(24) NOT NULL CHECK (validation_status IN ('PENDING', 'VALID', 'INVALID')),
    effective_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (rule_set_id, version_label)
);

ALTER TABLE rule_sets
    ADD CONSTRAINT fk_rule_sets_active_version
    FOREIGN KEY (active_rule_set_version_id) REFERENCES rule_set_versions(rule_set_version_id);

CREATE TABLE rule_category_weights (
    rule_set_version_id UUID NOT NULL REFERENCES rule_set_versions(rule_set_version_id),
    category VARCHAR(32) NOT NULL,
    weight NUMERIC(9, 6) NOT NULL CHECK (weight > 0 AND weight <= 1),
    PRIMARY KEY (rule_set_version_id, category)
);

CREATE TABLE rules (
    rule_id VARCHAR(64) NOT NULL,
    rule_set_version_id UUID NOT NULL REFERENCES rule_set_versions(rule_set_version_id),
    rule_version VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500) NOT NULL,
    priority INTEGER NOT NULL CHECK (priority >= 0),
    evidence_signal_key VARCHAR(64) NOT NULL,
    formula_id VARCHAR(32) NOT NULL CHECK (formula_id IN ('PRESENCE', 'COUNT_CAP', 'PERCENTAGE')),
    formula_parameter NUMERIC(18, 6) NOT NULL CHECK (formula_parameter >= 0),
    weight NUMERIC(9, 6) NOT NULL CHECK (weight > 0 AND weight <= 1),
    missing_data_policy VARCHAR(16) NOT NULL CHECK (missing_data_policy IN ('ZERO', 'SKIP')),
    enabled BOOLEAN NOT NULL,
    PRIMARY KEY (rule_set_version_id, rule_id)
);

CREATE INDEX idx_rules_version_category_priority
    ON rules(rule_set_version_id, category, priority, rule_id);

INSERT INTO rule_sets (rule_set_id, name, scope, status, created_at)
VALUES ('10000000-0000-0000-0000-000000000001', 'DevPath Baseline', 'REPOSITORY_BASELINE', 'ACTIVE', '2026-08-11T00:00:00Z');

INSERT INTO rule_set_versions (
    rule_set_version_id, rule_set_id, version_label, formula_library_version,
    required_extractor_version, status, validation_status, effective_at, created_at
) VALUES (
    '11000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
    'baseline-v1', 'formula-v1', 'engineering-evidence-extractor-v1',
    'ACTIVE', 'VALID', '2026-08-11T00:00:00Z', '2026-08-11T00:00:00Z'
);

UPDATE rule_sets
SET active_rule_set_version_id = '11000000-0000-0000-0000-000000000001'
WHERE rule_set_id = '10000000-0000-0000-0000-000000000001';

INSERT INTO rule_category_weights (rule_set_version_id, category, weight) VALUES
('11000000-0000-0000-0000-000000000001', 'LANGUAGE', 0.250000),
('11000000-0000-0000-0000-000000000001', 'FRAMEWORK', 0.150000),
('11000000-0000-0000-0000-000000000001', 'TESTING', 0.250000),
('11000000-0000-0000-0000-000000000001', 'DOCUMENTATION', 0.200000),
('11000000-0000-0000-0000-000000000001', 'ACTIVITY', 0.150000);

INSERT INTO rules (
    rule_id, rule_set_version_id, rule_version, category, name, description, priority,
    evidence_signal_key, formula_id, formula_parameter, weight, missing_data_policy, enabled
) VALUES
('LANGUAGE_PRIMARY_SHARE', '11000000-0000-0000-0000-000000000001', '1.0.0', 'LANGUAGE', 'Primary language share', 'Scores the measured share of the primary language.', 20, 'LANGUAGE_PRIMARY_SHARE', 'PERCENTAGE', 0, 0.700000, 'ZERO', TRUE),
('LANGUAGE_DIVERSITY', '11000000-0000-0000-0000-000000000001', '1.0.0', 'LANGUAGE', 'Language diversity', 'Caps language diversity credit at three measured languages.', 20, 'LANGUAGE_DIVERSITY', 'COUNT_CAP', 3, 0.300000, 'ZERO', TRUE),
('FRAMEWORK_COUNT', '11000000-0000-0000-0000-000000000001', '1.0.0', 'FRAMEWORK', 'Framework evidence', 'Caps framework evidence credit at two normalized frameworks.', 20, 'FRAMEWORK_COUNT', 'COUNT_CAP', 2, 1.000000, 'ZERO', TRUE),
('TEST_FILES', '11000000-0000-0000-0000-000000000001', '1.0.0', 'TESTING', 'Test files', 'Caps test-file evidence credit at ten normalized test files.', 20, 'TEST_FILES', 'COUNT_CAP', 10, 0.500000, 'ZERO', TRUE),
('TEST_FRAMEWORKS', '11000000-0000-0000-0000-000000000001', '1.0.0', 'TESTING', 'Test frameworks', 'Caps test-framework evidence credit at two normalized frameworks.', 20, 'TEST_FRAMEWORKS', 'COUNT_CAP', 2, 0.300000, 'ZERO', TRUE),
('CI_WORKFLOW_METADATA', '11000000-0000-0000-0000-000000000001', '1.0.0', 'TESTING', 'CI workflow metadata', 'Credits traceable CI workflow metadata presence.', 20, 'CI_WORKFLOW_METADATA', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('README_PRESENT', '11000000-0000-0000-0000-000000000001', '1.0.0', 'DOCUMENTATION', 'README present', 'Credits a repository README path.', 20, 'README_PRESENT', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('API_DOCUMENTATION', '11000000-0000-0000-0000-000000000001', '1.0.0', 'DOCUMENTATION', 'API documentation', 'Credits API contract or documentation paths.', 20, 'API_DOCUMENTATION', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('ARCHITECTURE_DOCUMENTATION', '11000000-0000-0000-0000-000000000001', '1.0.0', 'DOCUMENTATION', 'Architecture documentation', 'Credits architecture or decision documentation paths.', 20, 'ARCHITECTURE_DOCUMENTATION', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('CONTRIBUTING_GUIDE', '11000000-0000-0000-0000-000000000001', '1.0.0', 'DOCUMENTATION', 'Contributing guide', 'Credits a contributing guide path.', 20, 'CONTRIBUTING_GUIDE', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('LICENSE_PRESENT', '11000000-0000-0000-0000-000000000001', '1.0.0', 'DOCUMENTATION', 'License present', 'Credits a repository license path.', 20, 'LICENSE_PRESENT', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('COMMIT_COUNT', '11000000-0000-0000-0000-000000000001', '1.0.0', 'ACTIVITY', 'Captured commits', 'Caps captured commit evidence credit at fifty commits.', 20, 'COMMIT_COUNT', 'COUNT_CAP', 50, 0.600000, 'ZERO', TRUE),
('CONTRIBUTOR_COUNT', '11000000-0000-0000-0000-000000000001', '1.0.0', 'ACTIVITY', 'Captured contributors', 'Caps captured contributor evidence credit at three contributors.', 20, 'CONTRIBUTOR_COUNT', 'COUNT_CAP', 3, 0.200000, 'ZERO', TRUE),
('BRANCH_COUNT', '11000000-0000-0000-0000-000000000001', '1.0.0', 'ACTIVITY', 'Captured branches', 'Caps captured branch evidence credit at three branches.', 20, 'BRANCH_COUNT', 'COUNT_CAP', 3, 0.200000, 'ZERO', TRUE);
