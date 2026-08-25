package com.devpath.learning.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LearningRoadmap(
    UUID roadmapId,
    UUID userId,
    UUID recommendationSetId,
    UUID policyId,
    String policyVersion,
    String status,
    BigDecimal progressPercent,
    List<RoadmapMilestone> milestones,
    List<RoadmapStep> steps,
    Instant generatedAt,
    Instant updatedAt
) {
    public LearningRoadmap {
        milestones = List.copyOf(milestones); steps = List.copyOf(steps);
    }

    public LearningRoadmap archive(Instant now) {
        return new LearningRoadmap(roadmapId, userId, recommendationSetId, policyId, policyVersion, "ARCHIVED",
            progressPercent, milestones, steps, generatedAt, now);
    }
}
