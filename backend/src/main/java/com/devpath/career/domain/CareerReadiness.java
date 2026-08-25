package com.devpath.career.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CareerReadiness(
    UUID readinessId,
    UUID userId,
    UUID skillMatrixId,
    String careerId,
    UUID careerProfileVersionId,
    String careerProfileVersion,
    UUID policyId,
    String policyVersion,
    String ruleSetVersion,
    CareerReadinessStatus status,
    BigDecimal readinessScore,
    String readinessLevel,
    BigDecimal confidence,
    List<String> unavailableCategories,
    List<SkillGap> skillGaps,
    Instant assessedAt
) {
    public CareerReadiness {
        Objects.requireNonNull(readinessId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(skillMatrixId);
        Objects.requireNonNull(careerProfileVersionId);
        Objects.requireNonNull(policyId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(confidence);
        Objects.requireNonNull(assessedAt);
        if (careerId == null || careerId.isBlank() || careerProfileVersion == null || careerProfileVersion.isBlank()
            || policyVersion == null || policyVersion.isBlank() || ruleSetVersion == null || ruleSetVersion.isBlank()) {
            throw new IllegalArgumentException("career readiness metadata is invalid");
        }
        unavailableCategories = List.copyOf(Objects.requireNonNull(unavailableCategories));
        skillGaps = List.copyOf(Objects.requireNonNull(skillGaps));
        if (status == CareerReadinessStatus.COMPLETED && (readinessScore == null || readinessLevel == null)
            || status == CareerReadinessStatus.INSUFFICIENT_EVIDENCE
                && (readinessScore != null || readinessLevel != null || unavailableCategories.isEmpty())) {
            throw new IllegalArgumentException("career readiness result is inconsistent");
        }
    }
}
