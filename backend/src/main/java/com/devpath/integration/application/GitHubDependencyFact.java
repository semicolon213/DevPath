package com.devpath.integration.application;

public record GitHubDependencyFact(
    String ecosystem,
    String packageName,
    String versionConstraint,
    String scope,
    String manifestPath
) {
    public GitHubDependencyFact {
        if (ecosystem == null || ecosystem.isBlank() || packageName == null || packageName.isBlank()
            || scope == null || scope.isBlank() || manifestPath == null || manifestPath.isBlank()) {
            throw new IllegalArgumentException("GitHub dependency fact is invalid");
        }
    }
}
