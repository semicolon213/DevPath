package com.devpath.identity.domain;

public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    DELETION_REQUESTED,
    DELETED;

    public boolean permitsAuthentication() {
        return this == ACTIVE;
    }
}
