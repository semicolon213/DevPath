import { useId, type ReactNode } from "react";
import type { GenerationJob } from "../api/generationApi";

type Messages = {
  queued: string;
  running: string;
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
  requestError: boolean;
  jobError: boolean;
  artifactPending: boolean;
  artifactError: boolean;
  cancelPending: boolean;
  onRequest: () => void;
  onReset: () => void;
  onCancel: () => void;
  onRefetchJob: () => void;
  onRefetchArtifact: () => void;
  children?: ReactNode;
};

export function AiGeneratedPanel({
  title, description, actionLabel, messages, job, generatedAt, validationStatus,
  requestPending, requestError, jobError, artifactPending, artifactError, cancelPending,
  onRequest, onReset, onCancel, onRefetchJob, onRefetchArtifact, children
}: Props) {
  const id = useId();
  const badgeId = `${id}-badge`;
  const titleId = `${id}-title`;
  const status = validationStatus ?? job?.validationStatus ?? "PENDING";
  const hasArtifact = Boolean(generatedAt && validationStatus);

  return <section className="ai-generated-panel" aria-labelledby={`${badgeId} ${titleId}`}>
    <div className="section-heading ai-generated-panel__heading">
      <div>
        <span id={badgeId} className="ai-generated-panel__badge">AI 생성</span>
        <h2 id={titleId}>{title}</h2>
        <p>{description}</p>
      </div>
      {!job && <button type="button" disabled={requestPending} onClick={onRequest}>
        {requestPending ? "요청 중…" : actionLabel}
      </button>}
    </div>
    {hasArtifact ? <dl className="ai-generated-panel__metadata">
      <div><dt>생성 시각</dt><dd><time dateTime={generatedAt}>{formatDateTime(generatedAt!)}</time></dd></div>
      <div><dt>검증 상태</dt><dd>{validationLabel(status)}</dd></div>
    </dl> : status === "REJECTED" ? <dl className="ai-generated-panel__metadata">
      <div><dt>검증 상태</dt><dd>{validationLabel(status)}</dd></div>
    </dl> : !jobError && !artifactError ? <p className="ai-generated-panel__metadata">{generationStateLabel(job?.status)}</p> : null}

    {requestError && <Failure message={messages.failed} retry={onRequest} />}
    {job && (job.status === "QUEUED" || job.status === "RUNNING") && <div className="state-panel" role="status">
      <p>{job.status === "QUEUED" ? messages.queued : messages.running}</p>
      <button type="button" disabled={cancelPending} onClick={onCancel}>생성 취소</button>
    </div>}
    {job?.status === "CANCELED" && <div className="state-panel"><p>{messages.canceled}</p>
      <button type="button" onClick={onReset}>다시 시작</button></div>}
    {job?.status === "FAILED" && <Failure message={job.validationStatus === "REJECTED" ? messages.rejected : messages.failed} retry={onRequest} />}
    {jobError && <Failure message={messages.failed} retry={onRefetchJob} />}
    {artifactPending && job?.status === "SUCCEEDED" && job.artifactUrl && <p role="status">{messages.loading}</p>}
    {artifactError && <Failure message={messages.failed} retry={onRefetchArtifact} />}
    {children}
  </section>;
}

function Failure({ message, retry }: { message: string; retry: () => void }) {
  return <div className="state-panel" role="alert"><p>{message}</p><button type="button" onClick={retry}>다시 시도</button></div>;
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
