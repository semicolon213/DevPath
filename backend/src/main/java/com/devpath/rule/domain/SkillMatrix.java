package com.devpath.rule.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SkillMatrix(
    UUID matrixId,
    UUID userId,
    UUID evaluationId,
    UUID policyId,
    String policyVersion,
    String ruleSetVersion,
    String status,
    Instant generatedAt,
    List<SkillAssessment> assessments
) {
    public SkillMatrix {
        Objects.requireNonNull(matrixId); Objects.requireNonNull(userId); Objects.requireNonNull(evaluationId);
        Objects.requireNonNull(policyId); Objects.requireNonNull(generatedAt);
        if (policyVersion == null || policyVersion.isBlank() || ruleSetVersion == null || ruleSetVersion.isBlank()
            || !("CURRENT".equals(status) || "SUPERSEDED".equals(status) || "ARCHIVED".equals(status))) {
            throw new IllegalArgumentException("skill matrix metadata is invalid");
        }
        assessments = List.copyOf(Objects.requireNonNull(assessments));
        if (assessments.isEmpty() || assessments.stream().map(value -> value.skill().skillId()).distinct().count() != assessments.size()) {
            throw new IllegalArgumentException("skill matrix assessments are invalid");
        }
    }
}
