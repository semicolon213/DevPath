package com.devpath.ai.application;

import com.devpath.recommendation.application.RecommendationSetView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CareerAdviceValidator {
    public static final String VERSION = "career-advice-validator-v1";
    private static final Pattern NUMBER = Pattern.compile("\\p{N}");
    private static final Pattern UNSAFE = Pattern.compile("(?i)<|javascript:|on(?:click|error|load)\\s*=");
    private final ObjectMapper mapper;

    public CareerAdviceValidator(ObjectMapper mapper) { this.mapper = mapper; }

    public CareerAdviceValidation validate(String raw, RecommendationSetView recommendations) {
        if (raw == null || raw.isBlank() || raw.length() > 20_000) {
            return new CareerAdviceValidation(null, List.of("MALFORMED_OR_EMPTY_RESPONSE"));
        }
        try {
            JsonNode root = mapper.readTree(raw);
            if (!root.isObject() || !fields(root).equals(Set.of("readinessSummary", "strengthExplanation",
                "gapExplanation", "nextSteps", "confidenceNotice")) || !root.path("nextSteps").isArray()) {
                return new CareerAdviceValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            }
            for (JsonNode step : root.path("nextSteps")) {
                if (!step.isObject() || !fields(step).equals(Set.of("recommendationId", "advice"))) {
                    return new CareerAdviceValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
                }
            }
            CareerAdviceContent content = mapper.treeToValue(root, CareerAdviceContent.class);
            List<String> violations = new ArrayList<>();
            validateText(content.readinessSummary(), violations);
            validateText(content.strengthExplanation(), violations);
            validateText(content.gapExplanation(), violations);
            validateText(content.confidenceNotice(), violations);
            validateSteps(content.nextSteps(), recommendations, violations);
            return new CareerAdviceValidation(content, violations.stream().distinct().toList());
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            return new CareerAdviceValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
        }
    }

    private static void validateSteps(
        List<CareerAdviceContent.NextStep> steps, RecommendationSetView source, List<String> violations
    ) {
        List<UUID> expected = source.recommendations().stream().limit(3)
            .map(value -> value.recommendationId()).toList();
        if (steps == null || !steps.stream().map(CareerAdviceContent.NextStep::recommendationId).toList().equals(expected)) {
            violations.add("RECOMMENDATION_ORDER_CHANGED");
            return;
        }
        steps.forEach(step -> validateText(step.advice(), violations));
    }

    private static void validateText(String value, List<String> violations) {
        if (value == null || value.isBlank() || value.length() > 2000) violations.add("INVALID_RESPONSE_SCHEMA");
        else {
            if (NUMBER.matcher(value).find()) violations.add("UNTRACEABLE_NUMERIC_CLAIM");
            if (UNSAFE.matcher(value).find()) violations.add("UNSAFE_RENDERED_CONTENT");
        }
    }

    private static Set<String> fields(JsonNode node) {
        Set<String> values = new HashSet<>();
        node.fieldNames().forEachRemaining(values::add);
        return values;
    }
}
