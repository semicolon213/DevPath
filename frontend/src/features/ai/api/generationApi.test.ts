import { afterEach, describe, expect, it, vi } from "vitest";
import { cancelGenerationJob, getGeneratedSkillExplanation, getGenerationJob, requestRepositoryReview, requestSkillExplanation } from "./generationApi";

describe("generationApi", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("uses CSRF and idempotency for generation and cancellation", async () => {
    const job = { jobId: "job-id", status: "QUEUED", validationStatus: "PENDING", artifactUrl: null,
      failureCode: null, createdAt: "2026-08-31T00:00:00Z", completedAt: null };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(json({ headerName: "X-CSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(json(job))
      .mockResolvedValueOnce(json(job))
      .mockResolvedValueOnce(json({ headerName: "X-CSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(json({ ...job, status: "CANCELED" }));
    vi.stubGlobal("fetch", fetchMock);

    await requestSkillExplanation("matrix-id");
    await getGenerationJob("job-id");
    await cancelGenerationJob("job-id");

    expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/api/v1/generation-requests"),
      expect.objectContaining({ method: "POST", body: expect.stringContaining("matrix-id"),
        headers: expect.objectContaining({ "X-CSRF-TOKEN": "csrf", "Idempotency-Key": expect.any(String) }) }));
    expect(fetchMock).toHaveBeenNthCalledWith(5, expect.stringContaining("/api/v1/generation-jobs/job-id/cancel"),
      expect.objectContaining({ method: "POST", body: "{}" }));
  });

  it("retrieves only the owner-authorized artifact API path", async () => {
    const artifact = { artifactId: "artifact-id", content: { summary: "검증된 설명", strengths: [], improvementAreas: [] } };
    const fetchMock = vi.fn().mockResolvedValue(json(artifact));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getGeneratedSkillExplanation("/api/v1/generated-artifacts/artifact-id"))
      .resolves.toMatchObject({ artifactId: "artifact-id" });
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("/api/v1/generated-artifacts/artifact-id"),
      expect.objectContaining({ credentials: "include" }));
  });

  it("requests a repository review from an analysis", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(json({ headerName: "X-CSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(json({ jobId: "job-id", status: "QUEUED" }));
    vi.stubGlobal("fetch", fetchMock);

    await requestRepositoryReview("analysis-id");

    expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/api/v1/generation-requests"),
      expect.objectContaining({ body: JSON.stringify({ taskType: "REPOSITORY_REVIEW",
        sourceResourceRefs: ["analysis-id"], outputType: "REPOSITORY_REVIEW" }) }));
  });
});

function json(data: unknown) {
  return new Response(JSON.stringify({ data, metadata: {} }), { status: 200, headers: { "Content-Type": "application/json" } });
}
