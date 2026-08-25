package com.devpath.rule.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

final class RuleGoldenFixture {
    private final JsonNode root;

    private RuleGoldenFixture(JsonNode root) { this.root = root; }

    static RuleGoldenFixture baselineV1() throws IOException {
        Path path = Path.of("..", "fixtures", "rule-engine", "baseline-v1.json");
        return new RuleGoldenFixture(new ObjectMapper().readTree(Files.readString(path)));
    }

    static RuleGoldenFixture baselineV2() throws IOException {
        Path path = Path.of("..", "fixtures", "rule-engine", "baseline-v2.json");
        return new RuleGoldenFixture(new ObjectMapper().readTree(Files.readString(path)));
    }

    RuleSetVersion ruleSet() {
        var weights = new EnumMap<RuleCategory, BigDecimal>(RuleCategory.class);
        root.path("categoryWeights").fields().forEachRemaining(entry ->
            weights.put(RuleCategory.valueOf(entry.getKey()), entry.getValue().decimalValue()));
        var rules = new ArrayList<RuleDefinition>();
        for (JsonNode node : root.path("rules")) {
            rules.add(new RuleDefinition(node.path("id").asText(), "1.0.0",
                RuleCategory.valueOf(node.path("category").asText()), node.path("id").asText(), 20,
                node.path("signal").asText(), RuleFormula.valueOf(node.path("formula").asText()),
                node.path("parameter").decimalValue(), node.path("weight").decimalValue(), MissingDataPolicy.ZERO, true));
        }
        return new RuleSetVersion(root.path("ruleSetId").asText(), root.path("versionId").asText(),
            root.path("versionLabel").asText(), root.path("formulaLibraryVersion").asText(),
            root.path("extractorVersion").asText(), weights, rules);
    }

    List<RuleEvidenceFact> facts() {
        var facts = new ArrayList<RuleEvidenceFact>();
        root.path("facts").fields().forEachRemaining(entry -> facts.add(RuleEvidenceFact.measured(
            entry.getKey(), entry.getValue().decimalValue(), List.of("fixture:" + entry.getKey()))));
        return facts;
    }

    BigDecimal expectedOverall() { return root.path("expected").path("overallScore").decimalValue(); }
    BigDecimal expectedConfidence() { return root.path("expected").path("confidence").decimalValue(); }
    BigDecimal expectedCategory(RuleCategory category) {
        return root.path("expected").path("categoryScores").path(category.name()).decimalValue();
    }
}
