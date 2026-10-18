package com.devpath.recommendation.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.devpath.recommendation.domain.RecommendationPriority;
import com.devpath.recommendation.domain.RecommendationType;
import com.devpath.recommendation.domain.Recommendation;
import com.devpath.recommendation.domain.RecommendationSet;
import com.devpath.recommendation.domain.RecommendationEvidence;
import com.devpath.learning.domain.LearningRoadmap;
import com.devpath.learning.domain.RoadmapMilestone;
import com.devpath.learning.domain.RoadmapStep;
import com.devpath.rule.domain.RuleCategory;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.hibernate.SessionFactory;
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

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=validate","spring.jpa.properties.hibernate.generate_statistics=true","spring.flyway.enabled=true","spring.flyway.locations=classpath:db/migration"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE) @Testcontainers(disabledWithoutDocker=true)
class RecommendationPolicyPersistenceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new com.devpath.test.PgVectorPostgreSQLContainer();
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry){registry.add("spring.datasource.url",POSTGRES::getJdbcUrl);registry.add("spring.datasource.username",POSTGRES::getUsername);registry.add("spring.datasource.password",POSTGRES::getPassword);}
    @Autowired RecommendationPolicyJpaRepository policies;@Autowired RecommendationTemplateJpaRepository templates;
    @Autowired RecommendationSetJpaRepository sets;@Autowired RecommendationJpaRepository recommendations;@Autowired RecommendationEvidenceJpaRepository evidence;
    @Autowired RoadmapPolicyJpaRepository roadmapPolicies;@Autowired LearningRoadmapJpaRepository roadmaps;@Autowired RoadmapMilestoneJpaRepository milestones;@Autowired RoadmapStepJpaRepository steps;
    @Autowired JdbcTemplate jdbc;@Autowired TestEntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
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
        UUID user=UUID.randomUUID(),identity=UUID.randomUUID(),repository=UUID.randomUUID(),snapshot=UUID.randomUUID(),evaluation=UUID.randomUUID(),matrix=UUID.randomUUID(),assessment=UUID.randomUUID(),readiness=UUID.randomUUID();
        UUID gap0=UUID.fromString("60000000-0000-0000-0000-000000000100"),gap1=UUID.fromString("60000000-0000-0000-0000-000000000101"),gap2=UUID.fromString("60000000-0000-0000-0000-000000000102"),gap3=UUID.fromString("60000000-0000-0000-0000-000000000103");
        List<UUID> evidenceIds=List.of(
            UUID.fromString("61000000-0000-0000-0000-000000000101"),
            UUID.fromString("61000000-0000-0000-0000-000000000102"),
            UUID.fromString("61000000-0000-0000-0000-000000000103"),
            UUID.fromString("61000000-0000-0000-0000-000000000104"),
            UUID.fromString("61000000-0000-0000-0000-000000000105"),
            UUID.fromString("61000000-0000-0000-0000-000000000106")
        );
        Instant now=Instant.parse("2026-08-25T00:00:00Z");seedBasis(user,identity,repository,snapshot,evaluation,matrix,assessment,readiness,gap0,evidenceIds.getFirst(),now);
        seedGap(UUID.randomUUID(),gap1,matrix,readiness,"20000000-0000-0000-0000-000000000006","database-engineering","DATABASE");
        seedGap(UUID.randomUUID(),gap2,matrix,readiness,"20000000-0000-0000-0000-000000000007","architecture-structure","ARCHITECTURE");
        seedGap(UUID.randomUUID(),gap3,matrix,readiness,"20000000-0000-0000-0000-000000000008","delivery-operations","DEVOPS");
        for(int index=1;index<evidenceIds.size();index++)seedEvidence(evidenceIds.get(index),user,snapshot,index,now);
        var policy=adapter.loadActivePolicy(UUID.fromString("32000000-0000-0000-0000-000000000101"));
        var recommendation2=new Recommendation(UUID.randomUUID(),gap2,RuleCategory.ARCHITECTURE,RecommendationType.PROJECT,RecommendationPriority.HIGH,"CAREER_REQUIRED_GAP","Position 2","Reach 60",List.of("test files"),List.of(evidenceIds.get(5),evidenceIds.get(4)),16,2,"PROPOSED");
        var recommendation0=new Recommendation(UUID.randomUUID(),gap0,RuleCategory.TESTING,RecommendationType.PROJECT,RecommendationPriority.HIGH,"CAREER_REQUIRED_GAP","Position 0","Reach 60",List.of("test files"),List.of(evidenceIds.get(1),evidenceIds.get(0)),16,0,"PROPOSED");
        var recommendation1=new Recommendation(UUID.randomUUID(),gap1,RuleCategory.DATABASE,RecommendationType.PROJECT,RecommendationPriority.HIGH,"CAREER_REQUIRED_GAP","Position 1","Reach 60",List.of("test files"),List.of(evidenceIds.get(3),evidenceIds.get(2)),16,1,"PROPOSED");
        var recommendation3=new Recommendation(UUID.randomUUID(),gap3,RuleCategory.DEVOPS,RecommendationType.PROJECT,RecommendationPriority.HIGH,"CAREER_REQUIRED_GAP","Position 3","Reach 60",List.of("test files"),List.of(evidenceIds.get(5),evidenceIds.get(0)),16,3,"PROPOSED");
        UUID setId=UUID.randomUUID();
        var set=new RecommendationSet(setId,user,readiness,policy.policyId(),policy.versionLabel(),"PUBLISHED",List.of(recommendation3,recommendation1,recommendation2,recommendation0),now);adapter.saveSet(set);
        var roadmapPolicy=adapter.loadActiveRoadmapPolicy(policy.policyId(),policy.careerProfileVersionId());
        var milestone1=new RoadmapMilestone(UUID.randomUUID(),1,RuleCategory.ARCHITECTURE,"Milestone 1","PLANNED");
        var milestone0=new RoadmapMilestone(UUID.randomUUID(),0,RuleCategory.TESTING,"Milestone 0","PLANNED");
        // Distinct per-step content so a dropped or swapped column in step hydration is detectable.
        UUID step0Id=UUID.randomUUID();UUID step1Id=UUID.randomUUID();UUID step2Id=UUID.randomUUID();UUID step3Id=UUID.randomUUID();
        var step3=new RoadmapStep(step3Id,milestone1.milestoneId(),recommendation3.recommendationId(),3,RuleCategory.DEVOPS,"Step 3","ADVANCED",24,List.of(step2Id,step1Id),"Done 3",List.of("pipeline","deploy log"),"SKIPPED");
        var step1=new RoadmapStep(step1Id,milestone0.milestoneId(),recommendation1.recommendationId(),1,RuleCategory.DATABASE,"Step 1","INTERMEDIATE",12,List.of(step0Id),"Done 1",List.of("schema migration"),"IN_PROGRESS");
        var step2=new RoadmapStep(step2Id,milestone1.milestoneId(),recommendation2.recommendationId(),2,RuleCategory.ARCHITECTURE,"Step 2","INTERMEDIATE",16,List.of(step0Id,step1Id),"Done 2",List.of("design doc","module diagram"),"COMPLETED");
        var step0=new RoadmapStep(step0Id,milestone0.milestoneId(),recommendation0.recommendationId(),0,RuleCategory.TESTING,"Step 0","BEGINNER",8,List.of(),"Done 0",List.of("test files"),"NOT_STARTED");
        var roadmap=new LearningRoadmap(UUID.randomUUID(),user,setId,roadmapPolicy.policyId(),roadmapPolicy.versionLabel(),"CREATED",new BigDecimal("0.00"),List.of(milestone1,milestone0),List.of(step3,step1,step2,step0),now,now);adapter.saveRoadmap(roadmap);
        entityManager.flush();entityManager.clear();
        assertThat(adapter.findSetByIdAndOwner(setId,user)).get().satisfies(value->{
            assertThat(value.recommendations()).extracting(Recommendation::position).containsExactly(0,1,2,3);
            assertThat(value.recommendations()).extracting(Recommendation::evidenceIds).containsExactly(
                List.of(evidenceIds.get(0),evidenceIds.get(1)),
                List.of(evidenceIds.get(2),evidenceIds.get(3)),
                List.of(evidenceIds.get(4),evidenceIds.get(5)),
                List.of(evidenceIds.get(0),evidenceIds.get(5))
            );
        });
        assertThat(adapter.findSetsByOwner(user)).extracting(value->value.recommendationSetId()).containsExactly(setId);
        assertThat(adapter.findSetsByOwner(user)).singleElement().satisfies(value->{
            assertThat(value.recommendations()).extracting(Recommendation::position).containsExactly(0,1,2,3);
            assertThat(value.recommendations()).extracting(Recommendation::evidenceIds).containsExactly(
                List.of(evidenceIds.get(0),evidenceIds.get(1)),
                List.of(evidenceIds.get(2),evidenceIds.get(3)),
                List.of(evidenceIds.get(4),evidenceIds.get(5)),
                List.of(evidenceIds.get(0),evidenceIds.get(5))
            );
        });
        assertThat(adapter.findSetsByOwner(UUID.randomUUID())).isEmpty();
        assertThat(adapter.findSetByRecommendationIdAndOwner(recommendation0.recommendationId(),user)).isPresent();
        assertThat(adapter.findSetByRecommendationIdAndOwner(recommendation0.recommendationId(),UUID.randomUUID())).isEmpty();
        // ORDER BY e.created_at DESC,e.evidence_id: created_at is equal, so evidence_id ascending decides.
        assertThat(adapter.findEvidenceByRecommendationAndOwner(recommendation0.recommendationId(),user)).extracting(RecommendationEvidence::evidenceId,RecommendationEvidence::observedFactSummary)
            .containsExactly(tuple(evidenceIds.get(0),"test files"),tuple(evidenceIds.get(1),"test files 1"));
        assertThat(adapter.findEvidenceByRecommendationAndOwner(recommendation0.recommendationId(),UUID.randomUUID())).isEmpty();
        assertThat(adapter.findRoadmapByIdAndOwner(roadmap.roadmapId(),user)).get().satisfies(value->{
            assertThat(value.milestones()).extracting(RoadmapMilestone::position).containsExactly(0,1);
            assertThat(value.steps()).extracting(RoadmapStep::position).containsExactly(0,1,2,3);
            assertThat(value.steps()).filteredOn(step->step.milestoneId().equals(milestone0.milestoneId())).hasSize(2);
            assertThat(value.steps()).filteredOn(step->step.milestoneId().equals(milestone1.milestoneId())).hasSize(2);
            assertThat(value.milestones()).extracting(RoadmapMilestone::milestoneId,RoadmapMilestone::category,RoadmapMilestone::title,RoadmapMilestone::status)
                .containsExactly(tuple(milestone0.milestoneId(),milestone0.category(),milestone0.title(),milestone0.status()),
                    tuple(milestone1.milestoneId(),milestone1.category(),milestone1.title(),milestone1.status()));
            assertThat(value.steps()).extracting(RoadmapStep::stepId,RoadmapStep::milestoneId,RoadmapStep::recommendationId,RoadmapStep::category,RoadmapStep::title)
                .containsExactly(tuple(step0.stepId(),step0.milestoneId(),step0.recommendationId(),step0.category(),step0.title()),
                    tuple(step1.stepId(),step1.milestoneId(),step1.recommendationId(),step1.category(),step1.title()),
                    tuple(step2.stepId(),step2.milestoneId(),step2.recommendationId(),step2.category(),step2.title()),
                    tuple(step3.stepId(),step3.milestoneId(),step3.recommendationId(),step3.category(),step3.title()));
            assertThat(value.steps()).extracting(RoadmapStep::difficulty,RoadmapStep::effortHours,RoadmapStep::completionCriteria,RoadmapStep::status)
                .containsExactly(tuple(step0.difficulty(),step0.effortHours(),step0.completionCriteria(),step0.status()),
                    tuple(step1.difficulty(),step1.effortHours(),step1.completionCriteria(),step1.status()),
                    tuple(step2.difficulty(),step2.effortHours(),step2.completionCriteria(),step2.status()),
                    tuple(step3.difficulty(),step3.effortHours(),step3.completionCriteria(),step3.status()));
            assertThat(value.steps()).extracting(RoadmapStep::expectedEvidence)
                .containsExactly(step0.expectedEvidence(),step1.expectedEvidence(),step2.expectedEvidence(),step3.expectedEvidence());
            assertThat(value.steps()).extracting(RoadmapStep::prerequisiteStepIds)
                .containsExactly(step0.prerequisiteStepIds(),step1.prerequisiteStepIds(),step2.prerequisiteStepIds(),step3.prerequisiteStepIds());
        });
        assertThat(adapter.findRoadmapsByOwner(user)).extracting(value->value.roadmapId()).containsExactly(roadmap.roadmapId());
        assertThat(adapter.findRoadmapsByOwner(user)).singleElement().satisfies(value->{
            assertThat(value.milestones()).extracting(RoadmapMilestone::milestoneId).containsExactly(milestone0.milestoneId(),milestone1.milestoneId());
            assertThat(value.steps()).extracting(RoadmapStep::stepId).containsExactly(step0.stepId(),step1.stepId(),step2.stepId(),step3.stepId());
        });
        assertThat(adapter.findRoadmapsByOwner(UUID.randomUUID())).isEmpty();
        Instant archivedAt=now.plusSeconds(60);var archived=adapter.updateRoadmap(roadmap.archive(archivedAt));
        entityManager.flush();entityManager.clear();
        assertThat(archived.status()).isEqualTo("ARCHIVED");
        assertThat(adapter.findRoadmapByIdAndOwner(roadmap.roadmapId(),user)).get().satisfies(value->{assertThat(value.status()).isEqualTo("ARCHIVED");assertThat(value.updatedAt()).isEqualTo(archivedAt);assertThat(value.steps()).hasSize(4);});
        assertThat(adapter.findSetByIdAndOwner(setId,UUID.randomUUID())).isEmpty();
    }

    @Test void batchesOwnerHistoryAndKeepsDeterministicSelectionAndOrdering(){
        var adapter=new JpaRecommendationAdapter(policies,templates,sets,recommendations,evidence,roadmapPolicies,roadmaps,milestones,steps);
        UUID user=UUID.randomUUID(),identity=UUID.randomUUID(),repository=UUID.randomUUID(),snapshot=UUID.randomUUID(),evaluation=UUID.randomUUID(),matrix=UUID.randomUUID(),assessment=UUID.randomUUID(),readiness=UUID.randomUUID(),gap=UUID.randomUUID(),evidenceId=UUID.randomUUID();
        UUID otherUser=UUID.randomUUID(),otherIdentity=UUID.randomUUID(),otherRepository=UUID.randomUUID(),otherSnapshot=UUID.randomUUID(),otherEvaluation=UUID.randomUUID(),otherMatrix=UUID.randomUUID(),otherAssessment=UUID.randomUUID(),otherReadiness=UUID.randomUUID(),otherGap=UUID.randomUUID(),otherEvidence=UUID.randomUUID();
        Instant now=Instant.parse("2026-08-25T00:00:00Z");
        seedBasis(user,identity,repository,snapshot,evaluation,matrix,assessment,readiness,gap,evidenceId,now);
        seedBasis(otherUser,otherIdentity,otherRepository,otherSnapshot,otherEvaluation,otherMatrix,otherAssessment,otherReadiness,otherGap,otherEvidence,now);
        UUID legacyPolicy=UUID.fromString("41000000-0000-0000-0000-000000000099");
        UUID legacyRoadmapPolicy=UUID.fromString("42000000-0000-0000-0000-000000000099");
        seedLegacyPolicies(legacyPolicy,legacyRoadmapPolicy,now);
        UUID activePolicy=UUID.fromString("41000000-0000-0000-0000-000000000001");
        UUID activeRoadmapPolicy=UUID.fromString("42000000-0000-0000-0000-000000000001");
        RecommendationSet older=set(UUID.fromString("51000000-0000-0000-0000-000000000001"),user,readiness,legacyPolicy,"recommendation-v0",gap,evidenceId,now);
        RecommendationSet newer=set(UUID.fromString("51000000-0000-0000-0000-000000000002"),user,readiness,activePolicy,"recommendation-v1",gap,evidenceId,now);
        RecommendationSet foreign=set(UUID.fromString("51000000-0000-0000-0000-000000000003"),otherUser,otherReadiness,activePolicy,"recommendation-v1",otherGap,otherEvidence,now);
        adapter.saveSet(older);adapter.saveSet(newer);adapter.saveSet(foreign);
        LearningRoadmap olderRoadmap=roadmap(UUID.fromString("52000000-0000-0000-0000-000000000001"),older,legacyRoadmapPolicy,"roadmap-v0","ARCHIVED",now);
        LearningRoadmap newerRoadmap=roadmap(UUID.fromString("52000000-0000-0000-0000-000000000002"),newer,activeRoadmapPolicy,"roadmap-v1","CREATED",now);
        LearningRoadmap foreignRoadmap=roadmap(UUID.fromString("52000000-0000-0000-0000-000000000003"),foreign,activeRoadmapPolicy,"roadmap-v1","CREATED",now);
        adapter.saveRoadmap(olderRoadmap);adapter.saveRoadmap(newerRoadmap);adapter.saveRoadmap(foreignRoadmap);
        entityManager.flush();entityManager.clear();
        var statistics=entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        assertThat(adapter.findSetsByOwner(user)).containsExactly(newer,older);
        long twoSetQueries=statistics.getPrepareStatementCount();
        statistics.clear();
        assertThat(adapter.findSetsByOwner(otherUser)).containsExactly(foreign);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(twoSetQueries);
        assertThat(twoSetQueries).isEqualTo(4);
        assertThat(adapter.findSetByReadiness(user,readiness)).contains(newer);

        statistics.clear();
        assertThat(adapter.findRoadmapsByOwner(user)).containsExactly(newerRoadmap,olderRoadmap);
        long twoRoadmapQueries=statistics.getPrepareStatementCount();
        statistics.clear();
        assertThat(adapter.findRoadmapsByOwner(otherUser)).containsExactly(foreignRoadmap);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(twoRoadmapQueries);
        assertThat(twoRoadmapQueries).isEqualTo(4);
        assertThat(adapter.findRoadmapBySet(user,newer.recommendationSetId())).contains(newerRoadmap);
        assertThat(adapter.findSetByReadiness(otherUser,readiness)).isEmpty();
        assertThat(adapter.findRoadmapBySet(otherUser,newer.recommendationSetId())).isEmpty();
    }

    private RecommendationSet set(UUID id,UUID user,UUID readiness,UUID policy,String version,UUID gap,UUID evidenceId,Instant now){
        Recommendation recommendation=new Recommendation(UUID.randomUUID(),gap,RuleCategory.TESTING,RecommendationType.PROJECT,
            RecommendationPriority.HIGH,"CAREER_REQUIRED_GAP","Add testing","Reach 60",List.of("test files"),
            List.of(evidenceId),16,0,"PROPOSED");
        return new RecommendationSet(id,user,readiness,policy,version,"PUBLISHED",List.of(recommendation),now);
    }

    private LearningRoadmap roadmap(UUID id,RecommendationSet set,UUID policy,String version,String status,Instant now){
        Recommendation recommendation=set.recommendations().getFirst();UUID milestoneId=UUID.randomUUID();
        var milestone=new RoadmapMilestone(milestoneId,0,RuleCategory.TESTING,"Testing","PLANNED");
        var step=new RoadmapStep(UUID.randomUUID(),milestoneId,recommendation.recommendationId(),0,RuleCategory.TESTING,
            recommendation.title(),"INTERMEDIATE",recommendation.effortHours(),List.of(),
            recommendation.completionCriteria(),recommendation.expectedEvidence(),"NOT_STARTED");
        return new LearningRoadmap(id,set.userId(),set.recommendationSetId(),policy,version,status,
            new BigDecimal("0.00"),List.of(milestone),List.of(step),now,now);
    }

    private void seedLegacyPolicies(UUID recommendationPolicy,UUID roadmapPolicy,Instant now){var at=now.atOffset(ZoneOffset.UTC);
        jdbc.update("INSERT INTO recommendation_policies(recommendation_policy_id,version_label,status,effective_at,created_at) VALUES (?,?,?,?,?)",recommendationPolicy,"recommendation-v0","SUPERSEDED",at,at);
        jdbc.update("INSERT INTO roadmap_policies(roadmap_policy_id,version_label,recommendation_policy_id,status,effective_at,created_at) VALUES (?,?,?,?,?,?)",roadmapPolicy,"roadmap-v0",recommendationPolicy,"SUPERSEDED",at,at);
    }
    private void seedEvidence(UUID evidence,UUID user,UUID snapshot,int index,Instant now){var at=now.atOffset(ZoneOffset.UTC);
        jdbc.update("INSERT INTO evidence_records(evidence_id,user_id,snapshot_id,evidence_type,source_reference,source_reference_hash,observed_fact_summary,confidence,created_at) VALUES (?,?,?,?,?,?,?,?,?)",evidence,user,snapshot,"SNAPSHOT_SIGNAL","snapshot:"+snapshot+":TEST_FILES:"+index,Integer.toHexString(index+3).repeat(64),"test files "+index,100,at);
    }
    private void seedGap(UUID assessment,UUID gap,UUID matrix,UUID readiness,String skillId,String skillKey,String category){
        jdbc.update("INSERT INTO skill_assessments(skill_assessment_id,skill_matrix_id,skill_id,score,skill_level,confidence,strength_flag,weakness_flag,growth_trend,aggregate_rule_result_reference,rule_set_version) VALUES (?,?,CAST(? AS uuid),30,'BEGINNER',90,false,true,'UNAVAILABLE',?,'baseline-v2')",assessment,matrix,skillId,"evaluation:"+readiness);
        jdbc.update("INSERT INTO skill_gaps(skill_gap_id,career_readiness_id,skill_assessment_id,skill_id,skill_key,category,actual_score,actual_level,expected_minimum,gap_state,career_weight) VALUES (?,?,?,CAST(? AS uuid),?,?,30,'BEGINNER',60,'WEAK',20)",gap,readiness,assessment,skillId,skillKey,category);
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
