package com.devpath.ai.application;

import com.devpath.ai.config.AiGenerationProperties;
import com.devpath.ai.domain.GenerationJob;
import com.devpath.ai.domain.GenerationJobStatus;
import com.devpath.analysis.application.AnalysisApplicationService;
import com.devpath.prompt.application.SkillExplanationPromptBuilder;
import com.devpath.prompt.application.RepositoryReviewPromptBuilder;
import com.devpath.prompt.application.PromptPackage;
import com.devpath.repository.application.RepositoryApplicationService;
import com.devpath.rule.application.CompletedRuleEvaluationApplicationService;
import com.devpath.rule.application.SkillMatrixApplicationService;
import com.devpath.shared.application.ObjectContentPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiGenerationApplicationService {
    public static final String SKILL_EXPLANATION_TASK = "SKILL_ANALYSIS_EXPLANATION";
    public static final String SKILL_EXPLANATION_OUTPUT = "SKILL_EXPLANATION";
    public static final String REPOSITORY_REVIEW_TASK = "REPOSITORY_REVIEW";
    public static final String REPOSITORY_REVIEW_OUTPUT = "REPOSITORY_REVIEW";
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");
    private final GenerationPersistencePort persistence;
    private final SkillMatrixApplicationService matrices;
    private final SkillExplanationPromptBuilder prompts;
    private final SkillExplanationValidator validator;
    private final AnalysisApplicationService analyses;
    private final RepositoryApplicationService repositories;
    private final CompletedRuleEvaluationApplicationService evaluations;
    private final RepositoryReviewPromptBuilder reviewPrompts;
    private final RepositoryReviewValidator reviewValidator;
    private final ObjectContentPort objects;
    private final AiGenerationProperties properties;
    private final AiAuditPort audit;
    private final AiGenerationTransactionService transactions;
    private final ObjectMapper mapper;
    private final Clock clock;

    public AiGenerationApplicationService(
        GenerationPersistencePort persistence, SkillMatrixApplicationService matrices,
        SkillExplanationPromptBuilder prompts, SkillExplanationValidator validator,
        AnalysisApplicationService analyses, RepositoryApplicationService repositories,
        CompletedRuleEvaluationApplicationService evaluations, RepositoryReviewPromptBuilder reviewPrompts,
        RepositoryReviewValidator reviewValidator, ObjectContentPort objects,
        AiGenerationProperties properties, AiAuditPort audit, AiGenerationTransactionService transactions,
        ObjectMapper mapper, Clock clock
    ) {
        this.persistence = persistence; this.matrices = matrices; this.prompts = prompts; this.validator = validator;
        this.analyses = analyses; this.repositories = repositories; this.evaluations = evaluations;
        this.reviewPrompts = reviewPrompts; this.reviewValidator = reviewValidator;
        this.objects = objects; this.properties = properties; this.audit = audit; this.transactions = transactions;
        this.mapper = mapper; this.clock = clock;
    }

    @Transactional
    public GenerationJobView request(
        UUID userId, String taskType, List<UUID> sourceResourceRefs, String outputType, String idempotencyKey
    ) {
        validateRequest(taskType, sourceResourceRefs, outputType, idempotencyKey);
        UUID sourceId = sourceResourceRefs.getFirst();
        Optional<GenerationJob> repeated = persistence.findByOwnerAndIdempotencyKey(userId, idempotencyKey);
        if (repeated.isPresent()) {
            if (!repeated.get().taskType().equals(taskType)
                || !persistence.findSourceResourceId(repeated.get().promptContextId()).equals(sourceId)) {
                throw new IllegalArgumentException("Idempotency key belongs to another generation request");
            }
            return view(repeated.get());
        }
        UUID matrixId;
        UUID analysisId = null;
        PromptPackage prompt;
        var template = persistence.loadActiveTemplate(taskType);
        if (SKILL_EXPLANATION_TASK.equals(taskType)) {
            matrixId = sourceId;
            prompt = prompts.build(matrices.get(userId, matrixId), template);
        } else {
            var analysis = analyses.getResult(userId, sourceId);
            analysisId = analysis.analysisId();
            matrixId = analysis.skillMatrixId();
            var evaluation = evaluations.getEvaluation(userId, analysis.evaluationId());
            prompt = reviewPrompts.build(repositories.get(userId, analysis.repositoryId()), analysis, evaluation,
                evaluations.getEvidence(userId, analysis.evaluationId()), template);
        }
        Instant now = clock.instant();
        UUID contextId = UUID.randomUUID();
        persistence.saveContext(new StoredPromptContext(contextId, userId, template.id(), matrixId, analysisId, taskType,
            prompt.tokenBudget(), prompt.contextHash(), prompt.contextPayload(), prompt.prompt(), now));
        GenerationJob job = persistence.saveJob(GenerationJob.queue(userId, contextId, idempotencyKey, taskType, now));
        audit.record(AiAuditEvent.GENERATION_REQUESTED, userId, job.id(), now);
        return view(job);
    }

    @Transactional(readOnly = true)
    public GenerationJobView getJob(UUID userId, UUID jobId) {
        return view(job(userId, jobId));
    }

    @Transactional
    public GenerationJobView cancel(UUID userId, UUID jobId) {
        GenerationJob current = job(userId, jobId);
        GenerationJob canceled = persistence.saveJob(current.cancel(clock.instant()));
        if (canceled.status() == GenerationJobStatus.CANCELED && current.status() != GenerationJobStatus.CANCELED) {
            audit.record(AiAuditEvent.GENERATION_CANCELED, userId, jobId, clock.instant());
        }
        return view(canceled);
    }

    @Transactional
    public Optional<GenerationWorkItem> claim() {
        return persistence.claim(clock.instant());
    }

    @Transactional
    public UUID beginExecution(GenerationWorkItem item) {
        return persistence.saveSubmittedExecution(item, "OLLAMA", properties.model(), clock.instant());
    }

    @Transactional
    public void providerFailed(GenerationWorkItem item, UUID executionId, String code, long latencyMs) {
        Instant now = clock.instant();
        persistence.failExecution(executionId, code, latencyMs, now);
        GenerationJob current = job(item.job().userId(), item.job().id());
        if (current.status() != GenerationJobStatus.RUNNING) return;
        GenerationJob failed = persistence.saveJob(current.retryOrFail(code, properties.maxAttempts(), now));
        if (failed.status() == GenerationJobStatus.FAILED) {
            audit.record(AiAuditEvent.GENERATION_FAILED, failed.userId(), failed.id(), now);
        }
    }

    public void providerSucceeded(
        GenerationWorkItem item, UUID executionId, GenerationProviderResult result, long latencyMs
    ) {
        Instant now = clock.instant();
        Optional<GenerationJob> currentValue = transactions.providerCompleted(item, executionId, result, latencyMs, now);
        if (currentValue.isEmpty()) return;
        GenerationJob current = currentValue.get();
        Object content;
        List<String> violations;
        String fileName;
        if (REPOSITORY_REVIEW_TASK.equals(item.job().taskType())) {
            var analysis = analyses.getResult(item.job().userId(), item.analysisId());
            var validation = reviewValidator.validate(result.content(),
                evaluations.getEvidence(item.job().userId(), analysis.evaluationId()));
            content = validation.content(); violations = validation.violations(); fileName = "repository-review.json";
        } else {
            var validation = validator.validate(result.content(), matrices.get(item.job().userId(), item.skillMatrixId()));
            content = validation.content(); violations = validation.violations(); fileName = "skill-explanation.json";
        }
        if (content == null || !violations.isEmpty()) {
            boolean recorded = storeRejected(item, executionId, result.content(), violations, now);
            if (recorded && item.job().attemptCount() >= properties.maxAttempts())
                audit.record(AiAuditEvent.GENERATION_FAILED, current.userId(), current.id(), now);
            return;
        }
        UUID artifactId = UUID.randomUUID();
        String reference = objects.put(current.userId(), artifactId, current.id(), fileName, json(content));
        try {
            if (transactions.validated(item, executionId, artifactId, reference, result, now)) {
                audit.record(AiAuditEvent.GENERATION_COMPLETED, current.userId(), artifactId, now);
            } else {
                objects.deleteVersion(current.userId(), artifactId, current.id());
            }
        } catch (RuntimeException exception) {
            objects.deleteVersion(current.userId(), artifactId, current.id());
            throw exception;
        }
    }

    public GeneratedArtifactView getArtifact(UUID userId, UUID artifactId) {
        ArtifactRecord artifact = transactions.artifact(userId, artifactId);
        Object content = read(objects.read(userId, artifact.contentReference()), artifact.artifactType());
        audit.record(AiAuditEvent.GENERATED_ARTIFACT_VIEWED, userId, artifactId, clock.instant());
        return artifactView(artifact, content);
    }

    @Transactional(readOnly = true)
    public ResponseValidationView getValidation(UUID userId, UUID artifactId) {
        ArtifactRecord artifact = persistence.findArtifact(userId, artifactId).orElseThrow(GenerationNotFoundException::new);
        return validationView(artifact);
    }

    private boolean storeRejected(
        GenerationWorkItem item, UUID executionId, String content, List<String> violations, Instant now
    ) {
        UUID responseResourceId = UUID.randomUUID();
        String retained = content.length() <= 20_000 ? content : content.substring(0, 20_000);
        String reference = objects.put(item.job().userId(), responseResourceId, item.job().id(), "rejected-response.txt", retained);
        try {
            if (!transactions.rejected(item, executionId, reference, violations, now)) {
                objects.deleteVersion(item.job().userId(), responseResourceId, item.job().id());
                return false;
            }
            return true;
        } catch (RuntimeException exception) {
            objects.deleteVersion(item.job().userId(), responseResourceId, item.job().id());
            throw exception;
        }
    }

    private GenerationJob job(UUID userId, UUID jobId) {
        return persistence.findJob(userId, jobId).orElseThrow(GenerationNotFoundException::new);
    }

    private GenerationJobView view(GenerationJob job) {
        return GenerationJobView.from(job, persistence.findArtifactIdByPromptContext(job.promptContextId()).orElse(null));
    }

    private GeneratedArtifactView artifactView(ArtifactRecord value, Object content) {
        return new GeneratedArtifactView(value.artifactId(), value.artifactType(), "VALIDATED",
            new GeneratedArtifactView.Provenance(value.skillMatrixId(), value.analysisId(), value.promptContextId(),
                value.templateVersion(), value.provider(), value.model(), value.contextHash(), value.createdAt()),
            validationView(value), "/api/v1/generated-artifacts/" + value.artifactId(), content);
    }

    private static ResponseValidationView validationView(ArtifactRecord value) {
        return new ResponseValidationView(value.validationStatus(), value.validatorVersion(),
            value.validatedAt(), value.violations());
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Artifact could not be serialized", exception); }
    }

    private Object read(String value, String artifactType) {
        try { return REPOSITORY_REVIEW_OUTPUT.equals(artifactType)
            ? mapper.readValue(value, RepositoryReviewContent.class)
            : mapper.readValue(value, SkillExplanationContent.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Artifact content is invalid", exception); }
    }

    private static void validateRequest(
        String taskType, List<UUID> sourceResourceRefs, String outputType, String idempotencyKey
    ) {
        boolean supported = SKILL_EXPLANATION_TASK.equals(taskType) && SKILL_EXPLANATION_OUTPUT.equals(outputType)
            || REPOSITORY_REVIEW_TASK.equals(taskType) && REPOSITORY_REVIEW_OUTPUT.equals(outputType);
        if (!supported || sourceResourceRefs == null
            || sourceResourceRefs.size() != 1 || sourceResourceRefs.getFirst() == null || idempotencyKey == null
            || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw new IllegalArgumentException("Generation request is invalid");
        }
    }
}
