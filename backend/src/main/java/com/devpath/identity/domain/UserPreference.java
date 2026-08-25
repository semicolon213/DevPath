package com.devpath.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class UserPreference {
    private final UUID id;
    private final UserId userId;
    private final PreferenceType type;
    private final String selectedValue;
    private final String catalogVersion;
    private final Instant selectedAt;
    private boolean active;
    private Instant supersededAt;
    private long version;

    private UserPreference(UUID id, UserId userId, PreferenceType type, String selectedValue, String catalogVersion,
                           boolean active, Instant selectedAt, Instant supersededAt, long version) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.type = Objects.requireNonNull(type);
        this.selectedValue = requireSlug(selectedValue);
        this.catalogVersion = Objects.requireNonNull(catalogVersion, "Catalog version is required");
        this.active = active;
        this.selectedAt = Objects.requireNonNull(selectedAt);
        this.supersededAt = supersededAt;
        this.version = version;
    }

    public static UserPreference select(UserId userId, PreferenceType type, String value, String catalogVersion, Instant now) {
        return new UserPreference(UUID.randomUUID(), userId, type, value, catalogVersion, true, now, null, 0);
    }

    public static UserPreference rehydrate(UUID id, UserId userId, PreferenceType type, String value, String catalogVersion,
                                           boolean active, Instant selectedAt, Instant supersededAt, long version) {
        return new UserPreference(id, userId, type, value, catalogVersion, active, selectedAt, supersededAt, version);
    }

    public void supersede(Instant now) {
        if (active) { active = false; supersededAt = Objects.requireNonNull(now); }
    }

    private static String requireSlug(String value) {
        if (value == null || !value.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || value.length() > 64) {
            throw new IllegalArgumentException("Target ID must be a lowercase slug");
        }
        return value;
    }

    public UUID id() { return id; }
    public UserId userId() { return userId; }
    public PreferenceType type() { return type; }
    public String selectedValue() { return selectedValue; }
    public String catalogVersion() { return catalogVersion; }
    public boolean active() { return active; }
    public Instant selectedAt() { return selectedAt; }
    public Instant supersededAt() { return supersededAt; }
    public long version() { return version; }
}
