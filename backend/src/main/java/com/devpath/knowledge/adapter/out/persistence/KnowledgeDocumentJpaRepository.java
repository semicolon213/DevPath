package com.devpath.knowledge.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface KnowledgeDocumentJpaRepository extends JpaRepository<KnowledgeDocumentJpaEntity, UUID> {
    Optional<KnowledgeDocumentJpaEntity> findByUserIdAndSourceTypeAndSourceObjectId(UUID userId, String sourceType, String sourceObjectId);
    Optional<KnowledgeDocumentJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    List<KnowledgeDocumentJpaEntity> findAllByUserIdOrderByUpdatedAtDescIdAsc(UUID userId);
}
