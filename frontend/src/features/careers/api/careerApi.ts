import { apiRequest } from "../../../shared/api/apiClient";

export type CareerSummary = {
  careerId: string;
  name: string;
  localizedName: string;
  status: "SUPPORTED";
  profileVersion: string;
  purpose: string;
};

export type CareerCatalog = { catalogVersion: string; careers: CareerSummary[] };

export type CareerProfile = CareerSummary & {
  careerProfileVersionId: string;
  coreTechnologies: string[];
  requiredCompetencies: string[];
  preferredCompetencies: string[];
  evaluationCategories: string[];
  priorityWeights: Record<string, "HIGH" | "MEDIUM" | "LOW">;
  roadmapTemplate: string[];
  effectiveAt: string;
};

export async function getCareers() {
  return (await apiRequest<CareerCatalog>("/api/v1/careers")).data;
}

export async function getCareer(careerId: string) {
  return (await apiRequest<CareerProfile>(`/api/v1/careers/${careerId}`)).data;
}
