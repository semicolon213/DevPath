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

export async function getCurrentSkillMatrix() {
  return (await apiRequest<SkillMatrix>("/api/v1/skill-matrices/current")).data;
}

export async function getSkillMatrix(skillMatrixId: string) {
  return (await apiRequest<SkillMatrix>(`/api/v1/skill-matrices/${skillMatrixId}`)).data;
}
