import { useLearningRecommendation } from "../model/useLearningRecommendation";

export function LearningRecommendationPanel({ learningRoadmapId }: { learningRoadmapId: string }) {
  const generation = useLearningRecommendation(learningRoadmapId);
  const job = generation.job.data;

  return <section className="skill-explanation" aria-labelledby="learning-recommendation-title">
    <div className="section-heading">
      <div>
        <p className="eyebrow">검증된 AI 학습 가이드</p>
        <h2 id="learning-recommendation-title">로드맵 실행 가이드</h2>
        <p>공식 순서와 완료 기준은 그대로 두고, 각 단계를 왜 학습하고 무엇을 연습할지 설명합니다.</p>
      </div>
      {!job && <button type="button" disabled={generation.request.isPending}
        onClick={() => generation.request.mutate()}>
        {generation.request.isPending ? "요청 중" : "가이드 생성"}
      </button>}
    </div>

    {generation.request.isError && <Failure retry={() => generation.request.mutate()} />}
    {job && ["QUEUED", "RUNNING"].includes(job.status) && <div className="state-panel" role="status">
      <p>{job.status === "QUEUED" ? "학습 가이드 생성을 기다리고 있습니다." : "로드맵 근거로 학습 가이드를 생성하고 있습니다."}</p>
      <button type="button" disabled={generation.cancel.isPending} onClick={() => generation.cancel.mutate()}>
        생성 취소
      </button>
    </div>}
    {job?.status === "CANCELED" && <div className="state-panel"><p>학습 가이드 생성을 취소했습니다.</p>
      <button type="button" onClick={() => generation.request.reset()}>다시 시작</button></div>}
    {job?.status === "FAILED" && <Failure rejected={job.validationStatus === "REJECTED"}
      retry={() => generation.request.mutate()} />}
    {generation.job.isError && <Failure retry={() => generation.job.refetch()} />}
    {generation.artifact.isPending && artifactUrl(job) && <p role="status">검증된 학습 가이드를 불러오는 중입니다.</p>}
    {generation.artifact.isError && <Failure retry={() => generation.artifact.refetch()} />}
    {generation.artifact.data && <Guide artifact={generation.artifact.data} />}
  </section>;
}

function Guide({ artifact }: { artifact: NonNullable<ReturnType<typeof useLearningRecommendation>["artifact"]["data"]> }) {
  return <article className="skill-explanation__result">
    <p>{artifact.content.recommendationSummary}</p>
    <ol>{artifact.content.steps.map(step => <li key={step.roadmapStepId}>
      <h3>{step.milestoneExplanation}</h3>
      <p><strong>학습 목표</strong> {step.objective}</p>
      <p><strong>연습 과제</strong> {step.practiceTask}</p>
      <p><strong>완료 기준</strong> {step.completionCriteria}</p>
      {step.expectedEvidence.length > 0 && <small>필요 근거: {step.expectedEvidence.join(", ")}</small>}
    </li>)}</ol>
    <small>검증기 {artifact.validation.validatorVersion} · 프롬프트 {artifact.provenance.templateVersion}</small>
  </article>;
}

function Failure({ retry, rejected = false }: { retry: () => void; rejected?: boolean }) {
  return <div className="state-panel" role="alert">
    <p>{rejected ? "생성된 가이드가 근거·안전성 검증을 통과하지 못했습니다." : "학습 가이드를 생성하지 못했습니다."}</p>
    <button type="button" onClick={retry}>다시 시도</button>
  </div>;
}

function artifactUrl(job: { status: string; artifactUrl: string | null } | undefined) {
  return job?.status === "SUCCEEDED" && Boolean(job.artifactUrl);
}
