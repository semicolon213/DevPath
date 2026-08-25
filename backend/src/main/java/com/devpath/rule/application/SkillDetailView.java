package com.devpath.rule.application;

import java.time.Instant;
import java.util.UUID;

public record SkillDetailView(
    UUID skillMatrixId, UUID evaluationId, String policyVersion, String ruleSetVersion,
    String matrixStatus, Instant generatedAt, SkillAssessmentView skill
) {}
