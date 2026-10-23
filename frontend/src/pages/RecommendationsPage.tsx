import { Link } from "react-router-dom";
import { useRecommendationHistory } from "../features/recommendations/model/useRecommendations";
import { categoryLabel, priorityLabel, recommendationTypeLabel } from "../shared/ui/labels";
import { PageState } from "../shared/ui/PageState";
import { PageHero } from "../shared/ui/PageHero";
import { ResultBasis } from "../shared/ui/ResultBasis";
import { StatusChip } from "../shared/ui/StatusChip";

export function RecommendationsPage() {
  const query = useRecommendationHistory();
  if (query.isPending) return <PageState state="loading" title="추천" message="추천 이력을 불러오는 중입니다." />;
  if (query.isError) return <PageState state="error" title="추천 이력을 불러오지 못했습니다" error={query.error} retry={() => query.refetch()} />;
  const sets = query.data;
  const recommendationCount = sets.reduce((total, set) => total + set.recommendations.length, 0);
  const currentSet = sets.find((set) => set.status === "PUBLISHED");
  return (
    <>
      <PageHero>
        <div>
          <p className="eyebrow">공식 추천</p>
          <h1>추천</h1>
          <p>공식 역량 차이와 버전 관리된 정책으로 생성된 추천을 시간순으로 확인합니다. AI가 우선순위나 완료 기준을 계산하지 않습니다.</p>
        </div>
        <div className="workspace-actions">
          <Link to="/career-readiness">커리어 준비도</Link>
          <Link className="button-link button-primary" to="/roadmap">
            학습 로드맵
          </Link>
        </div>
      </PageHero>
      <section className="recommendation-history-summary" aria-label="추천 이력 요약">
        <div>
          <strong>{sets.length}</strong>
          <span>추천 세트</span>
        </div>
        <div>
          <strong>{recommendationCount}</strong>
          <span>전체 추천</span>
        </div>
        <div>
          <strong>{sets.filter((set) => set.status === "PUBLISHED").length}</strong>
          <span>현재 세트</span>
        </div>
      </section>
      {sets.length === 0 ? (
        <section className="state-panel">
          <h2>아직 생성된 추천이 없습니다</h2>
          <p>분석과 커리어 준비도 평가가 완료되면 측정 가능한 추천이 생성됩니다.</p>
          <Link className="button-link" to="/repositories">
            저장소 확인
          </Link>
        </section>
      ) : (
        <section aria-labelledby="recommendation-history-title">
          <div className="section-heading">
            <div>
              <h2 id="recommendation-history-title">추천 세트 이력</h2>
              <p>과거 추천은 변경하지 않고 생성 당시 정책 버전과 상태를 보존합니다.</p>
            </div>
          </div>
          <div className="recommendation-set-list">
            {sets.map((set) => (
              <article className="recommendation-set" key={set.recommendationSetId}>
                <header>
                  <div>
                    <StatusChip variant={set.status === "PUBLISHED" ? "info" : "neutral"}>{set.status === "PUBLISHED" ? "현재 적용" : "과거 세트"}</StatusChip>
                    <h3>{formatDate(set.generatedAt)} 추천</h3>
                    <Link to={`/career-readiness/${set.careerReadinessId}`}>준비도 근거 보기</Link>
                  </div>
                  <time dateTime={set.generatedAt}>{formatDate(set.generatedAt)}</time>
                </header>
                <ol>
                  {set.recommendations.map((item, index) => {
                    const current = index === 0 && set.status === "PUBLISHED";
                    return (
                      <li className={current ? "recommendation-priority recommendation-priority--current" : "recommendation-priority"} key={item.recommendationId}>
                        <span className="recommendation-priority__index">{index + 1}</span>
                        <div>
                          <span>{current ? "지금 할 일" : `${index + 1}번째 추천`}</span>
                          <strong>{priorityLabel(item.priority)}</strong>
                        </div>
                        <h4>{item.title}</h4>
                        <p>{item.completionCriteria}</p>
                        <footer>
                          <small>
                            {categoryLabel(item.category)} · {recommendationTypeLabel(item.type)} · 예상 {item.effortHours}시간 · 근거 {item.evidenceIds.length}개
                          </small>
                          <Link to={`/recommendations/${item.recommendationId}`}>상세 근거 보기</Link>
                        </footer>
                      </li>
                    );
                  })}
                </ol>
              </article>
            ))}
          </div>
        </section>
      )}
      {currentSet ? <ResultBasis items={[{ label: "추천 기준 시각", value: <time dateTime={currentSet.generatedAt}>{formatDate(currentSet.generatedAt)}</time> }, { label: "추천 정책", value: currentSet.policyVersion }]} /> : null}
    </>
  );
}
function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "long",
    timeStyle: "short",
  }).format(new Date(value));
}
