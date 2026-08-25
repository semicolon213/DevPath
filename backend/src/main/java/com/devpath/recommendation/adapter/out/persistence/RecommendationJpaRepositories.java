package com.devpath.recommendation.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RecommendationPolicyJpaRepository extends JpaRepository<RecommendationPolicyJpaEntity, UUID>{Optional<RecommendationPolicyJpaEntity> findByStatus(String status);}
interface RecommendationTemplateJpaRepository extends JpaRepository<RecommendationTemplateJpaEntity, RecommendationTemplateId>{List<RecommendationTemplateJpaEntity> findAllByPolicyIdAndCareerProfileVersionId(UUID policyId, UUID profileId);}
interface RecommendationSetJpaRepository extends JpaRepository<RecommendationSetJpaEntity, UUID>{
    Optional<RecommendationSetJpaEntity> findByUserIdAndReadinessIdAndPolicyId(UUID userId, UUID readinessId, UUID policyId);
    Optional<RecommendationSetJpaEntity> findFirstByUserIdOrderByGeneratedAtDescIdDesc(UUID userId);
    List<RecommendationSetJpaEntity> findAllByUserIdAndStatus(UUID userId, String status);
    Optional<RecommendationSetJpaEntity> findByIdAndUserId(UUID id, UUID userId);
}
interface RecommendationJpaRepository extends JpaRepository<RecommendationJpaEntity, UUID>{List<RecommendationJpaEntity> findAllBySetIdOrderByPosition(UUID setId);}
interface RecommendationEvidenceJpaRepository extends JpaRepository<RecommendationEvidenceJpaEntity, RecommendationEvidenceId>{List<RecommendationEvidenceJpaEntity> findAllByRecommendationIdIn(List<UUID> ids);}
interface RoadmapPolicyJpaRepository extends JpaRepository<RoadmapPolicyJpaEntity, UUID>{Optional<RoadmapPolicyJpaEntity> findByStatusAndRecommendationPolicyId(String status, UUID policyId);}
interface LearningRoadmapJpaRepository extends JpaRepository<LearningRoadmapJpaEntity, UUID>{
    Optional<LearningRoadmapJpaEntity> findByUserIdAndSetIdAndPolicyId(UUID userId, UUID setId, UUID policyId);
    Optional<LearningRoadmapJpaEntity> findFirstByUserIdAndStatusInOrderByGeneratedAtDescIdDesc(UUID userId, List<String> statuses);
    Optional<LearningRoadmapJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    List<LearningRoadmapJpaEntity> findAllByUserIdAndStatusIn(UUID userId, List<String> statuses);
}
interface RoadmapMilestoneJpaRepository extends JpaRepository<RoadmapMilestoneJpaEntity, UUID>{List<RoadmapMilestoneJpaEntity> findAllByRoadmapIdOrderByPosition(UUID roadmapId);}
interface RoadmapStepJpaRepository extends JpaRepository<RoadmapStepJpaEntity, UUID>{List<RoadmapStepJpaEntity> findAllByRoadmapIdOrderByPosition(UUID roadmapId);}
