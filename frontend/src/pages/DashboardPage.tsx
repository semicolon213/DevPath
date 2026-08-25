import { Link } from "react-router-dom";
import type { DashboardSourceStatus, DashboardTarget } from "../features/dashboard/api/dashboardApi";
import { useDashboard } from "../features/dashboard/model/useDashboard";
import { ApiError } from "../shared/api/apiClient";

export function DashboardPage() {
  const query = useDashboard();
  if (query.isPending) return <Shell><p role="status">개발 현황을 모으는 중입니다.</p></Shell>;
  if (query.isError) return <Shell><DashboardError error={query.error} retry={() => query.refetch()} /></Shell>;

  const view = query.data;
  const latest = view.analyses.latest;
  return (
    <Shell>
      <header className="workspace-header">
        <div><p className="eyebrow">MVP 개발 현황</p><h1>내 DevPath 대시보드</h1><p>선택한 목표와 GitHub 근거에서 계산된 최신 공식 결과를 한곳에서 확인합니다.</p></div>
        <div className="workspace-actions"><Link to="/repositories">저장소</Link><Link className="button-link" to="/analyses">분석 이력</Link></div>
      </header>

      <section className="dashboard-metrics" aria-label="개발 현황 요약">
        <Metric value={valueOrDash(view.repositories.status, view.repositories.totalCount)} label="등록 저장소" status={view.repositories.status} />
        <Metric value={valueOrDash(view.repositories.status, view.repositories.synchronizedCount)} label="동기화 완료" status={view.repositories.status} />
        <Metric value={valueOrDash(view.analyses.status, view.analyses.totalCount)} label="완료된 분석" status={view.analyses.status} />
        <Metric value={valueOrDash(view.skillOverview.status, view.skillOverview.skillCount)} label="평가된 역량" status={view.skillOverview.status} />
      </section>

      <section className="dashboard-grid" aria-label="목표와 공식 결과">
        <article className="dashboard-card dashboard-card--target">
          <CardHeading eyebrow="선택 목표" title="커리어 방향" link="/careers" linkLabel="목표 변경" />
          <Target label="직무" target={view.targets.career} status={view.targets.status} fallback="목표 직무를 선택하지 않았습니다." />
          <Target label="회사" target={view.targets.company} status={view.targets.status} fallback="선택한 회사가 없습니다." />
          <p className="dashboard-footnote">대시보드는 결과를 다시 계산하지 않고 서버의 공식 결과만 요약합니다.</p>
        </article>

        <article className="dashboard-card">
          <CardHeading eyebrow="Rule Engine" title="최신 공식 분석" link="/analyses" linkLabel="전체 이력" />
          {view.analyses.status === "UNAVAILABLE" ? <Unavailable /> : latest ? <><div className="dashboard-score"><strong>{formatScore(latest.overallScore)}</strong><span>/ 100</span></div><h3>{latest.repositoryFullName}</h3><p>{latest.currentForRepository ? "현재 저장소에 적용되는 최신 결과" : "보존된 과거 분석 결과"}</p><Link to={`/analyses/${latest.analysisId}`}>점수 근거 확인</Link></> : <Empty text="완료된 분석이 없습니다." action="저장소에서 분석 시작" to="/repositories" />}
        </article>

        <article className="dashboard-card">
          <CardHeading eyebrow="Skill Matrix" title="강점과 개선 영역" link="/skills" linkLabel="전체 역량" />
          {view.skillOverview.status === "UNAVAILABLE" ? <Unavailable /> : view.skillOverview.status === "EMPTY" ? <Empty text="기술 역량 결과가 없습니다." action="분석 준비" to="/repositories" /> : <><div className="dashboard-split"><div><strong>{view.skillOverview.strengthCount}</strong><span>강점</span></div><div><strong>{view.skillOverview.weaknessCount}</strong><span>개선 영역</span></div></div><p>정책 {view.skillOverview.policyVersion} · 규칙 {view.skillOverview.ruleSetVersion}</p></>}
        </article>

        <article className="dashboard-card">
          <CardHeading eyebrow="Career Engine" title="커리어 준비도" link="/career-readiness" linkLabel="상세 보기" />
          {view.readiness.status === "UNAVAILABLE" ? <Unavailable /> : view.readiness.status === "EMPTY" ? <Empty text="아직 준비도 결과가 없습니다." action="분석 준비" to="/repositories" /> : view.readiness.resultStatus === "INSUFFICIENT_EVIDENCE" ? <><span className="status-badge">근거 부족</span><p>확인 불가 범주: {view.readiness.unavailableCategories.join(", ")}</p></> : <><div className="dashboard-score"><strong>{formatScore(view.readiness.score ?? 0)}</strong><span>/ 100</span></div><p>{view.readiness.level} · 신뢰도 {formatScore(view.readiness.confidence ?? 0)}%</p></>}
        </article>

        <article className="dashboard-card">
          <CardHeading eyebrow="Recommendation Engine" title="우선 추천" link="/recommendations" linkLabel="추천 전체 보기" />
          {view.recommendations.status === "UNAVAILABLE" ? <Unavailable /> : view.recommendations.items.length ? <ul className="dashboard-analysis-list">{view.recommendations.items.map(item => <li key={item.recommendationId}><div><strong>{item.title}</strong><span>{item.category} · {item.effortHours}시간</span></div><span>{item.priority}</span></li>)}</ul> : <Empty text="아직 생성된 추천이 없습니다." action="준비도 확인" to="/career-readiness" />}
        </article>

        <article className="dashboard-card">
          <CardHeading eyebrow="Learning Roadmap" title="학습 진행" link="/roadmap" linkLabel="로드맵 보기" />
          {view.roadmap.status === "UNAVAILABLE" ? <Unavailable /> : view.roadmap.status === "EMPTY" ? <Empty text="활성 학습 로드맵이 없습니다." action="추천 확인" to="/roadmap" /> : <><div className="dashboard-score"><strong>{formatScore(view.roadmap.progressPercent ?? 0)}</strong><span>%</span></div><p>마일스톤 {view.roadmap.milestoneCount}개 · 단계 {view.roadmap.stepCount}개</p></>}
        </article>
      </section>

      <section className="dashboard-card dashboard-card--wide" aria-labelledby="recent-jobs-title">
        <CardHeading eyebrow="Background Jobs" title="최근 작업" id="recent-jobs-title" />
        {view.recentJobs.status === "UNAVAILABLE" ? <Unavailable /> : view.recentJobs.items.length ? <ul className="dashboard-analysis-list">{view.recentJobs.items.map(job => <li key={job.jobId}><div><strong>{job.jobType === "REPOSITORY_SYNC" ? "저장소 동기화" : "분석"}</strong><time dateTime={job.submittedAt}>{formatDate(job.submittedAt)}</time></div><span>{job.progressPercent}%</span><small>{job.status}</small></li>)}</ul> : <Empty text="최근 실행된 작업이 없습니다." action="저장소 확인" to="/repositories" />}
      </section>

      <section className="dashboard-card dashboard-card--wide" aria-labelledby="repository-analysis-title">
        <CardHeading eyebrow="최신 결과" title="저장소별 현재 분석" link="/repositories" linkLabel="저장소 관리" id="repository-analysis-title" />
        {view.analyses.status === "UNAVAILABLE" ? <Unavailable /> : view.analyses.currentByRepository.length ? <ul className="dashboard-analysis-list">{view.analyses.currentByRepository.map(item => <li key={item.analysisId}><div><strong>{item.repositoryFullName}</strong><time dateTime={item.completedAt}>{formatDate(item.completedAt)}</time></div><span>{formatScore(item.overallScore)}점</span><Link to={`/analyses/${item.analysisId}`}>상세</Link></li>)}</ul> : <Empty text="저장소별 최신 분석 결과가 아직 없습니다." action="분석 준비" to="/repositories" />}
      </section>
    </Shell>
  );
}

