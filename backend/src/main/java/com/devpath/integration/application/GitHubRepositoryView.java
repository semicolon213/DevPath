package com.devpath.integration.application;

public record GitHubRepositoryView(
    String providerRepositoryId,
    String name,
    String fullName,
    String owner,
    boolean privateRepository,
    boolean archived,
    String defaultBranch,
    String htmlUrl
) {
}
