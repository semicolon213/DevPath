package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleCategoryScoreView;
import com.devpath.rule.application.RuleEvidenceSummaryView;
import com.devpath.rule.application.RuleEvidenceView;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.RuleResultView;
import com.devpath.prompt.application.RepositorySummaryRuleOutputSelection;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositorySummaryValidatorTest {
    private final RepositorySummaryValidator validator = new RepositorySummaryValidator(new ObjectMapper());

    @Test
    void acceptsOnlySuppliedTechnologiesAndGroundingReferences() {
        Source source = source();
        String response = response("E1", source.ruleReference());

        var validation = validator.validate(response, List.of("Java"), source.evaluation(), source.evidence());

        assertThat(validation.passed()).isTrue();
        assertThat(validation.content().summary().evidenceReferences()).containsExactly(source.evidenceId());
        assertThat(validation.content().strengths().getFirst().ruleOutputReferences()).containsExactly(source.ruleReference());
        assertThat(validator.validate(response.replace("Java", "Python"), List.of("Java"), source.evaluation(), source.evidence()).violations())
            .contains("UNSUPPORTED_TECHNOLOGIES");
    }

    @Test
    void rejectsMalformedUnsupportedDuplicateNumericAndIndividuallyUngroundedClaims() {
        Source source = source();
        assertThat(validator.validate("{}", List.of("Java"), source.evaluation(), source.evidence()).violations())
            .contains("INVALID_RESPONSE_SCHEMA");
        assertThat(validator.validate(response("E1", source.ruleReference()).replace("\"technologies\"", "\"extra\":true,\"technologies\""), List.of("Java"), source.evaluation(), source.evidence()).violations())
            .contains("INVALID_RESPONSE_SCHEMA");
        assertThat(validator.validate(response("E9", source.ruleReference()), List.of("Java"), source.evaluation(), source.evidence()).violations())
            .contains("INVALID_RESPONSE_SCHEMA");
        assertThat(validator.validate(response(UUID.randomUUID().toString(), source.ruleReference()), List.of("Java"), source.evaluation(), source.evidence()).violations())
            .contains("UNSUPPORTED_EVIDENCE_REFERENCE");
        assertThat(validator.validate(response("E1", "unsupported-rule-output"), List.of("Java"), source.evaluation(), source.evidence()).violations())
            .contains("UNSUPPORTED_RULE_OUTPUT_REFERENCE");
        assertThat(validator.validate(response("E1", source.ruleReference()).replace("[\"" + source.ruleReference() + "\"]", "[\"" + source.ruleReference() + "\",\"" + source.ruleReference() + "\"]"), List.of("Java"), source.evaluation(), source.evidence()).violations())
            .contains("UNSUPPORTED_RULE_OUTPUT_REFERENCE");
        assertThat(validator.validate(response("E1", source.ruleReference()).replace("Grounded summary", "Has 2 facts"), List.of("Java"), source.evaluation(), source.evidence()).violations())
            .contains("UNTRACEABLE_NUMERIC_CLAIM");
        assertThat(validator.validate(response("E1", source.ruleReference()).replace("Grounded summary", "<script>bad</script>"), List.of("Java"), source.evaluation(), source.evidence()).violations())
            .contains("UNSAFE_RENDERED_CONTENT");
        String unrelatedGrounding = "{\"summary\":" + claim("Ungrounded summary", "", "")
            + ",\"strengths\":[" + claim("Grounded strength", "E1", "")
            + "],\"improvementAreas\":[],\"technologies\":[\"Java\"]}";
        assertThat(validator.validate(unrelatedGrounding, List.of("Java"), source.evaluation(), source.evidence()).violations())
            .contains("MISSING_GROUNDING_REFERENCE");
    }

    @Test
    void permitsEmptyStrengthsAndImprovementAreasButBoundsTheirSize() {
        Source source = source();
        assertThat(validator.validate(response("E1", source.ruleReference()), List.of("Java"), source.evaluation(), source.evidence()).passed()).isTrue();
        String items = java.util.stream.Stream.of("a", "b", "c", "d", "e", "f")
            .map(value -> claim(value, "E1", "")).collect(java.util.stream.Collectors.joining(","));
        String tooMany = response("E1", source.ruleReference()).replace("\"strengths\":[" + claim("Grounded strength", "", source.ruleReference()) + "]", "\"strengths\":[" + items + "]");
        assertThat(validator.validate(tooMany, List.of("Java"), source.evaluation(), source.evidence()).violations()).contains("INVALID_RESPONSE_SCHEMA");
    }

    private static String response(String evidence, String output) {
        return "{\"summary\":" + claim("Grounded summary", evidence, "")
            + ",\"strengths\":[" + claim("Grounded strength", "", output)
            + "],\"improvementAreas\":[],\"technologies\":[\"Java\"]}";
    }

    private static String claim(String text, String evidence, String output) {
        return "{\"text\":\"" + text + "\",\"evidenceReferences\":" + references(evidence)
            + ",\"ruleOutputReferences\":" + references(output) + "}";
    }

    private static String references(String value) {
        return value.isEmpty() ? "[]" : "[\"" + value + "\"]";
    }

    private static Source source() {
        UUID evidenceId = UUID.randomUUID(), evaluationId = UUID.randomUUID();
        var evaluation = new RuleEvaluationView(evaluationId, UUID.randomUUID(), UUID.randomUUID(), "rule-v1", "formula-v1", "extractor-v1", BigDecimal.ONE, BigDecimal.ONE, new RuleEvidenceSummaryView(1, 0, 0), List.of(new RuleCategoryScoreView("DOCUMENTATION", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, List.of(new RuleResultView("README_PRESENT", "v1", "SATISFIED", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "BOOLEAN_100", "trace", List.of())), List.of())), List.of(), Instant.EPOCH);
        var evidence = new RuleEvidenceListView(evaluationId, List.of(new RuleEvidenceView(evidenceId, "README_PRESENT", "DIRECT", "REPOSITORY_PATH", "README.md", "README is present", BigDecimal.ONE)));
        return new Source(evidenceId, evaluation, evidence, RepositorySummaryRuleOutputSelection.select(evaluation).getFirst().reference());
    }

    private record Source(UUID evidenceId, RuleEvaluationView evaluation, RuleEvidenceListView evidence, String ruleReference) {}
}
