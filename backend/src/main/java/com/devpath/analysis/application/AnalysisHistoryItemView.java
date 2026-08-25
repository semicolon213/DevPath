package com.devpath.analysis.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AnalysisHistoryItemView(
    UUID analysisId,
    UUID repositoryId,
    String repositoryFullName,
    UUID snapshotId,
    UUID evaluationId,
    UUID skillMatrixId,
    String analysisScope,
    BigDecimal overallScore,
    BigDecimal confidence,
    String ruleSetVersion,
    String policyVersion,
    boolean currentForRepository,
    Instant completedAt
) {}
