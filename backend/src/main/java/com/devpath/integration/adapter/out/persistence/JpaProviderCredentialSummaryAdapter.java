package com.devpath.integration.adapter.out.persistence;

import com.devpath.integration.application.ConnectedAccountView;
import com.devpath.integration.application.ProviderCredentialSummaryPort;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaProviderCredentialSummaryAdapter implements ProviderCredentialSummaryPort {
    private final ProviderCredentialJpaRepository repository;
    private final EncryptedNotionCredentialStore notion;

    JpaProviderCredentialSummaryAdapter(ProviderCredentialJpaRepository repository, EncryptedNotionCredentialStore notion) {
        this.repository = repository;
        this.notion = notion;
    }

    @Override
    public List<ConnectedAccountView> findByUserId(UUID userId) {
        var github = repository.findAllByUserIdOrderByConnectedAtAsc(userId).stream()
            .map(entity -> new ConnectedAccountView(
                entity.id(),
                entity.provider(),
                entity.status(),
                parseScopes(entity.scopeSummary()),
                entity.connectedAt(),
                entity.refreshTokenExpiresAt() == null ? entity.expiresAt() : entity.refreshTokenExpiresAt()
            ))
            .toList();
        return java.util.stream.Stream.concat(github.stream(), notion.findAllViews(userId).stream())
            .sorted(java.util.Comparator.comparing(ConnectedAccountView::connectedAt))
            .toList();
    }

    private List<String> parseScopes(String value) {
        if (value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(" "))
            .filter(scope -> !scope.isBlank())
            .sorted()
            .toList();
    }
}
