package com.devpath.prompt.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PromptSecretDetectorTest {
    @Test
    void rejectsHighConfidenceSecretsButNotShortOrPlaceholderValues() {
        List<String> secrets = List.of(
            "-----BEGIN" + " PRIVATE KEY-----",
            "ghp_" + "a".repeat(30), "gho_" + "a".repeat(30), "ghu_" + "a".repeat(30),
            "ghs_" + "a".repeat(30), "ghr_" + "a".repeat(30),
            "github_pat_" + "a".repeat(40),
            "sk-proj-" + "a".repeat(20),
            "AKIA" + "A".repeat(16),
            "password=${PROMPT_VALUE}"
        );
        List<String> safeValues = List.of(
            "ghp_" + "a".repeat(29), "github_pat_example", "sk-example", "AKIA" + "A".repeat(15),
            "GITHUB_TOKEN=${GITHUB_TOKEN}", "password"
        );

        secrets.forEach(value -> assertEquals("Prompt context contains sensitive data",
            assertThrows(IllegalArgumentException.class, () -> PromptSecretDetector.reject(value)).getMessage()));
        safeValues.forEach(value -> assertDoesNotThrow(() -> PromptSecretDetector.reject(value)));
    }
}
