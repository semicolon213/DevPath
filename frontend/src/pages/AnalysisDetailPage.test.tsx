import { fireEvent, screen, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { AnalysisDetailPage } from "./AnalysisDetailPage";

afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T10:00:00Z" };

type EvidenceStatus = "NONE" | "INSUFFICIENT" | "SUFFICIENT" | null;

function analysisDetailResponse(input: RequestInfo | URL, evidenceStatus: EvidenceStatus = null) {
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
      evidenceStatus,
      aggregateRuleResultReference: "evaluation-id:DOCUMENTATION", evidenceIds: ["evidence-id"],
      repositoryIds: ["repository-id"], recommendationInputFacts: [], ruleSetVersion: "baseline-v1" }]
  }, metadata }));
  return Promise.reject(new Error(`Unexpected URL ${url}`));
}

it("explains immutable official rules and marks the repository's current result", async () => {
  vi.stubGlobal("fetch", vi.fn(analysisDetailResponse));

  renderWithProviders(<Routes><Route path="/analyses/:analysisId" element={<AnalysisDetailPage />} /></Routes>, ["/analyses/analysis-id"]);

  expect(await screen.findByRole("heading", { name: "owner/devpath" })).toBeInTheDocument();
  expect(screen.getByText("82.5")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "왜 이 점수가 나왔나요?" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "영역별 공식 결과" })).toBeInTheDocument();
  expect(screen.getByText("이 저장소의 현재 적용 결과")).toBeInTheDocument();
  expect(screen.getAllByText("README").length).toBeGreaterThan(0);
  expect(screen.getByText("README 파일을 확인했습니다.")).toBeInTheDocument();
  expect(screen.getByText("기술 문서화")).toBeInTheDocument();
  const officialRegion = screen.getByRole("heading", { name: "영역별 공식 결과" }).closest("section")!;
  for (const aiName of ["저장소 요약", "저장소 리뷰"]) {
    const aiRegion = screen.getByRole("region", { name: `AI 생성 · 설명만 제공 ${aiName}` });
    expect(officialRegion.compareDocumentPosition(aiRegion) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  }
  expect(document.querySelectorAll(".button-primary")).toHaveLength(0);
  expect(screen.getByRole("button", { name: "요약 생성" })).toHaveClass("button-ai");
});

it.each([
  ["NONE", false, "근거 상태: 근거 없음"],
  ["INSUFFICIENT", true, "근거 상태: 근거 부족"],
  ["SUFFICIENT", true, null],
  [null, true, null]
] as const)("renders the API %s evidence status in the historical skill matrix", async (evidenceStatus, hasProgress, noteName) => {
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => analysisDetailResponse(input, evidenceStatus)));

  renderWithProviders(<Routes><Route path="/analyses/:analysisId" element={<AnalysisDetailPage />} /></Routes>, ["/analyses/analysis-id"]);

  const section = (await screen.findByRole("heading", { name: "이 분석의 기술 역량" })).closest("section")!;
  if (hasProgress) expect(within(section).getByRole("progressbar")).toHaveAttribute("value", "100");
  else expect(within(section).queryByRole("progressbar")).not.toBeInTheDocument();
  expect(within(section).getByText(/공식 점수 100/)).toBeInTheDocument();
  if (noteName) expect(within(section).getByRole("note", { name: noteName })).toBeInTheDocument();
  else expect(within(section).queryByRole("note", { name: /근거 상태:/ })).not.toBeInTheDocument();
  const row = section.querySelector(".historical-skill-row")!;
  expect(Array.from(row.children).map(child => child.className)).toEqual(["historical-skill-row__name", "historical-skill-row__score", "historical-skill-row__status"]);
  const score = row.querySelector<HTMLElement>(".historical-skill-row__score")!;
  expect(within(score).getByText(hasProgress ? /^공식 점수 100 · 신뢰도/ : /^저장된 공식 점수 100 · 신뢰도/)).toBeInTheDocument();
  if (hasProgress) expect(within(score).getByRole("progressbar")).toBeInTheDocument();
  expect(row.textContent!.split("근거 없음").length - 1).toBe(evidenceStatus === "NONE" ? 1 : 0);
  if (noteName) expect(row.querySelector(".historical-skill-row__status")).toContainElement(within(section).getByRole("note", { name: noteName }));
});

it("uses the shared network failure state and recovers on retry", async () => {
  let analysisAttempts = 0;
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    if (input.toString().endsWith("/analyses/analysis-id")) {
      analysisAttempts += 1;
      if (analysisAttempts === 1) return Promise.reject(new TypeError("offline"));
    }
    return analysisDetailResponse(input);
  }));
  renderWithProviders(<Routes><Route path="/analyses/:analysisId" element={<AnalysisDetailPage />} /></Routes>, ["/analyses/analysis-id"]);
  expect(await screen.findByRole("heading", { name: "서버에 연결할 수 없습니다." })).toBeInTheDocument();
  expect(analysisAttempts).toBe(1);
  fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
  expect(await screen.findByRole("heading", { name: "owner/devpath" })).toBeInTheDocument();
  expect(analysisAttempts).toBe(2);
});
