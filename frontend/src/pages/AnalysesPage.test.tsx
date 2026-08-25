import { screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { AnalysesPage } from "./AnalysesPage";

afterEach(() => vi.unstubAllGlobals());

it("renders the repository's newest official analysis as current", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({ data: {
    analyses: [{ analysisId: "analysis-id", repositoryId: "repository-id", repositoryFullName: "owner/devpath",
      snapshotId: "snapshot-id", evaluationId: "evaluation-id", skillMatrixId: "matrix-id",
      analysisScope: "REPOSITORY_BASELINE", currentForRepository: true, overallScore: 78.5, confidence: 91,
      ruleSetVersion: "baseline-v1", policyVersion: "skill-matrix-v1", completedAt: "2026-08-11T10:00:00Z" }],
    limit: 20, nextCursor: null, totalCount: 1
  }, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T10:00:00Z" } })));

  renderWithProviders(<Routes><Route path="/analyses" element={<AnalysesPage />} /></Routes>, ["/analyses"]);

  expect(await screen.findByRole("heading", { name: "분석 이력" })).toBeInTheDocument();
  expect(screen.getByText("owner/devpath")).toBeInTheDocument();
  expect(screen.getByLabelText("공식 점수 78.5점")).toBeInTheDocument();
  expect(screen.getByText("현재 적용")).toBeInTheDocument();
  expect(screen.getByText("91%")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "결과 상세" })).toHaveAttribute("href", "/analyses/analysis-id");
});
