package com.devpath.repository.domain;

import java.util.List;

public record EngineeringEvidenceSignal(
    String signalKey,
    String label,
    boolean present,
    int count,
    String observedValue,
    List<String> evidencePaths
) {
    public EngineeringEvidenceSignal {
        evidencePaths = List.copyOf(evidencePaths).stream().limit(20).toList();
    }
}
