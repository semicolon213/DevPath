package com.devpath.integration.application;

import java.util.List;

public record GitHubDocumentFact(
    String documentType,
    String path,
    String contentHash,
    long byteSize,
    List<String> qualitySignals
) {
    public GitHubDocumentFact {
        qualitySignals = List.copyOf(qualitySignals);
    }
}
