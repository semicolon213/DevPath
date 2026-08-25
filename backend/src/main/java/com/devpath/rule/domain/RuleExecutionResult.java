package com.devpath.rule.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record RuleExecutionResult(
    String ruleId,
    String ruleVersion,
    RuleCategory category,
    RuleOutcomeStatus status,
    BigDecimal rawValue,
    BigDecimal score,
    BigDecimal weight,
    String formulaId,
    String trace,
    List<String> evidenceReferences
) {
    public RuleExecutionResult {
        Objects.requireNonNull(category); Objects.requireNonNull(status); Objects.requireNonNull(rawValue);
        Objects.requireNonNull(score); Objects.requireNonNull(weight);
        if (ruleId == null || ruleId.isBlank() || ruleId.length() > 64
            || ruleVersion == null || ruleVersion.isBlank() || ruleVersion.length() > 32
            || formulaId == null || formulaId.isBlank() || formulaId.length() > 80
            || trace == null || trace.isBlank() || trace.length() > 1000
            || rawValue.signum() < 0 || score.signum() < 0 || score.compareTo(new java.math.BigDecimal("100")) > 0
            || weight.signum() <= 0 || weight.compareTo(java.math.BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("rule execution result is invalid");
        }
        evidenceReferences = List.copyOf(Objects.requireNonNull(evidenceReferences));
        if (evidenceReferences.stream().anyMatch(value -> value == null || value.isBlank() || value.length() > 1200)) {
            throw new IllegalArgumentException("rule evidence reference is invalid");
        }
    }
}
