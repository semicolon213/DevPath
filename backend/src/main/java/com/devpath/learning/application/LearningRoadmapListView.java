package com.devpath.learning.application;

import java.util.List;

public record LearningRoadmapListView(List<LearningRoadmapView> roadmaps) {
    public LearningRoadmapListView {
        roadmaps = List.copyOf(roadmaps);
    }
}
