package com.devpath.career.adapter.out.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

class CareerReadinessWeightId implements Serializable {
    UUID policyId;
    UUID careerProfileVersionId;
    String category;
    CareerReadinessWeightId() {}
    CareerReadinessWeightId(UUID policyId, UUID careerProfileVersionId, String category) {
        this.policyId = policyId; this.careerProfileVersionId = careerProfileVersionId; this.category = category;
    }
    @Override public boolean equals(Object other) {
        return other instanceof CareerReadinessWeightId value && Objects.equals(policyId, value.policyId)
            && Objects.equals(careerProfileVersionId, value.careerProfileVersionId)
            && Objects.equals(category, value.category);
    }
    @Override public int hashCode() { return Objects.hash(policyId, careerProfileVersionId, category); }
}
