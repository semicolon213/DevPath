INSERT INTO rule_set_versions (
    rule_set_version_id, rule_set_id, version_label, formula_library_version,
    required_extractor_version, status, validation_status, effective_at, created_at
) VALUES (
    '11000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
    'baseline-v2', 'formula-v1', 'engineering-evidence-extractor-v2',
    'ACTIVE', 'VALID', '2026-08-24T00:00:00Z', '2026-08-24T00:00:00Z'
);

UPDATE rule_set_versions
SET status = 'SUPERSEDED'
WHERE rule_set_version_id = '11000000-0000-0000-0000-000000000001';

UPDATE rule_sets
SET active_rule_set_version_id = '11000000-0000-0000-0000-000000000002'
WHERE rule_set_id = '10000000-0000-0000-0000-000000000001';

INSERT INTO rule_category_weights (rule_set_version_id, category, weight) VALUES
('11000000-0000-0000-0000-000000000002', 'LANGUAGE', 0.150000),
('11000000-0000-0000-0000-000000000002', 'FRAMEWORK', 0.150000),
('11000000-0000-0000-0000-000000000002', 'DATABASE', 0.150000),
('11000000-0000-0000-0000-000000000002', 'ARCHITECTURE', 0.150000),
('11000000-0000-0000-0000-000000000002', 'TESTING', 0.150000),
('11000000-0000-0000-0000-000000000002', 'DEVOPS', 0.100000),
('11000000-0000-0000-0000-000000000002', 'DOCUMENTATION', 0.100000),
('11000000-0000-0000-0000-000000000002', 'ACTIVITY', 0.050000);

