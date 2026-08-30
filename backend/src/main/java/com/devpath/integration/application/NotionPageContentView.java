package com.devpath.integration.application;

import java.time.Instant;
import java.util.UUID;

public record NotionPageContentView(
    UUID connectionId,
    String providerPageId,
    String title,
    Instant sourceUpdatedAt,
    String normalizedContent
) {}
