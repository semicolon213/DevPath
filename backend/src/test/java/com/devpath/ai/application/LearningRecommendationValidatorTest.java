package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.learning.application.LearningRoadmapView;
import com.devpath.learning.application.RoadmapStepView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LearningRecommendationValidatorTest {
    private final LearningRecommendationValidator validator = new LearningRecommendationValidator(new ObjectMapper());

    @Test
    void restoresAuthoritativeStepIdentityOrderAndCompletionCriteria() {
        var roadmap = roadmap();
        String raw = """
            {"recommendationSummary":"현재 로드맵은 기초를 적용한 뒤 결과 근거를 남기는 흐름입니다.",
             "steps":[{"position":0,"milestoneExplanation":"테스트 역량을 실제 변경에 연결하는 단계입니다.",
             "objective":"변경을 안전하게 검증하는 습관을 익힙니다.",
             "practiceTask":"작은 기능에 자동 검증을 추가하고 실패를 먼저 확인하세요."}]}
            """;

        LearningRecommendationValidation result = validator.validate(raw, roadmap);

        assertThat(result.violations()).isEmpty();
        assertThat(result.content().steps().getFirst()).satisfies(step -> {
            assertThat(step.roadmapStepId()).isEqualTo(roadmap.steps().getFirst().roadmapStepId());
            assertThat(step.completionCriteria()).isEqualTo("Automated tests pass");
            assertThat(step.expectedEvidence()).containsExactly("test files");
        });
    }

    @Test
    void rejectsMissingRoadmapSteps() {
        assertThat(validator.validate("{\"recommendationSummary\":\"요약\",\"steps\":[]}", roadmap()).violations())
            .contains("INCOMPLETE_LEARNING_PLAN");
    }

    private static LearningRoadmapView roadmap() {
        UUID milestone = UUID.randomUUID();
        return new LearningRoadmapView(UUID.randomUUID(), UUID.randomUUID(), "roadmap-v1", "CREATED",
            BigDecimal.ZERO, List.of(), List.of(new RoadmapStepView(UUID.randomUUID(), milestone, UUID.randomUUID(),
                0, "TESTING", "Add tests", "INTERMEDIATE", 8, List.of(), "Automated tests pass",
                List.of("test files"), "NOT_STARTED")), Instant.parse("2026-09-02T00:00:00Z"),
            Instant.parse("2026-09-02T00:00:00Z"));
    }
}
