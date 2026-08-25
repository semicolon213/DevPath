package com.devpath.learning.application;

import java.util.List;
import java.util.UUID;

public record RoadmapStepView(UUID roadmapStepId, UUID milestoneId, UUID recommendationId, int position,
    String category, String title, String difficulty, int effortHours, List<UUID> prerequisiteStepIds,
    String completionCriteria, List<String> expectedEvidence, String status) {}
