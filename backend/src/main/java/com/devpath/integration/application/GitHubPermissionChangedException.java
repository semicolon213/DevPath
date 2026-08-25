package com.devpath.integration.application;

public class GitHubPermissionChangedException extends GitHubIntegrationUnavailableException {
    public GitHubPermissionChangedException(String message, Throwable cause) {
        super(message, cause);
    }
}
