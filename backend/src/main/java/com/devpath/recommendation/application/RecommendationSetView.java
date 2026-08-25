package com.devpath.recommendation.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecommendationSetView(UUID recommendationSetId, UUID careerReadinessId, String policyVersion,
    String status, List<RecommendationView> recommendations, Instant generatedAt) {}
