package com.devpath.integration.application;

import java.util.List;

public record ConnectedAccountListView(List<ConnectedAccountView> connections) {
    public ConnectedAccountListView {
        connections = List.copyOf(connections);
    }
}
