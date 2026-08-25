CREATE TABLE recommendation_policies (
    recommendation_policy_id UUID PRIMARY KEY,
    version_label VARCHAR(32) NOT NULL UNIQUE,
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'DEPRECATED')),
    effective_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE recommendation_policy_templates (
    recommendation_policy_id UUID NOT NULL REFERENCES recommendation_policies(recommendation_policy_id),
    career_profile_version_id UUID NOT NULL REFERENCES career_profile_versions(career_profile_version_id),
    category VARCHAR(32) NOT NULL,
    recommendation_type VARCHAR(24) NOT NULL CHECK (recommendation_type IN ('STUDY', 'PROJECT', 'ARCHITECTURE', 'PORTFOLIO')),
    prerequisite_order INTEGER NOT NULL CHECK (prerequisite_order >= 0),
    effort_hours INTEGER NOT NULL CHECK (effort_hours > 0),
    title VARCHAR(160) NOT NULL,
    rationale_code VARCHAR(64) NOT NULL,
    completion_criteria VARCHAR(500) NOT NULL,
    expected_evidence JSONB NOT NULL,
    PRIMARY KEY (recommendation_policy_id, career_profile_version_id, category),
    UNIQUE (recommendation_policy_id, career_profile_version_id, prerequisite_order)
);

CREATE TABLE recommendation_sets (
    recommendation_set_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    career_readiness_id UUID NOT NULL REFERENCES career_readiness_assessments(career_readiness_id),
    recommendation_policy_id UUID NOT NULL REFERENCES recommendation_policies(recommendation_policy_id),
    status VARCHAR(24) NOT NULL CHECK (status IN ('PUBLISHED', 'SUPERSEDED')),
    generated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, career_readiness_id, recommendation_policy_id)
);

CREATE INDEX idx_recommendation_sets_owner_time
    ON recommendation_sets(user_id, generated_at DESC, recommendation_set_id DESC);

CREATE TABLE recommendations (
    recommendation_id UUID PRIMARY KEY,
    recommendation_set_id UUID NOT NULL REFERENCES recommendation_sets(recommendation_set_id) ON DELETE CASCADE,
    skill_gap_id UUID NOT NULL REFERENCES skill_gaps(skill_gap_id),
    category VARCHAR(32) NOT NULL,
    recommendation_type VARCHAR(24) NOT NULL CHECK (recommendation_type IN ('STUDY', 'PROJECT', 'ARCHITECTURE', 'PORTFOLIO')),
    priority VARCHAR(16) NOT NULL CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    rationale_code VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    completion_criteria VARCHAR(500) NOT NULL,
    expected_evidence JSONB NOT NULL,
    effort_hours INTEGER NOT NULL CHECK (effort_hours > 0),
    position INTEGER NOT NULL CHECK (position >= 0),
    status VARCHAR(24) NOT NULL CHECK (status IN ('PROPOSED', 'ACCEPTED', 'DISMISSED', 'COMPLETED')),
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (recommendation_set_id, skill_gap_id),
    UNIQUE (recommendation_set_id, position)
);

CREATE TABLE recommendation_evidence_links (
    recommendation_id UUID NOT NULL REFERENCES recommendations(recommendation_id) ON DELETE CASCADE,
    evidence_id UUID NOT NULL REFERENCES evidence_records(evidence_id) ON DELETE CASCADE,
    PRIMARY KEY (recommendation_id, evidence_id)
);

