package com.devpath.rule.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.rule.domain.RuleCategory;
import com.devpath.rule.domain.CompletedRuleEvaluation;
import com.devpath.rule.domain.RuleCategoryScore;
import com.devpath.rule.domain.RuleEvaluationResult;
import com.devpath.rule.domain.RuleExecutionResult;
import com.devpath.rule.domain.RuleOutcomeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
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
class RuleCatalogPersistenceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired RuleSetJpaRepository ruleSets;
    @Autowired RuleSetVersionJpaRepository versions;
    @Autowired RuleCategoryWeightJpaRepository categoryWeights;
    @Autowired RuleDefinitionJpaRepository rules;
    @Autowired RuleEvaluationJpaRepository evaluations;
    @Autowired EvaluationWarningJpaRepository warnings;
    @Autowired RuleCategoryEvaluationJpaRepository categoryEvaluations;
    @Autowired CategoryMissingEvidenceJpaRepository missingEvidence;
    @Autowired RuleExecutionResultJpaRepository executionResults;
    @Autowired RuleEvidenceJpaRepository evidence;
    @Autowired ScoreEvidenceLinkJpaRepository evidenceLinks;
    @Autowired SkillJpaRepository skills;
    @Autowired SkillMatrixPolicyJpaRepository skillPolicies;
    @Autowired SkillPolicyMappingJpaRepository skillMappings;
    @Autowired SkillMatrixJpaRepository skillMatrices;
    @Autowired SkillAssessmentJpaRepository skillAssessments;
    @Autowired SkillEvidenceLinkJpaRepository skillEvidenceLinks;
    @Autowired SkillRepositoryLinkJpaRepository skillRepositoryLinks;
    @Autowired SkillAssessmentFactJpaRepository skillFacts;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void flywaySeedsValidatedImmutableBaselineV2Catalog() {
        var adapter = new JpaRuleCatalogAdapter(ruleSets, versions, categoryWeights, rules);

        var catalog = adapter.loadActive("REPOSITORY_BASELINE");

        assertThat(catalog.versionLabel()).isEqualTo("baseline-v2");
        assertThat(catalog.formulaLibraryVersion()).isEqualTo("formula-v1");
        assertThat(catalog.requiredExtractorVersion()).isEqualTo("engineering-evidence-extractor-v2");
        assertThat(catalog.rules()).hasSize(25);
        assertThat(catalog.categoryWeights())
            .containsEntry(RuleCategory.LANGUAGE, new BigDecimal("0.150000"))
            .containsEntry(RuleCategory.DATABASE, new BigDecimal("0.150000"))
            .containsEntry(RuleCategory.ARCHITECTURE, new BigDecimal("0.150000"))
            .containsEntry(RuleCategory.DEVOPS, new BigDecimal("0.100000"));
        assertThat(catalog.categoryWeights().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo("1.000000");
        Integer tableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('rule_sets','rule_set_versions','rule_category_weights','rules')",
            Integer.class
        );
        assertThat(tableCount).isEqualTo(4);
    }

    @Test
    void persistsAndReloadsOwnerScopedCompletedEvaluationWithEvidenceLinks() {
        UUID userId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        UUID evaluationId = UUID.randomUUID();
        UUID versionId = UUID.fromString("11000000-0000-0000-0000-000000000001");
        Instant now = Instant.parse("2026-08-11T10:00:00Z");
        seedSnapshot(userId, identityId, repositoryId, snapshotId, now);
        var ruleResult = new RuleExecutionResult("README_PRESENT", "1.0.0", RuleCategory.DOCUMENTATION,
            RuleOutcomeStatus.PASSED, BigDecimal.ONE, new BigDecimal("100.00"), new BigDecimal("0.200000"),
            "PRESENCE@formula-v1", "formula=PRESENCE; raw=1; score=100.00",
            List.of("snapshot:" + snapshotId + ":path:README.md"));
        var category = new RuleCategoryScore(RuleCategory.DOCUMENTATION, new BigDecimal("100.00"),
            new BigDecimal("0.200000"), new BigDecimal("100.00"), List.of(ruleResult), List.of());
        var result = new RuleEvaluationResult(snapshotId.toString(), versionId.toString(), "baseline-v1", "formula-v1",
            "engineering-evidence-extractor-v1", new BigDecimal("20.00"), new BigDecimal("100.00"),
            List.of(category), List.of());
        var completed = new CompletedRuleEvaluation(evaluationId, userId, snapshotId, versionId,
            "c".repeat(64), result, now, now);
        var adapter = new JpaRuleEvaluationAdapter(evaluations, warnings, categoryEvaluations, missingEvidence,
            executionResults, evidence, evidenceLinks);

        adapter.saveCompleted(completed);

        var reloaded = adapter.findByIdAndOwner(evaluationId, userId).orElseThrow();
        assertThat(reloaded.id()).isEqualTo(completed.id());
        assertThat(reloaded.inputHash()).isEqualTo(completed.inputHash());
        assertThat(reloaded.result().overallScore()).isEqualByComparingTo(completed.result().overallScore());
        assertThat(reloaded.result().categoryScores()).hasSize(1);
        assertThat(adapter.findByIdAndOwner(evaluationId, UUID.randomUUID())).isEmpty();
        assertThat(adapter.findEvidenceByEvaluationAndOwner(evaluationId, userId)).singleElement()
            .satisfies(link -> {
                assertThat(link.ruleId()).isEqualTo("README_PRESENT");
                assertThat(link.evidence().sourceReference()).endsWith(":path:README.md");
            });
    }

    @Test
    void flywaySeedsVersionedSkillMatrixPolicyForTheBaselineRuleSet() {
        var adapter = new JpaSkillMatrixAdapter(skills, skillPolicies, skillMappings, skillMatrices, skillAssessments,
            skillEvidenceLinks, skillRepositoryLinks, skillFacts, evaluations);

        var policy = adapter.loadActivePolicy(UUID.fromString("11000000-0000-0000-0000-000000000002"));

        assertThat(policy.versionLabel()).isEqualTo("skill-matrix-v2");
        assertThat(policy.skills()).hasSize(8);
        assertThat(policy.developingMinimum()).isEqualByComparingTo("40.00");
        assertThat(policy.strengthMinimum()).isEqualByComparingTo("80.00");
        Integer tableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('skills','skill_matrix_policies','skill_policy_mappings','skill_matrices','skill_assessments','skill_evidence_links','skill_repository_links','skill_assessment_facts')",
            Integer.class
        );
        assertThat(tableCount).isEqualTo(8);
    }

    private void seedSnapshot(UUID userId, UUID identityId, UUID repositoryId, UUID snapshotId, Instant now) {
        var databaseTimestamp = now.atOffset(ZoneOffset.UTC);
        jdbcTemplate.update("INSERT INTO users(user_id,account_status,display_name,created_at,updated_at,version) VALUES (?,?,?,?,?,0)",
            userId, "ACTIVE", "Test User", databaseTimestamp, databaseTimestamp);
        jdbcTemplate.update("INSERT INTO external_identities(external_identity_id,user_id,provider,provider_subject,linked_at,updated_at,version) VALUES (?,?,?,?,?,?,0)",
            identityId, userId, "GITHUB", "subject-" + userId, databaseTimestamp, databaseTimestamp);
        jdbcTemplate.update("INSERT INTO repositories(repository_id,user_id,external_identity_id,provider,provider_repository_id,repository_name,full_name,owner_login,visibility,default_branch,provider_archived,lifecycle_status,sync_status,html_url,discovered_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
            repositoryId, userId, identityId, "GITHUB", "provider-" + repositoryId, "repo", "owner/repo", "owner",
            "PRIVATE", "main", false, "ACTIVE", "SYNCHRONIZED", "https://github.com/owner/repo", databaseTimestamp,
            databaseTimestamp);
        jdbcTemplate.update("INSERT INTO repository_snapshots(snapshot_id,repository_id,user_id,source_revision,captured_at,snapshot_status,content_hash,branch_count,commit_count,retention_status) VALUES (?,?,?,?,?,?,?,?,?,?)",
            snapshotId, repositoryId, userId, "a".repeat(40), databaseTimestamp, "READY", "b".repeat(64), 1, 1,
            "ACTIVE");
    }
}
