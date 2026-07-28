package com.devpath.identity.domain;

public final class DisabledAccountException extends RuntimeException {
    public DisabledAccountException() {
        super("The account cannot authenticate");
    }
}
