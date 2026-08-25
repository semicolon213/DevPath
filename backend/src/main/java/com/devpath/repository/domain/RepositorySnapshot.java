package com.devpath.repository.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

public record RepositorySnapshot(
    UUID id,
    UUID repositoryId,
    UUID userId,
    String sourceRevision,
    Instant capturedAt,
    String status,
    String contentHash,
    String retentionStatus,
    List<RepositoryBranch> branches,
    List<RepositoryCommit> commits,
    List<RepositoryLanguage> languages,
    List<RepositoryDependency> dependencies,
    List<RepositoryFile> files,
    List<RepositoryPullRequest> pullRequests,
    List<RepositoryIssue> issues,
    List<RepositoryDocument> documents
) {
    public RepositorySnapshot {
        Objects.requireNonNull(id);
        Objects.requireNonNull(repositoryId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(capturedAt);
        Objects.requireNonNull(branches);
        Objects.requireNonNull(commits);
        Objects.requireNonNull(languages);
        Objects.requireNonNull(dependencies);
        Objects.requireNonNull(files);
        Objects.requireNonNull(pullRequests);
        Objects.requireNonNull(issues);
        Objects.requireNonNull(documents);
        if (sourceRevision == null || !sourceRevision.matches("[a-fA-F0-9]{40,64}")
            || !"READY".equals(status) || contentHash == null || !contentHash.matches("[a-f0-9]{64}")
            || !("ACTIVE".equals(retentionStatus) || "DELETED_BY_POLICY".equals(retentionStatus))) {
            throw new IllegalArgumentException("Repository snapshot metadata is invalid");
        }
        branches = List.copyOf(branches);
        commits = List.copyOf(commits);
        languages = List.copyOf(languages);
        dependencies = List.copyOf(dependencies);
        files = List.copyOf(files);
        pullRequests = List.copyOf(pullRequests);
        issues = List.copyOf(issues);
        documents = List.copyOf(documents);
        if (branches.stream().map(RepositoryBranch::name).distinct().count() != branches.size()
            || commits.stream().map(RepositoryCommit::sha).distinct().count() != commits.size()
            || languages.stream().map(RepositoryLanguage::providerLabel).distinct().count() != languages.size()
            || files.stream().map(RepositoryFile::path).distinct().count() != files.size()
            || pullRequests.stream().map(RepositoryPullRequest::providerPullRequestId).distinct().count() != pullRequests.size()
            || issues.stream().map(RepositoryIssue::providerIssueId).distinct().count() != issues.size()
            || documents.stream().map(value -> value.documentType() + ":" + value.path()).distinct().count() != documents.size()) {
            throw new IllegalArgumentException("Repository snapshot facts contain duplicates");
        }
    }

    public static RepositorySnapshot ready(
        UUID repositoryId,
        UUID userId,
        String sourceRevision,
        Instant capturedAt,
        List<RepositoryBranch> branches,
        List<RepositoryCommit> commits,
        List<RepositoryLanguage> languages,
        List<RepositoryDependency> dependencies,
        List<RepositoryFile> files
    ) {
        return ready(repositoryId, userId, sourceRevision, capturedAt, branches, commits, languages,
            dependencies, files, List.of(), List.of(), List.of());
    }

    public static RepositorySnapshot ready(
        UUID repositoryId,
        UUID userId,
        String sourceRevision,
        Instant capturedAt,
        List<RepositoryBranch> branches,
        List<RepositoryCommit> commits,
        List<RepositoryLanguage> languages,
        List<RepositoryDependency> dependencies,
        List<RepositoryFile> files,
        List<RepositoryPullRequest> pullRequests,
        List<RepositoryIssue> issues,
        List<RepositoryDocument> documents
    ) {
        return new RepositorySnapshot(
            UUID.randomUUID(), repositoryId, userId, sourceRevision, capturedAt,
            "READY", hash(sourceRevision, branches, commits, languages, dependencies, files,
                pullRequests, issues, documents), "ACTIVE",
            branches, commits, languages, dependencies, files, pullRequests, issues, documents
        );
    }

    public static RepositorySnapshot ready(
        UUID repositoryId, UUID userId, String sourceRevision, Instant capturedAt,
        List<RepositoryBranch> branches, List<RepositoryCommit> commits
    ) {
        return ready(repositoryId, userId, sourceRevision, capturedAt, branches, commits, List.of(), List.of(), List.of());
    }

    public static RepositorySnapshot ready(
        UUID repositoryId, UUID userId, String sourceRevision, Instant capturedAt,
        List<RepositoryBranch> branches, List<RepositoryCommit> commits, List<RepositoryLanguage> languages
    ) {
        return ready(repositoryId, userId, sourceRevision, capturedAt, branches, commits, languages, List.of(), List.of());
    }

    public static RepositorySnapshot ready(
        UUID repositoryId, UUID userId, String sourceRevision, Instant capturedAt,
        List<RepositoryBranch> branches, List<RepositoryCommit> commits,
        List<RepositoryLanguage> languages, List<RepositoryDependency> dependencies
    ) {
        return ready(repositoryId, userId, sourceRevision, capturedAt, branches, commits, languages, dependencies, List.of());
    }

    private static String hash(
        String sourceRevision,
        List<RepositoryBranch> branches,
        List<RepositoryCommit> commits,
        List<RepositoryLanguage> languages,
        List<RepositoryDependency> dependencies,
        List<RepositoryFile> files,
        List<RepositoryPullRequest> pullRequests,
        List<RepositoryIssue> issues,
        List<RepositoryDocument> documents
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(sourceRevision.getBytes(StandardCharsets.UTF_8));
            branches.stream().sorted(java.util.Comparator.comparing(RepositoryBranch::name)).forEach(branch ->
                digest.update((branch.name() + ":" + branch.headCommitSha() + ":" + branch.defaultBranch())
                    .getBytes(StandardCharsets.UTF_8))
            );
            commits.stream().sorted(java.util.Comparator.comparing(RepositoryCommit::sha)).forEach(commit ->
                digest.update((commit.sha() + ":" + commit.committedAt()).getBytes(StandardCharsets.UTF_8))
            );
            languages.stream().sorted(java.util.Comparator.comparing(RepositoryLanguage::providerLabel)).forEach(language ->
                digest.update((language.providerLabel() + ":" + language.byteCount() + ":" + language.percentage())
                    .getBytes(StandardCharsets.UTF_8))
            );
            dependencies.stream().sorted(java.util.Comparator.comparing(RepositoryDependency::manifestPath)
                .thenComparing(RepositoryDependency::packageName)).forEach(dependency ->
                digest.update((dependency.ecosystem() + ":" + dependency.packageName() + ":"
                    + dependency.versionConstraint() + ":" + dependency.scope() + ":" + dependency.manifestPath())
                    .getBytes(StandardCharsets.UTF_8))
            );
            files.stream().sorted(java.util.Comparator.comparing(RepositoryFile::path)).forEach(file ->
                digest.update((file.path() + ":" + file.blobSha() + ":" + file.byteSize())
                    .getBytes(StandardCharsets.UTF_8))
            );
            pullRequests.stream().sorted(java.util.Comparator.comparing(RepositoryPullRequest::providerPullRequestId))
                .forEach(value -> digest.update((value.providerPullRequestId() + ":" + value.status() + ":"
                    + value.openedAt() + ":" + value.closedAt() + ":" + value.mergedAt() + ":" + value.reviewCount())
                    .getBytes(StandardCharsets.UTF_8)));
            issues.stream().sorted(java.util.Comparator.comparing(RepositoryIssue::providerIssueId))
                .forEach(value -> digest.update((value.providerIssueId() + ":" + value.status() + ":"
                    + value.openedAt() + ":" + value.closedAt() + ":" + String.join(",", value.labels()))
                    .getBytes(StandardCharsets.UTF_8)));
            documents.stream().sorted(java.util.Comparator.comparing(RepositoryDocument::path))
                .forEach(value -> digest.update((value.documentType() + ":" + value.path() + ":"
                    + value.contentHash() + ":" + value.byteSize() + ":" + String.join(",", value.qualitySignals()))
                    .getBytes(StandardCharsets.UTF_8)));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
