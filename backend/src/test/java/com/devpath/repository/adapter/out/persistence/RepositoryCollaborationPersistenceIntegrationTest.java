package com.devpath.repository.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.repository.domain.RepositoryDocument;
import com.devpath.repository.domain.RepositoryIssue;
import com.devpath.repository.domain.RepositoryPullRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RepositoryCollaborationPersistenceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired RepositoryPullRequestJpaRepository pullRequests;
    @Autowired RepositoryIssueJpaRepository issues;
    @Autowired RepositoryDocumentJpaRepository documents;

    @Test
    void roundTripsImmutableCollaborationAndDocumentFactsAgainstFlywaySchema() {
        UUID snapshotId = seedSnapshot();
        Instant openedAt = Instant.parse("2026-08-01T00:00:00Z");
        Instant closedAt = Instant.parse("2026-08-03T00:00:00Z");
        pullRequests.saveAndFlush(new RepositoryPullRequestJpaEntity(snapshotId,
            new RepositoryPullRequest("501", "MERGED", openedAt, closedAt, closedAt, 2)));
        issues.saveAndFlush(new RepositoryIssueJpaEntity(snapshotId,
            new RepositoryIssue("601", "CLOSED", List.of("bug", "backend"), openedAt, closedAt)));
        documents.saveAndFlush(new RepositoryDocumentJpaEntity(snapshotId,
            new RepositoryDocument("README", "README.md", "c".repeat(64), 120,
                List.of("OVERVIEW", "SETUP", "USAGE"))));

        assertThat(pullRequests.findAllBySnapshotIdOrderByOpenedAtDesc(snapshotId))
            .singleElement().extracting(value -> value.toDomain().reviewCount()).isEqualTo(2);
        assertThat(issues.findAllBySnapshotIdOrderByOpenedAtDesc(snapshotId))
            .singleElement().satisfies(value -> assertThat(value.toDomain().labels()).containsExactly("backend", "bug"));
        assertThat(documents.findAllBySnapshotIdOrderByDocumentTypeAscPathAsc(snapshotId))
            .singleElement().satisfies(value -> assertThat(value.toDomain().qualitySignals())
                .containsExactly("OVERVIEW", "SETUP", "USAGE"));
    }

    private UUID seedSnapshot() {
        UUID userId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        java.sql.Timestamp timestamp = java.sql.Timestamp.from(now);
        jdbc.update("insert into users(user_id, account_status, display_name, created_at, updated_at) values (?, 'ACTIVE', 'Owner', ?, ?)", userId, timestamp, timestamp);
        jdbc.update("insert into external_identities(external_identity_id, user_id, provider, provider_subject, linked_at, updated_at) values (?, ?, 'GITHUB', ?, ?, ?)", identityId, userId, UUID.randomUUID().toString(), timestamp, timestamp);
        jdbc.update("insert into repositories(repository_id, user_id, external_identity_id, provider, provider_repository_id, repository_name, full_name, owner_login, visibility, default_branch, provider_archived, lifecycle_status, sync_status, html_url, discovered_at, updated_at) values (?, ?, ?, 'GITHUB', '42', 'repo', 'owner/repo', 'owner', 'PRIVATE', 'main', false, 'ACTIVE', 'SYNCHRONIZED', 'https://github.com/owner/repo', ?, ?)", repositoryId, userId, identityId, timestamp, timestamp);
        jdbc.update("insert into repository_snapshots(snapshot_id, repository_id, user_id, source_revision, captured_at, snapshot_status, content_hash, branch_count, commit_count, retention_status) values (?, ?, ?, ?, ?, 'READY', ?, 0, 0, 'ACTIVE')", snapshotId, repositoryId, userId, "a".repeat(40), timestamp, "b".repeat(64));
        return snapshotId;
    }
}
