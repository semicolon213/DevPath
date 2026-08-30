package com.devpath.knowledge.application;

import com.devpath.knowledge.domain.KnowledgeIngestionJob;
import java.time.Instant;
import java.util.UUID;

public record KnowledgeIngestionJobView(
    UUID jobId, String jobType, String status, String phase, int progressPercent, int attemptCount,
    int maxAttempts, Instant submittedAt, Instant startedAt, Instant completedAt, String pollingUrl,
    String resultResourceUrl, String errorCode, String errorMessage, boolean retryable
) {
    static KnowledgeIngestionJobView from(KnowledgeIngestionJob job) {
        return new KnowledgeIngestionJobView(job.id(), "KNOWLEDGE_INGESTION", job.status().name().toLowerCase(),
            job.phase(), job.progressPercent(), job.attemptCount(), job.maxAttempts(), job.submittedAt(),
            job.startedAt(), job.completedAt(), "/api/v1/knowledge-ingestion-jobs/" + job.id(),
            job.status().name().equals("SUCCEEDED") ? "/api/v1/knowledge-documents/" + job.documentId() : null,
            job.errorCode(), job.errorMessage(), job.status().name().equals("QUEUED") ||
                job.status().name().equals("RUNNING") || job.attemptCount() < job.maxAttempts());
    }
}
