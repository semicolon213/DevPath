package com.devpath.rule.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DeterministicSkillMatrixBuilder {
    public SkillMatrix build(
        UUID matrixId,
        CompletedRuleEvaluation evaluation,
        UUID repositoryId,
        SkillMatrixPolicy policy,
        List<RuleEvaluationEvidenceLink> evidenceLinks,
        java.time.Instant generatedAt
    ) {
        if (!evaluation.ruleSetVersionId().equals(policy.ruleSetVersionId())) {
            throw new IllegalArgumentException("skill policy does not match evaluation rule-set version");
        }
        Map<String, RuleCategory> categoryByRule = evaluation.result().categoryScores().stream()
            .flatMap(category -> category.ruleResults().stream().map(rule -> Map.entry(rule.ruleId(), category.category())))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<RuleCategory, List<UUID>> evidenceByCategory = evidenceLinks.stream()
            .filter(link -> categoryByRule.containsKey(link.ruleId()))
            .collect(Collectors.groupingBy(link -> categoryByRule.get(link.ruleId()),
                Collectors.mapping(link -> link.evidence().evidenceId(), Collectors.collectingAndThen(Collectors.toList(),
                    values -> values.stream().distinct().sorted().toList()))));
        Map<RuleCategory, RuleCategoryScore> scores = evaluation.result().categoryScores().stream()
            .collect(Collectors.toUnmodifiableMap(RuleCategoryScore::category, Function.identity()));
        List<SkillAssessment> assessments = policy.skills().stream().map(skill -> {
            RuleCategoryScore category = scores.get(skill.category());
            if (category == null) throw new IllegalArgumentException("evaluation is missing category " + skill.category());
            var facts = List.of(
                "CATEGORY=" + skill.category().name(),
                "CATEGORY_SCORE=" + category.score().toPlainString(),
                "CONFIDENCE=" + category.confidence().toPlainString(),
                "MISSING_EVIDENCE_COUNT=" + category.missingEvidence().size()
            );
            return new SkillAssessment(UUID.randomUUID(), skill, category.score(), policy.level(category.score()),
                category.confidence(), category.score().compareTo(policy.strengthMinimum()) >= 0,
                category.score().compareTo(policy.weaknessMaximum()) <= 0, "UNAVAILABLE",
                "evaluation:" + evaluation.id() + ":category:" + skill.category().name(),
                evidenceByCategory.getOrDefault(skill.category(), List.of()), List.of(repositoryId), facts,
                evaluation.result().ruleSetVersion());
        }).toList();
        return new SkillMatrix(matrixId, evaluation.userId(), evaluation.id(), policy.policyId(), policy.versionLabel(),
            evaluation.result().ruleSetVersion(), "CURRENT", generatedAt, assessments);
    }
}
