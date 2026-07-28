package com.devpath.shared.api;

import java.time.Instant;

public record ApiMetadata(String requestId, String apiVersion, Instant timestamp) {
    public static ApiMetadata create(String requestId) {
        return new ApiMetadata(requestId, "v1", Instant.now());
    }
}
