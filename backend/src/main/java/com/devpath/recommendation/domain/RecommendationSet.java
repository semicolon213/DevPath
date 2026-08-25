package com.devpath.recommendation.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecommendationSet(
    UUID recommendationSetId,
    UUID userId,
    UUID careerReadinessId,
    UUID policyId,
    String policyVersion,
    String status,
    List<Recommendation> recommendations,
    Instant generatedAt
) {
    public RecommendationSet {
        recommendations = List.copyOf(recommendations);
        if (recommendationSetId == null || userId == null || careerReadinessId == null || policyId == null
            || policyVersion == null || policyVersion.isBlank() || !List.of("PUBLISHED", "SUPERSEDED").contains(status)
            || generatedAt == null) throw new IllegalArgumentException("recommendation set is invalid");
    }
}
