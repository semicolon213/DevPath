package com.devpath.knowledge.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("devpath.knowledge.embedding")
public record KnowledgeEmbeddingProperties(
    String baseUrl, String model, String modelVersion, int dimension, Duration connectTimeout, Duration readTimeout
) {
    public KnowledgeEmbeddingProperties {
        if (baseUrl == null || baseUrl.isBlank() || model == null || model.isBlank() || modelVersion == null
            || modelVersion.isBlank() || dimension != 768 || connectTimeout == null || readTimeout == null
            || connectTimeout.isNegative() || connectTimeout.isZero() || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("Knowledge embedding configuration is invalid");
        }
    }
}
