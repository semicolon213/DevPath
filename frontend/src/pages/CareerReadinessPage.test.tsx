import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRoutes } from "../routes/AppRoutes";
import { renderWithProviders } from "../test/renderWithProviders";

describe("CareerReadinessPage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("shows the official result, version basis, and ordered category gaps", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({
      data: {
        careerReadinessId: "cr1", skillMatrixId: "m1", careerId: "backend", careerProfileVersionId: "cp1",
        careerProfileVersion: "career-v2", readinessPolicyVersion: "readiness-v1", ruleSetVersion: "baseline-v2",
        status: "COMPLETED", readinessScore: 67.5, readinessLevel: "COMPETENT", confidence: 82,
        unavailableCategories: [], assessedAt: "2026-08-24T00:00:00Z",
        skillGaps: [{ skillGapId: "g1", skillId: "s1", skillKey: "testing-discipline", category: "TESTING",
          actualScore: 50, actualLevel: "DEVELOPING", expectedMinimum: 60, gapState: "PARTIAL", careerWeight: 20,
          evidenceIds: ["e1"] }]
      }, metadata: { requestId: "r1", apiVersion: "v1", timestamp: "2026-08-24T00:00:00Z" }
    })));

    renderWithProviders(<AppRoutes />, ["/career-readiness"]);

    expect(await screen.findByRole("heading", { name: "커리어 준비도" })).toBeInTheDocument();
    expect(screen.getByText("67.5")).toBeInTheDocument();
    expect(screen.getByText("일부 충족")).toBeInTheDocument();
    expect(screen.getByText("정책 readiness-v1")).toBeInTheDocument();
  });
});
