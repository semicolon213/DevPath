package com.devpath.repository.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.identity.application.ExternalIdentityRepositoryPort;
import com.devpath.identity.domain.ExternalIdentity;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.ProviderSubject;
import com.devpath.identity.domain.UserId;
import com.devpath.integration.application.GitHubIntegrationApplicationService;
import com.devpath.integration.application.GitHubRepositoryListView;
import com.devpath.integration.application.GitHubRepositoryView;
import com.devpath.repository.domain.Repository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RepositoryApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");

    @Test
    void importsOnlyMetadataReverifiedThroughTheAuthenticatedGitHubConnection() {
        UUID userId = UUID.randomUUID();
        UserId owner = new UserId(userId);
        ExternalIdentity identity = ExternalIdentity.link(
            owner, OAuthProvider.GITHUB, new ProviderSubject("1849102"),
            "owner", "Owner", null, NOW
        );
        var github = mock(GitHubIntegrationApplicationService.class);
        var identities = mock(ExternalIdentityRepositoryPort.class);
        var transactions = mock(RepositoryRegistrationTransaction.class);
        var providerRepository = new GitHubRepositoryView(
            "42", "devpath", "owner/devpath", "owner", true, false,
            "main", "https://github.com/owner/devpath"
        );
        when(github.listRepositories(userId)).thenReturn(new GitHubRepositoryListView(List.of(providerRepository)));
        when(identities.findByUserIdAndProvider(owner, OAuthProvider.GITHUB)).thenReturn(Optional.of(identity));
        when(transactions.register(any(), eq(NOW))).thenAnswer(invocation ->
            RepositoryView.from(invocation.getArgument(0))
        );
        var service = new RepositoryApplicationService(
            github, identities, transactions, Clock.fixed(NOW, ZoneOffset.UTC)
        );

        RepositoryView result = service.importGitHub(userId, "42");

        assertThat(result.fullName()).isEqualTo("owner/devpath");
        assertThat(result.visibility()).isEqualTo("PRIVATE");
        var candidate = ArgumentCaptor.forClass(Repository.class);
        verify(transactions).register(candidate.capture(), eq(NOW));
        assertThat(candidate.getValue().userId()).isEqualTo(userId);
        assertThat(candidate.getValue().syncStatus().name()).isEqualTo("NOT_SYNCED");
    }

    @Test
    void returnsBoundedCursorPaginationMetadata() {
        UUID userId = UUID.randomUUID();
        var github = mock(GitHubIntegrationApplicationService.class);
        var identities = mock(ExternalIdentityRepositoryPort.class);
        var transactions = mock(RepositoryRegistrationTransaction.class);
        when(transactions.count(userId, false)).thenReturn(21L);
        List<Repository> page = java.util.stream.IntStream.range(0, 20)
            .mapToObj(index -> repository(userId, Integer.toString(index + 1)))
            .toList();
        when(transactions.findPage(userId, 0, 20, false)).thenReturn(page);
        var service = new RepositoryApplicationService(
            github, identities, transactions, Clock.fixed(NOW, ZoneOffset.UTC)
        );

        RepositoryListView result = service.list(userId, null, null, false);

        assertThat(result.totalCount()).isEqualTo(21);
        assertThat(result.limit()).isEqualTo(20);
        assertThat(result.nextCursor()).isEqualTo("MjA");
    }

    private Repository repository(UUID userId, String providerRepositoryId) {
        return Repository.discover(
            userId, UUID.randomUUID(), providerRepositoryId, "devpath", "owner/devpath", "owner",
            false, "main", false, "https://github.com/owner/devpath", NOW
        );
    }
}
