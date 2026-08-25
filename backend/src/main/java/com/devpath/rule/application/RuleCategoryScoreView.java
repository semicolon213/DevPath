package com.devpath.rule.application;

import java.math.BigDecimal;
import java.util.List;

public record RuleCategoryScoreView(
    String category, BigDecimal score, BigDecimal weight, BigDecimal confidence,
    List<RuleResultView> ruleResults, List<String> missingEvidence
) {
    public RuleCategoryScoreView { ruleResults = List.copyOf(ruleResults); missingEvidence = List.copyOf(missingEvidence); }
}
