package com.devpath.recommendation.domain;

import com.devpath.rule.domain.RuleCategory;
import java.util.Map;
import java.util.UUID;

public record RecommendationPolicy(
    UUID policyId,
    String versionLabel,
    UUID careerProfileVersionId,
    Map<RuleCategory, RecommendationTemplate> templates
) {
    public RecommendationPolicy {
        templates = Map.copyOf(templates);
        if (policyId == null || careerProfileVersionId == null || versionLabel == null || versionLabel.isBlank()
            || templates.isEmpty() || templates.entrySet().stream().anyMatch(entry -> entry.getKey() != entry.getValue().category())) {
            throw new IllegalArgumentException("recommendation policy is invalid");
        }
    }
}
