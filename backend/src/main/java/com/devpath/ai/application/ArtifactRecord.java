package com.devpath.ai.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArtifactRecord(
    UUID artifactId, UUID promptContextId, UUID skillMatrixId, String artifactType,
    String contentReference, String templateVersion, String provider, String model, String contextHash,
    String validationStatus, String validatorVersion, List<String> violations, Instant validatedAt, Instant createdAt
) {}
