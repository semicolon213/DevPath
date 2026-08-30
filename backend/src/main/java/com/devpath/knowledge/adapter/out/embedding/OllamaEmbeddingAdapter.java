package com.devpath.knowledge.adapter.out.embedding;

import com.devpath.knowledge.application.EmbeddingPort;
import com.devpath.knowledge.application.EmbeddingVector;
import com.devpath.knowledge.config.KnowledgeEmbeddingProperties;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaEmbeddingAdapter implements EmbeddingPort {
    private final RestClient client;
    private final KnowledgeEmbeddingProperties properties;

    public OllamaEmbeddingAdapter(@Qualifier("embeddingRestClient") RestClient client,
        KnowledgeEmbeddingProperties properties) {
        this.client = client; this.properties = properties;
    }

    @Override
    public EmbeddingVector embed(String content) {
        OllamaEmbeddingResponse response = client.post().uri("/api/embed")
            .body(Map.of("model", properties.model(), "input", content)).retrieve()
            .body(OllamaEmbeddingResponse.class);
        if (response == null || response.embeddings() == null || response.embeddings().size() != 1) {
            throw new IllegalStateException("Embedding provider returned an invalid response");
        }
        List<Double> values = response.embeddings().getFirst();
        if (values == null || values.size() != properties.dimension()
            || values.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new IllegalStateException("Embedding provider returned an invalid vector");
        }
        return new EmbeddingVector("OLLAMA", properties.model(), properties.modelVersion(),
            properties.dimension(), values);
    }

    private record OllamaEmbeddingResponse(List<List<Double>> embeddings) {}
}
