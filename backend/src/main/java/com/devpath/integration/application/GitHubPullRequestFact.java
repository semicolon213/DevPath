package com.devpath.integration.application;

import java.time.Instant;

public record GitHubPullRequestFact(
    String providerPullRequestId,
    String status,
    Instant openedAt,
    Instant closedAt,
    Instant mergedAt,
    int reviewCount
) {}
