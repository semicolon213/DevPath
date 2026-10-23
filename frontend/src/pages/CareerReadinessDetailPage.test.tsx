import { screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";

afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" };
const gap = { skillGapId: "gap-1", skillId: "skill-1", skillKey: "testing-discipline", category: "TESTING",
  actualScore: 50, actualLevel: "DEVELOPING", expectedMinimum: 60, gapState: "PARTIAL", careerWeight: 20,
  evidenceIds: ["evidence-1"] };

it("connects immutable readiness gaps to skill evidence and next actions", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => Promise.resolve(Response.json({
    data: String(input).endsWith("/skill-gaps") ? { careerReadinessId: "readiness-1", skillGaps: [gap] } : {
      careerReadinessId: "readiness-1", skillMatrixId: "matrix-1", careerId: "backend",
      careerProfileVersionId: "profile-1", careerProfileVersion: "career-v2", readinessPolicyVersion: "readiness-v1",
      ruleSetVersion: "baseline-v2", status: "COMPLETED", readinessScore: 67.5, readinessLevel: "COMPETENT",
      confidence: 82, unavailableCategories: [], skillGaps: [gap], assessedAt: "2026-08-25T00:00:00Z"
    }, metadata
  }))));
  renderWithProviders(undefined, ["/career-readiness/readiness-1"]);

  expect(await screen.findByText("67.5")).toBeInTheDocument();
  expect(screen.getByRole("navigation", { name: "현재 위치" })).toHaveTextContent("커리어 준비도준비도 상세");
  expect(screen.getByText("준비도 상세")).toHaveAttribute("aria-current", "page");
  expect(screen.getByRole("heading", { name: "커리어 준비도 상세" })).toBeInTheDocument();
  expect(screen.getByText("일부 충족")).toBeInTheDocument();
  expect(screen.getByRole("img", { name: "테스트 실제 점수 50점, 기준 60점" }).querySelector(".readiness-gap__reference")).toHaveStyle({ left: "60%" });
  expect(screen.getByRole("link", { name: "기술 평가와 근거 보기" })).toHaveAttribute("href", "/skills/skill-1");
  expect(screen.getByRole("link", { name: "추천 이력" })).toHaveAttribute("href", "/recommendations");
  expect(document.querySelectorAll(".button-primary")).toHaveLength(1);
});
