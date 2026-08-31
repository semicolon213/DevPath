package com.devpath.ai.application;

import java.util.List;

public record SkillExplanationValidation(SkillExplanationContent content, List<String> violations) {
    public SkillExplanationValidation {
        violations = List.copyOf(violations);
    }
    public boolean passed() { return violations.isEmpty(); }
}
