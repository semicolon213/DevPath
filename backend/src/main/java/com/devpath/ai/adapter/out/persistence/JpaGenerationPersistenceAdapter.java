package com.devpath.ai.adapter.out.persistence;

import com.devpath.ai.application.ArtifactRecord;
import com.devpath.ai.application.GenerationPersistencePort;
import com.devpath.ai.application.GenerationWorkItem;
import com.devpath.ai.application.StoredPromptContext;
import com.devpath.ai.domain.GenerationJob;
import com.devpath.ai.domain.GenerationJobStatus;
import com.devpath.prompt.application.PromptTemplateVersion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaGenerationPersistenceAdapter implements GenerationPersistencePort {
    static final String VALIDATOR_VERSION = "skill-explanation-validator-v1";
    private final PromptTemplateVersionJpaRepository templates;
    private final PromptContextJpaRepository contexts;
    private final AiTaskJpaRepository tasks;
    private final ModelExecutionJpaRepository executions;
    private final AiResponseJpaRepository responses;
    private final ResponseValidationResultJpaRepository validations;
    private final GeneratedArtifactJpaRepository artifacts;

    JpaGenerationPersistenceAdapter(
        PromptTemplateVersionJpaRepository templates, PromptContextJpaRepository contexts,
        AiTaskJpaRepository tasks, ModelExecutionJpaRepository executions, AiResponseJpaRepository responses,
        ResponseValidationResultJpaRepository validations, GeneratedArtifactJpaRepository artifacts
    ) {
        this.templates = templates; this.contexts = contexts; this.tasks = tasks; this.executions = executions;
        this.responses = responses; this.validations = validations; this.artifacts = artifacts;
    }

    @Override
    public PromptTemplateVersion loadActiveTemplate(String taskType) {
        var value = templates.findFirstByTaskTypeAndStatusOrderByCreatedAtDesc(taskType, "ACTIVE")
            .orElseThrow(() -> new IllegalStateException("Active prompt template is unavailable"));
        return new PromptTemplateVersion(value.id, value.taskType, value.versionLabel,
            value.systemPrompt, value.outputFormatPrompt);
    }

    @Override
    public void saveContext(StoredPromptContext value) {
        contexts.save(new PromptContextJpaEntity(value));
    }

    @Override
    public Optional<GenerationJob> findByOwnerAndIdempotencyKey(UUID userId, String idempotencyKey) {
        return tasks.findByUserIdAndIdempotencyKey(userId, idempotencyKey).map(this::toDomain);
    }

    @Override
    public GenerationJob saveJob(GenerationJob value) {
        AiTaskJpaEntity entity = tasks.findById(value.id()).orElseGet(AiTaskJpaEntity::new);
        entity.id = value.id(); entity.userId = value.userId(); entity.promptContextId = value.promptContextId();
        entity.idempotencyKey = value.idempotencyKey(); entity.taskType = value.taskType();
        entity.status = value.status().name(); entity.validationStatus = value.validationStatus();
        entity.attemptCount = value.attemptCount(); entity.failureCode = value.failureCode();
        entity.requestedAt = value.requestedAt(); entity.startedAt = value.startedAt(); entity.completedAt = value.completedAt();
        return toDomain(tasks.save(entity));
    }

    @Override
    public Optional<GenerationJob> findJob(UUID userId, UUID jobId) {
        return tasks.findByIdAndUserId(jobId, userId).map(this::toDomain);
    }

    @Override
    public UUID findSkillMatrixId(UUID promptContextId) {
        return contexts.findById(promptContextId).orElseThrow().skillMatrixId;
    }

    @Override
    public Optional<GenerationWorkItem> claim(Instant now) {
        return tasks.findFirstByStatusOrderByRequestedAtAsc("QUEUED").map(entity -> {
            GenerationJob started = saveJob(toDomain(entity).start(now));
            PromptContextJpaEntity context = contexts.findById(started.promptContextId())
                .orElseThrow(() -> new IllegalStateException("Prompt context is unavailable"));
            return new GenerationWorkItem(started, context.skillMatrixId, context.providerPrompt);
        });
    }

    @Override
    public UUID saveSubmittedExecution(GenerationWorkItem item, String provider, String model, Instant submittedAt) {
        var entity = new ModelExecutionJpaEntity();
        entity.id = UUID.randomUUID(); entity.taskId = item.job().id(); entity.provider = provider; entity.model = model;
        entity.attemptNumber = item.job().attemptCount(); entity.status = "SUBMITTED"; entity.submittedAt = submittedAt;
        return executions.save(entity).id;
    }

    @Override
    public void completeExecution(
        UUID executionId, long latencyMs, Integer promptTokens, Integer completionTokens, Instant now
    ) {
        var entity = executions.findById(executionId).orElseThrow();
        entity.status = "COMPLETED"; entity.latencyMs = latencyMs; entity.promptTokens = promptTokens;
        entity.completionTokens = completionTokens; entity.completedAt = now;
        executions.save(entity);
    }

    @Override
    public void failExecution(UUID executionId, String failureCode, long latencyMs, Instant now) {
        var entity = executions.findById(executionId).orElseThrow();
        entity.status = "FAILED"; entity.failureCode = failureCode; entity.latencyMs = latencyMs; entity.completedAt = now;
        executions.save(entity);
    }

    @Override
    public void recordRejectedResponse(
        GenerationWorkItem item, UUID executionId, String contentReference, List<String> violations, Instant now
    ) {
        AiResponseJpaEntity response = response(item, executionId, contentReference, "REJECTED", now);
        validation(response.id, "REJECTED", violations, now);
    }

    @Override
    public void saveValidatedArtifact(
        GenerationWorkItem item, UUID executionId, UUID artifactId, String contentReference,
        Instant now
    ) {
        AiResponseJpaEntity response = response(item, executionId, contentReference, "PASSED", now);
        validation(response.id, "PASSED", List.of(), now);
        var artifact = new GeneratedArtifactJpaEntity();
        artifact.id = artifactId; artifact.userId = item.job().userId(); artifact.responseId = response.id;
        artifact.promptContextId = item.job().promptContextId(); artifact.artifactType = "SKILL_EXPLANATION";
        artifact.status = "VALIDATED"; artifact.contentReference = contentReference; artifact.createdAt = now;
        artifacts.save(artifact);
        saveJob(item.job().succeed(now));
    }

    @Override
    public Optional<UUID> findArtifactIdByPromptContext(UUID promptContextId) {
        return artifacts.findByPromptContextId(promptContextId).map(value -> value.id);
    }

    @Override
    public Optional<ArtifactRecord> findArtifact(UUID userId, UUID artifactId) {
        return artifacts.findByIdAndUserId(artifactId, userId).map(artifact -> {
            PromptContextJpaEntity context = contexts.findById(artifact.promptContextId).orElseThrow();
            PromptTemplateVersionJpaEntity template = templates.findById(context.templateVersionId).orElseThrow();
            AiResponseJpaEntity response = responses.findById(artifact.responseId).orElseThrow();
            ModelExecutionJpaEntity execution = executions.findById(response.modelExecutionId).orElseThrow();
            ResponseValidationResultJpaEntity validation = validations.findByResponseId(response.id).orElseThrow();
            return new ArtifactRecord(artifact.id, artifact.promptContextId, context.skillMatrixId,
                artifact.artifactType, artifact.contentReference, template.versionLabel, execution.provider,
                execution.model, context.contextHash, validation.status, validation.validatorVersion,
                validation.violations, validation.validatedAt, artifact.createdAt);
        });
    }

    private AiResponseJpaEntity response(
        GenerationWorkItem item, UUID executionId, String reference, String status, Instant now
    ) {
        var response = new AiResponseJpaEntity();
        response.id = UUID.randomUUID(); response.modelExecutionId = executionId;
        response.promptContextId = item.job().promptContextId(); response.validationStatus = status;
        response.contentReference = reference; response.receivedAt = now;
        return responses.save(response);
    }

    private void validation(UUID responseId, String status, List<String> violations, Instant now) {
        var value = new ResponseValidationResultJpaEntity();
        value.id = UUID.randomUUID(); value.responseId = responseId; value.status = status;
        value.validatorVersion = VALIDATOR_VERSION; value.violations = List.copyOf(violations); value.validatedAt = now;
        validations.save(value);
    }

    private GenerationJob toDomain(AiTaskJpaEntity value) {
        return new GenerationJob(value.id, value.userId, value.promptContextId, value.idempotencyKey, value.taskType,
            GenerationJobStatus.valueOf(value.status), value.validationStatus, value.attemptCount, value.failureCode,
            value.requestedAt, value.startedAt, value.completedAt);
    }

}
