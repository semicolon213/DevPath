package com.devpath.identity.application;

public enum AuthenticationAuditEvent {
    LOGIN_SUCCEEDED,
    LOGOUT_SUCCEEDED,
    SESSION_ABSOLUTE_TIMEOUT,
    LOGIN_FAILED,
    EXTERNAL_IDENTITY_LINKED,
    AUTHENTICATION_REJECTED_DISABLED_ACCOUNT
}
