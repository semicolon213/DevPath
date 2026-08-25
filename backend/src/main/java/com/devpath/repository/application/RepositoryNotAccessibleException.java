package com.devpath.repository.application;

public class RepositoryNotAccessibleException extends RuntimeException {
    public RepositoryNotAccessibleException() {
        super("The selected GitHub repository is not accessible");
    }
}
