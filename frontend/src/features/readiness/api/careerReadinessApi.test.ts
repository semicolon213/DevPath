import { afterEach, describe, expect, it, vi } from "vitest";
import { getCurrentCareerReadiness } from "./careerReadinessApi";

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
});
