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
public class CareerCoachingPromptBuilder {
    private static final int TOKEN_BUDGET = 8192;
    private final ObjectMapper mapper;

    public CareerCoachingPromptBuilder(ObjectMapper mapper) { this.mapper = mapper; }

    public PromptPackage build(
        CareerReadinessView readiness, RecommendationSetView recommendations, LearningRoadmapView roadmap,
        PromptTemplateVersion template
    ) {
        if (!"CAREER_COACHING".equals(template.taskType()) || template.systemPrompt() == null
            || !template.systemPrompt().toLowerCase(java.util.Locale.ROOT).contains("never calculate")
            || template.outputFormatPrompt() == null || template.outputFormatPrompt().isBlank()
            || !recommendations.careerReadinessId().equals(readiness.careerReadinessId())
            || !roadmap.recommendationSetId().equals(recommendations.recommendationSetId())) {
            throw new IllegalArgumentException("Prompt template is invalid");
        }
        try {
            var nextRecommendations = recommendations.recommendations().stream().limit(3).toList();
            var recommendationIds = nextRecommendations.stream().map(value -> value.recommendationId()).toList();
            String context = mapper.writeValueAsString(Map.of(
                "readiness", Map.of(
                    "careerReadinessId", readiness.careerReadinessId(), "careerId", readiness.careerId(),
                    "readinessLevel", readiness.readinessLevel(), "status", readiness.status(),
                    "unavailableCategories", readiness.unavailableCategories()),
                "strengths", readiness.skillGaps().stream()
                    .filter(value -> "SUFFICIENT".equals(value.gapState()) || "STRONG".equals(value.gapState())).toList(),
                "gaps", readiness.skillGaps().stream()
                    .filter(value -> !"SUFFICIENT".equals(value.gapState()) && !"STRONG".equals(value.gapState())).toList(),
                "recommendationSetId", recommendations.recommendationSetId(),
                "recommendations", nextRecommendations,
                "roadmapId", roadmap.roadmapId(),
                "roadmapSteps", roadmap.steps().stream()
                    .filter(value -> recommendationIds.contains(value.recommendationId())).toList()
            ));
            PromptSecretDetector.reject(context);
            String prompt = template.systemPrompt()
                + "\n\nTask: Coach the user in Korean using only the supplied career data. Explain what the verified strengths enable,"
                + " why each gap matters for the selected career, and how to execute the supplied next steps."
                + " Keep nextSteps in the exact supplied recommendation order and include every supplied recommendationId once."
                + " Do not merely repeat labels, scores, titles, priorities, or completion criteria."
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
