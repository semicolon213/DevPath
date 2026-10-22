import { Link } from "react-router-dom";
import type { DashboardSourceStatus, DashboardTarget } from "../features/dashboard/api/dashboardApi";
import { useDashboard } from "../features/dashboard/model/useDashboard";
import { categoryLabel, jobStatusLabel, levelLabel, priorityLabel } from "../shared/ui/labels";
import { PageState } from "../shared/ui/PageState";

export function DashboardPage() {
  const query = useDashboard();
  if (query.isPending) return <PageState state="loading" title="내 DevPath 대시보드" message="개발 현황을 모으는 중입니다." />;
  if (query.isError) return <PageState state="error" title="대시보드를 불러오지 못했습니다." description="기존 공식 결과는 변경되지 않았습니다." error={query.error} retry={() => query.refetch()} />;

  const view = query.data;
  const latest = view.analyses.latest;
  const topRecommendation = view.recommendations.status === "AVAILABLE" ? view.recommendations.items[0] : undefined;
  return (
    <>
      <header className="dashboard-hero">
        <div className="dashboard-hero__content">
          <div className="dashboard-hero__intro">
            <p className="dashboard-hero__eyebrow">{view.targets.status === "UNAVAILABLE" ? "목표 · 목표 정보를 불러오지 못했습니다." : <>목표 · {targetName(view.targets.career, "목표 직무를 선택하지 않았습니다.")} · {targetName(view.targets.company, "선택한 회사가 없습니다.")}</>}</p>
            <h1>내 DevPath 대시보드</h1>
            <p>선택한 목표와 GitHub 근거에서 계산된 최신 공식 결과와 다음 행동을 확인하세요.</p>
            <div className="dashboard-hero__actions">
              <Link className="button-link" to={topRecommendation ? `/recommendations/${topRecommendation.recommendationId}` : "/career-readiness"} aria-label={topRecommendation ? `지금 할 일 보기: ${topRecommendation.title}` : undefined}>{topRecommendation ? "지금 할 일 보기" : "준비도 확인"}</Link>
              <Link className="button-link button-secondary" to="/analyses">분석 이력</Link>
            </div>
            <p className="dashboard-hero__basis">기준 시각 <time dateTime={view.generatedAt}>{formatDate(view.generatedAt)}</time> · <Link to="/careers">목표 변경</Link> · <Link to="/repositories">저장소</Link></p>
          </div>
          <section className="dashboard-readiness" aria-labelledby="dashboard-readiness-title">
            <span id="dashboard-readiness-title">커리어 준비도</span>
            <Readiness status={view.readiness.status} resultStatus={view.readiness.resultStatus} score={view.readiness.score} level={view.readiness.level} confidence={view.readiness.confidence} unavailableCategories={view.readiness.unavailableCategories} />
            <Link to="/career-readiness">준비도 상세</Link>
          </section>
        </div>
      </header>

      <section className="dashboard-metrics" aria-label="개발 현황 요약">
        <Metric value={valueOrDash(view.repositories.status, view.repositories.totalCount)} label="등록 저장소" status={view.repositories.status} />
        <Metric value={valueOrDash(view.repositories.status, view.repositories.synchronizedCount)} label="동기화 완료" status={view.repositories.status} />
        <Metric value={valueOrDash(view.analyses.status, view.analyses.totalCount)} label="완료된 분석" status={view.analyses.status} />
        <Metric value={valueOrDash(view.skillOverview.status, view.skillOverview.skillCount)} label="평가된 역량" status={view.skillOverview.status} />
      </section>

      <section className="dashboard-section dashboard-section--roadmap" aria-labelledby="dashboard-roadmap-title">
        <CardHeading eyebrow="공식 학습 계획" title="학습 로드맵" link="/roadmap" linkLabel="로드맵 보기" id="dashboard-roadmap-title" />
        {view.roadmap.status === "UNAVAILABLE" ? <Unavailable /> : view.roadmap.status === "EMPTY" ? <Empty text="활성 학습 로드맵이 없습니다." action="추천 확인" to="/roadmap" /> : <div className="dashboard-progress"><div><strong>진행률 {formatScore(view.roadmap.progressPercent ?? 0)}%</strong><span>마일스톤 {view.roadmap.milestoneCount}개 · 단계 {view.roadmap.stepCount}개</span></div><progress aria-label="학습 로드맵 진행률" max="100" value={view.roadmap.progressPercent ?? 0}>{view.roadmap.progressPercent ?? 0}%</progress></div>}
      </section>

      <section className="dashboard-section dashboard-section--recommendations" aria-labelledby="dashboard-recommendations-title">
        <CardHeading eyebrow="공식 추천" title="우선 추천" link="/recommendations" linkLabel="추천 전체 보기" id="dashboard-recommendations-title" />
        {view.recommendations.status === "UNAVAILABLE" ? <Unavailable /> : view.recommendations.items.length ? <ol className="dashboard-recommendation-list">{view.recommendations.items.map((item, index) => <li className={index === 0 ? "dashboard-recommendation dashboard-recommendation--current" : "dashboard-recommendation"} key={item.recommendationId}><span className="dashboard-recommendation__number">{index + 1}</span><div><span className="dashboard-recommendation__state">{index === 0 ? "지금 할 일" : `${index + 1}순위`}</span><strong>{item.title}</strong><small>{categoryLabel(item.category)} · {item.effortHours}시간</small></div><span className="dashboard-recommendation__priority">{priorityLabel(item.priority)}</span><Link to={`/recommendations/${item.recommendationId}`} aria-label={`${item.title} 추천 상세`}>추천 상세</Link></li>)}</ol> : <Empty text="아직 생성된 추천이 없습니다." action="준비도 확인" to="/career-readiness" />}
      </section>

      <div className="dashboard-feature-grid">
        <section className="dashboard-section dashboard-section--skills" aria-labelledby="dashboard-skills-title">
          <CardHeading eyebrow="기술 역량" title="강점과 개선 영역" link="/skills" linkLabel="전체 역량" id="dashboard-skills-title" />
          {view.skillOverview.status === "UNAVAILABLE" ? <Unavailable /> : view.skillOverview.status === "EMPTY" ? <Empty text="기술 역량 결과가 없습니다." action="분석 준비" to="/repositories" /> : <><div className="dashboard-split"><div><strong>{view.skillOverview.strengthCount}</strong><span>강점</span></div><div><strong>{view.skillOverview.weaknessCount}</strong><span>개선 영역</span></div></div><p>정책 {view.skillOverview.policyVersion} · 규칙 {view.skillOverview.ruleSetVersion}</p></>}
        </section>

        <section className="dashboard-section dashboard-section--analysis" aria-labelledby="dashboard-latest-title">
          <CardHeading eyebrow="공식 분석" title="최신 공식 분석" link="/analyses" linkLabel="전체 이력" id="dashboard-latest-title" />
          {view.analyses.status === "UNAVAILABLE" ? <Unavailable /> : latest ? <><div className="dashboard-score"><strong>{formatScore(latest.overallScore)}</strong><span>/ 100</span></div><h3>{latest.repositoryFullName}</h3><p>{latest.currentForRepository ? "현재 저장소에 적용되는 최신 결과" : "보존된 과거 분석 결과"}</p><Link to={`/analyses/${latest.analysisId}`}>점수 근거 확인</Link></> : <Empty text="완료된 분석이 없습니다." action="저장소에서 분석 시작" to="/repositories" />}
        </section>
      </div>

      <section className="dashboard-section dashboard-section--list" aria-labelledby="recent-jobs-title">
        <CardHeading eyebrow="백그라운드 작업" title="최근 작업" id="recent-jobs-title" />
        {view.recentJobs.status === "UNAVAILABLE" ? <Unavailable /> : view.recentJobs.items.length ? <ul className="dashboard-analysis-list">{view.recentJobs.items.map(job => <li key={job.jobId}><div><strong>{job.jobType === "REPOSITORY_SYNC" ? "저장소 동기화" : "분석"}</strong><time dateTime={job.submittedAt}>{formatDate(job.submittedAt)}</time></div><span>{job.progressPercent}%</span><small>{jobStatusLabel(job.status)}</small></li>)}</ul> : <Empty text="최근 실행된 작업이 없습니다." action="저장소 확인" to="/repositories" />}
      </section>

      <section className="dashboard-section dashboard-section--list" aria-labelledby="repository-analysis-title">
        <CardHeading eyebrow="최신 결과" title="저장소별 현재 분석" link="/repositories" linkLabel="저장소 관리" id="repository-analysis-title" />
        {view.analyses.status === "UNAVAILABLE" ? <Unavailable /> : view.analyses.currentByRepository.length ? <ul className="dashboard-analysis-list">{view.analyses.currentByRepository.map(item => <li key={item.analysisId}><div><strong>{item.repositoryFullName}</strong><time dateTime={item.completedAt}>{formatDate(item.completedAt)}</time></div><span>{formatScore(item.overallScore)}점</span><Link to={`/analyses/${item.analysisId}`}>상세</Link></li>)}</ul> : <Empty text="저장소별 최신 분석 결과가 아직 없습니다." action="분석 준비" to="/repositories" />}
      </section>

      <footer className="dashboard-basis">기준 시각 <time dateTime={view.generatedAt}>{formatDate(view.generatedAt)}</time> · 커리어 프로필 {view.targets.career?.profileVersion ?? "없음"} · Skill Matrix 정책 {view.skillOverview.policyVersion ?? "없음"} · 규칙 {view.skillOverview.ruleSetVersion ?? "없음"} · 추천 정책 {view.recommendations.policyVersion ?? "없음"} · 로드맵 정책 {view.roadmap.policyVersion ?? "없음"}</footer>
    </>
  );
}

