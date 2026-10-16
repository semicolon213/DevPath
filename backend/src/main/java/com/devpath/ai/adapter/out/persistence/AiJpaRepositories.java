package com.devpath.ai.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

interface PromptTemplateVersionJpaRepository extends JpaRepository<PromptTemplateVersionJpaEntity, UUID> {
    Optional<PromptTemplateVersionJpaEntity> findFirstByTaskTypeAndStatusOrderByCreatedAtDesc(
        String taskType, String status
    );
}

interface PromptContextJpaRepository extends JpaRepository<PromptContextJpaEntity, UUID> {}

interface AiTaskJpaRepository extends JpaRepository<AiTaskJpaEntity, UUID> {
    Optional<AiTaskJpaEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    Optional<AiTaskJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AiTaskJpaEntity> findWithLockByIdAndUserId(UUID id, UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select task from AiTaskJpaEntity task where task.nextAttemptAt <= :now and task.status in ('QUEUED', 'RUNNING') order by task.requestedAt")
    List<AiTaskJpaEntity> findClaimable(@Param("now") Instant now, Pageable pageable);
}

interface ModelExecutionJpaRepository extends JpaRepository<ModelExecutionJpaEntity, UUID> {}

interface AiResponseJpaRepository extends JpaRepository<AiResponseJpaEntity, UUID> {}

interface ResponseValidationResultJpaRepository extends JpaRepository<ResponseValidationResultJpaEntity, UUID> {
    Optional<ResponseValidationResultJpaEntity> findByResponseId(UUID responseId);
}

interface GeneratedArtifactJpaRepository extends JpaRepository<GeneratedArtifactJpaEntity, UUID> {
    Optional<GeneratedArtifactJpaEntity> findByPromptContextIdAndUserId(UUID promptContextId, UUID userId);
    Optional<GeneratedArtifactJpaEntity> findByIdAndUserId(UUID id, UUID userId);
}
