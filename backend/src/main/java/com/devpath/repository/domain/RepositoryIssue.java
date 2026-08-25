package com.devpath.repository.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RepositoryIssue(
    String providerIssueId,
    String status,
    List<String> labels,
    Instant openedAt,
    Instant closedAt
) {
    public RepositoryIssue {
        Objects.requireNonNull(providerIssueId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(labels);
        Objects.requireNonNull(openedAt);
        labels = labels.stream().map(String::trim).filter(value -> !value.isBlank())
            .distinct().sorted(String.CASE_INSENSITIVE_ORDER).limit(100).toList();
        if (providerIssueId.isBlank() || providerIssueId.length() > 128
            || !(status.equals("OPEN") || status.equals("CLOSED"))) {
            throw new IllegalArgumentException("Issue fact is invalid");
        }
    }
}
