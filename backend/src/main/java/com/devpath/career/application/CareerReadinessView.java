package com.devpath.career.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CareerReadinessView(
    UUID careerReadinessId,
    UUID skillMatrixId,
    String careerId,
    UUID careerProfileVersionId,
    String careerProfileVersion,
    String readinessPolicyVersion,
    String ruleSetVersion,
    String status,
    BigDecimal readinessScore,
    String readinessLevel,
    BigDecimal confidence,
    List<String> unavailableCategories,
    List<SkillGapView> skillGaps,
    Instant assessedAt
) {
    public CareerReadinessView {
        unavailableCategories = List.copyOf(unavailableCategories);
        skillGaps = List.copyOf(skillGaps);
    }
}
