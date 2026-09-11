import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useRepositorySummary } from "../model/useRepositorySummary";
import { RepositorySummaryPanel } from "./RepositorySummaryPanel";

vi.mock("../model/useRepositorySummary", () => ({ useRepositorySummary: vi.fn() }));

describe("RepositorySummaryPanel", () => {
  const mutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useRepositorySummary).mockReturnValue(state() as ReturnType<typeof useRepositorySummary>);
  });

  it("requests a summary and renders validated claims with both reference types", () => {
    render(<RepositorySummaryPanel analysisId="analysis-id" />);
    fireEvent.click(screen.getByRole("button", { name: "요약 생성" }));
    expect(mutate).toHaveBeenCalledOnce();

    vi.mocked(useRepositorySummary).mockReturnValue(state({ artifact: { data: artifact(), isPending: false, isError: false, refetch: vi.fn() } }) as ReturnType<typeof useRepositorySummary>);
    render(<RepositorySummaryPanel analysisId="analysis-id" />);
    expect(screen.getByText("Grounded summary")).toBeInTheDocument();
    expect(screen.getByText("evidence-id", { exact: false })).toBeInTheDocument();
    expect(screen.getByText("rule-output", { exact: false })).toBeInTheDocument();
    expect(screen.getByText("repository-summary-validator-v1", { exact: false })).toBeInTheDocument();
    expect(document.querySelector("[dangerouslySetInnerHTML]")).toBeNull();
  });

  it("shows the rejection state", () => {
    vi.mocked(useRepositorySummary).mockReturnValue(state({ job: { data: { status: "FAILED", validationStatus: "REJECTED" }, isError: false, refetch: vi.fn() } }) as ReturnType<typeof useRepositorySummary>);
    render(<RepositorySummaryPanel analysisId="analysis-id" />);
    expect(screen.getByRole("alert")).toBeInTheDocument();
  });

  function artifact() {
    return { artifactId: "artifact-id", type: "REPOSITORY_SUMMARY", status: "VALIDATED", provenance: { skillMatrixId: "matrix-id", analysisId: "analysis-id", promptContextId: "context-id", templateVersion: "v2", provider: "OLLAMA", model: "qwen-test", contextHash: "a".repeat(64), generatedAt: "2026-09-11T00:00:00Z" }, validation: { status: "PASSED", validatorVersion: "repository-summary-validator-v1", validatedAt: "2026-09-11T00:00:00Z", violations: [] }, contentRef: "/api/v1/generated-artifacts/artifact-id", content: { summary: { text: "Grounded summary", evidenceReferences: ["evidence-id"], ruleOutputReferences: ["rule-output"] }, strengths: [], improvementAreas: [], technologies: ["Java"] } };
  }

  function state(overrides: Record<string, unknown> = {}) {
    return { request: { data: undefined, isPending: false, isError: false, mutate, reset: vi.fn() }, job: { data: undefined, isError: false, refetch: vi.fn() }, artifact: { data: undefined, isPending: false, isError: false, refetch: vi.fn() }, cancel: { isPending: false, mutate: vi.fn() }, ...overrides } as unknown as ReturnType<typeof useRepositorySummary>;
  }
});
