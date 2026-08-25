package com.devpath.recommendation.domain;

import com.devpath.rule.domain.RuleCategory;
import java.util.List;
import java.util.UUID;

public record Recommendation(
    UUID recommendationId,
    UUID skillGapId,
    RuleCategory category,
    RecommendationType type,
    RecommendationPriority priority,
    String rationaleCode,
    String title,
    String completionCriteria,
    List<String> expectedEvidence,
    List<UUID> evidenceIds,
    int effortHours,
    int position,
    String status
) {
    public Recommendation {
        expectedEvidence = List.copyOf(expectedEvidence);
        evidenceIds = List.copyOf(evidenceIds);
        if (recommendationId == null || skillGapId == null || category == null || type == null || priority == null
            || effortHours <= 0 || position < 0 || !List.of("PROPOSED", "ACCEPTED", "DISMISSED", "COMPLETED").contains(status)) {
            throw new IllegalArgumentException("recommendation is invalid");
        }
    }
}
