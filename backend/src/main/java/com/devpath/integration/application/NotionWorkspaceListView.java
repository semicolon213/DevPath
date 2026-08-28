package com.devpath.integration.application;

import java.util.List;

public record NotionWorkspaceListView(List<NotionWorkspaceView> workspaces) {
    public NotionWorkspaceListView {
        workspaces = List.copyOf(workspaces);
    }
}
