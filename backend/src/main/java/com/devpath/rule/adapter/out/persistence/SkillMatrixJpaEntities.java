package com.devpath.rule.adapter.out.persistence;

import com.devpath.rule.domain.SkillAssessment;
import com.devpath.rule.domain.SkillMatrix;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "skills")
class SkillJpaEntity {
    @Id @Column(name = "skill_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "stable_key", nullable = false, updatable = false, length = 64) private String stableKey;
    @Column(name = "name", nullable = false, length = 120) private String name;
    @Column(name = "category", nullable = false, length = 32) private String category;
    @Column(name = "status", nullable = false, length = 24) private String status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected SkillJpaEntity() {}
    UUID id() { return id; } String stableKey() { return stableKey; } String name() { return name; } String category() { return category; }
}

@Entity
@Table(name = "skill_matrix_policies")
class SkillMatrixPolicyJpaEntity {
    @Id @Column(name = "skill_matrix_policy_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "rule_set_version_id", nullable = false, updatable = false) private UUID ruleSetVersionId;
    @Column(name = "version_label", nullable = false, updatable = false, length = 32) private String versionLabel;
    @Column(name = "beginner_minimum", nullable = false, precision = 5, scale = 2) private BigDecimal beginnerMinimum;
    @Column(name = "developing_minimum", nullable = false, precision = 5, scale = 2) private BigDecimal developingMinimum;
    @Column(name = "competent_minimum", nullable = false, precision = 5, scale = 2) private BigDecimal competentMinimum;
    @Column(name = "strong_minimum", nullable = false, precision = 5, scale = 2) private BigDecimal strongMinimum;
    @Column(name = "weakness_maximum", nullable = false, precision = 5, scale = 2) private BigDecimal weaknessMaximum;
    @Column(name = "strength_minimum", nullable = false, precision = 5, scale = 2) private BigDecimal strengthMinimum;
    @Column(name = "status", nullable = false, length = 24) private String status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected SkillMatrixPolicyJpaEntity() {}
    UUID id() { return id; } UUID ruleSetVersionId() { return ruleSetVersionId; } String versionLabel() { return versionLabel; }
    BigDecimal beginnerMinimum() { return beginnerMinimum; } BigDecimal developingMinimum() { return developingMinimum; }
    BigDecimal competentMinimum() { return competentMinimum; } BigDecimal strongMinimum() { return strongMinimum; }
    BigDecimal weaknessMaximum() { return weaknessMaximum; } BigDecimal strengthMinimum() { return strengthMinimum; }
}

@Entity
@Table(name = "skill_policy_mappings")
class SkillPolicyMappingJpaEntity {
    @Id @Column(name = "skill_policy_mapping_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "skill_matrix_policy_id", nullable = false, updatable = false) private UUID policyId;
    @Column(name = "skill_id", nullable = false, updatable = false) private UUID skillId;
    @Column(name = "source_category", nullable = false, updatable = false, length = 32) private String sourceCategory;
    @Column(name = "enabled", nullable = false) private boolean enabled;
    protected SkillPolicyMappingJpaEntity() {}
    UUID skillId() { return skillId; } String sourceCategory() { return sourceCategory; }
}

@Entity
@Table(name = "skill_matrices")
class SkillMatrixJpaEntity {
    @Id @Column(name = "skill_matrix_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "evaluation_id", nullable = false, updatable = false) private UUID evaluationId;
    @Column(name = "skill_matrix_policy_id", nullable = false, updatable = false) private UUID policyId;
    @Column(name = "policy_version", nullable = false, updatable = false, length = 32) private String policyVersion;
    @Column(name = "rule_set_version", nullable = false, updatable = false, length = 32) private String ruleSetVersion;
    @Column(name = "status", nullable = false, length = 24) private String status;
    @Column(name = "generated_at", nullable = false, updatable = false) private Instant generatedAt;
    @Version @Column(name = "version", nullable = false) private long version;
    protected SkillMatrixJpaEntity() {}
    SkillMatrixJpaEntity(SkillMatrix value) {
        id = value.matrixId(); userId = value.userId(); evaluationId = value.evaluationId(); policyId = value.policyId();
        policyVersion = value.policyVersion(); ruleSetVersion = value.ruleSetVersion(); status = value.status();
        generatedAt = value.generatedAt(); version = 0;
    }
    void supersede() { if ("CURRENT".equals(status)) status = "SUPERSEDED"; }
    UUID id() { return id; } UUID userId() { return userId; } UUID evaluationId() { return evaluationId; }
    UUID policyId() { return policyId; } String policyVersion() { return policyVersion; }
    String ruleSetVersion() { return ruleSetVersion; } String status() { return status; } Instant generatedAt() { return generatedAt; }
}

@Entity
@Table(name = "skill_assessments")
class SkillAssessmentJpaEntity {
    @Id @Column(name = "skill_assessment_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "skill_matrix_id", nullable = false, updatable = false) private UUID matrixId;
    @Column(name = "skill_id", nullable = false, updatable = false) private UUID skillId;
    @Column(name = "score", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal score;
    @Column(name = "skill_level", nullable = false, updatable = false, length = 24) private String level;
    @Column(name = "confidence", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal confidence;
    @Column(name = "strength_flag", nullable = false, updatable = false) private boolean strength;
    @Column(name = "weakness_flag", nullable = false, updatable = false) private boolean weakness;
    @Column(name = "growth_trend", nullable = false, updatable = false, length = 24) private String growthTrend;
    @Column(name = "aggregate_rule_result_reference", nullable = false, updatable = false, length = 160) private String aggregateReference;
    @Column(name = "rule_set_version", nullable = false, updatable = false, length = 32) private String ruleSetVersion;
    protected SkillAssessmentJpaEntity() {}
    SkillAssessmentJpaEntity(UUID matrixId, SkillAssessment value) {
        id = value.assessmentId(); this.matrixId = matrixId; skillId = value.skill().skillId(); score = value.score();
        level = value.level().name(); confidence = value.confidence(); strength = value.strength(); weakness = value.weakness();
        growthTrend = value.growthTrend(); aggregateReference = value.aggregateRuleResultReference(); ruleSetVersion = value.ruleSetVersion();
    }
    UUID id() { return id; } UUID skillId() { return skillId; } BigDecimal score() { return score; } String level() { return level; }
    BigDecimal confidence() { return confidence; } boolean strength() { return strength; } boolean weakness() { return weakness; }
    String growthTrend() { return growthTrend; } String aggregateReference() { return aggregateReference; } String ruleSetVersion() { return ruleSetVersion; }
}

@Entity
@Table(name = "skill_evidence_links")
class SkillEvidenceLinkJpaEntity {
    @Id @Column(name = "skill_evidence_link_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "skill_assessment_id", nullable = false, updatable = false) private UUID assessmentId;
    @Column(name = "evidence_id", nullable = false, updatable = false) private UUID evidenceId;
    @Column(name = "evidence_strength", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal strength;
    @Column(name = "source_role", nullable = false, updatable = false, length = 32) private String sourceRole;
    protected SkillEvidenceLinkJpaEntity() {}
    SkillEvidenceLinkJpaEntity(UUID assessmentId, UUID evidenceId, BigDecimal strength) {
        id = UUID.randomUUID(); this.assessmentId = assessmentId; this.evidenceId = evidenceId;
        this.strength = strength; sourceRole = "DIRECT";
    }
    UUID evidenceId() { return evidenceId; }
}

@Entity
@Table(name = "skill_repository_links")
class SkillRepositoryLinkJpaEntity {
    @Id @Column(name = "skill_repository_link_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "skill_assessment_id", nullable = false, updatable = false) private UUID assessmentId;
    @Column(name = "repository_id", nullable = false, updatable = false) private UUID repositoryId;
    protected SkillRepositoryLinkJpaEntity() {}
    SkillRepositoryLinkJpaEntity(UUID assessmentId, UUID repositoryId) {
        id = UUID.randomUUID(); this.assessmentId = assessmentId; this.repositoryId = repositoryId;
    }
    UUID repositoryId() { return repositoryId; }
}

@Entity
@Table(name = "skill_assessment_facts")
class SkillAssessmentFactJpaEntity {
    @Id @Column(name = "skill_assessment_fact_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "skill_assessment_id", nullable = false, updatable = false) private UUID assessmentId;
    @Column(name = "fact_order", nullable = false, updatable = false) private int orderIndex;
    @Column(name = "fact_value", nullable = false, updatable = false, length = 160) private String value;
    protected SkillAssessmentFactJpaEntity() {}
    SkillAssessmentFactJpaEntity(UUID assessmentId, int orderIndex, String value) {
        id = UUID.randomUUID(); this.assessmentId = assessmentId; this.orderIndex = orderIndex; this.value = value;
    }
    String value() { return value; }
}
