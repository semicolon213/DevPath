package com.devpath.ai.application;

import com.devpath.rule.application.SkillAssessmentView;
import com.devpath.rule.application.SkillMatrixView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SkillExplanationValidator {
    public static final String VERSION = "skill-explanation-validator-v1";
    private static final Pattern NUMBER = Pattern.compile("\\p{N}");
    private static final Pattern UNSAFE = Pattern.compile("(?i)<|javascript:|on(?:click|error|load)\\s*=");
    private final ObjectMapper mapper;

    public SkillExplanationValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public SkillExplanationValidation validate(String raw, SkillMatrixView matrix) {
        List<String> violations = new ArrayList<>();
        if (raw == null || raw.isBlank() || raw.length() > 20_000) {
            return new SkillExplanationValidation(null, List.of("MALFORMED_OR_EMPTY_RESPONSE"));
        }
        try {
            JsonNode root = mapper.readTree(raw);
            if (!root.isObject() || !fields(root).equals(Set.of("summary", "strengths", "improvementAreas"))) {
                return new SkillExplanationValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            }
            SkillExplanationContent content = mapper.treeToValue(root, SkillExplanationContent.class);
            validateText(content.summary(), 2000, violations);
            validateItems(content.strengths(), matrix, SkillAssessmentView::strength, violations);
            validateItems(content.improvementAreas(), matrix, SkillAssessmentView::weakness, violations);
            validateCoverage(content.strengths(), matrix, SkillAssessmentView::strength, violations);
            validateCoverage(content.improvementAreas(), matrix, SkillAssessmentView::weakness, violations);
            return new SkillExplanationValidation(content, violations.stream().distinct().toList());
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            return new SkillExplanationValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
        }
    }

    private static void validateItems(
        List<SkillExplanationContent.Item> items, SkillMatrixView matrix,
        Predicate<SkillAssessmentView> expectedState, List<String> violations
    ) {
        if (items.size() > 20) violations.add("INVALID_RESPONSE_SCHEMA");
        Map<String, SkillAssessmentView> skills = matrix.skills().stream()
            .collect(Collectors.toMap(SkillAssessmentView::skillKey, value -> value));
        Set<String> seen = new HashSet<>();
        for (var item : items) {
            SkillAssessmentView source = item.skillKey() == null ? null : skills.get(item.skillKey());
            if (source == null || !expectedState.test(source) || !seen.add(item.skillKey())) {
                violations.add("UNSUPPORTED_SKILL_CLAIM");
            }
            validateText(item.explanation(), 1000, violations);
            if (item.evidenceIds().size() > 20 || source == null
                || !source.evidenceIds().isEmpty() && item.evidenceIds().isEmpty()
                || !new HashSet<>(source.evidenceIds()).containsAll(item.evidenceIds())) {
                violations.add("UNSUPPORTED_EVIDENCE_REFERENCE");
            }
        }
    }

    private static void validateCoverage(
        List<SkillExplanationContent.Item> items, SkillMatrixView matrix,
        Predicate<SkillAssessmentView> expectedState, List<String> violations
    ) {
        Set<String> expected = matrix.skills().stream().filter(expectedState).map(SkillAssessmentView::skillKey)
            .collect(Collectors.toSet());
        Set<String> actual = items.stream().map(SkillExplanationContent.Item::skillKey).collect(Collectors.toSet());
        if (!actual.equals(expected)) violations.add("INCOMPLETE_SKILL_EXPLANATION");
    }

    private static void validateText(String value, int maxLength, List<String> violations) {
        if (value == null || value.isBlank() || value.length() > maxLength) violations.add("INVALID_RESPONSE_SCHEMA");
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
