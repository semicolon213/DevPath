package com.devpath.integration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class GitHubIntegrationConfigurationTest {
    private final GitHubIntegrationConfiguration configuration = new GitHubIntegrationConfiguration();

    @Test
    void createsABoundedProviderHttpClient() {
        assertThat(configuration.githubRestClientBuilder(Duration.ofSeconds(5), Duration.ofSeconds(30)))
            .isNotNull();
    }

    @Test
    void rejectsAnUnboundedTimeoutConfiguration() {
        assertThatThrownBy(() -> configuration.githubRestClientBuilder(Duration.ZERO, Duration.ofSeconds(30)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> configuration.githubRestClientBuilder(Duration.ofSeconds(5), Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
