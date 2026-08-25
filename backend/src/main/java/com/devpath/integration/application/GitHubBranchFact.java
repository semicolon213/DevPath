package com.devpath.integration.application;

public record GitHubBranchFact(String name, String headCommitSha, boolean defaultBranch) {}
