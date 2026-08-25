package com.devpath.repository.application;

public enum RepositoryAuditEvent {
    REPOSITORY_IMPORTED,
    REPOSITORY_ARCHIVED,
    REPOSITORY_RESTORED,
    REPOSITORY_SYNC_REQUESTED,
    REPOSITORY_SYNC_SUCCEEDED,
    REPOSITORY_SYNC_FAILED
}
