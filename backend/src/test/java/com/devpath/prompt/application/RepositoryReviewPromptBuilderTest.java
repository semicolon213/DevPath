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
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RepositoryReviewPromptBuilderTest {
    @Test
    void locksOnlyOwnedAnalysisFactsIntoThePrompt() {
        Instant now = Instant.parse("2026-08-31T00:00:00Z");
        UUID analysisId = UUID.randomUUID(), repositoryId = UUID.randomUUID(), evaluationId = UUID.randomUUID();
        var repository = new RepositoryView(repositoryId, "1", "devpath", "owner/devpath", "owner", "PUBLIC",
            "main", false, "ACTIVE", "SYNCHRONIZED", "https://example.invalid/private", now, now, UUID.randomUUID());
        var analysis = new AnalysisResultView(analysisId, repositoryId, UUID.randomUUID(), evaluationId,
            UUID.randomUUID(), "REPOSITORY_BASELINE", true, now);
        var evaluation = new RuleEvaluationView(evaluationId, analysis.snapshotId(), UUID.randomUUID(), "rule-v1",
            "formula-v1", "extractor-v1", BigDecimal.TEN, BigDecimal.ONE,
            new RuleEvidenceSummaryView(0, 0, 0), List.of(new RuleCategoryScoreView("TESTING", BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.ONE, List.of(new RuleResultView("TEST_FILES", "v1", "SATISFIED",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, "formula", "private trace",
                List.of("private/path"))), List.of())), List.of(), now);
        var template = new PromptTemplateVersion(UUID.randomUUID(), "REPOSITORY_REVIEW", "review-v1",
            "Never calculate or alter scores.", "Return JSON.");

        PromptPackage prompt = new RepositoryReviewPromptBuilder(new ObjectMapper()).build(repository, analysis,
            evaluation, new RuleEvidenceListView(evaluationId, List.of()), template);

        assertThat(prompt.contextPayload()).contains(analysisId.toString(), "owner/devpath");
        assertThat(prompt.contextPayload()).contains("TEST_FILES", "SATISFIED")
            .doesNotContain("example.invalid", "private trace", "private/path");
        assertThat(prompt.prompt()).contains("one short Korean sentence", "sections field must be a JSON array",
            "supplied evidenceKey values in evidenceIds");
        assertThat(prompt.contextHash()).matches("[a-f0-9]{64}");
    }

    @Test
    void boundsLargeEvidenceSetsByReviewCategory() throws Exception {
        Instant now = Instant.parse("2026-08-31T00:00:00Z");
        UUID analysisId = UUID.randomUUID(), repositoryId = UUID.randomUUID(), evaluationId = UUID.randomUUID();
        var repository = new RepositoryView(repositoryId, "1", "devpath", "owner/devpath", "owner", "PUBLIC",
            "main", false, "ACTIVE", "SYNCHRONIZED", "https://example.invalid/private", now, now, UUID.randomUUID());
        var analysis = new AnalysisResultView(analysisId, repositoryId, UUID.randomUUID(), evaluationId,
            UUID.randomUUID(), "REPOSITORY_BASELINE", true, now);
        var evaluation = new RuleEvaluationView(evaluationId, analysis.snapshotId(), UUID.randomUUID(), "rule-v1",
            "formula-v1", "extractor-v1", BigDecimal.TEN, BigDecimal.ONE,
            new RuleEvidenceSummaryView(250, 250, 0), List.of(), List.of(), now);
        List<RuleEvidenceView> evidence = IntStream.range(0, 250).mapToObj(index ->
            new RuleEvidenceView(UUID.randomUUID(),
                List.of("LANGUAGE_COUNT", "TEST_FILES", "CI_WORKFLOWS", "README_PRESENT", "COMMIT_COUNT").get(index % 5),
                "DIRECT", "SNAPSHOT_SIGNAL", "snapshot:signal:" + index, "x".repeat(300), BigDecimal.ONE)).toList();
        var template = new PromptTemplateVersion(UUID.randomUUID(), "REPOSITORY_REVIEW", "review-v1",
            "Never calculate or alter scores.", "Return JSON.");

        PromptPackage prompt = new RepositoryReviewPromptBuilder(new ObjectMapper()).build(repository, analysis,
            evaluation, new RuleEvidenceListView(evaluationId, evidence), template);

        var groupedEvidence = new ObjectMapper().readTree(prompt.contextPayload()).path("evidence");
        assertThat(groupedEvidence).hasSize(5);
        assertThat(groupedEvidence.properties()).allSatisfy(entry -> {
            assertThat(entry.getValue()).hasSize(2);
            assertThat(entry.getValue().path(0).path("evidenceKey").asText()).isEqualTo("A");
            assertThat(entry.getValue().path(1).path("evidenceKey").asText()).isEqualTo("B");
            assertThat(entry.getValue().path(0).has("evidenceId")).isFalse();
        });
        assertThat((prompt.prompt().length() + 3) / 4).isLessThanOrEqualTo(prompt.tokenBudget());
    }
}
