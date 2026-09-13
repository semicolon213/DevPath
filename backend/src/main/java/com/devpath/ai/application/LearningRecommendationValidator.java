package com.devpath.ai.application;

import com.devpath.learning.application.LearningRoadmapView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LearningRecommendationValidator {
    public static final String VERSION = "learning-recommendation-validator-v2";
    private static final Pattern NUMBER = Pattern.compile("\\p{N}");
    private final ObjectMapper mapper;

    public LearningRecommendationValidator(ObjectMapper mapper) { this.mapper = mapper; }

    public LearningRecommendationValidation validate(String raw, LearningRoadmapView roadmap) {
        if (raw == null || raw.isBlank() || raw.length() > 30_000) {
            return new LearningRecommendationValidation(null, List.of("MALFORMED_OR_EMPTY_RESPONSE"));
        }
        try {
            JsonNode root = mapper.readTree(raw);
            if (!root.isObject() || !fields(root).equals(Set.of("recommendationSummary", "steps"))
                || !root.path("steps").isArray()) {
                return new LearningRecommendationValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            }
            List<RawStep> rawSteps = new ArrayList<>();
            for (JsonNode step : root.path("steps")) {
                if (!step.isObject() || !fields(step).equals(Set.of(
                    "position", "milestoneExplanation", "objective", "practiceTask"))) {
                    return new LearningRecommendationValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
                }
                rawSteps.add(mapper.treeToValue(step, RawStep.class));
            }
            List<String> violations = new ArrayList<>();
            String summary = root.path("recommendationSummary").isTextual()
                ? root.path("recommendationSummary").textValue() : null;
            validateText(summary, violations);
            if (rawSteps.size() != roadmap.steps().size()) violations.add("INCOMPLETE_LEARNING_PLAN");
            List<LearningRecommendationContent.Step> steps = new ArrayList<>();
            for (int index = 0; index < Math.min(rawSteps.size(), roadmap.steps().size()); index++) {
                RawStep generated = rawSteps.get(index);
                var source = roadmap.steps().get(index);
                if (generated.position() != source.position()) violations.add("ROADMAP_ORDER_CHANGED");
                validateText(generated.milestoneExplanation(), violations);
                validateText(generated.objective(), violations);
                validateText(generated.practiceTask(), violations);
                steps.add(new LearningRecommendationContent.Step(source.roadmapStepId(), source.position(),
                    generated.milestoneExplanation(), generated.objective(), generated.practiceTask(),
                    source.completionCriteria(), source.expectedEvidence()));
            }
            return new LearningRecommendationValidation(
                new LearningRecommendationContent(summary, steps), violations.stream().distinct().toList());
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            return new LearningRecommendationValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
        }
    }

    private static void validateText(String value, List<String> violations) {
        if (value == null || value.isBlank() || value.length() > 2000) violations.add("INVALID_RESPONSE_SCHEMA");
        else {
            if (NUMBER.matcher(value).find()) violations.add("UNTRACEABLE_NUMERIC_CLAIM");
            violations.addAll(GeneratedOutputPolicy.renderSafetyViolations(value));
        }
    }

    private static Set<String> fields(JsonNode node) {
        Set<String> values = new HashSet<>();
        node.fieldNames().forEachRemaining(values::add);
        return values;
    }

    private record RawStep(int position, String milestoneExplanation, String objective, String practiceTask) {}
}
