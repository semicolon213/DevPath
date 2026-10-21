package com.devpath.rule.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.rule.domain.SkillAssessment;
import com.devpath.rule.domain.SkillLevel;
import com.devpath.rule.domain.SkillMatrix;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class SkillMatrixPersistenceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new com.devpath.test.PgVectorPostgreSQLContainer();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired SkillJpaRepository skills;
    @Autowired SkillMatrixPolicyJpaRepository policies;
    @Autowired SkillPolicyMappingJpaRepository mappings;
    @Autowired SkillMatrixJpaRepository matrices;
    @Autowired SkillAssessmentJpaRepository assessments;
    @Autowired SkillEvidenceLinkJpaRepository evidenceLinks;
    @Autowired SkillRepositoryLinkJpaRepository repositoryLinks;
    @Autowired SkillAssessmentFactJpaRepository facts;
    @Autowired RuleEvaluationJpaRepository evaluations;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestEntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void savesAndReloadsAllAssessmentChildrenWithConstantQueryCount() {
        var adapter = adapter();
        TestBasis singleBasis = seedBasis(1);
        TestBasis multipleBasis = seedBasis(2);
        SkillMatrix single = matrix(adapter, singleBasis, 1);
        SkillMatrix multiple = matrix(adapter, multipleBasis, 2);
        adapter.saveAsCurrent(single);
        adapter.saveAsCurrent(multiple);
        entityManager.flush();
        entityManager.clear();

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(adapter.findByIdAndOwner(single.matrixId(), single.userId())).contains(single);
        long singleAssessmentQueries = statistics.getPrepareStatementCount();

        statistics.clear();
        assertThat(adapter.findByIdAndOwner(multiple.matrixId(), multiple.userId())).contains(multiple);
        long multipleAssessmentQueries = statistics.getPrepareStatementCount();

        assertThat(singleAssessmentQueries).isEqualTo(6);
        assertThat(multipleAssessmentQueries).isEqualTo(singleAssessmentQueries);
    }

    @Test
    void loadsEarlierPolicyAssessmentWithoutRecomputingEvidenceStatus() {
        var adapter = adapter();
        TestBasis basis = seedBasis(3);
        UUID matrixId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        var at = basis.now().atOffset(ZoneOffset.UTC);
        jdbc.update("INSERT INTO skill_matrices(skill_matrix_id,user_id,evaluation_id,skill_matrix_policy_id,policy_version,rule_set_version,status,generated_at,version) VALUES (?,?,?,?,?,?,?,?,0)",
            matrixId, basis.userId(), basis.evaluationId(), UUID.fromString("21000000-0000-0000-0000-000000000002"),
            "skill-matrix-v2", "baseline-v2", "CURRENT", at);
        jdbc.update("INSERT INTO skill_assessments(skill_assessment_id,skill_matrix_id,skill_id,score,skill_level,confidence,strength_flag,weakness_flag,growth_trend,aggregate_rule_result_reference,rule_set_version) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
            assessmentId, matrixId, UUID.fromString("20000000-0000-0000-0000-000000000003"), 60, "COMPETENT", 100,
            false, false, "UNAVAILABLE", "evaluation:" + basis.evaluationId(), "baseline-v2");
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findByIdAndOwner(matrixId, basis.userId())).get()
            .satisfies(matrix -> assertThat(matrix.assessments()).singleElement()
                .satisfies(assessment -> assertThat(assessment.evidenceStatus()).isNull()));
    }

    private JpaSkillMatrixAdapter adapter() {
        return new JpaSkillMatrixAdapter(skills, policies, mappings, matrices, assessments, evidenceLinks,
            repositoryLinks, facts, evaluations);
    }

    private SkillMatrix matrix(JpaSkillMatrixAdapter adapter, TestBasis basis, int assessmentCount) {
        var policy = adapter.loadActivePolicy(UUID.fromString("11000000-0000-0000-0000-000000000002"));
        assertThat(policy.versionLabel()).isEqualTo("skill-matrix-v3");
        List<SkillAssessment> values = java.util.stream.IntStream.range(0, assessmentCount).mapToObj(index ->
            new SkillAssessment(UUID.randomUUID(), policy.skills().get(index), new BigDecimal("50.00"),
                SkillLevel.DEVELOPING, new BigDecimal("90.00"), com.devpath.rule.domain.EvidenceStatus.SUFFICIENT,
                false, false, "UNAVAILABLE",
                "evaluation:" + basis.evaluationId(), basis.evidenceIds(), basis.repositoryIds(),
                List.of("fact-" + index + "-first", "fact-" + index + "-second"), "baseline-v2")
        ).toList();
        return new SkillMatrix(UUID.randomUUID(), basis.userId(), basis.evaluationId(), policy.policyId(),
            policy.versionLabel(), "baseline-v2", "CURRENT", basis.now(), values);
    }

    private TestBasis seedBasis(int suffix) {
        UUID user = UUID.randomUUID(), identity = UUID.randomUUID(), repositoryOne = UUID.randomUUID();
        UUID repositoryTwo = UUID.randomUUID(), snapshot = UUID.randomUUID(), evaluation = UUID.randomUUID();
        List<UUID> repositoryIds = java.util.stream.Stream.of(repositoryOne, repositoryTwo)
            .sorted(Comparator.comparing(UUID::toString)).toList();
        List<UUID> evidenceIds = java.util.stream.Stream.of(UUID.randomUUID(), UUID.randomUUID())
            .sorted(Comparator.comparing(UUID::toString)).toList();
        Instant now = Instant.parse("2026-08-25T00:00:0" + suffix + "Z");
        var at = now.atOffset(ZoneOffset.UTC);
        jdbc.update("INSERT INTO users(user_id,account_status,display_name,created_at,updated_at,version) VALUES (?,?,?,?,?,0)",
            user, "ACTIVE", "Test", at, at);
        jdbc.update("INSERT INTO external_identities(external_identity_id,user_id,provider,provider_subject,linked_at,updated_at,version) VALUES (?,?,?,?,?,?,0)",
            identity, user, "GITHUB", "subject-" + user, at, at);
        for (UUID repository : repositoryIds) {
            jdbc.update("INSERT INTO repositories(repository_id,user_id,external_identity_id,provider,provider_repository_id,repository_name,full_name,owner_login,visibility,default_branch,provider_archived,lifecycle_status,sync_status,html_url,discovered_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
                repository, user, identity, "GITHUB", "provider-" + repository, "repo", "owner/" + repository,
                "owner", "PRIVATE", "main", false, "ACTIVE", "SYNCHRONIZED",
                "https://github.com/owner/" + repository, at, at);
        }
        jdbc.update("INSERT INTO repository_snapshots(snapshot_id,repository_id,user_id,source_revision,captured_at,snapshot_status,content_hash,branch_count,commit_count,retention_status) VALUES (?,?,?,?,?,?,?,?,?,?)",
            snapshot, repositoryIds.getFirst(), user, "a".repeat(40), at, "READY",
            Integer.toHexString(suffix).repeat(64), 1, 1, "ACTIVE");
        jdbc.update("INSERT INTO evaluations(evaluation_id,user_id,snapshot_id,rule_set_version_id,status,input_hash,rule_set_version_label,formula_library_version,extractor_version,overall_score,confidence,started_at,completed_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
            evaluation, user, snapshot, UUID.fromString("11000000-0000-0000-0000-000000000002"), "COMPLETED",
            Integer.toHexString(suffix + 2).repeat(64), "baseline-v2", "formula-v1",
            "engineering-evidence-extractor-v2", 50, 90, at, at);
        for (int index = 0; index < evidenceIds.size(); index++) {
            jdbc.update("INSERT INTO evidence_records(evidence_id,user_id,snapshot_id,evidence_type,source_reference,source_reference_hash,observed_fact_summary,confidence,created_at) VALUES (?,?,?,?,?,?,?,?,?)",
                evidenceIds.get(index), user, snapshot, "SNAPSHOT_SIGNAL", "snapshot:" + snapshot + ":signal:" + index,
                Integer.toHexString(suffix + index + 4).repeat(64), "fact-" + index, 100, at);
        }
        return new TestBasis(user, evaluation, repositoryIds, evidenceIds, now);
    }

    private record TestBasis(
        UUID userId, UUID evaluationId, List<UUID> repositoryIds, List<UUID> evidenceIds, Instant now
    ) {}
}
