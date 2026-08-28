package com.devpath.integration.application;

import java.time.Instant;

public record NotionWorkspacePageView(
    String providerPageId,
    String title,
    String objectType,
    String url,
    Instant lastEditedAt,
    boolean inTrash
) {}
