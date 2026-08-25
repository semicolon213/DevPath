package com.devpath.rule.application;

import java.math.BigDecimal;
import java.util.List;

public record RuleResultView(
    String ruleId, String ruleVersion, String status, BigDecimal rawValue, BigDecimal score,
    BigDecimal weight, String formulaId, String trace, List<String> evidenceReferences
) {
    public RuleResultView { evidenceReferences = List.copyOf(evidenceReferences); }
}
