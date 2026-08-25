package com.devpath.career.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.rule.domain.RuleCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
class CareerReadinessPolicyPersistenceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CareerReadinessPolicyJpaRepository policies;
    @Autowired CareerReadinessWeightJpaRepository weights;
    @Autowired CareerReadinessJpaRepository readiness;
    @Autowired SkillGapJpaRepository gaps;
    @Autowired SkillGapEvidenceJpaRepository evidence;
    @Autowired CareerCatalogJpaRepository careers;

    @Test
    void loadsApprovedBackendAndFrontendReadinessWeightsFromPostgresql() {
        var adapter = new JpaCareerReadinessAdapter(policies, weights, readiness, gaps, evidence, careers);

        var backend = adapter.loadActivePolicy(java.util.UUID.fromString("32000000-0000-0000-0000-000000000101"));
        var frontend = adapter.loadActivePolicy(java.util.UUID.fromString("32000000-0000-0000-0000-000000000102"));

        assertThat(backend.versionLabel()).isEqualTo("readiness-v1");
        assertThat(backend.expectedMinimum()).isEqualByComparingTo("60");
        assertThat(backend.categoryWeights()).containsEntry(RuleCategory.DATABASE, new java.math.BigDecimal("20.00"));
        assertThat(backend.categoryWeights().values()).containsExactlyInAnyOrderElementsOf(java.util.List.of(
            new java.math.BigDecimal("15.00"), new java.math.BigDecimal("20.00"), new java.math.BigDecimal("20.00"),
            new java.math.BigDecimal("15.00"), new java.math.BigDecimal("20.00"), new java.math.BigDecimal("10.00")));
        assertThat(frontend.categoryWeights()).containsEntry(RuleCategory.LANGUAGE, new java.math.BigDecimal("30.00"))
            .containsEntry(RuleCategory.DOCUMENTATION, new java.math.BigDecimal("20.00"));
    }
}
