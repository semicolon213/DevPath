package com.devpath.rule.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SkillAssessmentView(
    UUID assessmentId, UUID skillId, String skillKey, String skillName, String category,
    BigDecimal score, String level, BigDecimal confidence, boolean strength, boolean weakness,
    String growthTrend, String aggregateRuleResultReference, List<UUID> evidenceIds,
    List<UUID> repositoryIds, List<String> recommendationInputFacts, String ruleSetVersion
) {
    public SkillAssessmentView {
        evidenceIds = List.copyOf(evidenceIds); repositoryIds = List.copyOf(repositoryIds);
        recommendationInputFacts = List.copyOf(recommendationInputFacts);
    }
}
