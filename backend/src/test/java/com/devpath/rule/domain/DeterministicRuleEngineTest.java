package com.devpath.rule.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeterministicRuleEngineTest {
    private final DeterministicRuleEngine engine = new DeterministicRuleEngine();

    @Test
    void baselineV1MatchesGoldenScoresAndIsDeterministic() throws Exception {
        RuleGoldenFixture fixture = RuleGoldenFixture.baselineV1();

        RuleEvaluationResult first = engine.evaluate("snapshot-1", "engineering-evidence-extractor-v1", fixture.ruleSet(), fixture.facts());
        RuleEvaluationResult second = engine.evaluate("snapshot-1", "engineering-evidence-extractor-v1", fixture.ruleSet(), fixture.facts());

        assertThat(first).isEqualTo(second);
        assertThat(first.overallScore()).isEqualByComparingTo(fixture.expectedOverall());
        assertThat(first.confidence()).isEqualByComparingTo(fixture.expectedConfidence());
        for (RuleCategory category : fixture.ruleSet().categoryWeights().keySet()) {
            assertThat(first.categoryScores().stream().filter(value -> value.category() == category).findFirst().orElseThrow().score())
                .isEqualByComparingTo(fixture.expectedCategory(category));
        }
        assertThat(first.categoryScores()).flatExtracting(RuleCategoryScore::ruleResults)
            .allSatisfy(result -> {
                assertThat(result.formulaId()).endsWith("@formula-v1");
                assertThat(result.trace()).contains("formula=", "raw=", "score=");
                assertThat(result.evidenceReferences()).isNotEmpty();
            });
    }

    @Test
    void baselineV2MatchesApprovedGoldenScoresAndIsDeterministic() throws Exception {
        RuleGoldenFixture fixture = RuleGoldenFixture.baselineV2();

        RuleEvaluationResult first = engine.evaluate("snapshot-2", "engineering-evidence-extractor-v2",
            fixture.ruleSet(), fixture.facts());
        RuleEvaluationResult second = engine.evaluate("snapshot-2", "engineering-evidence-extractor-v2",
            fixture.ruleSet(), fixture.facts());

        assertThat(first).isEqualTo(second);
        assertThat(first.overallScore()).isEqualByComparingTo(fixture.expectedOverall());
        assertThat(first.confidence()).isEqualByComparingTo(fixture.expectedConfidence());
        for (RuleCategory category : fixture.ruleSet().categoryWeights().keySet()) {
            assertThat(first.categoryScores().stream().filter(value -> value.category() == category)
                .findFirst().orElseThrow().score()).isEqualByComparingTo(fixture.expectedCategory(category));
        }
    }

    @Test
    void rejectsExtractorMismatchAndInvalidWeights() throws Exception {
        RuleGoldenFixture fixture = RuleGoldenFixture.baselineV1();
        assertThatThrownBy(() -> engine.evaluate("snapshot-1", "wrong-version", fixture.ruleSet(), fixture.facts()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("requires extractor version");

        var invalid = new EnumMap<RuleCategory, BigDecimal>(fixture.ruleSet().categoryWeights());
        invalid.put(RuleCategory.LANGUAGE, new BigDecimal("0.10"));
        assertThatThrownBy(() -> new RuleSetVersion("set", "version", "bad", "formula-v1",
            "engineering-evidence-extractor-v1", invalid, fixture.ruleSet().rules()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must total");
    }

    @Test
    void skippedEvidenceReducesConfidenceWithoutInventingScore() {
        var rule = new RuleDefinition("TEST", "1", RuleCategory.TESTING, "test", 20, "UNAVAILABLE",
            RuleFormula.PRESENCE, BigDecimal.ZERO, BigDecimal.ONE, MissingDataPolicy.SKIP, true);
        var set = new RuleSetVersion("set", "version", "v1", "formula-v1", "extractor-v1",
            Map.of(RuleCategory.TESTING, BigDecimal.ONE), List.of(rule));

        RuleEvaluationResult result = engine.evaluate("snapshot", "extractor-v1", set,
            List.of(RuleEvidenceFact.unavailable("UNAVAILABLE")));

        assertThat(result.overallScore()).isEqualByComparingTo("0.00");
        assertThat(result.confidence()).isEqualByComparingTo("0.00");
        assertThat(result.categoryScores().getFirst().ruleResults().getFirst().status()).isEqualTo(RuleOutcomeStatus.SKIPPED);
        assertThat(result.warnings()).hasSize(1);
    }
}
