package com.devpath.rule.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SkillMatrixView(
    UUID skillMatrixId, UUID evaluationId, String policyVersion, String ruleSetVersion,
    String status, List<SkillAssessmentView> skills, List<String> strengths,
    List<String> weaknesses, Instant generatedAt
) {
    public SkillMatrixView {
        skills = List.copyOf(skills); strengths = List.copyOf(strengths); weaknesses = List.copyOf(weaknesses);
    }
}
