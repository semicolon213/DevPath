package com.devpath.prompt.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devpath.rule.application.SkillAssessmentView;
import com.devpath.rule.application.SkillMatrixView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkillExplanationPromptBuilderTest {
    private final SkillExplanationPromptBuilder builder = new SkillExplanationPromptBuilder(new ObjectMapper());

    @Test
    void buildsAnImmutableBoundedContextWithoutCalculatingResults() {
        UUID matrixId = UUID.randomUUID();
        var matrix = matrix(matrixId);
        var template = new PromptTemplateVersion(UUID.randomUUID(), "SKILL_ANALYSIS_EXPLANATION", "v1",
            "Never calculate or alter any score.", "Return the required JSON object.");

        PromptPackage first = builder.build(matrix, template);
        PromptPackage second = builder.build(matrix, template);

        assertThat(first.contextHash()).isEqualTo(second.contextHash()).hasSize(64);
        assertThat(first.contextPayload()).contains(matrixId.toString(), "testing-discipline");
        assertThat(first.prompt()).contains("Never calculate", "<skill-matrix-data>");
        assertThat((first.prompt().length() + 3) / 4).isLessThanOrEqualTo(first.tokenBudget());
    }

    @Test
    void rejectsATemplateWithoutTheNoCalculationBoundary() {
        var template = new PromptTemplateVersion(UUID.randomUUID(), "SKILL_ANALYSIS_EXPLANATION", "v1",
            "Explain the data.", "Return JSON.");
        assertThatThrownBy(() -> builder.build(matrix(UUID.randomUUID()), template))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static SkillMatrixView matrix(UUID matrixId) {
        var skill = new SkillAssessmentView(UUID.randomUUID(), UUID.randomUUID(), "testing-discipline", "Testing",
            "TESTING", BigDecimal.valueOf(80), "STRONG", BigDecimal.valueOf(90), true, false, "STABLE", "rule-result", List.of(UUID.randomUUID()),
            List.of(UUID.randomUUID()), List.of(), "rule-v1");
        return new SkillMatrixView(matrixId, UUID.randomUUID(), "skill-v1", "rule-v1", "CURRENT", List.of(skill),
            List.of("testing-discipline"), List.of(), Instant.parse("2026-08-31T00:00:00Z"));
    }
}
