import { apiRequest } from "../../../shared/api/apiClient";

export type DashboardSourceStatus = "AVAILABLE" | "EMPTY" | "UNAVAILABLE";
export type DashboardTarget = { id: string; localizedName: string; profileVersion: string };
export type DashboardAnalysis = {
  analysisId: string; repositoryId: string; repositoryFullName: string; overallScore: number;
  confidence: number; currentForRepository: boolean; completedAt: string;
};
export type DashboardRecommendation = {
  recommendationId: string; category: string; type: string; priority: string; title: string;
  effortHours: number; position: number; status: string;
};
export type DashboardJob = {
  jobId: string; jobType: "REPOSITORY_SYNC" | "ANALYSIS"; repositoryId: string;
  status: string; phase: string; progressPercent: number; submittedAt: string; completedAt: string | null;
};

export type DashboardSummary = {
  generatedAt: string;
  targets: { status: DashboardSourceStatus; career: DashboardTarget | null; company: DashboardTarget | null };
  repositories: {
    status: DashboardSourceStatus; totalCount: number; synchronizedCount: number;
    recent: { repositoryId: string; fullName: string; syncStatus: string; lastSyncedAt: string | null }[];
  };
  analyses: {
    status: DashboardSourceStatus; totalCount: number; latest: DashboardAnalysis | null;
    currentByRepository: DashboardAnalysis[];
  };
  skillOverview: {
    status: DashboardSourceStatus; skillMatrixId: string | null; skillCount: number;
    strengthCount: number; weaknessCount: number; policyVersion: string | null;
    ruleSetVersion: string | null; generatedAt: string | null;
  };
  readiness: {
    status: DashboardSourceStatus; careerReadinessId: string | null; resultStatus: string | null;
    score: number | null; level: string | null; confidence: number | null;
    unavailableCategories: string[]; assessedAt: string | null;
  };
  recommendations: {
    status: DashboardSourceStatus; recommendationSetId: string | null; policyVersion: string | null;
    items: DashboardRecommendation[]; generatedAt: string | null;
  };
  roadmap: {
    status: DashboardSourceStatus; roadmapId: string | null; policyVersion: string | null;
    resultStatus: string | null; progressPercent: number | null; milestoneCount: number;
    stepCount: number; updatedAt: string | null;
  };
  recentJobs: { status: DashboardSourceStatus; items: DashboardJob[] };
};

export async function getDashboardSummary(): Promise<DashboardSummary> {
  return (await apiRequest<DashboardSummary>("/api/v1/dashboard/summary")).data;
}
