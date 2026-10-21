package com.devpath.shared.application;

public class DuplicateRequestException extends RuntimeException {
    public DuplicateRequestException() {
        super("The idempotency key belongs to another request");
    }
}
