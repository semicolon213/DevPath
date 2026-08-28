package com.devpath.repository.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RepositoryActivityTimelineExtractorTest {
    @Test
    void createsANewestFirstMeasuredTimelineWithoutInventingAStalenessClassification() {
        Instant capturedAt = Instant.parse("2026-08-11T00:00:00Z");
        String revision = "a".repeat(40);
        RepositorySnapshot snapshot = RepositorySnapshot.ready(
            UUID.randomUUID(), UUID.randomUUID(), revision, capturedAt,
            List.of(new RepositoryBranch("main", revision, true)),
            List.of(new RepositoryCommit(revision, "owner", capturedAt.minusSeconds(86_400), "commit")),
            List.of(), List.of(), List.of(),
            List.of(new RepositoryPullRequest("501", "MERGED", capturedAt.minusSeconds(259_200),
                capturedAt.minusSeconds(43_200), capturedAt.minusSeconds(43_200), 2)),
            List.of(new RepositoryIssue("601", "CLOSED", List.of("bug"),
                capturedAt.minusSeconds(345_600), capturedAt.minusSeconds(172_800))),
            List.of()
        );

        RepositoryActivityTimeline timeline = RepositoryActivityTimelineExtractor.extract(snapshot);

        assertThat(timeline.scope()).isEqualTo("CURRENT_SNAPSHOT");
        assertThat(timeline.latestActivityAt()).isEqualTo(capturedAt.minusSeconds(43_200));
        assertThat(timeline.daysSinceLatestActivity()).isZero();
        assertThat(timeline.totalEventCount()).isEqualTo(5);
        assertThat(timeline.truncated()).isFalse();
        assertThat(timeline.events()).extracting(RepositoryActivityEvent::eventType)
            .containsExactly("PULL_REQUEST_MERGED", "COMMIT", "ISSUE_CLOSED", "PULL_REQUEST_OPENED", "ISSUE_OPENED");
    }

    @Test
    void boundsLargeTimelinesAndReportsTheCompleteMeasuredCount() {
        Instant capturedAt = Instant.parse("2026-08-11T00:00:00Z");
        String revision = "a".repeat(40);
        var commits = IntStream.range(0, 105).mapToObj(index -> new RepositoryCommit(
            "%040x".formatted(index + 1), "owner", capturedAt.minusSeconds(index), "commit"
        )).toList();
        RepositorySnapshot snapshot = RepositorySnapshot.ready(
            UUID.randomUUID(), UUID.randomUUID(), revision, capturedAt,
            List.of(new RepositoryBranch("main", revision, true)), commits
        );

        RepositoryActivityTimeline timeline = RepositoryActivityTimelineExtractor.extract(snapshot);

        assertThat(timeline.events()).hasSize(100);
        assertThat(timeline.totalEventCount()).isEqualTo(105);
        assertThat(timeline.truncated()).isTrue();
        assertThat(timeline.events()).isSortedAccordingTo(java.util.Comparator
            .comparing(RepositoryActivityEvent::occurredAt).reversed()
            .thenComparing(RepositoryActivityEvent::eventType)
            .thenComparing(RepositoryActivityEvent::sourceReference));
    }
}
