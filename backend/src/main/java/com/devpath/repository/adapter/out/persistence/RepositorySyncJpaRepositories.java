package com.devpath.repository.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RepositorySyncJobJpaRepository extends JpaRepository<RepositorySyncJobJpaEntity, UUID> {
    Optional<RepositorySyncJobJpaEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    Optional<RepositorySyncJobJpaEntity> findFirstByRepositoryIdAndStatusIn(UUID repositoryId, List<String> statuses);
    Optional<RepositorySyncJobJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    List<RepositorySyncJobJpaEntity> findAllByUserIdOrderBySubmittedAtDescIdDesc(UUID userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from RepositorySyncJobJpaEntity job where job.nextAttemptAt <= :now and job.status in ('QUEUED', 'RUNNING') order by job.submittedAt")
    List<RepositorySyncJobJpaEntity> findClaimable(@Param("now") Instant now, Pageable pageable);
}

interface RepositorySnapshotJpaRepository extends JpaRepository<RepositorySnapshotJpaEntity, UUID> {
    List<RepositorySnapshotJpaEntity> findAllByUserIdAndRepositoryIdOrderByCapturedAtDesc(UUID userId, UUID repositoryId);
    Optional<RepositorySnapshotJpaEntity> findByIdAndUserIdAndRepositoryId(UUID id, UUID userId, UUID repositoryId);
}

interface RepositoryBranchJpaRepository extends JpaRepository<RepositoryBranchJpaEntity, UUID> {
    List<RepositoryBranchJpaEntity> findAllBySnapshotIdOrderByName(UUID snapshotId);
}

interface RepositoryCommitJpaRepository extends JpaRepository<RepositoryCommitJpaEntity, UUID> {
    List<RepositoryCommitJpaEntity> findAllBySnapshotIdOrderByCommittedAtDesc(UUID snapshotId);
}

interface RepositoryLanguageJpaRepository extends JpaRepository<RepositoryLanguageJpaEntity, UUID> {
    List<RepositoryLanguageJpaEntity> findAllBySnapshotIdOrderByPercentageDescProviderLabelAsc(UUID snapshotId);
}

interface RepositoryDependencyJpaRepository extends JpaRepository<RepositoryDependencyJpaEntity, UUID> {
    List<RepositoryDependencyJpaEntity> findAllBySnapshotIdOrderByManifestPathAscPackageNameAsc(UUID snapshotId);
}

interface RepositoryFileJpaRepository extends JpaRepository<RepositoryFileJpaEntity, UUID> {
    List<RepositoryFileJpaEntity> findAllBySnapshotIdOrderByPathAsc(UUID snapshotId);
}

interface RepositoryPullRequestJpaRepository extends JpaRepository<RepositoryPullRequestJpaEntity, UUID> {
    List<RepositoryPullRequestJpaEntity> findAllBySnapshotIdOrderByOpenedAtDesc(UUID snapshotId);
}

interface RepositoryIssueJpaRepository extends JpaRepository<RepositoryIssueJpaEntity, UUID> {
    List<RepositoryIssueJpaEntity> findAllBySnapshotIdOrderByOpenedAtDesc(UUID snapshotId);
}

interface RepositoryDocumentJpaRepository extends JpaRepository<RepositoryDocumentJpaEntity, UUID> {
    List<RepositoryDocumentJpaEntity> findAllBySnapshotIdOrderByDocumentTypeAscPathAsc(UUID snapshotId);
}

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {}
