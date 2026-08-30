package com.devpath.integration.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.integration.application.NotionWorkspacePageView;
import com.devpath.integration.config.NotionIntegrationProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
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
class NotionConnectionPersistenceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new com.devpath.test.PgVectorPostgreSQLContainer();

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired NotionWorkspaceConnectionJpaRepository connections;
    @Autowired NotionPageMetadataJpaRepository metadata;

    @Test
    void connectionLifecycleRemainsOwnerScopedAndRefreshSafe() {
        UUID owner = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();
        Instant connectedAt = Instant.parse("2026-08-29T00:00:00Z");
        insertUser(owner, connectedAt);
        insertUser(otherOwner, connectedAt);
        var credentials = new EncryptedNotionCredentialStore(connections, properties());
        var pages = new NotionPageMetadataStore(metadata);

        var ownerCredential = credentials.save(owner, "workspace-a", "bot-a", "Owner workspace", null,
            "owner-access-v1", "owner-refresh-v1", connectedAt);
        var otherCredential = credentials.save(otherOwner, "workspace-b", "bot-b", "Other workspace", null,
            "other-access", "other-refresh", connectedAt);
        pages.replace(ownerCredential.connectionId(), owner, List.of(page("shared-page", "First title")), connectedAt);
        pages.replace(otherCredential.connectionId(), otherOwner, List.of(page("other-page", "Other title")), connectedAt);

        Instant refreshedAt = connectedAt.plusSeconds(60);
        pages.replace(ownerCredential.connectionId(), owner, List.of(page("shared-page", "Updated title")), refreshedAt);
        metadata.flush();
        assertThat(metadataCount(ownerCredential.connectionId())).isEqualTo(1);
        assertThat(metadataTitle(ownerCredential.connectionId())).isEqualTo("Updated title");
        assertThat(metadataCount(otherCredential.connectionId())).isEqualTo(1);

        var rotated = credentials.save(owner, "workspace-a", "bot-a", "Owner workspace", null,
            "owner-access-v2", "owner-refresh-v2", refreshedAt);
        assertThat(rotated.connectionId()).isEqualTo(ownerCredential.connectionId());
        assertThat(credentials.findActive(owner)).get().satisfies(credential -> {
            assertThat(credential.userId()).isEqualTo(owner);
            assertThat(credential.accessToken()).isEqualTo("owner-access-v2");
        });
        assertThat(credentials.findActive(otherOwner)).get().satisfies(credential -> {
            assertThat(credential.userId()).isEqualTo(otherOwner);
            assertThat(credential.accessToken()).isEqualTo("other-access");
        });

        credentials.revokeActive(owner, refreshedAt.plusSeconds(60));
        assertThat(credentials.findActive(owner)).isEmpty();
        assertThat(credentials.findAllViews(owner)).singleElement().satisfies(connection -> {
            assertThat(connection.status()).isEqualTo("REVOKED");
            assertThat(connection.scopes()).isEmpty();
        });
        assertThat(credentials.findActive(otherOwner)).isPresent();

        pages.delete(ownerCredential.connectionId());
        metadata.flush();
        assertThat(metadataCount(ownerCredential.connectionId())).isZero();
        assertThat(metadataCount(otherCredential.connectionId())).isEqualTo(1);
    }

    private NotionIntegrationProperties properties() {
        return new NotionIntegrationProperties("client", "secret", "http://localhost/callback",
            Base64.getEncoder().encodeToString(new byte[32]), "test-v1");
    }

    private NotionWorkspacePageView page(String id, String title) {
        return new NotionWorkspacePageView(id, title, "PAGE", null, Instant.parse("2026-08-29T00:00:00Z"), false);
    }

    private void insertUser(UUID userId, Instant now) {
        jdbcTemplate.update("""
            INSERT INTO users (user_id, account_status, display_name, created_at, updated_at, version)
            VALUES (?, 'ACTIVE', 'DevPath User', ?, ?, 0)
            """, userId, Timestamp.from(now), Timestamp.from(now));
    }

    private int metadataCount(UUID connectionId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM notion_page_metadata WHERE notion_connection_id = ?", Integer.class, connectionId);
    }

    private String metadataTitle(UUID connectionId) {
        return jdbcTemplate.queryForObject(
            "SELECT title FROM notion_page_metadata WHERE notion_connection_id = ?", String.class, connectionId);
    }
}
