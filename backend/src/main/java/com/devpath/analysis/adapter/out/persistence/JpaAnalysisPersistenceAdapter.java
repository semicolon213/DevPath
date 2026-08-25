package com.devpath.analysis.adapter.out.persistence;

import com.devpath.analysis.application.AnalysisPersistencePort;
import com.devpath.analysis.application.AnalysisHistoryItemView;
import com.devpath.analysis.domain.AnalysisJob;
import com.devpath.analysis.domain.CompletedAnalysis;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@org.springframework.stereotype.Repository
class JpaAnalysisPersistenceAdapter implements AnalysisPersistencePort {
    private final AnalysisJobJpaRepository jobs;
    private final AnalysisResultJpaRepository results;
    private final AnalysisOutboxJpaRepository outbox;

    JpaAnalysisPersistenceAdapter(
        AnalysisJobJpaRepository jobs, AnalysisResultJpaRepository results, AnalysisOutboxJpaRepository outbox
    ) {
        this.jobs = jobs; this.results = results; this.outbox = outbox;
    }

    public Optional<AnalysisJob> findByOwnerAndIdempotencyKey(UUID userId, String key) {
        return jobs.findByUserIdAndIdempotencyKey(userId, key).map(AnalysisJobJpaEntity::toDomain);
    }
    public Optional<AnalysisJob> findActiveByBasis(UUID userId, UUID snapshotId, String scope) {
        return jobs.findFirstByUserIdAndSnapshotIdAndAnalysisScopeAndStatusIn(
            userId, snapshotId, scope, List.of("QUEUED", "RUNNING")
        ).map(AnalysisJobJpaEntity::toDomain);
    }
    public Optional<AnalysisJob> findByIdAndOwner(UUID jobId, UUID userId) {
        return jobs.findByIdAndUserId(jobId, userId).map(AnalysisJobJpaEntity::toDomain);
    }
    public Optional<AnalysisJob> findNextClaimable(Instant now) {
        return jobs.findClaimable(now, PageRequest.of(0, 1)).stream().findFirst().map(AnalysisJobJpaEntity::toDomain);
    }
    public List<AnalysisJob> findRecentJobsByOwner(UUID userId, int limit) {
        return jobs.findAllByUserIdOrderBySubmittedAtDescIdDesc(userId, PageRequest.of(0, limit)).stream()
            .map(AnalysisJobJpaEntity::toDomain).toList();
    }
    public Optional<CompletedAnalysis> findReusableResult(UUID userId, UUID snapshotId, String scope) {
        return results.findReusableResult(userId, snapshotId, scope, PageRequest.of(0, 1)).stream().findFirst()
            .map(AnalysisResultJpaEntity::toDomain);
    }
    public AnalysisJob saveJob(AnalysisJob job) {
        return jobs.saveAndFlush(new AnalysisJobJpaEntity(job)).toDomain();
    }
    public CompletedAnalysis saveResult(CompletedAnalysis analysis) {
        return results.saveAndFlush(new AnalysisResultJpaEntity(analysis)).toDomain();
    }
    public Optional<CompletedAnalysis> findResultByIdAndOwner(UUID analysisId, UUID userId) {
        return results.findByIdAndUserId(analysisId, userId).map(AnalysisResultJpaEntity::toDomain);
    }
    public boolean isCurrentForRepository(UUID userId, UUID repositoryId, UUID analysisId) {
        return results.findFirstByUserIdAndRepositoryIdOrderByCompletedAtDescIdDesc(userId, repositoryId)
            .map(value -> value.toDomain().id().equals(analysisId)).orElse(false);
    }
    public List<AnalysisHistoryItemView> findHistoryByOwner(UUID userId, int page, int limit) {
        return results.findHistoryByOwner(userId, PageRequest.of(page, limit)).stream().map(this::toView).toList();
    }
    public List<AnalysisHistoryItemView> findHistoryByOwnerAndRepository(
        UUID userId, UUID repositoryId, int page, int limit
    ) {
        return results.findHistoryByOwnerAndRepository(userId, repositoryId, PageRequest.of(page, limit)).stream()
            .map(this::toView).toList();
    }
    public List<AnalysisHistoryItemView> findHistoryByOwnerAndIds(UUID userId, List<UUID> analysisIds) {
        return results.findHistoryByOwnerAndIds(userId, analysisIds).stream().map(this::toView).toList();
    }
    public long countHistoryByOwner(UUID userId) { return results.countByUserId(userId); }
    public long countHistoryByOwnerAndRepository(UUID userId, UUID repositoryId) {
        return results.countByUserIdAndRepositoryId(userId, repositoryId);
    }
    public void appendOutbox(String type, UUID id, String eventType, String payload, Instant occurredAt) {
        outbox.save(new AnalysisOutboxJpaEntity(type, id, eventType, payload, occurredAt));
    }
    private AnalysisHistoryItemView toView(AnalysisHistoryProjection value) {
        return new AnalysisHistoryItemView(value.getAnalysisId(), value.getRepositoryId(),
            value.getRepositoryFullName(), value.getSnapshotId(), value.getEvaluationId(), value.getSkillMatrixId(),
            value.getAnalysisScope(), value.getOverallScore(), value.getConfidence(), value.getRuleSetVersion(),
            value.getPolicyVersion(), value.getCurrentForRepository(), value.getCompletedAt());
    }
}
