import { getAnalysisHistory, type AnalysisHistoryItem } from "../../analysis/api/analysisApi";
import { getCareer, type CareerProfile } from "../../careers/api/careerApi";
import { getCompany, type CompanyProfile } from "../../companies/api/companyApi";
import { getPreferences, type UserPreferences } from "../../profile/api/profileApi";
import { getRepositories, type ImportedRepository } from "../../repositories/api/repositoryApi";
import { getCurrentSkillMatrix, type SkillMatrix } from "../../skills/api/skillMatrixApi";
import { getCurrentCareerReadiness, type CareerReadiness } from "../../readiness/api/careerReadinessApi";
import { ApiError } from "../../../shared/api/apiClient";

export type DashboardSource<T> =
  | { status: "available"; data: T }
  | { status: "empty"; data: null }
  | { status: "unavailable"; data: null };

export type DashboardView = {
  preferences: UserPreferences;
  career: DashboardSource<CareerProfile>;
  company: DashboardSource<CompanyProfile>;
  repositories: DashboardSource<{ items: ImportedRepository[]; totalCount: number }>;
  analyses: DashboardSource<{ items: AnalysisHistoryItem[]; totalCount: number }>;
  skillMatrix: DashboardSource<SkillMatrix>;
  careerReadiness: DashboardSource<CareerReadiness>;
};

export async function getDashboardView(): Promise<DashboardView> {
  const preferences = await getPreferences();
  const [career, company, repositories, analyses, skillMatrix, careerReadiness] = await Promise.all([
    optional(preferences.careerId ? getCareer(preferences.careerId) : Promise.resolve(null)),
    optional(preferences.companyId ? getCompany(preferences.companyId) : Promise.resolve(null)),
    optional(getRepositories()),
    optional(getAnalysisHistory()),
    optional(getCurrentSkillMatrix()),
    optional(getCurrentCareerReadiness())
  ]);

  return {
    preferences,
    career,
    company,
    repositories: mapSource(repositories, page => ({ items: page.repositories, totalCount: page.totalCount })),
    analyses: mapSource(analyses, page => ({ items: page.analyses, totalCount: page.totalCount })),
    skillMatrix,
    careerReadiness
  };
}

async function optional<T>(promise: Promise<T | null>): Promise<DashboardSource<T>> {
  try {
    const data = await promise;
    return data === null ? { status: "empty", data: null } : { status: "available", data };
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) throw error;
    if (error instanceof ApiError && error.status === 404) return { status: "empty", data: null };
    return { status: "unavailable", data: null };
  }
}

function mapSource<T, R>(source: DashboardSource<T>, mapper: (data: T) => R): DashboardSource<R> {
  return source.status === "available" ? { status: "available", data: mapper(source.data) } : source;
}
