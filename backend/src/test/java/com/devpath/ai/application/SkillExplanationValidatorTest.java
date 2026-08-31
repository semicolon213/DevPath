package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.rule.application.SkillAssessmentView;
import com.devpath.rule.application.SkillMatrixView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkillExplanationValidatorTest {
    private final SkillExplanationValidator validator = new SkillExplanationValidator(new ObjectMapper());
    private final UUID evidenceId = UUID.randomUUID();
    private final SkillMatrixView matrix = matrix(evidenceId);

    @Test
    void acceptsOnlyGroundedNonNumericPlainText() {
        String response = """
            {"summary":"테스트 근거가 강점으로 확인됩니다.","strengths":[{"skillKey":"testing-discipline",
            "explanation":"자동화된 테스트 근거가 일관된 검증 습관을 보여 줍니다.","evidenceIds":["%s"]}],
            "improvementAreas":[]}
            """.formatted(evidenceId);

        SkillExplanationValidation result = validator.validate(response, matrix);

        assertThat(result.passed()).isTrue();
        assertThat(result.content().strengths()).singleElement()
            .satisfies(item -> assertThat(item.evidenceIds()).containsExactly(evidenceId));
    }

    @Test
    void rejectsUntraceableNumbersUnsupportedReferencesAndUnsafeMarkup() {
        String response = """
            {"summary":"점수 99 <script>alert</script>","strengths":[{"skillKey":"invented",
            "explanation":"근거 없는 설명","evidenceIds":["%s"]}],"improvementAreas":[]}
            """.formatted(UUID.randomUUID());

        SkillExplanationValidation result = validator.validate(response, matrix);

        assertThat(result.violations()).contains("UNTRACEABLE_NUMERIC_CLAIM", "UNSAFE_RENDERED_CONTENT",
            "UNSUPPORTED_SKILL_CLAIM", "UNSUPPORTED_EVIDENCE_REFERENCE");
    }

    private static SkillMatrixView matrix(UUID evidenceId) {
        var skill = new SkillAssessmentView(UUID.randomUUID(), UUID.randomUUID(), "testing-discipline", "Testing",
            "TESTING", BigDecimal.valueOf(80), "STRONG", BigDecimal.valueOf(90), true, false, "STABLE", "rule-result", List.of(evidenceId),
            List.of(UUID.randomUUID()), List.of(), "rule-v1");
        return new SkillMatrixView(UUID.randomUUID(), UUID.randomUUID(), "skill-v1", "rule-v1", "CURRENT",
            List.of(skill), List.of("testing-discipline"), List.of(), Instant.parse("2026-08-31T00:00:00Z"));
    }
}
