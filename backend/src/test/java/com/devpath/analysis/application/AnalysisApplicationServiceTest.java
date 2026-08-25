package com.devpath.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devpath.analysis.domain.AnalysisJob;
import com.devpath.analysis.domain.AnalysisJobStatus;
import com.devpath.analysis.domain.CompletedAnalysis;
import com.devpath.repository.domain.RepositoryBranch;
import com.devpath.repository.domain.RepositoryCommit;
import com.devpath.repository.domain.RepositorySnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalysisApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");

    @Test
    void queuesIdempotentlyClaimsAndPublishesOwnerScopedResultReferences() {
        UUID userId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        RepositorySnapshot snapshot = snapshot(userId, repositoryId);
        var persistence = new InMemoryPersistence();
        var service = new AnalysisApplicationService(persistence, (owner, repository, requestedSnapshot) -> {
            if (!owner.equals(userId) || !repository.equals(repositoryId)
                || requestedSnapshot != null && !requestedSnapshot.equals(snapshot.id())) {
                throw new AnalysisNotFoundException();
            }
            return snapshot;
        }, (event, owner, resource, at) -> persistence.auditEvents.add(event), Clock.fixed(NOW, ZoneOffset.UTC));

        AnalysisJobView first = service.request(userId, repositoryId, null, "REPOSITORY_BASELINE", "analysis-key");
        AnalysisJobView repeated = service.request(userId, repositoryId, null, "REPOSITORY_BASELINE", "analysis-key");
        AnalysisJobView active = service.request(userId, repositoryId, snapshot.id(), "REPOSITORY_BASELINE", "other-key");

        assertThat(repeated.jobId()).isEqualTo(first.jobId());
        assertThat(active.jobId()).isEqualTo(first.jobId());
        assertThat(persistence.jobs).hasSize(1);

        AnalysisWorkItem work = service.claim(NOW).orElseThrow();
        UUID evaluationId = UUID.randomUUID();
        UUID matrixId = UUID.randomUUID();
        service.complete(work, evaluationId, matrixId, NOW.plusSeconds(2));

        AnalysisJobView completed = service.getJob(userId, first.jobId());
        assertThat(completed.status()).isEqualTo("succeeded");
        UUID analysisId = UUID.fromString(completed.resultResourceUrl().substring(completed.resultResourceUrl().lastIndexOf('/') + 1));
        AnalysisResultView result = service.getResult(userId, analysisId);
        assertThat(result.evaluationId()).isEqualTo(evaluationId);
        assertThat(result.skillMatrixId()).isEqualTo(matrixId);
        assertThat(result.currentForRepository()).isTrue();
        AnalysisJobView completedRepeat = service.request(
            userId, repositoryId, snapshot.id(), "REPOSITORY_BASELINE", "completed-repeat-key"
        );
        assertThat(completedRepeat.jobId()).isEqualTo(first.jobId());
        assertThat(persistence.jobs).hasSize(1);
        assertThat(persistence.results).hasSize(1);
        assertThatThrownBy(() -> service.getResult(UUID.randomUUID(), analysisId))
            .isInstanceOf(AnalysisNotFoundException.class);
        assertThat(persistence.events).containsExactly("AnalysisRequested", "AnalysisCompleted");

        AnalysisHistoryView history = service.listHistory(userId, 20, null);
        assertThat(history.totalCount()).isEqualTo(1);
        assertThat(history.analyses()).extracting(AnalysisHistoryItemView::analysisId).containsExactly(analysisId);
        assertThat(persistence.auditEvents).containsExactly(
            AnalysisAuditEvent.ANALYSIS_RESULT_VIEWED, AnalysisAuditEvent.ANALYSIS_HISTORY_VIEWED
        );
    }

    @Test
    void rejectsReuseOfAnIdempotencyKeyForAnotherRepository() {
        UUID userId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        RepositorySnapshot snapshot = snapshot(userId, repositoryId);
        var service = new AnalysisApplicationService(new InMemoryPersistence(), (owner, repository, id) -> snapshot,
            (event, owner, resource, at) -> {}, Clock.fixed(NOW, ZoneOffset.UTC));
        service.request(userId, repositoryId, null, null, "same-key");

        assertThatThrownBy(() -> service.request(userId, UUID.randomUUID(), null, null, "same-key"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static RepositorySnapshot snapshot(UUID userId, UUID repositoryId) {
        String revision = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        return RepositorySnapshot.ready(repositoryId, userId, revision, NOW,
            List.of(new RepositoryBranch("main", revision, true)),
            List.of(new RepositoryCommit(revision, "owner", NOW.minusSeconds(60), "change")));
    }

    private static final class InMemoryPersistence implements AnalysisPersistencePort {
        private final List<AnalysisJob> jobs = new ArrayList<>();
        private final List<CompletedAnalysis> results = new ArrayList<>();
        private final List<String> events = new ArrayList<>();
        private final List<AnalysisAuditEvent> auditEvents = new ArrayList<>();

        public Optional<AnalysisJob> findByOwnerAndIdempotencyKey(UUID userId, String key) {
            return jobs.stream().filter(job -> job.userId().equals(userId) && job.idempotencyKey().equals(key)).findFirst();
        }
        public Optional<AnalysisJob> findActiveByBasis(UUID userId, UUID snapshotId, String scope) {
            return jobs.stream().filter(job -> job.userId().equals(userId) && job.snapshotId().equals(snapshotId)
                && job.analysisScope().equals(scope) && (job.status() == AnalysisJobStatus.QUEUED
                || job.status() == AnalysisJobStatus.RUNNING)).findFirst();
        }
        public Optional<AnalysisJob> findByIdAndOwner(UUID jobId, UUID userId) {
            return jobs.stream().filter(job -> job.id().equals(jobId) && job.userId().equals(userId)).findFirst();
        }
        public Optional<AnalysisJob> findNextClaimable(Instant now) {
            return jobs.stream().filter(job -> job.status() == AnalysisJobStatus.QUEUED
                && !job.nextAttemptAt().isAfter(now)).findFirst();
        }
        public Optional<CompletedAnalysis> findReusableResult(UUID userId, UUID snapshotId, String scope) {
            return results.stream().filter(value -> value.userId().equals(userId) && value.snapshotId().equals(snapshotId)
                && value.analysisScope().equals(scope)).findFirst();
        }
        public AnalysisJob saveJob(AnalysisJob job) {
            jobs.removeIf(existing -> existing.id().equals(job.id())); jobs.add(job); return job;
        }
        public CompletedAnalysis saveResult(CompletedAnalysis analysis) { results.add(analysis); return analysis; }
        public Optional<CompletedAnalysis> findResultByIdAndOwner(UUID id, UUID userId) {
            return results.stream().filter(value -> value.id().equals(id) && value.userId().equals(userId)).findFirst();
        }
        public boolean isCurrentForRepository(UUID userId, UUID repositoryId, UUID analysisId) {
            return results.stream().filter(value -> value.userId().equals(userId)
                && value.repositoryId().equals(repositoryId)).max(java.util.Comparator.comparing(CompletedAnalysis::completedAt)
                .thenComparing(CompletedAnalysis::id)).map(value -> value.id().equals(analysisId)).orElse(false);
        }
        public List<AnalysisHistoryItemView> findHistoryByOwner(UUID userId, int page, int limit) {
            return results.stream().filter(value -> value.userId().equals(userId)).map(this::history).toList();
        }
        public List<AnalysisHistoryItemView> findHistoryByOwnerAndRepository(
            UUID userId, UUID repositoryId, int page, int limit
        ) {
            return results.stream().filter(value -> value.userId().equals(userId)
                && value.repositoryId().equals(repositoryId)).map(this::history).toList();
        }
        public long countHistoryByOwner(UUID userId) {
            return results.stream().filter(value -> value.userId().equals(userId)).count();
        }
        public long countHistoryByOwnerAndRepository(UUID userId, UUID repositoryId) {
            return results.stream().filter(value -> value.userId().equals(userId)
                && value.repositoryId().equals(repositoryId)).count();
        }
        public void appendOutbox(String aggregateType, UUID id, String eventType, String payload, Instant occurredAt) {
            events.add(eventType);
        }
        private AnalysisHistoryItemView history(CompletedAnalysis value) {
            return new AnalysisHistoryItemView(value.id(), value.repositoryId(), "owner/devpath", value.snapshotId(),
                value.evaluationId(), value.skillMatrixId(), value.analysisScope(),
                new java.math.BigDecimal("75.00"), new java.math.BigDecimal("90.00"),
                "baseline-v1", "skill-matrix-v1", true, value.completedAt());
        }
    }
}
