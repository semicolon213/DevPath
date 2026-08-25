package com.devpath.rule.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record RuleEvaluationEvidence(
    UUID evidenceId,
    UUID userId,
    UUID snapshotId,
    String evidenceType,
    String sourceReference,
    String observedFactSummary,
    BigDecimal confidence
) {
    public RuleEvaluationEvidence {
        Objects.requireNonNull(evidenceId); Objects.requireNonNull(userId); Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(confidence);
        if (evidenceType == null || evidenceType.isBlank() || sourceReference == null || sourceReference.isBlank()
            || observedFactSummary == null || observedFactSummary.isBlank()
            || confidence.signum() < 0 || confidence.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("evaluation evidence is invalid");
        }
    }
}
