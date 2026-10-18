import { screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { SkillMatrixComparisonPage } from "./SkillMatrixComparisonPage";

afterEach(() => vi.unstubAllGlobals());

const skill = (score: number, level: "DEVELOPING" | "STRONG", evidenceCount: number) => ({
  assessmentId: `assessment-${score}`, skillId: "skill-testing", skillKey: "testing-discipline",
  skillName: "Testing Discipline", category: "TESTING", score, level, confidence: 90,
  strength: level === "STRONG", weakness: false, growthTrend: "UNAVAILABLE",
  aggregateRuleResultReference: "category:TESTING", evidenceIds: Array.from({ length: evidenceCount }, (_, i) => `e-${i}`),
  repositoryIds: ["repository-1"], recommendationInputFacts: [], ruleSetVersion: "baseline-v2"
});
const matrix = (id: string, score: number, level: "DEVELOPING" | "STRONG", status: "CURRENT" | "SUPERSEDED") => ({
  skillMatrixId: id, evaluationId: `evaluation-${id}`, policyVersion: "skill-matrix-v2", ruleSetVersion: "baseline-v2",
  status, skills: [skill(score, level, score === 85 ? 2 : 1)], strengths: level === "STRONG" ? ["testing-discipline"] : [],
  weaknesses: [], generatedAt: "2026-08-25T00:00:00Z"
});

it("compares stored skill values without deriving a delta", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({
    data: { matrices: [matrix("matrix-a", 55, "DEVELOPING", "SUPERSEDED"), matrix("matrix-b", 85, "STRONG", "CURRENT")] },
    metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" }
  })));
  renderWithProviders(<Routes><Route path="/skills/compare" element={<SkillMatrixComparisonPage />} /></Routes>,
    ["/skills/compare?skillMatrixId=matrix-a&skillMatrixId=matrix-b"]);

  const row = await screen.findByRole("row", { name: /테스트 역량/ });
  expect(screen.getByRole("heading", { name: "기술 역량 비교" })).toBeInTheDocument();
  expect(row).toHaveTextContent("55");
  expect(row).toHaveTextContent("85");
  expect(screen.getByText(/차이 점수나 성장 추세를 새로 계산하지 않습니다/)).toBeInTheDocument();
  const region = screen.getByRole("region", { name: "역량별 저장 결과 표" });
  expect(region).toHaveAttribute("tabindex", "0");
  expect(screen.getByText("표를 좌우로 스크롤해 비교할 수 있습니다.")).toBeInTheDocument();
});
