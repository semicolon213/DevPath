package com.devpath.rule.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RuleEvaluationJpaRepository extends JpaRepository<RuleEvaluationJpaEntity, UUID> {
    Optional<RuleEvaluationJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<RuleEvaluationJpaEntity> findByUserIdAndSnapshotIdAndRuleSetVersionIdAndInputHash(
        UUID userId, UUID snapshotId, UUID ruleSetVersionId, String inputHash
    );
    @Query(value = "SELECT s.repository_id FROM evaluations e JOIN repository_snapshots s ON s.snapshot_id=e.snapshot_id AND s.user_id=e.user_id WHERE e.evaluation_id=?1 AND e.user_id=?2", nativeQuery = true)
    Optional<UUID> findRepositoryIdByEvaluationIdAndUserId(UUID evaluationId, UUID userId);
}
interface EvaluationWarningJpaRepository extends JpaRepository<EvaluationWarningJpaEntity, UUID> {
    List<EvaluationWarningJpaEntity> findAllByEvaluationIdOrderByOrderIndexAsc(UUID evaluationId);
}
interface RuleCategoryEvaluationJpaRepository extends JpaRepository<RuleCategoryEvaluationJpaEntity, UUID> {
    List<RuleCategoryEvaluationJpaEntity> findAllByEvaluationIdOrderByCategoryAsc(UUID evaluationId);
}
interface CategoryMissingEvidenceJpaRepository extends JpaRepository<CategoryMissingEvidenceJpaEntity, UUID> {
    List<CategoryMissingEvidenceJpaEntity> findAllByCategoryEvaluationIdOrderByOrderIndexAsc(UUID categoryEvaluationId);
}
interface RuleExecutionResultJpaRepository extends JpaRepository<RuleExecutionResultJpaEntity, UUID> {
    List<RuleExecutionResultJpaEntity> findAllByEvaluationIdOrderByCategoryAscRuleIdAsc(UUID evaluationId);
}
interface RuleEvidenceJpaRepository extends JpaRepository<RuleEvidenceJpaEntity, UUID> {
    Optional<RuleEvidenceJpaEntity> findByUserIdAndSnapshotIdAndSourceReferenceHash(UUID userId, UUID snapshotId, String hash);
    List<RuleEvidenceJpaEntity> findAllByIdInAndUserIdOrderByIdAsc(List<UUID> ids, UUID userId);
}
interface ScoreEvidenceLinkJpaRepository extends JpaRepository<ScoreEvidenceLinkJpaEntity, UUID> {
    List<ScoreEvidenceLinkJpaEntity> findAllByEvaluationIdOrderByRuleExecutionResultIdAscEvidenceIdAsc(UUID evaluationId);
}
