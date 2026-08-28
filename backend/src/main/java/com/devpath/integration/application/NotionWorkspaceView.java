package com.devpath.integration.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotionWorkspaceView(
    UUID connectionId,
    String workspaceId,
    String workspaceName,
    String workspaceIconUrl,
    String status,
    Instant connectedAt,
    Instant discoveredAt,
    List<NotionWorkspacePageView> pages
) {
    public NotionWorkspaceView {
        pages = List.copyOf(pages);
    }
}
