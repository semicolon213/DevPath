package com.devpath.repository.domain;

public record RepositoryBranch(String name, String headCommitSha, boolean defaultBranch) {
    public RepositoryBranch {
        if (name == null || name.isBlank() || name.length() > 255
            || headCommitSha == null || !headCommitSha.matches("[a-fA-F0-9]{40,64}")) {
            throw new IllegalArgumentException("Repository branch fact is invalid");
        }
    }
}
