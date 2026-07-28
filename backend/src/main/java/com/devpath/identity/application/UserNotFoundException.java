package com.devpath.identity.application;

public final class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("Authenticated user was not found");
    }
}
