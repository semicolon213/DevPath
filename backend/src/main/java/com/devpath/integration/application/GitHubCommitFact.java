package com.devpath.integration.application;

import java.time.Instant;

public record GitHubCommitFact(
    String sha,
    String authorLogin,
    Instant committedAt,
    String messageSummary
) {}
