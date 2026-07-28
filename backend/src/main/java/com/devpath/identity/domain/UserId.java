package com.devpath.identity.domain;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "User ID is required");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
