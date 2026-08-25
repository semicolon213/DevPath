package com.devpath.repository.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositorySnapshotTest {
    @Test
    void createsAStableImmutableHashIndependentOfFactOrdering() {
        UUID repositoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        String firstSha = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String secondSha = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        var branches = List.of(
            new RepositoryBranch("main", firstSha, true),
            new RepositoryBranch("feature", secondSha, false)
        );
        var commits = List.of(
            new RepositoryCommit(firstSha, "owner", now, "first"),
            new RepositoryCommit(secondSha, null, now.minusSeconds(10), "second")
        );
        var languages = List.of(
            RepositoryLanguage.normalize("Java", 7500, 10000),
            RepositoryLanguage.normalize("TypeScript", 2500, 10000)
        );
        var dependencies = List.of(
            RepositoryDependency.normalized("npm", "react", "18.3.1", "RUNTIME", "frontend/package.json"),
            RepositoryDependency.normalized("gradle", "org.postgresql:postgresql", "42.7.4", "RUNTIME", "backend/build.gradle")
        );
        var files = List.of(
            RepositoryFile.normalized("backend/build.gradle", "cccccccccccccccccccccccccccccccccccccccc", 200),
            RepositoryFile.normalized("frontend/package.json", "dddddddddddddddddddddddddddddddddddddddd", 100)
        );

        RepositorySnapshot first = RepositorySnapshot.ready(
            repositoryId, userId, firstSha, now, branches, commits, languages, dependencies, files
        );
        RepositorySnapshot reordered = RepositorySnapshot.ready(
            repositoryId, userId, firstSha, now, branches.reversed(), commits.reversed(),
            languages.reversed(), dependencies.reversed(), files.reversed()
        );

        assertThat(first.contentHash()).isEqualTo(reordered.contentHash()).hasSize(64);
        assertThat(first.status()).isEqualTo("READY");
        assertThat(first.branches()).isUnmodifiable();
        assertThat(first.languages().get(0).percentage()).isEqualByComparingTo("75.0000");
        assertThat(first.dependencies()).isUnmodifiable();
        assertThat(first.files()).isUnmodifiable();
    }
}
