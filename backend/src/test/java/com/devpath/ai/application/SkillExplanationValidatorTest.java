package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.rule.application.RuleCategoryScoreView;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvidenceSummaryView;
import com.devpath.rule.application.RuleEvidenceView;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.RuleResultView;
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
            "explanation":"자동화된 테스트 근거가 일관된 검증 습관을 보여 줍니다.","evidenceIds":[]}],
            "improvementAreas":[]}
            """;

        SkillExplanationValidation result = validator.validate(response, matrix, evaluation(), evidence());

        assertThat(result.passed()).isTrue();
        assertThat(result.content().strengths()).singleElement()
            .satisfies(item -> assertThat(item.evidenceIds()).containsExactly(evidenceId));
    }

    @Test
    void replacesUntrustedEvidenceAndRejectsUnsafeClaims() {
        String response = """
            {"summary":"점수 99 <script>alert</script>","strengths":[{"skillKey":"invented",
            "explanation":"근거 없는 설명","evidenceIds":["%s"]}],"improvementAreas":[]}
            """.formatted(UUID.randomUUID());

        SkillExplanationValidation result = validator.validate(response, matrix, evaluation(), evidence());

        assertThat(result.violations()).contains("UNTRACEABLE_NUMERIC_CLAIM", "UNSAFE_RENDERED_CONTENT",
            "UNSUPPORTED_SKILL_CLAIM").doesNotContain("UNSUPPORTED_EVIDENCE_REFERENCE");
    }

    @Test
    void appendsConcreteGroundedAdviceToImprovementAreas() {
        UUID commitEvidence = UUID.randomUUID(), evaluationId = UUID.randomUUID();
        var activity = new SkillAssessmentView(UUID.randomUUID(), UUID.randomUUID(), "development-activity", "Activity",
            "ACTIVITY", BigDecimal.ONE, "FOUNDATIONAL", BigDecimal.ONE, null, false, true, "STABLE", "activity-result",
            List.of(commitEvidence), List.of(), List.of(), "rule-v1");
        var delivery = new SkillAssessmentView(UUID.randomUUID(), UUID.randomUUID(), "delivery-operations", "Delivery",
            "DEVOPS", BigDecimal.ZERO, "FOUNDATIONAL", BigDecimal.ONE, null, false, true, "STABLE", "delivery-result",
            List.of(), List.of(), List.of(), "rule-v1");
        var weakMatrix = new SkillMatrixView(UUID.randomUUID(), evaluationId, "skill-v1", "rule-v1", "CURRENT",
            List.of(activity, delivery), List.of(), List.of("development-activity", "delivery-operations"),
            Instant.parse("2026-08-31T00:00:00Z"));
        var commitRule = new RuleResultView("COMMIT_COUNT", "v1", "PARTIAL", BigDecimal.ONE, BigDecimal.ONE,
            BigDecimal.ONE, "COUNT_CAP", "trace", List.of());
        var workflowRule = new RuleResultView("CI_WORKFLOWS", "v1", "FAILED", BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ONE, "PRESENCE", "trace", List.of());
        var evaluation = new RuleEvaluationView(evaluationId, UUID.randomUUID(), UUID.randomUUID(), "rule-v1",
            "formula-v1", "extractor-v1", BigDecimal.ONE, BigDecimal.ONE, new RuleEvidenceSummaryView(1, 1, 0),
            List.of(
                new RuleCategoryScoreView("ACTIVITY", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                    List.of(commitRule), List.of()),
                new RuleCategoryScoreView("DEVOPS", BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE,
                    List.of(workflowRule), List.of())),
            List.of(), Instant.parse("2026-08-31T00:00:00Z"));
        var evidence = new RuleEvidenceListView(evaluationId, List.of(new RuleEvidenceView(commitEvidence,
            "COMMIT_COUNT", "DIRECT", "SNAPSHOT_SIGNAL", "snapshot:signal:COMMIT_COUNT", "commits", BigDecimal.ONE)));
        String response = """
            {"strengths":[],"improvementAreas":[{"skillKey":"development-activity","skillName":"Activity",
            "explanation":"Activity needs attention.","evidenceIds":[]}]}
            """;

        SkillExplanationValidation result = validator.validate(response, weakMatrix, evaluation, evidence);

        assertThat(result.passed()).isTrue();
        assertThat(result.content().summary()).contains("Rule Engine 근거");
        assertThat(result.content().improvementAreas()).filteredOn(item -> item.skillKey().equals("development-activity"))
            .singleElement().satisfies(item -> assertThat(item.explanation()).contains("커밋으로 나누어", "추가 커밋 이력"));
        assertThat(result.content().improvementAreas()).filteredOn(item -> item.skillKey().equals("delivery-operations"))
            .singleElement().satisfies(item -> assertThat(item.explanation())
                .contains("배포 및 운영 개선 필요성", "CI 워크플로 설정 파일", "워크플로 설정이 감지"));
    }

    private static SkillMatrixView matrix(UUID evidenceId) {
        var skill = new SkillAssessmentView(UUID.randomUUID(), UUID.randomUUID(), "testing-discipline", "Testing",
            "TESTING", BigDecimal.valueOf(80), "STRONG", BigDecimal.valueOf(90), null, true, false, "STABLE", "rule-result", List.of(evidenceId),
            List.of(UUID.randomUUID()), List.of(), "rule-v1");
        return new SkillMatrixView(UUID.randomUUID(), UUID.randomUUID(), "skill-v1", "rule-v1", "CURRENT",
            List.of(skill), List.of("testing-discipline"), List.of(), Instant.parse("2026-08-31T00:00:00Z"));
    }

    private RuleEvaluationView evaluation() {
        var rule = new RuleResultView("TEST_FILES", "v1", "PASSED", BigDecimal.ONE, BigDecimal.ONE,
            BigDecimal.ONE, "PRESENCE", "trace", List.of());
        var category = new RuleCategoryScoreView("TESTING", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
            List.of(rule), List.of());
        return new RuleEvaluationView(matrix.evaluationId(), UUID.randomUUID(), UUID.randomUUID(), "rule-v1",
            "formula-v1", "extractor-v1", BigDecimal.ONE, BigDecimal.ONE,
            new RuleEvidenceSummaryView(1, 1, 0), List.of(category), List.of(), Instant.parse("2026-08-31T00:00:00Z"));
    }

    private RuleEvidenceListView evidence() {
        return new RuleEvidenceListView(matrix.evaluationId(), List.of(
            new RuleEvidenceView(evidenceId, "TEST_FILES", "DIRECT", "REPOSITORY_PATH",
                "snapshot:path:test", "test path", BigDecimal.ONE),
            new RuleEvidenceView(evidenceId, "OTHER_RULE", "SUPPORTING", "REPOSITORY_PATH",
                "snapshot:path:test", "test path", BigDecimal.ONE)));
    }
}
