import { screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";

const metadata = { requestId: "request-1", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" };
const envelope = (data: unknown) => Response.json({ data, metadata });

describe("DashboardPage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("renders the canonical owner-scoped dashboard summary and uses the top recommendation as the primary action", async () => {
    const data = summary();
    data.targets.company = { id: "toss", localizedName: "토스", profileVersion: "company-v3" };
    const fetchMock = vi.fn((_input: string | URL | Request) => Promise.resolve(envelope(data)));
    vi.stubGlobal("fetch", fetchMock);

    renderWithProviders(undefined, ["/dashboard"]);

    expect(await screen.findByText(/백엔드 개발자/)).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "내 DevPath 대시보드", level: 1 })).toBeInTheDocument();
    expect(screen.getAllByText("devpath/backend")).toHaveLength(2);
    expect(screen.getByText("테스트 보강")).toBeInTheDocument();
    expect(screen.getByText("지금 할 일")).toBeInTheDocument();
    expect(screen.getByText("저장소 동기화")).toBeInTheDocument();
    expect(screen.getByText("충분 · 신뢰도 88%")).toBeInTheDocument();
    expect(screen.getByText("테스트 · 16시간")).toBeInTheDocument();
    expect(screen.getByText("높음")).toBeInTheDocument();
    expect(screen.getByText("완료")).toBeInTheDocument();
    expect(screen.getAllByText(/기준 시각/)).toHaveLength(2);
    expect(screen.getByRole("link", { name: "지금 할 일 보기: 테스트 보강" })).toHaveAttribute("href", "/recommendations/rec1");
    expect(screen.getByRole("link", { name: "테스트 보강 추천 상세" })).toHaveAttribute("href", "/recommendations/rec1");
    expect(screen.getByRole("link", { name: "목표 변경" })).toHaveAttribute("href", "/careers");
    const hero = screen.getByRole("heading", { name: "내 DevPath 대시보드" }).closest(".dashboard-hero");
    expect(hero).not.toBeNull();
    expect(within(hero as HTMLElement).getByRole("link", { name: "저장소" })).toHaveAttribute("href", "/repositories");
    expect(screen.getByText(/커리어 프로필 career-v2/)).toBeInTheDocument();
    expect(screen.getByText(/회사 프로필 company-v3/)).toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: "학습 로드맵 진행률" })).toHaveAttribute("value", "25");
    ["COMPETENT", "TESTING", "HIGH", "SUCCEEDED"].forEach(value => expect(screen.queryByText(value)).not.toBeInTheDocument());
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0]?.[0]).toBe("http://localhost:8080/api/v1/dashboard/summary");
  });

  it("keeps every empty state and uses the existing fallback primary action", async () => {
    const data = summary();
    data.targets = { status: "EMPTY", career: null, company: null };
    data.repositories = { ...data.repositories, status: "EMPTY", totalCount: 0, synchronizedCount: 0 };
    data.analyses = { ...data.analyses, status: "EMPTY", totalCount: 0, latest: null, currentByRepository: [] };
    data.skillOverview = { ...data.skillOverview, status: "EMPTY" };
    data.readiness = { ...data.readiness, status: "EMPTY" };
    data.recommendations = { ...data.recommendations, status: "EMPTY", items: [] };
    data.roadmap = { ...data.roadmap, status: "EMPTY" };
    data.recentJobs = { status: "EMPTY", items: [] };
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(envelope(data))));

    renderWithProviders(undefined, ["/dashboard"]);

    expect(await screen.findByText(/목표 직무를 선택하지 않았습니다/)).toBeInTheDocument();
    expect(screen.getByText(/선택한 회사가 없습니다/)).toBeInTheDocument();
    expect(screen.getByText("아직 준비도 결과가 없습니다.")).toBeInTheDocument();
    expect(screen.getByText("활성 학습 로드맵이 없습니다.")).toBeInTheDocument();
    expect(screen.getByText("아직 생성된 추천이 없습니다.")).toBeInTheDocument();
    expect(screen.getByText("기술 역량 결과가 없습니다.")).toBeInTheDocument();
    expect(screen.getByText("완료된 분석이 없습니다.")).toBeInTheDocument();
    expect(screen.getByText("최근 실행된 작업이 없습니다.")).toBeInTheDocument();
    expect(screen.getByText("저장소별 최신 분석 결과가 아직 없습니다.")).toBeInTheDocument();
    const fallbackActions = screen.getAllByRole("link", { name: "준비도 확인" });
    expect(fallbackActions).toHaveLength(2);
    expect(fallbackActions[0]).toHaveAttribute("href", "/career-readiness");
    expect(fallbackActions[0]).toHaveClass("button-link");
    expect(fallbackActions[1]).not.toHaveClass("button-link");
  });

  it("keeps available structure visible when every source is unavailable", async () => {
    const data = summary();
    data.targets.status = "UNAVAILABLE";
    data.repositories.status = "UNAVAILABLE";
    data.analyses.status = "UNAVAILABLE";
    data.skillOverview.status = "UNAVAILABLE";
    data.readiness.status = "UNAVAILABLE";
    data.recommendations.status = "UNAVAILABLE";
    data.roadmap.status = "UNAVAILABLE";
    data.recentJobs.status = "UNAVAILABLE";
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(envelope(data))));

    renderWithProviders(undefined, ["/dashboard"]);

    expect(await screen.findByText("등록 저장소 · 확인 실패")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "내 DevPath 대시보드" })).toBeInTheDocument();
    expect(screen.getByText("목표 · 확인 실패")).toBeInTheDocument();
    expect(screen.getByText(/커리어 프로필 확인 실패 · 회사 프로필 확인 실패/)).toBeInTheDocument();
    expect(screen.getByText("커리어 준비도 정보를 불러오지 못했습니다.")).toBeInTheDocument();
    expect(screen.getAllByText(/일부 정보를 불러오지 못했습니다/)).toHaveLength(6);
    expect(screen.queryByRole("link", { name: "지금 할 일 보기: 테스트 보강" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "준비도 확인" })).toHaveAttribute("href", "/career-readiness");
  });

  it("distinguishes insufficient readiness evidence from a low score", async () => {
    const data = summary();
    data.readiness = { ...data.readiness, resultStatus: "INSUFFICIENT_EVIDENCE", score: null, level: null, confidence: null, unavailableCategories: ["TESTING"] };
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(envelope(data))));

    renderWithProviders(undefined, ["/dashboard"]);

    expect(await screen.findByText("근거 부족")).toBeInTheDocument();
    expect(screen.getByText("확인 불가 범주: 테스트")).toBeInTheDocument();
    expect(screen.queryByText("0", { exact: true })).not.toBeInTheDocument();
  });

  it("preserves loading and request error states", async () => {
    vi.stubGlobal("fetch", vi.fn(() => new Promise<Response>(() => undefined)));
    const loading = renderWithProviders(undefined, ["/dashboard"]);
    expect(screen.getByRole("status")).toHaveTextContent("개발 현황을 모으는 중입니다.");
    loading.unmount();

    vi.stubGlobal("fetch", vi.fn(() => Promise.reject(new TypeError("network"))));
    renderWithProviders(undefined, ["/dashboard"]);
    expect(await screen.findByRole("heading", { name: "서버에 연결할 수 없습니다." })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
  });
});

