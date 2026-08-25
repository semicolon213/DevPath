package com.devpath.recommendation.application;

import java.util.List;
import java.util.UUID;

public record RecommendationView(UUID recommendationId, UUID skillGapId, String category, String type,
    String priority, String rationaleCode, String title, String completionCriteria, List<String> expectedEvidence,
    List<UUID> evidenceIds, int effortHours, int position, String status) {}
