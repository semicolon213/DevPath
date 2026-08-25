package com.devpath.analysis.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AnalysisJobJpaRepository extends JpaRepository<AnalysisJobJpaEntity, UUID> {
    Optional<AnalysisJobJpaEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    Optional<AnalysisJobJpaEntity> findFirstByUserIdAndSnapshotIdAndAnalysisScopeAndStatusIn(
        UUID userId, UUID snapshotId, String scope, List<String> statuses
    );
    Optional<AnalysisJobJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    List<AnalysisJobJpaEntity> findAllByUserIdOrderBySubmittedAtDescIdDesc(UUID userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from AnalysisJobJpaEntity job where job.status = 'QUEUED' and job.nextAttemptAt <= :now order by job.submittedAt")
    List<AnalysisJobJpaEntity> findClaimable(@Param("now") Instant now, Pageable pageable);
}

interface AnalysisResultJpaRepository extends JpaRepository<AnalysisResultJpaEntity, UUID> {
    Optional<AnalysisResultJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<AnalysisResultJpaEntity> findFirstByUserIdAndRepositoryIdOrderByCompletedAtDescIdDesc(
        UUID userId, UUID repositoryId
    );

    @Query(value = """
        SELECT ar.* FROM analysis_results ar
        JOIN evaluations e ON e.evaluation_id = ar.evaluation_id AND e.user_id = ar.user_id
        JOIN rule_set_versions rsv ON rsv.rule_set_version_id = e.rule_set_version_id
        WHERE ar.user_id = :userId AND ar.snapshot_id = :snapshotId
          AND ar.analysis_scope = :analysisScope AND rsv.status = 'ACTIVE'
        ORDER BY ar.completed_at DESC, ar.analysis_id DESC
        """, nativeQuery = true)
    List<AnalysisResultJpaEntity> findReusableResult(
        @Param("userId") UUID userId,
        @Param("snapshotId") UUID snapshotId,
        @Param("analysisScope") String analysisScope,
        Pageable pageable
    );

    @Query(value = """
        SELECT ar.analysis_id AS "analysisId", ar.repository_id AS "repositoryId",
               r.full_name AS "repositoryFullName", ar.snapshot_id AS "snapshotId",
               ar.evaluation_id AS "evaluationId", ar.skill_matrix_id AS "skillMatrixId",
               ar.analysis_scope AS "analysisScope", e.overall_score AS "overallScore",
               e.confidence AS "confidence", e.rule_set_version_label AS "ruleSetVersion",
               sm.policy_version AS "policyVersion",
               NOT EXISTS (
                   SELECT 1 FROM analysis_results newer
                   WHERE newer.user_id = ar.user_id AND newer.repository_id = ar.repository_id
                     AND (newer.completed_at > ar.completed_at
                       OR (newer.completed_at = ar.completed_at AND newer.analysis_id > ar.analysis_id))
               ) AS "currentForRepository", ar.completed_at AS "completedAt"
        FROM analysis_results ar
        JOIN repositories r ON r.repository_id = ar.repository_id AND r.user_id = ar.user_id
        JOIN evaluations e ON e.evaluation_id = ar.evaluation_id AND e.user_id = ar.user_id
        JOIN skill_matrices sm ON sm.skill_matrix_id = ar.skill_matrix_id AND sm.user_id = ar.user_id
        WHERE ar.user_id = :userId
        ORDER BY ar.completed_at DESC, ar.analysis_id DESC
        """, nativeQuery = true)
    List<AnalysisHistoryProjection> findHistoryByOwner(
        @Param("userId") UUID userId, Pageable pageable
    );

    @Query(value = """
        SELECT ar.analysis_id AS "analysisId", ar.repository_id AS "repositoryId",
               r.full_name AS "repositoryFullName", ar.snapshot_id AS "snapshotId",
               ar.evaluation_id AS "evaluationId", ar.skill_matrix_id AS "skillMatrixId",
               ar.analysis_scope AS "analysisScope", e.overall_score AS "overallScore",
               e.confidence AS "confidence", e.rule_set_version_label AS "ruleSetVersion",
               sm.policy_version AS "policyVersion",
               NOT EXISTS (
                   SELECT 1 FROM analysis_results newer
                   WHERE newer.user_id = ar.user_id AND newer.repository_id = ar.repository_id
                     AND (newer.completed_at > ar.completed_at
                       OR (newer.completed_at = ar.completed_at AND newer.analysis_id > ar.analysis_id))
               ) AS "currentForRepository", ar.completed_at AS "completedAt"
        FROM analysis_results ar
        JOIN repositories r ON r.repository_id = ar.repository_id AND r.user_id = ar.user_id
        JOIN evaluations e ON e.evaluation_id = ar.evaluation_id AND e.user_id = ar.user_id
        JOIN skill_matrices sm ON sm.skill_matrix_id = ar.skill_matrix_id AND sm.user_id = ar.user_id
        WHERE ar.user_id = :userId AND ar.repository_id = :repositoryId
        ORDER BY ar.completed_at DESC, ar.analysis_id DESC
        """, nativeQuery = true)
    List<AnalysisHistoryProjection> findHistoryByOwnerAndRepository(
        @Param("userId") UUID userId, @Param("repositoryId") UUID repositoryId, Pageable pageable
    );

    @Query(value = """
        SELECT ar.analysis_id AS "analysisId", ar.repository_id AS "repositoryId",
               r.full_name AS "repositoryFullName", ar.snapshot_id AS "snapshotId",
               ar.evaluation_id AS "evaluationId", ar.skill_matrix_id AS "skillMatrixId",
               ar.analysis_scope AS "analysisScope", e.overall_score AS "overallScore",
               e.confidence AS "confidence", e.rule_set_version_label AS "ruleSetVersion",
               sm.policy_version AS "policyVersion",
               NOT EXISTS (
                   SELECT 1 FROM analysis_results newer
                   WHERE newer.user_id = ar.user_id AND newer.repository_id = ar.repository_id
                     AND (newer.completed_at > ar.completed_at
                       OR (newer.completed_at = ar.completed_at AND newer.analysis_id > ar.analysis_id))
               ) AS "currentForRepository", ar.completed_at AS "completedAt"
        FROM analysis_results ar
        JOIN repositories r ON r.repository_id = ar.repository_id AND r.user_id = ar.user_id
        JOIN evaluations e ON e.evaluation_id = ar.evaluation_id AND e.user_id = ar.user_id
        JOIN skill_matrices sm ON sm.skill_matrix_id = ar.skill_matrix_id AND sm.user_id = ar.user_id
        WHERE ar.user_id = :userId AND ar.analysis_id IN (:analysisIds)
        """, nativeQuery = true)
    List<AnalysisHistoryProjection> findHistoryByOwnerAndIds(
        @Param("userId") UUID userId, @Param("analysisIds") List<UUID> analysisIds
    );

    long countByUserId(UUID userId);
    long countByUserIdAndRepositoryId(UUID userId, UUID repositoryId);
}

interface AnalysisHistoryProjection {
    UUID getAnalysisId();
    UUID getRepositoryId();
    String getRepositoryFullName();
    UUID getSnapshotId();
    UUID getEvaluationId();
    UUID getSkillMatrixId();
    String getAnalysisScope();
    BigDecimal getOverallScore();
    BigDecimal getConfidence();
    String getRuleSetVersion();
    String getPolicyVersion();
    boolean getCurrentForRepository();
    Instant getCompletedAt();
}

interface AnalysisOutboxJpaRepository extends JpaRepository<AnalysisOutboxJpaEntity, UUID> {}
