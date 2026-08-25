package com.devpath.recommendation.application;

import java.util.List;

public record RecommendationSetListView(List<RecommendationSetView> recommendationSets) {
    public RecommendationSetListView {
        recommendationSets = List.copyOf(recommendationSets);
    }
}
