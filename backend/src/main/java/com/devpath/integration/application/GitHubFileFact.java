package com.devpath.integration.application;

public record GitHubFileFact(String path, String blobSha, long byteSize) {
    public GitHubFileFact {
        if (path == null || path.isBlank() || path.length() > 1000 || path.startsWith("/") || path.contains("../")
            || blobSha == null || !blobSha.matches("[a-fA-F0-9]{40,64}") || byteSize < 0) {
            throw new IllegalArgumentException("GitHub file fact is invalid");
        }
    }
}
