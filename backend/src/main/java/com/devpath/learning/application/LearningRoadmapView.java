package com.devpath.learning.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LearningRoadmapView(UUID roadmapId, UUID recommendationSetId, String policyVersion, String status,
    BigDecimal progressPercent, List<RoadmapMilestoneView> milestones, List<RoadmapStepView> steps,
    Instant generatedAt, Instant updatedAt) {}
