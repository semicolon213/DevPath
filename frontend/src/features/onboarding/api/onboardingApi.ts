import { apiRequest } from "../../../shared/api/apiClient";

export type OnboardingStepName = "ACCOUNT" | "PROFILE" | "CAREER_TARGET" | "COMPANY_TARGET"
  | "GITHUB_CONNECTION" | "REPOSITORY_IMPORT" | "INITIAL_SYNC" | "INITIAL_ANALYSIS";
export type OnboardingStep = { step: OnboardingStepName; requirement: "REQUIRED" | "RECOMMENDED" | "OPTIONAL"; status: "COMPLETE" | "INCOMPLETE"; resourceId: string | null; actionPath: string };
export type OnboardingProgress = { status: "GETTING_STARTED" | "IN_PROGRESS" | "DASHBOARD_READY"; completedStepCount: number; totalStepCount: number; nextStep: OnboardingStepName | "DASHBOARD_READY"; steps: OnboardingStep[]; generatedAt: string };

export async function getOnboardingProgress() {
  return (await apiRequest<OnboardingProgress>("/api/v1/users/me/onboarding-progress")).data;
}
