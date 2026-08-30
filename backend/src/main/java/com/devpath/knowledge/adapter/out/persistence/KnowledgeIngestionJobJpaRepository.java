package com.devpath.knowledge.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface KnowledgeIngestionJobJpaRepository extends JpaRepository<KnowledgeIngestionJobJpaEntity, UUID> {
    Optional<KnowledgeIngestionJobJpaEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    Optional<KnowledgeIngestionJobJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<KnowledgeIngestionJobJpaEntity> findFirstByUserIdAndSourceObjectIdAndStatusInOrderBySubmittedAtDesc(
        UUID userId, String sourceObjectId, java.util.Collection<String> statuses
    );
    @Query(value = "SELECT * FROM knowledge_ingestion_jobs WHERE " +
        "((status = 'QUEUED' AND next_attempt_at <= :now) OR (status = 'RUNNING' AND next_attempt_at <= :now)) " +
        "ORDER BY submitted_at FOR UPDATE SKIP LOCKED LIMIT 1", nativeQuery = true)
    Optional<KnowledgeIngestionJobJpaEntity> findNextClaimable(Instant now);
}
