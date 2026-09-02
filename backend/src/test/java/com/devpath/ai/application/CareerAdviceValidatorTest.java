package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.recommendation.application.RecommendationSetView;
import com.devpath.recommendation.application.RecommendationView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareerAdviceValidatorTest {
    private final CareerAdviceValidator validator = new CareerAdviceValidator(new ObjectMapper());

    @Test
    void acceptsAdviceThatPreservesDeterministicRecommendationOrder() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        String raw = """
            {"readinessSummary":"현재 근거를 바탕으로 실무 역량을 확장할 단계입니다.",
             "strengthExplanation":"확인된 강점을 구현 품질을 높이는 데 활용할 수 있습니다.",
             "gapExplanation":"부족한 영역은 프로젝트 결과물로 보완하는 것이 좋습니다.",
             "nextSteps":[{"recommendationId":"%s","advice":"먼저 작은 결과물을 완성하고 근거를 남기세요."},
                          {"recommendationId":"%s","advice":"이후 다음 과제를 연결해 반복 가능한 경험을 만드세요."}],
             "confidenceNotice":"확인된 저장소 근거 범위 안에서 제공한 조언입니다."}
            """.formatted(first, second);

        CareerAdviceValidation result = validator.validate(raw, recommendations(first, second));

        assertThat(result.violations()).isEmpty();
        assertThat(result.content().nextSteps()).extracting(CareerAdviceContent.NextStep::recommendationId)
            .containsExactly(first, second);
    }

    @Test
    void rejectsChangedRecommendationOrder() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        String raw = """
            {"readinessSummary":"요약","strengthExplanation":"강점","gapExplanation":"격차",
             "nextSteps":[{"recommendationId":"%s","advice":"조언"},{"recommendationId":"%s","advice":"조언"}],
             "confidenceNotice":"확인 범위"}
            """.formatted(second, first);

        assertThat(validator.validate(raw, recommendations(first, second)).violations())
            .contains("RECOMMENDATION_ORDER_CHANGED");
    }

    private static RecommendationSetView recommendations(UUID... ids) {
        return new RecommendationSetView(UUID.randomUUID(), UUID.randomUUID(), "recommendation-v1", "PUBLISHED",
            java.util.Arrays.stream(ids).map(id -> new RecommendationView(id, UUID.randomUUID(), "TESTING", "STUDY",
                "HIGH", "GAP", "title", "criteria", List.of(), List.of(), 1, 1, "PROPOSED")).toList(),
            Instant.parse("2026-09-02T00:00:00Z"));
    }
}
