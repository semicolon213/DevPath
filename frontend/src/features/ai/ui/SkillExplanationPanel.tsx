import { useSkillExplanation } from "../model/useSkillExplanation";
import { AiGeneratedPanel } from "./AiGeneratedPanel";
import { EvidenceFootnotes, EvidenceMarkers, type ResolvedEvidence } from "./EvidenceReferences";
import { skillNameLabel } from "../../../shared/ui/labels";
import { useAnalysisDetail } from "../../analysis/model/useAnalysis";

export function SkillExplanationPanel({ skillMatrixId, evidence = [] }: { skillMatrixId: string; evidence?: ResolvedEvidence }) {
  const generation = useSkillExplanation(skillMatrixId);
  const artifact = generation.artifact.data;
  const analysis = useAnalysisDetail(artifact?.provenance.analysisId ?? undefined);
  const resolvedEvidence = [...evidence, ...(analysis.data?.evidence ?? [])];
  return <AiGeneratedPanel title="기술 분석 설명"
    description="공식 점수는 공식 분석 결과(Rule Engine) 그대로 유지하며, AI는 근거가 확인된 강점과 개선 영역만 설명합니다."
    actionLabel="설명 생성" messages={{ canceled: "설명 생성을 취소했습니다.", failed: "설명을 생성하지 못했습니다.", rejected: "생성된 설명이 근거·안전성 검증을 통과하지 못했습니다.", loading: "검증된 설명을 불러오는 중입니다." }}
    job={generation.job.data} generatedAt={artifact?.provenance.generatedAt} validationStatus={artifact?.validation.status}
    requestPending={generation.request.isPending} requestError={generation.request.error} jobError={generation.job.isError}
    artifactPending={generation.artifact.isPending} artifactError={generation.artifact.isError} cancelPending={generation.cancel.isPending}
    onRequest={() => generation.request.mutate()} onCancel={() => generation.cancel.mutate()}
    onRefetchJob={() => { void generation.job.refetch(); }} onRefetchArtifact={() => { void generation.artifact.refetch(); }}>
    {artifact && <Explanation artifact={artifact} evidence={resolvedEvidence} />}
  </AiGeneratedPanel>;
}

function Explanation({ artifact, evidence }: { artifact: NonNullable<ReturnType<typeof useSkillExplanation>["artifact"]["data"]>; evidence: ResolvedEvidence }) {
  const references = unique([...artifact.content.improvementAreas, ...artifact.content.strengths].flatMap(item => item.evidenceIds));
  return <article className="ai-generated-panel__result"><p>{artifact.content.summary}</p>
    <ExplanationList title="개선할 점" items={artifact.content.improvementAreas} references={references} evidence={evidence} />
    <ExplanationList title="강점" items={artifact.content.strengths} references={references} evidence={evidence} />
    <EvidenceFootnotes references={references} evidence={evidence} scope="skill-explanation" regionLabel="기술 역량 설명 근거" analysisId={artifact.provenance.analysisId} />
  </article>;
}

function ExplanationList({ title, items, references, evidence }: { title: string; items: Array<{ skillKey: string; explanation: string; evidenceIds: string[] }>; references: string[]; evidence: ResolvedEvidence }) {
  if (!items.length) return null;
  return <section><h3>{title}</h3><ul>{items.map(item => <li key={item.skillKey}>
    <strong>{skillNameLabel(item.skillKey)}</strong><p>{item.explanation}<EvidenceMarkers references={item.evidenceIds} allReferences={references} evidence={evidence} scope="skill-explanation" /></p>
  </li>)}</ul></section>;
}

function unique(values: string[]) { return [...new Set(values)]; }
