package com.devpath.career.application;

import java.util.List;
import java.util.UUID;

public record SkillGapListView(UUID careerReadinessId, List<SkillGapView> skillGaps) {
    public SkillGapListView { skillGaps = List.copyOf(skillGaps); }
}
