package com.devpath.repository.adapter.out.persistence;

import com.devpath.repository.application.RepositorySynchronizationPersistencePort;
import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.repository.domain.RepositorySyncJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

@org.springframework.stereotype.Repository
class JpaRepositorySynchronizationAdapter implements RepositorySynchronizationPersistencePort {
    private final RepositorySyncJobJpaRepository jobs;
    private final RepositorySnapshotJpaRepository snapshots;
    private final RepositoryBranchJpaRepository branches;
    private final RepositoryCommitJpaRepository commits;
    private final RepositoryLanguageJpaRepository languages;
    private final RepositoryDependencyJpaRepository dependencies;
    private final RepositoryFileJpaRepository files;
    private final RepositoryPullRequestJpaRepository pullRequests;
    private final RepositoryIssueJpaRepository issues;
    private final RepositoryDocumentJpaRepository documents;
    private final OutboxEventJpaRepository outbox;
    private final JdbcTemplate jdbc;

    JpaRepositorySynchronizationAdapter(
        RepositorySyncJobJpaRepository jobs,
        RepositorySnapshotJpaRepository snapshots,
        RepositoryBranchJpaRepository branches,
        RepositoryCommitJpaRepository commits,
        RepositoryLanguageJpaRepository languages,
        RepositoryDependencyJpaRepository dependencies,
        RepositoryFileJpaRepository files,
        RepositoryPullRequestJpaRepository pullRequests,
        RepositoryIssueJpaRepository issues,
        RepositoryDocumentJpaRepository documents,
        OutboxEventJpaRepository outbox,
        JdbcTemplate jdbc
    ) {
        this.jobs = jobs; this.snapshots = snapshots; this.branches = branches;
        this.commits = commits; this.languages = languages; this.dependencies = dependencies;
        this.files = files; this.pullRequests = pullRequests; this.issues = issues;
        this.documents = documents; this.outbox = outbox; this.jdbc = jdbc;
    }

    public void acquireRequestLocks(UUID userId, UUID repositoryId, String idempotencyKey) {
        advisoryLock("repository-sync:key:" + userId + ":" + idempotencyKey);
        advisoryLock("repository-sync:repository:" + repositoryId);
    }

    public Optional<RepositorySyncJob> findByOwnerAndIdempotencyKey(UUID userId, String key) {
        return jobs.findByUserIdAndIdempotencyKey(userId, key).map(RepositorySyncJobJpaEntity::toDomain);
    }
    public Optional<RepositorySyncJob> findActiveByRepository(UUID repositoryId) {
        return jobs.findFirstByRepositoryIdAndStatusIn(repositoryId, List.of("QUEUED", "RUNNING"))
            .map(RepositorySyncJobJpaEntity::toDomain);
    }
    public Optional<RepositorySyncJob> findByIdAndOwner(UUID jobId, UUID userId) {
        return jobs.findByIdAndUserId(jobId, userId).map(RepositorySyncJobJpaEntity::toDomain);
    }
    public Optional<RepositorySyncJob> findNextClaimable(Instant now) {
        return jobs.findClaimable(now, PageRequest.of(0, 1)).stream().findFirst().map(RepositorySyncJobJpaEntity::toDomain);
    }
    public List<RepositorySyncJob> findRecentByOwner(UUID userId, int limit) {
        return jobs.findAllByUserIdOrderBySubmittedAtDescIdDesc(userId, PageRequest.of(0, limit)).stream()
            .map(RepositorySyncJobJpaEntity::toDomain).toList();
    }
    public RepositorySyncJob saveJob(RepositorySyncJob job) {
        return jobs.saveAndFlush(new RepositorySyncJobJpaEntity(job)).toDomain();
    }
    public RepositorySnapshot saveSnapshot(RepositorySnapshot snapshot) {
        RepositorySnapshotJpaEntity saved = snapshots.saveAndFlush(new RepositorySnapshotJpaEntity(snapshot));
        branches.saveAll(snapshot.branches().stream().map(value -> new RepositoryBranchJpaEntity(saved.id(), value)).toList());
        commits.saveAll(snapshot.commits().stream().map(value -> new RepositoryCommitJpaEntity(saved.id(), value)).toList());
        languages.saveAll(snapshot.languages().stream().map(value -> new RepositoryLanguageJpaEntity(saved.id(), value)).toList());
        dependencies.saveAll(snapshot.dependencies().stream()
            .map(value -> new RepositoryDependencyJpaEntity(saved.id(), value)).toList());
        files.saveAll(snapshot.files().stream().map(value -> new RepositoryFileJpaEntity(saved.id(), value)).toList());
        pullRequests.saveAll(snapshot.pullRequests().stream()
            .map(value -> new RepositoryPullRequestJpaEntity(saved.id(), value)).toList());
        issues.saveAll(snapshot.issues().stream().map(value -> new RepositoryIssueJpaEntity(saved.id(), value)).toList());
        documents.saveAll(snapshot.documents().stream().map(value -> new RepositoryDocumentJpaEntity(saved.id(), value)).toList());
        return snapshot;
    }
    public List<RepositorySnapshot> findSnapshots(UUID userId, UUID repositoryId) {
        return snapshots.findAllByUserIdAndRepositoryIdOrderByCapturedAtDesc(userId, repositoryId).stream()
            .map(this::hydrate).toList();
    }
    public Optional<RepositorySnapshot> findSnapshot(UUID userId, UUID repositoryId, UUID snapshotId) {
        return snapshots.findByIdAndUserIdAndRepositoryId(snapshotId, userId, repositoryId).map(this::hydrate);
    }
    public void appendOutbox(String aggregateType, UUID aggregateId, String eventType, String payload, Instant occurredAt) {
        outbox.save(new OutboxEventJpaEntity(aggregateType, aggregateId, eventType, payload, occurredAt));
    }
    private void advisoryLock(String key) {
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(?, 0))",
            (ResultSetExtractor<Void>) resultSet -> null, key);
    }
    private RepositorySnapshot hydrate(RepositorySnapshotJpaEntity value) {
        return value.toDomain(
            branches.findAllBySnapshotIdOrderByName(value.id()).stream().map(RepositoryBranchJpaEntity::toDomain).toList(),
            commits.findAllBySnapshotIdOrderByCommittedAtDesc(value.id()).stream().map(RepositoryCommitJpaEntity::toDomain).toList(),
            languages.findAllBySnapshotIdOrderByPercentageDescProviderLabelAsc(value.id()).stream()
                .map(RepositoryLanguageJpaEntity::toDomain).toList(),
            dependencies.findAllBySnapshotIdOrderByManifestPathAscPackageNameAsc(value.id()).stream()
                .map(RepositoryDependencyJpaEntity::toDomain).toList(),
            files.findAllBySnapshotIdOrderByPathAsc(value.id()).stream().map(RepositoryFileJpaEntity::toDomain).toList(),
            pullRequests.findAllBySnapshotIdOrderByOpenedAtDesc(value.id()).stream()
                .map(RepositoryPullRequestJpaEntity::toDomain).toList(),
            issues.findAllBySnapshotIdOrderByOpenedAtDesc(value.id()).stream().map(RepositoryIssueJpaEntity::toDomain).toList(),
            documents.findAllBySnapshotIdOrderByDocumentTypeAscPathAsc(value.id()).stream()
                .map(RepositoryDocumentJpaEntity::toDomain).toList()
        );
    }
}
