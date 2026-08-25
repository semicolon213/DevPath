package com.devpath.learning.domain;

import com.devpath.rule.domain.RuleCategory;
import java.util.UUID;

public record RoadmapMilestone(UUID milestoneId, int position, RuleCategory category, String title, String status) {}
