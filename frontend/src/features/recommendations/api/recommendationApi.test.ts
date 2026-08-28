import { afterEach, describe, expect, it, vi } from "vitest";
import { archiveRoadmap } from "./recommendationApi";

const metadata = { requestId: "request-1", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" };

describe("recommendationApi", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("archives a roadmap with the server CSRF header and an idempotency key", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf-token" }, metadata }))
      .mockResolvedValueOnce(Response.json({ data: {
        roadmapId: "roadmap-1", recommendationSetId: "set-1", policyVersion: "roadmap-v1",
        status: "ARCHIVED", progressPercent: 0, milestones: [], steps: [],
        generatedAt: metadata.timestamp, updatedAt: metadata.timestamp
      }, metadata }));
    vi.stubGlobal("fetch", fetchMock);

    await archiveRoadmap("roadmap-1");

    expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/api/v1/learning-roadmaps/roadmap-1/archive"), expect.objectContaining({
      method: "POST",
      credentials: "include",
      headers: expect.objectContaining({
        "X-CSRF-TOKEN": "csrf-token",
        "Idempotency-Key": expect.any(String)
      })
    }));
  });
});
