package com.devpath.integration.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotionPageMetadataJpaRepository extends JpaRepository<NotionPageMetadataJpaEntity, UUID> {
    void deleteAllByConnectionId(UUID connectionId);
}
