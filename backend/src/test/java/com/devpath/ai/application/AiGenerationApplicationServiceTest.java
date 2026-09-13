package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.ai.config.AiGenerationProperties;
import com.devpath.ai.domain.GenerationJob;
import com.devpath.analysis.application.AnalysisApplicationService;
import com.devpath.analysis.application.AnalysisNotFoundException;
import com.devpath.analysis.application.AnalysisResultView;
import com.devpath.career.application.CareerReadinessApplicationService;
import com.devpath.career.application.CareerReadinessNotFoundException;
import com.devpath.career.application.CareerReadinessView;
import com.devpath.learning.application.LearningRoadmapListView;
import com.devpath.learning.application.LearningRoadmapView;
import com.devpath.prompt.application.CareerCoachingPromptBuilder;
import com.devpath.prompt.application.LearningRecommendationPromptBuilder;
import com.devpath.prompt.application.PromptPackage;
import com.devpath.prompt.application.PromptTemplateVersion;
import com.devpath.prompt.application.RepositoryReviewPromptBuilder;
import com.devpath.prompt.application.RepositorySummaryPromptBuilder;
import com.devpath.prompt.application.SkillExplanationPromptBuilder;
import com.devpath.repository.application.RepositoryApplicationService;
import com.devpath.repository.application.RepositorySynchronizationApplicationService;
import com.devpath.repository.application.TechnologySummaryView;
import com.devpath.repository.application.RepositoryView;
import com.devpath.recommendation.application.RecommendationApplicationService;
import com.devpath.recommendation.application.RecommendationNotFoundException;
import com.devpath.recommendation.application.RecommendationSetListView;
import com.devpath.recommendation.application.RecommendationSetView;
import com.devpath.rule.application.CompletedRuleEvaluationApplicationService;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.SkillMatrixApplicationService;
import com.devpath.rule.application.SkillMatrixNotFoundException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    @Mock RepositorySynchronizationApplicationService synchronization;
    @Mock RepositorySummaryPromptBuilder summaryPrompts;
    @Mock RepositorySummaryValidator summaryValidator;
    @Mock CareerReadinessApplicationService readiness;
    @Mock RecommendationApplicationService recommendations;
    @Mock CareerCoachingPromptBuilder coachingPrompts;
    @Mock CareerAdviceValidator adviceValidator;
    @Mock LearningRecommendationPromptBuilder learningPrompts;
    @Mock LearningRecommendationValidator learningValidator;
    @Mock ObjectContentPort objects;
    @Mock AiAuditPort audit;
    @Mock AiGenerationTransactionService transactions;
    AiGenerationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AiGenerationApplicationService(persistence, matrices, prompts, validator, analyses, repositories,
            evaluations, reviewPrompts, reviewValidator, synchronization, summaryPrompts, summaryValidator,
            readiness, recommendations, coachingPrompts, adviceValidator,
            learningPrompts, learningValidator, objects,
            new AiGenerationProperties("http://localhost", "qwen-test", Duration.ofSeconds(1),
                Duration.ofSeconds(2), 2), audit, transactions, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsOneLockedContextFromAnOwnerScopedSkillMatrix() {
        UUID owner = UUID.randomUUID(), matrixId = UUID.randomUUID(), evaluationId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        SkillMatrixView matrix = org.mockito.Mockito.mock(SkillMatrixView.class);
        RuleEvaluationView evaluation = org.mockito.Mockito.mock(RuleEvaluationView.class);
        var evidence = new RuleEvidenceListView(evaluationId, List.of());
        when(persistence.findByOwnerAndIdempotencyKey(owner, "key")).thenReturn(Optional.empty());
        when(matrices.get(owner, matrixId)).thenReturn(matrix);
        when(matrix.evaluationId()).thenReturn(evaluationId);
        when(evaluations.getEvaluation(owner, evaluationId)).thenReturn(evaluation);
        when(evaluations.getEvidence(owner, evaluationId)).thenReturn(evidence);
        when(persistence.loadActiveTemplate(AiGenerationApplicationService.SKILL_EXPLANATION_TASK)).thenReturn(
            new PromptTemplateVersion(templateId, AiGenerationApplicationService.SKILL_EXPLANATION_TASK, "v1",
                "Never calculate scores.", "Return JSON."));
        when(prompts.build(matrix, evaluation, evidence,
            new PromptTemplateVersion(templateId, AiGenerationApplicationService.SKILL_EXPLANATION_TASK, "v1",
                "Never calculate scores.", "Return JSON.")))
            .thenReturn(new PromptPackage("{}", "prompt", "a".repeat(64), 4096));
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
        GenerationWorkItem item = new GenerationWorkItem(running, matrix, null, null, null, "prompt", "{}", null);
        SkillMatrixView matrixView = org.mockito.Mockito.mock(SkillMatrixView.class);
        UUID evaluationId = UUID.randomUUID();
        RuleEvaluationView evaluation = org.mockito.Mockito.mock(RuleEvaluationView.class);
        var evidence = new RuleEvidenceListView(evaluationId, List.of());
        var content = new SkillExplanationContent("grounded", List.of(), List.of());
        when(transactions.providerCompleted(item, execution,
            new GenerationProviderResult("raw", 10, 5), 12, NOW))
            .thenReturn(Optional.of(running));
        when(matrices.get(owner, matrix)).thenReturn(matrixView);
        when(matrixView.evaluationId()).thenReturn(evaluationId);
        when(evaluations.getEvaluation(owner, evaluationId)).thenReturn(evaluation);
        when(evaluations.getEvidence(owner, evaluationId)).thenReturn(evidence);
        when(validator.validate("raw", matrixView, evaluation, evidence))
            .thenReturn(new SkillExplanationValidation(content, List.of()));
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
    void rejectsTrustedInstructionDisclosureWithoutCreatingAnArtifact() {
        UUID owner = UUID.randomUUID(), context = UUID.randomUUID(), matrix = UUID.randomUUID(), execution = UUID.randomUUID();
        GenerationJob running = GenerationJob.queue(owner, context, "prompt-leak",
            AiGenerationApplicationService.SKILL_EXPLANATION_TASK, NOW.minusSeconds(1)).start(NOW.minusMillis(500));
        String systemPrompt = "Trusted instruction: return only schema v1.";
        String raw = "{\"nested\":{\"text\":\"" + systemPrompt + "\"}}";
        GenerationWorkItem item = new GenerationWorkItem(running, matrix, null, null, null,
            systemPrompt + "\n\n<context>untrusted repository text</context>", "{}", systemPrompt);
        when(transactions.providerCompleted(item, execution, new GenerationProviderResult(raw, 10, 5), 12, NOW))
            .thenReturn(Optional.of(running));
        when(objects.put(eq(owner), any(), eq(running.id()), eq("rejected-response.txt"), eq(raw)))
            .thenReturn("object://owner/rejected");
        when(transactions.rejected(eq(item), eq(execution), eq("object://owner/rejected"), any(), eq(NOW))).thenReturn(true);

        service.providerSucceeded(item, execution, new GenerationProviderResult(raw, 10, 5), 12);

        verify(transactions).rejected(eq(item), eq(execution), eq("object://owner/rejected"),
            eq(List.of("TRUSTED_INSTRUCTION_DISCLOSURE")), eq(NOW));
        verify(transactions, never()).validated(eq(item), eq(execution), any(), any(), any(), any());
        verify(objects, never()).put(eq(owner), any(), eq(running.id()), eq("skill-explanation.json"), any());
        verify(audit, never()).record(eq(AiAuditEvent.GENERATION_COMPLETED), any(), any(), any());
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
    void createsCareerCoachingContextFromOwnedDeterministicOutputs() {
        UUID owner = UUID.randomUUID(), readinessId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        UUID setId = UUID.randomUUID(), templateId = UUID.randomUUID();
        CareerReadinessView readinessView = org.mockito.Mockito.mock(CareerReadinessView.class);
        RecommendationSetView set = org.mockito.Mockito.mock(RecommendationSetView.class);
        LearningRoadmapView roadmap = org.mockito.Mockito.mock(LearningRoadmapView.class);
        var template = new PromptTemplateVersion(templateId, AiGenerationApplicationService.CAREER_COACHING_TASK,
            "v1", "Never calculate scores.", "Return JSON.");
        when(persistence.findByOwnerAndIdempotencyKey(owner, "coaching-key")).thenReturn(Optional.empty());
        when(persistence.loadActiveTemplate(AiGenerationApplicationService.CAREER_COACHING_TASK)).thenReturn(template);
        when(readiness.get(owner, readinessId)).thenReturn(readinessView);
        when(readinessView.status()).thenReturn("COMPLETED");
        when(readinessView.skillMatrixId()).thenReturn(matrixId);
        when(set.careerReadinessId()).thenReturn(readinessId);
        when(set.recommendationSetId()).thenReturn(setId);
        when(recommendations.listRecommendationSets(owner)).thenReturn(new RecommendationSetListView(List.of(set)));
        when(roadmap.recommendationSetId()).thenReturn(setId);
        when(recommendations.listRoadmaps(owner)).thenReturn(new LearningRoadmapListView(List.of(roadmap)));
        when(coachingPrompts.build(readinessView, set, roadmap, template))
            .thenReturn(new PromptPackage("{}", "coaching prompt", "c".repeat(64), 8192));
        when(persistence.saveJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistence.findArtifactIdByPromptContext(any())).thenReturn(Optional.empty());

        service.request(owner, AiGenerationApplicationService.CAREER_COACHING_TASK, List.of(readinessId),
            AiGenerationApplicationService.CAREER_COACHING_OUTPUT, "coaching-key");

        ArgumentCaptor<StoredPromptContext> context = ArgumentCaptor.forClass(StoredPromptContext.class);
        verify(persistence).saveContext(context.capture());
        assertThat(context.getValue().careerReadinessId()).isEqualTo(readinessId);
        assertThat(context.getValue().skillMatrixId()).isEqualTo(matrixId);
        assertThat(context.getValue().analysisId()).isNull();
    }

    @Test
    void createsLearningRecommendationContextFromOwnedRoadmap() {
        UUID owner = UUID.randomUUID(), roadmapId = UUID.randomUUID(), setId = UUID.randomUUID();
        UUID readinessId = UUID.randomUUID(), matrixId = UUID.randomUUID(), templateId = UUID.randomUUID();
        LearningRoadmapView roadmap = org.mockito.Mockito.mock(LearningRoadmapView.class);
        RecommendationSetView set = org.mockito.Mockito.mock(RecommendationSetView.class);
        CareerReadinessView readinessView = org.mockito.Mockito.mock(CareerReadinessView.class);
        var template = new PromptTemplateVersion(templateId, AiGenerationApplicationService.LEARNING_RECOMMENDATION_TASK,
            "v1", "Never calculate scores.", "Return JSON.");
        when(persistence.findByOwnerAndIdempotencyKey(owner, "learning-key")).thenReturn(Optional.empty());
        when(persistence.loadActiveTemplate(AiGenerationApplicationService.LEARNING_RECOMMENDATION_TASK)).thenReturn(template);
        when(recommendations.getRoadmap(owner, roadmapId)).thenReturn(roadmap);
        when(roadmap.roadmapId()).thenReturn(roadmapId);
        when(roadmap.recommendationSetId()).thenReturn(setId);
        when(recommendations.get(owner, setId)).thenReturn(set);
        when(set.careerReadinessId()).thenReturn(readinessId);
        when(readiness.get(owner, readinessId)).thenReturn(readinessView);
        when(readinessView.careerReadinessId()).thenReturn(readinessId);
        when(readinessView.skillMatrixId()).thenReturn(matrixId);
        when(learningPrompts.build(readinessView, set, roadmap, template))
            .thenReturn(new PromptPackage("{}", "learning prompt", "d".repeat(64), 8192));
        when(persistence.saveJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistence.findArtifactIdByPromptContext(any())).thenReturn(Optional.empty());

        service.request(owner, AiGenerationApplicationService.LEARNING_RECOMMENDATION_TASK, List.of(roadmapId),
            AiGenerationApplicationService.LEARNING_RECOMMENDATION_OUTPUT, "learning-key");

        ArgumentCaptor<StoredPromptContext> context = ArgumentCaptor.forClass(StoredPromptContext.class);
        verify(persistence).saveContext(context.capture());
        assertThat(context.getValue().learningRoadmapId()).isEqualTo(roadmapId);
        assertThat(context.getValue().careerReadinessId()).isEqualTo(readinessId);
        assertThat(context.getValue().skillMatrixId()).isEqualTo(matrixId);
    }

    @Test
    void persistsRepositoryReviewOnlyAfterGroundingValidation() {
        UUID owner = UUID.randomUUID(), context = UUID.randomUUID(), matrix = UUID.randomUUID();
        UUID analysisId = UUID.randomUUID(), evaluationId = UUID.randomUUID(), execution = UUID.randomUUID();
        GenerationJob running = GenerationJob.queue(owner, context, "review-key",
            AiGenerationApplicationService.REPOSITORY_REVIEW_TASK, NOW.minusSeconds(1)).start(NOW.minusMillis(500));
        GenerationWorkItem item = new GenerationWorkItem(running, matrix, analysisId, null, null, "prompt", "{}", null);
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

    @Test
    void createsAndPersistsRepositorySummaryFromTheAnalysisSnapshot() {
        UUID owner = UUID.randomUUID(), analysisId = UUID.randomUUID(), repositoryId = UUID.randomUUID(), snapshotId = UUID.randomUUID();
        UUID evaluationId = UUID.randomUUID(), matrixId = UUID.randomUUID(), templateId = UUID.randomUUID(), execution = UUID.randomUUID();
        var analysis = new AnalysisResultView(analysisId, repositoryId, snapshotId, evaluationId, matrixId, "REPOSITORY_BASELINE", true, NOW);
        RepositoryView repository = org.mockito.Mockito.mock(RepositoryView.class);
        RuleEvaluationView evaluation = org.mockito.Mockito.mock(RuleEvaluationView.class);
        var evidence = new RuleEvidenceListView(evaluationId, List.of());
        var technologies = new TechnologySummaryView(repositoryId, snapshotId, "v1", "v1", null, List.of());
        var template = new PromptTemplateVersion(templateId, AiGenerationApplicationService.REPOSITORY_SUMMARY_TASK, "v2", "Never calculate scores.", "Return JSON.");
        var prompt = new PromptPackage("{}", "summary prompt", "e".repeat(64), 8192);
        when(persistence.findByOwnerAndIdempotencyKey(owner, "summary-key")).thenReturn(Optional.empty());
        when(persistence.loadActiveTemplate(AiGenerationApplicationService.REPOSITORY_SUMMARY_TASK)).thenReturn(template);
        when(analyses.getResult(owner, analysisId)).thenReturn(analysis);
        when(repositories.get(owner, repositoryId)).thenReturn(repository);
        when(evaluations.getEvaluation(owner, evaluationId)).thenReturn(evaluation);
        when(evaluations.getEvidence(owner, evaluationId)).thenReturn(evidence);
        when(synchronization.getTechnologies(owner, repositoryId, snapshotId)).thenReturn(technologies);
        when(summaryPrompts.build(repository, analysis, evaluation, List.of(), evidence, template)).thenReturn(prompt);
        when(persistence.saveJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistence.findArtifactIdByPromptContext(any())).thenReturn(Optional.empty());

        GenerationJobView requested = service.request(owner, AiGenerationApplicationService.REPOSITORY_SUMMARY_TASK, List.of(analysisId), AiGenerationApplicationService.REPOSITORY_SUMMARY_OUTPUT, "summary-key");
        verify(synchronization).getTechnologies(owner, repositoryId, snapshotId);
        assertThat(requested.status()).isEqualTo("QUEUED");

        GenerationJob running = GenerationJob.queue(owner, UUID.randomUUID(), "summary-provider", AiGenerationApplicationService.REPOSITORY_SUMMARY_TASK, NOW.minusSeconds(1)).start(NOW.minusMillis(500));
        GenerationWorkItem item = new GenerationWorkItem(running, matrixId, analysisId, null, null, "prompt", "{}", null);
        var content = new RepositorySummaryContent(
            new RepositorySummaryContent.Claim("grounded", List.of(), List.of("rule-output")), List.of(), List.of(), List.of()
        );
        when(transactions.providerCompleted(item, execution, new GenerationProviderResult("raw", 10, 5), 12, NOW)).thenReturn(Optional.of(running));
        when(summaryValidator.validate("raw", List.of(), evaluation, evidence)).thenReturn(new RepositorySummaryValidation(content, List.of()));
        when(objects.put(eq(owner), any(), eq(running.id()), eq("repository-summary.json"), any())).thenReturn("object://owner/summary");
        when(transactions.validated(eq(item), eq(execution), any(), eq("object://owner/summary"), any(), eq(NOW))).thenReturn(true);

        service.providerSucceeded(item, execution, new GenerationProviderResult("raw", 10, 5), 12);

        verify(transactions).validated(eq(item), eq(execution), any(), eq("object://owner/summary"), any(), eq(NOW));
    }

    @Test
    void rejectsUngroundedRepositorySummaryWithoutCreatingAnArtifact() {
        UUID owner = UUID.randomUUID(), context = UUID.randomUUID(), matrixId = UUID.randomUUID(), analysisId = UUID.randomUUID();
        UUID evaluationId = UUID.randomUUID(), execution = UUID.randomUUID();
        GenerationJob running = GenerationJob.queue(owner, context, "summary-rejected", AiGenerationApplicationService.REPOSITORY_SUMMARY_TASK, NOW.minusSeconds(1)).start(NOW.minusMillis(500));
        GenerationWorkItem item = new GenerationWorkItem(running, matrixId, analysisId, null, null, "prompt", "{}", null);
        var analysis = new AnalysisResultView(analysisId, UUID.randomUUID(), UUID.randomUUID(), evaluationId, matrixId, "REPOSITORY_BASELINE", true, NOW);
        RuleEvaluationView evaluation = org.mockito.Mockito.mock(RuleEvaluationView.class);
        var evidence = new RuleEvidenceListView(evaluationId, List.of());
        when(transactions.providerCompleted(item, execution, new GenerationProviderResult("raw", 10, 5), 12, NOW)).thenReturn(Optional.of(running));
        when(analyses.getResult(owner, analysisId)).thenReturn(analysis);
        when(evaluations.getEvaluation(owner, evaluationId)).thenReturn(evaluation);
        when(evaluations.getEvidence(owner, evaluationId)).thenReturn(evidence);
        when(synchronization.getTechnologies(owner, analysis.repositoryId(), analysis.snapshotId())).thenReturn(new TechnologySummaryView(analysis.repositoryId(), analysis.snapshotId(), "v1", "v1", null, List.of()));
        when(summaryValidator.validate("raw", List.of(), evaluation, evidence)).thenReturn(new RepositorySummaryValidation(null, List.of("MISSING_GROUNDING_REFERENCE")));
        when(objects.put(eq(owner), any(), eq(running.id()), eq("rejected-response.txt"), eq("raw"))).thenReturn("object://owner/rejected");
        when(transactions.rejected(eq(item), eq(execution), eq("object://owner/rejected"), any(), eq(NOW))).thenReturn(true);

        service.providerSucceeded(item, execution, new GenerationProviderResult("raw", 10, 5), 12);

        verify(transactions).rejected(eq(item), eq(execution), eq("object://owner/rejected"), any(), eq(NOW));
        verify(transactions, never()).validated(eq(item), eq(execution), any(), any(), any(), any());
    }

    @Test
    void readsValidatedRepositorySummaryArtifactsAsSummaryContent() {
        UUID owner = UUID.randomUUID(), artifactId = UUID.randomUUID();
        var record = new ArtifactRecord(artifactId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null,
            AiGenerationApplicationService.REPOSITORY_SUMMARY_OUTPUT, "object://owner/summary", "v2", "OLLAMA", "qwen-test",
            "a".repeat(64), "PASSED", RepositorySummaryValidator.VERSION, List.of(), NOW, NOW);
        when(transactions.artifact(owner, artifactId)).thenReturn(record);
        when(objects.read(owner, "object://owner/summary")).thenReturn("{\"summary\":{\"text\":\"grounded\",\"evidenceReferences\":[],\"ruleOutputReferences\":[\"rule-output\"]},\"strengths\":[],\"improvementAreas\":[],\"technologies\":[]}");

        GeneratedArtifactView artifact = service.getArtifact(owner, artifactId);

        assertThat(artifact.type()).isEqualTo(AiGenerationApplicationService.REPOSITORY_SUMMARY_OUTPUT);
        assertThat(artifact.content()).isInstanceOf(RepositorySummaryContent.class);
    }

    @ParameterizedTest
    @MethodSource("foreignSourceRequests")
    void rejectsForeignSourceReferencesBeforePersistingAJob(
        String taskType, String outputType, RuntimeException notFound
    ) {
        UUID owner = UUID.randomUUID(), foreignSource = UUID.randomUUID();
        when(persistence.findByOwnerAndIdempotencyKey(owner, "foreign-source")).thenReturn(Optional.empty());
        when(persistence.loadActiveTemplate(taskType)).thenReturn(new PromptTemplateVersion(UUID.randomUUID(), taskType,
            "v1", "trusted", "json"));
        switch (taskType) {
            case AiGenerationApplicationService.SKILL_EXPLANATION_TASK ->
                when(matrices.get(owner, foreignSource)).thenThrow(notFound);
            case AiGenerationApplicationService.REPOSITORY_REVIEW_TASK,
                 AiGenerationApplicationService.REPOSITORY_SUMMARY_TASK ->
                when(analyses.getResult(owner, foreignSource)).thenThrow(notFound);
            case AiGenerationApplicationService.CAREER_COACHING_TASK ->
                when(readiness.get(owner, foreignSource)).thenThrow(notFound);
            default -> when(recommendations.getRoadmap(owner, foreignSource)).thenThrow(notFound);
        }

        assertThatThrownBy(() -> service.request(owner, taskType, List.of(foreignSource), outputType, "foreign-source"))
            .isSameAs(notFound);

        verify(persistence, never()).saveContext(any());
        verify(persistence, never()).saveJob(any());
    }

    static java.util.stream.Stream<Arguments> foreignSourceRequests() {
        return java.util.stream.Stream.of(
            Arguments.of(AiGenerationApplicationService.SKILL_EXPLANATION_TASK, AiGenerationApplicationService.SKILL_EXPLANATION_OUTPUT, new SkillMatrixNotFoundException()),
            Arguments.of(AiGenerationApplicationService.REPOSITORY_REVIEW_TASK, AiGenerationApplicationService.REPOSITORY_REVIEW_OUTPUT, new AnalysisNotFoundException()),
            Arguments.of(AiGenerationApplicationService.REPOSITORY_SUMMARY_TASK, AiGenerationApplicationService.REPOSITORY_SUMMARY_OUTPUT, new AnalysisNotFoundException()),
            Arguments.of(AiGenerationApplicationService.CAREER_COACHING_TASK, AiGenerationApplicationService.CAREER_COACHING_OUTPUT, new CareerReadinessNotFoundException()),
            Arguments.of(AiGenerationApplicationService.LEARNING_RECOMMENDATION_TASK, AiGenerationApplicationService.LEARNING_RECOMMENDATION_OUTPUT, new RecommendationNotFoundException())
        );
    }

    @Test
    void scopesIdempotencyKeysToTheirOwners() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), matrix = UUID.randomUUID(), evaluation = UUID.randomUUID();
        SkillMatrixView matrixView = org.mockito.Mockito.mock(SkillMatrixView.class);
        RuleEvaluationView evaluationView = org.mockito.Mockito.mock(RuleEvaluationView.class);
        var template = new PromptTemplateVersion(UUID.randomUUID(), AiGenerationApplicationService.SKILL_EXPLANATION_TASK, "v1", "trusted", "json");
        when(persistence.findByOwnerAndIdempotencyKey(first, "shared-key")).thenReturn(Optional.empty());
        when(persistence.findByOwnerAndIdempotencyKey(second, "shared-key")).thenReturn(Optional.empty());
        when(persistence.loadActiveTemplate(AiGenerationApplicationService.SKILL_EXPLANATION_TASK)).thenReturn(template);
        when(matrices.get(first, matrix)).thenReturn(matrixView);
        when(matrices.get(second, matrix)).thenReturn(matrixView);
        when(matrixView.evaluationId()).thenReturn(evaluation);
        when(evaluations.getEvaluation(first, evaluation)).thenReturn(evaluationView);
        when(evaluations.getEvaluation(second, evaluation)).thenReturn(evaluationView);
        when(evaluations.getEvidence(eq(first), eq(evaluation))).thenReturn(new RuleEvidenceListView(evaluation, List.of()));
        when(evaluations.getEvidence(eq(second), eq(evaluation))).thenReturn(new RuleEvidenceListView(evaluation, List.of()));
        when(prompts.build(eq(matrixView), eq(evaluationView), any(), eq(template)))
            .thenReturn(new PromptPackage("{}", "prompt", "a".repeat(64), 4096));
        when(persistence.saveJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistence.findArtifactIdByPromptContext(any())).thenReturn(Optional.empty());

        service.request(first, AiGenerationApplicationService.SKILL_EXPLANATION_TASK, List.of(matrix), AiGenerationApplicationService.SKILL_EXPLANATION_OUTPUT, "shared-key");
        service.request(second, AiGenerationApplicationService.SKILL_EXPLANATION_TASK, List.of(matrix), AiGenerationApplicationService.SKILL_EXPLANATION_OUTPUT, "shared-key");

        verify(persistence).findByOwnerAndIdempotencyKey(first, "shared-key");
        verify(persistence).findByOwnerAndIdempotencyKey(second, "shared-key");
        ArgumentCaptor<GenerationJob> jobs = ArgumentCaptor.forClass(GenerationJob.class);
        verify(persistence, org.mockito.Mockito.times(2)).saveJob(jobs.capture());
        assertThat(jobs.getAllValues()).extracting(GenerationJob::userId).containsExactly(first, second);
    }

    @Test
    void hidesForeignJobsArtifactsAndValidationRecords() {
        UUID owner = UUID.randomUUID(), foreignJob = UUID.randomUUID(), foreignArtifact = UUID.randomUUID();
        when(persistence.findJob(owner, foreignJob)).thenReturn(Optional.empty());
        when(transactions.artifact(owner, foreignArtifact)).thenThrow(new GenerationNotFoundException());
        when(persistence.findArtifact(owner, foreignArtifact)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJob(owner, foreignJob)).isInstanceOf(GenerationNotFoundException.class);
        assertThatThrownBy(() -> service.cancel(owner, foreignJob)).isInstanceOf(GenerationNotFoundException.class);
        assertThatThrownBy(() -> service.getArtifact(owner, foreignArtifact)).isInstanceOf(GenerationNotFoundException.class);
        assertThatThrownBy(() -> service.getValidation(owner, foreignArtifact)).isInstanceOf(GenerationNotFoundException.class);

        verify(persistence, never()).saveJob(any());
        verify(objects, never()).read(any(), any());
    }
}
