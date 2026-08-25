package com.devpath.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.analysis.application.AnalysisPersistencePort;
import com.devpath.analysis.application.AnalysisHistoryItemView;
import com.devpath.analysis.domain.AnalysisJob;
import com.devpath.career.application.CareerCatalogApplicationService;
import com.devpath.career.application.CareerReadinessApplicationService;
import com.devpath.career.application.CareerReadinessNotFoundException;
import com.devpath.career.application.CareerReadinessView;
import com.devpath.company.application.CompanyCatalogApplicationService;
import com.devpath.identity.application.UserPreferenceView;
import com.devpath.identity.application.UserProfileApplicationService;
import com.devpath.identity.domain.UserId;
import com.devpath.recommendation.application.RecommendationApplicationService;
import com.devpath.recommendation.application.RecommendationNotFoundException;
import com.devpath.recommendation.application.RecommendationSetView;
import com.devpath.recommendation.application.RecommendationView;
import com.devpath.learning.application.LearningRoadmapView;
import com.devpath.repository.application.RepositoryPersistencePort;
import com.devpath.repository.application.RepositorySynchronizationPersistencePort;
import com.devpath.repository.domain.Repository;
import com.devpath.repository.domain.RepositorySyncJob;
import com.devpath.rule.application.SkillMatrixApplicationService;
import com.devpath.rule.application.SkillMatrixNotFoundException;
import com.devpath.rule.application.SkillMatrixView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DashboardApplicationServiceTest {
    private static final UUID USER_ID = UUID.fromString("4f67cd50-a37a-4a0e-86ef-60e56e46836a");
    private static final Instant NOW = Instant.parse("2026-08-25T03:00:00Z");

    @Test
    void buildsAnOwnerScopedEmptySummaryAndAuditsTheView() {
        Fixture fixture = new Fixture();
        fixture.stubEmpty();

        DashboardSummaryView result = fixture.service().getSummary(USER_ID);

        assertThat(result.generatedAt()).isEqualTo(NOW);
        assertThat(result.targets().status()).isEqualTo(DashboardSummaryView.SourceStatus.EMPTY);
        assertThat(result.repositories().status()).isEqualTo(DashboardSummaryView.SourceStatus.EMPTY);
        assertThat(result.analyses().status()).isEqualTo(DashboardSummaryView.SourceStatus.EMPTY);
        assertThat(result.skillOverview().status()).isEqualTo(DashboardSummaryView.SourceStatus.EMPTY);
        assertThat(result.readiness().status()).isEqualTo(DashboardSummaryView.SourceStatus.EMPTY);
        assertThat(result.recommendations().status()).isEqualTo(DashboardSummaryView.SourceStatus.EMPTY);
        assertThat(result.roadmap().status()).isEqualTo(DashboardSummaryView.SourceStatus.EMPTY);
        assertThat(result.recentJobs().status()).isEqualTo(DashboardSummaryView.SourceStatus.EMPTY);
        verify(fixture.repositories).countByOwner(USER_ID, false);
        verify(fixture.analyses).countHistoryByOwner(USER_ID);
        verify(fixture.audit).record(DashboardAuditEvent.DASHBOARD_SUMMARY_VIEWED, USER_ID, NOW);
    }

    @Test
    void preservesAvailableSectionsWhenAnotherSourceFails() {
        Fixture fixture = new Fixture();
        fixture.stubEmpty();
        when(fixture.repositories.countByOwner(USER_ID, false)).thenThrow(new IllegalStateException("database"));
        reset(fixture.skillMatrices);
        when(fixture.skillMatrices.getCurrent(USER_ID)).thenThrow(new IllegalStateException("database"));

        DashboardSummaryView result = fixture.service().getSummary(USER_ID);

        assertThat(result.repositories().status()).isEqualTo(DashboardSummaryView.SourceStatus.UNAVAILABLE);
        assertThat(result.skillOverview().status()).isEqualTo(DashboardSummaryView.SourceStatus.UNAVAILABLE);
        assertThat(result.analyses().status()).isEqualTo(DashboardSummaryView.SourceStatus.EMPTY);
        assertThat(result.readiness().status()).isEqualTo(DashboardSummaryView.SourceStatus.EMPTY);
    }

    @Test
    void mapsOfficialResultsAndOrdersRecentJobsWithoutRecalculatingThem() {
        Fixture fixture = new Fixture();
        fixture.stubEmpty();
        UUID repositoryId = UUID.randomUUID();
        Repository repository = Repository.discover(USER_ID, UUID.randomUUID(), "42", "devpath", "owner/devpath",
            "owner", false, "main", false, "https://github.com/owner/devpath", NOW.minusSeconds(60))
            .markSynchronized(UUID.randomUUID(), NOW.minusSeconds(30));
        when(fixture.repositories.countByOwner(USER_ID, false)).thenReturn(1L);
        when(fixture.repositories.countByOwnerAndSyncStatus(USER_ID, "SYNCHRONIZED", false)).thenReturn(1L);
        when(fixture.repositories.findPageByOwner(USER_ID, 0, 8, false)).thenReturn(List.of(repository));
        var analysis = new AnalysisHistoryItemView(UUID.randomUUID(), repositoryId, "owner/devpath",
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "REPOSITORY_BASELINE",
            new BigDecimal("82.50"), new BigDecimal("90.00"), "rules-v1", "skills-v1", true, NOW);
        when(fixture.analyses.countHistoryByOwner(USER_ID)).thenReturn(1L);
        when(fixture.analyses.findHistoryByOwner(USER_ID, 0, 8)).thenReturn(List.of(analysis));
        reset(fixture.skillMatrices, fixture.readiness, fixture.recommendations);
        when(fixture.skillMatrices.getCurrent(USER_ID)).thenReturn(new SkillMatrixView(UUID.randomUUID(),
            UUID.randomUUID(), "skills-v1", "rules-v1", "CURRENT", List.of(), List.of("testing"),
            List.of("documentation"), NOW));
        when(fixture.readiness.getCurrent(USER_ID)).thenReturn(new CareerReadinessView(UUID.randomUUID(),
            UUID.randomUUID(), "backend", UUID.randomUUID(), "career-v2", "readiness-v1", "rules-v1",
            "COMPLETED", new BigDecimal("76.50"), "COMPETENT", new BigDecimal("88.00"), List.of(),
            List.of(), NOW));
        var recommendations = java.util.stream.IntStream.range(0, 4).mapToObj(index -> new RecommendationView(
            UUID.randomUUID(), UUID.randomUUID(), "TESTING", "PROJECT", "HIGH", "CAREER_REQUIRED_GAP",
            "Recommendation " + index, "Reach 60", List.of("tests"), List.of(), 8, index, "PROPOSED"
        )).toList();
        UUID setId = UUID.randomUUID();
        when(fixture.recommendations.getCurrent(USER_ID)).thenReturn(new RecommendationSetView(
            setId, UUID.randomUUID(), "recommendation-v1", "PUBLISHED", recommendations, NOW));
        when(fixture.recommendations.getActiveRoadmap(USER_ID)).thenReturn(new LearningRoadmapView(UUID.randomUUID(),
            setId, "roadmap-v1", "CREATED", new BigDecimal("25.00"), List.of(), List.of(), NOW, NOW));
        RepositorySyncJob syncJob = RepositorySyncJob.queue(USER_ID, repositoryId, "sync-key", NOW.minusSeconds(20));
        AnalysisJob analysisJob = AnalysisJob.queue(USER_ID, repositoryId, UUID.randomUUID(), "analysis-key",
            "REPOSITORY_BASELINE", NOW.minusSeconds(10));
        when(fixture.repositoryJobs.findRecentByOwner(USER_ID, 8)).thenReturn(List.of(syncJob));
        when(fixture.analyses.findRecentJobsByOwner(USER_ID, 8)).thenReturn(List.of(analysisJob));

        DashboardSummaryView result = fixture.service().getSummary(USER_ID);

        assertThat(result.repositories().synchronizedCount()).isEqualTo(1);
        assertThat(result.analyses().latest().overallScore()).isEqualByComparingTo("82.50");
        assertThat(result.skillOverview().strengthCount()).isEqualTo(1);
        assertThat(result.readiness().score()).isEqualByComparingTo("76.50");
        assertThat(result.recommendations().items()).hasSize(3);
        assertThat(result.roadmap().progressPercent()).isEqualByComparingTo("25.00");
        assertThat(result.recentJobs().items()).extracting(DashboardSummaryView.JobItem::jobType)
            .containsExactly("ANALYSIS", "REPOSITORY_SYNC");
    }

    private static final class Fixture {
        private final UserProfileApplicationService profiles = mock(UserProfileApplicationService.class);
        private final CareerCatalogApplicationService careers = mock(CareerCatalogApplicationService.class);
        private final CompanyCatalogApplicationService companies = mock(CompanyCatalogApplicationService.class);
        private final RepositoryPersistencePort repositories = mock(RepositoryPersistencePort.class);
        private final RepositorySynchronizationPersistencePort repositoryJobs =
            mock(RepositorySynchronizationPersistencePort.class);
        private final AnalysisPersistencePort analyses = mock(AnalysisPersistencePort.class);
        private final SkillMatrixApplicationService skillMatrices = mock(SkillMatrixApplicationService.class);
        private final CareerReadinessApplicationService readiness = mock(CareerReadinessApplicationService.class);
        private final RecommendationApplicationService recommendations = mock(RecommendationApplicationService.class);
        private final DashboardAuditPort audit = mock(DashboardAuditPort.class);

        void stubEmpty() {
            when(profiles.getPreferences(new UserId(USER_ID))).thenReturn(new UserPreferenceView(null, null, null));
            when(repositories.countByOwner(USER_ID, false)).thenReturn(0L);
            when(repositories.findPageByOwner(USER_ID, 0, 8, false)).thenReturn(List.of());
            when(analyses.countHistoryByOwner(USER_ID)).thenReturn(0L);
            when(analyses.findHistoryByOwner(USER_ID, 0, 8)).thenReturn(List.of());
            when(repositoryJobs.findRecentByOwner(USER_ID, 8)).thenReturn(List.of());
            when(analyses.findRecentJobsByOwner(USER_ID, 8)).thenReturn(List.of());
            when(skillMatrices.getCurrent(USER_ID)).thenThrow(new SkillMatrixNotFoundException());
            when(readiness.getCurrent(USER_ID)).thenThrow(new CareerReadinessNotFoundException());
            when(recommendations.getCurrent(USER_ID)).thenThrow(new RecommendationNotFoundException());
            when(recommendations.getActiveRoadmap(USER_ID)).thenThrow(new RecommendationNotFoundException());
        }

        DashboardApplicationService service() {
            return new DashboardApplicationService(profiles, careers, companies, repositories, repositoryJobs,
                analyses, skillMatrices, readiness, recommendations, audit,
                Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }
}
