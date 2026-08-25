import { apiRequest } from "../../../shared/api/apiClient";

export type RuleCategory = "LANGUAGE" | "FRAMEWORK" | "DATABASE" | "ARCHITECTURE" | "TESTING" | "DEVOPS" | "DOCUMENTATION" | "ACTIVITY";
export type ReadinessLevel = "NONE" | "BEGINNER" | "DEVELOPING" | "COMPETENT" | "STRONG";

export type SkillGap = {
  skillGapId: string;
  skillId: string;
  skillKey: string;
  category: RuleCategory;
  actualScore: number;
  actualLevel: ReadinessLevel;
  expectedMinimum: number;
  gapState: "MISSING" | "WEAK" | "PARTIAL" | "SUFFICIENT" | "STRONG";
  careerWeight: number;
  evidenceIds: string[];
};

export type CareerReadiness = {
  careerReadinessId: string;
  skillMatrixId: string;
  careerId: string;
  careerProfileVersionId: string;
  careerProfileVersion: string;
  readinessPolicyVersion: string;
  ruleSetVersion: string;
  status: "COMPLETED" | "INSUFFICIENT_EVIDENCE";
  readinessScore: number | null;
  readinessLevel: ReadinessLevel | null;
  confidence: number;
  unavailableCategories: RuleCategory[];
  skillGaps: SkillGap[];
  assessedAt: string;
};

export async function getCurrentCareerReadiness() {
  return (await apiRequest<CareerReadiness>("/api/v1/career-readiness/current")).data;
}

export async function getCareerReadiness(careerReadinessId: string) {
  return (await apiRequest<CareerReadiness>(`/api/v1/career-readiness/${careerReadinessId}`)).data;
}
