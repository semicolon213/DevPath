import { useId } from "react";
import type { SkillAssessment } from "../../features/skills/api/skillMatrixApi";
import { evidenceStatusLabel } from "./labels";
import { StatusChip } from "./StatusChip";

export function EvidenceStatus({ status, guidance = true }: { status: SkillAssessment["evidenceStatus"] | undefined; guidance?: boolean }) {
  const chipId = useId();
  if (status !== "NONE" && status !== "INSUFFICIENT") return null;
  // The note is named by its visible chip; the hidden prefix only gives the chip context.
  return <div className="evidence-status" role="note" aria-labelledby={chipId}>
    <StatusChip id={chipId} variant={status === "INSUFFICIENT" ? "warning" : "neutral"}><span className="visually-hidden">근거 상태: </span>{evidenceStatusLabel(status)}</StatusChip>
    {status === "NONE" && guidance ? <p>평가에 연결된 근거가 없습니다. 저장소 근거를 추가한 뒤 다시 분석해 주세요.</p> : null}
  </div>;
}
