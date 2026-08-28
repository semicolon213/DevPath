package com.devpath.integration.application;

public class GitHubCollectionLimitExceededException extends GitHubIntegrationUnavailableException {
    public GitHubCollectionLimitExceededException(String message) {
        super(message);
    }
}
