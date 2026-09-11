package com.devpath.prompt.application;

import com.devpath.analysis.application.AnalysisResultView;
import com.devpath.repository.application.RepositoryView;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvaluationView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RepositorySummaryPromptBuilder {
    private static final int TOKEN_BUDGET = 8192;
    private static final Pattern SECRET = Pattern.compile("(?i)(github_pat_|ghp_|sk-[a-z0-9]|-----BEGIN [A-Z ]+PRIVATE KEY-----|password\\s*[:=])");
    private final ObjectMapper mapper;

    public RepositorySummaryPromptBuilder(ObjectMapper mapper) { this.mapper = mapper; }

    public PromptPackage build(
        RepositoryView repository, AnalysisResultView analysis, RuleEvaluationView evaluation,
        List<String> technologies, RuleEvidenceListView evidence, PromptTemplateVersion template
    ) {
        if (!"REPOSITORY_SUMMARY".equals(template.taskType()) || template.systemPrompt() == null
            || !template.systemPrompt().toLowerCase(java.util.Locale.ROOT).contains("never calculate")
            || template.outputFormatPrompt() == null || template.outputFormatPrompt().isBlank()) {
            throw new IllegalArgumentException("Prompt template is invalid");
        }
        try {
            var selectedEvidence = java.util.stream.IntStream.range(0, Math.min(20, evidence.evidence().size()))
                .mapToObj(index -> {
                    var value = evidence.evidence().get(index);
                    return Map.of("evidenceKey", "E" + (index + 1), "ruleId", value.ruleId(),
                        "fact", value.observedFactSummary());
                }).toList();
            var outputs = RepositorySummaryRuleOutputSelection.select(evaluation);
            var categories = evaluation.categoryScores().stream().limit(5).map(category -> Map.of(
                "category", category.category(), "score", category.score(), "confidence", category.confidence()
            )).toList();
            String context = mapper.writeValueAsString(Map.of(
                "repository", Map.of("repositoryId", repository.repositoryId(), "fullName", repository.fullName()),
                "analysis", Map.of("analysisId", analysis.analysisId(), "snapshotId", analysis.snapshotId(), "evaluationId", analysis.evaluationId()),
                "categoryScores", categories, "ruleOutputs", outputs, "technologies", technologies, "evidence", selectedEvidence
            ));
            if (SECRET.matcher(context).find()) throw new IllegalArgumentException("Prompt context contains sensitive data");
            String prompt = template.systemPrompt() + "\n\nTask: Summarize only supplied repository facts in Korean. Represent summary and every strengths/improvementAreas item as a claim object. Each claim must cite at least one supplied evidenceKey in evidenceReferences or supplied rule-output reference in ruleOutputReferences; do not write UUIDs. Keep each claim text to one short sentence. Copy technologies exactly as supplied.\n\nOutput contract: " + template.outputFormatPrompt() + "\n\n<repository-summary-data>\n" + context + "\n</repository-summary-data>";
            if ((prompt.length() + 3) / 4 > TOKEN_BUDGET) throw new IllegalArgumentException("Prompt context exceeds the token budget");
            return new PromptPackage(context, prompt, sha256(context), TOKEN_BUDGET);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Prompt context could not be serialized", exception);
        }
    }

    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }
}
