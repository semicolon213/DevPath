import { Link, useParams } from "react-router-dom";
import type { RuleCategoryScore, RuleEvidence, RuleResult } from "../features/analysis/api/analysisApi";
import { useAnalysisDetail } from "../features/analysis/model/useAnalysis";
import { ApiError } from "../shared/api/apiClient";

export function AnalysisDetailPage() {
  const { analysisId } = useParams();
  const query = useAnalysisDetail(analysisId);

  if (query.isPending) return <PageShell><p role="status">분석 결과와 근거를 불러오는 중입니다.</p></PageShell>;
  if (query.isError) return <PageShell><DetailError error={query.error} retry={() => query.refetch()} /></PageShell>;

  const { result, repository, evaluation, evidence, matrix } = query.data;
  return (
    <PageShell>
      <header className="workspace-header analysis-detail-header">
        <div>
          <p className="eyebrow">결정론적 분석 결과</p>
          <h1>{repository.fullName}</h1>
          <p>스냅샷과 규칙 버전이 고정되어 언제든 같은 입력과 정책으로 추적할 수 있는 공식 결과입니다.</p>
          <span className={`status-badge${result.currentForRepository ? " status-badge--active" : ""}`}>
            {result.currentForRepository ? "이 저장소의 현재 적용 결과" : "이 저장소의 과거 결과"}
          </span>
        </div>
        <div className="workspace-actions">
          <Link to={`/repositories/${repository.repositoryId}`}>저장소 보기</Link>
          <Link className="button-link" to="/analyses">분석 이력</Link>
        </div>
      </header>

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
        <div className="section-heading"><div><h2 id="category-results-title">영역별 공식 결과</h2><p>규칙을 펼치면 어떤 관찰값이 어떤 공식과 가중치로 반영됐는지 확인할 수 있습니다.</p></div></div>
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
        <div className="section-heading"><div><h2 id="historical-skills-title">이 분석의 역량 매트릭스</h2><p>현재 값으로 덮어쓰지 않는 당시의 공식 역량 평가입니다.</p></div></div>
        <div className="historical-skill-list">
          {matrix.skills.map(skill => (
            <article key={skill.assessmentId}>
              <div><strong>{skillName(skill.skillKey, skill.skillName)}</strong><span>{levelLabel(skill.level)}</span></div>
              <progress max="100" value={skill.score}>{skill.score}점</progress>
              <small>공식 점수 {formatScore(skill.score)} · 신뢰도 {formatScore(skill.confidence)}%</small>
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
          <div><dt>역량 매트릭스 ID</dt><dd><code>{result.skillMatrixId}</code></dd></div>
        </dl>
      </details>
    </PageShell>
  );
}

function CategoryResult({ category }: { category: RuleCategoryScore }) {
  return (
    <article className="category-result-card">
      <div className="category-result-card__heading"><div><span>{categoryLabel(category.category)}</span><strong>{formatScore(category.score)}점</strong></div><small>종합 점수 반영 비중 {formatPercent(category.weight)}</small></div>
      <progress max="100" value={category.score}>{category.score}점</progress>
      <p>근거 신뢰도 {formatScore(category.confidence)}%</p>
      <div className="rule-result-list">{category.ruleResults.map(rule => <RuleResultItem key={rule.ruleId} rule={rule} />)}</div>
      {category.missingEvidence.length > 0 ? <p className="missing-evidence">부족한 근거: {category.missingEvidence.map(ruleLabel).join(", ")}</p> : null}
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
  return <article><div><strong>{ruleLabel(evidence.ruleId)}</strong><span className="status-badge">{roleLabel(evidence.contributionRole)}</span></div><p>{evidence.observedFactSummary}</p><span>{evidenceTypeLabel(evidence.evidenceType)}</span><code>{evidence.sourceReference}</code><small>신뢰도 {formatScore(evidence.confidence)}%</small></article>;
}

function DetailError({ error, retry }: { error: Error; retry: () => void }) {
  if (error instanceof ApiError && error.status === 404) return <div className="state-panel" role="alert"><h1>분석 결과를 찾을 수 없습니다</h1><p>결과가 없거나 현재 계정으로 접근할 수 없습니다.</p><Link className="button-link" to="/analyses">분석 이력으로 이동</Link></div>;
  if (error instanceof ApiError && error.status === 401) return <div className="state-panel" role="alert"><h1>로그인이 필요합니다</h1><Link className="button-link" to="/">홈으로 이동</Link></div>;
  return <div className="state-panel" role="alert"><h1>분석 상세를 불러오지 못했습니다</h1><p>저장된 결과는 변경되지 않았습니다.</p><button type="button" onClick={retry}>다시 시도</button></div>;
}

function PageShell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/analyses">← 분석 이력</Link></nav>{children}</main>; }
function formatScore(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function formatPercent(value: number) { return `${formatScore(value * 100)}%`; }
function formatDateTime(value: string) { return new Intl.DateTimeFormat("ko-KR", { dateStyle: "long", timeStyle: "short" }).format(new Date(value)); }
function countScoredRules(categories: RuleCategoryScore[]) { return categories.reduce((count, category) => count + category.ruleResults.filter(rule => rule.status !== "SKIPPED" && rule.status !== "ERROR").length, 0); }
function categoryLabel(value: RuleCategoryScore["category"]) { return { LANGUAGE: "언어", FRAMEWORK: "프레임워크", TESTING: "테스트", DOCUMENTATION: "문서화", ACTIVITY: "개발 활동" }[value]; }
function ruleStatusLabel(value: RuleResult["status"]) { return { PASSED: "충족", FAILED: "미충족", PARTIAL: "부분 충족", SKIPPED: "평가 제외", ERROR: "평가 오류" }[value]; }
function roleLabel(value: RuleEvidence["contributionRole"]) { return { DIRECT: "직접 근거", SUPPORTING: "보조 근거", MISSING: "부족한 근거" }[value]; }
function evidenceTypeLabel(value: RuleEvidence["evidenceType"]) { return { REPOSITORY_PATH: "저장소 파일", LANGUAGE_STATISTIC: "언어 통계", SNAPSHOT_SIGNAL: "스냅샷 지표" }[value]; }
function levelLabel(value: string) { return { NONE: "관찰 없음", BEGINNER: "입문", DEVELOPING: "성장", COMPETENT: "충분", STRONG: "강점" }[value] ?? value; }
function skillName(key: string, fallback: string) { return { "language-engineering": "언어 활용", "framework-application": "프레임워크 적용", "testing-discipline": "테스트 역량", "technical-documentation": "기술 문서화", "development-activity": "개발 활동" }[key] ?? fallback; }

const ruleLabels: Record<string, string> = {
  LANGUAGE_PRIMARY_SHARE: "주 사용 언어 비중", LANGUAGE_DIVERSITY: "사용 언어 다양성", FRAMEWORK_COUNT: "프레임워크 사용 수",
  TEST_FILES: "테스트 파일", TEST_FRAMEWORKS: "테스트 프레임워크", CI_WORKFLOW_METADATA: "CI 워크플로",
  README_PRESENT: "README", API_DOCUMENTATION: "API 문서", ARCHITECTURE_DOCUMENTATION: "아키텍처 문서",
  CONTRIBUTING_GUIDE: "기여 가이드", LICENSE_PRESENT: "라이선스", COMMIT_COUNT: "커밋 수",
  CONTRIBUTOR_COUNT: "기여자 수", BRANCH_COUNT: "브랜치 수"
};
function ruleLabel(ruleId: string) { return ruleLabels[ruleId] ?? ruleId; }
function formulaLabel(formulaId: string) { return ({ BOOLEAN_100: "존재하면 100점", LINEAR_0_100: "관찰값을 구간 안에서 0~100점으로 환산", COUNT_CAPPED_100: "개수를 상한까지 0~100점으로 환산" } as Record<string, string>)[formulaId] ?? formulaId; }
function formatObservedValue(rule: RuleResult) { return rule.formulaId === "BOOLEAN_100" ? (rule.rawValue > 0 ? "있음" : "없음") : formatScore(rule.rawValue); }
function ruleReason(rule: RuleResult) { return `${ruleLabel(rule.ruleId)}의 관찰값은 ${formatObservedValue(rule)}이며, '${formulaLabel(rule.formulaId)}' 공식을 적용한 공식 규칙 점수는 ${formatScore(rule.score)}점입니다.`; }
