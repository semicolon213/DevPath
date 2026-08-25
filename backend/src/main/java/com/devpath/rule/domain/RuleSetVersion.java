package com.devpath.rule.domain;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record RuleSetVersion(
    String ruleSetId,
    String versionId,
    String versionLabel,
    String formulaLibraryVersion,
    String requiredExtractorVersion,
    Map<RuleCategory, BigDecimal> categoryWeights,
    List<RuleDefinition> rules
) {
    private static final BigDecimal ONE = new BigDecimal("1.000000");

    public RuleSetVersion {
        if (ruleSetId == null || ruleSetId.isBlank()) throw new IllegalArgumentException("ruleSetId is required");
        if (versionId == null || versionId.isBlank()) throw new IllegalArgumentException("versionId is required");
        if (versionLabel == null || versionLabel.isBlank()) throw new IllegalArgumentException("versionLabel is required");
        if (formulaLibraryVersion == null || formulaLibraryVersion.isBlank()) throw new IllegalArgumentException("formulaLibraryVersion is required");
        if (requiredExtractorVersion == null || requiredExtractorVersion.isBlank()) throw new IllegalArgumentException("requiredExtractorVersion is required");
        Objects.requireNonNull(categoryWeights, "categoryWeights are required");
        Objects.requireNonNull(rules, "rules are required");

        var copiedWeights = new EnumMap<RuleCategory, BigDecimal>(RuleCategory.class);
        copiedWeights.putAll(categoryWeights);
        categoryWeights = Map.copyOf(copiedWeights);
        rules = rules.stream().filter(RuleDefinition::enabled)
            .sorted(Comparator.comparingInt(RuleDefinition::priority).thenComparing(RuleDefinition::ruleId)).toList();
        if (rules.isEmpty()) throw new IllegalArgumentException("at least one enabled rule is required");

        Set<RuleCategory> categories = rules.stream().map(RuleDefinition::category).collect(Collectors.toSet());
        if (!categoryWeights.keySet().equals(categories)) throw new IllegalArgumentException("category weights must match enabled rule categories");
        validateNormalized(categoryWeights.values().stream().toList(), "category weights");
        for (RuleCategory category : categories) {
            validateNormalized(rules.stream().filter(rule -> rule.category() == category).map(RuleDefinition::weight).toList(), category + " rule weights");
        }
        if (rules.stream().map(RuleDefinition::ruleId).distinct().count() != rules.size()) {
            throw new IllegalArgumentException("rule IDs must be unique");
        }
    }

    private static void validateNormalized(List<BigDecimal> weights, String label) {
        if (weights.stream().anyMatch(weight -> weight == null || weight.signum() <= 0)) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        BigDecimal total = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(ONE) != 0) throw new IllegalArgumentException(label + " must total 1.000000");
    }
}
