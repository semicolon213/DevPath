import { fireEvent, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { SkillDetailPage } from "./SkillDetailPage";

afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" };

it("shows the current authoritative skill and its normalized evidence", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
    const url = String(input);
    if (url.includes("/api/v1/analyses?")) {
      const older = url.includes("cursor=older");
      return Promise.resolve(Response.json({ data: { analyses: [older ?
        { analysisId: "analysis-0", repositoryId: "repository-1", repositoryFullName: "owner/devpath", skillMatrixId: "matrix-0" } :
        { analysisId: "analysis-1", repositoryId: "repository-1", repositoryFullName: "owner/devpath", skillMatrixId: "matrix-1" }],
        nextCursor: older ? null : "older", totalCount: 2 }, metadata }));
    }
    if (url.includes("/skill-matrices/")) {
      const historical = url.endsWith("matrix-0");
      return Promise.resolve(Response.json({ data: {
        skillMatrixId: historical ? "matrix-0" : "matrix-1", evaluationId: historical ? "evaluation-0" : "evaluation-1",
        policyVersion: "skill-matrix-v2", ruleSetVersion: "baseline-v2", status: historical ? "SUPERSEDED" : "CURRENT",
        generatedAt: historical ? "2026-07-25T00:00:00Z" : "2026-08-25T00:00:00Z", strengths: [], weaknesses: [],
        skills: [{ assessmentId: historical ? "assessment-0" : "assessment-1", skillId: "skill-1", skillKey: "testing-discipline",
          skillName: "Testing Discipline", category: "TESTING", score: historical ? 54 : 72,
          level: historical ? "DEVELOPING" : "COMPETENT", confidence: historical ? 80 : 90,
          strength: false, weakness: historical, growthTrend: "UNAVAILABLE", aggregateRuleResultReference: "category:TESTING",
          evidenceIds: historical ? [] : ["evidence-1"], repositoryIds: ["repository-1"], recommendationInputFacts: [], ruleSetVersion: "baseline-v2" }]
      }, metadata }));
    }
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
  expect(screen.getAllByText("72")).toHaveLength(2);
  expect(screen.getByText("테스트 파일이 확인되었습니다.")).toBeInTheDocument();
  expect(screen.getByText(/브라우저는 계산하거나 수정하지 않습니다/)).toBeInTheDocument();
  expect(await screen.findByRole("heading", { name: "기술 평가 이력" })).toBeInTheDocument();
  expect(screen.getByText(/변화량이나 성장 추세는 계산하지 않습니다/)).toBeInTheDocument();
  expect(screen.getByText("owner/devpath")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "이전 평가 더 보기" }));
  expect(await screen.findByLabelText("저장된 공식 점수 54점")).toBeInTheDocument();
  expect(screen.getAllByText("owner/devpath")).toHaveLength(2);
  expect(screen.getByText("저장된 기술 평가를 모두 확인했습니다.")).toBeInTheDocument();
});

it("keeps current detail available when only historical matrices fail", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
    const url = String(input);
    if (url.includes("/api/v1/analyses?")) return Promise.reject(new TypeError("history offline"));
    if (url.endsWith("/evidence")) return Promise.resolve(Response.json({ data: {
      skillId: "skill-1", skillAssessmentId: "assessment-1", skillMatrixId: "matrix-1", evidence: []
    }, metadata }));
    return Promise.resolve(Response.json({ data: {
      skillMatrixId: "matrix-1", evaluationId: "evaluation-1", policyVersion: "skill-matrix-v2",
      ruleSetVersion: "baseline-v2", matrixStatus: "CURRENT", generatedAt: "2026-08-25T00:00:00Z",
      skill: { assessmentId: "assessment-1", skillId: "skill-1", skillKey: "testing-discipline",
        skillName: "Testing Discipline", category: "TESTING", score: 72, level: "COMPETENT", confidence: 90,
        strength: false, weakness: false, growthTrend: "UNAVAILABLE", aggregateRuleResultReference: "category:TESTING",
        evidenceIds: [], repositoryIds: ["repository-1"], recommendationInputFacts: [], ruleSetVersion: "baseline-v2" }
    }, metadata }));
  }));

  renderWithProviders(<Routes><Route path="/skills/:skillId" element={<SkillDetailPage />} /></Routes>, ["/skills/skill-1"]);

  expect(await screen.findByRole("heading", { name: "Testing Discipline" })).toBeInTheDocument();
  expect(await screen.findByRole("heading", { name: "기술 평가 이력만 불러오지 못했습니다." })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "이력 다시 시도" })).toBeInTheDocument();
});
