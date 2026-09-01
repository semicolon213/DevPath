package com.devpath.prompt.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devpath.rule.application.SkillAssessmentView;
import com.devpath.rule.application.SkillMatrixView;
import com.devpath.rule.application.RuleCategoryScoreView;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvidenceSummaryView;
import com.devpath.rule.application.RuleEvidenceView;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.RuleResultView;
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

        PromptPackage first = builder.build(matrix, evaluation(matrix), evidence(matrix), template);
        PromptPackage second = builder.build(matrix, evaluation(matrix), evidence(matrix), template);

        assertThat(first.contextHash()).isEqualTo(second.contextHash()).hasSize(64);
        assertThat(first.contextPayload()).contains(matrixId.toString(), "testing-discipline", "strengths", "improvementAreas");
        assertThat(first.contextPayload()).contains("TEST_FILES", "confirmedRuleIds", "CI_WORKFLOW_METADATA", "missingRuleIds",
            "Repository path detected: backend/src/test/java/ExampleTest.java");
        assertThat(first.contextPayload()).doesNotContain(matrix.skills().getFirst().evidenceIds().getFirst().toString());
        assertThat(first.contextPayload()).doesNotContain("score", "level", "confidence");
        assertThat(first.prompt()).contains("Never calculate", "every supplied skill", "must be Korean",
            "concrete evidence facts", "confirmedRuleIds as present", "one short sentence", "<skill-matrix-data>");
        assertThat((first.prompt().length() + 3) / 4).isLessThanOrEqualTo(first.tokenBudget());
    }

    @Test
    void rejectsMissingEvidenceFacts() {
        var matrix = matrix(UUID.randomUUID());
        var template = new PromptTemplateVersion(UUID.randomUUID(), "SKILL_ANALYSIS_EXPLANATION", "v1",
            "Never calculate or alter any score.", "Return the required JSON object.");

        assertThatThrownBy(() -> builder.build(matrix, evaluation(matrix),
            new RuleEvidenceListView(matrix.evaluationId(), List.of()), template))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("Prompt context is missing evidence");
    }

    @Test
    void rejectsATemplateWithoutTheNoCalculationBoundary() {
        var template = new PromptTemplateVersion(UUID.randomUUID(), "SKILL_ANALYSIS_EXPLANATION", "v1",
            "Explain the data.", "Return JSON.");
        var matrix = matrix(UUID.randomUUID());
        assertThatThrownBy(() -> builder.build(matrix, evaluation(matrix), evidence(matrix), template))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static RuleEvaluationView evaluation(SkillMatrixView matrix) {
        var rule = new RuleResultView("TEST_FILES", "v1", "PASSED", BigDecimal.ONE, BigDecimal.ONE,
            BigDecimal.ONE, "PRESENCE", "trace", List.of());
        var missingRule = new RuleResultView("CI_WORKFLOW_METADATA", "v1", "FAILED", BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ONE, "PRESENCE", "trace", List.of());
        var category = new RuleCategoryScoreView("TESTING", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
            List.of(rule, missingRule), List.of());
        return new RuleEvaluationView(matrix.evaluationId(), UUID.randomUUID(), UUID.randomUUID(), "rule-v1",
            "formula-v1", "extractor-v1", BigDecimal.ONE, BigDecimal.ONE,
            new RuleEvidenceSummaryView(2, 1, 0), List.of(category), List.of(), Instant.parse("2026-08-31T00:00:00Z"));
    }

    private static RuleEvidenceListView evidence(SkillMatrixView matrix) {
        List<UUID> ids = matrix.skills().getFirst().evidenceIds();
        return new RuleEvidenceListView(matrix.evaluationId(), List.of(
            new RuleEvidenceView(ids.getFirst(), "TEST_FILES", "DIRECT", "REPOSITORY_PATH",
                "snapshot:value:path:backend/src/test/java/ExampleTest.java", "stored path", BigDecimal.ONE),
            new RuleEvidenceView(ids.getFirst(), "CI_WORKFLOW_METADATA", "SUPPORTING", "REPOSITORY_PATH",
                "snapshot:value:path:backend/src/test/java/ExampleTest.java", "stored path", BigDecimal.ONE),
            new RuleEvidenceView(ids.get(1), "TEST_FILES", "SUPPORTING", "SNAPSHOT_SIGNAL",
                "snapshot:value:signal:TEST_FRAMEWORKS", "stored signal", BigDecimal.ONE)
        ));
    }

    private static SkillMatrixView matrix(UUID matrixId) {
        var skill = new SkillAssessmentView(UUID.randomUUID(), UUID.randomUUID(), "testing-discipline", "Testing",
            "TESTING", BigDecimal.valueOf(80), "STRONG", BigDecimal.valueOf(90), true, false, "STABLE", "rule-result", List.of(UUID.randomUUID(), UUID.randomUUID()),
            List.of(UUID.randomUUID()), List.of(), "rule-v1");
        var weakSkill = new SkillAssessmentView(UUID.randomUUID(), UUID.randomUUID(), "delivery-operations", "Delivery",
            "TESTING", BigDecimal.ZERO, "FOUNDATIONAL", BigDecimal.ONE, false, true, "STABLE", "rule-result", List.of(),
            List.of(UUID.randomUUID()), List.of(), "rule-v1");
        return new SkillMatrixView(matrixId, UUID.randomUUID(), "skill-v1", "rule-v1", "CURRENT", List.of(skill, weakSkill),
            List.of("testing-discipline"), List.of("delivery-operations"), Instant.parse("2026-08-31T00:00:00Z"));
    }
}
