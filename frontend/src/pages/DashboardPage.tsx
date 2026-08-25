import { Link } from "react-router-dom";
import type { DashboardSource } from "../features/dashboard/api/dashboardApi";
import { useDashboard } from "../features/dashboard/model/useDashboard";
import type { SkillMatrix } from "../features/skills/api/skillMatrixApi";
import { ApiError } from "../shared/api/apiClient";

export function DashboardPage() {
  const query = useDashboard();
  if (query.isPending) return <Shell><p role="status">개발 현황을 모으는 중입니다…</p></Shell>;
  if (query.isError) return <Shell><DashboardError error={query.error} retry={() => query.refetch()} /></Shell>;

  const view = query.data;
  const repositories = view.repositories.status === "available" ? view.repositories.data : null;
  const analyses = view.analyses.status === "available" ? view.analyses.data : null;
  const matrix = view.skillMatrix.status === "available" ? view.skillMatrix.data : null;
  const currentAnalyses = analyses?.items.filter(item => item.currentForRepository) ?? [];
  const latestAnalysis = analyses?.items[0] ?? null;
  const synchronized = repositories?.items.filter(item => item.syncStatus === "SYNCHRONIZED").length ?? 0;

  return (
    <Shell>
      <header className="workspace-header">
        <div><p className="eyebrow">MVP 개발 현황</p><h1>내 DevPath 대시보드</h1><p>선택한 목표와 GitHub 근거, 저장소별 최신 공식 분석을 한곳에서 확인합니다.</p></div>
        <div className="workspace-actions"><Link to="/repositories">저장소</Link><Link className="button-link" to="/analyses">분석 이력</Link></div>
      </header>

      <section className="dashboard-metrics" aria-label="개발 현황 요약">
        <Metric value={repositories?.totalCount ?? "—"} label="등록 저장소" unavailable={view.repositories.status === "unavailable"} />
        <Metric value={repositories ? synchronized : "—"} label="현재 목록의 동기화 완료" unavailable={view.repositories.status === "unavailable"} />
        <Metric value={analyses?.totalCount ?? "—"} label="완료된 분석" unavailable={view.analyses.status === "unavailable"} />
        <Metric value={matrix?.skills.length ?? "—"} label="평가된 역량" unavailable={view.skillMatrix.status === "unavailable"} />
      </section>

      <section className="dashboard-grid" aria-label="목표와 공식 결과">
        <article className="dashboard-card dashboard-card--target">
          <CardHeading eyebrow="선택 목표" title="커리어 방향" link="/careers" linkLabel="목표 변경" />
          <Target label="직무" source={view.career} fallback="아직 목표 직무를 선택하지 않았습니다." />
          <Target label="회사" source={view.company} fallback="선택 회사 없음 (선택 사항)" />
          <p className="dashboard-footnote">프로필 버전은 향후 준비도 평가의 기준이지만, 이 화면에서 준비도를 계산하지 않습니다.</p>
        </article>

        <article className="dashboard-card">
          <CardHeading eyebrow="Rule Engine" title="최신 공식 분석" link="/analyses" linkLabel="전체 이력" />
          {view.analyses.status === "unavailable" ? <Unavailable /> : latestAnalysis ? (
            <><div className="dashboard-score"><strong>{formatScore(latestAnalysis.overallScore)}</strong><span>/ 100</span></div><h3>{latestAnalysis.repositoryFullName}</h3><p>{latestAnalysis.currentForRepository ? "이 저장소에 현재 적용되는 최신 결과" : "보존된 과거 분석 결과"}</p><Link to={`/analyses/${latestAnalysis.analysisId}`}>점수 근거 확인</Link></>
          ) : <Empty text="완료된 분석이 없습니다." action="저장소에서 분석 시작" to="/repositories" />}
        </article>

        <article className="dashboard-card">
          <CardHeading eyebrow="Skill Matrix" title="강점과 개선 영역" link="/skills" linkLabel="전체 역량" />
          <SkillOverview source={view.skillMatrix} />
        </article>

        <article className="dashboard-card">
          <CardHeading eyebrow="Career Engine" title="커리어 준비도" link="/career-readiness" linkLabel="상세 보기" />
          {view.careerReadiness.status === "unavailable" ? <Unavailable /> : view.careerReadiness.status === "empty" ? <Empty text="아직 준비도 결과가 없습니다." action="분석 준비" to="/repositories" /> : view.careerReadiness.data.status === "INSUFFICIENT_EVIDENCE" ? <><span className="status-badge">근거 부족</span><p>필수 범주: {view.careerReadiness.data.unavailableCategories.join(", ")}</p></> : <><div className="dashboard-score"><strong>{formatScore(view.careerReadiness.data.readinessScore!)}</strong><span>/ 100</span></div><p>{view.careerReadiness.data.readinessLevel} · 신뢰도 {formatScore(view.careerReadiness.data.confidence)}%</p></>}
        </article>
      </section>

      <section className="dashboard-card dashboard-card--wide" aria-labelledby="repository-analysis-title">
        <CardHeading eyebrow="최근 분석 결과 범위" title="최근 결과 중 현재 적용되는 분석" link="/repositories" linkLabel="저장소 관리" id="repository-analysis-title" />
        <p className="dashboard-footnote">최근 분석 20개 범위에서 저장소별 현재 결과로 표시된 항목입니다. 전체 이력은 분석 이력 화면에서 확인할 수 있습니다.</p>
        {view.analyses.status === "unavailable" ? <Unavailable /> : currentAnalyses.length ? <ul className="dashboard-analysis-list">{currentAnalyses.slice(0, 6).map(item => <li key={item.analysisId}><div><strong>{item.repositoryFullName}</strong><time dateTime={item.completedAt}>{formatDate(item.completedAt)}</time></div><span>{formatScore(item.overallScore)}점</span><Link to={`/analyses/${item.analysisId}`}>상세</Link></li>)}</ul> : <Empty text="저장소별 최신 분석 결과가 아직 없습니다." action="분석 준비" to="/repositories" />}
      </section>
    </Shell>
  );
}

