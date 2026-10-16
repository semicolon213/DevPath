package com.devpath.repository.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

interface RepositorySyncJobJpaRepository extends JpaRepository<RepositorySyncJobJpaEntity, UUID> {
    Optional<RepositorySyncJobJpaEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    Optional<RepositorySyncJobJpaEntity> findFirstByRepositoryIdAndStatusIn(UUID repositoryId, List<String> statuses);
    Optional<RepositorySyncJobJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    List<RepositorySyncJobJpaEntity> findAllByUserIdOrderBySubmittedAtDescIdDesc(UUID userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select job from RepositorySyncJobJpaEntity job where job.nextAttemptAt <= :now and job.status in ('QUEUED', 'RUNNING') order by job.submittedAt")
    List<RepositorySyncJobJpaEntity> findClaimable(@Param("now") Instant now, Pageable pageable);
}

interface RepositorySnapshotJpaRepository extends JpaRepository<RepositorySnapshotJpaEntity, UUID> {
    Optional<RepositorySnapshotJpaEntity> findByIdAndUserIdAndRepositoryId(UUID id, UUID userId, UUID repositoryId);

    @Query("""
        select snapshot.id as snapshotId, snapshot.repositoryId as repositoryId,
            snapshot.capturedAt as capturedAt, snapshot.sourceRevision as sourceRevision,
            snapshot.status as status, snapshot.contentHash as contentHash,
            snapshot.branchCount as branchCount, snapshot.commitCount as commitCount,
            (select count(pullRequest) from RepositoryPullRequestJpaEntity pullRequest
                where pullRequest.snapshotId = snapshot.id) as pullRequestCount,
            (select count(issue) from RepositoryIssueJpaEntity issue
                where issue.snapshotId = snapshot.id) as issueCount,
            (select count(document) from RepositoryDocumentJpaEntity document
                where document.snapshotId = snapshot.id) as documentCount
        from RepositorySnapshotJpaEntity snapshot
        where snapshot.userId = :userId and snapshot.repositoryId = :repositoryId
        order by snapshot.capturedAt desc
        """)
    List<RepositorySnapshotSummaryProjection> findSummaries(
        @Param("userId") UUID userId,
        @Param("repositoryId") UUID repositoryId
    );
}

interface RepositorySnapshotSummaryProjection {
    UUID getSnapshotId();
    UUID getRepositoryId();
    Instant getCapturedAt();
    String getSourceRevision();
    String getStatus();
    String getContentHash();
    long getBranchCount();
    long getCommitCount();
    long getPullRequestCount();
    long getIssueCount();
    long getDocumentCount();
}

interface RepositoryBranchJpaRepository extends JpaRepository<RepositoryBranchJpaEntity, UUID> {
    List<RepositoryBranchJpaEntity> findAllBySnapshotIdOrderByName(UUID snapshotId);

    @Query("""
        select child from RepositoryBranchJpaEntity child, RepositorySnapshotJpaEntity snapshot
        where child.snapshotId = snapshot.id and snapshot.id = :snapshotId
            and snapshot.userId = :userId and snapshot.repositoryId = :repositoryId
        order by child.name
        """)
    List<RepositoryBranchJpaEntity> findAllForOwner(
        @Param("snapshotId") UUID snapshotId, @Param("userId") UUID userId,
        @Param("repositoryId") UUID repositoryId
    );
}

interface RepositoryCommitJpaRepository extends JpaRepository<RepositoryCommitJpaEntity, UUID> {
    List<RepositoryCommitJpaEntity> findAllBySnapshotIdOrderByCommittedAtDesc(UUID snapshotId);

    @Query("""
        select child from RepositoryCommitJpaEntity child, RepositorySnapshotJpaEntity snapshot
        where child.snapshotId = snapshot.id and snapshot.id = :snapshotId
            and snapshot.userId = :userId and snapshot.repositoryId = :repositoryId
        order by child.committedAt desc
        """)
    List<RepositoryCommitJpaEntity> findAllForOwner(
        @Param("snapshotId") UUID snapshotId, @Param("userId") UUID userId,
        @Param("repositoryId") UUID repositoryId
    );
}

interface RepositoryLanguageJpaRepository extends JpaRepository<RepositoryLanguageJpaEntity, UUID> {
    List<RepositoryLanguageJpaEntity> findAllBySnapshotIdOrderByPercentageDescProviderLabelAsc(UUID snapshotId);

