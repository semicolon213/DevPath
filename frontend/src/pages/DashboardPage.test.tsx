import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRoutes } from "../routes/AppRoutes";
import { renderWithProviders } from "../test/renderWithProviders";

const metadata = { requestId: "request-1", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" };
const envelope = (data: unknown) => Response.json({ data, metadata });

describe("DashboardPage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("renders the canonical owner-scoped dashboard summary", async () => {
    const fetchMock = vi.fn((_input: string | URL | Request) => Promise.resolve(envelope(summary())));
    vi.stubGlobal("fetch", fetchMock);

    renderWithProviders(<AppRoutes />, ["/dashboard"]);

    expect(await screen.findByRole("heading", { name: "내 DevPath 대시보드" })).toBeInTheDocument();
    expect(screen.getByText("백엔드 개발자")).toBeInTheDocument();
    expect(screen.getAllByText("devpath/backend")).toHaveLength(2);
    expect(screen.getByText("테스트 보강")).toBeInTheDocument();
    expect(screen.getByText("저장소 동기화")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0]?.[0]).toBe("http://localhost:8080/api/v1/dashboard/summary");
  });

  it("keeps available sections visible when repositories and skills are unavailable", async () => {
    const data = summary();
    data.repositories.status = "UNAVAILABLE";
    data.skillOverview.status = "UNAVAILABLE";
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(envelope(data))));

    renderWithProviders(<AppRoutes />, ["/dashboard"]);

    expect(await screen.findByRole("heading", { name: "내 DevPath 대시보드" })).toBeInTheDocument();
    expect(screen.getByText("등록 저장소 · 확인 실패")).toBeInTheDocument();
    expect(screen.getByText("82.5")).toBeInTheDocument();
    expect(screen.getAllByText(/일부 정보를 불러오지 못했습니다/)).toHaveLength(1);
  });
});

function summary() {
  return {
    generatedAt: "2026-08-25T00:00:00Z",
    targets: { status: "AVAILABLE" as const, career: { id: "backend", localizedName: "백엔드 개발자", profileVersion: "career-v2" }, company: null },
    repositories: { status: "AVAILABLE" as "AVAILABLE" | "UNAVAILABLE", totalCount: 2, synchronizedCount: 1, recent: [] },
    analyses: { status: "AVAILABLE" as const, totalCount: 1, latest: analysis(), currentByRepository: [analysis()] },
    skillOverview: { status: "AVAILABLE" as "AVAILABLE" | "UNAVAILABLE", skillMatrixId: "m1", skillCount: 5, strengthCount: 2, weaknessCount: 1, policyVersion: "skills-v1", ruleSetVersion: "rules-v1", generatedAt: "2026-08-25T00:00:00Z" },
    readiness: { status: "AVAILABLE" as const, careerReadinessId: "cr1", resultStatus: "COMPLETED", score: 76.5, level: "COMPETENT", confidence: 88, unavailableCategories: [], assessedAt: "2026-08-25T00:00:00Z" },
    recommendations: { status: "AVAILABLE" as const, recommendationSetId: "rs1", policyVersion: "recommendation-v1", items: [{ recommendationId: "rec1", category: "TESTING", type: "PROJECT", priority: "HIGH", title: "테스트 보강", effortHours: 16, position: 0, status: "PROPOSED" }], generatedAt: "2026-08-25T00:00:00Z" },
    roadmap: { status: "AVAILABLE" as const, roadmapId: "rm1", policyVersion: "roadmap-v1", resultStatus: "IN_PROGRESS", progressPercent: 25, milestoneCount: 2, stepCount: 4, updatedAt: "2026-08-25T00:00:00Z" },
    recentJobs: { status: "AVAILABLE" as const, items: [{ jobId: "j1", jobType: "REPOSITORY_SYNC" as const, repositoryId: "r1", status: "SUCCEEDED", phase: "COMPLETED", progressPercent: 100, submittedAt: "2026-08-25T00:00:00Z", completedAt: "2026-08-25T00:01:00Z" }] }
  };
}

function analysis() {
  return { analysisId: "a1", repositoryId: "r1", repositoryFullName: "devpath/backend", overallScore: 82.5, confidence: 90, currentForRepository: true, completedAt: "2026-08-25T00:00:00Z" };
}
