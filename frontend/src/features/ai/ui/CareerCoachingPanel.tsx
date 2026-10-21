import { Link } from "react-router-dom";
import { useCareerCoaching } from "../model/useCareerCoaching";
import { AiGeneratedPanel } from "./AiGeneratedPanel";

export function CareerCoachingPanel({ careerReadinessId }: { careerReadinessId: string }) {
  const generation = useCareerCoaching(careerReadinessId);
  const artifact = generation.artifact.data;
  return <AiGeneratedPanel title="커리어 코칭" description="공식 준비도와 추천 우선순위는 그대로 두고, 확인된 강점과 격차를 바탕으로 다음 행동을 설명합니다."
    actionLabel="코칭 생성" messages={{ queued: "코칭 생성을 기다리고 있습니다.", running: "준비도 근거로 코칭을 생성하고 있습니다.", canceled: "코칭 생성을 취소했습니다.", failed: "코칭을 생성하지 못했습니다.", rejected: "생성된 코칭이 근거·안전성 검증을 통과하지 못했습니다.", loading: "검증된 코칭을 불러오는 중입니다." }}
    job={generation.job.data} generatedAt={artifact?.provenance.generatedAt} validationStatus={artifact?.validation.status}
    requestPending={generation.request.isPending} requestError={generation.request.error} jobError={generation.job.isError}
    artifactPending={generation.artifact.isPending} artifactError={generation.artifact.isError} cancelPending={generation.cancel.isPending}
    onRequest={() => generation.request.mutate()} onReset={() => generation.request.reset()} onCancel={() => generation.cancel.mutate()}
    onRefetchJob={() => { void generation.job.refetch(); }} onRefetchArtifact={() => { void generation.artifact.refetch(); }}>
    {artifact && <Advice artifact={artifact} />}
  </AiGeneratedPanel>;
}

function Advice({ artifact }: { artifact: NonNullable<ReturnType<typeof useCareerCoaching>["artifact"]["data"]> }) {
  return <article className="ai-generated-panel__result"><p>{artifact.content.readinessSummary}</p>
    <section><h3>활용할 강점</h3><p>{artifact.content.strengthExplanation}</p></section>
    <section><h3>집중할 격차</h3><p>{artifact.content.gapExplanation}</p></section>
    <section><h3>다음 행동</h3><ol>{artifact.content.nextSteps.map(step => <li key={step.recommendationId}><p>{step.advice}</p><Link to={`/recommendations/${step.recommendationId}`}>공식 추천 근거 보기</Link></li>)}</ol></section>
    <p><small>{artifact.content.confidenceNotice}</small></p>
  </article>;
}
