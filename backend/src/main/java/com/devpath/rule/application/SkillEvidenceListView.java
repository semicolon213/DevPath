package com.devpath.rule.application;

import java.util.List;
import java.util.UUID;

public record SkillEvidenceListView(
    UUID skillId, UUID skillAssessmentId, UUID skillMatrixId, List<SkillEvidenceView> evidence
) {
    public SkillEvidenceListView { evidence = List.copyOf(evidence); }
}
