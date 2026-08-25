package com.devpath.integration.application;

import java.util.List;

public record GitHubRepositorySnapshot(
    String sourceRevision,
    List<GitHubBranchFact> branches,
    List<GitHubCommitFact> commits,
    List<GitHubLanguageFact> languages,
    List<GitHubDependencyFact> dependencies,
    List<GitHubFileFact> files,
    List<GitHubPullRequestFact> pullRequests,
    List<GitHubIssueFact> issues,
    List<GitHubDocumentFact> documents
) {
    public GitHubRepositorySnapshot {
        branches = List.copyOf(branches);
        commits = List.copyOf(commits);
        languages = List.copyOf(languages);
        dependencies = List.copyOf(dependencies);
        files = List.copyOf(files);
        pullRequests = List.copyOf(pullRequests);
        issues = List.copyOf(issues);
        documents = List.copyOf(documents);
    }
}
