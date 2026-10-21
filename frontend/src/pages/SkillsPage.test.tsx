import { fireEvent, screen, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";
import { SkillsPage } from "./SkillsPage";

afterEach(() => vi.unstubAllGlobals());

it("renders official scores, confidence, evidence, and Korean labels", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({
    data: {
      skillMatrixId: "matrix-1",
      evaluationId: "evaluation-1",
      policyVersion: "skill-matrix-v1",
      ruleSetVersion: "baseline-v1",
      status: "CURRENT",
      generatedAt: "2026-08-11T10:00:00Z",
      strengths: ["technical-documentation"],
      weaknesses: [],
      skills: [{
        assessmentId: "assessment-1",
        skillId: "skill-1",
        skillKey: "technical-documentation",
        skillName: "Technical Documentation",
        category: "DOCUMENTATION",
        score: 100,
        level: "STRONG",
        confidence: 87.5,
        evidenceStatus: "SUFFICIENT",
        strength: true,
        weakness: false,
        growthTrend: "UNAVAILABLE",
        aggregateRuleResultReference: "evaluation:evaluation-1:category:DOCUMENTATION",
        evidenceIds: ["evidence-1"],
        repositoryIds: ["repository-1"],
        recommendationInputFacts: [],
        ruleSetVersion: "baseline-v1"
      }]
    },
    metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T10:00:00Z" }
  })));

  renderWithProviders(<SkillsPage />, ["/skills"]);

  expect(await screen.findByRole("heading", { name: "기술 문서화" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "기술 역량 분석" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "기술 문서화" })).toHaveAttribute("href", "/skills/skill-1");
  expect(screen.getByText("100")).toBeInTheDocument();
  expect(screen.getByText("87.5%")).toBeInTheDocument();
  expect(screen.getAllByText("강점")).toHaveLength(2);
  expect(screen.getByText("점수와 등급은 AI가 아닌 Rule Engine이 계산합니다.", { exact: false })).toBeInTheDocument();
  const officialRegion = screen.getByRole("heading", { name: "역량별 결과" }).closest("section")!;
  const aiRegion = screen.getByRole("region", { name: "AI 생성 기술 분석 설명" });
  expect(officialRegion.compareDocumentPosition(aiRegion) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
});

it("uses the API evidence status without deriving it from evidence counts", async () => {
  const statuses = [
    { id: "none", name: "근거 없는 역량", status: "NONE", score: 15, evidenceIds: [] },
    { id: "insufficient", name: "근거 부족 역량", status: "INSUFFICIENT", score: 45, evidenceIds: ["evidence-1"] },
    { id: "sufficient", name: "근거 충분 역량", status: "SUFFICIENT", score: 80, evidenceIds: [] },
    { id: "legacy", name: "과거 역량", status: null, score: 60, evidenceIds: [] }
  ];
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({ data: {
    skillMatrixId: "matrix-1", evaluationId: "evaluation-1", policyVersion: "skill-matrix-v3",
    ruleSetVersion: "baseline-v1", status: "CURRENT", generatedAt: "2026-08-11T10:00:00Z",
    strengths: [], weaknesses: [], skills: statuses.map(item => ({
      assessmentId: item.id, skillId: item.id, skillKey: item.id, skillName: item.name,
      category: "TESTING", score: item.score, level: "DEVELOPING", confidence: 90,
      evidenceStatus: item.status, strength: false, weakness: false, growthTrend: "UNAVAILABLE",
      aggregateRuleResultReference: "category:TESTING", evidenceIds: item.evidenceIds,
      repositoryIds: ["repository-1"], recommendationInputFacts: [], ruleSetVersion: "baseline-v1"
    }))
  }, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T10:00:00Z" } })));

  renderWithProviders(<SkillsPage />, ["/skills"]);

  const none = (await screen.findByRole("heading", { name: "근거 없는 역량" })).closest("article")!;
  expect(within(none).queryByRole("progressbar")).not.toBeInTheDocument();
  expect(within(none).getByRole("note", { name: "근거 상태: 근거 없음" })).toHaveTextContent("저장소 근거를 추가한 뒤 다시 분석해 주세요.");
  const insufficient = screen.getByRole("heading", { name: "근거 부족 역량" }).closest("article")!;
  expect(within(insufficient).getByRole("progressbar")).toHaveAttribute("value", "45");
  expect(within(insufficient).getByRole("note", { name: "근거 상태: 근거 부족" })).toBeInTheDocument();
  const sufficient = screen.getByRole("heading", { name: "근거 충분 역량" }).closest("article")!;
  expect(within(sufficient).getByRole("progressbar")).toHaveAttribute("value", "80");
  expect(within(sufficient).queryByText("근거 없음")).not.toBeInTheDocument();
  const legacy = screen.getByRole("heading", { name: "과거 역량" }).closest("article")!;
  expect(within(legacy).getByRole("progressbar")).toHaveAttribute("value", "60");
  expect(within(legacy).queryByRole("note", { name: /근거 상태:/ })).not.toBeInTheDocument();
});

it("renders an actionable empty state for a missing matrix", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 404 })));

  renderWithProviders(<SkillsPage />, ["/skills"]);

  expect(await screen.findByRole("heading", { name: "아직 기술 분석 결과가 없습니다" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "저장소로 이동" })).toHaveAttribute("href", "/repositories");
});

it("keeps transport errors separate and offers retry", async () => {
  const fetchMock = vi.fn()
    .mockRejectedValueOnce(new TypeError("offline"))
    .mockRejectedValueOnce(new TypeError("offline"));
  vi.stubGlobal("fetch", fetchMock);

  renderWithProviders(<SkillsPage />, ["/skills"]);

  expect(await screen.findByRole("heading", { name: "서버에 연결할 수 없습니다." })).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
  expect(fetchMock).toHaveBeenCalledTimes(2);
});

it("shows the internal error state with the request reference for a server failure", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(
    { error: { code: "INTERNAL_ERROR", message: "safe" } },
    { status: 500, headers: { "X-Request-Id": "skills-request-500" } }
  )));

  renderWithProviders(<SkillsPage />, ["/skills"]);

  expect(await screen.findByRole("heading", { name: "기술 분석을 불러오지 못했습니다" })).toBeInTheDocument();
  expect(screen.getByRole("alert")).toHaveTextContent("기존 결과는 변경되지 않았습니다.");
  expect(screen.getByText("skills-request-500")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
});
