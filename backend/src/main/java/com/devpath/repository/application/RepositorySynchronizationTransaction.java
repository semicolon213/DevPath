package com.devpath.repository.application;

import com.devpath.integration.application.GitHubRepositorySnapshot;
import com.devpath.repository.domain.Repository;
import com.devpath.repository.domain.RepositoryBranch;
import com.devpath.repository.domain.RepositoryCommit;
import com.devpath.repository.domain.RepositoryDependency;
import com.devpath.repository.domain.RepositoryFile;
import com.devpath.repository.domain.RepositoryLifecycle;
import com.devpath.repository.domain.RepositoryLanguage;
import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.repository.domain.RepositorySyncJob;
import com.devpath.repository.domain.RepositorySyncJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RepositorySynchronizationTransaction {
    private final RepositoryPersistencePort repositories;
    private final RepositorySynchronizationPersistencePort synchronization;
    private final RepositoryAuditPort audit;

    RepositorySynchronizationTransaction(
        RepositoryPersistencePort repositories,
        RepositorySynchronizationPersistencePort synchronization,
        RepositoryAuditPort audit
    ) {
        this.repositories = repositories;
        this.synchronization = synchronization;
        this.audit = audit;
    }

    @Transactional
    RepositorySyncJobView request(UUID userId, UUID repositoryId, String idempotencyKey, Instant now) {
        Optional<RepositorySyncJob> repeated = synchronization.findByOwnerAndIdempotencyKey(userId, idempotencyKey);
        if (repeated.isPresent()) {
            if (!repeated.get().repositoryId().equals(repositoryId)) {
                throw new IllegalArgumentException("Idempotency key belongs to another repository command");
            }
            return RepositorySyncJobView.from(repeated.get());
        }
        Repository repository = owned(userId, repositoryId);
        if (repository.lifecycle() == RepositoryLifecycle.ARCHIVED
            || repository.lifecycle() == RepositoryLifecycle.DELETED_EXTERNALLY
            || repository.providerArchived()) {
            throw new IllegalStateException("Archived or deleted repositories cannot be synchronized");
        }
        Optional<RepositorySyncJob> active = synchronization.findActiveByRepository(repositoryId);
        if (active.isPresent()) {
            return RepositorySyncJobView.from(active.get());
        }
        RepositorySyncJob job = synchronization.saveJob(
            RepositorySyncJob.queue(userId, repositoryId, idempotencyKey, now)
        );
        synchronization.appendOutbox(
            "REPOSITORY", repositoryId, "RepositorySynchronizationRequested",
            "{\"jobId\":\"" + job.id() + "\",\"repositoryId\":\"" + repositoryId + "\"}", now
        );
        audit.record(RepositoryAuditEvent.REPOSITORY_SYNC_REQUESTED, userId, repositoryId, now);
        return RepositorySyncJobView.from(job);
    }

    @Transactional(readOnly = true)
    Repository target(UUID userId, UUID repositoryId) {
        return owned(userId, repositoryId);
    }

    @Transactional(readOnly = true)
    RepositorySyncJobView getJob(UUID userId, UUID jobId) {
        return synchronization.findByIdAndOwner(jobId, userId)
            .map(RepositorySyncJobView::from)
            .orElseThrow(RepositoryNotFoundException::new);
    }

    @Transactional(readOnly = true)
    RepositorySnapshotListView listSnapshots(UUID userId, UUID repositoryId) {
        owned(userId, repositoryId);
        return new RepositorySnapshotListView(
            synchronization.findSnapshots(userId, repositoryId).stream()
                .map(RepositorySnapshotView::from).toList()
        );
    }

    @Transactional(readOnly = true)
    RepositorySnapshotView getSnapshot(UUID userId, UUID repositoryId, UUID snapshotId) {
        owned(userId, repositoryId);
        return synchronization.findSnapshot(userId, repositoryId, snapshotId)
            .map(RepositorySnapshotView::from)
            .orElseThrow(RepositoryNotFoundException::new);
    }

    @Transactional(readOnly = true)
    TechnologySummaryView getTechnologies(UUID userId, UUID repositoryId) {
        Repository repository = owned(userId, repositoryId);
        if (repository.currentSnapshotId() == null) {
            throw new RepositoryNotFoundException();
        }
        return synchronization.findSnapshot(userId, repositoryId, repository.currentSnapshotId())
            .map(TechnologySummaryView::from)
            .orElseThrow(RepositoryNotFoundException::new);
    }

    @Transactional(readOnly = true)
    RepositoryEvidenceSummaryView getEvidence(UUID userId, UUID repositoryId) {
        Repository repository = owned(userId, repositoryId);
        if (repository.currentSnapshotId() == null) {
            throw new RepositoryNotFoundException();
        }
        return synchronization.findSnapshot(userId, repositoryId, repository.currentSnapshotId())
            .map(RepositoryEvidenceSummaryView::from)
            .orElseThrow(RepositoryNotFoundException::new);
    }

    @Transactional
    Optional<RepositorySyncWorkItem> claim(Instant now) {
        return synchronization.findNextClaimable(now).map(job -> {
            RepositorySyncJob running = synchronization.saveJob(job.start(now));
            return new RepositorySyncWorkItem(running, owned(running.userId(), running.repositoryId()));
        });
    }

    @Transactional
    void complete(RepositorySyncWorkItem item, GitHubRepositorySnapshot collected, Instant now) {
        RepositorySyncJob current = synchronization.findByIdAndOwner(item.job().id(), item.job().userId())
            .orElseThrow(RepositoryNotFoundException::new);
        if (current.status() != RepositorySyncJobStatus.RUNNING) {
            return;
        }
        Repository currentRepository = owned(item.job().userId(), item.repository().id());
        if (currentRepository.lifecycle() == RepositoryLifecycle.ARCHIVED
            || currentRepository.lifecycle() == RepositoryLifecycle.DELETED_EXTERNALLY) {
            synchronization.saveJob(current.failTerminal(
                "RESOURCE_CONFLICT", "Repository lifecycle changed during synchronization.", now
            ));
            audit.record(RepositoryAuditEvent.REPOSITORY_SYNC_FAILED, item.job().userId(), item.repository().id(), now);
            return;
        }
        List<RepositoryBranch> branches = collected.branches().stream()
            .map(value -> new RepositoryBranch(value.name(), value.headCommitSha(), value.defaultBranch()))
            .toList();
        List<RepositoryCommit> commits = collected.commits().stream()
            .map(value -> new RepositoryCommit(
                value.sha(), value.authorLogin(), value.committedAt(), value.messageSummary()
            )).toList();
        long totalLanguageBytes = collected.languages().stream().mapToLong(value -> value.byteCount()).sum();
        var languages = collected.languages().stream()
            .map(value -> RepositoryLanguage.normalize(value.providerLabel(), value.byteCount(), totalLanguageBytes))
            .toList();
        var dependencies = collected.dependencies().stream()
            .map(value -> RepositoryDependency.normalized(
                value.ecosystem(), value.packageName(), value.versionConstraint(), value.scope(), value.manifestPath()
            ))
            .toList();
        var files = collected.files().stream()
            .map(value -> RepositoryFile.normalized(value.path(), value.blobSha(), value.byteSize()))
            .toList();
        RepositorySnapshot snapshot = synchronization.saveSnapshot(RepositorySnapshot.ready(
            currentRepository.id(), currentRepository.userId(), collected.sourceRevision(), now,
            branches, commits, languages, dependencies, files
        ));
        repositories.save(currentRepository.markSynchronized(snapshot.id(), now));
        synchronization.saveJob(current.succeed(snapshot.id(), now));
        synchronization.appendOutbox(
            "REPOSITORY_SNAPSHOT", snapshot.id(), "RepositorySnapshotCreated",
            "{\"snapshotId\":\"" + snapshot.id() + "\",\"repositoryId\":\"" + item.repository().id() + "\"}", now
        );
        audit.record(RepositoryAuditEvent.REPOSITORY_SYNC_SUCCEEDED, item.job().userId(), item.repository().id(), now);
    }

    @Transactional
    void fail(RepositorySyncWorkItem item, String errorCode, String safeMessage, Instant now) {
        RepositorySyncJob current = synchronization.findByIdAndOwner(item.job().id(), item.job().userId())
            .orElseThrow(RepositoryNotFoundException::new);
        RepositorySyncJob failedOrQueued = current.failOrRetry(errorCode, safeMessage, now);
        synchronization.saveJob(failedOrQueued);
        if (failedOrQueued.status() == RepositorySyncJobStatus.FAILED) {
            Repository currentRepository = owned(item.job().userId(), item.repository().id());
            if (currentRepository.lifecycle() != RepositoryLifecycle.ARCHIVED
                && currentRepository.lifecycle() != RepositoryLifecycle.DELETED_EXTERNALLY) {
                repositories.save(currentRepository.markSyncFailed(now));
            }
            synchronization.appendOutbox(
                "REPOSITORY", item.repository().id(), "RepositorySynchronizationFailed",
                "{\"jobId\":\"" + item.job().id() + "\",\"errorCode\":\"" + errorCode + "\"}", now
            );
            audit.record(RepositoryAuditEvent.REPOSITORY_SYNC_FAILED, item.job().userId(), item.repository().id(), now);
        }
    }

    private Repository owned(UUID userId, UUID repositoryId) {
        return repositories.findByIdAndOwner(repositoryId, userId)
            .orElseThrow(RepositoryNotFoundException::new);
    }
}
