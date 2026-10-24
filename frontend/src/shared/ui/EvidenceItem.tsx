import { Link } from "react-router-dom";
import { evidenceTypeLabel } from "./labels";
import { StatusChip } from "./StatusChip";

export type EvidenceItemValue = {
  evidenceId?: string;
  evidenceType?: string;
  sourceReference: string;
  observedFactSummary?: string;
  confidence?: number;
  meta?: string;
};

export function EvidenceItem({ evidence, variant = "list", number, linkTo, omitId = false }: {
  evidence: EvidenceItemValue;
  variant?: "list" | "footnote";
  number?: number;
  linkTo?: string;
  omitId?: boolean;
}) {
  const type = evidence.evidenceType ? evidenceTypeLabel(evidence.evidenceType) : null;
  const id = omitId ? undefined : evidence.evidenceId;
  const content = <>
    {number ? <strong className="evidence-item__number">[{number}]</strong> : null}
    {type ? <StatusChip variant={type === "알 수 없음" ? "neutral" : "info"}>{type}</StatusChip> : <span className="evidence-item__label">근거 ID</span>}
    <code>{evidence.sourceReference}</code>
    {evidence.confidence == null ? null : <small className="evidence-item__confidence">신뢰도 <strong>{format(evidence.confidence)}%</strong></small>}
    {evidence.observedFactSummary ? <span className="evidence-item__fact">{evidence.observedFactSummary}</span> : null}
    {evidence.meta ? <small className="evidence-item__meta">{evidence.meta}</small> : null}
    {linkTo ? <Link to={linkTo} aria-label={`근거 보기: ${evidence.sourceReference}`}>근거 보기</Link> : null}
  </>;

  return variant === "footnote"
    ? <li className="evidence-item evidence-item--footnote" id={id ? `footnote-${id}` : undefined}>{content}</li>
    : <article className="evidence-item" id={id ? `evidence-${id}` : undefined} tabIndex={id ? -1 : undefined}>{content}</article>;
}

export function EvidenceEmptyState({ description, to, linkLabel }: { description: string; to: string; linkLabel: string }) {
  return <div className="evidence-empty"><strong>연결된 근거가 없습니다</strong><span>{description}</span><Link to={to}>{linkLabel}</Link></div>;
}

function format(value: number) {
  return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value);
}
