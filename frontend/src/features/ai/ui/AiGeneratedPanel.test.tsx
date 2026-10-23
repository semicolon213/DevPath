import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { GenerationJob } from "../api/generationApi";
import { AiGeneratedPanel } from "./AiGeneratedPanel";

describe("AiGeneratedPanel", () => {
  it.each([
    [undefined, "아직 생성하지 않았습니다."],
    ["QUEUED", "생성 요청이 대기 중입니다."],
    ["RUNNING", "AI 결과를 생성하고 있습니다."],
    ["CANCELED", "생성이 취소되었습니다."],
    ["FAILED", "생성에 실패했습니다."],
    ["SUCCEEDED", "생성 결과를 불러오고 있습니다."]
  ] as const)("renders state-appropriate metadata for %s", (status, metadata) => {
    render(<Panel job={status ? job(status) : undefined} />);
    expect(screen.getByText(metadata)).toBeInTheDocument();
    expect(screen.queryByText("생성 시각")).not.toBeInTheDocument();
    expect(screen.queryByText("검증 대기")).not.toBeInTheDocument();
  });

  it.each([
    ["QUEUED", "생성 요청이 대기 중입니다."],
    ["RUNNING", "AI 결과를 생성하고 있습니다."],
    ["CANCELED", "생성을 취소했습니다."],
    ["FAILED", "생성하지 못했습니다."]
  ] as const)("renders the %s job frame", (status, message) => {
    render(<Panel job={job(status)} />);
    expect(screen.getByText(message)).toBeInTheDocument();
  });

  it("distinguishes validation rejection from generation failure", () => {
    render(<Panel job={job("FAILED", "REJECTED")} />);
    expect(screen.getByRole("alert")).toHaveTextContent("검증을 통과하지 못했습니다.");
    expect(screen.getByText("검증 거부")).toBeInTheDocument();
    expect(screen.queryByText("생성 시각")).not.toBeInTheDocument();
  });

  it("cancels active generation", () => {
    const onCancel = vi.fn();
    render(<Panel job={job("RUNNING")} onCancel={onCancel} />);
    fireEvent.click(screen.getByRole("button", { name: "생성 취소" }));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it("shows the shared AI identity and validated result metadata without official-result styling", () => {
    const { container } = render(<Panel job={job("SUCCEEDED", "PASSED")} generatedAt="2026-09-02T00:00:00Z" validationStatus="PASSED"><p>검증된 결과</p></Panel>);
    expect(screen.getByText("AI 생성")).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "AI 생성 AI 설명" })).toBeInTheDocument();
    expect(screen.getByText("생성 시각").nextElementSibling).not.toHaveTextContent("생성 전");
    expect(container.querySelector("time")).toHaveAttribute("datetime", "2026-09-02T00:00:00Z");
    expect(screen.getByText("검증 통과")).toBeInTheDocument();
    expect(screen.getByText("검증된 결과")).toBeInTheDocument();
    expect(container.querySelector(".category-result-card")).toBeNull();
  });

  it.each([
    ["requestError", "request"],
    ["jobError", "job"],
    ["artifactError", "artifact"]
  ] as const)("retries the %s path", (error, retry) => {
    const onRequest = vi.fn();
    const onRefetchJob = vi.fn();
    const onRefetchArtifact = vi.fn();
    render(<Panel {...{ [error]: true }} onRequest={onRequest} onRefetchJob={onRefetchJob} onRefetchArtifact={onRefetchArtifact} />);
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    expect(retry === "request" ? onRequest : retry === "job" ? onRefetchJob : onRefetchArtifact).toHaveBeenCalledOnce();
  });

  it("does not show contradictory metadata beside loading failures", () => {
    const { rerender } = render(<Panel job={job("SUCCEEDED")} artifactError />);
    expect(screen.queryByText("생성 결과를 불러오고 있습니다.")).not.toBeInTheDocument();

    rerender(<Panel jobError />);
    expect(screen.queryByText("아직 생성하지 않았습니다.")).not.toBeInTheDocument();
  });
});

const messages = {
  queued: "생성을 기다리고 있습니다.", running: "근거로 생성하고 있습니다.", canceled: "생성을 취소했습니다.",
  failed: "생성하지 못했습니다.", rejected: "검증을 통과하지 못했습니다.", loading: "결과를 불러오는 중입니다."
};

function Panel({ job: value, onCancel = vi.fn(), onRequest = vi.fn(), onRefetchJob = vi.fn(), onRefetchArtifact = vi.fn(), children, generatedAt, validationStatus,
  requestError = false, jobError = false, artifactError = false }: {
  job?: GenerationJob;
  onCancel?: () => void;
  onRequest?: () => void;
  onRefetchJob?: () => void;
  onRefetchArtifact?: () => void;
  children?: React.ReactNode;
  generatedAt?: string;
  validationStatus?: "PENDING" | "PASSED" | "REJECTED";
  requestError?: boolean;
  jobError?: boolean;
  artifactError?: boolean;
}) {
  return <AiGeneratedPanel title="AI 설명" description="공식 결과와 분리된 설명입니다." actionLabel="설명 생성" messages={messages}
    job={value} generatedAt={generatedAt} validationStatus={validationStatus}
    requestPending={false} requestError={requestError} jobError={jobError} artifactPending={false} artifactError={artifactError} cancelPending={false}
    onRequest={onRequest} onReset={vi.fn()} onCancel={onCancel} onRefetchJob={onRefetchJob} onRefetchArtifact={onRefetchArtifact}>{children}</AiGeneratedPanel>;
}

function job(status: GenerationJob["status"], validationStatus: GenerationJob["validationStatus"] = "PENDING"): GenerationJob {
  return { jobId: "job-id", status, validationStatus, artifactUrl: status === "SUCCEEDED" ? "/artifact" : null, failureCode: null,
    createdAt: "2026-09-02T00:00:00Z", completedAt: ["SUCCEEDED", "FAILED", "CANCELED"].includes(status) ? "2026-09-02T00:01:00Z" : null };
}
