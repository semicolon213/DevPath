package com.devpath.identity.domain;

import java.time.Instant;
import java.util.Objects;

public final class User {
    private final UserId id;
    private final Instant createdAt;
    private AccountStatus status;
    private String displayName;
    private String avatarUrl;
    private Instant updatedAt;
    private long version;

    private User(
        UserId id,
        AccountStatus status,
        String displayName,
        String avatarUrl,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {
        this.id = Objects.requireNonNull(id, "User ID is required");
        this.status = Objects.requireNonNull(status, "Account status is required");
        this.displayName = requireDisplayName(displayName);
        this.avatarUrl = normalizeOptional(avatarUrl);
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update time is required");
        this.version = version;
    }

    public static User register(String displayName, String avatarUrl, Instant now) {
        return new User(UserId.newId(), AccountStatus.ACTIVE, displayName, avatarUrl, now, now, 0);
    }

    public static User rehydrate(
        UserId id,
        AccountStatus status,
        String displayName,
        String avatarUrl,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {
        return new User(id, status, displayName, avatarUrl, createdAt, updatedAt, version);
    }

    public void assertAuthenticationAllowed() {
        if (!status.permitsAuthentication()) {
            throw new DisabledAccountException();
        }
    }

    public UserId id() {
        return id;
    }

    public AccountStatus status() {
        return status;
    }

    public String displayName() {
        return displayName;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    private static String requireDisplayName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Display name is required");
        }
        String normalized = value.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Display name exceeds 120 characters");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
