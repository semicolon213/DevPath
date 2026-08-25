import { Link, useParams } from "react-router-dom";
import type { ReadinessLevel, RuleCategory, SkillGap } from "../features/readiness/api/careerReadinessApi";
import { useCareerReadinessWorkspace } from "../features/readiness/model/useCareerReadiness";
import { ApiError } from "../shared/api/apiClient";

export function CareerReadinessDetailPage() {
  const { careerReadinessId } = useParams();
  const query = useCareerReadinessWorkspace(careerReadinessId);
  if (query.isPending) return <Shell><p role="status">과거 준비도와 전체 역량 격차를 불러오는 중입니다.</p></Shell>;
  if (query.isError) return <Shell><ErrorState error={query.error} retry={() => query.refetch()} /></Shell>;
  const result = query.data.readiness;
  const incomplete = result.status === "INSUFFICIENT_EVIDENCE";
  const needsAction = result.skillGaps.filter(gap => !["SUFFICIENT", "STRONG"].includes(gap.gapState));

  return <Shell>
    <header className="workspace-header"><div><p className="eyebrow">Immutable Career Assessment</p><h1>커리어 준비도 상세</h1>
      <p>평가 당시 Skill Matrix와 커리어 정책으로 생성된 불변 결과입니다.</p></div>
      <div className="analysis-version"><span>정책 {result.readinessPolicyVersion}</span><span>프로필 {result.careerProfileVersion}</span><span>규칙 {result.ruleSetVersion}</span></div>
    </header>
    <section className="readiness-hero" aria-labelledby="historical-readiness-title"><div><span id="historical-readiness-title">저장된 준비도</span>
      <strong>{incomplete ? "근거 부족" : format(result.readinessScore!)}</strong><small>{incomplete ? "필수 범주 근거가 부족한 결과입니다." : `${levelLabel(result.readinessLevel!)} · 신뢰도 ${format(result.confidence)}%`}</small></div>
      {incomplete ? <p>평가 불가 범주: {result.unavailableCategories.map(categoryLabel).join(", ")}</p> : <progress max="100" value={result.readinessScore!}>{result.readinessScore}점</progress>}</section>
    <section className="recommendation-history-summary" aria-label="격차 요약"><div><strong>{result.skillGaps.length}</strong><span>평가 역량</span></div>
      <div><strong>{needsAction.length}</strong><span>개선 대상</span></div><div><strong>{result.skillGaps.reduce((sum, gap) => sum + gap.evidenceIds.length, 0)}</strong><span>연결 근거</span></div></section>
    <section aria-labelledby="historical-gap-title"><div className="section-heading"><div><h2 id="historical-gap-title">전체 Skill Gap</h2>
      <p>저장된 실제 점수와 기대 최소값을 표시하며 추천 우선순위를 새로 계산하지 않습니다.</p></div></div>
      <div className="readiness-gap-list">{result.skillGaps.map(gap => <GapCard key={gap.skillGapId} gap={gap} />)}</div></section>
    <section className="skill-next-action"><div><h2>결과에서 실행으로</h2><p>연결된 추천과 학습 로드맵에서 당시 격차에 대한 결정론적 다음 단계를 확인할 수 있습니다.</p></div>
      <div className="workspace-actions"><Link to="/recommendations">추천 이력</Link><Link className="button-link" to="/roadmap">학습 로드맵</Link></div></section>
  </Shell>;
}

function GapCard({ gap }: { gap: SkillGap }) { return <article className={`readiness-gap readiness-gap--${gap.gapState.toLowerCase()}`}>
  <div><span>{categoryLabel(gap.category)}</span><strong>{gapStateLabel(gap.gapState)}</strong></div><p><b>{format(gap.actualScore)}</b> / 기대 최소 {format(gap.expectedMinimum)}</p>
  <progress max="100" value={gap.actualScore}>{gap.actualScore}점</progress><small>직무 가중치 {format(gap.careerWeight)}% · 근거 {gap.evidenceIds.length}개</small>
  <Link to={`/skills/${gap.skillId}`}>기술 평가와 증거 보기</Link></article>; }
function ErrorState({ error, retry }: { error: Error; retry: () => void }) { if (error instanceof ApiError && error.status === 401) return <State title="로그인이 필요합니다" to="/" />; if (error instanceof ApiError && error.status === 404) return <State title="준비도 결과를 찾을 수 없습니다" />; return <section className="state-panel" role="alert"><h1>준비도 상세를 불러오지 못했습니다</h1><button type="button" onClick={retry}>다시 시도</button></section>; }
function State({ title, to = "/recommendations" }: { title: string; to?: string }) { return <section className="state-panel" role="alert"><h1>{title}</h1><Link className="button-link" to={to}>돌아가기</Link></section>; }
function Shell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/career-readiness">현재 준비도</Link></nav>{children}</main>; }
function format(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function categoryLabel(value: RuleCategory) { return { LANGUAGE: "언어", FRAMEWORK: "프레임워크", DATABASE: "데이터베이스", ARCHITECTURE: "아키텍처", TESTING: "테스트", DEVOPS: "DevOps", DOCUMENTATION: "문서화", ACTIVITY: "활동" }[value]; }
function levelLabel(value: ReadinessLevel) { return { NONE: "관찰 없음", BEGINNER: "입문", DEVELOPING: "성장", COMPETENT: "충분", STRONG: "강점" }[value]; }
function gapStateLabel(value: SkillGap["gapState"]) { return { MISSING: "관찰 없음", WEAK: "약함", PARTIAL: "일부 충족", SUFFICIENT: "충족", STRONG: "강점" }[value]; }
