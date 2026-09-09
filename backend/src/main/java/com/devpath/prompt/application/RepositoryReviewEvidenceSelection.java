package com.devpath.prompt.application;

import com.devpath.rule.application.RuleEvidenceView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RepositoryReviewEvidenceSelection {
    public static final List<String> CATEGORIES = List.of(
        "ARCHITECTURE", "TESTING", "DEVOPS", "DOCUMENTATION", "COLLABORATION"
    );
    private static final List<String> KEYS = List.of("A", "B");

    private RepositoryReviewEvidenceSelection() {}

    public static Map<String, List<SelectedEvidence>> select(List<RuleEvidenceView> evidence) {
        Map<String, List<SelectedEvidence>> selected = new LinkedHashMap<>();
        CATEGORIES.forEach(category -> selected.put(category, new ArrayList<>()));
        for (RuleEvidenceView value : evidence) {
            String category = categoryForRule(value.ruleId());
            if (category.isEmpty() || selected.get(category).size() >= KEYS.size()) continue;
            List<SelectedEvidence> categoryEvidence = selected.get(category);
            categoryEvidence.add(new SelectedEvidence(KEYS.get(categoryEvidence.size()), value));
        }
        return selected;
    }

    public static String categoryForRule(String ruleId) {
        if (ruleId.startsWith("LANGUAGE_") || ruleId.startsWith("FRAMEWORK_")
            || "STRUCTURED_BOUNDARIES".equals(ruleId) || "MODULE_LAYOUT".equals(ruleId)
            || "ARCHITECTURE_DOCUMENTATION_STRUCTURE".equals(ruleId)
            || "ARCHITECTURE_DOCUMENTATION".equals(ruleId)) return "ARCHITECTURE";
        if (ruleId.startsWith("TEST_")) return "TESTING";
        if (ruleId.startsWith("CI_") || ruleId.startsWith("CONTAINER_")
            || ruleId.startsWith("INFRASTRUCTURE_") || ruleId.startsWith("DEPLOYMENT_")) return "DEVOPS";
        if (ruleId.startsWith("README_") || "README_PRESENT".equals(ruleId)
            || "API_DOCUMENTATION".equals(ruleId) || "LICENSE_PRESENT".equals(ruleId)) return "DOCUMENTATION";
        if (ruleId.startsWith("CONTRIBUT") || ruleId.startsWith("COMMIT_")
            || ruleId.startsWith("BRANCH_") || ruleId.startsWith("ACTIVITY_")) return "COLLABORATION";
        return "";
    }

    public record SelectedEvidence(String key, RuleEvidenceView evidence) {}
}
