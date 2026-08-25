package com.devpath.integration.application;

public class GitHubIntegrationUnavailableException extends RuntimeException {
    public GitHubIntegrationUnavailableException(String message) {
        super(message);
    }

    public GitHubIntegrationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
