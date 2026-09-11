package com.devpath.prompt.application;

public record PromptPackage(String contextPayload, String prompt, String contextHash, int tokenBudget) {
    // Covers the fixed provider system guard and its two-newline separator.
    public static final int PROVIDER_ENVELOPE_RESERVE_CHARS = 161;

    public static int estimatedTokenCount(String instructions, String contextPayload) {
        return (instructions.length() + contextPayload.length() + PROVIDER_ENVELOPE_RESERVE_CHARS + 3) / 4;
    }
}
