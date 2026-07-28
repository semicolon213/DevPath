package com.devpath.identity.application;

public enum AuthenticationAuditEvent {
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    EXTERNAL_IDENTITY_LINKED,
    AUTHENTICATION_REJECTED_DISABLED_ACCOUNT
}
