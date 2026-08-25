package com.devpath.analysis.application;

import com.devpath.analysis.domain.AnalysisJob;
import com.devpath.analysis.domain.AnalysisJobStatus;
import com.devpath.analysis.domain.CompletedAnalysis;
import java.time.Clock;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisApplicationService {
    public static final String REPOSITORY_BASELINE = "REPOSITORY_BASELINE";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final AnalysisPersistencePort persistence;
    private final AnalysisSourcePort sources;
    private final AnalysisAuditPort audit;
    private final Clock clock;

    public AnalysisApplicationService(
        AnalysisPersistencePort persistence, AnalysisSourcePort sources, AnalysisAuditPort audit, Clock clock
    ) {
        this.persistence = persistence;
        this.sources = sources;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public AnalysisJobView request(
        UUID userId, UUID repositoryId, UUID snapshotId, String analysisScope, String idempotencyKey
    ) {
        String scope = analysisScope == null || analysisScope.isBlank() ? REPOSITORY_BASELINE : analysisScope;
        validate(idempotencyKey, scope);
        Optional<AnalysisJob> repeated = persistence.findByOwnerAndIdempotencyKey(userId, idempotencyKey);
        if (repeated.isPresent()) {
            AnalysisJob existing = repeated.get();
            if (!existing.repositoryId().equals(repositoryId) || !existing.analysisScope().equals(scope)
                || snapshotId != null && !existing.snapshotId().equals(snapshotId)) {
                throw new IllegalArgumentException("Idempotency key belongs to another analysis command");
            }
            return AnalysisJobView.from(existing);
        }

        var snapshot = sources.resolveOwnedSnapshot(userId, repositoryId, snapshotId);
        Optional<CompletedAnalysis> reusable = persistence.findReusableResult(userId, snapshot.id(), scope);
        if (reusable.isPresent()) {
            return persistence.findByIdAndOwner(reusable.get().jobId(), userId).map(AnalysisJobView::from)
                .orElseThrow(AnalysisNotFoundException::new);
        }
        Optional<AnalysisJob> active = persistence.findActiveByBasis(userId, snapshot.id(), scope);
        if (active.isPresent()) return AnalysisJobView.from(active.get());

        Instant now = clock.instant();
        AnalysisJob job = persistence.saveJob(
            AnalysisJob.queue(userId, repositoryId, snapshot.id(), idempotencyKey, scope, now)
        );
        persistence.appendOutbox("ANALYSIS_JOB", job.id(), "AnalysisRequested",
            "{\"jobId\":\"" + job.id() + "\",\"snapshotId\":\"" + snapshot.id() + "\"}", now);
        return AnalysisJobView.from(job);
    }

    @Transactional(readOnly = true)
    public AnalysisJobView getJob(UUID userId, UUID jobId) {
        return persistence.findByIdAndOwner(jobId, userId).map(AnalysisJobView::from)
            .orElseThrow(AnalysisNotFoundException::new);
    }

    @Transactional
    public AnalysisResultView getResult(UUID userId, UUID analysisId) {
        CompletedAnalysis analysis = persistence.findResultByIdAndOwner(analysisId, userId)
            .orElseThrow(AnalysisNotFoundException::new);
        AnalysisResultView result = AnalysisResultView.from(analysis,
            persistence.isCurrentForRepository(userId, analysis.repositoryId(), analysis.id()));
        audit.record(AnalysisAuditEvent.ANALYSIS_RESULT_VIEWED, userId, analysisId.toString(), clock.instant());
        return result;
    }

    @Transactional
    public AnalysisHistoryView listHistory(UUID userId, Integer requestedLimit, String cursor) {
        PageRequest page = page(requestedLimit, cursor);
        long total = persistence.countHistoryByOwner(userId);
        var analyses = persistence.findHistoryByOwner(userId, page.number(), page.limit());
        AnalysisHistoryView history = history(analyses, page, total);
        audit.record(AnalysisAuditEvent.ANALYSIS_HISTORY_VIEWED, userId, "ALL", clock.instant());
        return history;
    }

    @Transactional
    public AnalysisHistoryView listRepositoryHistory(
        UUID userId, UUID repositoryId, Integer requestedLimit, String cursor
    ) {
        sources.verifyOwnedRepository(userId, repositoryId);
        PageRequest page = page(requestedLimit, cursor);
        long total = persistence.countHistoryByOwnerAndRepository(userId, repositoryId);
        var analyses = persistence.findHistoryByOwnerAndRepository(userId, repositoryId, page.number(), page.limit());
        AnalysisHistoryView history = history(analyses, page, total);
        audit.record(AnalysisAuditEvent.ANALYSIS_HISTORY_VIEWED, userId, repositoryId.toString(), clock.instant());
        return history;
    }

    @Transactional
    public AnalysisComparisonView compare(UUID userId, List<UUID> analysisIds) {
        if (analysisIds == null || analysisIds.size() != 2 || analysisIds.get(0).equals(analysisIds.get(1))) {
            throw new IllegalArgumentException("Exactly two distinct analyses are required");
        }
        var owned = persistence.findHistoryByOwnerAndIds(userId, analysisIds);
        if (owned.size() != 2) throw new AnalysisNotFoundException();
        var byId = owned.stream().collect(java.util.stream.Collectors.toMap(AnalysisHistoryItemView::analysisId, item -> item));
        var ordered = analysisIds.stream().map(byId::get).toList();
        if (ordered.stream().anyMatch(java.util.Objects::isNull)) throw new AnalysisNotFoundException();
        audit.record(AnalysisAuditEvent.ANALYSES_COMPARED, userId,
            analysisIds.get(0) + ":" + analysisIds.get(1), clock.instant());
        return new AnalysisComparisonView(ordered);
    }

    @Transactional
    Optional<AnalysisWorkItem> claim(Instant now) {
        return persistence.findNextClaimable(now).map(job -> {
            AnalysisJob running = persistence.saveJob(job.start(now));
            return new AnalysisWorkItem(running,
                sources.resolveOwnedSnapshot(running.userId(), running.repositoryId(), running.snapshotId()));
        });
    }

    @Transactional
    void complete(AnalysisWorkItem item, UUID evaluationId, UUID skillMatrixId, Instant now) {
        AnalysisJob current = persistence.findByIdAndOwner(item.job().id(), item.job().userId())
            .orElseThrow(AnalysisNotFoundException::new);
        if (current.status() != AnalysisJobStatus.RUNNING) return;
        CompletedAnalysis result = persistence.saveResult(new CompletedAnalysis(
            UUID.randomUUID(), current.id(), current.userId(), current.repositoryId(), current.snapshotId(),
            evaluationId, skillMatrixId, current.analysisScope(), now
        ));
        persistence.saveJob(current.succeed(result.id(), now));
        persistence.appendOutbox("ANALYSIS", result.id(), "AnalysisCompleted",
            "{\"analysisId\":\"" + result.id() + "\",\"evaluationId\":\"" + evaluationId
                + "\",\"skillMatrixId\":\"" + skillMatrixId + "\"}", now);
    }

    @Transactional
    void fail(AnalysisWorkItem item, String errorCode, String safeMessage, Instant now) {
        AnalysisJob current = persistence.findByIdAndOwner(item.job().id(), item.job().userId())
            .orElseThrow(AnalysisNotFoundException::new);
        AnalysisJob failed = persistence.saveJob(current.failOrRetry(errorCode, safeMessage, now));
        if (failed.status() == AnalysisJobStatus.FAILED) {
            persistence.appendOutbox("ANALYSIS_JOB", failed.id(), "AnalysisFailed",
                "{\"jobId\":\"" + failed.id() + "\",\"errorCode\":\"" + errorCode + "\"}", now);
        }
    }

    private static void validate(String key, String scope) {
        if (key == null || key.isBlank() || key.length() > 128 || !REPOSITORY_BASELINE.equals(scope)) {
            throw new IllegalArgumentException("Analysis request is invalid");
        }
    }

    private static PageRequest page(Integer requestedLimit, String cursor) {
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_LIMIT) throw new IllegalArgumentException("Analysis page limit is invalid");
        int offset = decodeCursor(cursor);
        if (offset % limit != 0) throw new IllegalArgumentException("Analysis cursor does not match the page limit");
        return new PageRequest(offset / limit, limit, offset);
    }

    private static AnalysisHistoryView history(
        java.util.List<AnalysisHistoryItemView> analyses, PageRequest page, long total
    ) {
        int nextOffset = page.offset() + analyses.size();
        String nextCursor = nextOffset < total ? Base64.getUrlEncoder().withoutPadding()
            .encodeToString(Integer.toString(nextOffset).getBytes(StandardCharsets.UTF_8)) : null;
        return new AnalysisHistoryView(analyses, page.limit(), nextCursor, total);
    }

    private static int decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0;
        try {
            int offset = Integer.parseInt(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
            if (offset < 0) throw new IllegalArgumentException("Analysis cursor is invalid");
            return offset;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Analysis cursor is invalid", exception);
        }
    }

    private record PageRequest(int number, int limit, int offset) {}
}
