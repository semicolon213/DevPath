import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useLearningRecommendation } from "../model/useLearningRecommendation";
import { LearningRecommendationPanel } from "./LearningRecommendationPanel";

vi.mock("../model/useLearningRecommendation", () => ({ useLearningRecommendation: vi.fn() }));

describe("LearningRecommendationPanel", () => {
  const mutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useLearningRecommendation).mockReturnValue(state() as ReturnType<typeof useLearningRecommendation>);
  });

  it("starts a guide for the deterministic roadmap", () => {
    render(<LearningRecommendationPanel learningRoadmapId="roadmap-id" />);
    fireEvent.click(screen.getByRole("button", { name: "가이드 생성" }));
    expect(mutate).toHaveBeenCalledOnce();
  });

  it("renders generated practice advice with server-restored completion criteria", () => {
    vi.mocked(useLearningRecommendation).mockReturnValue(state({
      job: { data: { status: "SUCCEEDED", artifactUrl: "/api/v1/generated-artifacts/artifact-id" } },
      artifact: { data: {
        artifactId: "artifact-id", type: "LEARNING_RECOMMENDATION", status: "VALIDATED",
        provenance: { skillMatrixId: "matrix-id", careerReadinessId: "readiness-id",
          learningRoadmapId: "roadmap-id", promptContextId: "context-id", templateVersion: "v1",
          provider: "OLLAMA", model: "qwen-test", contextHash: "a".repeat(64),
          generatedAt: "2026-09-02T00:00:00Z" },
        validation: { status: "PASSED", validatorVersion: "learning-recommendation-validator-v2",
          validatedAt: "2026-09-02T00:00:00Z", violations: [] },
        contentRef: "/api/v1/generated-artifacts/artifact-id",
        content: { recommendationSummary: "실행 순서 해석", steps: [{ roadmapStepId: "step-id", position: 0,
          milestoneExplanation: "테스트 역량을 적용하는 단계입니다.", objective: "회귀를 막는 습관을 익힙니다.",
          practiceTask: "작은 기능에 자동 테스트를 추가하세요.", completionCriteria: "All tests pass",
          expectedEvidence: ["test files"] }] }
      } }
    }) as ReturnType<typeof useLearningRecommendation>);

    render(<LearningRecommendationPanel learningRoadmapId="roadmap-id" />);

    expect(screen.getByText("작은 기능에 자동 테스트를 추가하세요.")).toBeInTheDocument();
    expect(screen.getByText("All tests pass")).toBeInTheDocument();
    expect(screen.getByText("필요 근거: test files")).toBeInTheDocument();
  });

  function state(overrides: Record<string, unknown> = {}) {
    return {
      request: { data: undefined, isPending: false, isError: false, mutate, reset: vi.fn() },
      job: { data: undefined, isError: false, refetch: vi.fn() },
      artifact: { data: undefined, isPending: false, isError: false, refetch: vi.fn() },
      cancel: { isPending: false, mutate: vi.fn() },
      ...overrides
    } as unknown as ReturnType<typeof useLearningRecommendation>;
  }
});
