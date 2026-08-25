package com.devpath.learning.domain;

import com.devpath.rule.domain.RuleCategory;
import java.util.List;
import java.util.UUID;

public record RoadmapStep(
    UUID stepId,
    UUID milestoneId,
    UUID recommendationId,
    int position,
    RuleCategory category,
    String title,
    String difficulty,
    int effortHours,
    List<UUID> prerequisiteStepIds,
    String completionCriteria,
    List<String> expectedEvidence,
    String status
) {
    public RoadmapStep {
        prerequisiteStepIds = List.copyOf(prerequisiteStepIds);
        expectedEvidence = List.copyOf(expectedEvidence);
    }
}
