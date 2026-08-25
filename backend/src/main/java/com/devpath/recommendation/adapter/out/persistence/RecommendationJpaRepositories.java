package com.devpath.recommendation.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RecommendationPolicyJpaRepository extends JpaRepository<RecommendationPolicyJpaEntity, UUID>{Optional<RecommendationPolicyJpaEntity> findByStatus(String status);}
interface RecommendationTemplateJpaRepository extends JpaRepository<RecommendationTemplateJpaEntity, RecommendationTemplateId>{List<RecommendationTemplateJpaEntity> findAllByPolicyIdAndCareerProfileVersionId(UUID policyId, UUID profileId);}
interface RecommendationSetJpaRepository extends JpaRepository<RecommendationSetJpaEntity, UUID>{
    Optional<RecommendationSetJpaEntity> findByUserIdAndReadinessIdAndPolicyId(UUID userId, UUID readinessId, UUID policyId);
    Optional<RecommendationSetJpaEntity> findFirstByUserIdOrderByGeneratedAtDescIdDesc(UUID userId);
    List<RecommendationSetJpaEntity> findAllByUserIdAndStatus(UUID userId, String status);
    Optional<RecommendationSetJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    List<RecommendationSetJpaEntity> findAllByUserIdOrderByGeneratedAtDescIdDesc(UUID userId);
    @Query("select s from RecommendationSetJpaEntity s, RecommendationJpaEntity r where r.setId=s.id and r.id=:recommendationId and s.userId=:userId")
    Optional<RecommendationSetJpaEntity> findByRecommendationIdAndUserId(@Param("recommendationId") UUID recommendationId,@Param("userId") UUID userId);
}
interface RecommendationJpaRepository extends JpaRepository<RecommendationJpaEntity, UUID>{List<RecommendationJpaEntity> findAllBySetIdOrderByPosition(UUID setId);}
interface RecommendationEvidenceJpaRepository extends JpaRepository<RecommendationEvidenceJpaEntity, RecommendationEvidenceId>{
    List<RecommendationEvidenceJpaEntity> findAllByRecommendationIdIn(List<UUID> ids);
    @Query(value="SELECT e.evidence_id AS evidenceId,e.evidence_type AS evidenceType,e.source_reference AS sourceReference,e.observed_fact_summary AS observedFactSummary,e.confidence AS confidence,e.created_at AS createdAt FROM recommendation_evidence_links l JOIN recommendations r ON r.recommendation_id=l.recommendation_id JOIN recommendation_sets s ON s.recommendation_set_id=r.recommendation_set_id JOIN evidence_records e ON e.evidence_id=l.evidence_id AND e.user_id=s.user_id WHERE l.recommendation_id=:recommendationId AND s.user_id=:userId ORDER BY e.created_at DESC,e.evidence_id",nativeQuery=true)
    List<RecommendationEvidenceProjection> findEvidenceByRecommendationAndOwner(@Param("recommendationId") UUID recommendationId,@Param("userId") UUID userId);
}
interface RecommendationEvidenceProjection {
    UUID getEvidenceId();String getEvidenceType();String getSourceReference();String getObservedFactSummary();
    java.math.BigDecimal getConfidence();java.time.Instant getCreatedAt();
}
interface RoadmapPolicyJpaRepository extends JpaRepository<RoadmapPolicyJpaEntity, UUID>{Optional<RoadmapPolicyJpaEntity> findByStatusAndRecommendationPolicyId(String status, UUID policyId);}
interface LearningRoadmapJpaRepository extends JpaRepository<LearningRoadmapJpaEntity, UUID>{
    Optional<LearningRoadmapJpaEntity> findByUserIdAndSetIdAndPolicyId(UUID userId, UUID setId, UUID policyId);
    Optional<LearningRoadmapJpaEntity> findFirstByUserIdAndStatusInOrderByGeneratedAtDescIdDesc(UUID userId, List<String> statuses);
    Optional<LearningRoadmapJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    List<LearningRoadmapJpaEntity> findAllByUserIdOrderByGeneratedAtDescIdDesc(UUID userId);
    List<LearningRoadmapJpaEntity> findAllByUserIdAndStatusIn(UUID userId, List<String> statuses);
}
interface RoadmapMilestoneJpaRepository extends JpaRepository<RoadmapMilestoneJpaEntity, UUID>{List<RoadmapMilestoneJpaEntity> findAllByRoadmapIdOrderByPosition(UUID roadmapId);}
interface RoadmapStepJpaRepository extends JpaRepository<RoadmapStepJpaEntity, UUID>{List<RoadmapStepJpaEntity> findAllByRoadmapIdOrderByPosition(UUID roadmapId);}
