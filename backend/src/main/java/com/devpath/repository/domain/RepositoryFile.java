package com.devpath.repository.domain;

public record RepositoryFile(String path, String blobSha, long byteSize, String extractorVersion) {
    public static final String EXTRACTOR_VERSION = "repository-tree-extractor-v1";

    public RepositoryFile {
        if (path == null || path.isBlank() || path.length() > 1000 || path.startsWith("/") || path.contains("../")
            || blobSha == null || !blobSha.matches("[a-fA-F0-9]{40,64}") || byteSize < 0
            || !EXTRACTOR_VERSION.equals(extractorVersion)) {
            throw new IllegalArgumentException("Repository file evidence is invalid");
        }
    }

    public static RepositoryFile normalized(String path, String blobSha, long byteSize) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("./")) normalized = normalized.substring(2);
        return new RepositoryFile(normalized, blobSha.toLowerCase(), byteSize, EXTRACTOR_VERSION);
    }
}
