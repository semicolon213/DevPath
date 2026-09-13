package com.devpath.ai.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class GeneratedOutputPolicy {
    private static final Pattern UNSAFE = Pattern.compile("(?i)<|javascript:|on(?:click|error|load)\\s*=");
    private final ObjectMapper mapper;

    GeneratedOutputPolicy(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    List<String> violations(String raw, String trustedSystemPrompt) {
        if (raw == null) return List.of();
        try {
            List<String> textValues = new ArrayList<>();
            JsonNode root = mapper.readTree(raw);
            if (root != null) textValues(root, textValues);
            List<String> violations = new ArrayList<>(renderSafetyViolations(textValues));
            if (containsTrustedSystemPrompt(textValues, trustedSystemPrompt)) {
                violations.add("TRUSTED_INSTRUCTION_DISCLOSURE");
            }
            return violations.stream().distinct().toList();
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    static List<String> renderSafetyViolations(String value) {
        return value != null && UNSAFE.matcher(value).find() ? List.of("UNSAFE_RENDERED_CONTENT") : List.of();
    }

    private static List<String> renderSafetyViolations(List<String> values) {
        return values.stream().anyMatch(value -> UNSAFE.matcher(value).find())
            ? List.of("UNSAFE_RENDERED_CONTENT") : List.of();
    }

    private static boolean containsTrustedSystemPrompt(List<String> values, String trustedSystemPrompt) {
        return trustedSystemPrompt != null && !trustedSystemPrompt.isBlank()
            && (values.stream().anyMatch(value -> value.contains(trustedSystemPrompt))
                || String.join("", values).contains(trustedSystemPrompt));
    }

    private static void textValues(JsonNode node, List<String> values) {
        if (node.isTextual()) values.add(node.textValue());
        node.forEach(child -> textValues(child, values));
    }
}
