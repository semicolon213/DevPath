package com.devpath.integration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.identity.application.ExternalIdentityRepositoryPort;
import com.devpath.identity.domain.ExternalIdentity;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.ProviderSubject;
import com.devpath.identity.domain.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GitHubIntegrationApplicationServiceTest {
    @Test
    void bindsProviderAuthorizationToTheAuthenticatedLoginIdentity() {
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        UserId userId = UserId.newId();
        ExternalIdentity identity = ExternalIdentity.link(
            userId, OAuthProvider.GITHUB, new ProviderSubject("1849102"),
            "devpath-user", "DevPath User", null, now
        );
        var identities = mock(ExternalIdentityRepositoryPort.class);
        var github = mock(GitHubConnectionPort.class);
        var audit = mock(IntegrationAuditPort.class);
        var expected = new ConnectedAccountView(UUID.randomUUID(), "GITHUB", "ACTIVE", java.util.List.of(), now, null);
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GITHUB)).thenReturn(Optional.of(identity));
        when(github.complete(userId.value(), identity.id().value(), "1849102", "code", now)).thenReturn(expected);
        var service = new GitHubIntegrationApplicationService(
            identities, github, audit, Clock.fixed(now, ZoneOffset.UTC)
        );

        assertThat(service.complete(userId.value(), "code")).isEqualTo(expected);
        verify(github).complete(userId.value(), identity.id().value(), "1849102", "code", now);
        verify(audit).record(
            IntegrationAuditEvent.GITHUB_CONNECTED,
            userId.value(),
            expected.connectionId().toString(),
            now
        );
    }

    @Test
    void disconnectsOnlyTheAuthenticatedUsersConnectionAndAuditsIt() {
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        UUID userId = UUID.randomUUID();
        var identities = mock(ExternalIdentityRepositoryPort.class);
        var github = mock(GitHubConnectionPort.class);
        var audit = mock(IntegrationAuditPort.class);
        var disconnected = new ConnectedAccountView(
            UUID.randomUUID(), "GITHUB", "REVOKED", java.util.List.of(), now.minusSeconds(60), now
        );
        when(github.disconnect(userId, now)).thenReturn(disconnected);
        var service = new GitHubIntegrationApplicationService(
            identities, github, audit, Clock.fixed(now, ZoneOffset.UTC)
        );

        assertThat(service.disconnect(userId)).isEqualTo(disconnected);

        verify(github).disconnect(userId, now);
        verify(audit).record(
            IntegrationAuditEvent.GITHUB_DISCONNECTED,
            userId,
            disconnected.connectionId().toString(),
            now
        );
    }
}
