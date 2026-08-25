package com.devpath.recommendation.domain;

import com.devpath.rule.domain.RuleCategory;
import java.util.List;

public record RecommendationTemplate(
    RuleCategory category,
    RecommendationType type,
    int prerequisiteOrder,
    int effortHours,
    String title,
    String rationaleCode,
    String completionCriteria,
    List<String> expectedEvidence
) {
    public RecommendationTemplate {
        expectedEvidence = List.copyOf(expectedEvidence);
        if (prerequisiteOrder < 0 || effortHours <= 0 || title == null || title.isBlank()
            || rationaleCode == null || rationaleCode.isBlank() || completionCriteria == null
            || completionCriteria.isBlank() || expectedEvidence.isEmpty()) {
            throw new IllegalArgumentException("recommendation template is invalid");
        }
    }
}
