package com.devpath.integration.application;

public class GitHubConnectionNotFoundException extends RuntimeException {
    public GitHubConnectionNotFoundException() {
        super("The GitHub connection was not found");
    }
}
