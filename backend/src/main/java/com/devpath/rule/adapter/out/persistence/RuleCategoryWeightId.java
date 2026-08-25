package com.devpath.rule.adapter.out.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class RuleCategoryWeightId implements Serializable {
    private UUID ruleSetVersionId;
    private String category;

    public RuleCategoryWeightId() {}

    public RuleCategoryWeightId(UUID ruleSetVersionId, String category) {
        this.ruleSetVersionId = ruleSetVersionId;
        this.category = category;
    }

    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof RuleCategoryWeightId other)) return false;
        return Objects.equals(ruleSetVersionId, other.ruleSetVersionId) && Objects.equals(category, other.category);
    }

    @Override public int hashCode() { return Objects.hash(ruleSetVersionId, category); }
}
