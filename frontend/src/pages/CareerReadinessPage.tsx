import { Link } from "react-router-dom";
import { CareerCoachingPanel } from "../features/ai/ui/CareerCoachingPanel";
import { useCurrentCareerReadiness } from "../features/readiness/model/useCareerReadiness";
import { ApiError } from "../shared/api/apiClient";
import { categoryLabel, levelLabel } from "../shared/ui/labels";
import { PageState } from "../shared/ui/PageState";
import { ReadinessGapList } from "../shared/ui/ReadinessGapList";

export function CareerReadinessPage() {
  const query = useCurrentCareerReadiness();
  if (query.isPending) return <PageState state="loading" title="커리어 준비도" message="커리어 준비도 결과를 불러오는 중입니다…" />;
  if (query.isError) return query.error instanceof ApiError && query.error.status === 404
    ? <PageState state="empty" title="아직 준비도 결과가 없습니다" description="지원 직무를 선택하고 새 분석을 완료하면 결과가 생성됩니다." actionTo="/repositories" actionLabel="분석 준비" />
    : <PageState state="error" title="준비도 결과를 표시할 수 없습니다" errorTitle="준비도를 불러오지 못했습니다" error={query.error} retry={() => query.refetch()} />;
  const result = query.data;
  const incomplete = result.status === "INSUFFICIENT_EVIDENCE";

  return <>
    <header className="workspace-header readiness-header">
      <div><p className="eyebrow">공식 커리어 평가</p><h1>커리어 준비도</h1><p>AI가 아닌 승인된 정책과 현재 기술 역량표(Skill Matrix)로 계산한 공식 결과입니다.</p></div>
      <div className="analysis-version"><span>정책 {result.readinessPolicyVersion}</span><span>프로필 {result.careerProfileVersion}</span><span>규칙 {result.ruleSetVersion}</span></div>
    </header>
    <section className="readiness-hero" aria-labelledby="readiness-result-title">
      <div><span id="readiness-result-title">현재 준비도</span><strong>{incomplete ? "근거 부족" : formatScore(result.readinessScore!)}</strong><small>{incomplete ? "필수 범주가 모두 평가된 뒤 점수와 등급을 표시합니다." : `${levelLabel(result.readinessLevel!)} · 신뢰도 ${formatScore(result.confidence)}%`}</small></div>
      {incomplete ? <p>평가 불가 범주: {result.unavailableCategories.map(categoryLabel).join(", ")}</p> : <progress max="100" value={result.readinessScore!}>{result.readinessScore}점</progress>}
    </section>
    <section aria-labelledby="gap-list-title">
      <div className="section-heading"><div><h2 id="gap-list-title">역량 비교</h2><p>모든 필수 범주를 격차 상태, 직무 가중치, 범주 순서로 보여줍니다. 추천 우선순위는 포함하지 않습니다.</p></div></div>
      <ReadinessGapList gaps={result.skillGaps} />
    </section>
    <section className="skill-next-action"><div><h2>결정론적 다음 단계</h2><p>충분 미만의 gap은 승인된 추천 정책과 선행 순서에 따라 로드맵으로 구성됩니다.</p></div><Link className="button-link button-primary" to="/roadmap">추천 로드맵 보기</Link></section>
    {!incomplete && <CareerCoachingPanel careerReadinessId={result.careerReadinessId} />}
    <Basis result={result} />
  </>;
}

function Basis({ result }: { result: ReturnType<typeof useCurrentCareerReadiness>["data"] }) { return result ? <footer className="readiness-basis">평가 기준 시각 <time dateTime={result.assessedAt}>{formatDate(result.assessedAt)}</time> · 정책 {result.readinessPolicyVersion} · 규칙 {result.ruleSetVersion} · 프로필 {result.careerProfileVersion}</footer> : null; }

function formatScore(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function formatDate(value: string) { return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
