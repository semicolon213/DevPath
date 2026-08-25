package com.devpath.repository.application;

public class RepositoryNotFoundException extends RuntimeException {
    public RepositoryNotFoundException() {
        super("The repository was not found");
    }
}
