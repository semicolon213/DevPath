package com.devpath.repository.application;

import com.devpath.repository.domain.RepositorySnapshot;
import java.time.Instant;
import java.util.UUID;

public record RepositorySnapshotView(
    UUID snapshotId,
    UUID repositoryId,
    Instant capturedAt,
    String sourceRevision,
    String status,
    boolean immutable,
    String contentHash,
    int branchCount,
    int commitCount
) {
    static RepositorySnapshotView from(RepositorySnapshot snapshot) {
        return new RepositorySnapshotView(
            snapshot.id(), snapshot.repositoryId(), snapshot.capturedAt(), snapshot.sourceRevision(),
            snapshot.status(), true, snapshot.contentHash(), snapshot.branches().size(), snapshot.commits().size()
        );
    }
}
