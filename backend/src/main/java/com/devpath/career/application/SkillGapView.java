package com.devpath.career.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SkillGapView(
    UUID skillGapId,
    UUID skillId,
    String skillKey,
    String category,
    BigDecimal actualScore,
    String actualLevel,
    BigDecimal expectedMinimum,
    String gapState,
    BigDecimal careerWeight,
    List<UUID> evidenceIds
) {
    public SkillGapView { evidenceIds = List.copyOf(evidenceIds); }
}
