package com.devpath.repository.adapter.out.persistence;

import com.devpath.repository.domain.RepositoryBranch;
import com.devpath.repository.domain.RepositoryCommit;
import com.devpath.repository.domain.RepositoryLanguage;
import com.devpath.repository.domain.RepositoryDependency;
import com.devpath.repository.domain.RepositoryFile;
import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.repository.domain.RepositoryPullRequest;
import com.devpath.repository.domain.RepositoryIssue;
import com.devpath.repository.domain.RepositoryDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "repository_snapshots")
class RepositorySnapshotJpaEntity {
    @Id @Column(name = "snapshot_id") private UUID id;
    @Column(name = "repository_id", nullable = false, updatable = false) private UUID repositoryId;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "source_revision", nullable = false, updatable = false, length = 64) private String sourceRevision;
    @Column(name = "captured_at", nullable = false, updatable = false) private Instant capturedAt;
    @Column(name = "snapshot_status", nullable = false, updatable = false, length = 24) private String status;
    @Column(name = "content_hash", nullable = false, updatable = false, length = 64) private String contentHash;
    @Column(name = "branch_count", nullable = false, updatable = false) private int branchCount;
    @Column(name = "commit_count", nullable = false, updatable = false) private int commitCount;
    @Column(name = "retention_status", nullable = false, length = 24) private String retentionStatus;

    protected RepositorySnapshotJpaEntity() {}

    RepositorySnapshotJpaEntity(RepositorySnapshot snapshot) {
        id = snapshot.id(); repositoryId = snapshot.repositoryId(); userId = snapshot.userId();
        sourceRevision = snapshot.sourceRevision(); capturedAt = snapshot.capturedAt(); status = snapshot.status();
        contentHash = snapshot.contentHash(); branchCount = snapshot.branches().size();
        commitCount = snapshot.commits().size(); retentionStatus = snapshot.retentionStatus();
    }

    UUID id() { return id; }

    RepositorySnapshot toDomain(
        List<RepositoryBranch> branches, List<RepositoryCommit> commits, List<RepositoryLanguage> languages,
        List<RepositoryDependency> dependencies, List<RepositoryFile> files,
        List<RepositoryPullRequest> pullRequests, List<RepositoryIssue> issues,
        List<RepositoryDocument> documents
    ) {
        return new RepositorySnapshot(
            id, repositoryId, userId, sourceRevision, capturedAt, status, contentHash,
            retentionStatus, branches, commits, languages, dependencies, files, pullRequests, issues, documents
        );
    }
}

@Entity
@Table(name = "repository_branches")
class RepositoryBranchJpaEntity {
    @Id @Column(name = "branch_record_id") private UUID id;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "branch_name", nullable = false, updatable = false, length = 255) private String name;
    @Column(name = "default_branch", nullable = false, updatable = false) private boolean defaultBranch;
    @Column(name = "head_commit_sha", nullable = false, updatable = false, length = 64) private String headCommitSha;

    protected RepositoryBranchJpaEntity() {}
    RepositoryBranchJpaEntity(UUID snapshotId, RepositoryBranch branch) {
        id = UUID.randomUUID(); this.snapshotId = snapshotId; name = branch.name();
        defaultBranch = branch.defaultBranch(); headCommitSha = branch.headCommitSha();
    }
    RepositoryBranch toDomain() { return new RepositoryBranch(name, headCommitSha, defaultBranch); }
}

@Entity
@Table(name = "repository_commits")
class RepositoryCommitJpaEntity {
    @Id @Column(name = "commit_record_id") private UUID id;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "commit_sha", nullable = false, updatable = false, length = 64) private String sha;
    @Column(name = "author_login", updatable = false, length = 255) private String authorLogin;
    @Column(name = "committed_at", nullable = false, updatable = false) private Instant committedAt;
    @Column(name = "message_summary", nullable = false, updatable = false, length = 500) private String messageSummary;

    protected RepositoryCommitJpaEntity() {}
    RepositoryCommitJpaEntity(UUID snapshotId, RepositoryCommit commit) {
        id = UUID.randomUUID(); this.snapshotId = snapshotId; sha = commit.sha(); authorLogin = commit.authorLogin();
        committedAt = commit.committedAt(); messageSummary = commit.messageSummary();
    }
    RepositoryCommit toDomain() { return new RepositoryCommit(sha, authorLogin, committedAt, messageSummary); }
}

