package com.devpath.ai.application;

import java.util.List;
import java.util.UUID;

public record LearningRecommendationContent(String recommendationSummary, List<Step> steps) {
    public LearningRecommendationContent { steps = steps == null ? null : List.copyOf(steps); }

    public record Step(
        UUID roadmapStepId, int position, String milestoneExplanation, String objective,
        String practiceTask, String completionCriteria, List<String> expectedEvidence
    ) {
        public Step { expectedEvidence = List.copyOf(expectedEvidence); }
    }
}
