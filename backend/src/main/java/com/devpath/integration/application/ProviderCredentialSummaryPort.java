package com.devpath.integration.application;

import java.util.List;
import java.util.UUID;

public interface ProviderCredentialSummaryPort {
    List<ConnectedAccountView> findByUserId(UUID userId);
}
