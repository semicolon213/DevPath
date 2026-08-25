package com.devpath.rule.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleEvaluationView(
    UUID evaluationId, UUID snapshotId, UUID ruleSetVersionId, String ruleSetVersion,
    String formulaLibraryVersion, String extractorVersion, BigDecimal overallScore,
    BigDecimal confidence, RuleEvidenceSummaryView evidenceSummary, List<RuleCategoryScoreView> categoryScores,
    List<String> warnings, Instant completedAt
) {
    public RuleEvaluationView { categoryScores = List.copyOf(categoryScores); warnings = List.copyOf(warnings); }
}
