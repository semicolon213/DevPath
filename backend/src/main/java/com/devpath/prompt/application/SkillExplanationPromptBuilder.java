package com.devpath.prompt.application;

import com.devpath.rule.application.RuleCategoryScoreView;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvidenceView;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.SkillAssessmentView;
import com.devpath.rule.application.SkillMatrixView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SkillExplanationPromptBuilder {
    private static final int TOKEN_BUDGET = 4096;
    private static final int EVIDENCE_LIMIT = 2;
    private final ObjectMapper mapper;

    public SkillExplanationPromptBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public PromptPackage build(
        SkillMatrixView matrix, RuleEvaluationView evaluation, RuleEvidenceListView evidence,
        PromptTemplateVersion template
    ) {
        if (!"SKILL_ANALYSIS_EXPLANATION".equals(template.taskType())
            || template.systemPrompt() == null || !template.systemPrompt().toLowerCase(java.util.Locale.ROOT)
                .contains("never calculate")
            || template.outputFormatPrompt() == null || template.outputFormatPrompt().isBlank()
            || !matrix.evaluationId().equals(evaluation.evaluationId())
            || !matrix.evaluationId().equals(evidence.evaluationId())) {
            throw new IllegalArgumentException("Prompt template is invalid");
        }
        try {
            Map<String, RuleCategoryScoreView> categories = evaluation.categoryScores().stream()
                .collect(Collectors.toMap(RuleCategoryScoreView::category, value -> value));
            Map<UUID, List<RuleEvidenceView>> evidenceById = evidence.evidence().stream()
                .collect(Collectors.groupingBy(RuleEvidenceView::evidenceId));
            String context = mapper.writeValueAsString(Map.of(
                "skillMatrixId", matrix.skillMatrixId(),
                "policyVersion", matrix.policyVersion(),
                "ruleSetVersion", matrix.ruleSetVersion(),
                "strengths", matrix.skills().stream().filter(SkillAssessmentView::strength)
                    .map(skill -> skillContext(skill, categories, evidenceById)).toList(),
                "improvementAreas", matrix.skills().stream().filter(SkillAssessmentView::weakness)
                    .map(skill -> skillContext(skill, categories, evidenceById)).toList()
            ));
            PromptSecretDetector.reject(context);
            String prompt = template.systemPrompt() + "\n\nTask: Explain the supplied Skill Matrix in Korean."
                + " Include every supplied skill exactly once in its classified list."
                + " Explain why each classification applies using its rule outcomes and concrete evidence facts."
                + " Treat confirmedRuleIds as present, partialRuleIds as incomplete, and missingRuleIds as absent."
                + " Each explanation must name at least one supplied rule and, when evidence exists, one concrete fact."
                + " Do not use generic statements that merely repeat the classification."
                + " Return an empty evidenceIds list for every item; the server attaches verified references."
                + " All human-readable text must be Korean. Keep the summary and each explanation to one short sentence."
                + "\n\nOutput contract: " + template.outputFormatPrompt();
            if (PromptPackage.estimatedTokenCount(prompt, context) > TOKEN_BUDGET) {
                throw new IllegalArgumentException("Prompt context exceeds the token budget");
            }
            return new PromptPackage(context, prompt, sha256(context), TOKEN_BUDGET);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Prompt context could not be serialized", exception);
        }
    }

    private static Map<String, Object> skillContext(
        SkillAssessmentView skill, Map<String, RuleCategoryScoreView> categories,
        Map<UUID, List<RuleEvidenceView>> evidenceById
    ) {
        RuleCategoryScoreView category = categories.get(skill.category());
        if (category == null) throw new IllegalArgumentException("Prompt context is missing rule outcomes");
        Map<String, String> statuses = category.ruleResults().stream()
            .collect(Collectors.toMap(rule -> rule.ruleId(), rule -> rule.status()));
        Set<String> relevantStatuses = skill.strength() ? Set.of("PASSED") : Set.of("PARTIAL", "FAILED");
        List<Map<String, Object>> facts = skill.evidenceIds().stream().map(id -> {
            List<RuleEvidenceView> values = evidenceById.get(id);
            if (values == null) throw new IllegalArgumentException("Prompt context is missing evidence");
            return values;
        }).flatMap(List::stream).filter(value -> statuses.containsKey(value.ruleId())
            && relevantStatuses.contains(statuses.get(value.ruleId())))
            .map(value -> Map.<String, Object>of(
                "evidenceType", value.evidenceType(), "fact", fact(value))).distinct().limit(EVIDENCE_LIMIT).toList();
        return Map.of(
            "skillKey", skill.skillKey(),
            "skillName", skill.skillName(),
            "confirmedRuleIds", skill.strength() ? ruleIds(category, "PASSED") : List.of(),
            "partialRuleIds", skill.weakness() ? ruleIds(category, "PARTIAL") : List.of(),
            "missingRuleIds", skill.weakness() ? ruleIds(category, "FAILED") : List.of(),
            "evidence", facts
        );
    }

    private static List<String> ruleIds(RuleCategoryScoreView category, String status) {
        return category.ruleResults().stream().filter(rule -> status.equals(rule.status()))
            .map(rule -> rule.ruleId()).toList();
    }

    private static String fact(RuleEvidenceView evidence) {
        return switch (evidence.evidenceType()) {
            case "REPOSITORY_PATH" -> "Repository path detected: " + suffix(evidence.sourceReference(), ":path:");
            case "LANGUAGE_STATISTIC" -> "Repository language detected: " + suffix(evidence.sourceReference(), ":language:");
            case "SNAPSHOT_SIGNAL" -> "Repository signal measured: " + suffix(evidence.sourceReference(), ":signal:");
            default -> evidence.observedFactSummary();
        };
    }

    private static String suffix(String reference, String marker) {
        int start = reference.indexOf(marker);
        if (start < 0 || start + marker.length() == reference.length()) {
            throw new IllegalArgumentException("Prompt evidence reference is invalid");
        }
        return reference.substring(start + marker.length());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