    @Query("""
        select child from RepositoryLanguageJpaEntity child, RepositorySnapshotJpaEntity snapshot
        where child.snapshotId = snapshot.id and snapshot.id = :snapshotId
            and snapshot.userId = :userId and snapshot.repositoryId = :repositoryId
        order by child.percentage desc, child.providerLabel
        """)
    List<RepositoryLanguageJpaEntity> findAllForOwner(
        @Param("snapshotId") UUID snapshotId, @Param("userId") UUID userId,
        @Param("repositoryId") UUID repositoryId
    );
}

interface RepositoryDependencyJpaRepository extends JpaRepository<RepositoryDependencyJpaEntity, UUID> {
    List<RepositoryDependencyJpaEntity> findAllBySnapshotIdOrderByManifestPathAscPackageNameAsc(UUID snapshotId);

    @Query("""
        select child from RepositoryDependencyJpaEntity child, RepositorySnapshotJpaEntity snapshot
        where child.snapshotId = snapshot.id and snapshot.id = :snapshotId
            and snapshot.userId = :userId and snapshot.repositoryId = :repositoryId
        order by child.manifestPath, child.packageName
        """)
    List<RepositoryDependencyJpaEntity> findAllForOwner(
        @Param("snapshotId") UUID snapshotId, @Param("userId") UUID userId,
        @Param("repositoryId") UUID repositoryId
    );
}

interface RepositoryFileJpaRepository extends JpaRepository<RepositoryFileJpaEntity, UUID> {
    List<RepositoryFileJpaEntity> findAllBySnapshotIdOrderByPathAsc(UUID snapshotId);

    @Query("""
        select child from RepositoryFileJpaEntity child, RepositorySnapshotJpaEntity snapshot
        where child.snapshotId = snapshot.id and snapshot.id = :snapshotId
            and snapshot.userId = :userId and snapshot.repositoryId = :repositoryId
        order by child.path
        """)
    List<RepositoryFileJpaEntity> findAllForOwner(
        @Param("snapshotId") UUID snapshotId, @Param("userId") UUID userId,
        @Param("repositoryId") UUID repositoryId
    );
}

interface RepositoryPullRequestJpaRepository extends JpaRepository<RepositoryPullRequestJpaEntity, UUID> {
    List<RepositoryPullRequestJpaEntity> findAllBySnapshotIdOrderByOpenedAtDesc(UUID snapshotId);

    @Query("""
        select child from RepositoryPullRequestJpaEntity child, RepositorySnapshotJpaEntity snapshot
        where child.snapshotId = snapshot.id and snapshot.id = :snapshotId
            and snapshot.userId = :userId and snapshot.repositoryId = :repositoryId
        order by child.openedAt desc
        """)
    List<RepositoryPullRequestJpaEntity> findAllForOwner(
        @Param("snapshotId") UUID snapshotId, @Param("userId") UUID userId,
        @Param("repositoryId") UUID repositoryId
    );
}

interface RepositoryIssueJpaRepository extends JpaRepository<RepositoryIssueJpaEntity, UUID> {
    List<RepositoryIssueJpaEntity> findAllBySnapshotIdOrderByOpenedAtDesc(UUID snapshotId);

    @Query("""
        select child from RepositoryIssueJpaEntity child, RepositorySnapshotJpaEntity snapshot
        where child.snapshotId = snapshot.id and snapshot.id = :snapshotId
            and snapshot.userId = :userId and snapshot.repositoryId = :repositoryId
        order by child.openedAt desc
        """)
    List<RepositoryIssueJpaEntity> findAllForOwner(
        @Param("snapshotId") UUID snapshotId, @Param("userId") UUID userId,
        @Param("repositoryId") UUID repositoryId
    );
}

interface RepositoryDocumentJpaRepository extends JpaRepository<RepositoryDocumentJpaEntity, UUID> {
    List<RepositoryDocumentJpaEntity> findAllBySnapshotIdOrderByDocumentTypeAscPathAsc(UUID snapshotId);

    @Query("""
        select child from RepositoryDocumentJpaEntity child, RepositorySnapshotJpaEntity snapshot
        where child.snapshotId = snapshot.id and snapshot.id = :snapshotId
            and snapshot.userId = :userId and snapshot.repositoryId = :repositoryId
        order by child.documentType, child.path
        """)
    List<RepositoryDocumentJpaEntity> findAllForOwner(
        @Param("snapshotId") UUID snapshotId, @Param("userId") UUID userId,
        @Param("repositoryId") UUID repositoryId
    );
}

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {}
