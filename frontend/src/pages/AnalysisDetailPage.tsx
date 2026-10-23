import { Link, useParams } from "react-router-dom";
import type { RuleCategoryScore, RuleEvidence, RuleResult } from "../features/analysis/api/analysisApi";
import { useAnalysisDetail } from "../features/analysis/model/useAnalysis";
import { RepositoryReviewPanel } from "../features/ai/ui/RepositoryReviewPanel";
import { RepositorySummaryPanel } from "../features/ai/ui/RepositorySummaryPanel";
import { categoryLabel, evidenceRoleLabel, evidenceTypeLabel, formulaLabel, levelLabel, ruleLabel, ruleStatusLabel, skillNameLabel } from "../shared/ui/labels";
import { PageState } from "../shared/ui/PageState";
import { Breadcrumbs } from "../shared/ui/Breadcrumbs";
import { EvidenceStatus } from "../shared/ui/EvidenceStatus";
import { PageHero } from "../shared/ui/PageHero";
import { ResultBasis } from "../shared/ui/ResultBasis";

export function AnalysisDetailPage() {
  const { analysisId } = useParams();
  const query = useAnalysisDetail(analysisId);

  if (query.isPending) return <PageShell><PageState state="loading" title="분석 결과" message="분석 결과와 근거를 불러오는 중입니다." /></PageShell>;
  if (query.isError) return <PageShell><PageState state="error" title="분석 결과를 찾을 수 없습니다" errorTitle="분석 상세를 불러오지 못했습니다" description="저장된 결과는 변경되지 않았습니다." error={query.error} retry={() => query.refetch()} backTo="/analyses" backLabel="분석 이력으로 이동" /></PageShell>;

  const { result, repository, evaluation, evidence, matrix } = query.data;
  return (
    <PageShell>
      <PageHero>
        <div>
          <p className="eyebrow">공식 분석 결과</p>
          <h1>{repository.fullName}</h1>
          <p>스냅샷과 규칙 버전이 고정되어 언제든 같은 입력과 정책으로 추적할 수 있는 공식 결과입니다.</p>
          <span className={`status-badge${result.currentForRepository ? " status-badge--info" : ""}`}>
            {result.currentForRepository ? "이 저장소의 현재 적용 결과" : "이 저장소의 과거 결과"}
          </span>
        </div>
        <div className="workspace-actions">
          <Link to={`/repositories/${repository.repositoryId}`}>저장소 보기</Link>
          <Link className="button-link button-secondary" to="/analyses">분석 이력</Link>
        </div>
      </PageHero>

      <section className="analysis-result-hero" aria-label="공식 분석 요약">
        <div className="analysis-result-score">
          <span>공식 종합 점수</span>
          <strong>{formatScore(evaluation.overallScore)}</strong>
          <small>100점 만점</small>
        </div>
        <dl>
          <div><dt>근거 신뢰도</dt><dd>{formatScore(evaluation.confidence)}%</dd></div>
          <div><dt>연결된 근거</dt><dd>{evaluation.evidenceSummary.evidenceCount}개</dd></div>
          <div><dt>규칙 버전</dt><dd>{evaluation.ruleSetVersion}</dd></div>
          <div><dt>공식 계산식</dt><dd>{evaluation.formulaLibraryVersion}</dd></div>
          <div><dt>역량 정책</dt><dd>{matrix.policyVersion}</dd></div>
          <div><dt>완료 시각</dt><dd>{formatDateTime(result.completedAt)}</dd></div>
        </dl>
      </section>

      <section className="score-explanation" aria-labelledby="score-explanation-title">
        <div>
          <p className="eyebrow">결과 해설</p>
          <h2 id="score-explanation-title">왜 이 점수가 나왔나요?</h2>
          <p>저장소 스냅샷에서 관찰한 사실을 각 규칙에 입력하고, 표시된 공식과 가중치를 적용한 결과입니다. 점수와 우선순위는 화면이 아닌 Rule Engine이 확정합니다.</p>
        </div>
        <dl>
          <div><dt>점수에 반영된 규칙</dt><dd>{countScoredRules(evaluation.categoryScores)}개</dd></div>
          <div><dt>근거가 부족한 항목</dt><dd>{evaluation.evidenceSummary.missingEvidenceCount}개</dd></div>
          <div><dt>확인 방법</dt><dd>아래 규칙을 펼쳐 관찰값·공식·가중치를 확인하세요.</dd></div>
        </dl>
      </section>

      {evaluation.warnings.length > 0 ? (
        <section className="analysis-warning" role="status"><h2>분석 경고</h2><ul>{evaluation.warnings.map(value => <li key={value}>{value}</li>)}</ul></section>
      ) : null}

      <section aria-labelledby="category-results-title">
        <div className="section-heading"><div><h2 id="category-results-title" tabIndex={-1}>영역별 공식 결과</h2><p>규칙을 펼치면 어떤 관찰값이 어떤 공식과 가중치로 반영됐는지 확인할 수 있습니다.</p></div></div>
        <div className="category-result-grid">
          {evaluation.categoryScores.map(category => <CategoryResult key={category.category} category={category} />)}
        </div>
      </section>

      <section className="analysis-evidence-section" aria-labelledby="evidence-title">
        <div className="section-heading"><div><h2 id="evidence-title">평가 근거</h2><p>분석 스냅샷에서 정규화되어 규칙 결과에 연결된 근거입니다.</p></div><span>{evidence.length}개</span></div>
        {evidence.length === 0 ? <p className="muted">직접 연결된 근거가 없습니다.</p> : (
          <div className="analysis-evidence-list">{evidence.map(item => <EvidenceItem key={item.evidenceId} evidence={item} />)}</div>
        )}
      </section>

      <section aria-labelledby="historical-skills-title">
        <div className="section-heading"><div><h2 id="historical-skills-title">이 분석의 기술 역량</h2><p>현재 값으로 덮어쓰지 않는 당시의 공식 역량 평가입니다.</p></div></div>
        <div className="historical-skill-list">
          {matrix.skills.map(skill => (
            <article className="historical-skill-row" key={skill.assessmentId}>
              <div className="historical-skill-row__name"><strong>{skillNameLabel(skill.skillKey, skill.skillName)}</strong><span>{levelLabel(skill.level)}</span></div>
              <div className="historical-skill-row__score">
                {skill.evidenceStatus !== "NONE" ? <progress aria-label={`${skillNameLabel(skill.skillKey, skill.skillName)} 공식 점수`} max="100" value={skill.score}>{skill.score}점</progress> : null}
                <small>{skill.evidenceStatus === "NONE" ? "저장된 " : ""}공식 점수 {formatScore(skill.score)} · 신뢰도 {formatScore(skill.confidence)}%</small>
              </div>
              <div className="historical-skill-row__status"><EvidenceStatus status={skill.evidenceStatus} guidance={false} /></div>
            </article>
          ))}
        </div>
      </section>

      <details className="analysis-trace">
        <summary>기술 추적 정보 보기</summary>
        <p>분석 결과가 참조하는 불변 스냅샷과 평가 리소스입니다.</p>
        <dl className="trace-grid">
          <div><dt>분석 ID</dt><dd><code>{result.analysisId}</code></dd></div>
          <div><dt>스냅샷 ID</dt><dd><code>{result.snapshotId}</code></dd></div>
          <div><dt>평가 ID</dt><dd><code>{result.evaluationId}</code></dd></div>
          <div><dt>Skill Matrix ID</dt><dd><code>{result.skillMatrixId}</code></dd></div>
        </dl>
      </details>

      <RepositorySummaryPanel analysisId={result.analysisId} />
      <RepositoryReviewPanel analysisId={result.analysisId} />
      <ResultBasis items={[{ label: "분석 기준 시각", value: <time dateTime={result.completedAt}>{formatDateTime(result.completedAt)}</time> }, { label: "규칙 버전", value: evaluation.ruleSetVersion }, { label: "공식 계산식", value: evaluation.formulaLibraryVersion }, { label: "Skill Matrix 정책", value: matrix.policyVersion }]} />
    </PageShell>
  );
}

