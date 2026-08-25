import { afterEach, expect, it, vi } from "vitest";
import { getAnalysisHistory, getAnalysisJob, requestAnalysis } from "./analysisApi";

afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };

it("queues and polls a CSRF-protected deterministic analysis", async () => {
  const queued = { jobId: "job-id", jobType: "REPOSITORY_ANALYSIS", status: "queued" };
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf" }, metadata }))
    .mockResolvedValueOnce(Response.json({ data: queued, metadata }))
    .mockResolvedValueOnce(Response.json({ data: { ...queued, status: "succeeded" }, metadata }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(requestAnalysis("repository-id")).resolves.toMatchObject({ status: "queued" });
  await expect(getAnalysisJob("job-id")).resolves.toMatchObject({ status: "succeeded" });

  expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/api/v1/analyses"), expect.objectContaining({
    method: "POST",
    headers: expect.objectContaining({ "X-CSRF-TOKEN": "csrf", "Idempotency-Key": expect.any(String), "Content-Type": "application/json" }),
    body: JSON.stringify({ repositoryId: "repository-id", analysisScope: "REPOSITORY_BASELINE" })
  }));
  expect(fetchMock).toHaveBeenNthCalledWith(3, expect.stringContaining("/api/v1/analysis-jobs/job-id"), expect.anything());
});

it("loads cursor-paginated official analysis history without recalculation", async () => {
  const fetchMock = vi.fn().mockResolvedValueOnce(Response.json({ data: {
    analyses: [{ analysisId: "analysis-id", repositoryId: "repository-id", repositoryFullName: "owner/devpath",
      snapshotId: "snapshot-id", evaluationId: "evaluation-id", skillMatrixId: "matrix-id",
      analysisScope: "REPOSITORY_BASELINE", currentForRepository: true, overallScore: 78.5, confidence: 90,
      ruleSetVersion: "baseline-v1", policyVersion: "skill-matrix-v1", completedAt: "2026-08-11T10:00:00Z" }],
    limit: 20, nextCursor: "next", totalCount: 21
  }, metadata }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(getAnalysisHistory()).resolves.toMatchObject({
    totalCount: 21,
    analyses: [expect.objectContaining({ overallScore: 78.5, confidence: 90, currentForRepository: true })]
  });
  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("/api/v1/analyses?limit=20"), expect.anything());
});
