package com.devpath.recommendation.application;

import com.devpath.learning.domain.LearningRoadmap;
import com.devpath.learning.domain.RoadmapPolicy;
import com.devpath.recommendation.domain.RecommendationPolicy;
import com.devpath.recommendation.domain.RecommendationSet;
import java.util.Optional;
import java.util.UUID;

public interface RecommendationPersistencePort {
    RecommendationPolicy loadActivePolicy(UUID careerProfileVersionId);
    RoadmapPolicy loadActiveRoadmapPolicy(UUID recommendationPolicyId, UUID careerProfileVersionId);
    void supersedeCurrent(UUID userId, java.time.Instant now);
    Optional<RecommendationSet> findSetByBasis(UUID userId, UUID readinessId, UUID policyId);
    Optional<RecommendationSet> findCurrentSet(UUID userId);
    Optional<RecommendationSet> findSetByIdAndOwner(UUID setId, UUID userId);
    RecommendationSet saveSet(RecommendationSet set);
    Optional<LearningRoadmap> findRoadmapByBasis(UUID userId, UUID setId, UUID policyId);
    Optional<LearningRoadmap> findActiveRoadmap(UUID userId);
    Optional<LearningRoadmap> findRoadmapByIdAndOwner(UUID roadmapId, UUID userId);
    LearningRoadmap saveRoadmap(LearningRoadmap roadmap);
}
