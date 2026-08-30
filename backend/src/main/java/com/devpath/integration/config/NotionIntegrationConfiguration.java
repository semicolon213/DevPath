package com.devpath.integration.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class NotionIntegrationConfiguration {
    @Bean("notionRestClientBuilder")
    RestClient.Builder notionRestClientBuilder(
        @Value("${devpath.notion.integration.connect-timeout:5s}") Duration connectTimeout,
        @Value("${devpath.notion.integration.read-timeout:30s}") Duration readTimeout
    ) {
        if (connectTimeout.isZero() || connectTimeout.isNegative() || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("Notion HTTP timeouts must be positive");
        }
        var requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
