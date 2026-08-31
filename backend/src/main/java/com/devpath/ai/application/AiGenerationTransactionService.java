package com.devpath.ai.application;

import com.devpath.ai.domain.GenerationJob;
import com.devpath.ai.domain.GenerationJobStatus;
import com.devpath.ai.config.AiGenerationProperties;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AiGenerationTransactionService {
    private final GenerationPersistencePort persistence;
    private final AiGenerationProperties properties;

    AiGenerationTransactionService(GenerationPersistencePort persistence, AiGenerationProperties properties) {
        this.persistence = persistence; this.properties = properties;
    }

    @Transactional
    Optional<GenerationJob> providerCompleted(
        GenerationWorkItem item, UUID executionId, GenerationProviderResult result, long latencyMs, Instant now
    ) {
        persistence.completeExecution(executionId, latencyMs, result.promptTokens(), result.completionTokens(), now);
        return persistence.findJob(item.job().userId(), item.job().id())
            .filter(job -> job.status() == GenerationJobStatus.RUNNING);
    }

    @Transactional
    boolean validated(
        GenerationWorkItem item, UUID executionId, UUID artifactId, String reference,
        GenerationProviderResult result, Instant now
    ) {
        if (persistence.findJob(item.job().userId(), item.job().id())
            .filter(job -> job.status() == GenerationJobStatus.RUNNING).isEmpty()) return false;
        persistence.saveValidatedArtifact(item, executionId, artifactId, reference, now);
        return true;
    }

    @Transactional
    boolean rejected(
        GenerationWorkItem item, UUID executionId, String reference, List<String> violations, Instant now
    ) {
        Optional<GenerationJob> current = persistence.findJob(item.job().userId(), item.job().id())
            .filter(job -> job.status() == GenerationJobStatus.RUNNING);
        if (current.isEmpty()) return false;
        persistence.recordRejectedResponse(item, executionId, reference, violations, now);
        persistence.saveJob(current.get().rejectOrRetry(properties.maxAttempts(), now));
        return true;
    }

    @Transactional(readOnly = true)
    ArtifactRecord artifact(UUID userId, UUID artifactId) {
        return persistence.findArtifact(userId, artifactId).orElseThrow(GenerationNotFoundException::new);
    }
}
