package com.devpath.ai.adapter.out.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.devpath.ai.config.AiGenerationProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OllamaGenerationAdapterTest {
    @Test
    void normalizesARecordedStructuredResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ollama.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ollama.test/api/chat")).andRespond(withSuccess(
            "{\"message\":{\"content\":\"{\\\"summary\\\":\\\"grounded\\\"}\"},\"prompt_eval_count\":12,\"eval_count\":7}",
            MediaType.APPLICATION_JSON));
        var adapter = new OllamaGenerationAdapter(builder.build(), new AiGenerationProperties(
            "http://ollama.test", "qwen-test", Duration.ofSeconds(1), Duration.ofSeconds(2), 2));

        var result = adapter.generate("prompt");

        assertThat(result.content()).contains("summary");
        assertThat(result.promptTokens()).isEqualTo(12);
        assertThat(result.completionTokens()).isEqualTo(7);
        server.verify();
    }
}
