package com.devpath.integration.application;

public enum IntegrationAuditEvent {
    GITHUB_CONNECTED,
    GITHUB_DISCONNECTED,
    GITHUB_TOKEN_REFRESH_FAILED,
    GITHUB_PERMISSION_CHANGED,
    GITHUB_RATE_LIMITED
}
