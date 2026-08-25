package com.devpath.repository.domain;

import java.time.Instant;
import java.util.Objects;

public record RepositoryPullRequest(
    String providerPullRequestId,
    String status,
    Instant openedAt,
    Instant closedAt,
    Instant mergedAt,
    int reviewCount
) {
    public RepositoryPullRequest {
        Objects.requireNonNull(providerPullRequestId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(openedAt);
        if (providerPullRequestId.isBlank() || providerPullRequestId.length() > 128
            || !(status.equals("OPEN") || status.equals("CLOSED") || status.equals("MERGED"))
            || reviewCount < 0 || (status.equals("MERGED") && mergedAt == null)) {
            throw new IllegalArgumentException("Pull request fact is invalid");
        }
    }
}
