import { useSkillExplanation } from "../model/useSkillExplanation";
import { AiGeneratedPanel } from "./AiGeneratedPanel";
import { EvidenceReferences } from "./EvidenceReferences";
import { skillNameLabel } from "../../../shared/ui/labels";

export function SkillExplanationPanel({ skillMatrixId }: { skillMatrixId: string }) {
  const generation = useSkillExplanation(skillMatrixId);
  const artifact = generation.artifact.data;
  return <AiGeneratedPanel title="기술 분석 설명"
    description="공식 점수는 공식 분석 결과(Rule Engine) 그대로 유지하며, AI는 근거가 확인된 강점과 개선 영역만 설명합니다."
    actionLabel="설명 생성" messages={{ canceled: "설명 생성을 취소했습니다.", failed: "설명을 생성하지 못했습니다.", rejected: "생성된 설명이 근거·안전성 검증을 통과하지 못했습니다.", loading: "검증된 설명을 불러오는 중입니다." }}
    job={generation.job.data} generatedAt={artifact?.provenance.generatedAt} validationStatus={artifact?.validation.status}
    requestPending={generation.request.isPending} requestError={generation.request.error} jobError={generation.job.isError}
    artifactPending={generation.artifact.isPending} artifactError={generation.artifact.isError} cancelPending={generation.cancel.isPending}
    onRequest={() => generation.request.mutate()} onReset={() => generation.request.reset()} onCancel={() => generation.cancel.mutate()}
    onRefetchJob={() => { void generation.job.refetch(); }} onRefetchArtifact={() => { void generation.artifact.refetch(); }}>
    {artifact && <Explanation artifact={artifact} />}
  </AiGeneratedPanel>;
}

function Explanation({ artifact }: { artifact: NonNullable<ReturnType<typeof useSkillExplanation>["artifact"]["data"]> }) {
  return <article className="ai-generated-panel__result"><p>{artifact.content.summary}</p>
    <ExplanationList title="강점" items={artifact.content.strengths} analysisId={artifact.provenance.analysisId} />
    <ExplanationList title="개선 영역" items={artifact.content.improvementAreas} analysisId={artifact.provenance.analysisId} />
  </article>;
}

function ExplanationList({ title, items, analysisId }: { title: string; items: Array<{ skillKey: string; explanation: string; evidenceIds: string[] }>; analysisId?: string | null }) {
  if (!items.length) return null;
  return <section><h3>{title}</h3><ul>{items.map(item => <li key={item.skillKey}>
    <strong>{skillNameLabel(item.skillKey)}</strong><p>{item.explanation}</p><EvidenceReferences references={item.evidenceIds} analysisId={analysisId} />
  </li>)}</ul></section>;
}
