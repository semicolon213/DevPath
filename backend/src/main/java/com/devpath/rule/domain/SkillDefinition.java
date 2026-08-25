package com.devpath.rule.domain;

import java.util.Objects;
import java.util.UUID;

public record SkillDefinition(UUID skillId, String stableKey, String name, RuleCategory category) {
    public SkillDefinition {
        Objects.requireNonNull(skillId); Objects.requireNonNull(category);
        if (stableKey == null || !stableKey.matches("[a-z0-9-]{2,64}") || name == null || name.isBlank() || name.length() > 120) {
            throw new IllegalArgumentException("skill definition is invalid");
        }
    }
}
