package com.devpath.learning.domain;

import com.devpath.rule.domain.RuleCategory;
import java.util.Map;
import java.util.UUID;

public record RoadmapPolicy(UUID policyId, String versionLabel, UUID recommendationPolicyId,
    Map<RuleCategory, Integer> categoryOrder) {
    public RoadmapPolicy {
        categoryOrder = Map.copyOf(categoryOrder);
        if (policyId == null || recommendationPolicyId == null || versionLabel == null || versionLabel.isBlank()) {
            throw new IllegalArgumentException("roadmap policy is invalid");
        }
    }
}
