package com.devpath.repository.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositoryTest {
    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");

    @Test
    void discoversPrivateAndArchivedGitHubMetadataWithoutInventingASyncResult() {
        Repository repository = Repository.discover(
            UUID.randomUUID(), UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            true, "main", true, "https://github.com/owner/devpath", NOW
        );

        assertThat(repository.visibility()).isEqualTo(RepositoryVisibility.PRIVATE);
        assertThat(repository.lifecycle()).isEqualTo(RepositoryLifecycle.ARCHIVED);
        assertThat(repository.syncStatus()).isEqualTo(RepositorySyncStatus.NOT_SYNCED);
        assertThat(repository.lastSyncedAt()).isNull();
    }

    @Test
    void rejectsNonGitHubRepositoryUrls() {
        assertThatThrownBy(() -> Repository.discover(
            UUID.randomUUID(), UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            false, "main", false, "https://example.com/owner/devpath", NOW
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void archivesAndRestoresLocalLifecycleWithoutChangingProviderMetadata() {
        Repository repository = Repository.discover(
            UUID.randomUUID(), UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            false, "main", false, "https://github.com/owner/devpath", NOW
        );

        Repository archived = repository.archive(NOW.plusSeconds(60));
        Repository restored = archived.restore(NOW.plusSeconds(120));

        assertThat(archived.lifecycle()).isEqualTo(RepositoryLifecycle.ARCHIVED);
        assertThat(restored.lifecycle()).isEqualTo(RepositoryLifecycle.DISCOVERED);
        assertThat(restored.providerArchived()).isFalse();
        assertThat(archived.archive(NOW.plusSeconds(180))).isSameAs(archived);
    }

    @Test
    void refusesToRestoreARepositoryArchivedAtTheProvider() {
        Repository repository = Repository.discover(
            UUID.randomUUID(), UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            false, "main", true, "https://github.com/owner/devpath", NOW
        );

        assertThatThrownBy(() -> repository.restore(NOW.plusSeconds(60)))
            .isInstanceOf(IllegalStateException.class);
    }
}
