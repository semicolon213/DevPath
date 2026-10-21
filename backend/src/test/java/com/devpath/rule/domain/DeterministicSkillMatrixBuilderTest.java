package com.devpath.rule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
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
        assertThat(assessment.evidenceStatus()).isNull();
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

    @Test
    void appliesEveryConfiguredEvidenceStatusBoundaryExactly() {
        SkillMatrixPolicy policy = evidencePolicy();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertThat(DeterministicSkillMatrixBuilder.evidenceStatus(policy, List.of(), new BigDecimal("100")))
            .isEqualTo(EvidenceStatus.NONE);
        assertThat(DeterministicSkillMatrixBuilder.evidenceStatus(policy, List.of(first), new BigDecimal("100")))
            .isEqualTo(EvidenceStatus.INSUFFICIENT);
        assertThat(DeterministicSkillMatrixBuilder.evidenceStatus(policy, List.of(first, first), new BigDecimal("100")))
            .isEqualTo(EvidenceStatus.INSUFFICIENT);
        assertThat(DeterministicSkillMatrixBuilder.evidenceStatus(
            policy, List.of(first, second), new BigDecimal("59.99")))
            .isEqualTo(EvidenceStatus.INSUFFICIENT);
        assertThat(DeterministicSkillMatrixBuilder.evidenceStatus(
            policy, List.of(first, second), new BigDecimal("60.00")))
            .isEqualTo(EvidenceStatus.SUFFICIENT);
        assertThat(DeterministicSkillMatrixBuilder.evidenceStatus(
            policy(UUID.randomUUID()), List.of(first, second), new BigDecimal("100"))).isNull();
    }

    @Test
    void buildDerivesEachEvidenceStatusFromItsOwnDistinctEvidenceAndCategoryConfidence() {
        EvidenceFixture fixture = new EvidenceFixture();

        SkillMatrix matrix = fixture.build(fixture.policy(2, new BigDecimal("60")));

        // TESTING: two distinct ids (one linked twice), score below the threshold but confidence 100.
        assertThat(fixture.assessment(matrix, RuleCategory.TESTING).evidenceStatus())
            .isEqualTo(EvidenceStatus.SUFFICIENT);
        // DOCUMENTATION: the same evidence id linked by two rules counts once.
        assertThat(fixture.assessment(matrix, RuleCategory.DOCUMENTATION).evidenceStatus())
            .isEqualTo(EvidenceStatus.INSUFFICIENT);
        // ARCHITECTURE: two distinct ids, score 75 but category confidence 59.99.
        assertThat(fixture.assessment(matrix, RuleCategory.ARCHITECTURE).evidenceStatus())
            .isEqualTo(EvidenceStatus.INSUFFICIENT);
        // DEVOPS: no linked evidence; the link to an unknown rule is ignored.
        assertThat(fixture.assessment(matrix, RuleCategory.DEVOPS).evidenceStatus())
            .isEqualTo(EvidenceStatus.NONE);
        assertThat(fixture.assessment(matrix, RuleCategory.TESTING).evidenceIds())
            .containsExactlyElementsOf(fixture.sorted(fixture.testingFirst, fixture.testingSecond));
        assertThat(fixture.assessment(matrix, RuleCategory.DOCUMENTATION).evidenceIds())
            .containsExactly(fixture.documentation);
        assertThat(fixture.assessment(matrix, RuleCategory.DEVOPS).evidenceIds()).isEmpty();
    }

    @Test
    void buildWithEvidenceParametersKeepsScoreLevelAndFlagsOfTheEquivalentPolicyWithoutThem() {
        EvidenceFixture fixture = new EvidenceFixture();

        SkillMatrix withStatus = fixture.build(fixture.policy(2, new BigDecimal("60")));
        SkillMatrix withoutStatus = fixture.build(fixture.policy(null, null));

        assertThat(withoutStatus.assessments()).allSatisfy(value -> assertThat(value.evidenceStatus()).isNull());
        assertThat(withStatus.assessments()).hasSameSizeAs(withoutStatus.assessments());
        for (RuleCategory category : List.of(RuleCategory.TESTING, RuleCategory.DOCUMENTATION,
            RuleCategory.ARCHITECTURE, RuleCategory.DEVOPS)) {
            SkillAssessment expected = fixture.assessment(withoutStatus, category);
            SkillAssessment actual = fixture.assessment(withStatus, category);
            assertThat(actual.score()).isEqualByComparingTo(expected.score());
            assertThat(actual.level()).isEqualTo(expected.level());
            assertThat(actual.confidence()).isEqualByComparingTo(expected.confidence());
            assertThat(actual.strength()).isEqualTo(expected.strength());
            assertThat(actual.weakness()).isEqualTo(expected.weakness());
            assertThat(actual.evidenceIds()).isEqualTo(expected.evidenceIds());
            assertThat(actual.recommendationInputFacts()).isEqualTo(expected.recommendationInputFacts());
        }
        assertThat(fixture.assessment(withStatus, RuleCategory.TESTING))
            .extracting(SkillAssessment::level, SkillAssessment::weakness, SkillAssessment::strength)
            .containsExactly(SkillLevel.BEGINNER, true, false);
        assertThat(fixture.assessment(withStatus, RuleCategory.DOCUMENTATION))
            .extracting(SkillAssessment::level, SkillAssessment::weakness, SkillAssessment::strength)
            .containsExactly(SkillLevel.STRONG, false, true);
        assertThat(fixture.assessment(withStatus, RuleCategory.ARCHITECTURE).level()).isEqualTo(SkillLevel.COMPETENT);
        assertThat(fixture.assessment(withStatus, RuleCategory.DEVOPS).level()).isEqualTo(SkillLevel.NONE);
    }

    private static SkillMatrixPolicy policy(UUID versionId) {
        var skill = new SkillDefinition(UUID.randomUUID(), "testing-discipline", "Testing Discipline", RuleCategory.TESTING);
        return new SkillMatrixPolicy(UUID.randomUUID(), versionId, "skill-matrix-v1", BigDecimal.ONE,
            new BigDecimal("40"), new BigDecimal("60"), new BigDecimal("80"), new BigDecimal("39.99"),
            new BigDecimal("80"), null, null, List.of(skill));
    }

    private static SkillMatrixPolicy evidencePolicy() {
        var skill = new SkillDefinition(UUID.randomUUID(), "testing-discipline", "Testing Discipline", RuleCategory.TESTING);
        return new SkillMatrixPolicy(UUID.randomUUID(), UUID.randomUUID(), "skill-matrix-v3", BigDecimal.ONE,
            new BigDecimal("40"), new BigDecimal("60"), new BigDecimal("80"), new BigDecimal("39.99"),
            new BigDecimal("80"), 2, new BigDecimal("60"), List.of(skill));
    }

    private static final class EvidenceFixture {
        final UUID userId = UUID.randomUUID();
        final UUID snapshotId = UUID.randomUUID();
        final UUID versionId = UUID.randomUUID();
        final UUID repositoryId = UUID.randomUUID();
        final UUID testingFirst = UUID.randomUUID();
        final UUID testingSecond = UUID.randomUUID();
        final UUID documentation = UUID.randomUUID();
        final UUID architectureFirst = UUID.randomUUID();
        final UUID architectureSecond = UUID.randomUUID();
        final UUID unknownRule = UUID.randomUUID();
        final List<SkillDefinition> skills = List.of(
            new SkillDefinition(UUID.randomUUID(), "testing-discipline", "Testing Discipline", RuleCategory.TESTING),
            new SkillDefinition(UUID.randomUUID(), "documentation", "Documentation", RuleCategory.DOCUMENTATION),
            new SkillDefinition(UUID.randomUUID(), "architecture", "Architecture", RuleCategory.ARCHITECTURE),
            new SkillDefinition(UUID.randomUUID(), "devops", "DevOps", RuleCategory.DEVOPS));
        final CompletedRuleEvaluation evaluation;
        final List<RuleEvaluationEvidenceLink> links;

        EvidenceFixture() {
            var categories = List.of(
                category(RuleCategory.TESTING, "30.00", "100.00", "TEST_FILES", "TEST_RUNNER"),
                category(RuleCategory.DOCUMENTATION, "85.00", "100.00", "README_PRESENT", "ADR_PRESENT"),
                category(RuleCategory.ARCHITECTURE, "75.00", "59.99", "MODULE_BOUNDARIES"),
                category(RuleCategory.DEVOPS, "0.00", "0.00", "CI_PIPELINE"));
            var result = new RuleEvaluationResult(snapshotId.toString(), versionId.toString(), "baseline-v2",
                "formula-v1", "engineering-evidence-extractor-v2", new BigDecimal("47.50"), new BigDecimal("64.99"),
                categories, List.of());
            evaluation = new CompletedRuleEvaluation(UUID.randomUUID(), userId, snapshotId, versionId, "a".repeat(64),
                result, Instant.parse("2026-10-19T00:00:00Z"), Instant.parse("2026-10-19T00:00:00Z"));
            links = List.of(
                link("TEST_FILES", testingFirst), link("TEST_RUNNER", testingSecond), link("TEST_FILES", testingSecond),
                link("README_PRESENT", documentation), link("ADR_PRESENT", documentation),
                link("MODULE_BOUNDARIES", architectureFirst), link("MODULE_BOUNDARIES", architectureSecond),
                link("UNKNOWN_RULE", unknownRule));
        }

        SkillMatrixPolicy policy(Integer evidenceMinimumCount, BigDecimal evidenceConfidenceThreshold) {
            return new SkillMatrixPolicy(UUID.randomUUID(), versionId,
                evidenceMinimumCount == null ? "skill-matrix-v2" : "skill-matrix-v3", BigDecimal.ONE,
                new BigDecimal("40"), new BigDecimal("60"), new BigDecimal("80"), new BigDecimal("39.99"),
                new BigDecimal("80"), evidenceMinimumCount, evidenceConfidenceThreshold, skills);
        }

        SkillMatrix build(SkillMatrixPolicy policy) {
            return new DeterministicSkillMatrixBuilder().build(UUID.randomUUID(), evaluation, repositoryId, policy,
                links, Instant.parse("2026-10-19T01:00:00Z"));
        }

        SkillAssessment assessment(SkillMatrix matrix, RuleCategory category) {
            return matrix.assessments().stream().filter(value -> value.skill().category() == category)
                .findFirst().orElseThrow();
        }

        List<UUID> sorted(UUID... ids) {
            return Arrays.stream(ids).sorted().toList();
        }

        private RuleEvaluationEvidenceLink link(String ruleId, UUID evidenceId) {
            return new RuleEvaluationEvidenceLink(ruleId, "DIRECT", new RuleEvaluationEvidence(evidenceId, userId,
                snapshotId, "REPOSITORY_PATH", "snapshot:" + snapshotId + ":evidence:" + evidenceId,
                "Normalized repository-path evidence", new BigDecimal("100")));
        }

        private static RuleCategoryScore category(RuleCategory category, String score, String confidence,
            String... ruleIds) {
            var rules = Arrays.stream(ruleIds).map(ruleId -> new RuleExecutionResult(ruleId, "1", category,
                RuleOutcomeStatus.PARTIAL, BigDecimal.ONE, new BigDecimal(score), BigDecimal.ONE,
                "COUNT_CAP@formula-v1", "formula=COUNT_CAP; score=" + score, List.of())).toList();
            return new RuleCategoryScore(category, new BigDecimal(score), new BigDecimal("0.250000"),
                new BigDecimal(confidence), rules, List.of());
        }
    }
}
