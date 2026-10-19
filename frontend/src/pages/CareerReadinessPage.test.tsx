import { fireEvent, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
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

    renderWithProviders(undefined, ["/career-readiness"]);

    expect(await screen.findByText("67.5")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "커리어 준비도" })).toBeInTheDocument();
    expect(screen.getByText("일부 충족")).toBeInTheDocument();
    expect(screen.getByText("정책 readiness-v1")).toBeInTheDocument();
    const officialRegion = screen.getByRole("heading", { name: "역량 비교" }).closest("section")!;
    const aiRegion = screen.getByRole("region", { name: "AI 생성 커리어 코칭" });
    expect(officialRegion.compareDocumentPosition(aiRegion) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("keeps a missing readiness result as the domain empty state", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(Response.json({ error: { code: "NOT_FOUND" } }, { status: 404 }))));
    renderWithProviders(undefined, ["/career-readiness"]);
    expect(await screen.findByRole("heading", { name: "아직 준비도 결과가 없습니다" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "분석 준비" })).toHaveAttribute("href", "/repositories");
  });

  it("shows the request reference for an internal readiness failure", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(Response.json({ error: { code: "FAILED" } }, { status: 500, headers: { "X-Request-Id": "readiness-request-500" } }))));
    renderWithProviders(undefined, ["/career-readiness"]);
    expect(await screen.findByRole("heading", { name: "준비도를 불러오지 못했습니다" })).toBeInTheDocument();
    expect(screen.getByText("readiness-request-500")).toBeInTheDocument();
  });

  it("uses the shared network failure state and recovers on retry", async () => {
    let readinessAttempts = 0;
    vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
      if (!String(input).endsWith("/api/v1/career-readiness/current")) return Promise.reject(new Error(String(input)));
      readinessAttempts += 1;
      return readinessAttempts === 1
        ? Promise.reject(new TypeError("offline"))
        : Promise.resolve(Response.json({
          data: {
            careerReadinessId: "cr1", skillMatrixId: "m1", careerId: "backend", careerProfileVersionId: "cp1",
            careerProfileVersion: "career-v2", readinessPolicyVersion: "readiness-v1", ruleSetVersion: "baseline-v2",
            status: "COMPLETED", readinessScore: 67.5, readinessLevel: "COMPETENT", confidence: 82,
            unavailableCategories: [], assessedAt: "2026-08-24T00:00:00Z", skillGaps: []
          }, metadata: { requestId: "r1", apiVersion: "v1", timestamp: "2026-08-24T00:00:00Z" }
        }));
    }));
    renderWithProviders(undefined, ["/career-readiness"]);
    expect(await screen.findByRole("heading", { name: "서버에 연결할 수 없습니다." })).toBeInTheDocument();
    expect(readinessAttempts).toBe(1);
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    expect(await screen.findByText("67.5")).toBeInTheDocument();
    expect(readinessAttempts).toBe(2);
  });
});
