package com.devpath.rule.application;

import java.util.List;
import java.util.UUID;

public record RuleEvidenceListView(UUID evaluationId, List<RuleEvidenceView> evidence) {
    public RuleEvidenceListView { evidence = List.copyOf(evidence); }
}
