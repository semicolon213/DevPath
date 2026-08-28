import { fireEvent, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { RepositorySnapshotDetailPage } from "./RepositorySnapshotDetailPage";

afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-26T00:00:00Z" };

it("shows immutable provenance and measured collection scope for the current snapshot", async () => {
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/analyses?")) return Promise.resolve(Response.json({ data: { analyses: [{
      analysisId: "analysis-1", repositoryId: "repository-1", repositoryFullName: "owner/devpath",
      snapshotId: "snapshot-1", evaluationId: "evaluation-1", skillMatrixId: "matrix-1",
      analysisScope: "REPOSITORY_BASELINE", currentForRepository: true, completedAt: "2026-08-26T01:00:00Z",
      overallScore: 82.5, confidence: 95, ruleSetVersion: "baseline-v2", policyVersion: "skill-matrix-v2"
    }], limit: 20, nextCursor: null, totalCount: 1 }, metadata }));
    if (url.endsWith("/snapshots/snapshot-1")) return Promise.resolve(Response.json({ data: {
      snapshotId: "snapshot-1", repositoryId: "repository-1", capturedAt: "2026-08-26T00:00:00Z",
      sourceRevision: "abcdef1234567890", status: "READY", immutable: true, contentHash: "sha256:content-hash",
      branchCount: 3, commitCount: 42, pullRequestCount: 8, issueCount: 5, documentCount: 2
    }, metadata }));
    return Promise.resolve(Response.json({ data: {
      repositoryId: "repository-1", fullName: "owner/devpath", currentSnapshotId: "snapshot-1"
    }, metadata }));
  }));

  renderWithProviders(<Routes><Route path="/repositories/:repositoryId/snapshots/:snapshotId" element={<RepositorySnapshotDetailPage />} /></Routes>,
    ["/repositories/repository-1/snapshots/snapshot-1"]);

  expect(await screen.findByRole("heading", { name: "owner/devpath 스냅샷" })).toBeInTheDocument();
  expect(screen.getByLabelText("스냅샷 적용 상태")).toHaveTextContent("현재 분석 기준");
  expect(screen.getByText("abcdef1234567890")).toBeInTheDocument();
  expect(screen.getByText("sha256:content-hash")).toBeInTheDocument();
  expect(screen.getByText("42")).toBeInTheDocument();
  expect(screen.getByText(/점수나 품질 판정이 아닙니다/)).toBeInTheDocument();
  expect(await screen.findByRole("heading", { name: "이 스냅샷의 공식 분석" })).toBeInTheDocument();
  expect(screen.getByLabelText("저장된 공식 점수 82.5점")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "공식 분석과 근거 보기" })).toHaveAttribute("href", "/analyses/analysis-1");
});

it("does not expose whether an unavailable snapshot belongs to another owner", async () => {
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => String(input).includes("/snapshots/")
    ? Promise.resolve(Response.json({ code: "NOT_FOUND" }, { status: 404 }))
    : Promise.resolve(Response.json({ data: { repositoryId: "repository-1", fullName: "owner/devpath", currentSnapshotId: null }, metadata }))));

  renderWithProviders(<Routes><Route path="/repositories/:repositoryId/snapshots/:snapshotId" element={<RepositorySnapshotDetailPage />} /></Routes>,
    ["/repositories/repository-1/snapshots/missing"]);

  expect(await screen.findByRole("alert")).toHaveTextContent("없거나 현재 계정으로 접근할 수 없습니다");
});

it("keeps snapshot provenance visible when only linked analysis history fails", async () => {
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/analyses?")) return Promise.reject(new TypeError("analysis history offline"));
    if (url.endsWith("/snapshots/snapshot-1")) return Promise.resolve(Response.json({ data: {
      snapshotId: "snapshot-1", repositoryId: "repository-1", capturedAt: "2026-08-26T00:00:00Z",
      sourceRevision: "abcdef1234567890", status: "READY", immutable: true, contentHash: "sha256:content-hash",
      branchCount: 3, commitCount: 42, pullRequestCount: 8, issueCount: 5, documentCount: 2
    }, metadata }));
    return Promise.resolve(Response.json({ data: {
      repositoryId: "repository-1", fullName: "owner/devpath", currentSnapshotId: "snapshot-1"
    }, metadata }));
  }));

  renderWithProviders(<Routes><Route path="/repositories/:repositoryId/snapshots/:snapshotId" element={<RepositorySnapshotDetailPage />} /></Routes>,
    ["/repositories/repository-1/snapshots/snapshot-1"]);

  expect(await screen.findByText("sha256:content-hash")).toBeInTheDocument();
  expect(await screen.findByRole("heading", { name: "연결된 분석만 불러오지 못했습니다." })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "분석 이력 다시 시도" })).toBeInTheDocument();
});

it("loads older repository history until an analysis for this snapshot is found", async () => {
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/analyses?")) {
      const older = url.includes("cursor=older");
      return Promise.resolve(Response.json({ data: { analyses: [{
        analysisId: older ? "analysis-old" : "analysis-new", repositoryId: "repository-1",
        repositoryFullName: "owner/devpath", snapshotId: older ? "snapshot-1" : "snapshot-2",
        evaluationId: older ? "evaluation-old" : "evaluation-new", skillMatrixId: older ? "matrix-old" : "matrix-new",
        analysisScope: "REPOSITORY_BASELINE", currentForRepository: !older, completedAt: "2026-08-26T01:00:00Z",
        overallScore: older ? 71 : 82, confidence: 90, ruleSetVersion: "baseline-v2", policyVersion: "skill-matrix-v2"
      }], limit: 20, nextCursor: older ? null : "older", totalCount: 2 }, metadata }));
    }
    if (url.endsWith("/snapshots/snapshot-1")) return Promise.resolve(Response.json({ data: {
      snapshotId: "snapshot-1", repositoryId: "repository-1", capturedAt: "2026-07-26T00:00:00Z",
      sourceRevision: "old-revision", status: "SUPERSEDED", immutable: true, contentHash: "sha256:old",
      branchCount: 1, commitCount: 10, pullRequestCount: 1, issueCount: 1, documentCount: 1
    }, metadata }));
    return Promise.resolve(Response.json({ data: {
      repositoryId: "repository-1", fullName: "owner/devpath", currentSnapshotId: "snapshot-2"
    }, metadata }));
  }));

  renderWithProviders(<Routes><Route path="/repositories/:repositoryId/snapshots/:snapshotId" element={<RepositorySnapshotDetailPage />} /></Routes>,
    ["/repositories/repository-1/snapshots/snapshot-1"]);

  expect(await screen.findByText("더 오래된 분석 이력을 확인할 수 있습니다.")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "이전 분석 더 확인" }));
  expect(await screen.findByLabelText("저장된 공식 점수 71점")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "공식 분석과 근거 보기" })).toHaveAttribute("href", "/analyses/analysis-old");
});
