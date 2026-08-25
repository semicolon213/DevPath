package com.devpath.integration.application;

import java.time.Instant;

public final class GitHubRateLimitExceededException extends GitHubIntegrationUnavailableException {
    private final Instant resetAt;
    private final Long retryAfterSeconds;

    public GitHubRateLimitExceededException(
        Instant resetAt,
        Long retryAfterSeconds,
        Throwable cause
    ) {
        super("GitHub request limit has been reached", cause);
        this.resetAt = resetAt;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Instant resetAt() {
        return resetAt;
    }

    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }

    public Instant retryAt(Instant now) {
        Instant maximumWait = now.plusSeconds(86_400);
        if (resetAt != null && resetAt.isAfter(now) && !resetAt.isAfter(maximumWait)) {
            return resetAt;
        }
        if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            return now.plusSeconds(Math.min(retryAfterSeconds, 86_400));
        }
        return now.plusSeconds(60);
    }
}