function SkillOverview({ source }: { source: DashboardSource<SkillMatrix> }) {
  if (source.status === "unavailable") return <Unavailable />;
  if (source.status === "empty") return <Empty text="기술 역량 결과가 없습니다." action="분석 준비" to="/repositories" />;
  return <><div className="dashboard-split"><div><strong>{source.data.strengths.length}</strong><span>강점</span></div><div><strong>{source.data.weaknesses.length}</strong><span>개선 영역</span></div></div><p>정책 {source.data.policyVersion} · 규칙 {source.data.ruleSetVersion}</p></>;
}

function Target({ label, source, fallback }: { label: string; source: DashboardSource<{ localizedName: string; profileVersion: string }>; fallback: string }) {
  if (source.status === "unavailable") return <div className="dashboard-target"><span>{label}</span><strong>불러오지 못함</strong></div>;
  if (source.status === "empty") return <div className="dashboard-target"><span>{label}</span><strong>{fallback}</strong></div>;
  return <div className="dashboard-target"><span>{label}</span><strong>{source.data.localizedName}</strong><small>{source.data.profileVersion}</small></div>;
}

function Metric({ value, label, unavailable }: { value: number | string; label: string; unavailable: boolean }) { return <div><strong>{value}</strong><span>{unavailable ? `${label} · 확인 실패` : label}</span></div>; }
function CardHeading({ eyebrow, title, link, linkLabel, id }: { eyebrow: string; title: string; link?: string; linkLabel?: string; id?: string }) { return <header className="dashboard-card__heading"><div><span>{eyebrow}</span><h2 id={id}>{title}</h2></div>{link ? <Link to={link}>{linkLabel}</Link> : null}</header>; }
function Empty({ text, action, to }: { text: string; action: string; to: string }) { return <div className="dashboard-empty"><p>{text}</p><Link to={to}>{action}</Link></div>; }
function Unavailable() { return <p role="status" className="dashboard-unavailable">일부 정보를 불러오지 못했습니다. 다른 카드의 확인 가능한 결과는 그대로 표시합니다.</p>; }
function DashboardError({ error, retry }: { error: Error; retry: () => void }) { const anonymous = error instanceof ApiError && error.status === 401; return <div className="state-panel" role="alert"><h1>{anonymous ? "로그인이 필요합니다" : "대시보드를 불러오지 못했습니다"}</h1><p>{anonymous ? "DevPath 홈에서 로그인한 뒤 다시 확인해 주세요." : "기존 공식 결과는 변경되지 않았습니다."}</p>{anonymous ? <Link className="button-link" to="/">홈으로 이동</Link> : <button type="button" onClick={retry}>다시 시도</button>}</div>; }
function Shell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/">← DevPath 홈</Link></nav>{children}</main>; }
function formatScore(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function formatDate(value: string) { return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
