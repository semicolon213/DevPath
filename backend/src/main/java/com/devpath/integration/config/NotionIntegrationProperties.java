package com.devpath.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("devpath.notion.integration")
public record NotionIntegrationProperties(
    String clientId,
    String clientSecret,
    String redirectUri,
    String credentialKey,
    String credentialKeyVersion
) {
    public boolean configured() {
        return hasText(clientId) && hasText(clientSecret) && hasText(redirectUri)
            && hasText(credentialKey) && hasText(credentialKeyVersion);
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
