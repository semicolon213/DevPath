package com.devpath.repository.domain;

import java.time.Instant;

public record RepositoryCommit(
    String sha,
    String authorLogin,
    Instant committedAt,
    String messageSummary
) {
    public RepositoryCommit {
        if (sha == null || !sha.matches("[a-fA-F0-9]{40,64}") || committedAt == null
            || messageSummary == null || messageSummary.length() > 500
            || (authorLogin != null && authorLogin.length() > 255)) {
            throw new IllegalArgumentException("Repository commit fact is invalid");
        }
    }
}
