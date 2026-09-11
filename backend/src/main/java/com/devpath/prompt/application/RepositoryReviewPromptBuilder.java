package com.devpath.prompt.application;

import com.devpath.analysis.application.AnalysisResultView;
import com.devpath.repository.application.RepositoryView;
import com.devpath.rule.application.RuleEvidenceView;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.RuleEvidenceListView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RepositoryReviewPromptBuilder {
    private static final int TOKEN_BUDGET = 8192;
    private static final Pattern SECRET = Pattern.compile(
        "(?i)(github_pat_|ghp_|sk-[a-z0-9]|-----BEGIN [A-Z ]+PRIVATE KEY-----|password\\s*[:=])"
    );
    private final ObjectMapper mapper;

    public RepositoryReviewPromptBuilder(ObjectMapper mapper) { this.mapper = mapper; }

    public PromptPackage build(
        RepositoryView repository, AnalysisResultView analysis, RuleEvaluationView evaluation,
        RuleEvidenceListView evidence, PromptTemplateVersion template
    ) {
        if (!"REPOSITORY_REVIEW".equals(template.taskType()) || template.systemPrompt() == null
            || !template.systemPrompt().toLowerCase(java.util.Locale.ROOT).contains("never calculate")
            || template.outputFormatPrompt() == null || template.outputFormatPrompt().isBlank()) {
            throw new IllegalArgumentException("Prompt template is invalid");
        }
        try {
            String context = mapper.writeValueAsString(Map.of(
                "repository", Map.of("repositoryId", repository.repositoryId(), "fullName", repository.fullName(),
                    "defaultBranch", repository.defaultBranch(), "visibility", repository.visibility()),
                "analysis", Map.of("analysisId", analysis.analysisId(), "snapshotId", analysis.snapshotId(),
                    "evaluationId", analysis.evaluationId()),
                "ruleFindings", selectFindings(evaluation),
                "evidence", selectEvidence(evidence.evidence())
            ));
            if (SECRET.matcher(context).find()) throw new IllegalArgumentException("Prompt context contains sensitive data");
            String prompt = template.systemPrompt() + "\n\nTask: Review the supplied repository findings in Korean. "
                + "Map language, framework, and architecture-documentation findings to ARCHITECTURE; test findings to "
                + "TESTING; CI findings to DEVOPS; README, API-documentation, and license findings to DOCUMENTATION; "
                + "and contribution or activity findings to COLLABORATION."
                + " Keep the summary and each section review to one short Korean sentence. "
                + "The sections field must be a JSON array, not an object keyed by category. "
                + "Put only supplied evidenceKey values in evidenceIds for the matching section; do not write UUIDs."
                + "\n\nOutput contract: " + template.outputFormatPrompt();
            if (PromptPackage.estimatedTokenCount(prompt, context) > TOKEN_BUDGET) {
                throw new IllegalArgumentException("Prompt context exceeds the token budget");
            }
            return new PromptPackage(context, prompt, sha256(context), TOKEN_BUDGET);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Prompt context could not be serialized", exception);
        }
    }

    private static Map<String, List<Map<String, Object>>> selectEvidence(List<RuleEvidenceView> evidence) {
        Map<String, List<Map<String, Object>>> selected = groupedValues();
        RepositoryReviewEvidenceSelection.select(evidence).forEach((category, values) -> values.forEach(value ->
            selected.get(category).add(Map.of("evidenceKey", value.key(), "ruleId", value.evidence().ruleId(),
                "fact", value.evidence().observedFactSummary()))));
        return selected;
    }

    private static Map<String, List<Map<String, Object>>> selectFindings(RuleEvaluationView evaluation) {
        Map<String, List<Map<String, Object>>> findings = groupedValues();
        evaluation.categoryScores().stream().flatMap(category -> category.ruleResults().stream()).forEach(rule -> {
            String category = RepositoryReviewEvidenceSelection.categoryForRule(rule.ruleId());
            if (!category.isEmpty()) findings.get(category).add(Map.of("ruleId", rule.ruleId(), "status", rule.status()));
        });
        return findings;
    }

    private static Map<String, List<Map<String, Object>>> groupedValues() {
        Map<String, List<Map<String, Object>>> values = new LinkedHashMap<>();
        RepositoryReviewEvidenceSelection.CATEGORIES.forEach(category -> values.put(category, new ArrayList<>()));
        return values;
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
