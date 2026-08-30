package com.devpath.knowledge.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface KnowledgeDocumentVersionJpaRepository extends JpaRepository<KnowledgeDocumentVersionJpaEntity, UUID> {
    Optional<KnowledgeDocumentVersionJpaEntity> findByIdAndDocumentId(UUID id, UUID documentId);
    @Query("select coalesce(max(v.versionNumber), 0) from KnowledgeDocumentVersionJpaEntity v where v.documentId = :documentId")
    int maxVersionNumber(UUID documentId);
}
