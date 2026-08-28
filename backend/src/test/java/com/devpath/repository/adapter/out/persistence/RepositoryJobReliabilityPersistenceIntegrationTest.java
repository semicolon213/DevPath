package com.devpath.repository.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.repository.domain.RepositorySyncJob;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
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
class RepositoryJobReliabilityPersistenceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired RepositorySyncJobJpaRepository jobs;

    @Test
    void persistsOneHundredQueuedJobsAndMakesAnExpiredLeaseClaimableAgain() {
        UUID userId = seedOwner();
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        var entities = new ArrayList<RepositorySyncJobJpaEntity>();
        UUID staleJobId = null;
        for (int index = 0; index < 100; index++) {
            UUID repositoryId = seedRepository(userId, index, now);
            Instant submittedAt = index == 0 ? now : now.plusSeconds(600);
            RepositorySyncJob job = RepositorySyncJob.queue(
                userId, repositoryId, "load-sync-" + index, submittedAt);
            if (index == 0) {
                job = job.claim(now, Duration.ofMinutes(5));
                staleJobId = job.id();
            }
            entities.add(new RepositorySyncJobJpaEntity(job));
        }

        jobs.saveAllAndFlush(entities);
        UUID expectedStaleJobId = staleJobId;

        assertThat(jobs.findAllByUserIdOrderBySubmittedAtDescIdDesc(userId, PageRequest.of(0, 100)))
            .hasSize(100);
        assertThat(jobs.findClaimable(now.plusSeconds(301), PageRequest.of(0, 1)))
            .singleElement().satisfies(candidate ->
                assertThat(candidate.toDomain().id()).isEqualTo(expectedStaleJobId));
    }

    private UUID seedOwner() {
        UUID userId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        Timestamp timestamp = Timestamp.from(now);
        jdbc.update("insert into users(user_id, account_status, display_name, created_at, updated_at) values (?, 'ACTIVE', 'Load Owner', ?, ?)",
            userId, timestamp, timestamp);
        jdbc.update("insert into external_identities(external_identity_id, user_id, provider, provider_subject, linked_at, updated_at) values (?, ?, 'GITHUB', ?, ?, ?)",
            identityId, userId, UUID.randomUUID().toString(), timestamp, timestamp);
        return userId;
    }

    private UUID seedRepository(UUID userId, int index, Instant now) {
        UUID repositoryId = UUID.randomUUID();
        UUID identityId = jdbc.queryForObject(
            "select external_identity_id from external_identities where user_id = ?", UUID.class, userId);
        Timestamp timestamp = Timestamp.from(now);
        jdbc.update("insert into repositories(repository_id, user_id, external_identity_id, provider, provider_repository_id, repository_name, full_name, owner_login, visibility, default_branch, provider_archived, lifecycle_status, sync_status, html_url, discovered_at, updated_at) values (?, ?, ?, 'GITHUB', ?, ?, ?, 'owner', 'PRIVATE', 'main', false, 'ACTIVE', 'NOT_SYNCED', ?, ?, ?)",
            repositoryId, userId, identityId, Integer.toString(index), "repo-" + index, "owner/repo-" + index,
            "https://github.com/owner/repo-" + index, timestamp, timestamp);
        return repositoryId;
    }
}
