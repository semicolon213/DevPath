package com.devpath.repository.domain;

import java.time.Instant;
import java.util.List;

public record RepositoryActivityTimeline(
    String scope,
    Instant measuredAt,
    Instant latestActivityAt,
    Long daysSinceLatestActivity,
    int totalEventCount,
    boolean truncated,
    List<RepositoryActivityEvent> events
) {
    public RepositoryActivityTimeline {
        events = List.copyOf(events);
        if (!"CURRENT_SNAPSHOT".equals(scope) || measuredAt == null || totalEventCount < events.size()
            || truncated != (totalEventCount > events.size())
            || (latestActivityAt == null) != (daysSinceLatestActivity == null)
            || daysSinceLatestActivity != null && daysSinceLatestActivity < 0) {
            throw new IllegalArgumentException("Repository activity timeline is invalid");
        }
    }
}
