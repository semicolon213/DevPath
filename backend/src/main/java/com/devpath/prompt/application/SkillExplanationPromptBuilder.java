package com.devpath.prompt.application;

import com.devpath.rule.application.SkillMatrixView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SkillExplanationPromptBuilder {
    private static final int TOKEN_BUDGET = 4096;
    private static final Pattern SECRET = Pattern.compile(
        "(?i)(github_pat_|ghp_|sk-[a-z0-9]|-----BEGIN [A-Z ]+PRIVATE KEY-----|password\\s*[:=])"
    );
    private final ObjectMapper mapper;

    public SkillExplanationPromptBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public PromptPackage build(SkillMatrixView matrix, PromptTemplateVersion template) {
        if (!"SKILL_ANALYSIS_EXPLANATION".equals(template.taskType())
            || template.systemPrompt() == null || !template.systemPrompt().toLowerCase(java.util.Locale.ROOT)
                .contains("never calculate")
            || template.outputFormatPrompt() == null || template.outputFormatPrompt().isBlank()) {
            throw new IllegalArgumentException("Prompt template is invalid");
        }
        try {
            String context = mapper.writeValueAsString(Map.of(
                "skillMatrixId", matrix.skillMatrixId(),
                "policyVersion", matrix.policyVersion(),
                "ruleSetVersion", matrix.ruleSetVersion(),
                "strengths", matrix.strengths(),
                "weaknesses", matrix.weaknesses(),
                "skills", matrix.skills().stream().map(skill -> Map.of(
                    "skillKey", skill.skillKey(),
                    "skillName", skill.skillName(),
                    "score", skill.score(),
                    "level", skill.level(),
                    "confidence", skill.confidence(),
                    "strength", skill.strength(),
                    "weakness", skill.weakness(),
                    "evidenceIds", skill.evidenceIds()
                )).toList()
            ));
            if (SECRET.matcher(context).find()) throw new IllegalArgumentException("Prompt context contains sensitive data");
            String prompt = template.systemPrompt() + "\n\nTask: Explain the supplied Skill Matrix in Korean."
                + "\n\nOutput contract: " + template.outputFormatPrompt()
                + "\n\n<skill-matrix-data>\n" + context + "\n</skill-matrix-data>";
            if ((prompt.length() + 3) / 4 > TOKEN_BUDGET) {
                throw new IllegalArgumentException("Prompt context exceeds the token budget");
            }
            return new PromptPackage(context, prompt, sha256(context), TOKEN_BUDGET);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Prompt context could not be serialized", exception);
        }
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
