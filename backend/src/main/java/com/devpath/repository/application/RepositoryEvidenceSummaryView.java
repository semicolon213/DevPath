package com.devpath.repository.application;

import com.devpath.repository.domain.RepositoryEvidenceExtractor;
import com.devpath.repository.domain.RepositorySnapshot;
import java.util.List;
import java.util.UUID;

public record RepositoryEvidenceSummaryView(
    UUID repositoryId,
    UUID snapshotId,
    String extractorVersion,
    List<EvidenceCategoryView> categories,
    RepositoryActivityTimelineView activityTimeline
) {
    static RepositoryEvidenceSummaryView from(RepositorySnapshot snapshot) {
        return new RepositoryEvidenceSummaryView(snapshot.repositoryId(), snapshot.id(),
            RepositoryEvidenceExtractor.EXTRACTOR_VERSION,
            RepositoryEvidenceExtractor.extract(snapshot).stream().map(EvidenceCategoryView::from).toList(),
            RepositoryActivityTimelineView.from(snapshot));
    }
}
