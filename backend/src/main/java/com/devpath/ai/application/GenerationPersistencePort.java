package com.devpath.ai.application;

import com.devpath.ai.domain.GenerationJob;
import com.devpath.prompt.application.PromptTemplateVersion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationPersistencePort {
    PromptTemplateVersion loadActiveTemplate(String taskType);
    void saveContext(StoredPromptContext context);
    Optional<GenerationJob> findByOwnerAndIdempotencyKey(UUID userId, String idempotencyKey);
    GenerationJob saveJob(GenerationJob job);
    Optional<GenerationJob> findJob(UUID userId, UUID jobId);
    UUID findSkillMatrixId(UUID promptContextId);
    Optional<GenerationWorkItem> claim(Instant now);
    UUID saveSubmittedExecution(GenerationWorkItem item, String provider, String model, Instant submittedAt);
    void completeExecution(UUID executionId, long latencyMs, Integer promptTokens, Integer completionTokens, Instant now);
    void failExecution(UUID executionId, String failureCode, long latencyMs, Instant now);
    void recordRejectedResponse(GenerationWorkItem item, UUID executionId, String contentReference,
        List<String> violations, Instant now);
    void saveValidatedArtifact(GenerationWorkItem item, UUID executionId, UUID artifactId,
        String contentReference, Instant now);
    Optional<UUID> findArtifactIdByPromptContext(UUID promptContextId);
    Optional<ArtifactRecord> findArtifact(UUID userId, UUID artifactId);
}