function Readiness({ status, resultStatus, score, level, confidence, unavailableCategories }: { status: DashboardSourceStatus; resultStatus: string | null; score: number | null; level: string | null; confidence: number | null; unavailableCategories: string[] }) {
  if (status === "UNAVAILABLE") return <Unavailable text="커리어 준비도 정보를 불러오지 못했습니다." />;
  if (status === "EMPTY") return <Empty text="아직 준비도 결과가 없습니다." action="분석 준비" to="/repositories" />;
  if (resultStatus === "INSUFFICIENT_EVIDENCE") return <div className="dashboard-readiness__insufficient"><span className="status-badge">근거 부족</span><p>확인 불가 범주: {unavailableCategories.map(categoryLabel).join(", ")}</p></div>;
  return <><div className="dashboard-score"><strong>{formatScore(score ?? 0)}</strong><span>/ 100</span></div><p>{levelLabel(level)} · 신뢰도 {formatScore(confidence ?? 0)}%</p></>;
}
function targetName(target: DashboardTarget | null, fallback: string) { return target?.localizedName ?? fallback; }
function Metric({ value, label, status }: { value: number | string; label: string; status: DashboardSourceStatus }) { return <div><strong>{value}</strong><span>{status === "UNAVAILABLE" ? `${label} · 확인 실패` : label}</span></div>; }
function CardHeading({ eyebrow, title, link, linkLabel, id }: { eyebrow: string; title: string; link?: string; linkLabel?: string; id?: string }) { return <header className="dashboard-section__heading"><div><span>{eyebrow}</span><h2 id={id}>{title}</h2></div>{link ? <Link to={link}>{linkLabel}</Link> : null}</header>; }
function Empty({ text, action, to }: { text: string; action: string; to: string }) { return <div className="dashboard-empty"><p>{text}</p><Link to={to}>{action}</Link></div>; }
function Unavailable({ text = "일부 정보를 불러오지 못했습니다. 다른 영역의 공식 결과는 그대로 표시합니다." }: { text?: string }) { return <p role="status" className="dashboard-unavailable">{text}</p>; }
function valueOrDash(status: DashboardSourceStatus, value: number) { return status === "UNAVAILABLE" ? "—" : value; }
function formatScore(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function formatDate(value: string) { return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
