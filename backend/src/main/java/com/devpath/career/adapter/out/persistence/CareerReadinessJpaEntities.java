package com.devpath.career.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "career_readiness_policies")
class CareerReadinessPolicyJpaEntity {
    @Id @Column(name = "career_readiness_policy_id") UUID id;
    @Column(name = "version_label", nullable = false) String versionLabel;
    @Column(name = "expected_minimum", nullable = false) BigDecimal expectedMinimum;
    @Column(name = "developing_minimum", nullable = false) BigDecimal developingMinimum;
    @Column(name = "strong_minimum", nullable = false) BigDecimal strongMinimum;
    @Column(name = "status", nullable = false) String status;
    @Column(name = "effective_at", nullable = false) Instant effectiveAt;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    protected CareerReadinessPolicyJpaEntity() {}
}

@Entity
@Table(name = "career_readiness_weights")
@IdClass(CareerReadinessWeightId.class)
class CareerReadinessWeightJpaEntity {
    @Id @Column(name = "career_readiness_policy_id") UUID policyId;
    @Id @Column(name = "career_profile_version_id") UUID careerProfileVersionId;
    @Id @Column(name = "category") String category;
    @Column(name = "weight", nullable = false) BigDecimal weight;
    protected CareerReadinessWeightJpaEntity() {}
}

@Entity
@Table(name = "career_readiness_assessments")
class CareerReadinessJpaEntity {
    @Id @Column(name = "career_readiness_id") UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "skill_matrix_id", nullable = false) UUID skillMatrixId;
    @Column(name = "career_profile_version_id", nullable = false) UUID careerProfileVersionId;
    @Column(name = "career_readiness_policy_id", nullable = false) UUID policyId;
    @Column(name = "status", nullable = false) String status;
    @Column(name = "readiness_score") BigDecimal readinessScore;
    @Column(name = "readiness_level") String readinessLevel;
    @Column(name = "confidence", nullable = false) BigDecimal confidence;
    @Column(name = "rule_set_version", nullable = false) String ruleSetVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "unavailable_categories", columnDefinition = "jsonb", nullable = false)
    List<String> unavailableCategories = new ArrayList<>();
    @Column(name = "assessed_at", nullable = false) Instant assessedAt;
    protected CareerReadinessJpaEntity() {}
}

@Entity
@Table(name = "skill_gaps")
class SkillGapJpaEntity {
    @Id @Column(name = "skill_gap_id") UUID id;
    @Column(name = "career_readiness_id", nullable = false) UUID readinessId;
    @Column(name = "skill_assessment_id", nullable = false) UUID skillAssessmentId;
    @Column(name = "skill_id", nullable = false) UUID skillId;
    @Column(name = "skill_key", nullable = false) String skillKey;
    @Column(name = "category", nullable = false) String category;
    @Column(name = "actual_score", nullable = false) BigDecimal actualScore;
    @Column(name = "actual_level", nullable = false) String actualLevel;
    @Column(name = "expected_minimum", nullable = false) BigDecimal expectedMinimum;
    @Column(name = "gap_state", nullable = false) String gapState;
    @Column(name = "career_weight", nullable = false) BigDecimal careerWeight;
    protected SkillGapJpaEntity() {}
}

@Entity
@Table(name = "skill_gap_evidence_links")
@IdClass(SkillGapEvidenceId.class)
class SkillGapEvidenceJpaEntity {
    @Id @Column(name = "skill_gap_id") UUID gapId;
    @Id @Column(name = "evidence_id") UUID evidenceId;
    protected SkillGapEvidenceJpaEntity() {}
    SkillGapEvidenceJpaEntity(UUID gapId, UUID evidenceId) { this.gapId = gapId; this.evidenceId = evidenceId; }
}
