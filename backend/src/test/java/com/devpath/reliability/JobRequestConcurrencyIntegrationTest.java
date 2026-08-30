package com.devpath.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.analysis.application.AnalysisApplicationService;
import com.devpath.analysis.application.AnalysisJobView;
import com.devpath.repository.application.RepositorySyncJobView;
import com.devpath.repository.application.RepositorySynchronizationApplicationService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
@Testcontainers(disabledWithoutDocker = true)
class JobRequestConcurrencyIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new com.devpath.test.PgVectorPostgreSQLContainer();
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired RepositorySynchronizationApplicationService repositorySync;
    @Autowired AnalysisApplicationService analyses;

    @Test
    void concurrentEquivalentCommandsReturnOneDurableJobPerBasis() throws Exception {
        Seed seed = seed();
        var ready = new CountDownLatch(4);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Future<UUID>> repositoryRequests = List.of(
                executor.submit(repositoryRequest(seed, "concurrent-sync-a", ready, start)),
                executor.submit(repositoryRequest(seed, "concurrent-sync-b", ready, start))
            );
            List<Future<UUID>> analysisRequests = List.of(
                executor.submit(analysisRequest(seed, "concurrent-analysis-a", ready, start)),
                executor.submit(analysisRequest(seed, "concurrent-analysis-b", ready, start))
            );
            ready.await();
            start.countDown();

            assertThat(repositoryRequests.get(0).get()).isEqualTo(repositoryRequests.get(1).get());
            assertThat(analysisRequests.get(0).get()).isEqualTo(analysisRequests.get(1).get());
        }

        assertThat(jdbc.queryForObject(
            "select count(*) from repository_sync_jobs where repository_id = ?", Integer.class, seed.repositoryId()))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "select count(*) from analysis_jobs where snapshot_id = ?", Integer.class, seed.snapshotId()))
            .isEqualTo(1);
    }

    private Callable<UUID> repositoryRequest(Seed seed, String key, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown(); start.await();
            RepositorySyncJobView job = repositorySync.request(seed.userId(), seed.repositoryId(), key);
            return job.jobId();
        };
    }

    private Callable<UUID> analysisRequest(Seed seed, String key, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown(); start.await();
            AnalysisJobView job = analyses.request(seed.userId(), seed.repositoryId(), seed.snapshotId(),
                AnalysisApplicationService.REPOSITORY_BASELINE, key);
            return job.jobId();
        };
    }

    private Seed seed() {
        UUID userId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        Timestamp timestamp = Timestamp.from(now);
        jdbc.update("insert into users(user_id, account_status, display_name, created_at, updated_at) values (?, 'ACTIVE', 'Concurrent Owner', ?, ?)",
            userId, timestamp, timestamp);
        jdbc.update("insert into external_identities(external_identity_id, user_id, provider, provider_subject, linked_at, updated_at) values (?, ?, 'GITHUB', ?, ?, ?)",
            identityId, userId, UUID.randomUUID().toString(), timestamp, timestamp);
        jdbc.update("insert into repositories(repository_id, user_id, external_identity_id, provider, provider_repository_id, repository_name, full_name, owner_login, visibility, default_branch, provider_archived, lifecycle_status, sync_status, html_url, discovered_at, updated_at) values (?, ?, ?, 'GITHUB', 'concurrent-42', 'repo', 'owner/repo', 'owner', 'PRIVATE', 'main', false, 'ACTIVE', 'SYNCHRONIZED', 'https://github.com/owner/repo', ?, ?)",
            repositoryId, userId, identityId, timestamp, timestamp);
        jdbc.update("insert into repository_snapshots(snapshot_id, repository_id, user_id, source_revision, captured_at, snapshot_status, content_hash, branch_count, commit_count, retention_status) values (?, ?, ?, ?, ?, 'READY', ?, 0, 0, 'ACTIVE')",
            snapshotId, repositoryId, userId, "a".repeat(40), timestamp, "b".repeat(64));
        jdbc.update("update repositories set current_snapshot_id = ?, last_synced_at = ? where repository_id = ?",
            snapshotId, timestamp, repositoryId);
        return new Seed(userId, repositoryId, snapshotId);
    }

    private record Seed(UUID userId, UUID repositoryId, UUID snapshotId) {}
}
