package com.devpath.prompt.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.analysis.application.AnalysisResultView;
import com.devpath.repository.application.RepositoryView;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvidenceSummaryView;
import com.devpath.rule.application.RuleEvaluationView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
            new RuleEvidenceSummaryView(0, 0, 0), List.of(), List.of(), now);
        var template = new PromptTemplateVersion(UUID.randomUUID(), "REPOSITORY_REVIEW", "review-v1",
            "Never calculate or alter scores.", "Return JSON.");

        PromptPackage prompt = new RepositoryReviewPromptBuilder(new ObjectMapper()).build(repository, analysis,
            evaluation, new RuleEvidenceListView(evaluationId, List.of()), template);

        assertThat(prompt.contextPayload()).contains(analysisId.toString(), "owner/devpath");
        assertThat(prompt.contextPayload()).doesNotContain("example.invalid");
        assertThat(prompt.contextHash()).matches("[a-f0-9]{64}");
    }
}
