package com.devpath.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class UserProfile {
    private final UUID id;
    private final UserId userId;
    private final Instant createdAt;
    private CareerStage careerStage;
    private String bio;
    private Instant updatedAt;
    private long version;

    private UserProfile(UUID id, UserId userId, CareerStage careerStage, String bio,
                        Instant createdAt, Instant updatedAt, long version) {
        this.id = Objects.requireNonNull(id, "Profile ID is required");
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.careerStage = careerStage;
        this.bio = normalizeBio(bio);
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update time is required");
        this.version = version;
    }

    public static UserProfile create(UserId userId, Instant now) {
        return new UserProfile(UUID.randomUUID(), userId, null, null, now, now, 0);
    }

    public static UserProfile rehydrate(UUID id, UserId userId, CareerStage careerStage, String bio,
                                        Instant createdAt, Instant updatedAt, long version) {
        return new UserProfile(id, userId, careerStage, bio, createdAt, updatedAt, version);
    }

    public void update(CareerStage careerStage, String bio, Instant now) {
        this.careerStage = careerStage;
        this.bio = normalizeBio(bio);
        this.updatedAt = Objects.requireNonNull(now, "Update time is required");
    }

    private static String normalizeBio(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 1000) throw new IllegalArgumentException("Bio exceeds 1000 characters");
        return normalized;
    }

    public UUID id() { return id; }
    public UserId userId() { return userId; }
    public CareerStage careerStage() { return careerStage; }
    public String bio() { return bio; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}
