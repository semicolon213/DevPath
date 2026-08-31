import { useSkillExplanation } from "../model/useSkillExplanation";

export function SkillExplanationPanel({ skillMatrixId }: { skillMatrixId: string }) {
  const generation = useSkillExplanation(skillMatrixId);
  const job = generation.job.data;

  return <section className="skill-explanation" aria-labelledby="skill-explanation-title">
    <div className="section-heading">
      <div>
        <p className="eyebrow">검증된 AI 설명</p>
        <h2 id="skill-explanation-title">기술 분석 설명</h2>
        <p>공식 점수는 Rule Engine 결과 그대로 유지하며, AI는 근거가 확인된 강점과 개선 영역만 설명합니다.</p>
      </div>
      {!job && <button type="button" disabled={generation.request.isPending}
        onClick={() => generation.request.mutate()}>
        {generation.request.isPending ? "요청 중…" : "설명 생성"}
      </button>}
    </div>

    {generation.request.isError && <Failure retry={() => generation.request.mutate()} />}
    {job && ["QUEUED", "RUNNING"].includes(job.status) && <div className="state-panel" role="status">
      <p>{job.status === "QUEUED" ? "설명 생성을 기다리고 있습니다." : "근거를 바탕으로 설명을 생성하고 있습니다."}</p>
      <button type="button" disabled={generation.cancel.isPending} onClick={() => generation.cancel.mutate()}>
        생성 취소
      </button>
    </div>}
    {job?.status === "CANCELED" && <div className="state-panel"><p>설명 생성을 취소했습니다.</p>
      <button type="button" onClick={() => generation.request.reset()}>다시 시작</button></div>}
    {job?.status === "FAILED" && <Failure rejected={job.validationStatus === "REJECTED"}
      retry={() => generation.request.mutate()} />}
    {generation.job.isError && <Failure retry={() => generation.job.refetch()} />}
    {generation.artifact.isPending && artifactUrl(job) && <p role="status">검증된 설명을 불러오는 중입니다.</p>}
    {generation.artifact.isError && <Failure retry={() => generation.artifact.refetch()} />}
    {generation.artifact.data && <Explanation artifact={generation.artifact.data} />}
  </section>;
}

function Explanation({ artifact }: { artifact: NonNullable<ReturnType<typeof useSkillExplanation>["artifact"]["data"]> }) {
  return <article className="skill-explanation__result">
    <p>{artifact.content.summary}</p>
    <ExplanationList title="강점" items={artifact.content.strengths} />
    <ExplanationList title="개선 영역" items={artifact.content.improvementAreas} />
    <small>검증기 {artifact.validation.validatorVersion} · 프롬프트 {artifact.provenance.templateVersion}</small>
  </article>;
}

function ExplanationList({ title, items }: { title: string; items: Array<{ skillKey: string; explanation: string; evidenceIds: string[] }> }) {
  if (!items.length) return null;
  return <section><h3>{title}</h3><ul>{items.map(item => <li key={item.skillKey}>
    <strong>{item.skillKey}</strong><p>{item.explanation}</p>
    {item.evidenceIds.length > 0 && <small>근거 {item.evidenceIds.join(", ")}</small>}
  </li>)}</ul></section>;
}

function Failure({ retry, rejected = false }: { retry: () => void; rejected?: boolean }) {
  return <div className="state-panel" role="alert">
    <p>{rejected ? "생성된 설명이 근거·안전성 검증을 통과하지 못했습니다." : "설명을 생성하지 못했습니다."}</p>
    <button type="button" onClick={retry}>다시 시도</button>
  </div>;
}

function artifactUrl(job: { status: string; artifactUrl: string | null } | undefined) {
  return job?.status === "SUCCEEDED" && Boolean(job.artifactUrl);
}
