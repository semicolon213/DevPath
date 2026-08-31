package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.ai.config.AiGenerationProperties;
import com.devpath.ai.domain.GenerationJob;
import com.devpath.analysis.application.AnalysisApplicationService;
import com.devpath.analysis.application.AnalysisResultView;
import com.devpath.prompt.application.PromptPackage;
import com.devpath.prompt.application.PromptTemplateVersion;
import com.devpath.prompt.application.RepositoryReviewPromptBuilder;
import com.devpath.prompt.application.SkillExplanationPromptBuilder;
import com.devpath.repository.application.RepositoryApplicationService;
import com.devpath.repository.application.RepositoryView;
import com.devpath.rule.application.CompletedRuleEvaluationApplicationService;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.SkillMatrixApplicationService;
import com.devpath.rule.application.SkillMatrixView;
import com.devpath.shared.application.ObjectContentPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiGenerationApplicationServiceTest {
    static final Instant NOW = Instant.parse("2026-08-31T00:00:00Z");
    @Mock GenerationPersistencePort persistence;
    @Mock SkillMatrixApplicationService matrices;
    @Mock SkillExplanationPromptBuilder prompts;
    @Mock SkillExplanationValidator validator;
    @Mock AnalysisApplicationService analyses;
    @Mock RepositoryApplicationService repositories;
    @Mock CompletedRuleEvaluationApplicationService evaluations;
    @Mock RepositoryReviewPromptBuilder reviewPrompts;
    @Mock RepositoryReviewValidator reviewValidator;
    @Mock ObjectContentPort objects;
    @Mock AiAuditPort audit;
    @Mock AiGenerationTransactionService transactions;
    AiGenerationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AiGenerationApplicationService(persistence, matrices, prompts, validator, analyses, repositories,
            evaluations, reviewPrompts, reviewValidator, objects,
            new AiGenerationProperties("http://localhost", "qwen-test", Duration.ofSeconds(1),
                Duration.ofSeconds(2), 2), audit, transactions, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsOneLockedContextFromAnOwnerScopedSkillMatrix() {
        UUID owner = UUID.randomUUID(), matrixId = UUID.randomUUID(), templateId = UUID.randomUUID();
        SkillMatrixView matrix = org.mockito.Mockito.mock(SkillMatrixView.class);
        when(persistence.findByOwnerAndIdempotencyKey(owner, "key")).thenReturn(Optional.empty());
        when(matrices.get(owner, matrixId)).thenReturn(matrix);
        when(persistence.loadActiveTemplate(AiGenerationApplicationService.SKILL_EXPLANATION_TASK)).thenReturn(
            new PromptTemplateVersion(templateId, AiGenerationApplicationService.SKILL_EXPLANATION_TASK, "v1",
                "Never calculate scores.", "Return JSON."));
        when(prompts.build(eq(matrix), any())).thenReturn(new PromptPackage("{}", "prompt", "a".repeat(64), 4096));
        when(persistence.saveJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistence.findArtifactIdByPromptContext(any())).thenReturn(Optional.empty());

        GenerationJobView result = service.request(owner, AiGenerationApplicationService.SKILL_EXPLANATION_TASK,
            List.of(matrixId), AiGenerationApplicationService.SKILL_EXPLANATION_OUTPUT, "key");

        ArgumentCaptor<StoredPromptContext> context = ArgumentCaptor.forClass(StoredPromptContext.class);
        verify(persistence).saveContext(context.capture());
        assertThat(context.getValue().userId()).isEqualTo(owner);
        assertThat(context.getValue().skillMatrixId()).isEqualTo(matrixId);
        assertThat(context.getValue().analysisId()).isNull();
        assertThat(context.getValue().templateVersionId()).isEqualTo(templateId);
        assertThat(result.status()).isEqualTo("QUEUED");
    }

    @Test
    void persistsAnArtifactOnlyAfterValidationPasses() {
        UUID owner = UUID.randomUUID(), context = UUID.randomUUID(), matrix = UUID.randomUUID(), execution = UUID.randomUUID();
        GenerationJob running = GenerationJob.queue(owner, context, "key", AiGenerationApplicationService.SKILL_EXPLANATION_TASK,
            NOW.minusSeconds(1)).start(NOW.minusMillis(500));
        GenerationWorkItem item = new GenerationWorkItem(running, matrix, null, "prompt");
        SkillMatrixView matrixView = org.mockito.Mockito.mock(SkillMatrixView.class);
        var content = new SkillExplanationContent("grounded", List.of(), List.of());
        when(transactions.providerCompleted(item, execution,
            new GenerationProviderResult("raw", 10, 5), 12, NOW))
            .thenReturn(Optional.of(running));
        when(matrices.get(owner, matrix)).thenReturn(matrixView);
        when(validator.validate("raw", matrixView)).thenReturn(new SkillExplanationValidation(content, List.of()));
        when(objects.put(eq(owner), any(), eq(running.id()), eq("skill-explanation.json"), any()))
            .thenReturn("object://owner/artifact");
        when(transactions.validated(eq(item), eq(execution), any(), eq("object://owner/artifact"), any(), eq(NOW)))
            .thenReturn(true);

        service.providerSucceeded(item, execution,
            new GenerationProviderResult("raw", 10, 5), 12);

        verify(transactions).validated(eq(item), eq(execution), any(), eq("object://owner/artifact"), any(), eq(NOW));
        verify(persistence, never()).recordRejectedResponse(any(), any(), any(), any(), any());
    }

    @Test
    void createsRepositoryReviewContextFromOwnedAnalysis() {
        UUID owner = UUID.randomUUID(), analysisId = UUID.randomUUID(), repositoryId = UUID.randomUUID();
        UUID evaluationId = UUID.randomUUID(), matrixId = UUID.randomUUID(), templateId = UUID.randomUUID();
        var analysis = new AnalysisResultView(analysisId, repositoryId, UUID.randomUUID(), evaluationId,
            matrixId, "REPOSITORY_BASELINE", true, NOW);
        RepositoryView repository = org.mockito.Mockito.mock(RepositoryView.class);
        RuleEvaluationView evaluation = org.mockito.Mockito.mock(RuleEvaluationView.class);
        var evidence = new RuleEvidenceListView(evaluationId, List.of());
        when(persistence.findByOwnerAndIdempotencyKey(owner, "review-key")).thenReturn(Optional.empty());
        when(persistence.loadActiveTemplate(AiGenerationApplicationService.REPOSITORY_REVIEW_TASK)).thenReturn(
            new PromptTemplateVersion(templateId, AiGenerationApplicationService.REPOSITORY_REVIEW_TASK, "v1",
                "Never calculate scores.", "Return JSON."));
        when(analyses.getResult(owner, analysisId)).thenReturn(analysis);
        when(repositories.get(owner, repositoryId)).thenReturn(repository);
        when(evaluations.getEvaluation(owner, evaluationId)).thenReturn(evaluation);
        when(evaluations.getEvidence(owner, evaluationId)).thenReturn(evidence);
        when(reviewPrompts.build(repository, analysis, evaluation, evidence,
            new PromptTemplateVersion(templateId, AiGenerationApplicationService.REPOSITORY_REVIEW_TASK, "v1",
                "Never calculate scores.", "Return JSON.")))
            .thenReturn(new PromptPackage("{}", "review prompt", "b".repeat(64), 8192));
        when(persistence.saveJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistence.findArtifactIdByPromptContext(any())).thenReturn(Optional.empty());

        service.request(owner, AiGenerationApplicationService.REPOSITORY_REVIEW_TASK, List.of(analysisId),
            AiGenerationApplicationService.REPOSITORY_REVIEW_OUTPUT, "review-key");

        ArgumentCaptor<StoredPromptContext> context = ArgumentCaptor.forClass(StoredPromptContext.class);
        verify(persistence).saveContext(context.capture());
        assertThat(context.getValue().analysisId()).isEqualTo(analysisId);
        assertThat(context.getValue().skillMatrixId()).isEqualTo(matrixId);
    }

    @Test
    void persistsRepositoryReviewOnlyAfterGroundingValidation() {
        UUID owner = UUID.randomUUID(), context = UUID.randomUUID(), matrix = UUID.randomUUID();
        UUID analysisId = UUID.randomUUID(), evaluationId = UUID.randomUUID(), execution = UUID.randomUUID();
        GenerationJob running = GenerationJob.queue(owner, context, "review-key",
            AiGenerationApplicationService.REPOSITORY_REVIEW_TASK, NOW.minusSeconds(1)).start(NOW.minusMillis(500));
        GenerationWorkItem item = new GenerationWorkItem(running, matrix, analysisId, "prompt");
        var analysis = new AnalysisResultView(analysisId, UUID.randomUUID(), UUID.randomUUID(), evaluationId,
            matrix, "REPOSITORY_BASELINE", true, NOW);
        var evidence = new RuleEvidenceListView(evaluationId, List.of());
        var content = new RepositoryReviewContent("grounded", List.of());
        when(transactions.providerCompleted(item, execution, new GenerationProviderResult("raw", 10, 5), 12, NOW))
            .thenReturn(Optional.of(running));
        when(analyses.getResult(owner, analysisId)).thenReturn(analysis);
        when(evaluations.getEvidence(owner, evaluationId)).thenReturn(evidence);
        when(reviewValidator.validate("raw", evidence)).thenReturn(new RepositoryReviewValidation(content, List.of()));
        when(objects.put(eq(owner), any(), eq(running.id()), eq("repository-review.json"), any()))
            .thenReturn("object://owner/review");
        when(transactions.validated(eq(item), eq(execution), any(), eq("object://owner/review"), any(), eq(NOW)))
            .thenReturn(true);

        service.providerSucceeded(item, execution, new GenerationProviderResult("raw", 10, 5), 12);

        verify(transactions).validated(eq(item), eq(execution), any(), eq("object://owner/review"), any(), eq(NOW));
    }
}
