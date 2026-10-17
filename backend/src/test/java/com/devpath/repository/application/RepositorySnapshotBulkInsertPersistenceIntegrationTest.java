package com.devpath.repository.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devpath.repository.domain.Repository;
import com.devpath.repository.domain.RepositoryBranch;
import com.devpath.repository.domain.RepositoryDependency;
import com.devpath.repository.domain.RepositoryFile;
import com.devpath.repository.domain.RepositorySnapshot;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
class RepositorySnapshotBulkInsertPersistenceIntegrationTest {
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
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void batchInsertsFiveHundredFilesAndReloadsThemInPathOrder() {
        Instant capturedAt = Instant.parse("2026-08-12T00:00:00Z");
        Repository repository = repository("bulk", capturedAt);
        List<RepositoryFile> files = IntStream.range(0, 500).mapToObj(index -> RepositoryFile.normalized(
            "src/main/java/File%03d.java".formatted(index), "%040x".formatted(index), index
        )).toList();
        RepositorySnapshot snapshot = RepositorySnapshot.ready(
            repository.id(), repository.userId(), "d".repeat(40), capturedAt,
            List.of(), List.of(), List.of(), List.of(), files
        );
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> snapshots.saveSnapshot(snapshot));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        assertThat(snapshots.findSnapshot(repository.userId(), repository.id(), snapshot.id()).orElseThrow().files())
            .extracting(RepositoryFile::path)
            .containsExactlyElementsOf(files.stream().map(RepositoryFile::path).toList());
    }

    @Test
    void rollsBackSnapshotAndChildrenWhenAChildBatchFails() {
        Instant capturedAt = Instant.parse("2026-08-13T00:00:00Z");
        Repository repository = repository("rollback", capturedAt);
        RepositoryDependency duplicate = RepositoryDependency.normalized(
            "gradle", "org.example:duplicate", "1.0", "RUNTIME", "build.gradle"
        );
        RepositorySnapshot snapshot = RepositorySnapshot.ready(
            repository.id(), repository.userId(), "e".repeat(40), capturedAt,
            List.of(new RepositoryBranch("main", "f".repeat(40), true)), List.of(), List.of(),
            List.of(duplicate, duplicate), List.of()
        );

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager)
            .executeWithoutResult(status -> snapshots.saveSnapshot(snapshot)))
            .isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject(
            "select count(*) from repository_snapshots where snapshot_id=?", Integer.class, snapshot.id()
        )).isZero();
        assertThat(jdbc.queryForObject(
            "select count(*) from repository_branches where snapshot_id=?", Integer.class, snapshot.id()
        )).isZero();
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
}
