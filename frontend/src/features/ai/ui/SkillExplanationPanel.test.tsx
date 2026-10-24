import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { useSkillExplanation } from "../model/useSkillExplanation";
import { useAnalysisDetail } from "../../analysis/model/useAnalysis";
import { SkillExplanationPanel } from "./SkillExplanationPanel";

vi.mock("../model/useSkillExplanation", () => ({ useSkillExplanation: vi.fn() }));
vi.mock("../../analysis/model/useAnalysis", () => ({ useAnalysisDetail: vi.fn() }));

describe("SkillExplanationPanel", () => {
  const mutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useSkillExplanation).mockReturnValue(state() as ReturnType<typeof useSkillExplanation>);
    vi.mocked(useAnalysisDetail).mockReturnValue({ data: undefined } as ReturnType<typeof useAnalysisDetail>);
  });

  it("starts an owner-scoped explanation request", () => {
    render(<SkillExplanationPanel skillMatrixId="matrix-id" />);
    expect(screen.getByText("공식 점수는 공식 분석 결과(Rule Engine) 그대로 유지하며, AI는 근거가 확인된 강점과 개선 영역만 설명합니다.")).toBeInTheDocument();
    expect(screen.queryByText(/Rule Engine 결과/)).not.toBeInTheDocument();
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
        validation: { status: "PASSED", validatorVersion: "skill-explanation-validator-v5",
          validatedAt: "2026-08-31T00:00:00Z", violations: [] },
        contentRef: "/api/v1/generated-artifacts/artifact-id",
        content: { summary: "검증된 설명", strengths: [{ skillKey: "testing-discipline",
          explanation: "테스트 근거가 강점입니다.", evidenceIds: ["evidence-id"] }], improvementAreas: [] }
      } }
    }) as ReturnType<typeof useSkillExplanation>);

    render(<SkillExplanationPanel skillMatrixId="matrix-id" />);

    expect(screen.getByText("검증된 설명")).toBeInTheDocument();
    expect(screen.getByText("테스트 근거가 강점입니다.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "근거 1: 근거 ID evidence-id" })).toHaveAttribute("href", "#footnote-skill-explanation-evidence-id");
    expect(screen.getByText("evidence-id")).toBeInTheDocument();
    expect(document.querySelector("[dangerouslySetInnerHTML]")).toBeNull();
  });

  it("resolves Skills page footnotes from the artifact analysis", () => {
    vi.mocked(useSkillExplanation).mockReturnValue(state({ artifact: { data: artifact(["analysis-evidence"]) } }) as ReturnType<typeof useSkillExplanation>);
    vi.mocked(useAnalysisDetail).mockReturnValue({ data: { evidence: [resolved("analysis-evidence", "분석 전체 근거입니다.")] } } as ReturnType<typeof useAnalysisDetail>);

    render(<MemoryRouter><SkillExplanationPanel skillMatrixId="matrix-id" /></MemoryRouter>);

    expect(useAnalysisDetail).toHaveBeenCalledWith("analysis-id");
    expect(screen.getByText("분석 전체 근거입니다.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "근거 1: 저장소 파일 snapshot:snapshot-1:path:src/main" })).toBeInTheDocument();
  });

  it("resolves Skill detail footnotes outside the skill evidence", () => {
    vi.mocked(useSkillExplanation).mockReturnValue(state({ artifact: { data: artifact(["other-skill-evidence"]) } }) as ReturnType<typeof useSkillExplanation>);
    vi.mocked(useAnalysisDetail).mockReturnValue({ data: { evidence: [resolved("other-skill-evidence", "다른 기술의 분석 근거입니다.")] } } as ReturnType<typeof useAnalysisDetail>);

    render(<MemoryRouter><SkillExplanationPanel skillMatrixId="matrix-id" evidence={[resolved("skill-evidence", "현재 기술 근거입니다.")]} /></MemoryRouter>);

    expect(screen.getByText("다른 기술의 분석 근거입니다.")).toBeInTheDocument();
    expect(screen.queryByText("other-skill-evidence")).not.toBeInTheDocument();
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

function artifact(evidenceIds: string[]) {
  return {
    artifactId: "artifact-id", type: "SKILL_EXPLANATION", status: "VALIDATED",
    provenance: { skillMatrixId: "matrix-id", analysisId: "analysis-id", promptContextId: "context-id", templateVersion: "v1",
      provider: "OLLAMA", model: "qwen-test", contextHash: "a".repeat(64), generatedAt: "2026-08-31T00:00:00Z" },
    validation: { status: "PASSED", validatorVersion: "skill-explanation-validator-v5", validatedAt: "2026-08-31T00:00:00Z", violations: [] },
    contentRef: "/api/v1/generated-artifacts/artifact-id",
    content: { summary: "검증된 설명", strengths: [{ skillKey: "testing-discipline", explanation: "테스트 근거가 강점입니다.", evidenceIds }], improvementAreas: [] }
  } as const;
}

function resolved(evidenceId: string, observedFactSummary: string) {
  return { evidenceId, ruleId: "TEST_FILES", contributionRole: "DIRECT" as const, evidenceType: "REPOSITORY_PATH" as const,
    sourceReference: "snapshot:snapshot-1:path:src/main", observedFactSummary, confidence: 100 };
}
