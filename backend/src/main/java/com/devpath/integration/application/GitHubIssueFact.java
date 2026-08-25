package com.devpath.integration.application;

import java.time.Instant;
import java.util.List;

public record GitHubIssueFact(
    String providerIssueId,
    String status,
    List<String> labels,
    Instant openedAt,
    Instant closedAt
) {
    public GitHubIssueFact {
        labels = List.copyOf(labels);
    }
}
