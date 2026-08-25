package com.devpath.repository.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.integration.application.GitHubIntegrationApplicationService;
import com.devpath.integration.application.GitHubRepositoryListView;
import com.devpath.integration.application.GitHubRepositoryView;
import com.devpath.repository.domain.Repository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositorySynchronizationApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");

    @Test
    void reverifiesProviderPermissionBeforeCreatingADurableJob() {
        UUID userId = UUID.randomUUID();
        Repository repository = Repository.discover(
            userId, UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            false, "main", false, "https://github.com/owner/devpath", NOW
        );
        var transactions = mock(RepositorySynchronizationTransaction.class);
        var github = mock(GitHubIntegrationApplicationService.class);
        when(transactions.target(userId, repository.id())).thenReturn(repository);
        when(github.listRepositories(userId)).thenReturn(new GitHubRepositoryListView(List.of(
            new GitHubRepositoryView("42", "devpath", "owner/devpath", "owner", false, false,
                "main", "https://github.com/owner/devpath")
        )));
        var service = new RepositorySynchronizationApplicationService(
            transactions, github, Clock.fixed(NOW, ZoneOffset.UTC)
        );

        service.request(userId, repository.id(), "sync-request-1");

        verify(transactions).request(userId, repository.id(), "sync-request-1", NOW);
    }

    @Test
    void rejectsARepositoryNoLongerVisibleThroughTheProviderConnection() {
        UUID userId = UUID.randomUUID();
        Repository repository = Repository.discover(
            userId, UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            false, "main", false, "https://github.com/owner/devpath", NOW
        );
        var transactions = mock(RepositorySynchronizationTransaction.class);
        var github = mock(GitHubIntegrationApplicationService.class);
        when(transactions.target(userId, repository.id())).thenReturn(repository);
        when(github.listRepositories(userId)).thenReturn(new GitHubRepositoryListView(List.of()));
        var service = new RepositorySynchronizationApplicationService(
            transactions, github, Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> service.request(userId, repository.id(), "sync-request-1"))
            .isInstanceOf(RepositoryNotAccessibleException.class);
    }
}
