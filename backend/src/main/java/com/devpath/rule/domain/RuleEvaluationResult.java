package com.devpath.rule.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record RuleEvaluationResult(
    String snapshotId,
    String ruleSetVersionId,
    String ruleSetVersion,
    String formulaLibraryVersion,
    String extractorVersion,
    BigDecimal overallScore,
    BigDecimal confidence,
    List<RuleCategoryScore> categoryScores,
    List<String> warnings
) {
    public RuleEvaluationResult {
        Objects.requireNonNull(overallScore); Objects.requireNonNull(confidence);
        if (snapshotId == null || snapshotId.isBlank() || ruleSetVersionId == null || ruleSetVersionId.isBlank()
            || ruleSetVersion == null || ruleSetVersion.isBlank() || formulaLibraryVersion == null || formulaLibraryVersion.isBlank()
            || extractorVersion == null || extractorVersion.isBlank()
            || overallScore.signum() < 0 || overallScore.compareTo(new java.math.BigDecimal("100")) > 0
            || confidence.signum() < 0 || confidence.compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("rule evaluation result is invalid");
        }
        categoryScores = List.copyOf(Objects.requireNonNull(categoryScores));
        warnings = List.copyOf(Objects.requireNonNull(warnings));
        if (categoryScores.stream().map(RuleCategoryScore::category).distinct().count() != categoryScores.size()
            || warnings.stream().anyMatch(value -> value == null || value.isBlank() || value.length() > 500)) {
            throw new IllegalArgumentException("rule evaluation result contents are invalid");
        }
    }
}
