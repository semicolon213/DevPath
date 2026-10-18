package com.devpath.repository.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devpath.repository.domain.Repository;
import com.devpath.repository.domain.RepositoryBranch;
import com.devpath.repository.domain.RepositoryCommit;
import com.devpath.repository.domain.RepositoryDependency;
import com.devpath.repository.domain.RepositoryDocument;
import com.devpath.repository.domain.RepositoryFile;
import com.devpath.repository.domain.RepositoryIssue;
import com.devpath.repository.domain.RepositoryLanguage;
import com.devpath.repository.domain.RepositoryPullRequest;
import com.devpath.repository.domain.RepositorySnapshot;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "devpath.runtime.worker-enabled=false",
    "devpath.security.frontend-origin=http://localhost:5100",
    "devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
@Testcontainers(disabledWithoutDocker = true)
class RepositorySnapshotReadPersistenceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new com.devpath.test.PgVectorPostgreSQLContainer();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired RepositoryPersistencePort repositories;
    @Autowired RepositorySynchronizationPersistencePort snapshots;
    @Autowired RepositorySynchronizationApplicationService synchronization;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void preservesSnapshotListTechnologyAndEvidenceViewsAcrossReadImplementations() {
        Instant olderAt = Instant.parse("2026-08-10T00:00:00Z");
        Instant newerAt = Instant.parse("2026-08-11T00:00:00Z");
        Repository repository = repository("owner", olderAt);
        RepositorySnapshot older = snapshot(repository, "a", olderAt, 1, 2, 3, 4, 5);
        RepositorySnapshot newer = snapshot(repository, "b", newerAt, 2, 3, 4, 5, 6);
        snapshots.saveSnapshot(older);
        snapshots.saveSnapshot(newer);
        repositories.save(repository.markSynchronized(newer.id(), newerAt));

        Repository otherRepository = repository("other", olderAt);
        snapshots.saveSnapshot(snapshot(otherRepository, "c", newerAt.plusSeconds(1), 3, 4, 5, 6, 7));

        assertThatThrownBy(() -> synchronization.listSnapshots(otherRepository.userId(), repository.id()))
            .isInstanceOf(RepositoryNotFoundException.class);
        assertThatThrownBy(() -> synchronization.getTechnologies(otherRepository.userId(), repository.id()))
            .isInstanceOf(RepositoryNotFoundException.class);
        assertThatThrownBy(() -> synchronization.getTechnologies(
            otherRepository.userId(), repository.id(), older.id()
        )).isInstanceOf(RepositoryNotFoundException.class);
        assertThatThrownBy(() -> synchronization.getEvidence(otherRepository.userId(), repository.id()))
            .isInstanceOf(RepositoryNotFoundException.class);

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        RepositorySnapshotListView snapshotList = synchronization.listSnapshots(repository.userId(), repository.id());
        assertThat(snapshotList).isEqualTo(new RepositorySnapshotListView(List.of(
                RepositorySnapshotView.from(newer), RepositorySnapshotView.from(older)
            )));
        assertThat(snapshotList.snapshots().get(0).branchCount()).isEqualTo(2);
        assertThat(snapshotList.snapshots().get(0).commitCount()).isEqualTo(3);
        assertThat(snapshotList.snapshots().get(0).pullRequestCount()).isEqualTo(4);
        assertThat(snapshotList.snapshots().get(0).issueCount()).isEqualTo(5);
        assertThat(snapshotList.snapshots().get(0).documentCount()).isEqualTo(6);
        assertThat(snapshotList.snapshots().get(1).branchCount()).isEqualTo(1);
        assertThat(snapshotList.snapshots().get(1).commitCount()).isEqualTo(2);
        assertThat(snapshotList.snapshots().get(1).pullRequestCount()).isEqualTo(3);
        assertThat(snapshotList.snapshots().get(1).issueCount()).isEqualTo(4);
        assertThat(snapshotList.snapshots().get(1).documentCount()).isEqualTo(5);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);

        statistics.clear();
        assertThat(synchronization.getTechnologies(repository.userId(), repository.id()))
            .isEqualTo(TechnologySummaryView.from(newer));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);

        statistics.clear();
        assertThat(synchronization.getTechnologies(repository.userId(), repository.id(), older.id()))
            .isEqualTo(TechnologySummaryView.from(older));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);

        statistics.clear();
        assertThat(synchronization.getEvidence(repository.userId(), repository.id()))
            .isEqualTo(RepositoryEvidenceSummaryView.from(newer));
        // Nine reads plus the audit entity existence check and durable insert.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(11);

        RepositorySnapshot evidence = snapshots.findEvidenceSnapshot(
            repository.userId(), repository.id(), newer.id()
        ).orElseThrow();
        assertThat(evidence.branches()).extracting(RepositoryBranch::name)
            .containsExactly("branch-01", "branch-02");
        assertThat(evidence.commits()).extracting(RepositoryCommit::sha)
            .containsExactly(commitSha("b", 2), commitSha("b", 1), commitSha("b", 0));
        assertThat(evidence.dependencies()).extracting(RepositoryDependency::manifestPath)
            .containsExactly("build.gradle", "pom.xml");
        assertThat(evidence.files()).extracting(RepositoryFile::path)
            .containsExactly(
                ".github/workflows/test.yml", "README.md",
                "src/main/java/domain/Model.java", "src/test/java/ModelTest.java"
            );
        assertThat(evidence.pullRequests()).extracting(RepositoryPullRequest::providerPullRequestId)
            .containsExactly("pr-b-3", "pr-b-2", "pr-b-1", "pr-b-0");
        assertThat(evidence.issues()).extracting(RepositoryIssue::providerIssueId)
            .containsExactly("issue-b-4", "issue-b-3", "issue-b-2", "issue-b-1", "issue-b-0");
        assertThat(evidence.documents()).extracting(RepositoryDocument::path)
            .containsExactly("README-01.md", "README-02.md", "README-03.md", "README-04.md", "README-05.md", "README-06.md");
    }

    private Repository repository(String owner, Instant now) {
        UUID userId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(now);
        jdbc.update("insert into users(user_id, account_status, display_name, created_at, updated_at) values (?, 'ACTIVE', ?, ?, ?)",
            userId, owner, timestamp, timestamp);
        jdbc.update("insert into external_identities(external_identity_id, user_id, provider, provider_subject, linked_at, updated_at) values (?, ?, 'GITHUB', ?, ?, ?)",
            identityId, userId, UUID.randomUUID().toString(), timestamp, timestamp);
        return repositories.save(Repository.discover(
            userId, identityId, UUID.randomUUID().toString(), "repo", owner + "/repo", owner,
            true, "main", false, "https://github.com/" + owner + "/repo", now
        ));
    }

    private RepositorySnapshot snapshot(
        Repository repository,
        String seed,
        Instant capturedAt,
        int branchCount,
        int commitCount,
        int pullRequestCount,
        int issueCount,
        int documentCount
    ) {
        String revision = seed.repeat(40);
        Instant openedAt = capturedAt.minusSeconds(172_800);
        return RepositorySnapshot.ready(
            repository.id(), repository.userId(), revision, capturedAt,
            IntStream.range(0, branchCount).mapToObj(index -> new RepositoryBranch(
                "branch-%02d".formatted(branchCount - index), commitSha(seed, index), index == 0
            )).toList(),
            IntStream.range(0, commitCount).mapToObj(index -> new RepositoryCommit(
                commitSha(seed, index), "developer-" + index,
                capturedAt.minusSeconds((commitCount - index) * 3_600L), "summary-" + index
            )).toList(),
            List.of(RepositoryLanguage.normalize("Java", 1_000, 1_000)),
            List.of(
                RepositoryDependency.normalized("gradle", "org.junit.jupiter:junit-jupiter", "5.11.3", "TEST", "build.gradle"),
                RepositoryDependency.normalized("maven", "org.postgresql:postgresql", "42.7.4", "RUNTIME", "pom.xml")
            ).reversed(),
            List.of(
                RepositoryFile.normalized(".github/workflows/test.yml", (seed + "e").repeat(20), 60),
                RepositoryFile.normalized("README.md", (seed + "f").repeat(20), 40),
                RepositoryFile.normalized("src/test/java/ModelTest.java", (seed + "d").repeat(20), 80),
                RepositoryFile.normalized("src/main/java/domain/Model.java", seed.repeat(40), 100)
            ),
            IntStream.range(0, pullRequestCount).mapToObj(index -> new RepositoryPullRequest(
                "pr-" + seed + "-" + index, "MERGED", openedAt.plusSeconds(index * 3_600L),
                capturedAt.minusSeconds(86_400), capturedAt.minusSeconds(86_400), index
            )).toList(),
            IntStream.range(0, issueCount).mapToObj(index -> new RepositoryIssue(
                "issue-" + seed + "-" + index, "CLOSED", List.of("backend"),
                openedAt.plusSeconds(index * 3_600L), capturedAt.minusSeconds(43_200)
            )).toList(),
            IntStream.range(0, documentCount).mapToObj(index -> new RepositoryDocument(
                "README", "README-%02d.md".formatted(documentCount - index), seed.repeat(64), 200 + index,
                List.of("OVERVIEW", "SETUP", "USAGE")
            )).toList()
        );
    }

    private String commitSha(String seed, int index) {
        return seed.repeat(39) + Integer.toHexString(index);
    }
}
