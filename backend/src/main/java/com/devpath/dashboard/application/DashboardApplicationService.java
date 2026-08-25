package com.devpath.dashboard.application;

import com.devpath.analysis.application.AnalysisHistoryItemView;
import com.devpath.analysis.application.AnalysisPersistencePort;
import com.devpath.career.application.CareerCatalogApplicationService;
import com.devpath.career.application.CareerReadinessApplicationService;
import com.devpath.career.application.CareerReadinessNotFoundException;
import com.devpath.company.application.CompanyCatalogApplicationService;
import com.devpath.dashboard.application.DashboardSummaryView.AnalysisItem;
import com.devpath.dashboard.application.DashboardSummaryView.SourceStatus;
import com.devpath.identity.application.UserProfileApplicationService;
import com.devpath.identity.domain.UserId;
import com.devpath.recommendation.application.RecommendationApplicationService;
import com.devpath.recommendation.application.RecommendationNotFoundException;
import com.devpath.repository.application.RepositoryPersistencePort;
import com.devpath.repository.application.RepositorySynchronizationPersistencePort;
import com.devpath.rule.application.SkillMatrixApplicationService;
import com.devpath.rule.application.SkillMatrixNotFoundException;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardApplicationService {
    private static final int RECENT_LIMIT = 8;
    private final UserProfileApplicationService profiles;
    private final CareerCatalogApplicationService careers;
    private final CompanyCatalogApplicationService companies;
    private final RepositoryPersistencePort repositories;
    private final RepositorySynchronizationPersistencePort repositoryJobs;
    private final AnalysisPersistencePort analyses;
    private final SkillMatrixApplicationService skillMatrices;
    private final CareerReadinessApplicationService readiness;
    private final RecommendationApplicationService recommendations;
    private final DashboardAuditPort audit;
    private final Clock clock;

    public DashboardApplicationService(
        UserProfileApplicationService profiles,
        CareerCatalogApplicationService careers,
        CompanyCatalogApplicationService companies,
        RepositoryPersistencePort repositories,
        RepositorySynchronizationPersistencePort repositoryJobs,
        AnalysisPersistencePort analyses,
        SkillMatrixApplicationService skillMatrices,
        CareerReadinessApplicationService readiness,
        RecommendationApplicationService recommendations,
        DashboardAuditPort audit,
        Clock clock
    ) {
        this.profiles = profiles; this.careers = careers; this.companies = companies;
        this.repositories = repositories; this.repositoryJobs = repositoryJobs; this.analyses = analyses;
        this.skillMatrices = skillMatrices; this.readiness = readiness; this.recommendations = recommendations;
        this.audit = audit; this.clock = clock;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DashboardSummaryView getSummary(UUID userId) {
        var result = new DashboardSummaryView(
            clock.instant(), targets(userId), repositorySummary(userId), analysisSummary(userId),
            skillSummary(userId), readinessSummary(userId), recommendationSummary(userId),
            roadmapSummary(userId), jobSummary(userId)
        );
        audit.record(DashboardAuditEvent.DASHBOARD_SUMMARY_VIEWED, userId, clock.instant());
        return result;
    }

    private DashboardSummaryView.TargetSummary targets(UUID userId) {
        return safe(() -> {
            var preferences = profiles.getPreferences(new UserId(userId));
            var career = preferences.careerId() == null ? null : careers.get(preferences.careerId());
            var company = preferences.companyId() == null ? null : companies.get(preferences.companyId());
            var careerTarget = career == null ? null
                : new DashboardSummaryView.Target(career.careerId(), career.localizedName(), career.profileVersion());
            var companyTarget = company == null ? null
                : new DashboardSummaryView.Target(company.companyId(), company.localizedName(), company.profileVersion());
            var status = careerTarget == null && companyTarget == null ? SourceStatus.EMPTY : SourceStatus.AVAILABLE;
            return new DashboardSummaryView.TargetSummary(status, careerTarget, companyTarget);
        }, new DashboardSummaryView.TargetSummary(SourceStatus.UNAVAILABLE, null, null));
    }

    private DashboardSummaryView.RepositorySummary repositorySummary(UUID userId) {
        return safe(() -> {
            long total = repositories.countByOwner(userId, false);
            var recent = repositories.findPageByOwner(userId, 0, RECENT_LIMIT, false).stream()
                .map(value -> new DashboardSummaryView.RepositoryItem(
                    value.id(), value.fullName(), value.syncStatus().name(), value.lastSyncedAt()))
                .toList();
            return new DashboardSummaryView.RepositorySummary(total == 0 ? SourceStatus.EMPTY : SourceStatus.AVAILABLE,
                total, repositories.countByOwnerAndSyncStatus(userId, "SYNCHRONIZED", false), recent);
        }, new DashboardSummaryView.RepositorySummary(SourceStatus.UNAVAILABLE, 0, 0, List.of()));
    }

    private DashboardSummaryView.AnalysisSummary analysisSummary(UUID userId) {
        return safe(() -> {
            long total = analyses.countHistoryByOwner(userId);
            var recent = analyses.findHistoryByOwner(userId, 0, RECENT_LIMIT);
            var items = recent.stream().map(this::analysisItem).toList();
            return new DashboardSummaryView.AnalysisSummary(total == 0 ? SourceStatus.EMPTY : SourceStatus.AVAILABLE,
                total, items.stream().findFirst().orElse(null),
                items.stream().filter(AnalysisItem::currentForRepository).toList());
        }, new DashboardSummaryView.AnalysisSummary(SourceStatus.UNAVAILABLE, 0, null, List.of()));
    }

    private AnalysisItem analysisItem(AnalysisHistoryItemView value) {
        return new AnalysisItem(value.analysisId(), value.repositoryId(), value.repositoryFullName(),
            value.overallScore(), value.confidence(), value.currentForRepository(), value.completedAt());
    }

    private DashboardSummaryView.SkillSummary skillSummary(UUID userId) {
        var empty = new DashboardSummaryView.SkillSummary(SourceStatus.EMPTY, null, 0, 0, 0, null, null, null);
        var unavailable = new DashboardSummaryView.SkillSummary(
            SourceStatus.UNAVAILABLE, null, 0, 0, 0, null, null, null);
        return safeOptional(() -> {
            var value = skillMatrices.getCurrent(userId);
            return new DashboardSummaryView.SkillSummary(SourceStatus.AVAILABLE, value.skillMatrixId(),
                value.skills().size(), value.strengths().size(), value.weaknesses().size(),
                value.policyVersion(), value.ruleSetVersion(), value.generatedAt());
        }, empty, unavailable, SkillMatrixNotFoundException.class);
    }

    private DashboardSummaryView.ReadinessSummary readinessSummary(UUID userId) {
        var empty = new DashboardSummaryView.ReadinessSummary(
            SourceStatus.EMPTY, null, null, null, null, null, List.of(), null);
        var unavailable = new DashboardSummaryView.ReadinessSummary(
            SourceStatus.UNAVAILABLE, null, null, null, null, null, List.of(), null);
        return safeOptional(() -> {
            var value = readiness.getCurrent(userId);
            return new DashboardSummaryView.ReadinessSummary(SourceStatus.AVAILABLE, value.careerReadinessId(),
                value.status(), value.readinessScore(), value.readinessLevel(), value.confidence(),
                value.unavailableCategories(), value.assessedAt());
        }, empty, unavailable, CareerReadinessNotFoundException.class);
    }

    private DashboardSummaryView.RecommendationSummary recommendationSummary(UUID userId) {
        var empty = new DashboardSummaryView.RecommendationSummary(
            SourceStatus.EMPTY, null, null, List.of(), null);
        var unavailable = new DashboardSummaryView.RecommendationSummary(
            SourceStatus.UNAVAILABLE, null, null, List.of(), null);
        return safeOptional(() -> {
            var value = recommendations.getCurrent(userId);
            var items = value.recommendations().stream().limit(3)
                .map(item -> new DashboardSummaryView.RecommendationItem(item.recommendationId(), item.category(),
                    item.type(), item.priority(), item.title(), item.effortHours(), item.position(), item.status()))
                .toList();
            return new DashboardSummaryView.RecommendationSummary(
                items.isEmpty() ? SourceStatus.EMPTY : SourceStatus.AVAILABLE,
                value.recommendationSetId(), value.policyVersion(), items, value.generatedAt());
        }, empty, unavailable, RecommendationNotFoundException.class);
    }

    private DashboardSummaryView.RoadmapSummary roadmapSummary(UUID userId) {
        var empty = new DashboardSummaryView.RoadmapSummary(
            SourceStatus.EMPTY, null, null, null, null, 0, 0, null);
        var unavailable = new DashboardSummaryView.RoadmapSummary(
            SourceStatus.UNAVAILABLE, null, null, null, null, 0, 0, null);
        return safeOptional(() -> {
            var value = recommendations.getActiveRoadmap(userId);
            return new DashboardSummaryView.RoadmapSummary(SourceStatus.AVAILABLE, value.roadmapId(),
                value.policyVersion(), value.status(), value.progressPercent(), value.milestones().size(),
                value.steps().size(), value.updatedAt());
        }, empty, unavailable, RecommendationNotFoundException.class);
    }

    private DashboardSummaryView.JobSummary jobSummary(UUID userId) {
        return safe(() -> {
            var sync = repositoryJobs.findRecentByOwner(userId, RECENT_LIMIT).stream()
                .map(value -> new DashboardSummaryView.JobItem(value.id(), "REPOSITORY_SYNC", value.repositoryId(),
                    value.status().name(), value.phase(), value.progressPercent(), value.submittedAt(), value.completedAt()));
            var analysis = analyses.findRecentJobsByOwner(userId, RECENT_LIMIT).stream()
                .map(value -> new DashboardSummaryView.JobItem(value.id(), "ANALYSIS", value.repositoryId(),
                    value.status().name(), value.phase(), value.progressPercent(), value.submittedAt(), value.completedAt()));
            var items = java.util.stream.Stream.concat(sync, analysis)
                .sorted(Comparator.comparing(DashboardSummaryView.JobItem::submittedAt).reversed())
                .limit(RECENT_LIMIT).toList();
            return new DashboardSummaryView.JobSummary(
                items.isEmpty() ? SourceStatus.EMPTY : SourceStatus.AVAILABLE, items);
        }, new DashboardSummaryView.JobSummary(SourceStatus.UNAVAILABLE, List.of()));
    }

    private <T> T safe(Supplier<T> supplier, T fallback) {
        try { return supplier.get(); } catch (RuntimeException ignored) { return fallback; }
    }

    private <T> T safeOptional(
        Supplier<T> supplier, T empty, T unavailable, Class<? extends RuntimeException> notFoundType
    ) {
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            return notFoundType.isInstance(exception) ? empty : unavailable;
        }
    }
}
