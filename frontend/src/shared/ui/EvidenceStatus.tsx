import type { SkillAssessment } from "../../features/skills/api/skillMatrixApi";
import { evidenceStatusLabel } from "./labels";

export function EvidenceStatus({ status }: { status: SkillAssessment["evidenceStatus"] | undefined }) {
  if (status !== "NONE" && status !== "INSUFFICIENT") return null;
  return <div className="evidence-status" role="note" aria-label={`근거 상태: ${evidenceStatusLabel(status)}`}>
    <span className="status-badge">{evidenceStatusLabel(status)}</span>
    {status === "NONE" ? <p>평가에 연결된 근거가 없습니다. 저장소 근거를 추가한 뒤 다시 분석해 주세요.</p> : null}
  </div>;
}
