import { afterEach, describe, expect, it, vi } from "vitest";
import { getCareerReadinessWorkspace, getCurrentCareerReadiness } from "./careerReadinessApi";

describe("careerReadinessApi", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("reads the current deterministic readiness with credentials", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({
      data: { careerReadinessId: "cr1", status: "COMPLETED", readinessScore: 70 },
      metadata: { requestId: "r1", apiVersion: "v1", timestamp: "2026-08-24T00:00:00Z" }
    }));
    vi.stubGlobal("fetch", fetchMock);

    expect((await getCurrentCareerReadiness()).readinessScore).toBe(70);
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/career-readiness/current",
      expect.objectContaining({ credentials: "include" }));
  });

  it("loads immutable readiness detail and canonical skill gaps together", async () => {
    const fetchMock = vi.fn((input: string | URL | Request) => Promise.resolve(Response.json({
      data: String(input).endsWith("/skill-gaps") ? { careerReadinessId: "cr1", skillGaps: [] }
        : { careerReadinessId: "cr1", skillGaps: [{ skillGapId: "embedded" }] },
      metadata: { requestId: "r1", apiVersion: "v1", timestamp: "2026-08-24T00:00:00Z" }
    })));
    vi.stubGlobal("fetch", fetchMock);

    const workspace = await getCareerReadinessWorkspace("cr1");

    expect(workspace.readiness.skillGaps).toEqual([]);
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/career-readiness/cr1",
      expect.objectContaining({ credentials: "include" }));
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/career-readiness/cr1/skill-gaps",
      expect.objectContaining({ credentials: "include" }));
  });
});
