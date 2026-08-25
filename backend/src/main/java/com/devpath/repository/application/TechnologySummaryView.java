package com.devpath.repository.application;

import com.devpath.repository.domain.RepositoryLanguage;
import com.devpath.repository.domain.DependencyTechnologyDetector;
import com.devpath.repository.domain.RepositorySnapshot;
import java.util.List;
import java.util.UUID;

public record TechnologySummaryView(
    UUID repositoryId,
    UUID snapshotId,
    String extractorVersion,
    String taxonomyVersion,
    String primaryLanguage,
    List<DetectedTechnologyView> technologies
) {
    static TechnologySummaryView from(RepositorySnapshot snapshot) {
        var values = new java.util.ArrayList<DetectedTechnologyView>();
        snapshot.languages().stream().map(DetectedTechnologyView::from).forEach(values::add);
        DependencyTechnologyDetector.detect(snapshot.dependencies()).stream()
            .map(DetectedTechnologyView::from).forEach(values::add);
        String primary = snapshot.languages().isEmpty() ? null : snapshot.languages().get(0).canonicalName();
        return new TechnologySummaryView(snapshot.repositoryId(), snapshot.id(), "repository-technology-summary-v1",
            RepositoryLanguage.TAXONOMY_VERSION, primary, List.copyOf(values));
    }
}
