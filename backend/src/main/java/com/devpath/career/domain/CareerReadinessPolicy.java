package com.devpath.career.domain;

import com.devpath.rule.domain.RuleCategory;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record CareerReadinessPolicy(
    UUID policyId,
    String versionLabel,
    UUID careerProfileVersionId,
    BigDecimal expectedMinimum,
    BigDecimal developingMinimum,
    BigDecimal strongMinimum,
    Map<RuleCategory, BigDecimal> categoryWeights
) {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public CareerReadinessPolicy {
        Objects.requireNonNull(policyId);
        Objects.requireNonNull(careerProfileVersionId);
        Objects.requireNonNull(expectedMinimum);
        Objects.requireNonNull(developingMinimum);
        Objects.requireNonNull(strongMinimum);
        if (versionLabel == null || versionLabel.isBlank()
            || developingMinimum.compareTo(BigDecimal.ZERO) <= 0
            || expectedMinimum.compareTo(developingMinimum) <= 0
            || strongMinimum.compareTo(expectedMinimum) <= 0
            || strongMinimum.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("readiness policy thresholds are invalid");
        }
        categoryWeights = Map.copyOf(Objects.requireNonNull(categoryWeights));
        BigDecimal total = categoryWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (categoryWeights.isEmpty() || categoryWeights.values().stream().anyMatch(value -> value.signum() <= 0)
            || total.compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("readiness policy weights must total 100");
        }
    }

    public GapState gapState(BigDecimal score) {
        if (score.signum() == 0) return GapState.MISSING;
        if (score.compareTo(developingMinimum) < 0) return GapState.WEAK;
        if (score.compareTo(expectedMinimum) < 0) return GapState.PARTIAL;
        if (score.compareTo(strongMinimum) < 0) return GapState.SUFFICIENT;
        return GapState.STRONG;
    }
}
