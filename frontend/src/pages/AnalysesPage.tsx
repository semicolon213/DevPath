import { Link } from "react-router-dom";
import { useState } from "react";
import { useAnalysisHistory } from "../features/analysis/model/useAnalysis";
import { ApiError } from "../shared/api/apiClient";

export function AnalysesPage() {
  const query = useAnalysisHistory();
  const [selected,setSelected]=useState<string[]>([]);

  if (query.isPending) return <PageShell><p role="status">분석 이력을 불러오는 중입니다.</p></PageShell>;
  if (query.isError) return <PageShell><HistoryError error={query.error} retry={() => query.refetch()} /></PageShell>;

  const analyses = query.data.pages.flatMap(page => page.analyses);
  const totalCount = query.data.pages[0]?.totalCount ?? 0;
  return (
    <PageShell>
      <header className="workspace-header">
        <div>
          <p className="eyebrow">재현 가능한 분석 기록</p>
          <h1>분석 이력</h1>
          <p>저장소와 스냅샷, 규칙 버전이 고정된 Rule Engine의 공식 결과를 시간순으로 확인합니다.</p>
        </div>
        <Link className="button-link" to="/repositories">새 분석 준비</Link>
      </header>

      <section className="analysis-history-summary" aria-label="분석 이력 요약">
        <strong>{totalCount.toLocaleString("ko-KR")}</strong>
        <span>저장된 분석 결과</span>
      </section>

      {analyses.length>=2?<fieldset className="analysis-selection-panel"><legend>비교할 분석 선택</legend><p>완료 분석 두 개를 선택하면 저장된 공식 결과를 나란히 확인할 수 있습니다.</p><div><strong>{selected.length} / 2 선택</strong>{selected.length===2?<Link className="button-link" to={`/analyses/compare?analysisId=${selected[0]}&analysisId=${selected[1]}`}>선택한 결과 비교</Link>:<span>두 개를 선택해 주세요</span>}</div></fieldset>:null}

      {analyses.length === 0 ? (
        <div className="state-panel">
          <h2>아직 완료된 분석이 없습니다</h2>
          <p>저장소를 동기화하고 결정론적 분석을 실행하면 결과가 이력으로 보존됩니다.</p>
          <Link className="button-link" to="/repositories">저장소로 이동</Link>
        </div>
      ) : (
        <section aria-labelledby="analysis-history-list-title">
          <div className="section-heading">
            <div>
              <h2 id="analysis-history-list-title">완료된 분석</h2>
              <p>저장소별 가장 최근 결과만 현재 결과로 적용하며, 이전 결과는 변경하지 않고 이력으로 보존합니다.</p>
            </div>
          </div>
          <div className="analysis-history-list">
            {analyses.map(analysis => (
              <article className="analysis-history-card" key={analysis.analysisId}>
                <label className="analysis-compare-choice"><input type="checkbox" checked={selected.includes(analysis.analysisId)} disabled={!selected.includes(analysis.analysisId)&&selected.length===2} onChange={()=>setSelected(current=>current.includes(analysis.analysisId)?current.filter(id=>id!==analysis.analysisId):[...current,analysis.analysisId])}/><span>비교 대상으로 선택</span></label>
                <div className="analysis-history-card__main">
                  <div>
                    <span className={`status-badge${analysis.currentForRepository ? " status-badge--active" : ""}`}>
                      {analysis.currentForRepository ? "현재 적용" : "과거 결과"}
                    </span>
                    <h3>{analysis.repositoryFullName}</h3>
                    <time dateTime={analysis.completedAt}>{formatDateTime(analysis.completedAt)}</time>
                  </div>
                  <div className="official-score" aria-label={`공식 점수 ${formatScore(analysis.overallScore)}점`}>
                    <strong>{formatScore(analysis.overallScore)}</strong><span>/ 100</span>
                  </div>
                </div>
                <dl className="analysis-history-metadata">
                  <div><dt>근거 신뢰도</dt><dd>{formatScore(analysis.confidence)}%</dd></div>
                  <div><dt>규칙 버전</dt><dd>{analysis.ruleSetVersion}</dd></div>
                  <div><dt>역량 정책</dt><dd>{analysis.policyVersion}</dd></div>
                  <div><dt>스냅샷</dt><dd><code>{analysis.snapshotId.slice(0, 8)}</code></dd></div>
                </dl>
                <div className="analysis-history-card__actions">
                  <Link to={`/repositories/${analysis.repositoryId}`}>저장소 보기</Link>
                  <Link className="button-link" to={`/analyses/${analysis.analysisId}`}>결과 상세</Link>
                </div>
              </article>
            ))}
          </div>
          {query.hasNextPage ? (
            <button type="button" disabled={query.isFetchingNextPage} onClick={() => query.fetchNextPage()}>
              {query.isFetchingNextPage ? "불러오는 중…" : "이전 분석 더 보기"}
            </button>
          ) : null}
        </section>
      )}
    </PageShell>
  );
}

function HistoryError({ error, retry }: { error: Error; retry: () => void }) {
  if (error instanceof ApiError && error.status === 401) {
    return <div className="state-panel" role="alert"><h1>로그인이 필요합니다</h1><p>다시 로그인한 뒤 분석 이력을 확인해 주세요.</p><Link className="button-link" to="/">홈으로 이동</Link></div>;
  }
  return <div className="state-panel" role="alert"><h1>분석 이력을 불러오지 못했습니다</h1><p>기존 결과는 변경되지 않았습니다.</p><button type="button" onClick={retry}>다시 시도</button></div>;
}

function PageShell({ children }: { children: React.ReactNode }) {
  return <main className="shell workspace"><nav><Link to="/">← DevPath 홈</Link></nav>{children}</main>;
}

function formatScore(value: number) {
  return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value);
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "long", timeStyle: "short" }).format(new Date(value));
}
