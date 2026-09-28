package com.devpath.ai.application;

import com.devpath.prompt.application.RepositoryReviewEvidenceSelection;
import com.devpath.rule.application.RuleEvidenceListView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RepositoryReviewValidator {
    public static final String VERSION = "repository-review-validator-v2";
    private static final Set<String> CATEGORIES = Set.copyOf(RepositoryReviewEvidenceSelection.CATEGORIES);
    private final ObjectMapper mapper;

    public RepositoryReviewValidator(ObjectMapper mapper) { this.mapper = mapper; }

    public RepositoryReviewValidation validate(String raw, RuleEvidenceListView source) {
        if (raw == null || raw.isBlank() || raw.length() > 30_000) {
            return new RepositoryReviewValidation(null, List.of("MALFORMED_OR_EMPTY_RESPONSE"));
        }
        try {
            JsonNode root = mapper.readTree(raw);
            if (!root.isObject() || !GeneratedOutputPolicy.fields(root).equals(Set.of("summary", "sections"))) {
                return new RepositoryReviewValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            }
            if (!root.path("sections").isArray()) {
                return new RepositoryReviewValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            }
            for (JsonNode section : root.path("sections")) {
                if (!section.isObject() || !GeneratedOutputPolicy.fields(section).equals(Set.of("category", "review", "evidenceIds"))) {
                    return new RepositoryReviewValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
                }
            }
            resolveEvidenceKeys(root, source);
            RepositoryReviewContent content = mapper.treeToValue(root, RepositoryReviewContent.class);
            List<String> violations = new ArrayList<>();
            GeneratedOutputPolicy.validateText(content.summary(), 2000, violations);
            validateSections(content.sections(), source, violations);
            return new RepositoryReviewValidation(content, violations.stream().distinct().toList());
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            return new RepositoryReviewValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
        }
    }

    private static void resolveEvidenceKeys(JsonNode root, RuleEvidenceListView source) {
        Map<String, Map<String, UUID>> evidenceByCategory = new HashMap<>();
        RepositoryReviewEvidenceSelection.select(source.evidence()).forEach((category, values) -> {
            Map<String, UUID> byKey = new HashMap<>();
            values.forEach(value -> byKey.put(value.key(), value.evidence().evidenceId()));
            evidenceByCategory.put(category, byKey);
        });
        for (JsonNode section : root.path("sections")) {
            Map<String, UUID> byKey = evidenceByCategory.getOrDefault(section.path("category").asText(), Map.of());
            JsonNode evidenceIds = section.path("evidenceIds");
            if (!evidenceIds.isArray()) continue;
            for (int index = 0; index < evidenceIds.size(); index++) {
                UUID evidenceId = byKey.get(evidenceIds.get(index).asText());
                if (evidenceId != null) ((ArrayNode) evidenceIds).set(index, TextNode.valueOf(evidenceId.toString()));
            }
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
            GeneratedOutputPolicy.validateText(section.review(), 2000, violations);
            Set<UUID> allowedEvidence = source.evidence().stream()
                .filter(value -> section.category().equals(RepositoryReviewEvidenceSelection.categoryForRule(value.ruleId())))
                .map(value -> value.evidenceId()).collect(java.util.stream.Collectors.toSet());
            if (section.evidenceIds() == null || section.evidenceIds().size() > 50
                || new HashSet<>(section.evidenceIds()).size() != section.evidenceIds().size()
                || !allowedEvidence.containsAll(section.evidenceIds())) {
                violations.add("UNSUPPORTED_EVIDENCE_REFERENCE");
            }
        }
        if (!categories.equals(CATEGORIES)) violations.add("INCOMPLETE_REPOSITORY_REVIEW");
    }

}
