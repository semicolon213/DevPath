package com.devpath.ai.application;

import com.devpath.recommendation.application.RecommendationSetView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CareerAdviceValidator {
    public static final String VERSION = "career-advice-validator-v2";
    private final ObjectMapper mapper;

    public CareerAdviceValidator(ObjectMapper mapper) { this.mapper = mapper; }

    public CareerAdviceValidation validate(String raw, RecommendationSetView recommendations) {
        if (raw == null || raw.isBlank() || raw.length() > 20_000) {
            return new CareerAdviceValidation(null, List.of("MALFORMED_OR_EMPTY_RESPONSE"));
        }
        try {
            JsonNode root = mapper.readTree(raw);
            if (!root.isObject() || !GeneratedOutputPolicy.fields(root).equals(Set.of("readinessSummary", "strengthExplanation",
                "gapExplanation", "nextSteps", "confidenceNotice")) || !root.path("nextSteps").isArray()) {
                return new CareerAdviceValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            }
            for (JsonNode step : root.path("nextSteps")) {
                if (!step.isObject() || !GeneratedOutputPolicy.fields(step).equals(Set.of("recommendationId", "advice"))) {
                    return new CareerAdviceValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
                }
            }
            CareerAdviceContent content = mapper.treeToValue(root, CareerAdviceContent.class);
            List<String> violations = new ArrayList<>();
            GeneratedOutputPolicy.validateText(content.readinessSummary(), 2000, violations);
            GeneratedOutputPolicy.validateText(content.strengthExplanation(), 2000, violations);
            GeneratedOutputPolicy.validateText(content.gapExplanation(), 2000, violations);
            GeneratedOutputPolicy.validateText(content.confidenceNotice(), 2000, violations);
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
        steps.forEach(step -> GeneratedOutputPolicy.validateText(step.advice(), 2000, violations));
    }
}
