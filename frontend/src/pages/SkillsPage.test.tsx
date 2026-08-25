import { fireEvent, screen } from "@testing-library/react";
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

  expect(await screen.findByRole("heading", { name: "기술 역량 분석" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "기술 문서화" })).toBeInTheDocument();
  expect(screen.getByText("100")).toBeInTheDocument();
  expect(screen.getByText("87.5%")).toBeInTheDocument();
  expect(screen.getAllByText("강점")).toHaveLength(2);
  expect(screen.getByText("점수와 등급은 AI가 아닌 Rule Engine이 계산합니다.", { exact: false })).toBeInTheDocument();
});

it("renders an actionable empty state for a missing matrix", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 404 })));

  renderWithProviders(<SkillsPage />, ["/skills"]);

  expect(await screen.findByRole("heading", { name: "아직 기술 분석 결과가 없습니다" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "저장소로 이동" })).toHaveAttribute("href", "/repositories");
});

it("keeps transport errors separate and offers retry", async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(new Response("", { status: 503 }))
    .mockResolvedValueOnce(new Response("", { status: 503 }));
  vi.stubGlobal("fetch", fetchMock);

  renderWithProviders(<SkillsPage />, ["/skills"]);

  expect(await screen.findByRole("heading", { name: "기술 분석을 불러오지 못했습니다" })).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
  expect(fetchMock).toHaveBeenCalledTimes(2);
});
