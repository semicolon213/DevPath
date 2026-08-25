package com.devpath.integration.application;

import java.util.List;

public record GitHubRepositorySnapshot(
    String sourceRevision,
    List<GitHubBranchFact> branches,
    List<GitHubCommitFact> commits,
    List<GitHubLanguageFact> languages,
    List<GitHubDependencyFact> dependencies,
    List<GitHubFileFact> files
) {
    public GitHubRepositorySnapshot {
        branches = List.copyOf(branches);
        commits = List.copyOf(commits);
        languages = List.copyOf(languages);
        dependencies = List.copyOf(dependencies);
        files = List.copyOf(files);
    }
}
