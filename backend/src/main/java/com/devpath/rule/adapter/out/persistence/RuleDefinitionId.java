package com.devpath.rule.adapter.out.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class RuleDefinitionId implements Serializable {
    private UUID ruleSetVersionId;
    private String ruleId;

    public RuleDefinitionId() {}

    public RuleDefinitionId(UUID ruleSetVersionId, String ruleId) {
        this.ruleSetVersionId = ruleSetVersionId;
        this.ruleId = ruleId;
    }

    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof RuleDefinitionId other)) return false;
        return Objects.equals(ruleSetVersionId, other.ruleSetVersionId) && Objects.equals(ruleId, other.ruleId);
    }

    @Override public int hashCode() { return Objects.hash(ruleSetVersionId, ruleId); }
}
