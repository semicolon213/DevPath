package com.devpath.recommendation.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.recommendation.domain.RecommendationPriority;
import com.devpath.recommendation.domain.RecommendationType;
import com.devpath.recommendation.domain.Recommendation;
import com.devpath.recommendation.domain.RecommendationSet;
import com.devpath.learning.domain.DeterministicRoadmapEngine;
import com.devpath.rule.domain.RuleCategory;
import java.util.UUID;
import java.util.List;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=validate","spring.flyway.enabled=true","spring.flyway.locations=classpath:db/migration"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE) @Testcontainers(disabledWithoutDocker=true)
class RecommendationPolicyPersistenceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry){registry.add("spring.datasource.url",POSTGRES::getJdbcUrl);registry.add("spring.datasource.username",POSTGRES::getUsername);registry.add("spring.datasource.password",POSTGRES::getPassword);}
    @Autowired RecommendationPolicyJpaRepository policies;@Autowired RecommendationTemplateJpaRepository templates;
    @Autowired RecommendationSetJpaRepository sets;@Autowired RecommendationJpaRepository recommendations;@Autowired RecommendationEvidenceJpaRepository evidence;
    @Autowired RoadmapPolicyJpaRepository roadmapPolicies;@Autowired LearningRoadmapJpaRepository roadmaps;@Autowired RoadmapMilestoneJpaRepository milestones;@Autowired RoadmapStepJpaRepository steps;
    @Autowired JdbcTemplate jdbc;@Autowired TestEntityManager entityManager;
    @Test void loadsApprovedBackendAndFrontendPolicies(){
        var adapter=new JpaRecommendationAdapter(policies,templates,sets,recommendations,evidence,roadmapPolicies,roadmaps,milestones,steps);
        var backend=adapter.loadActivePolicy(UUID.fromString("32000000-0000-0000-0000-000000000101"));
        var frontend=adapter.loadActivePolicy(UUID.fromString("32000000-0000-0000-0000-000000000102"));
        assertThat(backend.versionLabel()).isEqualTo("recommendation-v1");assertThat(backend.templates()).hasSize(6);
        assertThat(backend.templates().get(RuleCategory.DATABASE).type()).isEqualTo(RecommendationType.PROJECT);
        assertThat(frontend.templates()).hasSize(4);assertThat(frontend.templates().get(RuleCategory.DOCUMENTATION).type()).isEqualTo(RecommendationType.PORTFOLIO);
        assertThat(adapter.loadActiveRoadmapPolicy(backend.policyId(),backend.careerProfileVersionId()).versionLabel()).isEqualTo("roadmap-v1");
        assertThat(RecommendationPriority.values()).containsExactly(RecommendationPriority.CRITICAL,RecommendationPriority.HIGH,RecommendationPriority.MEDIUM,RecommendationPriority.LOW);
    }
    @Test void persistsAndReloadsRecommendationEvidenceAndRoadmapPrerequisites(){
        var adapter=new JpaRecommendationAdapter(policies,templates,sets,recommendations,evidence,roadmapPolicies,roadmaps,milestones,steps);
        UUID user=UUID.randomUUID(),identity=UUID.randomUUID(),repository=UUID.randomUUID(),snapshot=UUID.randomUUID(),evaluation=UUID.randomUUID(),matrix=UUID.randomUUID(),assessment=UUID.randomUUID(),readiness=UUID.randomUUID(),gap=UUID.randomUUID(),evidenceId=UUID.randomUUID();
        Instant now=Instant.parse("2026-08-25T00:00:00Z");seedBasis(user,identity,repository,snapshot,evaluation,matrix,assessment,readiness,gap,evidenceId,now);
        var policy=adapter.loadActivePolicy(UUID.fromString("32000000-0000-0000-0000-000000000101"));
        UUID setId=UUID.randomUUID();var recommendation=new Recommendation(UUID.randomUUID(),gap,RuleCategory.TESTING,RecommendationType.PROJECT,RecommendationPriority.HIGH,"CAREER_REQUIRED_GAP","Add testing","Reach 60",List.of("test files"),List.of(evidenceId),16,0,"PROPOSED");
        var set=new RecommendationSet(setId,user,readiness,policy.policyId(),policy.versionLabel(),"PUBLISHED",List.of(recommendation),now);adapter.saveSet(set);
        var roadmapPolicy=adapter.loadActiveRoadmapPolicy(policy.policyId(),policy.careerProfileVersionId());var roadmap=new DeterministicRoadmapEngine().generate(UUID.randomUUID(),set,roadmapPolicy,now);adapter.saveRoadmap(roadmap);
        entityManager.flush();entityManager.clear();
        assertThat(adapter.findSetByIdAndOwner(setId,user)).get().satisfies(value->assertThat(value.recommendations().getFirst().evidenceIds()).containsExactly(evidenceId));
        assertThat(adapter.findSetsByOwner(user)).extracting(value->value.recommendationSetId()).containsExactly(setId);
        assertThat(adapter.findSetsByOwner(UUID.randomUUID())).isEmpty();
        assertThat(adapter.findSetByRecommendationIdAndOwner(recommendation.recommendationId(),user)).isPresent();
        assertThat(adapter.findSetByRecommendationIdAndOwner(recommendation.recommendationId(),UUID.randomUUID())).isEmpty();
        assertThat(adapter.findEvidenceByRecommendationAndOwner(recommendation.recommendationId(),user)).singleElement().satisfies(value->{assertThat(value.evidenceId()).isEqualTo(evidenceId);assertThat(value.observedFactSummary()).isEqualTo("test files");});
        assertThat(adapter.findEvidenceByRecommendationAndOwner(recommendation.recommendationId(),UUID.randomUUID())).isEmpty();
        assertThat(adapter.findRoadmapByIdAndOwner(roadmap.roadmapId(),user)).get().satisfies(value->{assertThat(value.steps()).hasSize(1);assertThat(value.steps().getFirst().expectedEvidence()).containsExactly("test files");});
        assertThat(adapter.findRoadmapsByOwner(user)).extracting(value->value.roadmapId()).containsExactly(roadmap.roadmapId());
        assertThat(adapter.findRoadmapsByOwner(UUID.randomUUID())).isEmpty();
        Instant archivedAt=now.plusSeconds(60);var archived=adapter.updateRoadmap(roadmap.archive(archivedAt));
        entityManager.flush();entityManager.clear();
        assertThat(archived.status()).isEqualTo("ARCHIVED");
        assertThat(adapter.findRoadmapByIdAndOwner(roadmap.roadmapId(),user)).get().satisfies(value->{assertThat(value.status()).isEqualTo("ARCHIVED");assertThat(value.updatedAt()).isEqualTo(archivedAt);assertThat(value.steps()).hasSize(1);});
        assertThat(adapter.findSetByIdAndOwner(setId,UUID.randomUUID())).isEmpty();
    }
    private void seedBasis(UUID user,UUID identity,UUID repository,UUID snapshot,UUID evaluation,UUID matrix,UUID assessment,UUID readiness,UUID gap,UUID evidence,Instant now){var at=now.atOffset(ZoneOffset.UTC);
        jdbc.update("INSERT INTO users(user_id,account_status,display_name,created_at,updated_at,version) VALUES (?,?,?,?,?,0)",user,"ACTIVE","Test",at,at);
        jdbc.update("INSERT INTO external_identities(external_identity_id,user_id,provider,provider_subject,linked_at,updated_at,version) VALUES (?,?,?,?,?,?,0)",identity,user,"GITHUB","subject-"+user,at,at);
        jdbc.update("INSERT INTO repositories(repository_id,user_id,external_identity_id,provider,provider_repository_id,repository_name,full_name,owner_login,visibility,default_branch,provider_archived,lifecycle_status,sync_status,html_url,discovered_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)",repository,user,identity,"GITHUB","provider-"+repository,"repo","owner/repo","owner","PRIVATE","main",false,"ACTIVE","SYNCHRONIZED","https://github.com/owner/repo",at,at);
        jdbc.update("INSERT INTO repository_snapshots(snapshot_id,repository_id,user_id,source_revision,captured_at,snapshot_status,content_hash,branch_count,commit_count,retention_status) VALUES (?,?,?,?,?,?,?,?,?,?)",snapshot,repository,user,"a".repeat(40),at,"READY","b".repeat(64),1,1,"ACTIVE");
        jdbc.update("INSERT INTO evaluations(evaluation_id,user_id,snapshot_id,rule_set_version_id,status,input_hash,rule_set_version_label,formula_library_version,extractor_version,overall_score,confidence,started_at,completed_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",evaluation,user,snapshot,UUID.fromString("11000000-0000-0000-0000-000000000002"),"COMPLETED","c".repeat(64),"baseline-v2","formula-v1","engineering-evidence-extractor-v2",50,90,at,at);
        jdbc.update("INSERT INTO skill_matrices(skill_matrix_id,user_id,evaluation_id,skill_matrix_policy_id,policy_version,rule_set_version,status,generated_at,version) VALUES (?,?,?,?,?,?,?,?,0)",matrix,user,evaluation,UUID.fromString("21000000-0000-0000-0000-000000000002"),"skill-matrix-v2","baseline-v2","CURRENT",at);
        jdbc.update("INSERT INTO skill_assessments(skill_assessment_id,skill_matrix_id,skill_id,score,skill_level,confidence,strength_flag,weakness_flag,growth_trend,aggregate_rule_result_reference,rule_set_version) VALUES (?,?,?,?,?,?,?,?,?,?,?)",assessment,matrix,UUID.fromString("20000000-0000-0000-0000-000000000003"),30,"BEGINNER",90,false,true,"UNAVAILABLE","evaluation:"+evaluation,"baseline-v2");
        jdbc.update("INSERT INTO evidence_records(evidence_id,user_id,snapshot_id,evidence_type,source_reference,source_reference_hash,observed_fact_summary,confidence,created_at) VALUES (?,?,?,?,?,?,?,?,?)",evidence,user,snapshot,"SNAPSHOT_SIGNAL","snapshot:"+snapshot+":TEST_FILES","d".repeat(64),"test files",100,at);
        jdbc.update("INSERT INTO career_readiness_assessments(career_readiness_id,user_id,skill_matrix_id,career_profile_version_id,career_readiness_policy_id,status,readiness_score,readiness_level,confidence,rule_set_version,unavailable_categories,assessed_at) VALUES (?,?,?,?,?,?,?,?,?,?,CAST(? AS jsonb),?)",readiness,user,matrix,UUID.fromString("32000000-0000-0000-0000-000000000101"),UUID.fromString("33000000-0000-0000-0000-000000000001"),"COMPLETED",50,"DEVELOPING",90,"baseline-v2","[]",at);
        jdbc.update("INSERT INTO skill_gaps(skill_gap_id,career_readiness_id,skill_assessment_id,skill_id,skill_key,category,actual_score,actual_level,expected_minimum,gap_state,career_weight) VALUES (?,?,?,?,?,?,?,?,?,?,?)",gap,readiness,assessment,UUID.fromString("20000000-0000-0000-0000-000000000003"),"testing-discipline","TESTING",30,"BEGINNER",60,"WEAK",20);
    }
}
