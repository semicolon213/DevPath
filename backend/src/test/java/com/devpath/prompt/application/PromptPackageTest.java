package com.devpath.prompt.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PromptPackageTest {
    @Test
    void includesTheFixedProviderEnvelopeAtTheTokenBudgetBoundary() {
        String instructions = "a".repeat(8192 * 4 - PromptPackage.PROVIDER_ENVELOPE_RESERVE_CHARS + 1);

        assertThat(PromptPackage.estimatedTokenCount(instructions, "")).isEqualTo(8193);
    }
}
