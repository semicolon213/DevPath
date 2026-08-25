package com.devpath.integration.application;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderConnectionApplicationService implements ListConnectedAccountsUseCase {
    private final ProviderCredentialSummaryPort credentials;
    private final Clock clock;

    public ProviderConnectionApplicationService(ProviderCredentialSummaryPort credentials, Clock clock) {
        this.credentials = credentials;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectedAccountListView listFor(UUID userId) {
        var now = clock.instant();
        var activeConnections = credentials.findActiveByUserId(Objects.requireNonNull(userId)).stream()
            .filter(connection -> connection.expiresAt() == null || connection.expiresAt().isAfter(now))
            .toList();
        return new ConnectedAccountListView(activeConnections);
    }
}
