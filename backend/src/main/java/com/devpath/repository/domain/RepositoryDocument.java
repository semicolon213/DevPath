package com.devpath.repository.domain;

import java.util.List;
import java.util.Objects;

public record RepositoryDocument(
    String documentType,
    String path,
    String contentHash,
    long byteSize,
    List<String> qualitySignals
) {
    private static final List<String> ALLOWED_SIGNALS = List.of(
        "OVERVIEW", "SETUP", "USAGE", "ARCHITECTURE", "TESTING", "LICENSE"
    );

    public RepositoryDocument {
        Objects.requireNonNull(documentType);
        Objects.requireNonNull(path);
        Objects.requireNonNull(contentHash);
        Objects.requireNonNull(qualitySignals);
        qualitySignals = qualitySignals.stream().distinct().sorted().toList();
        if (!documentType.equals("README") || path.isBlank() || path.length() > 1000
            || !contentHash.matches("[a-f0-9]{64}") || byteSize < 0
            || !ALLOWED_SIGNALS.containsAll(qualitySignals)) {
            throw new IllegalArgumentException("Repository document fact is invalid");
        }
    }
}
