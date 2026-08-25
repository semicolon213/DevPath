package com.devpath.repository.application;

import com.devpath.repository.domain.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RepositoryRegistrationTransaction {
    private final RepositoryPersistencePort repositories;
    private final RepositoryAuditPort audit;

    RepositoryRegistrationTransaction(RepositoryPersistencePort repositories, RepositoryAuditPort audit) {
        this.repositories = repositories;
        this.audit = audit;
    }

    @Transactional
    RepositoryView register(Repository candidate, Instant now) {
        return repositories.findByOwnerAndProviderReference(
                candidate.userId(), candidate.provider(), candidate.providerRepositoryId()
            )
            .map(RepositoryView::from)
            .orElseGet(() -> {
                Repository saved = repositories.save(candidate);
                audit.record(RepositoryAuditEvent.REPOSITORY_IMPORTED, saved.userId(), saved.id(), now);
                return RepositoryView.from(saved);
            });
    }

    @Transactional(readOnly = true)
    List<Repository> findPage(UUID userId, int page, int limit, boolean includeArchived) {
        return repositories.findPageByOwner(userId, page, limit, includeArchived);
    }

    @Transactional(readOnly = true)
    long count(UUID userId, boolean includeArchived) {
        return repositories.countByOwner(userId, includeArchived);
    }

    @Transactional(readOnly = true)
    RepositoryView get(UUID userId, UUID repositoryId) {
        return repositories.findByIdAndOwner(repositoryId, userId)
            .map(RepositoryView::from)
            .orElseThrow(RepositoryNotFoundException::new);
    }

    @Transactional
    RepositoryView archive(UUID userId, UUID repositoryId, Instant now) {
        Repository current = owned(userId, repositoryId);
        Repository archived = current.archive(now);
        if (archived == current) {
            return RepositoryView.from(current);
        }
        Repository saved = repositories.save(archived);
        audit.record(RepositoryAuditEvent.REPOSITORY_ARCHIVED, userId, repositoryId, now);
        return RepositoryView.from(saved);
    }

    @Transactional
    RepositoryView restore(UUID userId, UUID repositoryId, Instant now) {
        Repository current = owned(userId, repositoryId);
        Repository restored = current.restore(now);
        if (restored == current) {
            return RepositoryView.from(current);
        }
        Repository saved = repositories.save(restored);
        audit.record(RepositoryAuditEvent.REPOSITORY_RESTORED, userId, repositoryId, now);
        return RepositoryView.from(saved);
    }

    private Repository owned(UUID userId, UUID repositoryId) {
        return repositories.findByIdAndOwner(repositoryId, userId)
            .orElseThrow(RepositoryNotFoundException::new);
    }
}
