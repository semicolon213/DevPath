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
    analysisId?: string | null;
    careerReadinessId?: string | null;
    learningRoadmapId?: string | null;
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

export type GeneratedRepositoryReview = {
  artifactId: string;
  type: "REPOSITORY_REVIEW";
  status: "VALIDATED";
  provenance: Omit<GeneratedSkillExplanation["provenance"], "analysisId"> & { analysisId: string };
  validation: {
    status: "PASSED";
    validatorVersion: "repository-review-validator-v1";
    validatedAt: string;
    violations: string[];
  };
  contentRef: string;
  content: {
    summary: string;
    sections: Array<{
      category: "ARCHITECTURE" | "TESTING" | "DEVOPS" | "DOCUMENTATION" | "COLLABORATION";
      review: string;
      evidenceIds: string[];
    }>;
  };
};

export type GeneratedRepositorySummary = {
  artifactId: string;
  type: "REPOSITORY_SUMMARY";
  status: "VALIDATED";
  provenance: Omit<GeneratedSkillExplanation["provenance"], "analysisId"> & { analysisId: string };
  validation: {
    status: "PASSED";
    validatorVersion: "repository-summary-validator-v1";
    validatedAt: string;
    violations: string[];
  };
  contentRef: string;
  content: {
    summary: RepositorySummaryClaim;
    strengths: RepositorySummaryClaim[];
    improvementAreas: RepositorySummaryClaim[];
    technologies: string[];
  };
};

export type RepositorySummaryClaim = {
  text: string;
  evidenceReferences: string[];
  ruleOutputReferences: string[];
};

export type GeneratedCareerAdvice = {
  artifactId: string;
  type: "CAREER_ADVICE";
  status: "VALIDATED";
  provenance: GeneratedSkillExplanation["provenance"] & { careerReadinessId: string };
  validation: {
    status: "PASSED";
    validatorVersion: "career-advice-validator-v1";
    validatedAt: string;
    violations: string[];
  };
  contentRef: string;
  content: {
    readinessSummary: string;
    strengthExplanation: string;
    gapExplanation: string;
    nextSteps: Array<{ recommendationId: string; advice: string }>;
    confidenceNotice: string;
  };
};

export type GeneratedLearningRecommendation = {
  artifactId: string;
  type: "LEARNING_RECOMMENDATION";
  status: "VALIDATED";
  provenance: GeneratedSkillExplanation["provenance"] & {
    careerReadinessId: string;
    learningRoadmapId: string;
  };
  validation: {
    status: "PASSED";
    validatorVersion: "learning-recommendation-validator-v1";
    validatedAt: string;
    violations: string[];
  };
  contentRef: string;
  content: {
    recommendationSummary: string;
    steps: Array<{
      roadmapStepId: string;
      position: number;
      milestoneExplanation: string;
      objective: string;
      practiceTask: string;
      completionCriteria: string;
      expectedEvidence: string[];
    }>;
  };
};

export async function requestSkillExplanation(skillMatrixId: string) {
  return requestGeneration("SKILL_ANALYSIS_EXPLANATION", skillMatrixId, "SKILL_EXPLANATION");
}

export async function requestRepositoryReview(analysisId: string) {
  return requestGeneration("REPOSITORY_REVIEW", analysisId, "REPOSITORY_REVIEW");
}

export async function requestRepositorySummary(analysisId: string) {
  return requestGeneration("REPOSITORY_SUMMARY", analysisId, "REPOSITORY_SUMMARY");
}

export async function requestCareerCoaching(careerReadinessId: string) {
  return requestGeneration("CAREER_COACHING", careerReadinessId, "CAREER_ADVICE");
}

export async function requestLearningRecommendation(learningRoadmapId: string) {
  return requestGeneration("LEARNING_RECOMMENDATION", learningRoadmapId, "LEARNING_RECOMMENDATION");
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

export async function getGeneratedRepositoryReview(artifactUrl: string) {
  return apiRequest<GeneratedRepositoryReview>(artifactUrl);
}

export async function getGeneratedRepositorySummary(artifactUrl: string) {
  return apiRequest<GeneratedRepositorySummary>(artifactUrl);
}

export async function getGeneratedCareerAdvice(artifactUrl: string) {
  return apiRequest<GeneratedCareerAdvice>(artifactUrl);
}

export async function getGeneratedLearningRecommendation(artifactUrl: string) {
  return apiRequest<GeneratedLearningRecommendation>(artifactUrl);
}

async function requestGeneration(taskType: string, sourceResourceRef: string, outputType: string) {
  return apiRequest<GenerationJob>("/api/v1/generation-requests", await withCsrf({
    method: "POST",
    headers: { "Content-Type": "application/json", "Idempotency-Key": crypto.randomUUID() },
    body: JSON.stringify({ taskType, sourceResourceRefs: [sourceResourceRef], outputType })
  }));
}
