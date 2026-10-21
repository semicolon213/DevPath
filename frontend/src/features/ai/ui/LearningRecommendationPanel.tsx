import { useLearningRecommendation } from "../model/useLearningRecommendation";
import { AiGeneratedPanel } from "./AiGeneratedPanel";
import { EvidenceReferences } from "./EvidenceReferences";

export function LearningRecommendationPanel({ learningRoadmapId }: { learningRoadmapId: string }) {
  const generation = useLearningRecommendation(learningRoadmapId);
  const artifact = generation.artifact.data;
  return <AiGeneratedPanel title="선택적 AI 학습 가이드" description="공식 점수나 로드맵 순서를 변경하지 않고, 각 단계를 왜 학습하고 무엇을 연습할지 설명합니다."
    actionLabel="가이드 생성" messages={{ queued: "학습 가이드 생성을 기다리고 있습니다.", running: "로드맵 근거로 학습 가이드를 생성하고 있습니다.", canceled: "학습 가이드 생성을 취소했습니다.", failed: "학습 가이드를 생성하지 못했습니다.", rejected: "생성된 가이드가 근거·안전성 검증을 통과하지 못했습니다.", loading: "검증된 학습 가이드를 불러오는 중입니다." }}
    job={generation.job.data} generatedAt={artifact?.provenance.generatedAt} validationStatus={artifact?.validation.status}
    requestPending={generation.request.isPending} requestError={generation.request.error} jobError={generation.job.isError}
    artifactPending={generation.artifact.isPending} artifactError={generation.artifact.isError} cancelPending={generation.cancel.isPending}
    onRequest={() => generation.request.mutate()} onReset={() => generation.request.reset()} onCancel={() => generation.cancel.mutate()}
    onRefetchJob={() => { void generation.job.refetch(); }} onRefetchArtifact={() => { void generation.artifact.refetch(); }}>
    {artifact && <Guide artifact={artifact} />}
  </AiGeneratedPanel>;
}

function Guide({ artifact }: { artifact: NonNullable<ReturnType<typeof useLearningRecommendation>["artifact"]["data"]> }) {
  return <article className="ai-generated-panel__result"><p>{artifact.content.recommendationSummary}</p><ol>{artifact.content.steps.map(step => <li key={step.roadmapStepId}>
    <h3>{step.milestoneExplanation}</h3><p><strong>학습 목표</strong> {step.objective}</p><p><strong>연습 과제</strong> {step.practiceTask}</p>
    <p><strong>완료 기준</strong> {step.completionCriteria}</p><EvidenceReferences references={step.expectedEvidence} label="필요 근거" showReference />
  </li>)}</ol></article>;
}
