package com.devpath.integration.application;

import com.devpath.identity.application.ExternalIdentityRepositoryPort;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GitHubIntegrationApplicationService {
    private final ExternalIdentityRepositoryPort identities;
    private final GitHubConnectionPort github;
    private final IntegrationAuditPort audit;
    private final Clock clock;

    public GitHubIntegrationApplicationService(
        ExternalIdentityRepositoryPort identities,
        GitHubConnectionPort github,
        IntegrationAuditPort audit,
        Clock clock
    ) {
        this.identities = identities;
        this.github = github;
        this.audit = audit;
        this.clock = clock;
    }

    public String authorizationUrl(String state) {
        return github.authorizationUrl(state);
    }

    public ConnectedAccountView complete(UUID userId, String code) {
        var identity = identities.findByUserIdAndProvider(new UserId(userId), OAuthProvider.GITHUB)
            .orElseThrow(() -> new GitHubIntegrationUnavailableException("GitHub login identity is unavailable"));
        ConnectedAccountView connection = github.complete(
            userId,
            identity.id().value(),
            identity.providerSubject().value(),
            code,
            clock.instant()
        );
        audit.record(
            IntegrationAuditEvent.GITHUB_CONNECTED,
            userId,
            connection.connectionId().toString(),
            clock.instant()
        );
        return connection;
    }

    public GitHubRepositoryListView listRepositories(UUID userId) {
        return github.listRepositories(userId, clock.instant());
    }

    public ConnectedAccountView disconnect(UUID userId) {
        Instant now = clock.instant();
        ConnectedAccountView connection = github.disconnect(userId, now);
        audit.record(
            IntegrationAuditEvent.GITHUB_DISCONNECTED,
            userId,
            connection.connectionId().toString(),
            now
        );
        return connection;
    }
}
