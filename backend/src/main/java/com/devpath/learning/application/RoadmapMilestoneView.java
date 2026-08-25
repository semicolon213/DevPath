package com.devpath.learning.application;

import java.util.UUID;

public record RoadmapMilestoneView(UUID milestoneId, int position, String category, String title, String status) {}
