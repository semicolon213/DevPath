package com.devpath.identity.domain;

public record ProviderSubject(String value) {
    public ProviderSubject {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Provider subject is required");
        }
        value = value.trim();
        if (value.length() > 255) {
            throw new IllegalArgumentException("Provider subject exceeds 255 characters");
        }
    }
}
