package com.devpath.rule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeterministicSkillMatrixBuilderTest {
    @Test
    void buildsTraceableSkillAssessmentFromOfficialCategoryScore() {
        UUID userId = UUID.randomUUID(); UUID snapshotId = UUID.randomUUID(); UUID evaluationId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID(); UUID repositoryId = UUID.randomUUID(); UUID evidenceId = UUID.randomUUID();
        var rule = new RuleExecutionResult("TEST_FILES", "1", RuleCategory.TESTING, RuleOutcomeStatus.PARTIAL,
            new BigDecimal("8"), new BigDecimal("75.00"), BigDecimal.ONE, "COUNT_CAP@formula-v1",
            "formula=COUNT_CAP; raw=8; score=75.00", List.of("snapshot:" + snapshotId + ":path:test.java"));
        var category = new RuleCategoryScore(RuleCategory.TESTING, new BigDecimal("75.00"), BigDecimal.ONE,
            new BigDecimal("100.00"), List.of(rule), List.of());
        var result = new RuleEvaluationResult(snapshotId.toString(), versionId.toString(), "baseline-v1", "formula-v1",
            "engineering-evidence-extractor-v1", new BigDecimal("75.00"), new BigDecimal("100.00"), List.of(category), List.of());
        var evaluation = new CompletedRuleEvaluation(evaluationId, userId, snapshotId, versionId, "a".repeat(64), result,
            Instant.parse("2026-08-11T00:00:00Z"), Instant.parse("2026-08-11T00:00:00Z"));
        var evidence = new RuleEvaluationEvidence(evidenceId, userId, snapshotId, "REPOSITORY_PATH",
            "snapshot:" + snapshotId + ":path:test.java", "Normalized repository-path evidence", new BigDecimal("100"));
        SkillMatrixPolicy policy = policy(versionId);

        SkillMatrix matrix = new DeterministicSkillMatrixBuilder().build(UUID.randomUUID(), evaluation, repositoryId,
            policy, List.of(new RuleEvaluationEvidenceLink("TEST_FILES", "DIRECT", evidence)),
            Instant.parse("2026-08-11T01:00:00Z"));

        SkillAssessment assessment = matrix.assessments().getFirst();
        assertThat(assessment.score()).isEqualByComparingTo("75.00");
        assertThat(assessment.level()).isEqualTo(SkillLevel.COMPETENT);
        assertThat(assessment.strength()).isFalse();
        assertThat(assessment.weakness()).isFalse();
        assertThat(assessment.evidenceIds()).containsExactly(evidenceId);
        assertThat(assessment.repositoryIds()).containsExactly(repositoryId);
        assertThat(assessment.aggregateRuleResultReference()).contains(evaluationId.toString(), "TESTING");
        assertThat(assessment.recommendationInputFacts()).contains("CATEGORY_SCORE=75.00");
    }

    @Test
    void appliesEveryConfiguredLevelBoundaryExactly() {
        SkillMatrixPolicy policy = policy(UUID.randomUUID());
        assertThat(policy.level(new BigDecimal("0"))).isEqualTo(SkillLevel.NONE);
        assertThat(policy.level(new BigDecimal("1"))).isEqualTo(SkillLevel.BEGINNER);
        assertThat(policy.level(new BigDecimal("39.99"))).isEqualTo(SkillLevel.BEGINNER);
        assertThat(policy.level(new BigDecimal("40"))).isEqualTo(SkillLevel.DEVELOPING);
        assertThat(policy.level(new BigDecimal("59.99"))).isEqualTo(SkillLevel.DEVELOPING);
        assertThat(policy.level(new BigDecimal("60"))).isEqualTo(SkillLevel.COMPETENT);
        assertThat(policy.level(new BigDecimal("79.99"))).isEqualTo(SkillLevel.COMPETENT);
        assertThat(policy.level(new BigDecimal("80"))).isEqualTo(SkillLevel.STRONG);
        assertThat(policy.level(new BigDecimal("100"))).isEqualTo(SkillLevel.STRONG);
    }

    private static SkillMatrixPolicy policy(UUID versionId) {
        var skill = new SkillDefinition(UUID.randomUUID(), "testing-discipline", "Testing Discipline", RuleCategory.TESTING);
        return new SkillMatrixPolicy(UUID.randomUUID(), versionId, "skill-matrix-v1", BigDecimal.ONE,
            new BigDecimal("40"), new BigDecimal("60"), new BigDecimal("80"), new BigDecimal("39.99"),
            new BigDecimal("80"), List.of(skill));
    }
}
