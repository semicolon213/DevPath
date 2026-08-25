package com.devpath.rule.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record RuleDefinition(
    String ruleId,
    String version,
    RuleCategory category,
    String name,
    int priority,
    String evidenceSignalKey,
    RuleFormula formula,
    BigDecimal formulaParameter,
    BigDecimal weight,
    MissingDataPolicy missingDataPolicy,
    boolean enabled
) {
    public RuleDefinition {
        if (ruleId == null || ruleId.isBlank()) throw new IllegalArgumentException("ruleId is required");
        if (version == null || version.isBlank()) throw new IllegalArgumentException("version is required");
        Objects.requireNonNull(category, "category is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (priority < 0) throw new IllegalArgumentException("priority must not be negative");
        if (evidenceSignalKey == null || evidenceSignalKey.isBlank()) throw new IllegalArgumentException("evidenceSignalKey is required");
        Objects.requireNonNull(formula, "formula is required");
        Objects.requireNonNull(formulaParameter, "formulaParameter is required");
        Objects.requireNonNull(weight, "weight is required");
        Objects.requireNonNull(missingDataPolicy, "missingDataPolicy is required");
        if (weight.signum() <= 0) throw new IllegalArgumentException("weight must be positive");
        if (formula == RuleFormula.COUNT_CAP && formulaParameter.signum() <= 0) {
            throw new IllegalArgumentException("COUNT_CAP requires a positive target");
        }
    }
}
