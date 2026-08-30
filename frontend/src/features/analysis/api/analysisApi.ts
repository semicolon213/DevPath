import { apiRequest, withCsrf } from "../../../shared/api/apiClient";
import { getRepository, type ImportedRepository } from "../../repositories/api/repositoryApi";
import { getSkillMatrix, type SkillMatrix } from "../../skills/api/skillMatrixApi";

export type AnalysisJob = {
  jobId: string;
  jobType: "REPOSITORY_ANALYSIS";
  status: "queued" | "running" | "succeeded" | "failed" | "cancelled" | "expired";
  phase: "QUEUED" | "EVALUATING_RULES" | "RETRY_WAIT" | "COMPLETED" | "FAILED";
  progressPercent: number;
  attemptCount: number;
  maxAttempts: number;
  submittedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  pollingUrl: string;
  resultResourceUrl: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  retryable: boolean;
};

export type AnalysisResult = {
  analysisId: string;
  repositoryId: string;
  snapshotId: string;
  evaluationId: string;
  skillMatrixId: string;
  analysisScope: "REPOSITORY_BASELINE";
  currentForRepository: boolean;
  completedAt: string;
};

export type AnalysisHistoryItem = AnalysisResult & {
  repositoryFullName: string;
  overallScore: number;
  confidence: number;
  ruleSetVersion: string;
  policyVersion: string;
};

export type AnalysisHistory = {
  analyses: AnalysisHistoryItem[];
  limit: number;
  nextCursor: string | null;
  totalCount: number;
};

export type RuleResult = {
  ruleId: string;
  ruleVersion: string;
  status: "PASSED" | "FAILED" | "PARTIAL" | "SKIPPED" | "ERROR";
  rawValue: number;
  score: number;
  weight: number;
  formulaId: string;
  trace: string;
  evidenceReferences: string[];
};

export type RuleCategoryScore = {
  category: "LANGUAGE" | "FRAMEWORK" | "TESTING" | "DOCUMENTATION" | "ACTIVITY";
  score: number;
  weight: number;
  confidence: number;
  ruleResults: RuleResult[];
  missingEvidence: string[];
};

export type RuleEvaluation = {
  evaluationId: string;
  snapshotId: string;
  ruleSetVersionId: string;
  ruleSetVersion: string;
  formulaLibraryVersion: string;
  extractorVersion: string;
  overallScore: number;
  confidence: number;
  evidenceSummary: { evidenceCount: number; rulesWithEvidence: number; missingEvidenceCount: number };
  categoryScores: RuleCategoryScore[];
  warnings: string[];
  completedAt: string;
};

export type RuleEvidence = {
  evidenceId: string;
  ruleId: string;
  contributionRole: "DIRECT" | "SUPPORTING" | "MISSING";
  evidenceType: "REPOSITORY_PATH" | "LANGUAGE_STATISTIC" | "SNAPSHOT_SIGNAL";
  sourceReference: string;
  observedFactSummary: string;
  confidence: number;
};

export type AnalysisDetail = {
  result: AnalysisResult;
  repository: ImportedRepository;
  evaluation: RuleEvaluation;
  evidence: RuleEvidence[];
  matrix: SkillMatrix;
};
export type AnalysisComparison = { analyses: AnalysisHistoryItem[] };
export type AnalysisComparisonDetail = { analyses: Array<{ summary: AnalysisHistoryItem; evaluation: RuleEvaluation }> };

export async function requestAnalysis(repositoryId: string) {
  return apiRequest<AnalysisJob>("/api/v1/analyses", await withCsrf({
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": crypto.randomUUID()
    },
    body: JSON.stringify({ repositoryId, analysisScope: "REPOSITORY_BASELINE" })
  }));
}

export async function getAnalysisJob(jobId: string) {
  return apiRequest<AnalysisJob>(`/api/v1/analysis-jobs/${jobId}`);
}

export async function getAnalysisHistory(cursor: string | null = null) {
  const query = new URLSearchParams({ limit: "20" });
  if (cursor) query.set("cursor", cursor);
  return apiRequest<AnalysisHistory>(`/api/v1/analyses?${query}`);
}

export async function getRepositoryAnalysisHistory(repositoryId: string, cursor: string | null = null) {
  const query = new URLSearchParams({ limit: "20" });
  if (cursor) query.set("cursor", cursor);
  return apiRequest<AnalysisHistory>(`/api/v1/repositories/${repositoryId}/analyses?${query}`);
}

export async function getAnalysisResult(analysisId: string) {
  return apiRequest<AnalysisResult>(`/api/v1/analyses/${analysisId}`);
}

export async function getAnalysisComparison(analysisIds:string[]):Promise<AnalysisComparisonDetail>{
  const query=new URLSearchParams();analysisIds.forEach(id=>query.append("analysisId",id));
  const comparison=await apiRequest<AnalysisComparison>(`/api/v1/analyses/compare?${query}`);
  const evaluations=await Promise.all(comparison.analyses.map(item=>apiRequest<RuleEvaluation>(`/api/v1/rule-evaluations/${item.evaluationId}`)));
  return {analyses:comparison.analyses.map((summary,index)=>({summary,evaluation:evaluations[index]}))};
}

export async function getAnalysisDetail(analysisId: string): Promise<AnalysisDetail> {
  const result = await getAnalysisResult(analysisId);
  const [repository, evaluation, evidence, matrix] = await Promise.all([
    getRepository(result.repositoryId),
    apiRequest<RuleEvaluation>(`/api/v1/rule-evaluations/${result.evaluationId}`),
    apiRequest<{ evaluationId: string; evidence: RuleEvidence[] }>(
      `/api/v1/rule-evaluations/${result.evaluationId}/evidence`
    ),
    getSkillMatrix(result.skillMatrixId)
  ]);
  return { result, repository, evaluation, evidence: evidence.evidence, matrix };
}