CREATE TABLE roadmap_policies (
    roadmap_policy_id UUID PRIMARY KEY,
    version_label VARCHAR(32) NOT NULL UNIQUE,
    recommendation_policy_id UUID NOT NULL UNIQUE REFERENCES recommendation_policies(recommendation_policy_id),
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'DEPRECATED')),
    effective_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE learning_roadmaps (
    roadmap_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    recommendation_set_id UUID NOT NULL REFERENCES recommendation_sets(recommendation_set_id),
    roadmap_policy_id UUID NOT NULL REFERENCES roadmap_policies(roadmap_policy_id),
    status VARCHAR(24) NOT NULL CHECK (status IN ('CREATED', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED')),
    progress_percent NUMERIC(5,2) NOT NULL CHECK (progress_percent BETWEEN 0 AND 100),
    generated_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (user_id, recommendation_set_id, roadmap_policy_id)
);

CREATE UNIQUE INDEX uq_learning_roadmaps_active_owner
    ON learning_roadmaps(user_id) WHERE status IN ('CREATED', 'IN_PROGRESS');

CREATE TABLE roadmap_milestones (
    milestone_id UUID PRIMARY KEY,
    roadmap_id UUID NOT NULL REFERENCES learning_roadmaps(roadmap_id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position >= 0),
    category VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('PLANNED', 'ACHIEVED', 'SKIPPED')),
    UNIQUE (roadmap_id, position),
    UNIQUE (roadmap_id, category)
);

CREATE TABLE roadmap_steps (
    roadmap_step_id UUID PRIMARY KEY,
    roadmap_id UUID NOT NULL REFERENCES learning_roadmaps(roadmap_id) ON DELETE CASCADE,
    milestone_id UUID NOT NULL REFERENCES roadmap_milestones(milestone_id) ON DELETE CASCADE,
    recommendation_id UUID NOT NULL REFERENCES recommendations(recommendation_id),
    position INTEGER NOT NULL CHECK (position >= 0),
    category VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    difficulty VARCHAR(24) NOT NULL CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    effort_hours INTEGER NOT NULL CHECK (effort_hours > 0),
    prerequisite_step_ids JSONB NOT NULL,
    completion_criteria VARCHAR(500) NOT NULL,
    expected_evidence JSONB NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED')),
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (roadmap_id, recommendation_id),
    UNIQUE (roadmap_id, position)
);

INSERT INTO recommendation_policies (
    recommendation_policy_id, version_label, status, effective_at, created_at
) VALUES (
    '41000000-0000-0000-0000-000000000001', 'recommendation-v1', 'ACTIVE',
    '2026-08-25T00:00:00Z', '2026-08-25T00:00:00Z'
);

INSERT INTO roadmap_policies (
    roadmap_policy_id, version_label, recommendation_policy_id, status, effective_at, created_at
) VALUES (
    '42000000-0000-0000-0000-000000000001', 'roadmap-v1',
    '41000000-0000-0000-0000-000000000001', 'ACTIVE',
    '2026-08-25T00:00:00Z', '2026-08-25T00:00:00Z'
);

INSERT INTO recommendation_policy_templates (
    recommendation_policy_id, career_profile_version_id, category, recommendation_type,
    prerequisite_order, effort_hours, title, rationale_code, completion_criteria, expected_evidence
) VALUES
('41000000-0000-0000-0000-000000000001','32000000-0000-0000-0000-000000000101','LANGUAGE','STUDY',0,12,'Strengthen backend language evidence','CAREER_REQUIRED_GAP','Reach an official Language score of at least 60 in a later analysis.','["source files","language statistics"]'),
('41000000-0000-0000-0000-000000000001','32000000-0000-0000-0000-000000000101','FRAMEWORK','PROJECT',1,20,'Build framework-backed service evidence','CAREER_REQUIRED_GAP','Reach an official Framework score of at least 60 in a later analysis.','["framework dependency declaration","framework source structure"]'),
('41000000-0000-0000-0000-000000000001','32000000-0000-0000-0000-000000000101','DATABASE','PROJECT',2,20,'Add measurable database persistence','CAREER_REQUIRED_GAP','Reach an official Database score of at least 60 in a later analysis.','["database dependency","data-access dependency","migration","persistence configuration"]'),
('41000000-0000-0000-0000-000000000001','32000000-0000-0000-0000-000000000101','ARCHITECTURE','ARCHITECTURE',3,16,'Establish traceable architecture boundaries','CAREER_REQUIRED_GAP','Reach an official Architecture score of at least 60 in a later analysis.','["structured boundaries","module layout","architecture documentation"]'),
('41000000-0000-0000-0000-000000000001','32000000-0000-0000-0000-000000000101','TESTING','PROJECT',4,16,'Add repeatable automated testing','CAREER_REQUIRED_GAP','Reach an official Testing score of at least 60 in a later analysis.','["test files","test framework","CI test workflow"]'),
('41000000-0000-0000-0000-000000000001','32000000-0000-0000-0000-000000000101','DEVOPS','PROJECT',5,16,'Create delivery and deployment evidence','CAREER_REQUIRED_GAP','Reach an official DevOps score of at least 60 in a later analysis.','["container configuration","CI workflow","deployment configuration"]'),
('41000000-0000-0000-0000-000000000001','32000000-0000-0000-0000-000000000102','LANGUAGE','STUDY',0,12,'Strengthen frontend language evidence','CAREER_REQUIRED_GAP','Reach an official Language score of at least 60 in a later analysis.','["TypeScript source files","language statistics"]'),
('41000000-0000-0000-0000-000000000001','32000000-0000-0000-0000-000000000102','FRAMEWORK','PROJECT',1,20,'Build measurable React application evidence','CAREER_REQUIRED_GAP','Reach an official Framework score of at least 60 in a later analysis.','["React dependency declaration","component source structure"]'),
('41000000-0000-0000-0000-000000000001','32000000-0000-0000-0000-000000000102','TESTING','PROJECT',2,16,'Add repeatable frontend testing','CAREER_REQUIRED_GAP','Reach an official Testing score of at least 60 in a later analysis.','["component tests","test framework","CI test workflow"]'),
('41000000-0000-0000-0000-000000000001','32000000-0000-0000-0000-000000000102','DOCUMENTATION','PORTFOLIO',3,8,'Document the frontend project evidence','CAREER_REQUIRED_GAP','Reach an official Documentation score of at least 60 in a later analysis.','["README","API or architecture documentation","usage evidence"]');
