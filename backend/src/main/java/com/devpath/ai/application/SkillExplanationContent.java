package com.devpath.ai.application;

import java.util.List;
import java.util.UUID;

public record SkillExplanationContent(
    String summary, List<Item> strengths, List<Item> improvementAreas
) {
    public SkillExplanationContent {
        strengths = strengths == null ? List.of() : List.copyOf(strengths);
        improvementAreas = improvementAreas == null ? List.of() : List.copyOf(improvementAreas);
    }

    public record Item(String skillKey, String explanation, List<UUID> evidenceIds) {
        public Item {
            evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        }
    }
}
