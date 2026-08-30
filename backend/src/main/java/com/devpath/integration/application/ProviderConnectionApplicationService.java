package com.devpath.integration.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderConnectionApplicationService {
    private final ProviderCredentialSummaryPort credentials;
    private final Clock clock;

    public ProviderConnectionApplicationService(ProviderCredentialSummaryPort credentials, Clock clock) {
        this.credentials = credentials;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ConnectedAccountListView listFor(UUID userId) {
        var now = clock.instant();
        var connections = credentials.findByUserId(Objects.requireNonNull(userId)).stream()
            .map(connection -> "ACTIVE".equals(connection.status())
                && connection.expiresAt() != null && !connection.expiresAt().isAfter(now)
                ? new ConnectedAccountView(connection.connectionId(), connection.provider(), "EXPIRED", List.of(),
                    connection.connectedAt(), connection.expiresAt())
                : connection)
            .toList();
        return new ConnectedAccountListView(connections);
    }
}
