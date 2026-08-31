package com.devpath.ai.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("devpath.ai.generation")
public record AiGenerationProperties(
    String baseUrl, String model, Duration connectTimeout, Duration readTimeout, int maxAttempts
) {
    public AiGenerationProperties {
        if (baseUrl == null || baseUrl.isBlank() || model == null || model.isBlank()
            || connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
            || readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()
            || maxAttempts < 1 || maxAttempts > 2) {
            throw new IllegalArgumentException("AI generation configuration is invalid");
        }
    }
}
