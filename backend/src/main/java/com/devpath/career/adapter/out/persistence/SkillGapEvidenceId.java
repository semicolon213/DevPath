package com.devpath.career.adapter.out.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

class SkillGapEvidenceId implements Serializable {
    UUID gapId;
    UUID evidenceId;
    SkillGapEvidenceId() {}
    SkillGapEvidenceId(UUID gapId, UUID evidenceId) { this.gapId = gapId; this.evidenceId = evidenceId; }
    @Override public boolean equals(Object other) {
        return other instanceof SkillGapEvidenceId value && Objects.equals(gapId, value.gapId)
            && Objects.equals(evidenceId, value.evidenceId);
    }
    @Override public int hashCode() { return Objects.hash(gapId, evidenceId); }
}
