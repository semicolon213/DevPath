package com.devpath.prompt.application;

import com.devpath.rule.application.RuleEvaluationView;
import java.util.List;

public final class RepositorySummaryRuleOutputSelection {
    public static final int MAX_RULE_OUTPUTS = 20;

    private RepositorySummaryRuleOutputSelection() {}

    public static List<RuleOutput> select(RuleEvaluationView evaluation) {
        return evaluation.categoryScores().stream().flatMap(category -> category.ruleResults().stream().map(rule ->
            new RuleOutput(evaluation.evaluationId() + ":" + category.category() + ":" + rule.ruleId() + ":" + rule.ruleVersion(),
                category.category(), rule.ruleId(), rule.ruleVersion(), rule.status())
        )).limit(MAX_RULE_OUTPUTS).toList();
    }

    public record RuleOutput(String reference, String category, String ruleId, String ruleVersion, String status) {}
}