@Entity
@Table(name = "repository_language_statistics")
class RepositoryLanguageJpaEntity {
    @Id @Column(name = "language_record_id") private UUID id;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "provider_label", nullable = false, updatable = false, length = 128) private String providerLabel;
    @Column(name = "canonical_name", nullable = false, updatable = false, length = 128) private String canonicalName;
    @Column(name = "byte_count", nullable = false, updatable = false) private long byteCount;
    @Column(name = "percentage", nullable = false, updatable = false, precision = 7, scale = 4) private java.math.BigDecimal percentage;
    @Column(name = "taxonomy_status", nullable = false, updatable = false, length = 16) private String taxonomyStatus;
    @Column(name = "taxonomy_version", nullable = false, updatable = false, length = 32) private String taxonomyVersion;
    @Column(name = "extractor_version", nullable = false, updatable = false, length = 32) private String extractorVersion;

    protected RepositoryLanguageJpaEntity() {}
    RepositoryLanguageJpaEntity(UUID snapshotId, RepositoryLanguage language) {
        id = UUID.randomUUID(); this.snapshotId = snapshotId; providerLabel = language.providerLabel();
        canonicalName = language.canonicalName(); byteCount = language.byteCount(); percentage = language.percentage();
        taxonomyStatus = language.taxonomyStatus(); taxonomyVersion = language.taxonomyVersion();
        extractorVersion = language.extractorVersion();
    }
    RepositoryLanguage toDomain() {
        return new RepositoryLanguage(providerLabel, canonicalName, byteCount, percentage, taxonomyStatus,
            taxonomyVersion, extractorVersion);
    }
}

@Entity
@Table(name = "repository_dependencies")
class RepositoryDependencyJpaEntity {
    @Id @Column(name = "dependency_record_id") private UUID id;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "ecosystem", nullable = false, updatable = false, length = 32) private String ecosystem;
    @Column(name = "package_name", nullable = false, updatable = false, length = 255) private String packageName;
    @Column(name = "version_constraint", updatable = false, length = 255) private String versionConstraint;
    @Column(name = "dependency_scope", nullable = false, updatable = false, length = 32) private String scope;
    @Column(name = "manifest_path", nullable = false, updatable = false, length = 500) private String manifestPath;
    @Column(name = "extractor_version", nullable = false, updatable = false, length = 32) private String extractorVersion;

    protected RepositoryDependencyJpaEntity() {}
    RepositoryDependencyJpaEntity(UUID snapshotId, RepositoryDependency dependency) {
        id = UUID.randomUUID(); this.snapshotId = snapshotId; ecosystem = dependency.ecosystem();
        packageName = dependency.packageName(); versionConstraint = dependency.versionConstraint();
        scope = dependency.scope(); manifestPath = dependency.manifestPath(); extractorVersion = dependency.extractorVersion();
    }
    RepositoryDependency toDomain() {
        return new RepositoryDependency(ecosystem, packageName, versionConstraint, scope, manifestPath, extractorVersion);
    }
}

@Entity
@Table(name = "repository_file_entries")
class RepositoryFileJpaEntity {
    @Id @Column(name = "file_entry_id") private UUID id;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "normalized_path", nullable = false, updatable = false, length = 1000) private String path;
    @Column(name = "blob_sha", nullable = false, updatable = false, length = 64) private String blobSha;
    @Column(name = "byte_size", nullable = false, updatable = false) private long byteSize;
    @Column(name = "extractor_version", nullable = false, updatable = false, length = 32) private String extractorVersion;

    protected RepositoryFileJpaEntity() {}
    RepositoryFileJpaEntity(UUID snapshotId, RepositoryFile file) {
        id = UUID.randomUUID(); this.snapshotId = snapshotId; path = file.path(); blobSha = file.blobSha();
        byteSize = file.byteSize(); extractorVersion = file.extractorVersion();
    }
    RepositoryFile toDomain() { return new RepositoryFile(path, blobSha, byteSize, extractorVersion); }
}

