package com.devpath.ai.application;

import com.devpath.ai.domain.GenerationJob;
import java.time.Instant;
import java.util.UUID;

public record GenerationJobView(
    UUID jobId, String status, String validationStatus, String artifactUrl,
    String failureCode, Instant createdAt, Instant completedAt
) {
    static GenerationJobView from(GenerationJob job, UUID artifactId) {
        return new GenerationJobView(job.id(), job.status().name(), job.validationStatus(),
            artifactId == null ? null : "/api/v1/generated-artifacts/" + artifactId,
            job.failureCode(), job.requestedAt(), job.completedAt());
    }
}
