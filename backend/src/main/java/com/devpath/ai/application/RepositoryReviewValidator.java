package com.devpath.ai.application;

import com.devpath.rule.application.RuleEvidenceListView;
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
public class RepositoryReviewValidator {
    public static final String VERSION = "repository-review-validator-v1";
    private static final Set<String> CATEGORIES = Set.of(
        "ARCHITECTURE", "TESTING", "DEVOPS", "DOCUMENTATION", "COLLABORATION"
    );
    private static final Pattern NUMBER = Pattern.compile("\\p{N}");
    private static final Pattern UNSAFE = Pattern.compile("(?i)<|javascript:|on(?:click|error|load)\\s*=");
    private final ObjectMapper mapper;

    public RepositoryReviewValidator(ObjectMapper mapper) { this.mapper = mapper; }

    public RepositoryReviewValidation validate(String raw, RuleEvidenceListView source) {
        if (raw == null || raw.isBlank() || raw.length() > 30_000) {
            return new RepositoryReviewValidation(null, List.of("MALFORMED_OR_EMPTY_RESPONSE"));
        }
        try {
            JsonNode root = mapper.readTree(raw);
            if (!root.isObject() || !fields(root).equals(Set.of("summary", "sections"))) {
                return new RepositoryReviewValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            }
            if (!root.path("sections").isArray()) {
                return new RepositoryReviewValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            }
            for (JsonNode section : root.path("sections")) {
                if (!section.isObject() || !fields(section).equals(Set.of("category", "review", "evidenceIds"))) {
                    return new RepositoryReviewValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
                }
            }
            RepositoryReviewContent content = mapper.treeToValue(root, RepositoryReviewContent.class);
            List<String> violations = new ArrayList<>();
            validateText(content.summary(), violations);
            validateSections(content.sections(), source, violations);
            return new RepositoryReviewValidation(content, violations.stream().distinct().toList());
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            return new RepositoryReviewValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
        }
    }

    private static void validateSections(
        List<RepositoryReviewContent.Section> sections, RuleEvidenceListView source, List<String> violations
    ) {
        if (sections == null || sections.size() != CATEGORIES.size()) {
            violations.add("INVALID_RESPONSE_SCHEMA");
            return;
        }
        Set<String> categories = new HashSet<>();
        for (var section : sections) {
            if (section == null || !CATEGORIES.contains(section.category()) || !categories.add(section.category())) {
                violations.add("INVALID_RESPONSE_SCHEMA");
                continue;
            }
            validateText(section.review(), violations);
            Set<UUID> allowedEvidence = source.evidence().stream()
                .filter(value -> section.category().equals(categoryForRule(value.ruleId())))
                .map(value -> value.evidenceId()).collect(java.util.stream.Collectors.toSet());
            if (section.evidenceIds() == null || section.evidenceIds().size() > 50
                || new HashSet<>(section.evidenceIds()).size() != section.evidenceIds().size()
                || !allowedEvidence.containsAll(section.evidenceIds())) {
                violations.add("UNSUPPORTED_EVIDENCE_REFERENCE");
            }
        }
        if (!categories.equals(CATEGORIES)) violations.add("INCOMPLETE_REPOSITORY_REVIEW");
    }

    private static String categoryForRule(String ruleId) {
        if (ruleId.startsWith("LANGUAGE_") || ruleId.startsWith("FRAMEWORK_")
            || "ARCHITECTURE_DOCUMENTATION".equals(ruleId)) return "ARCHITECTURE";
        if (ruleId.startsWith("TEST_")) return "TESTING";
        if (ruleId.startsWith("CI_")) return "DEVOPS";
        if (ruleId.startsWith("README_") || "README_PRESENT".equals(ruleId)
            || "API_DOCUMENTATION".equals(ruleId) || "LICENSE_PRESENT".equals(ruleId)) return "DOCUMENTATION";
        if (ruleId.startsWith("CONTRIBUT") || ruleId.startsWith("COMMIT_")
            || ruleId.startsWith("BRANCH_") || ruleId.startsWith("ACTIVITY_")) return "COLLABORATION";
        return "";
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
