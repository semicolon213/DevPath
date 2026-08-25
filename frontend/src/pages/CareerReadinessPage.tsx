import { Link } from "react-router-dom";
import type { ReadinessLevel, RuleCategory, SkillGap } from "../features/readiness/api/careerReadinessApi";
import { useCurrentCareerReadiness } from "../features/readiness/model/useCareerReadiness";
import { ApiError } from "../shared/api/apiClient";

export function CareerReadinessPage() {
  const query = useCurrentCareerReadiness();
  if (query.isPending) return <Shell><p role="status">커리어 준비도 결과를 불러오는 중입니다…</p></Shell>;
  if (query.isError) return <Shell><ReadinessError error={query.error} retry={() => query.refetch()} /></Shell>;
  const result = query.data;
  const incomplete = result.status === "INSUFFICIENT_EVIDENCE";

  return <Shell>
    <header className="workspace-header">
      <div><p className="eyebrow">Deterministic Career Engine</p><h1>커리어 준비도</h1><p>AI가 아닌 승인된 정책과 현재 Skill Matrix로 계산한 공식 결과입니다.</p></div>
      <div className="analysis-version"><span>정책 {result.readinessPolicyVersion}</span><span>프로필 {result.careerProfileVersion}</span><span>규칙 {result.ruleSetVersion}</span></div>
    </header>
    <section className="readiness-hero" aria-labelledby="readiness-result-title">
      <div><span id="readiness-result-title">현재 준비도</span><strong>{incomplete ? "근거 부족" : formatScore(result.readinessScore!)}</strong><small>{incomplete ? "필수 범주가 모두 평가된 뒤 점수와 등급을 표시합니다." : `${levelLabel(result.readinessLevel!)} · 신뢰도 ${formatScore(result.confidence)}%`}</small></div>
      {incomplete ? <p>평가 불가 범주: {result.unavailableCategories.map(categoryLabel).join(", ")}</p> : <progress max="100" value={result.readinessScore!}>{result.readinessScore}점</progress>}
    </section>
    <section aria-labelledby="gap-list-title">
      <div className="section-heading"><div><h2 id="gap-list-title">역량 비교</h2><p>모든 필수 범주를 격차 상태, 직무 가중치, 범주 순서로 보여줍니다. 추천 우선순위는 포함하지 않습니다.</p></div></div>
      <div className="readiness-gap-list">{result.skillGaps.map(gap => <GapCard key={gap.skillGapId} gap={gap} />)}</div>
    </section>
    <section className="skill-next-action"><div><h2>결정론적 다음 단계</h2><p>충분 미만의 gap은 승인된 추천 정책과 선행 순서에 따라 로드맵으로 구성됩니다.</p></div><Link className="button-link" to="/roadmap">추천 로드맵 보기</Link></section>
  </Shell>;
}

function GapCard({ gap }: { gap: SkillGap }) {
  return <article className={`readiness-gap readiness-gap--${gap.gapState.toLowerCase()}`}>
    <div><span>{categoryLabel(gap.category)}</span><strong>{gapStateLabel(gap.gapState)}</strong></div>
    <p><b>{formatScore(gap.actualScore)}</b> / 기대 최소 {formatScore(gap.expectedMinimum)}</p>
    <progress max="100" value={gap.actualScore}>{gap.actualScore}점</progress>
    <small>직무 가중치 {formatScore(gap.careerWeight)}% · 근거 {gap.evidenceIds.length}개</small><Link to={`/skills/${gap.skillId}`}>기술 평가와 증거 보기</Link>
  </article>;
}

function ReadinessError({ error, retry }: { error: Error; retry: () => void }) {
  if (error instanceof ApiError && error.status === 404) return <div className="state-panel"><h1>아직 준비도 결과가 없습니다</h1><p>백엔드 또는 프론트엔드 목표를 선택하고 새 분석을 완료하면 결과가 생성됩니다.</p><Link className="button-link" to="/repositories">분석 준비</Link></div>;
  if (error instanceof ApiError && error.status === 401) return <div className="state-panel" role="alert"><h1>로그인이 필요합니다</h1><Link className="button-link" to="/">로그인 화면으로 이동</Link></div>;
  return <div className="state-panel" role="alert"><h1>준비도를 불러오지 못했습니다</h1><button type="button" onClick={retry}>다시 시도</button></div>;
}
function Shell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/dashboard">← 대시보드</Link></nav>{children}</main>; }
function formatScore(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function categoryLabel(value: RuleCategory) { return { LANGUAGE: "언어", FRAMEWORK: "프레임워크", DATABASE: "데이터베이스", ARCHITECTURE: "아키텍처", TESTING: "테스트", DEVOPS: "DevOps", DOCUMENTATION: "문서화", ACTIVITY: "활동" }[value]; }
function levelLabel(value: ReadinessLevel) { return { NONE: "관찰 없음", BEGINNER: "입문", DEVELOPING: "성장", COMPETENT: "충분", STRONG: "강점" }[value]; }
function gapStateLabel(value: SkillGap["gapState"]) { return { MISSING: "관찰 없음", WEAK: "약함", PARTIAL: "일부 충족", SUFFICIENT: "충족", STRONG: "강점" }[value]; }
