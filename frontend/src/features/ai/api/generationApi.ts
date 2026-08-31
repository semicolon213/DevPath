import { apiRequest, withCsrf } from "../../../shared/api/apiClient";

export type GenerationJob = {
  jobId: string;
  status: "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELED";
  validationStatus: "PENDING" | "PASSED" | "REJECTED";
  artifactUrl: string | null;
  failureCode: string | null;
  createdAt: string;
  completedAt: string | null;
};

export type SkillExplanationItem = {
  skillKey: string;
  explanation: string;
  evidenceIds: string[];
};

export type GeneratedSkillExplanation = {
  artifactId: string;
  type: "SKILL_EXPLANATION";
  status: "VALIDATED";
  provenance: {
    skillMatrixId: string;
    promptContextId: string;
    templateVersion: string;
    provider: "OLLAMA";
    model: string;
    contextHash: string;
    generatedAt: string;
  };
  validation: {
    status: "PASSED";
    validatorVersion: "skill-explanation-validator-v1";
    validatedAt: string;
    violations: string[];
  };
  contentRef: string;
  content: {
    summary: string;
    strengths: SkillExplanationItem[];
    improvementAreas: SkillExplanationItem[];
  };
};

export async function requestSkillExplanation(skillMatrixId: string) {
  return apiRequest<GenerationJob>("/api/v1/generation-requests", await withCsrf({
    method: "POST",
    headers: { "Content-Type": "application/json", "Idempotency-Key": crypto.randomUUID() },
    body: JSON.stringify({
      taskType: "SKILL_ANALYSIS_EXPLANATION",
      sourceResourceRefs: [skillMatrixId],
      outputType: "SKILL_EXPLANATION"
    })
  }));
}

export async function getGenerationJob(jobId: string) {
  return apiRequest<GenerationJob>(`/api/v1/generation-jobs/${jobId}`);
}

export async function cancelGenerationJob(jobId: string) {
  return apiRequest<GenerationJob>(`/api/v1/generation-jobs/${jobId}/cancel`, await withCsrf({
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: "{}"
  }));
}

export async function getGeneratedSkillExplanation(artifactUrl: string) {
  return apiRequest<GeneratedSkillExplanation>(artifactUrl);
}
