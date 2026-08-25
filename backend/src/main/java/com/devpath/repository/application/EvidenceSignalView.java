package com.devpath.repository.application;

import com.devpath.repository.domain.EngineeringEvidenceSignal;
import java.util.List;

public record EvidenceSignalView(
    String signalKey, String label, boolean present, int count, String observedValue, List<String> evidencePaths
) {
    static EvidenceSignalView from(EngineeringEvidenceSignal signal) {
        return new EvidenceSignalView(signal.signalKey(), signal.label(), signal.present(), signal.count(),
            signal.observedValue(), signal.evidencePaths());
    }
}
