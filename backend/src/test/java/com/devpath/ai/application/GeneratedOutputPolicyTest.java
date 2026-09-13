package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
