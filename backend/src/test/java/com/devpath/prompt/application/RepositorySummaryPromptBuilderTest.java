package com.devpath.prompt.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.analysis.application.AnalysisResultView;
import com.devpath.repository.application.RepositoryView;
import com.devpath.rule.application.RuleCategoryScoreView;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvidenceSummaryView;
import com.devpath.rule.application.RuleEvidenceView;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.RuleResultView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositorySummaryPromptBuilderTest {
    @Test
    void includesBoundedRuleOutputsAndEvidenceWithoutPrivateRuleTrace() throws Exception {
        UUID repositoryId = UUID.randomUUID(), analysisId = UUID.randomUUID(), evaluationId = UUID.randomUUID();
        var repository = new RepositoryView(repositoryId, "1", "repo", "owner/repo", "owner", "PUBLIC", "main", false, "ACTIVE", "SYNCHRONIZED", "https://example.invalid/private", Instant.EPOCH, Instant.EPOCH, UUID.randomUUID());
        var analysis = new AnalysisResultView(analysisId, repositoryId, UUID.randomUUID(), evaluationId, UUID.randomUUID(), "REPOSITORY_BASELINE", true, Instant.EPOCH);
        var evaluation = new RuleEvaluationView(evaluationId, analysis.snapshotId(), UUID.randomUUID(), "rule-v1", "formula-v1", "extractor-v1", BigDecimal.TEN, BigDecimal.ONE, new RuleEvidenceSummaryView(1, 0, 0), List.of(new RuleCategoryScoreView("DOCUMENTATION", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, List.of(new RuleResultView("README_PRESENT", "v1", "SATISFIED", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, "BOOLEAN_100", "private trace", List.of("private/path"))), List.of())), List.of(), Instant.EPOCH);
        var evidence = new RuleEvidenceListView(evaluationId, List.of(new RuleEvidenceView(UUID.randomUUID(), "README_PRESENT", "DIRECT", "REPOSITORY_PATH", "README.md", "README is present", BigDecimal.ONE)));
        var template = new PromptTemplateVersion(UUID.randomUUID(), "REPOSITORY_SUMMARY", "v2", "Never calculate scores.", "Return JSON.");

        PromptPackage prompt = new RepositorySummaryPromptBuilder(new ObjectMapper()).build(repository, analysis, evaluation, List.of("Java"), evidence, template);
        var context = new ObjectMapper().readTree(prompt.contextPayload());

        assertThat(context.path("categoryScores").get(0).path("score").decimalValue()).isEqualByComparingTo("10");
        assertThat(context.path("ruleOutputs").get(0).path("reference").asText()).contains(evaluationId.toString(), "README_PRESENT", "v1");
        assertThat(prompt.contextPayload()).contains("E1", "Java").doesNotContain("private trace", "private/path", "example.invalid");
        assertThat(prompt.prompt()).contains("every strengths/improvementAreas item", "evidenceReferences", "ruleOutputReferences");
    }
}
