package com.devpath.recommendation.application;

import java.util.List;
import java.util.UUID;

public record RecommendationEvidenceListView(UUID recommendationId, List<RecommendationEvidenceView> evidence) {
    public RecommendationEvidenceListView {
        evidence = List.copyOf(evidence);
    }
}
