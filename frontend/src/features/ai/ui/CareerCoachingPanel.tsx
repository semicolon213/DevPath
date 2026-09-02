import { Link } from "react-router-dom";
import { useCareerCoaching } from "../model/useCareerCoaching";

export function CareerCoachingPanel({ careerReadinessId }: { careerReadinessId: string }) {
  const generation = useCareerCoaching(careerReadinessId);
  const job = generation.job.data;

  return <section className="skill-explanation" aria-labelledby="career-coaching-title">
    <div className="section-heading">
      <div>
        <p className="eyebrow">검증된 AI 코칭</p>
        <h2 id="career-coaching-title">커리어 코칭</h2>
        <p>공식 준비도와 추천 순서는 그대로 두고, 확인된 강점·격차와 다음 행동을 실천 관점에서 조언합니다.</p>
      </div>
      {!job && <button type="button" disabled={generation.request.isPending}
        onClick={() => generation.request.mutate()}>
        {generation.request.isPending ? "요청 중" : "코칭 생성"}
      </button>}
    </div>

    {generation.request.isError && <Failure retry={() => generation.request.mutate()} />}
    {job && ["QUEUED", "RUNNING"].includes(job.status) && <div className="state-panel" role="status">
      <p>{job.status === "QUEUED" ? "코칭 생성을 기다리고 있습니다." : "근거와 로드맵을 바탕으로 코칭을 생성하고 있습니다."}</p>
      <button type="button" disabled={generation.cancel.isPending} onClick={() => generation.cancel.mutate()}>
        생성 취소
      </button>
    </div>}
    {job?.status === "CANCELED" && <div className="state-panel"><p>코칭 생성을 취소했습니다.</p>
      <button type="button" onClick={() => generation.request.reset()}>다시 시작</button></div>}
    {job?.status === "FAILED" && <Failure rejected={job.validationStatus === "REJECTED"}
      retry={() => generation.request.mutate()} />}
    {generation.job.isError && <Failure retry={() => generation.job.refetch()} />}
    {generation.artifact.isPending && artifactUrl(job) && <p role="status">검증된 코칭을 불러오는 중입니다.</p>}
    {generation.artifact.isError && <Failure retry={() => generation.artifact.refetch()} />}
    {generation.artifact.data && <Advice artifact={generation.artifact.data} />}
  </section>;
}

function Advice({ artifact }: { artifact: NonNullable<ReturnType<typeof useCareerCoaching>["artifact"]["data"]> }) {
  return <article className="skill-explanation__result">
    <p>{artifact.content.readinessSummary}</p>
    <section><h3>활용할 강점</h3><p>{artifact.content.strengthExplanation}</p></section>
    <section><h3>집중할 격차</h3><p>{artifact.content.gapExplanation}</p></section>
    <section><h3>다음 행동</h3><ol>{artifact.content.nextSteps.map(step => <li key={step.recommendationId}>
      <p>{step.advice}</p><Link to={`/recommendations/${step.recommendationId}`}>공식 추천 근거 보기</Link>
    </li>)}</ol></section>
    <p><small>{artifact.content.confidenceNotice}</small></p>
    <small>검증기 {artifact.validation.validatorVersion} · 프롬프트 {artifact.provenance.templateVersion}</small>
  </article>;
}

function Failure({ retry, rejected = false }: { retry: () => void; rejected?: boolean }) {
  return <div className="state-panel" role="alert">
    <p>{rejected ? "생성된 코칭이 근거·안전성 검증을 통과하지 못했습니다." : "코칭을 생성하지 못했습니다."}</p>
    <button type="button" onClick={retry}>다시 시도</button>
  </div>;
}

function artifactUrl(job: { status: string; artifactUrl: string | null } | undefined) {
  return job?.status === "SUCCEEDED" && Boolean(job.artifactUrl);
}
