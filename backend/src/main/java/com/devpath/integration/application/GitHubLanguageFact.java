package com.devpath.integration.application;

public record GitHubLanguageFact(String providerLabel, long byteCount) {
    public GitHubLanguageFact {
        if (providerLabel == null || providerLabel.isBlank() || providerLabel.length() > 128 || byteCount < 0) {
            throw new IllegalArgumentException("GitHub language fact is invalid");
        }
    }
}
