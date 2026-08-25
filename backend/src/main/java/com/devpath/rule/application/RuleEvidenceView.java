package com.devpath.rule.application;

import java.math.BigDecimal;
import java.util.UUID;

public record RuleEvidenceView(
    UUID evidenceId, String ruleId, String contributionRole, String evidenceType,
    String sourceReference, String observedFactSummary, BigDecimal confidence
) {}
