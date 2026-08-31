package com.devpath.ai.application;

import java.time.Instant;
import java.util.UUID;

public record StoredPromptContext(
    UUID id, UUID userId, UUID templateVersionId, UUID skillMatrixId, String taskType,
    int tokenBudget, String contextHash, String contextPayload, String prompt, Instant lockedAt
) {}
