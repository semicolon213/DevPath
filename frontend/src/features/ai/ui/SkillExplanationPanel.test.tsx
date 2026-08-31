import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useSkillExplanation } from "../model/useSkillExplanation";
import { SkillExplanationPanel } from "./SkillExplanationPanel";

vi.mock("../model/useSkillExplanation", () => ({ useSkillExplanation: vi.fn() }));

describe("SkillExplanationPanel", () => {
  const mutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useSkillExplanation).mockReturnValue(state() as ReturnType<typeof useSkillExplanation>);
  });

  it("starts an owner-scoped explanation request", () => {
    render(<SkillExplanationPanel skillMatrixId="matrix-id" />);
    fireEvent.click(screen.getByRole("button", { name: "설명 생성" }));
    expect(mutate).toHaveBeenCalledOnce();
  });

  it("renders only a validator-approved artifact as text", () => {
    vi.mocked(useSkillExplanation).mockReturnValue(state({
      job: { data: { status: "SUCCEEDED", artifactUrl: "/api/v1/generated-artifacts/artifact-id" } },
      artifact: { data: {
        artifactId: "artifact-id", type: "SKILL_EXPLANATION", status: "VALIDATED",
        provenance: { skillMatrixId: "matrix-id", promptContextId: "context-id", templateVersion: "v1",
          provider: "OLLAMA", model: "qwen-test", contextHash: "a".repeat(64), generatedAt: "2026-08-31T00:00:00Z" },
        validation: { status: "PASSED", validatorVersion: "skill-explanation-validator-v1",
          validatedAt: "2026-08-31T00:00:00Z", violations: [] },
        contentRef: "/api/v1/generated-artifacts/artifact-id",
        content: { summary: "검증된 설명", strengths: [{ skillKey: "testing-discipline",
          explanation: "테스트 근거가 강점입니다.", evidenceIds: [] }], improvementAreas: [] }
      } }
    }) as ReturnType<typeof useSkillExplanation>);

    render(<SkillExplanationPanel skillMatrixId="matrix-id" />);

    expect(screen.getByText("검증된 설명")).toBeInTheDocument();
    expect(screen.getByText("테스트 근거가 강점입니다.")).toBeInTheDocument();
    expect(document.querySelector("[dangerouslySetInnerHTML]")).toBeNull();
  });

  function state(overrides: Record<string, unknown> = {}) {
    return {
      request: { data: undefined, isPending: false, isError: false, mutate, reset: vi.fn() },
      job: { data: undefined, isError: false, refetch: vi.fn() },
      artifact: { data: undefined, isPending: false, isError: false, refetch: vi.fn() },
      cancel: { isPending: false, mutate: vi.fn() },
      ...overrides
    } as unknown as ReturnType<typeof useSkillExplanation>;
  }
});
