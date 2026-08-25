package com.devpath.integration.application;

import java.util.List;

public record GitHubRepositoryListView(List<GitHubRepositoryView> repositories) {
    public GitHubRepositoryListView {
        repositories = List.copyOf(repositories);
    }
}
