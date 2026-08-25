package com.devpath.rule.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record RuleCategoryScore(
    RuleCategory category,
    BigDecimal score,
    BigDecimal weight,
    BigDecimal confidence,
    List<RuleExecutionResult> ruleResults,
    List<String> missingEvidence
) {
    public RuleCategoryScore {
        Objects.requireNonNull(category); Objects.requireNonNull(score); Objects.requireNonNull(weight); Objects.requireNonNull(confidence);
        if (score.signum() < 0 || score.compareTo(new java.math.BigDecimal("100")) > 0
            || confidence.signum() < 0 || confidence.compareTo(new java.math.BigDecimal("100")) > 0
            || weight.signum() <= 0 || weight.compareTo(java.math.BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("category score is invalid");
        }
        ruleResults = List.copyOf(Objects.requireNonNull(ruleResults));
        missingEvidence = List.copyOf(Objects.requireNonNull(missingEvidence));
        if (ruleResults.stream().anyMatch(value -> value.category() != category)
            || ruleResults.stream().map(RuleExecutionResult::ruleId).distinct().count() != ruleResults.size()
            || missingEvidence.stream().anyMatch(value -> value == null || value.isBlank() || value.length() > 128)) {
            throw new IllegalArgumentException("category result contents are invalid");
        }
    }
}
