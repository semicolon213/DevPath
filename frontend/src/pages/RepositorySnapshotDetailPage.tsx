import { Link, useParams } from "react-router-dom";
import type { RepositorySnapshot } from "../features/repositories/api/repositoryApi";
import { useRepository, useRepositorySnapshot } from "../features/repositories/model/useRepositories";
import type { AnalysisHistoryItem } from "../features/analysis/api/analysisApi";
import { useRepositoryAnalysisHistory } from "../features/analysis/model/useAnalysis";
import { ApiError } from "../shared/api/apiClient";

export function RepositorySnapshotDetailPage() {
  const { repositoryId, snapshotId } = useParams();
  const repository = useRepository(repositoryId);
  const snapshot = useRepositorySnapshot(repositoryId, snapshotId);
  const analyses = useRepositoryAnalysisHistory(repositoryId);
  if (repository.isPending || snapshot.isPending) return <Shell repositoryId={repositoryId}><p role="status">불변 스냅샷 상세를 불러오는 중입니다.</p></Shell>;
  if (repository.isError || snapshot.isError) return <Shell repositoryId={repositoryId}><SnapshotError error={snapshot.error ?? repository.error} /></Shell>;

  const value = snapshot.data;
  const current = repository.data.currentSnapshotId === value.snapshotId;
  return <Shell repositoryId={repositoryId}>
    <header className="workspace-header snapshot-detail-header"><div><p className="eyebrow">Immutable Repository Snapshot</p>
      <h1>{repository.data.fullName} 스냅샷</h1><p>한 번 완료된 동기화 결과의 출처와 수집 범위를 변경 없이 확인합니다.</p></div>
      <div className="analysis-version" aria-label="스냅샷 적용 상태"><span>{current ? "현재 분석 기준" : "과거 기록"}</span><span>{snapshotStatusLabel(value.status)}</span></div>
    </header>

    <section className="analysis-notice"><div><h2>불변성 보장</h2><p>이 화면은 서버에 저장된 스냅샷 메타데이터만 표시합니다. 과거 스냅샷을 현재 결과로 바꾸지 않습니다.</p></div>
      <span className={`status-badge${value.immutable ? " status-badge--active" : ""}`}>{value.immutable ? "불변" : "검증 필요"}</span></section>

    <section aria-labelledby="snapshot-provenance-title"><div className="section-heading"><div><h2 id="snapshot-provenance-title">출처와 무결성</h2>
      <p>분석 결과를 재현할 때 참조하는 전체 리비전과 콘텐츠 해시입니다.</p></div></div>
      <dl className="snapshot-provenance">
        <Fact label="스냅샷 ID" value={value.snapshotId} code />
        <Fact label="소스 리비전" value={value.sourceRevision} code />
        <Fact label="콘텐츠 해시" value={value.contentHash} code />
        <Fact label="수집 시각" value={formatDate(value.capturedAt)} />
      </dl>
    </section>

    <section aria-labelledby="snapshot-collection-title"><div className="section-heading"><div><h2 id="snapshot-collection-title">수집 범위</h2>
      <p>이 스냅샷에 실제 저장된 공급자 메타데이터 건수입니다. 점수나 품질 판정이 아닙니다.</p></div></div>
      <div className="snapshot-count-grid"><Count label="브랜치" value={value.branchCount} /><Count label="커밋" value={value.commitCount} />
        <Count label="Pull Request" value={value.pullRequestCount} /><Count label="이슈" value={value.issueCount} /><Count label="문서" value={value.documentCount} /></div>
    </section>

    <SnapshotAnalysisHistory snapshotId={value.snapshotId} query={analyses} />

    {!current ? <section className="state-panel state-panel--compact"><h2>과거 스냅샷입니다.</h2>
      <p>기술·엔지니어링 증거 화면은 현재 스냅샷만 제공합니다. 이 기록의 공식 결과는 분석 이력에서 확인해 주세요.</p></section> : null}
    <div className="workspace-actions"><Link to={`/repositories/${repositoryId}`}>저장소 상세</Link><Link to="/analyses">분석 이력</Link></div>
  </Shell>;
}

type RepositoryAnalysisHistoryQuery = ReturnType<typeof useRepositoryAnalysisHistory>;

