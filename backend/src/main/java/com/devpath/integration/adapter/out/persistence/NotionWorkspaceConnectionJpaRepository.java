package com.devpath.integration.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotionWorkspaceConnectionJpaRepository extends JpaRepository<NotionWorkspaceConnectionJpaEntity, UUID> {
    Optional<NotionWorkspaceConnectionJpaEntity> findByUserId(UUID userId);
    Optional<NotionWorkspaceConnectionJpaEntity> findFirstByUserIdAndStatusOrderByConnectedAtAsc(UUID userId, String status);
    List<NotionWorkspaceConnectionJpaEntity> findAllByUserIdOrderByConnectedAtAsc(UUID userId);
}
