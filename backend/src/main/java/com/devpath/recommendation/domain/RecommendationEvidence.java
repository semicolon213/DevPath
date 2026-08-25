package com.devpath.recommendation.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecommendationEvidence(
    UUID evidenceId,
    String evidenceType,
    String sourceReference,
    String observedFactSummary,
    BigDecimal confidence,
    Instant createdAt
) {}