@Entity
@Table(name = "repository_pull_requests")
class RepositoryPullRequestJpaEntity {
    @Id @Column(name = "pull_request_record_id") private UUID id;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "provider_pull_request_id", nullable = false, updatable = false, length = 128) private String providerId;
    @Column(name = "status", nullable = false, updatable = false, length = 16) private String status;
    @Column(name = "opened_at", nullable = false, updatable = false) private Instant openedAt;
    @Column(name = "closed_at", updatable = false) private Instant closedAt;
    @Column(name = "merged_at", updatable = false) private Instant mergedAt;
    @Column(name = "review_count", nullable = false, updatable = false) private int reviewCount;

    protected RepositoryPullRequestJpaEntity() {}
    RepositoryPullRequestJpaEntity(UUID snapshotId, RepositoryPullRequest value) {
        id = UUID.randomUUID(); this.snapshotId = snapshotId; providerId = value.providerPullRequestId();
        status = value.status(); openedAt = value.openedAt(); closedAt = value.closedAt();
        mergedAt = value.mergedAt(); reviewCount = value.reviewCount();
    }
    RepositoryPullRequest toDomain() {
        return new RepositoryPullRequest(providerId, status, openedAt, closedAt, mergedAt, reviewCount);
    }
}

@Entity
@Table(name = "repository_issues")
class RepositoryIssueJpaEntity {
    @Id @Column(name = "issue_record_id") private UUID id;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "provider_issue_id", nullable = false, updatable = false, length = 128) private String providerId;
    @Column(name = "status", nullable = false, updatable = false, length = 16) private String status;
    @Column(name = "labels_text", nullable = false, updatable = false) private String labelsText;
    @Column(name = "opened_at", nullable = false, updatable = false) private Instant openedAt;
    @Column(name = "closed_at", updatable = false) private Instant closedAt;

    protected RepositoryIssueJpaEntity() {}
    RepositoryIssueJpaEntity(UUID snapshotId, RepositoryIssue value) {
        id = UUID.randomUUID(); this.snapshotId = snapshotId; providerId = value.providerIssueId();
        status = value.status(); labelsText = String.join("\n", value.labels());
        openedAt = value.openedAt(); closedAt = value.closedAt();
    }
    RepositoryIssue toDomain() {
        return new RepositoryIssue(providerId, status,
            labelsText.isBlank() ? List.of() : List.of(labelsText.split("\\n", -1)), openedAt, closedAt);
    }
}

@Entity
@Table(name = "repository_documents")
class RepositoryDocumentJpaEntity {
    @Id @Column(name = "repository_document_id") private UUID id;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "document_type", nullable = false, updatable = false, length = 32) private String documentType;
    @Column(name = "normalized_path", nullable = false, updatable = false, length = 1000) private String path;
    @Column(name = "content_hash", nullable = false, updatable = false, length = 64) private String contentHash;
    @Column(name = "byte_size", nullable = false, updatable = false) private long byteSize;
    @Column(name = "quality_signals", nullable = false, updatable = false, length = 255) private String qualitySignals;

    protected RepositoryDocumentJpaEntity() {}
    RepositoryDocumentJpaEntity(UUID snapshotId, RepositoryDocument value) {
        id = UUID.randomUUID(); this.snapshotId = snapshotId; documentType = value.documentType(); path = value.path();
        contentHash = value.contentHash(); byteSize = value.byteSize(); qualitySignals = String.join(",", value.qualitySignals());
    }
    RepositoryDocument toDomain() {
        return new RepositoryDocument(documentType, path, contentHash, byteSize,
            qualitySignals.isBlank() ? List.of() : List.of(qualitySignals.split(",", -1)));
    }
}
