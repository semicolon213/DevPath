package com.devpath.career.domain;

import com.devpath.rule.domain.RuleCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SkillGap(
    UUID gapId,
    UUID skillAssessmentId,
    UUID skillId,
    String skillKey,
    RuleCategory category,
    BigDecimal actualScore,
    String actualLevel,
    BigDecimal expectedMinimum,
    GapState gapState,
    BigDecimal careerWeight,
    List<UUID> evidenceIds
) {
    public SkillGap {
        Objects.requireNonNull(gapId);
        Objects.requireNonNull(skillAssessmentId);
        Objects.requireNonNull(skillId);
        Objects.requireNonNull(category);
        Objects.requireNonNull(actualScore);
        Objects.requireNonNull(expectedMinimum);
        Objects.requireNonNull(gapState);
        Objects.requireNonNull(careerWeight);
        if (skillKey == null || skillKey.isBlank() || actualLevel == null || actualLevel.isBlank()) {
            throw new IllegalArgumentException("skill gap metadata is invalid");
        }
        evidenceIds = List.copyOf(Objects.requireNonNull(evidenceIds));
    }
}
