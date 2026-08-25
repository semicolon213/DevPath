package com.devpath.rule.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record RuleEvidenceFact(
    String signalKey,
    boolean available,
    boolean present,
    BigDecimal numericValue,
    List<String> evidenceReferences
) {
    public RuleEvidenceFact {
        if (signalKey == null || signalKey.isBlank()) throw new IllegalArgumentException("signalKey is required");
        Objects.requireNonNull(numericValue, "numericValue is required");
        if (numericValue.signum() < 0) throw new IllegalArgumentException("numericValue must not be negative");
        evidenceReferences = List.copyOf(Objects.requireNonNull(evidenceReferences, "evidenceReferences are required"));
        if (!available && (present || numericValue.signum() != 0 || !evidenceReferences.isEmpty())) {
            throw new IllegalArgumentException("unavailable evidence cannot contain an observed value");
        }
    }

    public static RuleEvidenceFact measured(String key, BigDecimal value, List<String> references) {
        return new RuleEvidenceFact(key, true, value.signum() > 0, value, references);
    }

    public static RuleEvidenceFact presence(String key, boolean present, List<String> references) {
        return measured(key, present ? BigDecimal.ONE : BigDecimal.ZERO, references);
    }

    public static RuleEvidenceFact unavailable(String key) {
        return new RuleEvidenceFact(key, false, false, BigDecimal.ZERO, List.of());
    }
}
