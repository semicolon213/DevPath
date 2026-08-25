package com.devpath.repository.application;

import com.devpath.repository.domain.EngineeringEvidenceCategory;
import java.util.List;

public record EvidenceCategoryView(String category, String label, List<EvidenceSignalView> signals) {
    static EvidenceCategoryView from(EngineeringEvidenceCategory category) {
        return new EvidenceCategoryView(category.category(), category.label(),
            category.signals().stream().map(EvidenceSignalView::from).toList());
    }
}
