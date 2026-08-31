package com.devpath.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.ai.application.StoredPromptContext;
import com.devpath.ai.domain.GenerationJob;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

    @Test
    void immutableContextExecutionValidationAndArtifactRemainOwnerScoped() {
        Instant now = Instant.parse("2026-08-31T00:00:00Z");
        UUID owner = UUID.randomUUID(), other = UUID.randomUUID(), matrix = UUID.randomUUID(), context = UUID.randomUUID();
        insertUser(owner, now); insertUser(other, now);
        insertMatrix(matrix, owner, now);
        var template = persistence.loadActiveTemplate("SKILL_ANALYSIS_EXPLANATION");
        persistence.saveContext(new StoredPromptContext(context, owner, template.id(), matrix,
            "SKILL_ANALYSIS_EXPLANATION", 4096, "a".repeat(64), "{}", "safe prompt", now));
        var queued = persistence.saveJob(GenerationJob.queue(owner, context, "key", "SKILL_ANALYSIS_EXPLANATION", now));

        var item = persistence.claim(now.plusSeconds(1)).orElseThrow();
        UUID execution = persistence.saveSubmittedExecution(item, "OLLAMA", "qwen-test", now.plusSeconds(1));
        persistence.completeExecution(execution, 12, 10, 5, now.plusSeconds(2));
        UUID artifact = UUID.randomUUID();
        persistence.saveValidatedArtifact(item, execution, artifact, "object://" + owner + "/artifact", now.plusSeconds(2));

        assertThat(persistence.findJob(owner, queued.id()).orElseThrow().status().name()).isEqualTo("SUCCEEDED");
        assertThat(persistence.findArtifact(owner, artifact)).isPresent().get().satisfies(value -> {
            assertThat(value.skillMatrixId()).isEqualTo(matrix);
            assertThat(value.contextHash()).isEqualTo("a".repeat(64));
            assertThat(value.violations()).isEmpty();
        });
        assertThat(persistence.findArtifact(other, artifact)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from ai_responses where validation_status='PASSED'",
            Integer.class)).isEqualTo(1);
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
}
