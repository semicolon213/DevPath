package com.devpath.learning.domain;

import com.devpath.recommendation.domain.Recommendation;
import com.devpath.recommendation.domain.RecommendationSet;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class DeterministicRoadmapEngine {
    public LearningRoadmap generate(UUID roadmapId, RecommendationSet set, RoadmapPolicy policy, Instant now) {
        if (!set.policyId().equals(policy.recommendationPolicyId())) {
            throw new IllegalArgumentException("roadmap policy does not match recommendation policy");
        }
        List<RoadmapMilestone> milestones = new ArrayList<>();
        List<RoadmapStep> steps = new ArrayList<>();
        UUID previousStep = null;
        List<Recommendation> ordered = set.recommendations().stream().sorted(
            Comparator.comparingInt((Recommendation value) -> policy.categoryOrder().getOrDefault(value.category(), Integer.MAX_VALUE))
                .thenComparing(value -> value.category().name())).toList();
        for (int position = 0; position < ordered.size(); position++) {
            Recommendation recommendation = ordered.get(position);
            UUID milestoneId = stableId(roadmapId + ":milestone:" + recommendation.recommendationId());
            UUID stepId = stableId(roadmapId + ":step:" + recommendation.recommendationId());
            milestones.add(new RoadmapMilestone(milestoneId, position, recommendation.category(),
                recommendation.title(), "PLANNED"));
            steps.add(new RoadmapStep(stepId, milestoneId, recommendation.recommendationId(), position,
                recommendation.category(), recommendation.title(), difficulty(recommendation.effortHours()),
                recommendation.effortHours(), previousStep == null ? List.of() : List.of(previousStep),
                recommendation.completionCriteria(), recommendation.expectedEvidence(), "NOT_STARTED"));
            previousStep = stepId;
        }
        return new LearningRoadmap(roadmapId, set.userId(), set.recommendationSetId(), policy.policyId(),
            policy.versionLabel(), "CREATED", BigDecimal.ZERO.setScale(2), milestones, steps, now, now);
    }

    private String difficulty(int effortHours) {
        if (effortHours <= 12) return "BEGINNER";
        if (effortHours <= 24) return "INTERMEDIATE";
        return "ADVANCED";
    }

    private UUID stableId(String basis) { return UUID.nameUUIDFromBytes(basis.getBytes(StandardCharsets.UTF_8)); }
}
