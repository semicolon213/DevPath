import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useCareerCoaching } from "../model/useCareerCoaching";
import { CareerCoachingPanel } from "./CareerCoachingPanel";

vi.mock("../model/useCareerCoaching", () => ({ useCareerCoaching: vi.fn() }));

describe("CareerCoachingPanel", () => {
  const mutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useCareerCoaching).mockReturnValue(state() as ReturnType<typeof useCareerCoaching>);
  });

  it("starts coaching for the readiness result", () => {
    render(<MemoryRouter><CareerCoachingPanel careerReadinessId="readiness-id" /></MemoryRouter>);
    fireEvent.click(screen.getByRole("button", { name: "코칭 생성" }));
    expect(mutate).toHaveBeenCalledOnce();
  });

  it("renders validated advice and links each step to its official recommendation", () => {
    vi.mocked(useCareerCoaching).mockReturnValue(state({
      job: { data: { status: "SUCCEEDED", artifactUrl: "/api/v1/generated-artifacts/artifact-id" } },
      artifact: { data: {
        artifactId: "artifact-id", type: "CAREER_ADVICE", status: "VALIDATED",
        provenance: { skillMatrixId: "matrix-id", careerReadinessId: "readiness-id", promptContextId: "context-id",
          templateVersion: "v1", provider: "OLLAMA", model: "qwen-test", contextHash: "a".repeat(64),
          generatedAt: "2026-09-02T00:00:00Z" },
        validation: { status: "PASSED", validatorVersion: "career-advice-validator-v2",
          validatedAt: "2026-09-02T00:00:00Z", violations: [] },
        contentRef: "/api/v1/generated-artifacts/artifact-id",
        content: { readinessSummary: "준비도 해석", strengthExplanation: "강점 활용법",
          gapExplanation: "격차 보완법", confidenceNotice: "확인된 범위의 조언",
          nextSteps: [{ recommendationId: "recommendation-id", advice: "작은 결과물부터 완성하세요." }] }
      } }
    }) as ReturnType<typeof useCareerCoaching>);

    render(<MemoryRouter><CareerCoachingPanel careerReadinessId="readiness-id" /></MemoryRouter>);

    expect(screen.getByText("작은 결과물부터 완성하세요.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "공식 추천 근거 보기" }))
      .toHaveAttribute("href", "/recommendations/recommendation-id");
  });

  function state(overrides: Record<string, unknown> = {}) {
    return {
      request: { data: undefined, isPending: false, isError: false, mutate, reset: vi.fn() },
      job: { data: undefined, isError: false, refetch: vi.fn() },
      artifact: { data: undefined, isPending: false, isError: false, refetch: vi.fn() },
      cancel: { isPending: false, mutate: vi.fn() },
      ...overrides
    } as unknown as ReturnType<typeof useCareerCoaching>;
  }
});
