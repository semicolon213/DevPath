package com.devpath.identity.domain;

import java.util.Objects;
import java.util.UUID;

public record ExternalIdentityId(UUID value) {
    public ExternalIdentityId {
        Objects.requireNonNull(value, "External identity ID is required");
    }

    public static ExternalIdentityId newId() {
        return new ExternalIdentityId(UUID.randomUUID());
    }
}