function CategoryResult({ category }: { category: RuleCategoryScore }) {
  return (
    <article className="category-result-card">
      <div className="category-result-card__heading"><div><span>{categoryLabel(category.category)}</span><strong>{formatScore(category.score)}점</strong></div><small>종합 점수 반영 비중 {formatPercent(category.weight)}</small></div>
      <div className="category-result-card__bar"><progress aria-label={`${categoryLabel(category.category)} 공식 점수`} max="100" value={category.score}>{category.score}점</progress><strong>{formatScore(category.score)} / 100</strong></div>
      <p>근거 신뢰도 {formatScore(category.confidence)}%</p>
      <div className="rule-result-list">{category.ruleResults.map(rule => <RuleResultItem key={rule.ruleId} rule={rule} />)}</div>
      {category.missingEvidence.length > 0 ? <p className="missing-evidence">부족한 근거: {category.missingEvidence.map(ruleId => ruleLabel(ruleId)).join(", ")}</p> : null}
    </article>
  );
}

function RuleResultItem({ rule }: { rule: RuleResult }) {
  return (
    <details>
      <summary><span>{ruleLabel(rule.ruleId)}</span><span>{ruleStatusLabel(rule.status)} · {formatScore(rule.score)}점</span></summary>
      <p className="rule-readable-reason">{ruleReason(rule)}</p>
      <dl>
        <div><dt>관찰값</dt><dd>{formatObservedValue(rule)}</dd></div><div><dt>규칙 가중치</dt><dd>{formatPercent(rule.weight)}</dd></div>
        <div><dt>계산식</dt><dd>{formulaLabel(rule.formulaId)}</dd></div><div><dt>규칙 버전</dt><dd>{rule.ruleVersion}</dd></div>
      </dl>
      <details className="calculation-trace"><summary>원본 계산 추적 보기</summary><p>{rule.trace}</p></details>
    </details>
  );
}

