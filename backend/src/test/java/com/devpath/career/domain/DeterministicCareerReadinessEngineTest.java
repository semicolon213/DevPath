package com.devpath.career.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.rule.domain.RuleCategory;
import com.devpath.rule.domain.SkillAssessment;
import com.devpath.rule.domain.SkillDefinition;
import com.devpath.rule.domain.SkillLevel;
import com.devpath.rule.domain.SkillMatrix;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeterministicCareerReadinessEngineTest {
    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");
    private final DeterministicCareerReadinessEngine engine = new DeterministicCareerReadinessEngine();

    @Test
    void computesWeightedScoreAndConfidenceAndOrdersAllCategoryComparisons() {
        UUID profileId = UUID.randomUUID();
        UUID readinessId = UUID.randomUUID();
        var matrix = matrix(List.of(
            assessment(RuleCategory.LANGUAGE, "language", "80", "90"),
            assessment(RuleCategory.FRAMEWORK, "framework", "50", "70"),
            assessment(RuleCategory.TESTING, "testing", "0", "40")
        ));
        var policy = policy(profileId, Map.of(
            RuleCategory.LANGUAGE, new BigDecimal("30"),
            RuleCategory.FRAMEWORK, new BigDecimal("30"),
            RuleCategory.TESTING, new BigDecimal("40")
        ));

        CareerReadiness result = engine.evaluate(readinessId, matrix, profile(profileId), policy, NOW);
        CareerReadiness repeated = engine.evaluate(readinessId, matrix, profile(profileId), policy, NOW);

        assertThat(repeated).isEqualTo(result);
        assertThat(result.status()).isEqualTo(CareerReadinessStatus.COMPLETED);
        assertThat(result.readinessScore()).isEqualByComparingTo("39.00");
        assertThat(result.readinessLevel()).isEqualTo("BEGINNER");
        assertThat(result.confidence()).isEqualByComparingTo("64.00");
        assertThat(result.skillGaps()).extracting(SkillGap::gapState)
            .containsExactly(GapState.MISSING, GapState.PARTIAL, GapState.STRONG);
        assertThat(result.skillGaps()).allSatisfy(gap -> assertThat(gap.expectedMinimum()).isEqualByComparingTo("60"));
    }

    @Test
    void returnsInsufficientEvidenceWithoutInventingReadinessScore() {
        UUID profileId = UUID.randomUUID();
        var result = engine.evaluate(UUID.randomUUID(),
            matrix(List.of(assessment(RuleCategory.LANGUAGE, "language", "80", "90"))),
            profile(profileId), policy(profileId, Map.of(
                RuleCategory.LANGUAGE, new BigDecimal("50"),
                RuleCategory.FRAMEWORK, new BigDecimal("50"))), NOW);

        assertThat(result.status()).isEqualTo(CareerReadinessStatus.INSUFFICIENT_EVIDENCE);
        assertThat(result.readinessScore()).isNull();
        assertThat(result.readinessLevel()).isNull();
        assertThat(result.unavailableCategories()).containsExactly("FRAMEWORK");
        assertThat(result.confidence()).isEqualByComparingTo("45.00");
    }

    private SkillMatrix matrix(List<SkillAssessment> assessments) {
        return new SkillMatrix(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "skill-matrix-v2", "baseline-v2", "CURRENT", NOW, assessments);
    }

    private SkillAssessment assessment(RuleCategory category, String key, String score, String confidence) {
        BigDecimal value = new BigDecimal(score);
        SkillLevel level = value.signum() == 0 ? SkillLevel.NONE : value.compareTo(new BigDecimal("80")) >= 0
            ? SkillLevel.STRONG : value.compareTo(new BigDecimal("60")) >= 0 ? SkillLevel.COMPETENT
            : value.compareTo(new BigDecimal("40")) >= 0 ? SkillLevel.DEVELOPING : SkillLevel.BEGINNER;
        return new SkillAssessment(UUID.randomUUID(), new SkillDefinition(UUID.randomUUID(), key, key, category),
            value, level, new BigDecimal(confidence), level == SkillLevel.STRONG,
            level == SkillLevel.NONE || level == SkillLevel.BEGINNER, "UNAVAILABLE", "evaluation:test",
            List.of(UUID.randomUUID()), List.of(UUID.randomUUID()), List.of("category=" + category), "baseline-v2");
    }

    private CareerProfile profile(UUID profileId) {
        return new CareerProfile("backend", "Backend Engineer", "백엔드 엔지니어",
            CareerProfile.CareerStatus.SUPPORTED, profileId, "career-v2", "purpose", List.of(), List.of(),
            List.of(), List.of(), Map.of(), List.of(), NOW);
    }

    private CareerReadinessPolicy policy(UUID profileId, Map<RuleCategory, BigDecimal> weights) {
        return new CareerReadinessPolicy(UUID.randomUUID(), "readiness-v1", profileId,
            new BigDecimal("60"), new BigDecimal("40"), new BigDecimal("80"), weights);
    }
}
