package com.devpath.ai.application;

import java.time.Instant;
import java.util.UUID;

public record GeneratedArtifactView(
    UUID artifactId, String type, String status, Provenance provenance,
    ResponseValidationView validation, String contentRef, Object content
) {
    public record Provenance(
        UUID skillMatrixId, UUID analysisId, UUID promptContextId, String templateVersion, String provider,
        String model, String contextHash, Instant generatedAt
    ) {}
}