function EvidenceItem({ evidence }: { evidence: RuleEvidence }) {
  return <article id={`evidence-${evidence.evidenceId}`} tabIndex={-1}><div><strong>{ruleLabel(evidence.ruleId)}</strong><span className="status-badge">{evidenceRoleLabel(evidence.contributionRole)}</span></div><p>{evidence.observedFactSummary}</p><span>{evidenceTypeLabel(evidence.evidenceType)}</span><code>{evidence.sourceReference}</code><small>신뢰도 {formatScore(evidence.confidence)}%</small></article>;
}

function PageShell({ children }: { children: React.ReactNode }) { return <><Breadcrumbs items={[{ label: "분석", to: "/analyses" }, { label: "분석 상세" }]} />{children}</>; }
function formatScore(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function formatPercent(value: number) { return `${formatScore(value * 100)}%`; }
function formatDateTime(value: string) { return new Intl.DateTimeFormat("ko-KR", { dateStyle: "long", timeStyle: "short" }).format(new Date(value)); }
function countScoredRules(categories: RuleCategoryScore[]) { return categories.reduce((count, category) => count + category.ruleResults.filter(rule => rule.status !== "SKIPPED" && rule.status !== "ERROR").length, 0); }
function formatObservedValue(rule: RuleResult) { return rule.formulaId === "BOOLEAN_100" ? (rule.rawValue > 0 ? "있음" : "없음") : formatScore(rule.rawValue); }
function ruleReason(rule: RuleResult) { return `${ruleLabel(rule.ruleId, "이 규칙")}의 관찰값은 ${formatObservedValue(rule)}이며, '${formulaLabel(rule.formulaId)}' 공식을 적용한 공식 규칙 점수는 ${formatScore(rule.score)}점입니다.`; }
