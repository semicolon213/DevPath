ALTER TABLE skill_matrix_policies
    ADD COLUMN evidence_min_count INTEGER,
    ADD COLUMN evidence_confidence_threshold NUMERIC(5,2),
    ADD CONSTRAINT chk_skill_matrix_evidence_thresholds CHECK (
        (evidence_min_count IS NULL AND evidence_confidence_threshold IS NULL)
        OR (evidence_min_count IS NOT NULL AND evidence_confidence_threshold IS NOT NULL
            AND evidence_min_count > 0 AND evidence_confidence_threshold BETWEEN 0 AND 100)
    );

ALTER TABLE skill_matrix_policies
    DROP CONSTRAINT skill_matrix_policies_rule_set_version_id_key;

CREATE UNIQUE INDEX uq_skill_matrix_policy_active_rule_set
    ON skill_matrix_policies(rule_set_version_id) WHERE status = 'ACTIVE';

ALTER TABLE skill_assessments
    ADD COLUMN evidence_status VARCHAR(24),
    ADD CONSTRAINT chk_skill_assessment_evidence_status CHECK (
        evidence_status IN ('NONE', 'INSUFFICIENT', 'SUFFICIENT')
    );

UPDATE skill_matrix_policies
SET status = 'SUPERSEDED'
WHERE skill_matrix_policy_id = '21000000-0000-0000-0000-000000000002';

INSERT INTO skill_matrix_policies (
    skill_matrix_policy_id, rule_set_version_id, version_label, beginner_minimum, developing_minimum,
    competent_minimum, strong_minimum, weakness_maximum, strength_minimum,
    evidence_min_count, evidence_confidence_threshold, status, created_at
)
SELECT
    '21000000-0000-0000-0000-000000000003', rule_set_version_id, 'skill-matrix-v3', beginner_minimum,
    developing_minimum, competent_minimum, strong_minimum, weakness_maximum, strength_minimum,
    2, 60, 'ACTIVE', '2026-10-19T00:00:00Z'
FROM skill_matrix_policies
WHERE skill_matrix_policy_id = '21000000-0000-0000-0000-000000000002';

INSERT INTO skill_policy_mappings (
    skill_policy_mapping_id, skill_matrix_policy_id, skill_id, source_category, enabled
)
SELECT
    CASE source_category
        WHEN 'ACTIVITY' THEN '22000000-0000-0000-0000-000000000021'::uuid
        WHEN 'ARCHITECTURE' THEN '22000000-0000-0000-0000-000000000022'::uuid
        WHEN 'DATABASE' THEN '22000000-0000-0000-0000-000000000023'::uuid
        WHEN 'DEVOPS' THEN '22000000-0000-0000-0000-000000000024'::uuid
        WHEN 'DOCUMENTATION' THEN '22000000-0000-0000-0000-000000000025'::uuid
        WHEN 'FRAMEWORK' THEN '22000000-0000-0000-0000-000000000026'::uuid
        WHEN 'LANGUAGE' THEN '22000000-0000-0000-0000-000000000027'::uuid
        WHEN 'TESTING' THEN '22000000-0000-0000-0000-000000000028'::uuid
    END,
    '21000000-0000-0000-0000-000000000003', skill_id, source_category, enabled
FROM skill_policy_mappings
WHERE skill_matrix_policy_id = '21000000-0000-0000-0000-000000000002';
