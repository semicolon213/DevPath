import { useRepositoryReview } from "../model/useRepositoryReview";
import { AiGeneratedPanel } from "./AiGeneratedPanel";
import { EvidenceFootnotes, EvidenceMarkers, type ResolvedEvidence } from "./EvidenceReferences";
import { categoryLabel } from "../../../shared/ui/labels";

export function RepositoryReviewPanel({ analysisId, evidence = [] }: { analysisId: string; evidence?: ResolvedEvidence }) {
  const generation = useRepositoryReview(analysisId);
  const artifact = generation.artifact.data;
  return <AiGeneratedPanel title="저장소 리뷰"
    description="AI가 공식 점수를 바꾸지 않고, 이 분석에 연결된 공식 분석 결과(Rule Engine)와 근거만 영역별로 설명합니다."
    actionLabel="리뷰 생성" messages={{ canceled: "리뷰 생성을 취소했습니다.", failed: "리뷰를 생성하지 못했습니다.", rejected: "생성된 리뷰가 근거·안전성 검증을 통과하지 못했습니다.", loading: "검증된 리뷰를 불러오는 중입니다." }}
    job={generation.job.data} generatedAt={artifact?.provenance.generatedAt} validationStatus={artifact?.validation.status}
    requestPending={generation.request.isPending} requestError={generation.request.error} jobError={generation.job.isError}
    artifactPending={generation.artifact.isPending} artifactError={generation.artifact.isError} cancelPending={generation.cancel.isPending}
    onRequest={() => generation.request.mutate()} onCancel={() => generation.cancel.mutate()}
    onRefetchJob={() => { void generation.job.refetch(); }} onRefetchArtifact={() => { void generation.artifact.refetch(); }}>
    {artifact && <Review artifact={artifact} evidence={evidence} />}
  </AiGeneratedPanel>;
}

function Review({ artifact, evidence }: { artifact: NonNullable<ReturnType<typeof useRepositoryReview>["artifact"]["data"]>; evidence: ResolvedEvidence }) {
  const references = [...new Set(artifact.content.sections.flatMap(section => section.evidenceIds))];
  return <article className="ai-generated-panel__result"><p>{artifact.content.summary}</p>
    <div className="ai-generated-panel__grid">{artifact.content.sections.map(section => <section key={section.category}>
      <h3>{categoryLabel(section.category)}</h3><p>{section.review}<EvidenceMarkers references={section.evidenceIds} allReferences={references} evidence={evidence} scope="repository-review" /></p>
    </section>)}</div>
    <EvidenceFootnotes references={references} evidence={evidence} scope="repository-review" regionLabel="저장소 리뷰 근거" analysisId={artifact.provenance.analysisId} />
  </article>;
}
