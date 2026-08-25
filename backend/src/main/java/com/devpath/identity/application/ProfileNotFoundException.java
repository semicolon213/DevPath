package com.devpath.identity.application;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException() { super("User profile was not found"); }
}
