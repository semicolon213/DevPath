import { apiRequest } from "../../../shared/api/apiClient";

export type SkillAssessment = {
  assessmentId: string;
  skillId: string;
  skillKey: string;
  skillName: string;
  category: "LANGUAGE" | "FRAMEWORK" | "DATABASE" | "ARCHITECTURE" | "TESTING" | "DEVOPS" | "DOCUMENTATION" | "ACTIVITY";
  score: number;
  level: "NONE" | "BEGINNER" | "DEVELOPING" | "COMPETENT" | "STRONG";
  confidence: number;
  strength: boolean;
  weakness: boolean;
  growthTrend: "UNAVAILABLE" | "IMPROVING" | "STABLE" | "DECLINING";
  aggregateRuleResultReference: string;
  evidenceIds: string[];
  repositoryIds: string[];
  recommendationInputFacts: string[];
  ruleSetVersion: string;
};

export type SkillMatrix = {
  skillMatrixId: string;
  evaluationId: string;
  policyVersion: string;
  ruleSetVersion: string;
  status: "CURRENT" | "SUPERSEDED" | "ARCHIVED";
  skills: SkillAssessment[];
  strengths: string[];
  weaknesses: string[];
  generatedAt: string;
};

export type SkillMatrixComparison = { matrices: SkillMatrix[] };
export type SkillDetail = {
  skillMatrixId: string; evaluationId: string; policyVersion: string; ruleSetVersion: string;
  matrixStatus: SkillMatrix["status"]; generatedAt: string; skill: SkillAssessment;
};
export type SkillEvidence = {
  evidenceId: string; snapshotId: string; evidenceType: string; sourceReference: string;
  observedFactSummary: string; confidence: number;
};
export type SkillEvidenceList = {
  skillId: string; skillAssessmentId: string; skillMatrixId: string; evidence: SkillEvidence[];
};
export type SkillWorkspace = { detail: SkillDetail; evidence: SkillEvidenceList };
export type SkillHistoryPoint = {
  analysisId: string;
  repositoryId: string;
  repositoryFullName: string;
  skillMatrixId: string;
  matrixStatus: SkillMatrix["status"];
  policyVersion: string;
  ruleSetVersion: string;
  generatedAt: string;
  skill: SkillAssessment;
};
export type SkillHistoryPage = {
  points: SkillHistoryPoint[];
  nextCursor: string | null;
  totalAnalysisCount: number;
};

type AnalysisHistoryPage = {
  analyses: Array<{
    analysisId: string;
    repositoryId: string;
    repositoryFullName: string;
    skillMatrixId: string;
  }>;
  nextCursor: string | null;
  totalCount: number;
};

export async function getCurrentSkillMatrix() {
  return apiRequest<SkillMatrix>("/api/v1/skill-matrices/current");
}

export async function getSkillMatrix(skillMatrixId: string) {
  return apiRequest<SkillMatrix>(`/api/v1/skill-matrices/${skillMatrixId}`);
}

export async function getSkillMatrixComparison(skillMatrixIds: string[]) {
  const query = new URLSearchParams();
  skillMatrixIds.forEach(id => query.append("skillMatrixId", id));
  return apiRequest<SkillMatrixComparison>(`/api/v1/skill-matrices/compare?${query}`);
}

export async function getSkillDetail(skillId: string) {
  return apiRequest<SkillDetail>(`/api/v1/skills/${skillId}`);
}

export async function getSkillEvidence(skillId: string) {
  return apiRequest<SkillEvidenceList>(`/api/v1/skills/${skillId}/evidence`);
}

export async function getSkillWorkspace(skillId: string): Promise<SkillWorkspace> {
  const [detail, evidence] = await Promise.all([getSkillDetail(skillId), getSkillEvidence(skillId)]);
  return { detail, evidence };
}

export async function getSkillHistoryPage(skillId: string, cursor: string | null = null): Promise<SkillHistoryPage> {
  const query = new URLSearchParams({ limit: "20" });
  if (cursor) query.set("cursor", cursor);
  const history = await apiRequest<AnalysisHistoryPage>(`/api/v1/analyses?${query}`);
  const uniqueAnalyses = [...new Map(history.analyses.map(item => [item.skillMatrixId, item])).values()];
  const points = (await Promise.all(uniqueAnalyses.map(async analysis => {
    const matrix = await getSkillMatrix(analysis.skillMatrixId);
    const skill = matrix.skills.find(item => item.skillId === skillId);
    if (!skill) return null;
    return {
      analysisId: analysis.analysisId,
      repositoryId: analysis.repositoryId,
      repositoryFullName: analysis.repositoryFullName,
      skillMatrixId: matrix.skillMatrixId,
      matrixStatus: matrix.status,
      policyVersion: matrix.policyVersion,
      ruleSetVersion: matrix.ruleSetVersion,
      generatedAt: matrix.generatedAt,
      skill
    } satisfies SkillHistoryPoint;
  }))).filter((point): point is SkillHistoryPoint => point !== null);
  points.sort((left, right) => Date.parse(right.generatedAt) - Date.parse(left.generatedAt));
  return { points, nextCursor: history.nextCursor, totalAnalysisCount: history.totalCount };
}
