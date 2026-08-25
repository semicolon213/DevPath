package com.devpath.analysis.application;

import com.devpath.analysis.domain.AnalysisJob;
import com.devpath.analysis.domain.CompletedAnalysis;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface AnalysisPersistencePort {
    Optional<AnalysisJob> findByOwnerAndIdempotencyKey(UUID userId, String idempotencyKey);
    Optional<AnalysisJob> findActiveByBasis(UUID userId, UUID snapshotId, String analysisScope);
    Optional<AnalysisJob> findByIdAndOwner(UUID jobId, UUID userId);
    Optional<AnalysisJob> findNextClaimable(Instant now);
    List<AnalysisJob> findRecentJobsByOwner(UUID userId, int limit);
    Optional<CompletedAnalysis> findReusableResult(UUID userId, UUID snapshotId, String analysisScope);
    AnalysisJob saveJob(AnalysisJob job);
    CompletedAnalysis saveResult(CompletedAnalysis analysis);
    Optional<CompletedAnalysis> findResultByIdAndOwner(UUID analysisId, UUID userId);
    boolean isCurrentForRepository(UUID userId, UUID repositoryId, UUID analysisId);
    List<AnalysisHistoryItemView> findHistoryByOwner(UUID userId, int page, int limit);
    List<AnalysisHistoryItemView> findHistoryByOwnerAndRepository(UUID userId, UUID repositoryId, int page, int limit);
    List<AnalysisHistoryItemView> findHistoryByOwnerAndIds(UUID userId, List<UUID> analysisIds);
    long countHistoryByOwner(UUID userId);
    long countHistoryByOwnerAndRepository(UUID userId, UUID repositoryId);
    void appendOutbox(String aggregateType, UUID aggregateId, String eventType, String payload, Instant occurredAt);
}
