import { Link } from "react-router-dom";
import { categoryLabel, ruleLabel } from "../../../shared/ui/labels";
import type { RepositorySummaryClaim } from "../api/generationApi";
import { useRepositorySummary } from "../model/useRepositorySummary";
import { AiGeneratedPanel } from "./AiGeneratedPanel";
import { EvidenceFootnotes, EvidenceMarkers, type ResolvedEvidence } from "./EvidenceReferences";

export function RepositorySummaryPanel({ analysisId, evidence = [] }: { analysisId: string; evidence?: ResolvedEvidence }) {
  const generation = useRepositorySummary(analysisId);
  const artifact = generation.artifact.data;
  return <AiGeneratedPanel title="저장소 요약" description="제공된 분석 근거와 기술 목록만 사용하며 공식 점수를 계산하거나 변경하지 않습니다."
    actionLabel="요약 생성" messages={{ canceled: "요약 생성을 취소했습니다.", failed: "요약을 생성하지 못했습니다.", rejected: "생성된 요약이 근거 검증을 통과하지 못했습니다.", loading: "검증된 요약을 불러오는 중입니다." }}
    job={generation.job.data} generatedAt={artifact?.provenance.generatedAt} validationStatus={artifact?.validation.status}
    requestPending={generation.request.isPending} requestError={generation.request.error} jobError={generation.job.isError}
    artifactPending={generation.artifact.isPending} artifactError={generation.artifact.isError} cancelPending={generation.cancel.isPending}
    onRequest={() => generation.request.mutate()} onCancel={() => generation.cancel.mutate()}
    onRefetchJob={() => { void generation.job.refetch(); }} onRefetchArtifact={() => { void generation.artifact.refetch(); }}>
    {artifact && <Summary artifact={artifact} evidence={evidence} />}
  </AiGeneratedPanel>;
}

function Summary({ artifact, evidence }: { artifact: NonNullable<ReturnType<typeof useRepositorySummary>["artifact"]["data"]>; evidence: ResolvedEvidence }) {
  const claims = [artifact.content.summary, ...artifact.content.improvementAreas, ...artifact.content.strengths];
  const references = [...new Set(claims.flatMap(claim => claim.evidenceReferences))];
  const analysisId = artifact.provenance.analysisId;
  return <article className="ai-generated-panel__result"><Claim claim={artifact.content.summary} references={references} evidence={evidence} analysisId={analysisId} />
    <dl><div><dt>기술</dt><dd>{artifact.content.technologies.join(", ") || "확인된 기술 없음"}</dd></div></dl>
    <div className="ai-generated-panel__grid"><List title="개선할 점" values={artifact.content.improvementAreas} references={references} evidence={evidence} analysisId={analysisId} />
      <List title="강점" values={artifact.content.strengths} references={references} evidence={evidence} analysisId={analysisId} /></div>
    <EvidenceFootnotes references={references} evidence={evidence} scope="repository-summary" regionLabel="저장소 요약 근거" analysisId={artifact.provenance.analysisId} />
  </article>;
}

function List({ title, values, references, evidence, analysisId }: { title: string; values: RepositorySummaryClaim[]; references: string[]; evidence: ResolvedEvidence; analysisId: string }) {
  return <section><h3>{title}</h3>{values.length ? <ul>{values.map(value => <li key={`${value.text}:${value.evidenceReferences.join()}:${value.ruleOutputReferences.join()}`}><Claim claim={value} references={references} evidence={evidence} analysisId={analysisId} /></li>)}</ul> : <p>제공된 근거에서 확인된 항목이 없습니다.</p>}</section>;
}

function Claim({ claim, references, evidence, analysisId }: { claim: RepositorySummaryClaim; references: string[]; evidence: ResolvedEvidence; analysisId: string }) {
  return <div><p>{claim.text}<EvidenceMarkers references={claim.evidenceReferences} allReferences={references} evidence={evidence} scope="repository-summary" /></p>
    {claim.ruleOutputReferences.length ? <p><small>규칙 결과{ruleOutputLabels(claim.ruleOutputReferences)} · <Link to={`/analyses/${analysisId}#category-results-title`}>영역별 공식 결과</Link></small></p> : null}</div>;
}

// Rule output references are "<evaluationId>:<CATEGORY>:<RULE_ID>:<version>"; only the human labels are shown, never the id.
function ruleOutputLabels(references: string[]) {
  const labels = [...new Set(references.flatMap(reference => {
    const [, category, ruleId] = reference.split(":");
    return category && ruleId ? [`${categoryLabel(category)} · ${ruleLabel(ruleId)}`] : [];
  }))];
  return labels.length ? `: ${labels.join(", ")}` : "";
}
