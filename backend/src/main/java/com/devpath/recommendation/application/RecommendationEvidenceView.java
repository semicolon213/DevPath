package com.devpath.recommendation.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecommendationEvidenceView(
    UUID evidenceId,
    String evidenceType,
    String sourceReference,
    String observedFactSummary,
    BigDecimal confidence,
    Instant createdAt
) {}
