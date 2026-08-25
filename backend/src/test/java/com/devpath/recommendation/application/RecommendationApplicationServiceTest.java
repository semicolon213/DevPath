package com.devpath.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.devpath.career.domain.CareerReadiness;
import com.devpath.career.domain.CareerReadinessStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import com.devpath.learning.application.LearningAuditPort;
import com.devpath.learning.application.LearningAuditEvent;
import com.devpath.learning.domain.LearningRoadmap;
import com.devpath.recommendation.domain.Recommendation;
import com.devpath.recommendation.domain.RecommendationEvidence;
import com.devpath.recommendation.domain.RecommendationPriority;
import com.devpath.recommendation.domain.RecommendationSet;
import com.devpath.recommendation.domain.RecommendationType;
import com.devpath.rule.domain.RuleCategory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationApplicationServiceTest {
    @Test
    void doesNotInventRecommendationsForInsufficientReadiness() {
        RecommendationPersistencePort persistence=mock(RecommendationPersistencePort.class);
        var service=new RecommendationApplicationService(persistence,mock(LearningAuditPort.class),Clock.systemUTC());
        var readiness=new CareerReadiness(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"backend",
            UUID.randomUUID(),"career-v2",UUID.randomUUID(),"readiness-v1","baseline-v2",
            CareerReadinessStatus.INSUFFICIENT_EVIDENCE,null,null,new BigDecimal("50"),List.of("DATABASE"),List.of(),
            Instant.parse("2026-08-25T00:00:00Z"));

        assertThat(service.generate(readiness,Instant.parse("2026-08-25T00:00:01Z"))).isEmpty();
        verifyNoInteractions(persistence);
    }

    @Test
    void listsOwnerRoadmapsAndArchivesTheSelectedRoadmapWithAnAuditRecord() {
        UUID userId=UUID.randomUUID();UUID roadmapId=UUID.randomUUID();
        Instant generatedAt=Instant.parse("2026-08-24T00:00:00Z");Instant archivedAt=Instant.parse("2026-08-25T00:00:00Z");
        LearningRoadmap roadmap=new LearningRoadmap(roadmapId,userId,UUID.randomUUID(),UUID.randomUUID(),
            "roadmap-v1","CREATED",BigDecimal.ZERO,List.of(),List.of(),generatedAt,generatedAt);
        RecommendationPersistencePort persistence=mock(RecommendationPersistencePort.class);
        LearningAuditPort audit=mock(LearningAuditPort.class);
        when(persistence.findRoadmapsByOwner(userId)).thenReturn(List.of(roadmap));
        when(persistence.findRoadmapByIdAndOwner(roadmapId,userId)).thenReturn(java.util.Optional.of(roadmap));
        when(persistence.updateRoadmap(any(LearningRoadmap.class))).thenAnswer(invocation->invocation.getArgument(0));
        var service=new RecommendationApplicationService(persistence,audit,Clock.fixed(archivedAt,java.time.ZoneOffset.UTC));

        assertThat(service.listRoadmaps(userId).roadmaps()).hasSize(1);
        assertThat(service.archiveRoadmap(userId,roadmapId).status()).isEqualTo("ARCHIVED");
        verify(audit).record(LearningAuditEvent.ROADMAP_ARCHIVED,userId,roadmapId,archivedAt);
    }

    @Test
    void repeatedArchiveReturnsCurrentStateWithoutAnotherWriteOrAudit() {
        UUID userId=UUID.randomUUID();UUID roadmapId=UUID.randomUUID();Instant now=Instant.parse("2026-08-25T00:00:00Z");
        LearningRoadmap archived=new LearningRoadmap(roadmapId,userId,UUID.randomUUID(),UUID.randomUUID(),
            "roadmap-v1","ARCHIVED",BigDecimal.ZERO,List.of(),List.of(),now,now);
        RecommendationPersistencePort persistence=mock(RecommendationPersistencePort.class);
        LearningAuditPort audit=mock(LearningAuditPort.class);
        when(persistence.findRoadmapByIdAndOwner(roadmapId,userId)).thenReturn(java.util.Optional.of(archived));
        var service=new RecommendationApplicationService(persistence,audit,Clock.fixed(now,java.time.ZoneOffset.UTC));

        assertThat(service.archiveRoadmap(userId,roadmapId).status()).isEqualTo("ARCHIVED");
        verifyNoInteractions(audit);
        org.mockito.Mockito.verify(persistence,org.mockito.Mockito.never()).updateRoadmap(any());
    }

    @Test
    void exposesOwnerScopedRecommendationHistoryDetailAndEvidence() {
        UUID userId=UUID.randomUUID();UUID recommendationId=UUID.randomUUID();UUID evidenceId=UUID.randomUUID();
        Instant now=Instant.parse("2026-08-25T00:00:00Z");
        Recommendation recommendation=new Recommendation(recommendationId,UUID.randomUUID(),RuleCategory.TESTING,
            RecommendationType.PROJECT,RecommendationPriority.HIGH,"CAREER_REQUIRED_GAP","Add testing","Reach 60",
            List.of("test files"),List.of(evidenceId),16,0,"PROPOSED");
        RecommendationSet set=new RecommendationSet(UUID.randomUUID(),userId,UUID.randomUUID(),UUID.randomUUID(),
            "recommendation-v1","PUBLISHED",List.of(recommendation),now);
        RecommendationPersistencePort persistence=mock(RecommendationPersistencePort.class);
        when(persistence.findSetsByOwner(userId)).thenReturn(List.of(set));
        when(persistence.findSetByRecommendationIdAndOwner(recommendationId,userId)).thenReturn(java.util.Optional.of(set));
        when(persistence.findEvidenceByRecommendationAndOwner(recommendationId,userId)).thenReturn(List.of(
            new RecommendationEvidence(evidenceId,"SNAPSHOT_SIGNAL","README.md","README is present",
                new BigDecimal("100"),now)));
        var service=new RecommendationApplicationService(persistence,mock(LearningAuditPort.class),Clock.systemUTC());

        assertThat(service.listRecommendationSets(userId).recommendationSets()).hasSize(1);
        assertThat(service.getRecommendation(userId,recommendationId).title()).isEqualTo("Add testing");
        assertThat(service.getRecommendationEvidence(userId,recommendationId).evidence()).singleElement()
            .satisfies(item->assertThat(item.sourceReference()).isEqualTo("README.md"));
    }
}
