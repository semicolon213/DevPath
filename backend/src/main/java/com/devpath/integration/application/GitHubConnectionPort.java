package com.devpath.integration.application;

import java.time.Instant;
import java.util.UUID;

public interface GitHubConnectionPort {
    String authorizationUrl(String state);

    ConnectedAccountView complete(
        UUID userId,
        UUID externalIdentityId,
        String expectedProviderSubject,
        String code,
        Instant now
    );

    GitHubRepositoryListView listRepositories(UUID userId, Instant now);

    GitHubRepositorySnapshot collectRepository(
        UUID userId,
        String providerRepositoryId,
        String defaultBranch,
        Instant now
    );

    ConnectedAccountView disconnect(UUID userId, Instant now);
}
