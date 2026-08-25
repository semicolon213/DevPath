package com.devpath.repository.application;

import java.util.List;

public record RepositoryListView(
    List<RepositoryView> repositories,
    int limit,
    String nextCursor,
    long totalCount
) {
    public RepositoryListView {
        repositories = List.copyOf(repositories);
    }
}
