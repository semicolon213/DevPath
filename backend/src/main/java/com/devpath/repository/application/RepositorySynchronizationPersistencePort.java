package com.devpath.repository.application;

import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.repository.domain.RepositorySyncJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorySynchronizationPersistencePort {
    Optional<RepositorySyncJob> findByOwnerAndIdempotencyKey(UUID userId, String idempotencyKey);
    Optional<RepositorySyncJob> findActiveByRepository(UUID repositoryId);
    Optional<RepositorySyncJob> findByIdAndOwner(UUID jobId, UUID userId);
    Optional<RepositorySyncJob> findNextClaimable(Instant now);
    List<RepositorySyncJob> findRecentByOwner(UUID userId, int limit);
    RepositorySyncJob saveJob(RepositorySyncJob job);
    RepositorySnapshot saveSnapshot(RepositorySnapshot snapshot);
    List<RepositorySnapshot> findSnapshots(UUID userId, UUID repositoryId);
    Optional<RepositorySnapshot> findSnapshot(UUID userId, UUID repositoryId, UUID snapshotId);
    void appendOutbox(String aggregateType, UUID aggregateId, String eventType, String payload, Instant occurredAt);
}
