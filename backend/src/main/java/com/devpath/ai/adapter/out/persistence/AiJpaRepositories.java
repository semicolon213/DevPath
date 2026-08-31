package com.devpath.ai.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

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
    Optional<AiTaskJpaEntity> findFirstByStatusOrderByRequestedAtAsc(String status);
}

interface ModelExecutionJpaRepository extends JpaRepository<ModelExecutionJpaEntity, UUID> {}

interface AiResponseJpaRepository extends JpaRepository<AiResponseJpaEntity, UUID> {}

interface ResponseValidationResultJpaRepository extends JpaRepository<ResponseValidationResultJpaEntity, UUID> {
    Optional<ResponseValidationResultJpaEntity> findByResponseId(UUID responseId);
}

interface GeneratedArtifactJpaRepository extends JpaRepository<GeneratedArtifactJpaEntity, UUID> {
    Optional<GeneratedArtifactJpaEntity> findByPromptContextId(UUID promptContextId);
    Optional<GeneratedArtifactJpaEntity> findByIdAndUserId(UUID id, UUID userId);
}
