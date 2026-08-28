package com.devpath.repository.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RepositoryActivityTimelineExtractor {
    public static final String EXTRACTOR_VERSION = "repository-activity-timeline-v1";
    public static final int EVENT_LIMIT = 100;

    private RepositoryActivityTimelineExtractor() {}

    public static RepositoryActivityTimeline extract(RepositorySnapshot snapshot) {
        var events = new ArrayList<RepositoryActivityEvent>();
        snapshot.commits().forEach(value -> events.add(new RepositoryActivityEvent(
            "COMMIT", value.sha(), value.committedAt()
        )));
        snapshot.pullRequests().forEach(value -> {
            events.add(new RepositoryActivityEvent(
                "PULL_REQUEST_OPENED", value.providerPullRequestId(), value.openedAt()
            ));
            if (value.mergedAt() != null) {
                events.add(new RepositoryActivityEvent(
                    "PULL_REQUEST_MERGED", value.providerPullRequestId(), value.mergedAt()
                ));
            } else if (value.closedAt() != null) {
                events.add(new RepositoryActivityEvent(
                    "PULL_REQUEST_CLOSED", value.providerPullRequestId(), value.closedAt()
                ));
            }
        });
        snapshot.issues().forEach(value -> {
            events.add(new RepositoryActivityEvent("ISSUE_OPENED", value.providerIssueId(), value.openedAt()));
            if (value.closedAt() != null) {
                events.add(new RepositoryActivityEvent("ISSUE_CLOSED", value.providerIssueId(), value.closedAt()));
            }
        });
        Comparator<RepositoryActivityEvent> order = Comparator
            .comparing(RepositoryActivityEvent::occurredAt).reversed()
            .thenComparing(RepositoryActivityEvent::eventType)
            .thenComparing(RepositoryActivityEvent::sourceReference);
        List<RepositoryActivityEvent> sorted = events.stream().sorted(order).toList();
        var latest = sorted.isEmpty() ? null : sorted.getFirst().occurredAt();
        Long age = latest == null ? null : Math.max(0, Duration.between(latest, snapshot.capturedAt()).toDays());
        List<RepositoryActivityEvent> visible = sorted.stream().limit(EVENT_LIMIT).toList();
        return new RepositoryActivityTimeline(
            "CURRENT_SNAPSHOT", snapshot.capturedAt(), latest, age, sorted.size(),
            sorted.size() > visible.size(), visible
        );
    }
}