function Target({ label, target, status, fallback }: { label: string; target: DashboardTarget | null; status: DashboardSourceStatus; fallback: string }) {
  if (status === "UNAVAILABLE") return <div className="dashboard-target"><span>{label}</span><strong>불러오지 못함</strong></div>;
  if (!target) return <div className="dashboard-target"><span>{label}</span><strong>{fallback}</strong></div>;
  return <div className="dashboard-target"><span>{label}</span><strong>{target.localizedName}</strong><small>{target.profileVersion}</small></div>;
}
function Metric({ value, label, status }: { value: number | string; label: string; status: DashboardSourceStatus }) { return <div><strong>{value}</strong><span>{status === "UNAVAILABLE" ? `${label} · 확인 실패` : label}</span></div>; }
function CardHeading({ eyebrow, title, link, linkLabel, id }: { eyebrow: string; title: string; link?: string; linkLabel?: string; id?: string }) { return <header className="dashboard-card__heading"><div><span>{eyebrow}</span><h2 id={id}>{title}</h2></div>{link ? <Link to={link}>{linkLabel}</Link> : null}</header>; }
function Empty({ text, action, to }: { text: string; action: string; to: string }) { return <div className="dashboard-empty"><p>{text}</p><Link to={to}>{action}</Link></div>; }
function Unavailable() { return <p role="status" className="dashboard-unavailable">일부 정보를 불러오지 못했습니다. 다른 카드의 공식 결과는 그대로 표시합니다.</p>; }
function DashboardError({ error, retry }: { error: Error; retry: () => void }) { const anonymous = error instanceof ApiError && error.status === 401; return <div className="state-panel" role="alert"><h1>{anonymous ? "로그인이 필요합니다." : "대시보드를 불러오지 못했습니다."}</h1><p>{anonymous ? "DevPath 안에서 로그인한 뒤 다시 확인해 주세요." : "기존 공식 결과는 변경되지 않았습니다."}</p>{anonymous ? <Link className="button-link" to="/">홈으로 이동</Link> : <button type="button" onClick={retry}>다시 시도</button>}</div>; }
function Shell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/">← DevPath 홈</Link></nav>{children}</main>; }
function valueOrDash(status: DashboardSourceStatus, value: number) { return status === "UNAVAILABLE" ? "—" : value; }
function formatScore(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function formatDate(value: string) { return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
