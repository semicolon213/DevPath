import { useRepositoryReview } from "../model/useRepositoryReview";

export function RepositoryReviewPanel({ analysisId }: { analysisId: string }) {
  const generation = useRepositoryReview(analysisId);
  const job = generation.job.data;

  return <section className="skill-explanation" aria-labelledby="repository-review-title">
    <div className="section-heading">
      <div>
        <p className="eyebrow">검증된 AI 리뷰</p>
        <h2 id="repository-review-title">저장소 리뷰</h2>
        <p>AI가 공식 점수를 바꾸지 않고, 이 분석에 연결된 Rule Engine 결과와 근거만 영역별로 설명합니다.</p>
      </div>
      {!job && <button type="button" disabled={generation.request.isPending}
        onClick={() => generation.request.mutate()}>
        {generation.request.isPending ? "요청 중" : "리뷰 생성"}
      </button>}
    </div>

    {generation.request.isError && <Failure retry={() => generation.request.mutate()} />}
    {job && ["QUEUED", "RUNNING"].includes(job.status) && <div className="state-panel" role="status">
      <p>{job.status === "QUEUED" ? "리뷰 생성을 기다리고 있습니다." : "근거를 바탕으로 리뷰를 생성하고 있습니다."}</p>
      <button type="button" disabled={generation.cancel.isPending} onClick={() => generation.cancel.mutate()}>
        생성 취소
      </button>
    </div>}
    {job?.status === "CANCELED" && <div className="state-panel"><p>리뷰 생성을 취소했습니다.</p>
      <button type="button" onClick={() => generation.request.reset()}>다시 시작</button></div>}
    {job?.status === "FAILED" && <Failure rejected={job.validationStatus === "REJECTED"}
      retry={() => generation.request.mutate()} />}
    {generation.job.isError && <Failure retry={() => generation.job.refetch()} />}
    {generation.artifact.isPending && artifactUrl(job) && <p role="status">검증된 리뷰를 불러오는 중입니다.</p>}
    {generation.artifact.isError && <Failure retry={() => generation.artifact.refetch()} />}
    {generation.artifact.data && <Review artifact={generation.artifact.data} />}
  </section>;
}

function Review({ artifact }: { artifact: NonNullable<ReturnType<typeof useRepositoryReview>["artifact"]["data"]> }) {
  return <article className="skill-explanation__result">
    <p>{artifact.content.summary}</p>
    <div className="category-result-grid">{artifact.content.sections.map(section => <section
      className="category-result-card" key={section.category}>
      <h3>{categoryLabel(section.category)}</h3>
      <p>{section.review}</p>
      {section.evidenceIds.length > 0 && <small>근거 {section.evidenceIds.join(", ")}</small>}
    </section>)}</div>
    <small>검증기 {artifact.validation.validatorVersion} · 프롬프트 {artifact.provenance.templateVersion}</small>
  </article>;
}

function Failure({ retry, rejected = false }: { retry: () => void; rejected?: boolean }) {
  return <div className="state-panel" role="alert">
    <p>{rejected ? "생성된 리뷰가 근거·안전성 검증을 통과하지 못했습니다." : "리뷰를 생성하지 못했습니다."}</p>
    <button type="button" onClick={retry}>다시 시도</button>
  </div>;
}

function artifactUrl(job: { status: string; artifactUrl: string | null } | undefined) {
  return job?.status === "SUCCEEDED" && Boolean(job.artifactUrl);
}

function categoryLabel(value: string) {
  return ({ ARCHITECTURE: "아키텍처", TESTING: "테스트", DEVOPS: "DevOps",
    DOCUMENTATION: "문서화", COLLABORATION: "협업" } as Record<string, string>)[value] ?? value;
}
