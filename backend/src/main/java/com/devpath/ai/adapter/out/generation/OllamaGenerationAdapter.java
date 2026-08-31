package com.devpath.ai.adapter.out.generation;

import com.devpath.ai.application.GenerationProviderException;
import com.devpath.ai.application.GenerationProviderPort;
import com.devpath.ai.application.GenerationProviderResult;
import com.devpath.ai.config.AiGenerationProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OllamaGenerationAdapter implements GenerationProviderPort {
    private final RestClient client;
    private final AiGenerationProperties properties;

    public OllamaGenerationAdapter(
        @Qualifier("generationRestClient") RestClient client, AiGenerationProperties properties
    ) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public GenerationProviderResult generate(String prompt) {
        try {
            OllamaResponse response = client.post().uri("/api/chat").body(Map.of(
                "model", properties.model(),
                "stream", false,
                "format", "json",
                "messages", List.of(Map.of("role", "user", "content", prompt))
            )).retrieve().body(OllamaResponse.class);
            if (response == null || response.message() == null || response.message().content() == null
                || response.message().content().isBlank()) {
                throw new GenerationProviderException("AI_PROVIDER_INVALID_RESPONSE", null);
            }
            return new GenerationProviderResult(response.message().content(), response.promptEvalCount(), response.evalCount());
        } catch (GenerationProviderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new GenerationProviderException("AI_PROVIDER_UNAVAILABLE", exception);
        }
    }

    private record OllamaResponse(
        Message message,
        @JsonProperty("prompt_eval_count") Integer promptEvalCount,
        @JsonProperty("eval_count") Integer evalCount
    ) {}
    private record Message(String content) {}
}
