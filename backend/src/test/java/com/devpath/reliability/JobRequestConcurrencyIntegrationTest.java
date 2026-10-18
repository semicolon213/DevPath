package com.devpath.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.ai.application.GenerationPersistencePort;
import com.devpath.analysis.application.AnalysisApplicationService;
import com.devpath.analysis.application.AnalysisJobView;
import com.devpath.analysis.application.AnalysisPersistencePort;
import com.devpath.repository.application.RepositorySyncJobView;
import com.devpath.repository.application.RepositorySynchronizationApplicationService;
import com.devpath.repository.application.RepositorySynchronizationPersistencePort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
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
    @Autowired RepositorySynchronizationPersistencePort repositoryJobs;
    @Autowired AnalysisPersistencePort analysisJobs;
    @Autowired GenerationPersistencePort aiJobs;
    @Autowired PlatformTransactionManager transactionManager;

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

    @Test
    void claimsSkipRowsLockedByAnotherWorker() throws Exception {
        jdbc.update("delete from ai_tasks");
        jdbc.update("delete from analysis_jobs");
        jdbc.update("delete from repository_sync_jobs");
        Seed seed = seed();
        Instant claimAt = Instant.now().plusSeconds(60);
        UUID syncJobId = repositorySync.request(seed.userId(), seed.repositoryId(), "locked-sync").jobId();
        UUID analysisJobId = analyses.request(seed.userId(), seed.repositoryId(), seed.snapshotId(),
            AnalysisApplicationService.REPOSITORY_BASELINE, "locked-analysis").jobId();
        UUID aiJobId = UUID.randomUUID();
        withoutForeignKeys(() -> jdbc.update("""
            insert into ai_tasks(ai_task_id,user_id,prompt_context_id,idempotency_key,task_type,status,
                validation_status,attempt_count,failure_code,version,requested_at,started_at,completed_at,next_attempt_at)
            values (?,?,?,'locked-ai','SKILL_ANALYSIS_EXPLANATION','QUEUED','PENDING',0,null,0,?,null,null,?)
            """, aiJobId, seed.userId(), UUID.randomUUID(), Timestamp.from(claimAt.minusSeconds(1)),
            Timestamp.from(claimAt.minusSeconds(1))));

        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(4)) {
            Future<?> holder = executor.submit(() -> transaction.executeWithoutResult(ignored -> {
                jdbc.queryForObject("select job_id from repository_sync_jobs where job_id = ? for update",
                    UUID.class, syncJobId);
                jdbc.queryForObject("select job_id from analysis_jobs where job_id = ? for update",
                    UUID.class, analysisJobId);
                jdbc.queryForObject("select ai_task_id from ai_tasks where ai_task_id = ? for update",
                    UUID.class, aiJobId);
                locked.countDown();
                await(release);
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Boolean> syncClaim = executor.submit(() ->
                transaction.execute(ignored -> repositoryJobs.findNextClaimable(claimAt).isEmpty()));
            Future<Boolean> analysisClaim = executor.submit(() ->
                transaction.execute(ignored -> analysisJobs.findNextClaimable(claimAt).isEmpty()));
            Future<Boolean> aiClaim = executor.submit(() ->
                transaction.execute(ignored -> aiJobs.claim(claimAt, 2).isEmpty()));

            assertThat(syncClaim.get(2, TimeUnit.SECONDS)).isTrue();
            assertThat(analysisClaim.get(2, TimeUnit.SECONDS)).isTrue();
            assertThat(aiClaim.get(2, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
        }
    }

    @Test
    void claimsNextRowsWhenOldestRowsAreLockedByAnotherWorker() throws Exception {
        jdbc.update("delete from ai_tasks");
        jdbc.update("delete from analysis_jobs");
        jdbc.update("delete from repository_sync_jobs");
        Seed oldest = seed();
        Seed next = seed();
        Instant claimAt = Instant.now().plusSeconds(60);
        UUID oldestSyncId = repositorySync.request(oldest.userId(), oldest.repositoryId(), "oldest-sync").jobId();
        UUID nextSyncId = repositorySync.request(next.userId(), next.repositoryId(), "next-sync").jobId();
        UUID oldestAnalysisId = analyses.request(oldest.userId(), oldest.repositoryId(), oldest.snapshotId(),
            AnalysisApplicationService.REPOSITORY_BASELINE, "oldest-analysis").jobId();
        UUID nextAnalysisId = analyses.request(next.userId(), next.repositoryId(), next.snapshotId(),
            AnalysisApplicationService.REPOSITORY_BASELINE, "next-analysis").jobId();
        UUID oldestAiId = UUID.randomUUID();
        UUID nextAiId = UUID.randomUUID();
        Instant oldestAt = claimAt.minusSeconds(20);
        Instant nextAt = claimAt.minusSeconds(10);
        jdbc.update("update repository_sync_jobs set submitted_at = ?, next_attempt_at = ? where job_id = ?",
            Timestamp.from(oldestAt), Timestamp.from(oldestAt), oldestSyncId);
        jdbc.update("update repository_sync_jobs set submitted_at = ?, next_attempt_at = ? where job_id = ?",
            Timestamp.from(nextAt), Timestamp.from(nextAt), nextSyncId);
        jdbc.update("update analysis_jobs set submitted_at = ?, next_attempt_at = ? where job_id = ?",
            Timestamp.from(oldestAt), Timestamp.from(oldestAt), oldestAnalysisId);
        jdbc.update("update analysis_jobs set submitted_at = ?, next_attempt_at = ? where job_id = ?",
            Timestamp.from(nextAt), Timestamp.from(nextAt), nextAnalysisId);
        seedAiJob(oldestAiId, oldest.userId(), oldestAt, "oldest-ai");
        seedAiJob(nextAiId, next.userId(), nextAt, "next-ai");

        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(4)) {
            Future<?> holder = executor.submit(() -> transaction.executeWithoutResult(ignored -> {
                jdbc.queryForObject("select job_id from repository_sync_jobs where job_id = ? for update",
                    UUID.class, oldestSyncId);
                jdbc.queryForObject("select job_id from analysis_jobs where job_id = ? for update",
                    UUID.class, oldestAnalysisId);
                jdbc.queryForObject("select ai_task_id from ai_tasks where ai_task_id = ? for update",
                    UUID.class, oldestAiId);
                locked.countDown();
                await(release);
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<UUID> syncClaim = executor.submit(() -> transaction.execute(ignored ->
                repositoryJobs.findNextClaimable(claimAt).orElseThrow().id()));
            Future<UUID> analysisClaim = executor.submit(() -> transaction.execute(ignored ->
                analysisJobs.findNextClaimable(claimAt).orElseThrow().id()));
            Future<UUID> aiClaim = executor.submit(() -> transaction.execute(ignored ->
                aiJobs.claim(claimAt, 2).orElseThrow().job().id()));

            assertThat(syncClaim.get(2, TimeUnit.SECONDS)).isEqualTo(nextSyncId);
            assertThat(analysisClaim.get(2, TimeUnit.SECONDS)).isEqualTo(nextAnalysisId);
            assertThat(aiClaim.get(2, TimeUnit.SECONDS)).isEqualTo(nextAiId);
            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
        }
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

    private void withoutForeignKeys(Runnable action) {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(ignored -> {
            jdbc.execute("set session_replication_role = replica");
            try {
                action.run();
            } finally {
                jdbc.execute("set session_replication_role = origin");
            }
        });
    }

    private void seedAiJob(UUID jobId, UUID userId, Instant requestedAt, String idempotencyKey) {
        UUID contextId = UUID.randomUUID();
        withoutForeignKeys(() -> {
            jdbc.update("""
                insert into prompt_contexts(prompt_context_id,user_id,prompt_template_version_id,skill_matrix_id,
                    task_type,token_budget,context_hash,context_payload,provider_prompt,status,locked_at)
                values (?,?,?,?,'SKILL_ANALYSIS_EXPLANATION',100,?,cast(? as jsonb),?,'LOCKED',?)
                """, contextId, userId, UUID.fromString("7bb42957-8dbc-4cc6-99f5-0fb23b11760d"), UUID.randomUUID(),
                "c".repeat(64), "{}", "test prompt", Timestamp.from(requestedAt));
            jdbc.update("""
                insert into ai_tasks(ai_task_id,user_id,prompt_context_id,idempotency_key,task_type,status,
                    validation_status,attempt_count,failure_code,version,requested_at,started_at,completed_at,next_attempt_at)
                values (?,?,?,?,'SKILL_ANALYSIS_EXPLANATION','QUEUED','PENDING',0,null,0,?,null,null,?)
                """, jobId, userId, contextId, idempotencyKey, Timestamp.from(requestedAt),
                Timestamp.from(requestedAt));
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for test release");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for test release", exception);
        }
    }

    private record Seed(UUID userId, UUID repositoryId, UUID snapshotId) {}
}