function SnapshotAnalysisHistory({ snapshotId, query }: { snapshotId: string; query: RepositoryAnalysisHistoryQuery }) {
  if (query.isPending) return <section className="snapshot-analysis-history" aria-labelledby="snapshot-analysis-title"><AnalysisHeading />
    <p role="status">이 스냅샷을 사용한 분석을 불러오는 중입니다.</p></section>;
  if (query.isError) return <section className="snapshot-analysis-history" aria-labelledby="snapshot-analysis-title"><AnalysisHeading />
    <div className="state-panel state-panel--compact" role="alert"><h3>연결된 분석만 불러오지 못했습니다.</h3>
      <p>스냅샷 출처와 수집 범위는 계속 확인할 수 있습니다.</p><button type="button" onClick={() => query.refetch()}>분석 이력 다시 시도</button></div></section>;
  const matches = query.data.pages.flatMap(page => page.analyses).filter(item => item.snapshotId === snapshotId);
  return <section className="snapshot-analysis-history" aria-labelledby="snapshot-analysis-title"><AnalysisHeading />
    {matches.length === 0 ? <div className="state-panel state-panel--compact"><h3>현재 불러온 이력에는 연결된 분석이 없습니다.</h3>
      <p>{query.hasNextPage ? "더 오래된 분석 이력을 확인할 수 있습니다." : "이 스냅샷으로 완료된 공식 분석이 아직 없습니다."}</p></div> :
      <div className="snapshot-analysis-list">{matches.map(item => <SnapshotAnalysisCard key={item.analysisId} item={item} />)}</div>}
    {query.hasNextPage ? <button type="button" disabled={query.isFetchingNextPage} onClick={() => query.fetchNextPage()}>
      {query.isFetchingNextPage ? "이전 분석을 불러오는 중…" : "이전 분석 더 확인"}
    </button> : matches.length > 0 ? <p className="skill-history-end">연결된 분석 이력을 모두 확인했습니다.</p> : null}
  </section>;
}

function AnalysisHeading() { return <div className="section-heading"><div><h2 id="snapshot-analysis-title">이 스냅샷의 공식 분석</h2>
  <p>이 불변 입력을 참조한 완료 분석의 저장된 점수와 버전입니다. 화면에서 다시 계산하지 않습니다.</p></div></div>; }

function SnapshotAnalysisCard({ item }: { item: AnalysisHistoryItem }) { return <article>
  <div><div><span className={`status-badge${item.currentForRepository ? " status-badge--active" : ""}`}>
    {item.currentForRepository ? "현재 분석" : "과거 분석"}</span><h3>{item.repositoryFullName}</h3>
    <time dateTime={item.completedAt}>{formatDate(item.completedAt)}</time></div>
    <div className="official-score" aria-label={`저장된 공식 점수 ${formatNumber(item.overallScore)}점`}><strong>{formatNumber(item.overallScore)}</strong><span>/ 100</span></div></div>
  <dl><Fact label="신뢰도" value={`${formatNumber(item.confidence)}%`} /><Fact label="규칙 버전" value={item.ruleSetVersion} />
    <Fact label="Skill Matrix 정책" value={item.policyVersion} /></dl>
  <Link to={`/analyses/${item.analysisId}`}>공식 분석과 근거 보기</Link>
</article>; }

function SnapshotError({ error }: { error: Error | null }) {
  if (error instanceof ApiError && error.status === 401) return <section className="state-panel" role="alert"><h1>로그인이 필요합니다.</h1><Link to="/">로그인 화면으로 이동</Link></section>;
  return <section className="state-panel" role="alert"><h1>스냅샷을 표시할 수 없습니다.</h1><p>스냅샷이 없거나 현재 계정으로 접근할 수 없습니다.</p></section>;
}
function Shell({ repositoryId, children }: { repositoryId: string | undefined; children: React.ReactNode }) {
  return <main className="shell workspace"><nav><Link to={repositoryId ? `/repositories/${repositoryId}` : "/repositories"}>← 저장소로 돌아가기</Link></nav>{children}</main>;
}
function Fact({ label, value, code = false }: { label: string; value: string; code?: boolean }) { return <div><dt>{label}</dt><dd>{code ? <code>{value}</code> : value}</dd></div>; }
function Count({ label, value }: { label: string; value: number }) { return <div><strong>{value.toLocaleString("ko-KR")}</strong><span>{label}</span></div>; }
function snapshotStatusLabel(status: RepositorySnapshot["status"]) { return { READY: "수집 완료", FAILED: "수집 실패", SUPERSEDED: "과거 버전", DELETED_BY_POLICY: "정책 삭제" }[status]; }
function formatDate(value: string) { return new Intl.DateTimeFormat("ko-KR", { dateStyle: "long", timeStyle: "short" }).format(new Date(value)); }
function formatNumber(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
