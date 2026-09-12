package com.devpath.prompt.application;

import com.devpath.career.application.CareerReadinessView;
import com.devpath.learning.application.LearningRoadmapView;
import com.devpath.recommendation.application.RecommendationSetView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LearningRecommendationPromptBuilder {
    private static final int TOKEN_BUDGET = 8192;
    private final ObjectMapper mapper;

    public LearningRecommendationPromptBuilder(ObjectMapper mapper) { this.mapper = mapper; }

    public PromptPackage build(
        CareerReadinessView readiness, RecommendationSetView recommendations, LearningRoadmapView roadmap,
        PromptTemplateVersion template
    ) {
        if (!"LEARNING_RECOMMENDATION".equals(template.taskType()) || template.systemPrompt() == null
            || !template.systemPrompt().toLowerCase(java.util.Locale.ROOT).contains("never calculate")
            || template.outputFormatPrompt() == null || template.outputFormatPrompt().isBlank()
            || !recommendations.careerReadinessId().equals(readiness.careerReadinessId())
            || !roadmap.recommendationSetId().equals(recommendations.recommendationSetId())
            || roadmap.steps().isEmpty()) {
            throw new IllegalArgumentException("Prompt template is invalid");
        }
        try {
            String context = mapper.writeValueAsString(Map.of(
                "roadmapId", roadmap.roadmapId(),
                "careerReadinessId", readiness.careerReadinessId(),
                "careerId", readiness.careerId(),
                "skillGaps", readiness.skillGaps(),
                "recommendationSetId", recommendations.recommendationSetId(),
                "recommendations", recommendations.recommendations(),
                "milestones", roadmap.milestones(),
                "steps", roadmap.steps()
            ));
            PromptSecretDetector.reject(context);
            String prompt = template.systemPrompt()
                + "\n\nTask: Explain the supplied learning roadmap in actionable Korean."
                + " For each supplied step, explain its milestone, learning objective, and a concrete practice task"
                + " that produces only the supplied expected evidence."
                + " Return every supplied position exactly once and in the supplied order."
                + " Do not repeat titles, priorities, hours, prerequisites, or completion criteria as the explanation."
                + " All human-readable text must be Korean and concise."
                + "\n\nOutput contract: " + template.outputFormatPrompt();
            if (PromptPackage.estimatedTokenCount(prompt, context) > TOKEN_BUDGET) {
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
