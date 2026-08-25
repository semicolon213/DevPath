package com.devpath.repository.domain;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Repository(
    UUID id,
    UUID userId,
    UUID externalIdentityId,
    String provider,
    String providerRepositoryId,
    String name,
    String fullName,
    String owner,
    RepositoryVisibility visibility,
    String defaultBranch,
    boolean providerArchived,
    RepositoryLifecycle lifecycle,
    RepositorySyncStatus syncStatus,
    String htmlUrl,
    Instant discoveredAt,
    Instant updatedAt,
    Instant lastSyncedAt,
    UUID currentSnapshotId,
    long version
) {
    public Repository {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(externalIdentityId);
        Objects.requireNonNull(visibility);
        Objects.requireNonNull(lifecycle);
        Objects.requireNonNull(syncStatus);
        Objects.requireNonNull(discoveredAt);
        Objects.requireNonNull(updatedAt);
        provider = required(provider, 32);
        providerRepositoryId = required(providerRepositoryId, 64);
        name = required(name, 100);
        fullName = required(fullName, 255);
        owner = required(owner, 255);
        defaultBranch = required(defaultBranch, 255);
        htmlUrl = required(htmlUrl, 2048);
        URI url = URI.create(htmlUrl);
        if (!"https".equalsIgnoreCase(url.getScheme()) || !"github.com".equalsIgnoreCase(url.getHost())) {
            throw new IllegalArgumentException("Repository URL must be a GitHub HTTPS URL");
        }
    }

    public static Repository discover(
        UUID userId,
        UUID externalIdentityId,
        String providerRepositoryId,
        String name,
        String fullName,
        String owner,
        boolean privateRepository,
        String defaultBranch,
        boolean archived,
        String htmlUrl,
        Instant now
    ) {
        return new Repository(
            UUID.randomUUID(), userId, externalIdentityId, "GITHUB", providerRepositoryId,
            name, fullName, owner,
            privateRepository ? RepositoryVisibility.PRIVATE : RepositoryVisibility.PUBLIC,
            defaultBranch, archived,
            archived ? RepositoryLifecycle.ARCHIVED : RepositoryLifecycle.DISCOVERED,
            RepositorySyncStatus.NOT_SYNCED, htmlUrl, now, now, null, null, 0
        );
    }

    public Repository archive(Instant now) {
        Objects.requireNonNull(now);
        if (lifecycle == RepositoryLifecycle.DELETED_EXTERNALLY) {
            throw new IllegalStateException("A provider-deleted repository cannot be archived locally");
        }
        if (lifecycle == RepositoryLifecycle.ARCHIVED) {
            return this;
        }
        return withLifecycle(RepositoryLifecycle.ARCHIVED, now);
    }

    public Repository restore(Instant now) {
        Objects.requireNonNull(now);
        if (lifecycle == RepositoryLifecycle.DELETED_EXTERNALLY) {
            throw new IllegalStateException("A provider-deleted repository cannot be restored locally");
        }
        if (lifecycle != RepositoryLifecycle.ARCHIVED) {
            return this;
        }
        if (providerArchived) {
            throw new IllegalStateException("A provider-archived repository cannot be restored locally");
        }
        RepositoryLifecycle restored = lastSyncedAt == null
            ? RepositoryLifecycle.DISCOVERED
            : RepositoryLifecycle.ACTIVE;
        return withLifecycle(restored, now);
    }

    private Repository withLifecycle(RepositoryLifecycle value, Instant now) {
        return new Repository(
            id, userId, externalIdentityId, provider, providerRepositoryId, name, fullName, owner,
            visibility, defaultBranch, providerArchived, value, syncStatus, htmlUrl,
            discoveredAt, now, lastSyncedAt, currentSnapshotId, version
        );
    }

    public Repository markSynchronized(UUID snapshotId, Instant now) {
        Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(now);
        return new Repository(
            id, userId, externalIdentityId, provider, providerRepositoryId, name, fullName, owner,
            visibility, defaultBranch, providerArchived, RepositoryLifecycle.ACTIVE,
            RepositorySyncStatus.SYNCHRONIZED, htmlUrl, discoveredAt, now, now, snapshotId, version
        );
    }

    public Repository markSyncFailed(Instant now) {
        Objects.requireNonNull(now);
        return new Repository(
            id, userId, externalIdentityId, provider, providerRepositoryId, name, fullName, owner,
            visibility, defaultBranch, providerArchived, lifecycle, RepositorySyncStatus.FAILED,
            htmlUrl, discoveredAt, now, lastSyncedAt, currentSnapshotId, version
        );
    }

    private static String required(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException("Repository metadata is invalid");
        }
        return value;
    }
}
