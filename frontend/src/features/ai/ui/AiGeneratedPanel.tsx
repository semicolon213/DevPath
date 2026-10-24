import { useEffect, useId, useState, type ReactNode } from "react";
import type { GenerationJob } from "../api/generationApi";
import { mutationErrorMessage } from "../../../shared/api/apiClient";

type Messages = {
  canceled: string;
  failed: string;
  rejected: string;
  loading: string;
};

type Props = {
  title: string;
  description: string;
  actionLabel: string;
  messages: Messages;
  job?: GenerationJob;
  generatedAt?: string;
  validationStatus?: "PENDING" | "PASSED" | "REJECTED";
  requestPending: boolean;
  requestError: unknown;
  jobError: boolean;
  artifactPending: boolean;
  artifactError: boolean;
  cancelPending: boolean;
  onRequest: () => void;
  onCancel: () => void;
  onRefetchJob: () => void;
  onRefetchArtifact: () => void;
  children?: ReactNode;
};

export function AiGeneratedPanel({
  title, description, actionLabel, messages, job, generatedAt, validationStatus,
  requestPending, requestError, jobError, artifactPending, artifactError, cancelPending,
  onRequest, onCancel, onRefetchJob, onRefetchArtifact, children
}: Props) {
  const id = useId();
  const badgeId = `${id}-badge`;
  const titleId = `${id}-title`;
  const status = validationStatus ?? job?.validationStatus ?? "PENDING";
  const hasArtifact = Boolean(generatedAt && validationStatus);
  const active = job?.status === "QUEUED" || job?.status === "RUNNING";

  return <section className="ai-generated-panel" aria-labelledby={`${badgeId} ${titleId}`}>
    <div className="ai-generated-panel__toolbar">
      <span id={badgeId} className="ai-generated-panel__badge">AI 생성 · 설명만 제공</span>
      {active ? <button className="button-ai" type="button" disabled={cancelPending} onClick={onCancel}>생성 취소</button>
        : <button className="button-ai" type="button" disabled={requestPending} onClick={onRequest}>{requestPending ? "요청 중…" : hasArtifact ? "다시 생성" : actionLabel}</button>}
    </div>
    <h2 id={titleId}>{title}{job?.status === "QUEUED" ? " · 대기 중" : job?.status === "RUNNING" ? " · 생성 중" : ""}</h2>
    <p className="ai-generated-panel__description">{description}</p>
    {!hasArtifact && !jobError && !artifactError && !active ? <p className="ai-generated-panel__state">{generationStateLabel(job?.status)}</p> : null}

    {Boolean(requestError) && <Failure message={mutationErrorMessage(requestError, messages.failed)} retry={onRequest} />}
    {job && active && <GenerationProgress job={job} />}
    {job?.status === "CANCELED" && <div className="state-panel"><p>{messages.canceled}</p></div>}
    {job?.status === "FAILED" && <Failure message={job.validationStatus === "REJECTED" ? messages.rejected : messages.failed} retry={onRequest} />}
    {jobError && <Failure message={messages.failed} retry={onRefetchJob} />}
    {artifactPending && job?.status === "SUCCEEDED" && job.artifactUrl && <p role="status">{messages.loading}</p>}
    {artifactError && <Failure message={messages.failed} retry={onRefetchArtifact} />}
    {children}
    {hasArtifact ? <p className="ai-generated-panel__metadata">생성 <time dateTime={generatedAt}>{formatDateTime(generatedAt!)}</time> · {validationLabel(status)} · 공식 결과는 바꾸지 않습니다</p>
      : status === "REJECTED" ? <p className="ai-generated-panel__metadata">{validationLabel(status)}</p> : null}
  </section>;
}

function GenerationProgress({ job }: { job: GenerationJob }) {
  const [trackedAt] = useState(Date.now());
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);
  const createdAt = Date.parse(job.createdAt);
  const started = Number.isNaN(createdAt) ? trackedAt : createdAt;
  return <div className="ai-generated-panel__progress" role="status">
    <p className="ai-generated-panel__phase">{generationStateLabel(job.status)}</p>
    <p className="ai-generated-panel__elapsed"><strong>{formatElapsed(now - started)}</strong><span>경과 · 보통 1분 정도 걸립니다</span></p>
    <p>페이지를 떠나도 서버에서 계속 진행되며, 돌아오면 결과를 이어서 보여 줍니다.</p>
  </div>;
}

function Failure({ message, retry }: { message: string; retry: () => void }) {
  return <div className="state-panel" role="alert"><p>{message}</p><button className="button-ai" type="button" onClick={retry}>다시 시도</button></div>;
}

function validationLabel(status: "PENDING" | "PASSED" | "REJECTED") {
  return { PENDING: "검증 대기", PASSED: "검증 통과", REJECTED: "검증 거부" }[status];
}

function generationStateLabel(status?: GenerationJob["status"]) {
  if (!status) return "아직 생성하지 않았습니다.";
  return {
    QUEUED: "생성 요청이 대기 중입니다.",
    RUNNING: "AI 결과를 생성하고 있습니다.",
    SUCCEEDED: "생성 결과를 불러오고 있습니다.",
    FAILED: "생성에 실패했습니다.",
    CANCELED: "생성이 취소되었습니다."
  }[status];
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function formatElapsed(milliseconds: number) {
  const seconds = Math.max(0, Math.floor(milliseconds / 1000));
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}
