import type { RepositorySummaryClaim } from "../api/generationApi";
import { useRepositorySummary } from "../model/useRepositorySummary";
import { AiGeneratedPanel } from "./AiGeneratedPanel";
import { EvidenceReferences } from "./EvidenceReferences";

export function RepositorySummaryPanel({ analysisId }: { analysisId: string }) {
  const generation = useRepositorySummary(analysisId);
  const artifact = generation.artifact.data;
  return <AiGeneratedPanel title="저장소 요약" description="제공된 분석 근거와 기술 목록만 사용하며 공식 점수를 계산하거나 변경하지 않습니다."
    actionLabel="요약 생성" messages={{ queued: "요약 생성을 기다리고 있습니다.", running: "요약을 생성하고 있습니다.", canceled: "요약 생성을 취소했습니다.", failed: "요약을 생성하지 못했습니다.", rejected: "생성된 요약이 근거 검증을 통과하지 못했습니다.", loading: "검증된 요약을 불러오는 중입니다." }}
    job={generation.job.data} generatedAt={artifact?.provenance.generatedAt} validationStatus={artifact?.validation.status}
    requestPending={generation.request.isPending} requestError={generation.request.isError} jobError={generation.job.isError}
    artifactPending={generation.artifact.isPending} artifactError={generation.artifact.isError} cancelPending={generation.cancel.isPending}
    onRequest={() => generation.request.mutate()} onReset={() => generation.request.reset()} onCancel={() => generation.cancel.mutate()}
    onRefetchJob={() => { void generation.job.refetch(); }} onRefetchArtifact={() => { void generation.artifact.refetch(); }}>
    {artifact && <article className="ai-generated-panel__result"><Claim claim={artifact.content.summary} analysisId={artifact.provenance.analysisId} />
      <dl><div><dt>기술</dt><dd>{artifact.content.technologies.join(", ") || "확인된 기술 없음"}</dd></div></dl>
      <div className="ai-generated-panel__grid"><List title="강점" values={artifact.content.strengths} analysisId={artifact.provenance.analysisId} />
        <List title="개선 영역" values={artifact.content.improvementAreas} analysisId={artifact.provenance.analysisId} /></div>
    </article>}
  </AiGeneratedPanel>;
}

function List({ title, values, analysisId }: { title: string; values: RepositorySummaryClaim[]; analysisId: string }) {
  return <section><h3>{title}</h3>{values.length ? <ul>{values.map(value => <li key={`${value.text}:${value.evidenceReferences.join()}:${value.ruleOutputReferences.join()}`}><Claim claim={value} analysisId={analysisId} /></li>)}</ul> : <p>제공된 근거에서 확인된 항목이 없습니다.</p>}</section>;
}

function Claim({ claim, analysisId }: { claim: RepositorySummaryClaim; analysisId: string }) {
  return <div><p>{claim.text}</p><EvidenceReferences references={claim.evidenceReferences} analysisId={analysisId} />
    <EvidenceReferences references={claim.ruleOutputReferences} analysisId={analysisId} label="규칙 결과" target="rules" /></div>;
}
