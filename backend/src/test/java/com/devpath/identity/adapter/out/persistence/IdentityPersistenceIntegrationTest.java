package com.devpath.identity.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devpath.identity.domain.AccountStatus;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.CareerStage;
import com.devpath.identity.domain.PreferenceType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
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
class IdentityPersistenceIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new com.devpath.test.PgVectorPostgreSQLContainer();

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserJpaRepository users;

    @Autowired
    private ExternalIdentityJpaRepository identities;

    @Autowired
    private UserProfileJpaRepository profiles;

    @Autowired
    private UserPreferenceJpaRepository preferences;

    @Test
    void flywayCreatesIdentityAndJdbcSessionTables() {
        Integer tableCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name IN ('users', 'external_identities', 'spring_session', 'spring_session_attributes', 'user_profiles', 'user_preferences', 'provider_credentials', 'audit_records', 'repositories')
            """,
            Integer.class
        );

        assertThat(tableCount).isEqualTo(9);

        Integer refreshTokenColumnCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'provider_credentials'
              AND column_name IN ('encrypted_refresh_token', 'refresh_token_iv', 'refresh_token_expires_at')
            """,
            Integer.class
        );

        assertThat(refreshTokenColumnCount).isEqualTo(3);
    }

    @Test
    void providerSubjectIsUniqueWithinTheProvider() {
        UUID userId = UUID.randomUUID();
        users.saveAndFlush(new UserJpaEntity(
            userId,
            AccountStatus.ACTIVE,
            "DevPath User",
            null,
            NOW,
            NOW,
            0
        ));
        identities.saveAndFlush(identity(UUID.randomUUID(), userId));

        assertThatThrownBy(() -> identities.saveAndFlush(identity(UUID.randomUUID(), userId)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userPersistenceRoundTripPreservesTheInternalIdentity() {
        UUID userId = UUID.randomUUID();
        users.saveAndFlush(new UserJpaEntity(
            userId,
            AccountStatus.ACTIVE,
            "DevPath User",
            "https://avatars.example/devpath",
            NOW,
            NOW,
            0
        ));

        UserJpaEntity restored = users.findById(userId).orElseThrow();

        assertThat(restored.id()).isEqualTo(userId);
        assertThat(restored.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(restored.displayName()).isEqualTo("DevPath User");
    }

    @Test
    void profileAndActiveTargetConstraintsProtectUserState() {
        UUID userId = UUID.randomUUID();
        users.saveAndFlush(new UserJpaEntity(userId, AccountStatus.ACTIVE, "DevPath User", null, NOW, NOW, 0));
        profiles.saveAndFlush(new UserProfileJpaEntity(UUID.randomUUID(), userId, CareerStage.JUNIOR, "Building APIs", NOW, NOW, 0));

        assertThat(profiles.findByUserId(userId)).get().extracting(UserProfileJpaEntity::careerStage).isEqualTo(CareerStage.JUNIOR);

        preferences.saveAndFlush(new UserPreferenceJpaEntity(UUID.randomUUID(), userId, PreferenceType.CAREER, "backend", "v1", true, NOW, null, 0));
        assertThatThrownBy(() -> preferences.saveAndFlush(new UserPreferenceJpaEntity(
            UUID.randomUUID(), userId, PreferenceType.CAREER, "frontend", "v1", true, NOW, null, 0
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private ExternalIdentityJpaEntity identity(UUID identityId, UUID userId) {
        return new ExternalIdentityJpaEntity(
            identityId,
            userId,
            OAuthProvider.GITHUB,
            "1849102",
            "devpath-user",
            "DevPath User",
            null,
            NOW,
            NOW,
            0
        );
    }
}
