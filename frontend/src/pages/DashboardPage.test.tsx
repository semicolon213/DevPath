import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRoutes } from "../routes/AppRoutes";
import { renderWithProviders } from "../test/renderWithProviders";

const metadata = { requestId: "request-1", apiVersion: "v1", timestamp: "2026-08-12T00:00:00Z" };
const envelope = (data: unknown) => Response.json({ data, metadata });

describe("DashboardPage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("composes authoritative targets, latest analysis, repositories, and Skill Matrix", async () => {
    vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
      const url = String(input);
      if (url.endsWith("/preferences")) return Promise.resolve(envelope({ careerId: "backend-engineer", companyId: "naver", updatedAt: "2026-08-12T00:00:00Z" }));
      if (url.endsWith("/careers/backend-engineer")) return Promise.resolve(envelope({ localizedName: "백엔드 엔지니어", profileVersion: "career-v1" }));
      if (url.endsWith("/companies/naver")) return Promise.resolve(envelope({ localizedName: "네이버", profileVersion: "company-v1" }));
      if (url.includes("/repositories?")) return Promise.resolve(envelope({ repositories: [repository("r1", "SYNCHRONIZED"), repository("r2", "NOT_SYNCED")], totalCount: 2, limit: 20, nextCursor: null }));
      if (url.includes("/analyses?")) return Promise.resolve(envelope({ analyses: [{ analysisId: "a1", repositoryId: "r1", snapshotId: "s1", evaluationId: "e1", skillMatrixId: "m1", analysisScope: "REPOSITORY_BASELINE", currentForRepository: true, completedAt: "2026-08-12T01:00:00Z", repositoryFullName: "devpath/backend", overallScore: 82.5, confidence: 90, ruleSetVersion: "rules-v1", policyVersion: "skills-v1" }], totalCount: 1, limit: 20, nextCursor: null }));
      if (url.endsWith("/skill-matrices/current")) return Promise.resolve(envelope({ skillMatrixId: "m1", evaluationId: "e1", policyVersion: "skills-v1", ruleSetVersion: "rules-v1", status: "CURRENT", skills: [{}, {}, {}], strengths: ["testing"], weaknesses: ["docs"], generatedAt: "2026-08-12T01:00:00Z" }));
      if (url.endsWith("/career-readiness/current")) return Promise.resolve(envelope({ careerReadinessId: "cr1", skillMatrixId: "m1", careerId: "backend", careerProfileVersionId: "cp1", careerProfileVersion: "career-v2", readinessPolicyVersion: "readiness-v1", ruleSetVersion: "baseline-v2", status: "COMPLETED", readinessScore: 76.5, readinessLevel: "COMPETENT", confidence: 88, unavailableCategories: [], skillGaps: [], assessedAt: "2026-08-12T01:00:00Z" }));
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    }));

    renderWithProviders(<AppRoutes />, ["/dashboard"]);

    expect(await screen.findByRole("heading", { name: "내 DevPath 대시보드" })).toBeInTheDocument();
    expect(screen.getByText("백엔드 엔지니어")).toBeInTheDocument();
    expect(screen.getByText("네이버")).toBeInTheDocument();
    expect(screen.getByText("82.5")).toBeInTheDocument();
    expect(screen.getByText("76.5")).toBeInTheDocument();
    expect(screen.getAllByText("devpath/backend")).toHaveLength(2);
  });

  it("shows partial state without hiding available dashboard data", async () => {
    vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
      const url = String(input);
      if (url.endsWith("/preferences")) return Promise.resolve(envelope({ careerId: null, companyId: null, updatedAt: null }));
      if (url.includes("/repositories?")) return Promise.reject(new TypeError("network"));
      if (url.includes("/analyses?")) return Promise.resolve(envelope({ analyses: [], totalCount: 0, limit: 20, nextCursor: null }));
      if (url.endsWith("/skill-matrices/current")) return Promise.resolve(new Response(null, { status: 404 }));
      if (url.endsWith("/career-readiness/current")) return Promise.resolve(new Response(null, { status: 404 }));
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    }));

    renderWithProviders(<AppRoutes />, ["/dashboard"]);

    expect(await screen.findByRole("heading", { name: "내 DevPath 대시보드" })).toBeInTheDocument();
    expect(screen.getByText("등록 저장소 · 확인 실패")).toBeInTheDocument();
    expect(screen.getByText("기술 역량 결과가 없습니다.")).toBeInTheDocument();
    expect(screen.getByText("완료된 분석이 없습니다.")).toBeInTheDocument();
  });
});

function repository(repositoryId: string, syncStatus: "SYNCHRONIZED" | "NOT_SYNCED") {
  return { repositoryId, providerRepositoryId: repositoryId, name: repositoryId, fullName: `devpath/${repositoryId}`, owner: "devpath", visibility: "PRIVATE", defaultBranch: "main", providerArchived: false, lifecycle: "ACTIVE", syncStatus, htmlUrl: "https://github.com/devpath/repo", discoveredAt: "2026-08-11T00:00:00Z", lastSyncedAt: null, currentSnapshotId: null };
}