INSERT INTO rules (
    rule_id, rule_set_version_id, rule_version, category, name, description, priority,
    evidence_signal_key, formula_id, formula_parameter, weight, missing_data_policy, enabled
) VALUES
('LANGUAGE_PRIMARY_SHARE', '11000000-0000-0000-0000-000000000002', '2.0.0', 'LANGUAGE', 'Primary language share', 'Scores the measured share of the primary language.', 20, 'LANGUAGE_PRIMARY_SHARE', 'PERCENTAGE', 0, 0.700000, 'ZERO', TRUE),
('LANGUAGE_DIVERSITY', '11000000-0000-0000-0000-000000000002', '2.0.0', 'LANGUAGE', 'Language diversity', 'Caps language diversity credit at three measured languages.', 20, 'LANGUAGE_DIVERSITY', 'COUNT_CAP', 3, 0.300000, 'ZERO', TRUE),
('FRAMEWORK_COUNT', '11000000-0000-0000-0000-000000000002', '2.0.0', 'FRAMEWORK', 'Framework evidence', 'Caps framework evidence credit at two normalized frameworks.', 20, 'FRAMEWORK_COUNT', 'COUNT_CAP', 2, 1.000000, 'ZERO', TRUE),
('DATABASE_TECHNOLOGIES', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DATABASE', 'Database technologies', 'Credits normalized database technology declarations.', 20, 'DATABASE_TECHNOLOGIES', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('DATA_ACCESS_DEPENDENCIES', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DATABASE', 'Data access dependencies', 'Credits normalized data-access dependencies.', 20, 'DATA_ACCESS_DEPENDENCIES', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('DATABASE_MIGRATIONS', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DATABASE', 'Database migrations', 'Credits immutable migration assets.', 20, 'DATABASE_MIGRATIONS', 'PRESENCE', 0, 0.350000, 'ZERO', TRUE),
('PERSISTENCE_CONFIGURATION', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DATABASE', 'Persistence configuration', 'Credits traceable persistence configuration.', 20, 'PERSISTENCE_CONFIGURATION', 'PRESENCE', 0, 0.250000, 'ZERO', TRUE),
('STRUCTURED_BOUNDARIES', '11000000-0000-0000-0000-000000000002', '2.0.0', 'ARCHITECTURE', 'Structured boundaries', 'Credits either complete hexagonal or complete layered boundaries.', 20, 'STRUCTURED_BOUNDARIES', 'PRESENCE', 0, 0.500000, 'ZERO', TRUE),
('MODULE_LAYOUT', '11000000-0000-0000-0000-000000000002', '2.0.0', 'ARCHITECTURE', 'Module layout', 'Credits a measurable multi-module or multi-root layout.', 20, 'MODULE_LAYOUT', 'PRESENCE', 0, 0.250000, 'ZERO', TRUE),
('ARCHITECTURE_DOCUMENTATION_STRUCTURE', '11000000-0000-0000-0000-000000000002', '2.0.0', 'ARCHITECTURE', 'Architecture documentation', 'Credits architecture or decision documentation paths.', 20, 'ARCHITECTURE_DOCUMENTATION', 'PRESENCE', 0, 0.250000, 'ZERO', TRUE),
('TEST_FILES', '11000000-0000-0000-0000-000000000002', '2.0.0', 'TESTING', 'Test files', 'Caps test-file evidence credit at ten normalized test files.', 20, 'TEST_FILES', 'COUNT_CAP', 10, 0.500000, 'ZERO', TRUE),
('TEST_FRAMEWORKS', '11000000-0000-0000-0000-000000000002', '2.0.0', 'TESTING', 'Test frameworks', 'Caps test-framework evidence credit at two normalized frameworks.', 20, 'TEST_FRAMEWORKS', 'COUNT_CAP', 2, 0.300000, 'ZERO', TRUE),
('CI_WORKFLOW_METADATA', '11000000-0000-0000-0000-000000000002', '2.0.0', 'TESTING', 'CI workflow metadata', 'Credits traceable CI workflow metadata presence.', 20, 'CI_WORKFLOW_METADATA', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('CONTAINER_CONFIGURATION', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DEVOPS', 'Container configuration', 'Credits container build or composition configuration.', 20, 'CONTAINER_CONFIGURATION', 'PRESENCE', 0, 0.300000, 'ZERO', TRUE),
('CI_WORKFLOWS', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DEVOPS', 'CI workflows', 'Credits CI workflow configuration.', 20, 'CI_WORKFLOWS', 'PRESENCE', 0, 0.300000, 'ZERO', TRUE),
('INFRASTRUCTURE_AS_CODE', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DEVOPS', 'Infrastructure as code', 'Credits infrastructure-as-code assets.', 20, 'INFRASTRUCTURE_AS_CODE', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('DEPLOYMENT_CONFIGURATION', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DEVOPS', 'Deployment configuration', 'Credits deployment configuration assets.', 20, 'DEPLOYMENT_CONFIGURATION', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('README_PRESENT', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DOCUMENTATION', 'README present', 'Credits a repository README path.', 20, 'README_PRESENT', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('API_DOCUMENTATION', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DOCUMENTATION', 'API documentation', 'Credits API contract or documentation paths.', 20, 'API_DOCUMENTATION', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('ARCHITECTURE_DOCUMENTATION', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DOCUMENTATION', 'Architecture documentation', 'Credits architecture or decision documentation paths.', 20, 'ARCHITECTURE_DOCUMENTATION', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('CONTRIBUTING_GUIDE', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DOCUMENTATION', 'Contributing guide', 'Credits a contributing guide path.', 20, 'CONTRIBUTING_GUIDE', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('LICENSE_PRESENT', '11000000-0000-0000-0000-000000000002', '2.0.0', 'DOCUMENTATION', 'License present', 'Credits a repository license path.', 20, 'LICENSE_PRESENT', 'PRESENCE', 0, 0.200000, 'ZERO', TRUE),
('COMMIT_COUNT', '11000000-0000-0000-0000-000000000002', '2.0.0', 'ACTIVITY', 'Captured commits', 'Caps captured commit evidence credit at fifty commits.', 20, 'COMMIT_COUNT', 'COUNT_CAP', 50, 0.600000, 'ZERO', TRUE),
('CONTRIBUTOR_COUNT', '11000000-0000-0000-0000-000000000002', '2.0.0', 'ACTIVITY', 'Captured contributors', 'Caps captured contributor evidence credit at three contributors.', 20, 'CONTRIBUTOR_COUNT', 'COUNT_CAP', 3, 0.200000, 'ZERO', TRUE),
('BRANCH_COUNT', '11000000-0000-0000-0000-000000000002', '2.0.0', 'ACTIVITY', 'Captured branches', 'Caps captured branch evidence credit at three captured branches.', 20, 'BRANCH_COUNT', 'COUNT_CAP', 3, 0.200000, 'ZERO', TRUE);

UPDATE skill_matrix_policies
SET status = 'SUPERSEDED'
WHERE skill_matrix_policy_id = '21000000-0000-0000-0000-000000000001';

INSERT INTO skills (skill_id, stable_key, name, category, status, created_at) VALUES
('20000000-0000-0000-0000-000000000006', 'database-engineering', 'Database Engineering', 'DATABASE', 'ACTIVE', '2026-08-24T00:00:00Z'),
('20000000-0000-0000-0000-000000000007', 'architecture-structure', 'Architecture Structure', 'ARCHITECTURE', 'ACTIVE', '2026-08-24T00:00:00Z'),
('20000000-0000-0000-0000-000000000008', 'delivery-operations', 'Delivery Operations', 'DEVOPS', 'ACTIVE', '2026-08-24T00:00:00Z');

INSERT INTO skill_matrix_policies (
    skill_matrix_policy_id, rule_set_version_id, version_label, beginner_minimum, developing_minimum,
    competent_minimum, strong_minimum, weakness_maximum, strength_minimum, status, created_at
) VALUES (
    '21000000-0000-0000-0000-000000000002', '11000000-0000-0000-0000-000000000002',
    'skill-matrix-v2', 1, 40, 60, 80, 39.99, 80, 'ACTIVE', '2026-08-24T00:00:00Z'
);

INSERT INTO skill_policy_mappings (skill_policy_mapping_id, skill_matrix_policy_id, skill_id, source_category, enabled) VALUES
('22000000-0000-0000-0000-000000000011', '21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'LANGUAGE', TRUE),
('22000000-0000-0000-0000-000000000012', '21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'FRAMEWORK', TRUE),
('22000000-0000-0000-0000-000000000013', '21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'TESTING', TRUE),
('22000000-0000-0000-0000-000000000014', '21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004', 'DOCUMENTATION', TRUE),
('22000000-0000-0000-0000-000000000015', '21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000005', 'ACTIVITY', TRUE),
('22000000-0000-0000-0000-000000000016', '21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000006', 'DATABASE', TRUE),
('22000000-0000-0000-0000-000000000017', '21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000007', 'ARCHITECTURE', TRUE),
('22000000-0000-0000-0000-000000000018', '21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000008', 'DEVOPS', TRUE);

UPDATE career_profile_versions
SET status = 'SUPERSEDED'
WHERE career_profile_version_id IN (
    '32000000-0000-0000-0000-000000000001',
    '32000000-0000-0000-0000-000000000002'
);

INSERT INTO career_profile_versions (
    career_profile_version_id, career_id, version_label, status, purpose, core_technologies,
    required_competencies, preferred_competencies, evaluation_categories, priority_weights,
    roadmap_template, effective_at
) VALUES
('32000000-0000-0000-0000-000000000101', 'backend', 'career-v2', 'ACTIVE', '서버 측 서비스와 API를 설계하고 구현합니다.',
 '["Java","Spring Boot","SQL","Redis","Docker"]', '["API 설계","데이터베이스 연동","테스트","아키텍처","개발 활동"]',
 '["캐싱","메시징","CI/CD","관측 가능성"]', '["LANGUAGE","FRAMEWORK","DATABASE","ARCHITECTURE","TESTING","DEVOPS"]',
 '{"LANGUAGE":"15","FRAMEWORK":"20","DATABASE":"20","ARCHITECTURE":"15","TESTING":"20","DEVOPS":"10"}',
 '["언어","프레임워크","데이터베이스","테스트","배포"]', '2026-08-24T00:00:00Z'),
('32000000-0000-0000-0000-000000000102', 'frontend', 'career-v2', 'ACTIVE', '접근 가능하고 유지보수 가능한 웹 사용자 경험을 구현합니다.',
 '["TypeScript","React","React Query","CSS"]', '["컴포넌트 구조","상태 관리","접근성","테스트"]',
 '["성능","디자인 시스템","E2E 테스트"]', '["LANGUAGE","FRAMEWORK","TESTING","DOCUMENTATION"]',
 '{"LANGUAGE":"30","FRAMEWORK":"30","TESTING":"20","DOCUMENTATION":"20"}',
 '["TypeScript","React","상태와 데이터","테스트","포트폴리오 완성도"]', '2026-08-24T00:00:00Z');

UPDATE careers SET active_profile_version_id = '32000000-0000-0000-0000-000000000101' WHERE career_id = 'backend';
UPDATE careers SET active_profile_version_id = '32000000-0000-0000-0000-000000000102' WHERE career_id = 'frontend';

CREATE TABLE career_readiness_policies (
    career_readiness_policy_id UUID PRIMARY KEY,
    version_label VARCHAR(32) NOT NULL UNIQUE,
    expected_minimum NUMERIC(5,2) NOT NULL CHECK (expected_minimum = 60),
    developing_minimum NUMERIC(5,2) NOT NULL CHECK (developing_minimum = 40),
    strong_minimum NUMERIC(5,2) NOT NULL CHECK (strong_minimum = 80),
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'DEPRECATED')),
    effective_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE career_readiness_weights (
    career_readiness_policy_id UUID NOT NULL REFERENCES career_readiness_policies(career_readiness_policy_id),
    career_profile_version_id UUID NOT NULL REFERENCES career_profile_versions(career_profile_version_id),
    category VARCHAR(32) NOT NULL,
    weight NUMERIC(5,2) NOT NULL CHECK (weight > 0 AND weight <= 100),
    PRIMARY KEY (career_readiness_policy_id, career_profile_version_id, category)
);

CREATE TABLE career_readiness_assessments (
    career_readiness_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    skill_matrix_id UUID NOT NULL REFERENCES skill_matrices(skill_matrix_id),
    career_profile_version_id UUID NOT NULL REFERENCES career_profile_versions(career_profile_version_id),
    career_readiness_policy_id UUID NOT NULL REFERENCES career_readiness_policies(career_readiness_policy_id),
    status VARCHAR(32) NOT NULL CHECK (status IN ('COMPLETED', 'INSUFFICIENT_EVIDENCE')),
    readiness_score NUMERIC(5,2) CHECK (readiness_score BETWEEN 0 AND 100),
    readiness_level VARCHAR(24) CHECK (readiness_level IN ('NONE', 'BEGINNER', 'DEVELOPING', 'COMPETENT', 'STRONG')),
    confidence NUMERIC(5,2) NOT NULL CHECK (confidence BETWEEN 0 AND 100),
    rule_set_version VARCHAR(32) NOT NULL,
    unavailable_categories JSONB NOT NULL,
    assessed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_career_readiness_result CHECK (
        (status = 'COMPLETED' AND readiness_score IS NOT NULL AND readiness_level IS NOT NULL)
        OR (status = 'INSUFFICIENT_EVIDENCE' AND readiness_score IS NULL AND readiness_level IS NULL)
    ),
    UNIQUE (user_id, skill_matrix_id, career_profile_version_id, career_readiness_policy_id)
);

CREATE INDEX idx_career_readiness_owner_time
    ON career_readiness_assessments(user_id, assessed_at DESC, career_readiness_id DESC);

CREATE TABLE skill_gaps (
    skill_gap_id UUID PRIMARY KEY,
    career_readiness_id UUID NOT NULL REFERENCES career_readiness_assessments(career_readiness_id) ON DELETE CASCADE,
    skill_assessment_id UUID NOT NULL REFERENCES skill_assessments(skill_assessment_id),
    skill_id UUID NOT NULL REFERENCES skills(skill_id),
    skill_key VARCHAR(64) NOT NULL,
    category VARCHAR(32) NOT NULL,
    actual_score NUMERIC(5,2) NOT NULL CHECK (actual_score BETWEEN 0 AND 100),
    actual_level VARCHAR(24) NOT NULL,
    expected_minimum NUMERIC(5,2) NOT NULL CHECK (expected_minimum = 60),
    gap_state VARCHAR(24) NOT NULL CHECK (gap_state IN ('MISSING', 'WEAK', 'PARTIAL', 'SUFFICIENT', 'STRONG')),
    career_weight NUMERIC(5,2) NOT NULL CHECK (career_weight > 0 AND career_weight <= 100),
    UNIQUE (career_readiness_id, category)
);

CREATE TABLE skill_gap_evidence_links (
    skill_gap_id UUID NOT NULL REFERENCES skill_gaps(skill_gap_id) ON DELETE CASCADE,
    evidence_id UUID NOT NULL REFERENCES evidence_records(evidence_id) ON DELETE CASCADE,
    PRIMARY KEY (skill_gap_id, evidence_id)
);

INSERT INTO career_readiness_policies (
    career_readiness_policy_id, version_label, expected_minimum, developing_minimum, strong_minimum,
    status, effective_at, created_at
) VALUES (
    '33000000-0000-0000-0000-000000000001', 'readiness-v1', 60, 40, 80,
    'ACTIVE', '2026-08-24T00:00:00Z', '2026-08-24T00:00:00Z'
);

INSERT INTO career_readiness_weights (
    career_readiness_policy_id, career_profile_version_id, category, weight
) VALUES
('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000101', 'LANGUAGE', 15),
('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000101', 'FRAMEWORK', 20),
('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000101', 'DATABASE', 20),
('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000101', 'ARCHITECTURE', 15),
('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000101', 'TESTING', 20),
('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000101', 'DEVOPS', 10),
('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000102', 'LANGUAGE', 30),
('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000102', 'FRAMEWORK', 30),
('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000102', 'TESTING', 20),
('33000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000102', 'DOCUMENTATION', 20);
