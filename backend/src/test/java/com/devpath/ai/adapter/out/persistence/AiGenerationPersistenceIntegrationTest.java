package com.devpath.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devpath.ai.application.StoredPromptContext;
import com.devpath.ai.domain.GenerationJob;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaGenerationPersistenceAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class AiGenerationPersistenceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new com.devpath.test.PgVectorPostgreSQLContainer();
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired JpaGenerationPersistenceAdapter persistence;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cancelWaitsForTheExistingOwnerScopedLock() throws Exception {
        Instant now = Instant.parse("2026-09-28T00:00:00Z");
        Instant canceledAt = now.plusSeconds(1);
        UUID owner = UUID.randomUUID(), matrix = UUID.randomUUID(), context = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.executeWithoutResult(ignored -> {
            insertUser(owner, now);
            insertMatrix(matrix, owner, now);
            var template = persistence.loadActiveTemplate("SKILL_ANALYSIS_EXPLANATION");
            persistence.saveContext(new StoredPromptContext(context, owner, template.id(), matrix, null, null, null,
                "SKILL_ANALYSIS_EXPLANATION", 4096, "c".repeat(64), "{}", "safe prompt", now));
            persistence.saveJob(new GenerationJob(jobId, owner, context, "cancel-lock-" + jobId,
                "SKILL_ANALYSIS_EXPLANATION", com.devpath.ai.domain.GenerationJobStatus.QUEUED, "PENDING", 0,
                null, now, null, null, now));
        });

        var locked = new CountDownLatch(1);
        var attempted = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        Future<?> holder = null;
        Future<GenerationJob> waiter = null;
        try {
            holder = executor.submit(() -> {
                transaction.executeWithoutResult(ignored -> {
                    var current = persistence.findJobForUpdate(owner, jobId).orElseThrow();
                    locked.countDown();
                    await(release);
                    persistence.saveJob(current.cancel(canceledAt));
                });
            });
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

            waiter = executor.submit(() -> transaction.execute(ignored -> {
                attempted.countDown();
                return persistence.findJobForUpdate(owner, jobId).orElseThrow();
            }));
            assertThat(attempted.await(5, TimeUnit.SECONDS)).isTrue();
            var waiting = waiter;
            assertThatThrownBy(() -> waiting.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);

            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
            var canceled = waiter.get(5, TimeUnit.SECONDS);
            assertThat(canceled.status().name()).isEqualTo("CANCELED");
            assertThat(canceled.completedAt()).isEqualTo(canceledAt);
        } finally {
            release.countDown();
            if (holder != null) holder.cancel(true);
            if (waiter != null) waiter.cancel(true);
            executor.shutdownNow();
            try {
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                transaction.executeWithoutResult(ignored -> {
                    jdbc.update("delete from ai_tasks where ai_task_id = ?", jobId);
                    jdbc.update("delete from prompt_contexts where prompt_context_id = ?", context);
                    jdbc.update("delete from skill_matrices where skill_matrix_id = ?", matrix);
                    jdbc.update("delete from users where user_id = ?", owner);
                });
            }
        }
    }

    @Test
    void immutableContextExecutionValidationAndArtifactRemainOwnerScoped() {
        Instant now = Instant.parse("2026-08-31T00:00:00Z");
        UUID owner = UUID.randomUUID(), other = UUID.randomUUID(), matrix = UUID.randomUUID(), context = UUID.randomUUID();
        insertUser(owner, now); insertUser(other, now);
        insertMatrix(matrix, owner, now);
        var template = persistence.loadActiveTemplate("SKILL_ANALYSIS_EXPLANATION");
        persistence.saveContext(new StoredPromptContext(context, owner, template.id(), matrix, null, null, null,
            "SKILL_ANALYSIS_EXPLANATION", 4096, "a".repeat(64), "{}", "safe prompt", now));
        var queued = persistence.saveJob(GenerationJob.queue(owner, context, "key", "SKILL_ANALYSIS_EXPLANATION", now));

        var item = persistence.claim(now.plusSeconds(1), 2).orElseThrow();
        UUID execution = persistence.saveSubmittedExecution(item, "OLLAMA", "qwen-test", now.plusSeconds(1));
        persistence.completeExecution(execution, 12, 10, 5, now.plusSeconds(2));
        UUID artifact = UUID.randomUUID();
        persistence.saveValidatedArtifact(item, execution, artifact, "object://" + owner + "/artifact", now.plusSeconds(2));

        assertThat(persistence.findJob(owner, queued.id()).orElseThrow().status().name()).isEqualTo("SUCCEEDED");
        assertThat(persistence.findJob(other, queued.id())).isEmpty();
        assertThat(persistence.findJobForUpdate(other, queued.id())).isEmpty();
        assertThat(persistence.findJobForUpdate(owner, queued.id())).isPresent().get().satisfies(value ->
            assertThat(value.id()).isEqualTo(queued.id()));
        assertThat(persistence.findByOwnerAndIdempotencyKey(other, "key")).isEmpty();
        assertThat(persistence.findArtifactIdByPromptContext(owner, context)).contains(artifact);
        assertThat(persistence.findArtifactIdByPromptContext(other, context)).isEmpty();
        assertThat(persistence.findArtifact(owner, artifact)).isPresent().get().satisfies(value -> {
            assertThat(value.skillMatrixId()).isEqualTo(matrix);
            assertThat(value.contextHash()).isEqualTo("a".repeat(64));
            assertThat(value.violations()).isEmpty();
        });
        assertThat(persistence.findArtifact(other, artifact)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from ai_responses where validation_status='PASSED'",
            Integer.class)).isEqualTo(1);
    }

    @Test
    void repositoryReviewKeepsItsOwnedAnalysisProvenance() {
        Instant now = Instant.parse("2026-08-31T00:00:00Z");
        UUID owner = UUID.randomUUID(), matrix = UUID.randomUUID(), analysis = UUID.randomUUID(), context = UUID.randomUUID();
        insertUser(owner, now);
        insertMatrix(matrix, owner, now);
        insertAnalysis(analysis, matrix, owner, now);
        var template = persistence.loadActiveTemplate("REPOSITORY_REVIEW");
        persistence.saveContext(new StoredPromptContext(context, owner, template.id(), matrix, analysis, null, null,
            "REPOSITORY_REVIEW", 8192, "b".repeat(64), "{}", "safe review prompt", now));
        persistence.saveJob(GenerationJob.queue(owner, context, "review-key", "REPOSITORY_REVIEW", now));

        var item = persistence.claim(now.plusSeconds(1), 2).orElseThrow();
        UUID execution = persistence.saveSubmittedExecution(item, "OLLAMA", "qwen-test", now.plusSeconds(1));
        persistence.completeExecution(execution, 12, 10, 5, now.plusSeconds(2));
        UUID artifact = UUID.randomUUID();
        persistence.saveValidatedArtifact(item, execution, artifact, "object://review", now.plusSeconds(2));

        assertThat(persistence.findArtifact(owner, artifact)).isPresent().get().satisfies(value -> {
            assertThat(value.analysisId()).isEqualTo(analysis);
            assertThat(value.artifactType()).isEqualTo("REPOSITORY_REVIEW");
            assertThat(value.validatorVersion()).isEqualTo("repository-review-validator-v2");
        });
    }

    @Test
    void reclaimsExpiredRunningTaskAndFailsItWhenTheFinalLeaseExpires() {
        Instant now = Instant.parse("2026-08-31T00:00:00Z");
        UUID owner = UUID.randomUUID(), matrix = UUID.randomUUID(), context = UUID.randomUUID();
        insertUser(owner, now);
        insertMatrix(matrix, owner, now);
        var template = persistence.loadActiveTemplate("SKILL_ANALYSIS_EXPLANATION");
        persistence.saveContext(new StoredPromptContext(context, owner, template.id(), matrix, null, null, null,
            "SKILL_ANALYSIS_EXPLANATION", 4096, "d".repeat(64), "{}", "safe prompt", now));
        persistence.saveJob(GenerationJob.queue(owner, context, "lease-recovery", "SKILL_ANALYSIS_EXPLANATION", now));

        var first = persistence.claim(now, 2).orElseThrow();
        assertThat(persistence.claim(first.job().nextAttemptAt().minusMillis(1), 2)).isEmpty();

        var recovered = persistence.claim(first.job().nextAttemptAt(), 2).orElseThrow();
        assertThat(recovered.job().status().name()).isEqualTo("RUNNING");
        assertThat(recovered.job().attemptCount()).isEqualTo(2);
        assertThat(recovered.job().failureCode()).isEqualTo("WORKER_LEASE_RECOVERED");

        var exhausted = persistence.claim(recovered.job().nextAttemptAt(), 2).orElseThrow();
        assertThat(exhausted.job().status().name()).isEqualTo("FAILED");
        assertThat(exhausted.job().failureCode()).isEqualTo("WORKER_LEASE_EXPIRED");
        assertThat(exhausted.job().completedAt()).isEqualTo(recovered.job().nextAttemptAt());
        assertThat(persistence.findJob(owner, exhausted.job().id())).contains(exhausted.job());
    }

    private void insertUser(UUID id, Instant now) {
        var timestamp = java.sql.Timestamp.from(now);
        jdbc.update("insert into users(user_id,account_status,display_name,created_at,updated_at,version) values (?,?,?,?,?,0)",
            id, "ACTIVE", "User", timestamp, timestamp);
    }

    private void insertMatrix(UUID id, UUID owner, Instant now) {
        jdbc.execute("set session_replication_role = replica");
        try {
            jdbc.update("insert into skill_matrices(skill_matrix_id,user_id,evaluation_id,skill_matrix_policy_id,policy_version,rule_set_version,status,generated_at,version) values (?,?,?,?,?,?,?,?,0)",
                id, owner, UUID.randomUUID(), UUID.randomUUID(), "skill-v1", "rule-v1", "CURRENT",
                java.sql.Timestamp.from(now));
        } finally {
            jdbc.execute("set session_replication_role = origin");
        }
    }

    private void insertAnalysis(UUID id, UUID matrix, UUID owner, Instant now) {
        jdbc.execute("set session_replication_role = replica");
        try {
            jdbc.update("insert into analysis_results(analysis_id,job_id,user_id,repository_id,snapshot_id,evaluation_id,skill_matrix_id,analysis_scope,completed_at) values (?,?,?,?,?,?,?,?,?)",
                id, UUID.randomUUID(), owner, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), matrix,
                "REPOSITORY_BASELINE", java.sql.Timestamp.from(now));
        } finally {
            jdbc.execute("set session_replication_role = origin");
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for test release");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for test release", exception);
        }
    }
}
