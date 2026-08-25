package com.devpath.rule.application;

import java.math.BigDecimal;
import java.util.UUID;

public record SkillEvidenceView(
    UUID evidenceId, UUID snapshotId, String evidenceType, String sourceReference,
    String observedFactSummary, BigDecimal confidence
) {}
