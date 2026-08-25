package com.devpath.repository.application;

import com.devpath.repository.domain.RepositoryLanguage;
import com.devpath.repository.domain.RepositoryTechnology;
import java.util.List;

public record DetectedTechnologyView(
    String name,
    String category,
    String evidenceLabel,
    Long byteCount,
    Double percentage,
    String taxonomyStatus,
    String evidenceType,
    List<String> evidencePaths
) {
    static DetectedTechnologyView from(RepositoryLanguage language) {
        return new DetectedTechnologyView(
            language.canonicalName(), "LANGUAGE", language.providerLabel(), language.byteCount(),
            language.percentage().doubleValue(), language.taxonomyStatus(), "PROVIDER_LANGUAGE_STATISTICS", List.of()
        );
    }

    static DetectedTechnologyView from(RepositoryTechnology technology) {
        return new DetectedTechnologyView(
            technology.name(), technology.category(), technology.evidenceLabel(), null, null,
            "SUPPORTED", "DEPENDENCY_DECLARATION", technology.evidencePaths()
        );
    }
}
