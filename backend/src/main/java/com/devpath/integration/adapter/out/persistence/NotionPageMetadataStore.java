package com.devpath.integration.adapter.out.persistence;

import com.devpath.integration.application.NotionWorkspacePageView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotionPageMetadataStore {
    private final NotionPageMetadataJpaRepository repository;

    public NotionPageMetadataStore(NotionPageMetadataJpaRepository repository) { this.repository = repository; }

    @Transactional
    public void replace(UUID connectionId, UUID userId, List<NotionWorkspacePageView> pages, Instant discoveredAt) {
        repository.deleteAllByConnectionId(connectionId);
        repository.flush();
        repository.saveAll(pages.stream().map(page -> new NotionPageMetadataJpaEntity(
            UUID.randomUUID(), connectionId, userId, page.providerPageId(), page.objectType(), page.title(),
            page.url(), page.lastEditedAt(), page.inTrash(), discoveredAt)).toList());
    }

    @Transactional
    public void delete(UUID connectionId) {
        repository.deleteAllByConnectionId(connectionId);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<OwnedNotionPage> findOwned(UUID userId, String providerPageId) {
        return repository.findFirstByUserIdAndProviderPageIdAndInTrashFalse(userId, providerPageId)
            .map(entity -> new OwnedNotionPage(entity.connectionId(), entity.toView()));
    }

    public record OwnedNotionPage(UUID connectionId, NotionWorkspacePageView page) {}
}
