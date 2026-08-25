package com.devpath.repository.domain;

public enum RepositorySyncJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    EXPIRED
}
