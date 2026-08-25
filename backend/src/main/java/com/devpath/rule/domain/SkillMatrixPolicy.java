package com.devpath.rule.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SkillMatrixPolicy(
    UUID policyId,
    UUID ruleSetVersionId,
    String versionLabel,
    BigDecimal beginnerMinimum,
    BigDecimal developingMinimum,
    BigDecimal competentMinimum,
    BigDecimal strongMinimum,
    BigDecimal weaknessMaximum,
    BigDecimal strengthMinimum,
    List<SkillDefinition> skills
) {
    public SkillMatrixPolicy {
        Objects.requireNonNull(policyId); Objects.requireNonNull(ruleSetVersionId);
        if (versionLabel == null || versionLabel.isBlank()) throw new IllegalArgumentException("policy version is required");
        List<BigDecimal> values = List.of(beginnerMinimum, developingMinimum, competentMinimum, strongMinimum,
            weaknessMaximum, strengthMinimum);
        if (values.stream().anyMatch(Objects::isNull)
            || beginnerMinimum.compareTo(BigDecimal.ONE) != 0
            || developingMinimum.compareTo(beginnerMinimum) <= 0
            || competentMinimum.compareTo(developingMinimum) <= 0
            || strongMinimum.compareTo(competentMinimum) <= 0
            || strongMinimum.compareTo(new BigDecimal("100")) > 0
            || weaknessMaximum.signum() < 0 || weaknessMaximum.compareTo(new BigDecimal("100")) > 0
            || strengthMinimum.signum() < 0 || strengthMinimum.compareTo(new BigDecimal("100")) > 0
            || weaknessMaximum.compareTo(strengthMinimum) >= 0) {
            throw new IllegalArgumentException("skill matrix thresholds are invalid");
        }
        skills = List.copyOf(Objects.requireNonNull(skills));
        if (skills.isEmpty() || skills.stream().map(SkillDefinition::skillId).distinct().count() != skills.size()
            || skills.stream().map(SkillDefinition::category).distinct().count() != skills.size()) {
            throw new IllegalArgumentException("skill mappings must contain one unique skill per category");
        }
    }

    public SkillLevel level(BigDecimal score) {
        if (score.signum() == 0) return SkillLevel.NONE;
        if (score.compareTo(strongMinimum) >= 0) return SkillLevel.STRONG;
        if (score.compareTo(competentMinimum) >= 0) return SkillLevel.COMPETENT;
        if (score.compareTo(developingMinimum) >= 0) return SkillLevel.DEVELOPING;
        return SkillLevel.BEGINNER;
    }
}
