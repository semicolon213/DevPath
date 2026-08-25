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

export async function getCurrentSkillMatrix() {
  return (await apiRequest<SkillMatrix>("/api/v1/skill-matrices/current")).data;
}

export async function getSkillMatrix(skillMatrixId: string) {
  return (await apiRequest<SkillMatrix>(`/api/v1/skill-matrices/${skillMatrixId}`)).data;
}

export async function getSkillMatrixComparison(skillMatrixIds: string[]) {
  const query = new URLSearchParams();
  skillMatrixIds.forEach(id => query.append("skillMatrixId", id));
  return (await apiRequest<SkillMatrixComparison>(`/api/v1/skill-matrices/compare?${query}`)).data;
}

export async function getSkillDetail(skillId: string) {
  return (await apiRequest<SkillDetail>(`/api/v1/skills/${skillId}`)).data;
}

export async function getSkillEvidence(skillId: string) {
  return (await apiRequest<SkillEvidenceList>(`/api/v1/skills/${skillId}/evidence`)).data;
}

export async function getSkillWorkspace(skillId: string): Promise<SkillWorkspace> {
  const [detail, evidence] = await Promise.all([getSkillDetail(skillId), getSkillEvidence(skillId)]);
  return { detail, evidence };
}
