import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useRepositoryReview } from "../model/useRepositoryReview";
import { RepositoryReviewPanel } from "./RepositoryReviewPanel";

vi.mock("../model/useRepositoryReview", () => ({ useRepositoryReview: vi.fn() }));

describe("RepositoryReviewPanel", () => {
  const mutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useRepositoryReview).mockReturnValue(state() as ReturnType<typeof useRepositoryReview>);
  });

  it("starts a repository review request", () => {
    render(<RepositoryReviewPanel analysisId="analysis-id" />);
    fireEvent.click(screen.getByRole("button", { name: "리뷰 생성" }));
    expect(mutate).toHaveBeenCalledOnce();
  });

  it("renders validated sections as plain text", () => {
    vi.mocked(useRepositoryReview).mockReturnValue(state({
      job: { data: { status: "SUCCEEDED", artifactUrl: "/api/v1/generated-artifacts/artifact-id" } },
      artifact: { data: {
        artifactId: "artifact-id", type: "REPOSITORY_REVIEW", status: "VALIDATED",
        provenance: { skillMatrixId: "matrix-id", analysisId: "analysis-id", promptContextId: "context-id",
          templateVersion: "v1", provider: "OLLAMA", model: "qwen-test", contextHash: "a".repeat(64),
          generatedAt: "2026-08-31T00:00:00Z" },
        validation: { status: "PASSED", validatorVersion: "repository-review-validator-v1",
          validatedAt: "2026-08-31T00:00:00Z", violations: [] },
        contentRef: "/api/v1/generated-artifacts/artifact-id",
        content: { summary: "근거 기반 리뷰", sections: [{ category: "DOCUMENTATION",
          review: "문서 근거가 확인됩니다", evidenceIds: ["evidence-id"] }] }
      } }
    }) as ReturnType<typeof useRepositoryReview>);

    render(<RepositoryReviewPanel analysisId="analysis-id" />);

    expect(screen.getByText("근거 기반 리뷰")).toBeInTheDocument();
    expect(screen.getByText("문서 근거가 확인됩니다")).toBeInTheDocument();
    expect(document.querySelector("[dangerouslySetInnerHTML]")).toBeNull();
  });

  function state(overrides: Record<string, unknown> = {}) {
    return {
      request: { data: undefined, isPending: false, isError: false, mutate, reset: vi.fn() },
      job: { data: undefined, isError: false, refetch: vi.fn() },
      artifact: { data: undefined, isPending: false, isError: false, refetch: vi.fn() },
      cancel: { isPending: false, mutate: vi.fn() },
      ...overrides
    } as unknown as ReturnType<typeof useRepositoryReview>;
  }
});
