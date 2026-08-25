import { screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { SkillDetailPage } from "./SkillDetailPage";

afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" };

it("shows the current authoritative skill and its normalized evidence", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
    const url = String(input);
    if (url.endsWith("/evidence")) return Promise.resolve(Response.json({ data: {
      skillId: "skill-1", skillAssessmentId: "assessment-1", skillMatrixId: "matrix-1", evidence: [{
        evidenceId: "evidence-1", snapshotId: "snapshot-1", evidenceType: "REPOSITORY_PATH",
        sourceReference: "snapshot:snapshot-1:path:src/test", observedFactSummary: "테스트 파일이 확인되었습니다.", confidence: 100
      }] }, metadata }));
    return Promise.resolve(Response.json({ data: {
      skillMatrixId: "matrix-1", evaluationId: "evaluation-1", policyVersion: "skill-matrix-v2",
      ruleSetVersion: "baseline-v2", matrixStatus: "CURRENT", generatedAt: "2026-08-25T00:00:00Z",
      skill: { assessmentId: "assessment-1", skillId: "skill-1", skillKey: "testing-discipline",
        skillName: "Testing Discipline", category: "TESTING", score: 72, level: "COMPETENT", confidence: 90,
        strength: false, weakness: false, growthTrend: "UNAVAILABLE", aggregateRuleResultReference: "category:TESTING",
        evidenceIds: ["evidence-1"], repositoryIds: ["repository-1"], recommendationInputFacts: [], ruleSetVersion: "baseline-v2" }
    }, metadata }));
  }));

  renderWithProviders(<Routes><Route path="/skills/:skillId" element={<SkillDetailPage />} /></Routes>, ["/skills/skill-1"]);

  expect(await screen.findByRole("heading", { name: "Testing Discipline" })).toBeInTheDocument();
  expect(screen.getByText("72")).toBeInTheDocument();
  expect(screen.getByText("테스트 파일이 확인되었습니다.")).toBeInTheDocument();
  expect(screen.getByText(/브라우저는 계산하거나 수정하지 않습니다/)).toBeInTheDocument();
});
