package com.devpath.integration.config;

import java.time.Duration;
import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GitHubIntegrationProperties.class)
public class GitHubIntegrationConfiguration {
    @Bean("githubRestClientBuilder")
    RestClient.Builder githubRestClientBuilder(
        @Value("${devpath.github.integration.connect-timeout:5s}") Duration connectTimeout,
        @Value("${devpath.github.integration.read-timeout:30s}") Duration readTimeout
    ) {
        if (connectTimeout.isZero() || connectTimeout.isNegative()
            || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("GitHub HTTP timeouts must be positive");
        }
        var requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
