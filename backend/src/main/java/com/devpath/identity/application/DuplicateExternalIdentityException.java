package com.devpath.identity.application;

public final class DuplicateExternalIdentityException extends RuntimeException {
    public DuplicateExternalIdentityException(Throwable cause) {
        super("External identity already exists", cause);
    }
}
