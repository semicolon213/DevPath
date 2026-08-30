package com.devpath.knowledge.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface KnowledgeChunkJpaRepository extends JpaRepository<KnowledgeChunkJpaEntity, UUID> {
    List<KnowledgeChunkJpaEntity> findAllByDocumentVersionIdOrderByPositionAsc(UUID documentVersionId);
    int countByDocumentVersionId(UUID documentVersionId);
}
