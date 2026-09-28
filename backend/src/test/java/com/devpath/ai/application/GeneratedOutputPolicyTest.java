package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeneratedOutputPolicyTest {
    private final GeneratedOutputPolicy policy = new GeneratedOutputPolicy(new ObjectMapper());

    @Test
    void rejectsExactTrustedInstructionsIncludingAcrossTextNodesAndRenderUnsafeText() {
        String prompt = "Trusted instruction: return only schema v1.";

        assertThat(policy.violations("{\"nested\":[\"Trusted instruction: return only schema\",{\"text\":\""
            + prompt + "\"}]}", prompt)).containsExactly("TRUSTED_INSTRUCTION_DISCLOSURE");
        assertThat(policy.violations("{\"parts\":[\"Trusted instruction: return \",\"only schema v1.\"]}", prompt))
            .containsExactly("TRUSTED_INSTRUCTION_DISCLOSURE");
        assertThat(policy.violations("{\"text\":\"<b>unsafe</b>\",\"link\":\"javascript:alert(1)\"}", prompt))
            .containsExactly("UNSAFE_RENDERED_CONTENT");
        assertThat(policy.violations("{\"text\":\"Trusted instruction: return only schema\"}", prompt)).isEmpty();
    }

    @Test
    void validatesTextWithTheExistingSchemaNumericAndRenderSafetyOrder() {
        List<String> violations = new ArrayList<>();

        GeneratedOutputPolicy.validateText(null, 3, violations);
        GeneratedOutputPolicy.validateText("   ", 3, violations);
        GeneratedOutputPolicy.validateText("abc", 3, violations);
        GeneratedOutputPolicy.validateText("abcd", 3, violations);
        GeneratedOutputPolicy.validateText("٣", 3, violations);
        GeneratedOutputPolicy.validateText("<b>x</b>", 10, violations);
        GeneratedOutputPolicy.validateText("1<script>", 3, violations);

        assertThat(violations).containsExactly(
            "INVALID_RESPONSE_SCHEMA", "INVALID_RESPONSE_SCHEMA", "INVALID_RESPONSE_SCHEMA",
            "UNTRACEABLE_NUMERIC_CLAIM", "UNSAFE_RENDERED_CONTENT", "INVALID_RESPONSE_SCHEMA");
    }
}
