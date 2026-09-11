import type { RepositorySummaryClaim } from "../api/generationApi";
import { useRepositorySummary } from "../model/useRepositorySummary";

export function RepositorySummaryPanel({ analysisId }: { analysisId: string }) {
  const generation = useRepositorySummary(analysisId);
  const job = generation.job.data;
  return <section className="skill-explanation" aria-labelledby="repository-summary-title">
    <div className="section-heading"><div><p className="eyebrow">검증된 AI 요약</p><h2 id="repository-summary-title">저장소 요약</h2>
      <p>제공된 분석 근거와 기술 목록만 사용하며 공식 점수를 계산하거나 변경하지 않습니다.</p></div>
      {!job && <button type="button" disabled={generation.request.isPending} onClick={() => generation.request.mutate()}>
        {generation.request.isPending ? "요청 중" : "요약 생성"}</button>}</div>
    {generation.request.isError && <Failure retry={() => generation.request.mutate()} />}
    {job && ["QUEUED", "RUNNING"].includes(job.status) && <div className="state-panel" role="status"><p>요약을 생성하고 있습니다.</p>
      <button type="button" disabled={generation.cancel.isPending} onClick={() => generation.cancel.mutate()}>생성 취소</button></div>}
    {job?.status === "CANCELED" && <div className="state-panel"><p>요약 생성을 취소했습니다.</p><button type="button" onClick={() => generation.request.reset()}>다시 시작</button></div>}
    {job?.status === "FAILED" && <Failure rejected={job.validationStatus === "REJECTED"} retry={() => generation.request.mutate()} />}
    {generation.job.isError && <Failure retry={() => generation.job.refetch()} />}
    {generation.artifact.isPending && job?.status === "SUCCEEDED" && job.artifactUrl && <p role="status">검증된 요약을 불러오는 중입니다.</p>}
    {generation.artifact.isError && <Failure retry={() => generation.artifact.refetch()} />}
    {generation.artifact.data && <article className="skill-explanation__result"><Claim claim={generation.artifact.data.content.summary} />
      <dl><div><dt>기술</dt><dd>{generation.artifact.data.content.technologies.join(", ") || "확인된 기술 없음"}</dd></div></dl>
      <div className="category-result-grid"><List title="강점" values={generation.artifact.data.content.strengths} />
        <List title="개선 영역" values={generation.artifact.data.content.improvementAreas} /></div>
      <small>검증기 {generation.artifact.data.validation.validatorVersion} · 프롬프트 {generation.artifact.data.provenance.templateVersion}</small></article>}
  </section>;
}

function List({ title, values }: { title: string; values: RepositorySummaryClaim[] }) {
  return <section className="category-result-card"><h3>{title}</h3>{values.length ? <ul>{values.map(value => <li key={`${value.text}:${value.evidenceReferences.join()}:${value.ruleOutputReferences.join()}`}><Claim claim={value} /></li>)}</ul> : <p>제공된 근거에서 확인된 항목이 없습니다.</p>}</section>;
}

function Claim({ claim }: { claim: RepositorySummaryClaim }) {
  return <div><p>{claim.text}</p><small>근거 {claim.evidenceReferences.join(", ") || "없음"} · 규칙 결과 {claim.ruleOutputReferences.join(", ") || "없음"}</small></div>;
}

function Failure({ retry, rejected = false }: { retry: () => void; rejected?: boolean }) {
  return <div className="state-panel" role="alert"><p>{rejected ? "생성된 요약이 근거 검증을 통과하지 못했습니다." : "요약을 생성하지 못했습니다."}</p><button type="button" onClick={retry}>다시 시도</button></div>;
}
