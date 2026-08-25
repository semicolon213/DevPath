package com.devpath.rule.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DeterministicRuleEngine {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;

    public RuleEvaluationResult evaluate(
        String snapshotId,
        String extractorVersion,
        RuleSetVersion ruleSet,
        List<RuleEvidenceFact> facts
    ) {
        if (!ruleSet.requiredExtractorVersion().equals(extractorVersion)) {
            throw new IllegalArgumentException("rule set requires extractor version " + ruleSet.requiredExtractorVersion());
        }
        Map<String, RuleEvidenceFact> factsByKey = facts.stream().collect(Collectors.toUnmodifiableMap(
            RuleEvidenceFact::signalKey, Function.identity(), (left, right) -> { throw new IllegalArgumentException("duplicate evidence signal " + left.signalKey()); }
        ));
        List<RuleExecutionResult> executions = ruleSet.rules().stream()
            .map(rule -> execute(rule, factsByKey.get(rule.evidenceSignalKey()), ruleSet.formulaLibraryVersion())).toList();
        List<RuleCategoryScore> categories = ruleSet.categoryWeights().keySet().stream()
            .sorted(Comparator.comparing(Enum::name))
            .map(category -> aggregateCategory(category, ruleSet.categoryWeights().get(category), executions))
            .toList();
        BigDecimal overall = categories.stream().map(value -> value.score().multiply(value.weight()))
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal confidence = categories.stream().map(value -> value.confidence().multiply(value.weight()))
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
        List<String> warnings = executions.stream().filter(value -> value.status() == RuleOutcomeStatus.SKIPPED)
            .map(value -> "Missing unavailable evidence: " + value.ruleId()).toList();
        return new RuleEvaluationResult(snapshotId, ruleSet.versionId(), ruleSet.versionLabel(),
            ruleSet.formulaLibraryVersion(), extractorVersion, overall, confidence, categories, warnings);
    }

    private RuleExecutionResult execute(RuleDefinition rule, RuleEvidenceFact fact, String formulaLibraryVersion) {
        if (fact == null || !fact.available()) {
            if (rule.missingDataPolicy() == MissingDataPolicy.SKIP) {
                return result(rule, RuleOutcomeStatus.SKIPPED, BigDecimal.ZERO, BigDecimal.ZERO,
                    formulaLibraryVersion, "evidence unavailable; rule skipped", List.of());
            }
            return result(rule, RuleOutcomeStatus.FAILED, BigDecimal.ZERO, BigDecimal.ZERO,
                formulaLibraryVersion, "evidence unavailable; ZERO policy applied", List.of());
        }
        BigDecimal score = switch (rule.formula()) {
            case PRESENCE -> fact.present() ? HUNDRED : BigDecimal.ZERO;
            case COUNT_CAP -> fact.numericValue().divide(rule.formulaParameter(), 8, RoundingMode.HALF_UP)
                .multiply(HUNDRED).min(HUNDRED);
            case PERCENTAGE -> fact.numericValue().min(HUNDRED);
        };
        score = score.max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);
        RuleOutcomeStatus status = score.signum() == 0 ? RuleOutcomeStatus.FAILED
            : score.compareTo(HUNDRED) == 0 ? RuleOutcomeStatus.PASSED : RuleOutcomeStatus.PARTIAL;
        String trace = "formula=" + rule.formula() + "; parameter=" + rule.formulaParameter().toPlainString()
            + "; raw=" + fact.numericValue().toPlainString() + "; score=" + score.toPlainString();
        return result(rule, status, fact.numericValue(), score, formulaLibraryVersion, trace, fact.evidenceReferences());
    }

    private RuleCategoryScore aggregateCategory(RuleCategory category, BigDecimal categoryWeight, List<RuleExecutionResult> executions) {
        List<RuleExecutionResult> rules = executions.stream().filter(value -> value.category() == category).toList();
        BigDecimal score = rules.stream().map(value -> value.score().multiply(value.weight()))
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal availableWeight = rules.stream().filter(value -> value.status() != RuleOutcomeStatus.SKIPPED)
            .map(RuleExecutionResult::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal confidence = availableWeight.multiply(HUNDRED).setScale(SCALE, RoundingMode.HALF_UP);
        List<String> missing = new ArrayList<>();
        rules.stream().filter(value -> value.status() == RuleOutcomeStatus.SKIPPED)
            .map(RuleExecutionResult::ruleId).forEach(missing::add);
        return new RuleCategoryScore(category, score, categoryWeight, confidence, rules, missing);
    }

    private RuleExecutionResult result(RuleDefinition rule, RuleOutcomeStatus status, BigDecimal raw, BigDecimal score,
                                       String formulaLibraryVersion, String trace, List<String> references) {
        return new RuleExecutionResult(rule.ruleId(), rule.version(), rule.category(), status, raw,
            score.setScale(SCALE, RoundingMode.HALF_UP), rule.weight(),
            rule.formula().name() + "@" + formulaLibraryVersion, trace, references);
    }
}
