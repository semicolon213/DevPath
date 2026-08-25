package com.devpath.rule.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RuleScoreBreakdownView(
    UUID evaluationId, BigDecimal overallScore, BigDecimal confidence,
    List<RuleCategoryScoreView> categories
) {
    public RuleScoreBreakdownView { categories = List.copyOf(categories); }
}
