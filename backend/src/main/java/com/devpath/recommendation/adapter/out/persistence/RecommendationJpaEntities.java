package com.devpath.recommendation.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="recommendation_policies")
class RecommendationPolicyJpaEntity {
    @Id @Column(name="recommendation_policy_id") UUID id;
    @Column(name="version_label") String versionLabel; @Column(name="status") String status;
    @Column(name="effective_at") Instant effectiveAt; @Column(name="created_at") Instant createdAt;
    protected RecommendationPolicyJpaEntity() {}
}
@Entity @Table(name="recommendation_policy_templates") @IdClass(RecommendationTemplateId.class)
class RecommendationTemplateJpaEntity {
    @Id @Column(name="recommendation_policy_id") UUID policyId;
    @Id @Column(name="career_profile_version_id") UUID careerProfileVersionId;
    @Id @Column(name="category") String category;
    @Column(name="recommendation_type") String type; @Column(name="prerequisite_order") int prerequisiteOrder;
    @Column(name="effort_hours") int effortHours; @Column(name="title") String title;
    @Column(name="rationale_code") String rationaleCode; @Column(name="completion_criteria") String completionCriteria;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="expected_evidence", columnDefinition="jsonb") List<String> expectedEvidence = new ArrayList<>();
    protected RecommendationTemplateJpaEntity() {}
}
@Entity @Table(name="recommendation_sets")
class RecommendationSetJpaEntity {
    @Id @Column(name="recommendation_set_id") UUID id; @Column(name="user_id") UUID userId;
    @Column(name="career_readiness_id") UUID readinessId; @Column(name="recommendation_policy_id") UUID policyId;
    @Column(name="status") String status; @Column(name="generated_at") Instant generatedAt;
    protected RecommendationSetJpaEntity() {}
}
@Entity @Table(name="recommendations")
class RecommendationJpaEntity {
    @Id @Column(name="recommendation_id") UUID id; @Column(name="recommendation_set_id") UUID setId;
    @Column(name="skill_gap_id") UUID gapId; @Column(name="category") String category;
    @Column(name="recommendation_type") String type; @Column(name="priority") String priority;
    @Column(name="rationale_code") String rationaleCode; @Column(name="title") String title;
    @Column(name="completion_criteria") String completionCriteria;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="expected_evidence", columnDefinition="jsonb") List<String> expectedEvidence = new ArrayList<>();
    @Column(name="effort_hours") int effortHours; @Column(name="position") int position;
    @Column(name="status") String status; @Column(name="updated_at") Instant updatedAt; @Version long version;
    protected RecommendationJpaEntity() {}
}
@Entity @Table(name="recommendation_evidence_links") @IdClass(RecommendationEvidenceId.class)
class RecommendationEvidenceJpaEntity {
    @Id @Column(name="recommendation_id") UUID recommendationId; @Id @Column(name="evidence_id") UUID evidenceId;
    protected RecommendationEvidenceJpaEntity() {}
    RecommendationEvidenceJpaEntity(UUID recommendationId, UUID evidenceId) { this.recommendationId=recommendationId; this.evidenceId=evidenceId; }
}
@Entity @Table(name="roadmap_policies")
class RoadmapPolicyJpaEntity {
    @Id @Column(name="roadmap_policy_id") UUID id; @Column(name="version_label") String versionLabel;
    @Column(name="recommendation_policy_id") UUID recommendationPolicyId; @Column(name="status") String status;
    @Column(name="effective_at") Instant effectiveAt; @Column(name="created_at") Instant createdAt;
    protected RoadmapPolicyJpaEntity() {}
}
@Entity @Table(name="learning_roadmaps")
class LearningRoadmapJpaEntity {
    @Id @Column(name="roadmap_id") UUID id; @Column(name="user_id") UUID userId;
    @Column(name="recommendation_set_id") UUID setId; @Column(name="roadmap_policy_id") UUID policyId;
    @Column(name="status") String status; @Column(name="progress_percent") BigDecimal progressPercent;
    @Column(name="generated_at") Instant generatedAt; @Column(name="updated_at") Instant updatedAt; @Version long version;
    protected LearningRoadmapJpaEntity() {}
}
@Entity @Table(name="roadmap_milestones")
class RoadmapMilestoneJpaEntity {
    @Id @Column(name="milestone_id") UUID id; @Column(name="roadmap_id") UUID roadmapId;
    @Column(name="position") int position; @Column(name="category") String category;
    @Column(name="title") String title; @Column(name="status") String status;
    protected RoadmapMilestoneJpaEntity() {}
}
@Entity @Table(name="roadmap_steps")
class RoadmapStepJpaEntity {
    @Id @Column(name="roadmap_step_id") UUID id; @Column(name="roadmap_id") UUID roadmapId;
    @Column(name="milestone_id") UUID milestoneId; @Column(name="recommendation_id") UUID recommendationId;
    @Column(name="position") int position; @Column(name="category") String category; @Column(name="title") String title;
    @Column(name="difficulty") String difficulty; @Column(name="effort_hours") int effortHours;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="prerequisite_step_ids", columnDefinition="jsonb") List<UUID> prerequisiteStepIds = new ArrayList<>();
    @Column(name="completion_criteria") String completionCriteria;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="expected_evidence", columnDefinition="jsonb") List<String> expectedEvidence = new ArrayList<>();
    @Column(name="status") String status; @Column(name="updated_at") Instant updatedAt; @Version long version;
    protected RoadmapStepJpaEntity() {}
}
