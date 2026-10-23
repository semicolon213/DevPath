import { Link, useParams } from "react-router-dom";
import { useCareerReadinessWorkspace } from "../features/readiness/model/useCareerReadiness";
import { categoryLabel, levelLabel } from "../shared/ui/labels";
import { PageState } from "../shared/ui/PageState";
import { Breadcrumbs } from "../shared/ui/Breadcrumbs";
import { ReadinessGapList } from "../shared/ui/ReadinessGapList";
import { PageHero } from "../shared/ui/PageHero";
import { ResultBasis } from "../shared/ui/ResultBasis";

export function CareerReadinessDetailPage() {
  const { careerReadinessId } = useParams();
  const query = useCareerReadinessWorkspace(careerReadinessId);
  if (query.isPending) return <Shell><PageState state="loading" title="커리어 준비도 상세" message="과거 준비도와 전체 역량 격차를 불러오는 중입니다." /></Shell>;
  if (query.isError) return <Shell><PageState state="error" title="준비도 결과를 표시할 수 없습니다" errorTitle="준비도 상세를 불러오지 못했습니다" error={query.error} retry={() => query.refetch()} backTo="/recommendations" /></Shell>;
  const result = query.data.readiness;
  const incomplete = result.status === "INSUFFICIENT_EVIDENCE";
  const needsAction = result.skillGaps.filter(gap => !["SUFFICIENT", "STRONG"].includes(gap.gapState));

  return <Shell>
    <PageHero><div><p className="eyebrow">저장된 커리어 평가</p><h1>커리어 준비도 상세</h1>
      <p>평가 당시 Skill Matrix와 커리어 정책으로 생성된 불변 결과입니다.</p></div>
    </PageHero>
    <section className="readiness-hero" aria-labelledby="historical-readiness-title"><div><span id="historical-readiness-title">저장된 준비도</span>
      <strong>{incomplete ? "근거 부족" : format(result.readinessScore!)}</strong><small>{incomplete ? "필수 범주 근거가 부족한 결과입니다." : `${levelLabel(result.readinessLevel!)} · 신뢰도 ${format(result.confidence)}%`}</small></div>
      {incomplete ? <p>평가 불가 범주: {result.unavailableCategories.map(categoryLabel).join(", ")}</p> : <progress aria-label="저장된 커리어 준비도 점수" max="100" value={result.readinessScore!}>{result.readinessScore}점</progress>}</section>
    <section className="recommendation-history-summary" aria-label="격차 요약"><div><strong>{result.skillGaps.length}</strong><span>평가 역량</span></div>
      <div><strong>{needsAction.length}</strong><span>개선 대상</span></div><div><strong>{result.skillGaps.reduce((sum, gap) => sum + gap.evidenceIds.length, 0)}</strong><span>연결 근거</span></div></section>
    <section aria-labelledby="historical-gap-title"><div className="section-heading"><div><h2 id="historical-gap-title">전체 Skill Gap</h2>
      <p>저장된 실제 점수와 기대 최소값을 표시하며 추천 우선순위를 새로 계산하지 않습니다.</p></div></div>
      <ReadinessGapList gaps={result.skillGaps} /></section>
    <section className="skill-next-action"><div><h2>결과에서 실행으로</h2><p>연결된 추천과 학습 로드맵에서 당시 격차에 대한 결정론적 다음 단계를 확인할 수 있습니다.</p></div>
      <div className="workspace-actions"><Link to="/recommendations">추천 이력</Link><Link className="button-link button-primary" to="/roadmap">학습 로드맵</Link></div></section>
    <ResultBasis items={[{ label: "평가 기준 시각", value: <time dateTime={result.assessedAt}>{formatDate(result.assessedAt)}</time> }, { label: "정책", value: result.readinessPolicyVersion }, { label: "규칙", value: result.ruleSetVersion }, { label: "프로필", value: result.careerProfileVersion }]} />
  </Shell>;
}

function Shell({ children }: { children: React.ReactNode }) { return <><Breadcrumbs items={[{ label: "커리어 준비도", to: "/career-readiness" }, { label: "준비도 상세" }]} />{children}</>; }
function format(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function formatDate(value: string) { return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
