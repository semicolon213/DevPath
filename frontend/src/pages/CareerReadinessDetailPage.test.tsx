import { screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";

afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" };
const gap = { skillGapId: "gap-1", skillId: "skill-1", skillKey: "testing-discipline", category: "TESTING",
  actualScore: 50, actualLevel: "DEVELOPING", expectedMinimum: 60, gapState: "PARTIAL", careerWeight: 20,
  evidenceIds: ["evidence-1"] };

it("connects immutable readiness gaps to skill evidence and next actions", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
    const url = String(input);
    const target = url.endsWith("/users/me/preferences") ? { careerId: "backend", companyId: "toss", updatedAt: null }
      : url.endsWith("/careers") ? { catalogVersion: "career-v2", careers: [{ careerId: "backend", localizedName: "백엔드 개발자" }] }
        : url.endsWith("/companies") ? { catalogVersion: "company-v1", companies: [{ companyId: "toss", localizedName: "토스" }] }
          : null;
    return Promise.resolve(Response.json({
    data: target ?? (url.endsWith("/skill-gaps") ? { careerReadinessId: "readiness-1", skillGaps: [gap] } : {
      careerReadinessId: "readiness-1", skillMatrixId: "matrix-1", careerId: "backend",
      careerProfileVersionId: "profile-1", careerProfileVersion: "career-v2", readinessPolicyVersion: "readiness-v1",
      ruleSetVersion: "baseline-v2", status: "COMPLETED", readinessScore: 67.5, readinessLevel: "COMPETENT",
      confidence: 82, unavailableCategories: [], skillGaps: [gap], assessedAt: "2026-08-25T00:00:00Z"
    }), metadata
  }));
  }));
  renderWithProviders(undefined, ["/career-readiness/readiness-1"]);

  expect(await screen.findByText("67.5")).toBeInTheDocument();
  expect(screen.getByRole("navigation", { name: "현재 위치" })).toHaveTextContent("커리어 준비도준비도 상세");
  expect(screen.getByText("준비도 상세")).toHaveAttribute("aria-current", "page");
  expect(screen.getByRole("heading", { name: "커리어 준비도 상세" })).toBeInTheDocument();
  expect(await screen.findByText("평가 직무 · 백엔드 개발자")).toBeInTheDocument();
  expect(screen.queryByText(/토스/)).not.toBeInTheDocument();
  expect(screen.getByText("일부 충족")).toBeInTheDocument();
  expect(screen.getByRole("img", { name: "테스트 실제 점수 50점, 기준 60점" }).querySelector(".readiness-gap__reference")).toHaveStyle({ left: "60%" });
  expect(screen.getByRole("link", { name: "기술 평가와 근거 보기" })).toHaveAttribute("href", "/skills/skill-1");
  expect(screen.getByRole("link", { name: "추천 이력" })).toHaveAttribute("href", "/recommendations");
  expect(document.querySelectorAll(".button-primary")).toHaveLength(1);
});

it("identifies the stored career when the current target differs", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
    const url = String(input);
    const data = url.endsWith("/users/me/preferences")
      ? { careerId: "frontend", companyId: "toss", updatedAt: null }
      : url.endsWith("/careers")
        ? { catalogVersion: "career-v2", careers: [{ careerId: "backend", localizedName: "백엔드 개발자" }, { careerId: "frontend", localizedName: "프론트엔드 개발자" }] }
        : url.endsWith("/skill-gaps")
          ? { careerReadinessId: "readiness-1", skillGaps: [gap] }
          : {
            careerReadinessId: "readiness-1", skillMatrixId: "matrix-1", careerId: "backend",
            careerProfileVersionId: "profile-1", careerProfileVersion: "career-v2", readinessPolicyVersion: "readiness-v1",
            ruleSetVersion: "baseline-v2", status: "COMPLETED", readinessScore: 67.5, readinessLevel: "COMPETENT",
            confidence: 82, unavailableCategories: [], skillGaps: [gap], assessedAt: "2026-08-25T00:00:00Z"
          };
    return Promise.resolve(Response.json({ data, metadata }));
  }));
  renderWithProviders(undefined, ["/career-readiness/readiness-1"]);

  expect(await screen.findByText("평가 직무 · 백엔드 개발자")).toBeInTheDocument();
  expect(screen.getByText("현재 목표 직무가 이 저장된 결과의 평가 직무와 다릅니다.")).toBeInTheDocument();
  expect(screen.queryByText(/목표 · 프론트엔드 개발자/)).not.toBeInTheDocument();
  expect(screen.queryByText(/토스/)).not.toBeInTheDocument();
});
