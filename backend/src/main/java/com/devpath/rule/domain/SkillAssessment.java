package com.devpath.rule.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SkillAssessment(
    UUID assessmentId,
    SkillDefinition skill,
    BigDecimal score,
    SkillLevel level,
    BigDecimal confidence,
    boolean strength,
    boolean weakness,
    String growthTrend,
    String aggregateRuleResultReference,
    List<UUID> evidenceIds,
    List<UUID> repositoryIds,
    List<String> recommendationInputFacts,
    String ruleSetVersion
) {
    public SkillAssessment {
        Objects.requireNonNull(assessmentId); Objects.requireNonNull(skill); Objects.requireNonNull(score);
        Objects.requireNonNull(level); Objects.requireNonNull(confidence);
        if (score.signum() < 0 || score.compareTo(new BigDecimal("100")) > 0
            || confidence.signum() < 0 || confidence.compareTo(new BigDecimal("100")) > 0
            || strength && weakness || !"UNAVAILABLE".equals(growthTrend)
            || aggregateRuleResultReference == null || aggregateRuleResultReference.isBlank()
            || ruleSetVersion == null || ruleSetVersion.isBlank()) {
            throw new IllegalArgumentException("skill assessment is invalid");
        }
        evidenceIds = List.copyOf(Objects.requireNonNull(evidenceIds));
        repositoryIds = List.copyOf(Objects.requireNonNull(repositoryIds));
        recommendationInputFacts = List.copyOf(Objects.requireNonNull(recommendationInputFacts));
        if (score.signum() > 0 && evidenceIds.isEmpty() && aggregateRuleResultReference.isBlank()) {
            throw new IllegalArgumentException("non-zero skill assessment requires traceable evidence");
        }
    }
}
