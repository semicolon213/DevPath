package com.devpath.ai.application;

import java.util.List;
import java.util.UUID;

public record RepositorySummaryContent(
    Claim summary, List<Claim> strengths, List<Claim> improvementAreas, List<String> technologies
) {
    public RepositorySummaryContent {
        strengths = strengths == null ? null : List.copyOf(strengths);
        improvementAreas = improvementAreas == null ? null : List.copyOf(improvementAreas);
        technologies = technologies == null ? null : List.copyOf(technologies);
    }

    public record Claim(String text, List<UUID> evidenceReferences, List<String> ruleOutputReferences) {
        public Claim {
            evidenceReferences = evidenceReferences == null ? null : List.copyOf(evidenceReferences);
            ruleOutputReferences = ruleOutputReferences == null ? null : List.copyOf(ruleOutputReferences);
        }
    }
}