function summary() {
  return {
    generatedAt: "2026-08-25T00:00:00Z",
    targets: { status: "AVAILABLE" as "AVAILABLE" | "EMPTY" | "UNAVAILABLE", career: { id: "backend", localizedName: "백엔드 개발자", profileVersion: "career-v2" } as { id: string; localizedName: string; profileVersion: string } | null, company: null as { id: string; localizedName: string; profileVersion: string } | null },
    repositories: { status: "AVAILABLE" as "AVAILABLE" | "EMPTY" | "UNAVAILABLE", totalCount: 2, synchronizedCount: 1, recent: [] },
    analyses: { status: "AVAILABLE" as "AVAILABLE" | "EMPTY" | "UNAVAILABLE", totalCount: 1, latest: analysis() as ReturnType<typeof analysis> | null, currentByRepository: [analysis()] },
    skillOverview: { status: "AVAILABLE" as "AVAILABLE" | "EMPTY" | "UNAVAILABLE", skillMatrixId: "m1", skillCount: 5, strengthCount: 2, weaknessCount: 1, policyVersion: "skills-v1", ruleSetVersion: "rules-v1", generatedAt: "2026-08-25T00:00:00Z" },
    readiness: { status: "AVAILABLE" as "AVAILABLE" | "EMPTY" | "UNAVAILABLE", careerReadinessId: "cr1", resultStatus: "COMPLETED" as string | null, score: 76.5 as number | null, level: "COMPETENT" as string | null, confidence: 88 as number | null, unavailableCategories: [] as string[], assessedAt: "2026-08-25T00:00:00Z" },
    recommendations: { status: "AVAILABLE" as "AVAILABLE" | "EMPTY" | "UNAVAILABLE", recommendationSetId: "rs1", policyVersion: "recommendation-v1", items: [{ recommendationId: "rec1", category: "TESTING", type: "PROJECT", priority: "HIGH", title: "테스트 보강", effortHours: 16, position: 0, status: "PROPOSED" }], generatedAt: "2026-08-25T00:00:00Z" },
    roadmap: { status: "AVAILABLE" as "AVAILABLE" | "EMPTY" | "UNAVAILABLE", roadmapId: "rm1", policyVersion: "roadmap-v1", resultStatus: "IN_PROGRESS", progressPercent: 25, milestoneCount: 2, stepCount: 4, updatedAt: "2026-08-25T00:00:00Z" },
    recentJobs: { status: "AVAILABLE" as "AVAILABLE" | "EMPTY" | "UNAVAILABLE", items: [{ jobId: "j1", jobType: "REPOSITORY_SYNC" as const, repositoryId: "r1", status: "SUCCEEDED", phase: "COMPLETED", progressPercent: 100, submittedAt: "2026-08-25T00:00:00Z", completedAt: "2026-08-25T00:01:00Z" }] }
  };
}

function analysis() {
  return { analysisId: "a1", repositoryId: "r1", repositoryFullName: "devpath/backend", overallScore: 82.5, confidence: 90, currentForRepository: true, completedAt: "2026-08-25T00:00:00Z" };
}
