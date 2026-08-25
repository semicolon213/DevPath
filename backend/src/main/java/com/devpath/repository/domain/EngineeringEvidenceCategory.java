package com.devpath.repository.domain;

import java.util.List;

public record EngineeringEvidenceCategory(String category, String label, List<EngineeringEvidenceSignal> signals) {
    public EngineeringEvidenceCategory {
        signals = List.copyOf(signals);
    }
}
