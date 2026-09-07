package com.devpath.ai.application;

import java.util.List;

public record LearningRecommendationValidation(LearningRecommendationContent content, List<String> violations) {
    public LearningRecommendationValidation { violations = List.copyOf(violations); }
}
