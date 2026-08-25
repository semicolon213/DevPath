package com.devpath.dashboard.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardSummaryView(
    Instant generatedAt,
    TargetSummary targets,
    RepositorySummary repositories,
    AnalysisSummary analyses,
    SkillSummary skillOverview,
    ReadinessSummary readiness,
    RecommendationSummary recommendations,
    RoadmapSummary roadmap,
    JobSummary recentJobs
) {
    public enum SourceStatus { AVAILABLE, EMPTY, UNAVAILABLE }

    public record TargetSummary(SourceStatus status, Target career, Target company) {}
    public record Target(String id, String localizedName, String profileVersion) {}

    public record RepositorySummary(
        SourceStatus status, long totalCount, long synchronizedCount, List<RepositoryItem> recent
    ) {
        public RepositorySummary { recent = List.copyOf(recent); }
    }
    public record RepositoryItem(
        UUID repositoryId, String fullName, String syncStatus, Instant lastSyncedAt
    ) {}

    public record AnalysisSummary(
        SourceStatus status, long totalCount, AnalysisItem latest, List<AnalysisItem> currentByRepository
    ) {
        public AnalysisSummary { currentByRepository = List.copyOf(currentByRepository); }
    }
    public record AnalysisItem(
        UUID analysisId, UUID repositoryId, String repositoryFullName, BigDecimal overallScore,
        BigDecimal confidence, boolean currentForRepository, Instant completedAt
    ) {}

    public record SkillSummary(
        SourceStatus status, UUID skillMatrixId, int skillCount, int strengthCount, int weaknessCount,
        String policyVersion, String ruleSetVersion, Instant generatedAt
    ) {}

    public record ReadinessSummary(
        SourceStatus status, UUID careerReadinessId, String resultStatus, BigDecimal score,
        String level, BigDecimal confidence, List<String> unavailableCategories, Instant assessedAt
    ) {
        public ReadinessSummary { unavailableCategories = List.copyOf(unavailableCategories); }
    }

    public record RecommendationSummary(
        SourceStatus status, UUID recommendationSetId, String policyVersion,
        List<RecommendationItem> items, Instant generatedAt
    ) {
        public RecommendationSummary { items = List.copyOf(items); }
    }
    public record RecommendationItem(
        UUID recommendationId, String category, String type, String priority, String title,
        int effortHours, int position, String status
    ) {}

    public record RoadmapSummary(
        SourceStatus status, UUID roadmapId, String policyVersion, String resultStatus,
        BigDecimal progressPercent, int milestoneCount, int stepCount, Instant updatedAt
    ) {}

    public record JobSummary(SourceStatus status, List<JobItem> items) {
        public JobSummary { items = List.copyOf(items); }
    }
    public record JobItem(
        UUID jobId, String jobType, UUID repositoryId, String status, String phase,
        int progressPercent, Instant submittedAt, Instant completedAt
    ) {}
}
