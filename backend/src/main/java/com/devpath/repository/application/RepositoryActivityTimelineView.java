package com.devpath.repository.application;

import com.devpath.repository.domain.RepositoryActivityTimeline;
import com.devpath.repository.domain.RepositoryActivityTimelineExtractor;
import com.devpath.repository.domain.RepositorySnapshot;
import java.time.Instant;
import java.util.List;

public record RepositoryActivityTimelineView(
    String extractorVersion,
    String scope,
    Instant measuredAt,
    Instant latestActivityAt,
    Long daysSinceLatestActivity,
    int totalEventCount,
    boolean truncated,
    List<RepositoryActivityEventView> events
) {
    static RepositoryActivityTimelineView from(RepositorySnapshot snapshot) {
        RepositoryActivityTimeline timeline = RepositoryActivityTimelineExtractor.extract(snapshot);
        return new RepositoryActivityTimelineView(
            RepositoryActivityTimelineExtractor.EXTRACTOR_VERSION, timeline.scope(), timeline.measuredAt(),
            timeline.latestActivityAt(), timeline.daysSinceLatestActivity(), timeline.totalEventCount(),
            timeline.truncated(), timeline.events().stream().map(RepositoryActivityEventView::from).toList()
        );
    }
}
