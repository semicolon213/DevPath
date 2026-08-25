package com.devpath.rule.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CompletedRuleEvaluation(
    UUID id,
    UUID userId,
    UUID snapshotId,
    UUID ruleSetVersionId,
    String inputHash,
    RuleEvaluationResult result,
    Instant startedAt,
    Instant completedAt
) {
    public CompletedRuleEvaluation {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(snapshotId, "snapshotId is required");
        Objects.requireNonNull(ruleSetVersionId, "ruleSetVersionId is required");
        Objects.requireNonNull(result, "result is required");
        Objects.requireNonNull(startedAt, "startedAt is required");
        Objects.requireNonNull(completedAt, "completedAt is required");
        if (inputHash == null || !inputHash.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("inputHash is invalid");
        if (!snapshotId.toString().equals(result.snapshotId()) || !ruleSetVersionId.toString().equals(result.ruleSetVersionId())) {
            throw new IllegalArgumentException("evaluation basis does not match its result");
        }
        if (completedAt.isBefore(startedAt)) throw new IllegalArgumentException("completion cannot precede start");
    }
}
