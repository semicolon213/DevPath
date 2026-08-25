package com.devpath.repository.application;

import java.util.List;

public record RepositorySnapshotListView(List<RepositorySnapshotView> snapshots) {
    public RepositorySnapshotListView {
        snapshots = List.copyOf(snapshots);
    }
}
