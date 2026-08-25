package com.devpath.integration.application;

public class GitHubInstallationRequiredException extends RuntimeException {
    public GitHubInstallationRequiredException() {
        super("GitHub App installation is required");
    }
}
