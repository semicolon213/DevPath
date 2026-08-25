package com.devpath.integration.application;

import java.util.UUID;

public interface ListConnectedAccountsUseCase {
    ConnectedAccountListView listFor(UUID userId);
}
