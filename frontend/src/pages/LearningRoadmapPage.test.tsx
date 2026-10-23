import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";

const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" };
const roadmap = {
  roadmapId: "rm1", recommendationSetId: "rs1", policyVersion: "roadmap-v1", status: "CREATED", progressPercent: 0,
  milestones: [{ milestoneId: "ms1", position: 0, category: "TESTING", title: "Add testing", status: "PLANNED" }],
  steps: [{ roadmapStepId: "st1", milestoneId: "ms1", recommendationId: "rec1", position: 0, category: "TESTING", title: "Add testing", difficulty: "INTERMEDIATE", effortHours: 16, prerequisiteStepIds: [], completionCriteria: "Reach an official Testing score of at least 60.", expectedEvidence: ["test files"], status: "NOT_STARTED" }],
  generatedAt: metadata.timestamp, updatedAt: metadata.timestamp
};

describe("LearningRoadmapPage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("renders deterministic recommendations and roadmap history", async () => {
    vi.stubGlobal("fetch", journeyFetch());
    renderWithProviders(undefined, ["/roadmap"]);
    expect(await screen.findByText(/선택적 AI 학습 가이드는 아래 별도 영역/)).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "학습 로드맵" })).toBeInTheDocument();
    expect(screen.getAllByText("Add testing")).toHaveLength(2);
    expect(screen.getByText("높음")).toBeInTheDocument();
    const roadmapRegion = screen.getByRole("heading", { name: "실행 순서" }).closest("section")!;
    const aiRegion = screen.getByRole("region", { name: "AI 생성 · 설명만 제공 선택적 AI 학습 가이드" });
    expect(roadmapRegion.compareDocumentPosition(aiRegion) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(aiRegion).toHaveTextContent("공식 점수나 로드맵 순서를 변경하지 않고");
    const historyRegion = screen.getByRole("heading", { name: "로드맵 이력" }).closest("section")!;
    expect(historyRegion.compareDocumentPosition(aiRegion) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(screen.getByRole("button", { name: "현재 로드맵 보관" })).toBeInTheDocument();
  });

  it("shows every server-provided roadmap step status with text and an icon", async () => {
    const statuses = ["COMPLETED", "IN_PROGRESS", "NOT_STARTED", "SKIPPED"] as const;
    const fetchMock = journeyFetch();
    fetchMock.mockImplementation((input: string | URL | Request) => {
      const url = String(input);
      if (url.endsWith("/recommendations/current")) return Promise.resolve(Response.json({ data: { recommendationSetId: "rs1", careerReadinessId: "cr1", policyVersion: "recommendation-v1", status: "PUBLISHED", generatedAt: metadata.timestamp, recommendations: [{ recommendationId: "rec1", skillGapId: "g1", category: "TESTING", type: "PROJECT", priority: "HIGH", rationaleCode: "CAREER_REQUIRED_GAP", title: "Add testing", completionCriteria: "Done", expectedEvidence: [], evidenceIds: [], effortHours: 16, position: 0, status: "PROPOSED" }] }, metadata }));
      if (url.endsWith("/learning-roadmaps")) return Promise.resolve(Response.json({ data: { roadmaps: [{ ...roadmap, steps: statuses.map((status, position) => ({ ...roadmap.steps[0], roadmapStepId: `step-${position}`, position, title: `Step ${position + 1}`, status })) }] }, metadata }));
      return Promise.reject(new Error(url));
    });
    vi.stubGlobal("fetch", fetchMock);
    renderWithProviders(undefined, ["/roadmap"]);
    for (const label of ["완료", "진행 중", "예정", "건너뜀"]) expect(await screen.findByText(label)).toBeInTheDocument();
    expect(document.querySelectorAll(".roadmap-step__status [aria-hidden='true']")).toHaveLength(4);
  });

  it("archives only after the shared confirmation is accepted", async () => {
    const fetchMock = journeyFetch(true);
    vi.stubGlobal("fetch", fetchMock);
    renderWithProviders(undefined, ["/roadmap"]);
    fireEvent.click(await screen.findByRole("button", { name: "현재 로드맵 보관" }));
    expect(screen.getByRole("dialog")).toHaveTextContent("로드맵 내용과 이력은 유지됩니다");
    expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith("/archive"))).toBe(false);
    fireEvent.click(screen.getByRole("button", { name: "취소" }));
    expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith("/archive"))).toBe(false);

    const archiveTrigger = screen.getByRole("button", { name: "현재 로드맵 보관" });
    archiveTrigger.focus();
    fireEvent.click(archiveTrigger);
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "로드맵 보관" }));
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith("/archive"))).toBe(true));
    expect(await screen.findByRole("heading", { name: "진행 중인 로드맵이 없습니다" })).toBeInTheDocument();
    const message = await screen.findByText("로드맵을 보관했습니다.");
    expect(message).toHaveAttribute("role", "status");
    await waitFor(() => expect(message).toHaveFocus());
    expect(document.activeElement).not.toBe(document.body);
  });

  it("uses the shared network failure state", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.reject(new TypeError("offline"))));
    renderWithProviders(undefined, ["/roadmap"]);
    expect(await screen.findByRole("heading", { name: "서버에 연결할 수 없습니다." })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
  });
});

function journeyFetch(withArchive = false) {
  let archived = false;
  return vi.fn((input: string | URL | Request, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/recommendations/current")) return Promise.resolve(Response.json({ data: { recommendationSetId: "rs1", careerReadinessId: "cr1", policyVersion: "recommendation-v1", status: "PUBLISHED", generatedAt: metadata.timestamp, recommendations: [{ recommendationId: "rec1", skillGapId: "g1", category: "TESTING", type: "PROJECT", priority: "HIGH", rationaleCode: "CAREER_REQUIRED_GAP", title: "Add testing", completionCriteria: "Reach an official Testing score of at least 60.", expectedEvidence: ["test files"], evidenceIds: ["e1"], effortHours: 16, position: 0, status: "PROPOSED" }] }, metadata }));
    if (url.endsWith("/learning-roadmaps")) return Promise.resolve(Response.json({ data: { roadmaps: [{ ...roadmap, status: archived ? "ARCHIVED" : "CREATED" }] }, metadata }));
    if (withArchive && url.endsWith("/csrf")) return Promise.resolve(Response.json({ data: { headerName: "X-CSRF-TOKEN", parameterName: "_csrf", token: "csrf" }, metadata }));
    if (withArchive && url.endsWith("/archive") && init?.method === "POST") {
      archived = true;
      return Promise.resolve(Response.json({ data: { ...roadmap, status: "ARCHIVED" }, metadata }));
    }
    return Promise.reject(new Error(url));
  });
}
