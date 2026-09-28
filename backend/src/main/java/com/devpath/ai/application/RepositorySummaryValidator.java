package com.devpath.ai.application;

import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.prompt.application.RepositorySummaryRuleOutputSelection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RepositorySummaryValidator {
    public static final String VERSION = "repository-summary-validator-v2";
    private final ObjectMapper mapper;

    public RepositorySummaryValidator(ObjectMapper mapper) { this.mapper = mapper; }

    public RepositorySummaryValidation validate(
        String raw, List<String> technologies, RuleEvaluationView evaluation, RuleEvidenceListView source
    ) {
        if (raw == null || raw.isBlank() || raw.length() > 30_000) {
            return new RepositorySummaryValidation(null, List.of("MALFORMED_OR_EMPTY_RESPONSE"));
        }
        try {
            JsonNode root = mapper.readTree(raw);
            if (!root.isObject() || !GeneratedOutputPolicy.fields(root).equals(Set.of(
                "summary", "strengths", "improvementAreas", "technologies"
            )) || !validClaimNodes(root)) return new RepositorySummaryValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            resolveEvidenceKeys(root, source);
            RepositorySummaryContent content = mapper.treeToValue(root, RepositorySummaryContent.class);
            List<String> violations = new ArrayList<>();
            Set<UUID> supported = source.evidence().stream().limit(20).map(value -> value.evidenceId()).collect(java.util.stream.Collectors.toSet());
            Set<String> outputs = RepositorySummaryRuleOutputSelection.select(evaluation).stream()
                .map(RepositorySummaryRuleOutputSelection.RuleOutput::reference).collect(java.util.stream.Collectors.toSet());
            validateClaim(content.summary(), supported, outputs, violations);
            validateClaimList(content.strengths(), supported, outputs, violations);
            validateClaimList(content.improvementAreas(), supported, outputs, violations);
            if (content.technologies() == null || !List.copyOf(technologies).equals(content.technologies())
                || content.technologies().size() > 20 || new HashSet<>(content.technologies()).size() != content.technologies().size())
                violations.add("UNSUPPORTED_TECHNOLOGIES");
            return new RepositorySummaryValidation(content, violations.stream().distinct().toList());
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            return new RepositorySummaryValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
        }
    }

    private static void resolveEvidenceKeys(JsonNode root, RuleEvidenceListView source) {
        resolveClaimEvidenceKeys(root.path("summary"), source);
        for (String field : List.of("strengths", "improvementAreas")) {
            JsonNode claims = root.path(field);
            if (claims.isArray()) claims.forEach(claim -> resolveClaimEvidenceKeys(claim, source));
        }
    }

    private static void resolveClaimEvidenceKeys(JsonNode claim, RuleEvidenceListView source) {
        JsonNode values = claim.path("evidenceReferences");
        if (!values.isArray()) return;
        for (int index = 0; index < values.size(); index++) {
            String key = values.get(index).asText();
            if (key.matches("E[1-9][0-9]*")) {
                int evidenceIndex = Integer.parseInt(key.substring(1)) - 1;
                if (evidenceIndex < source.evidence().size()) ((ArrayNode) values).set(index,
                    TextNode.valueOf(source.evidence().get(evidenceIndex).evidenceId().toString()));
            }
        }
    }

    private static boolean validClaimNodes(JsonNode root) {
        if (!validClaimNode(root.path("summary"))) return false;
        for (String field : List.of("strengths", "improvementAreas")) {
            JsonNode claims = root.path(field);
            if (!claims.isArray()) return false;
            for (JsonNode claim : claims) if (!validClaimNode(claim)) return false;
        }
        return true;
    }

    private static boolean validClaimNode(JsonNode claim) {
        return claim.isObject() && GeneratedOutputPolicy.fields(claim).equals(Set.of("text", "evidenceReferences", "ruleOutputReferences"));
    }

    private static void validateClaimList(
        List<RepositorySummaryContent.Claim> values, Set<UUID> evidence, Set<String> outputs, List<String> violations
    ) {
        if (values == null || values.size() > 5 || new HashSet<>(values).size() != values.size()) { violations.add("INVALID_RESPONSE_SCHEMA"); return; }
        values.forEach(value -> validateClaim(value, evidence, outputs, violations));
    }

    private static void validateClaim(
        RepositorySummaryContent.Claim claim, Set<UUID> evidence, Set<String> outputs, List<String> violations
    ) {
        if (claim == null) { violations.add("INVALID_RESPONSE_SCHEMA"); return; }
        GeneratedOutputPolicy.validateText(claim.text(), 2000, violations);
        if (claim.evidenceReferences() == null || claim.evidenceReferences().size() > 20
            || new HashSet<>(claim.evidenceReferences()).size() != claim.evidenceReferences().size()
            || !evidence.containsAll(claim.evidenceReferences())) violations.add("UNSUPPORTED_EVIDENCE_REFERENCE");
        if (claim.ruleOutputReferences() == null || claim.ruleOutputReferences().size() > 20
            || new HashSet<>(claim.ruleOutputReferences()).size() != claim.ruleOutputReferences().size()
            || !outputs.containsAll(claim.ruleOutputReferences())) violations.add("UNSUPPORTED_RULE_OUTPUT_REFERENCE");
        if (claim.evidenceReferences() != null && claim.ruleOutputReferences() != null
            && claim.evidenceReferences().isEmpty() && claim.ruleOutputReferences().isEmpty())
            violations.add("MISSING_GROUNDING_REFERENCE");
    }

}
