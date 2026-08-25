import { screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { AnalysisDetailPage } from "./AnalysisDetailPage";

afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T10:00:00Z" };

it("explains immutable official rules and marks the repository's current result", async () => {
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    const url = input.toString();
    if (url.endsWith("/analyses/analysis-id")) return Promise.resolve(Response.json({ data: {
      analysisId: "analysis-id", repositoryId: "repository-id", snapshotId: "snapshot-id",
      evaluationId: "evaluation-id", skillMatrixId: "matrix-id", analysisScope: "REPOSITORY_BASELINE",
      currentForRepository: true, completedAt: "2026-08-11T10:00:00Z"
    }, metadata }));
    if (url.endsWith("/repositories/repository-id")) return Promise.resolve(Response.json({ data: {
      repositoryId: "repository-id", fullName: "owner/devpath", owner: "owner", visibility: "PUBLIC",
      defaultBranch: "main", lifecycle: "ACTIVE", syncStatus: "SYNCHRONIZED", providerArchived: false,
      htmlUrl: "https://github.com/owner/devpath", currentSnapshotId: "snapshot-id"
    }, metadata }));
    if (url.endsWith("/evidence")) return Promise.resolve(Response.json({ data: { evaluationId: "evaluation-id", evidence: [{
      evidenceId: "evidence-id", ruleId: "README_PRESENT", contributionRole: "DIRECT", evidenceType: "REPOSITORY_PATH",
      sourceReference: "README.md", observedFactSummary: "README 파일을 확인했습니다.", confidence: 100
    }] }, metadata }));
    if (url.endsWith("/rule-evaluations/evaluation-id")) return Promise.resolve(Response.json({ data: {
      evaluationId: "evaluation-id", snapshotId: "snapshot-id", ruleSetVersionId: "version-id",
      ruleSetVersion: "baseline-v1", formulaLibraryVersion: "formula-v1", extractorVersion: "extractor-v1",
      overallScore: 82.5, confidence: 95, evidenceSummary: { evidenceCount: 1, rulesWithEvidence: 1, missingEvidenceCount: 0 },
      categoryScores: [{ category: "DOCUMENTATION", score: 100, weight: 0.2, confidence: 100,
        ruleResults: [{ ruleId: "README_PRESENT", ruleVersion: "1.0", status: "PASSED", rawValue: 1,
          score: 100, weight: 1, formulaId: "PRESENCE", trace: "README_PRESENT=1", evidenceReferences: ["README.md"] }],
        missingEvidence: [] }], warnings: [], completedAt: "2026-08-11T10:00:00Z"
    }, metadata }));
    if (url.endsWith("/skill-matrices/matrix-id")) return Promise.resolve(Response.json({ data: {
      skillMatrixId: "matrix-id", evaluationId: "evaluation-id", policyVersion: "skill-matrix-v1",
      ruleSetVersion: "baseline-v1", status: "SUPERSEDED", strengths: ["technical-documentation"], weaknesses: [],
      generatedAt: "2026-08-11T10:00:00Z", skills: [{ assessmentId: "assessment-id", skillId: "skill-id",
        skillKey: "technical-documentation", skillName: "Technical Documentation", category: "DOCUMENTATION",
        score: 100, level: "STRONG", confidence: 100, strength: true, weakness: false, growthTrend: "UNAVAILABLE",
        aggregateRuleResultReference: "evaluation-id:DOCUMENTATION", evidenceIds: ["evidence-id"],
        repositoryIds: ["repository-id"], recommendationInputFacts: [], ruleSetVersion: "baseline-v1" }]
    }, metadata }));
    return Promise.reject(new Error(`Unexpected URL ${url}`));
  }));

  renderWithProviders(<Routes><Route path="/analyses/:analysisId" element={<AnalysisDetailPage />} /></Routes>, ["/analyses/analysis-id"]);

  expect(await screen.findByRole("heading", { name: "owner/devpath" })).toBeInTheDocument();
  expect(screen.getByText("82.5")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "왜 이 점수가 나왔나요?" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "영역별 공식 결과" })).toBeInTheDocument();
  expect(screen.getByText("이 저장소의 현재 적용 결과")).toBeInTheDocument();
  expect(screen.getAllByText("README").length).toBeGreaterThan(0);
  expect(screen.getByText("README 파일을 확인했습니다.")).toBeInTheDocument();
  expect(screen.getByText("기술 문서화")).toBeInTheDocument();
});
