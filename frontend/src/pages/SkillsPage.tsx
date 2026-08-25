import { Link } from "react-router-dom";
import type { SkillAssessment } from "../features/skills/api/skillMatrixApi";
import { useCurrentSkillMatrix } from "../features/skills/model/useSkillMatrix";
import { ApiError } from "../shared/api/apiClient";

export function SkillsPage() {
  const query = useCurrentSkillMatrix();

  if (query.isPending) {
    return <PageShell><p role="status">최신 기술 분석 결과를 불러오는 중입니다…</p></PageShell>;
  }
  if (query.isError) {
    return <PageShell><SkillMatrixError error={query.error} retry={() => query.refetch()} /></PageShell>;
  }

  const matrix = query.data;
  const strengths = matrix.skills.filter(skill => skill.strength);
  const weaknesses = matrix.skills.filter(skill => skill.weakness);

  return (
    <PageShell>
      <header className="workspace-header skill-header">
        <div>
          <p className="eyebrow">결정론적 Rule Engine 결과</p>
          <h1>기술 역량 분석</h1>
          <p>저장소에서 확인된 근거를 버전이 고정된 평가 규칙으로 계산한 공식 결과입니다.</p>
        </div>
        <div className="analysis-version" aria-label="분석 버전 정보">
          <span>정책 {matrix.policyVersion}</span>
          <span>규칙 {matrix.ruleSetVersion}</span>
        </div>
      </header>

      <section className="skill-summary" aria-label="기술 분석 요약">
        <Summary label="평가된 역량" value={matrix.skills.length} />
        <Summary label="확인된 강점" value={strengths.length} />
        <Summary label="개선할 영역" value={weaknesses.length} />
      </section>

      <section className="analysis-notice" aria-labelledby="analysis-basis-title">
        <div>
          <h2 id="analysis-basis-title">분석 기준</h2>
          <p>점수와 등급은 AI가 아닌 Rule Engine이 계산합니다. 근거가 부족한 항목은 낮은 점수와 별도로 신뢰도를 함께 확인하세요.</p>
        </div>
        <time dateTime={matrix.generatedAt}>{formatDateTime(matrix.generatedAt)} 생성</time>
      </section>

      <section aria-labelledby="skill-list-title">
        <div className="section-heading">
          <div>
            <h2 id="skill-list-title">역량별 결과</h2>
            <p>각 항목에서 점수, 신뢰도, 연결된 근거를 함께 확인할 수 있습니다.</p>
          </div>
        </div>
        <div className="skill-grid">
          {matrix.skills.map(skill => <SkillCard key={skill.assessmentId} skill={skill} />)}
        </div>
      </section>

      <section className="skill-next-action" aria-labelledby="next-action-title">
        <div>
          <h2 id="next-action-title">다음 분석을 준비하세요</h2>
          <p>저장소를 다시 동기화하면 새로운 불변 스냅샷과 평가 결과를 기준으로 분석할 수 있습니다.</p>
        </div>
        <div className="workspace-actions"><Link to="/analyses">분석 이력</Link><Link className="button-link" to="/repositories">저장소 확인</Link></div>
      </section>
    </PageShell>
  );
}

function SkillCard({ skill }: { skill: SkillAssessment }) {
  const state = skill.strength ? "강점" : skill.weakness ? "개선 필요" : "성장 중";
  return (
    <article className={`skill-card skill-card--${skill.strength ? "strength" : skill.weakness ? "weakness" : "neutral"}`}>
      <div className="skill-card__heading">
        <div>
          <span className="skill-category">{categoryLabel(skill.category)}</span>
          <h3><Link to={`/skills/${skill.skillId}`}>{skillName(skill)}</Link></h3>
        </div>
        <span className="status-badge">{state}</span>
      </div>
      <div className="skill-score-row">
        <strong>{formatScore(skill.score)}</strong>
        <span>{levelLabel(skill.level)}</span>
      </div>
      <label className="skill-meter">
        <span>공식 점수</span>
        <progress max="100" value={skill.score}>{skill.score}점</progress>
      </label>
      <dl className="skill-metadata">
        <div><dt>근거 신뢰도</dt><dd>{formatScore(skill.confidence)}%</dd></div>
        <div><dt>연결된 근거</dt><dd>{skill.evidenceIds.length}개</dd></div>
        <div><dt>분석 저장소</dt><dd>{skill.repositoryIds.length}개</dd></div>
        <div><dt>성장 추세</dt><dd>{growthLabel(skill.growthTrend)}</dd></div>
      </dl>
      <details>
        <summary>근거와 계산 기준 보기</summary>
        <dl className="evidence-details">
          <div><dt>평가 참조</dt><dd><code>{skill.aggregateRuleResultReference}</code></dd></div>
          <div><dt>규칙 버전</dt><dd>{skill.ruleSetVersion}</dd></div>
          <div><dt>근거 ID</dt><dd>{skill.evidenceIds.length ? skill.evidenceIds.join(", ") : "직접 근거 없음"}</dd></div>
        </dl>
      </details>
    </article>
  );
}

function SkillMatrixError({ error, retry }: { error: Error; retry: () => void }) {
  if (error instanceof ApiError && error.status === 404) {
    return (
      <div className="state-panel">
        <h1>아직 기술 분석 결과가 없습니다</h1>
        <p>GitHub 저장소를 동기화하고 결정론적 분석이 완료되면 이곳에 결과가 표시됩니다.</p>
        <Link className="button-link" to="/repositories">저장소로 이동</Link>
      </div>
    );
  }
  if (error instanceof ApiError && error.status === 401) {
    return (
      <div className="state-panel" role="alert">
        <h1>로그인이 필요합니다</h1>
        <p>DevPath 홈에서 GitHub로 다시 로그인한 뒤 기술 분석을 확인해 주세요.</p>
        <Link className="button-link" to="/">로그인 화면으로 이동</Link>
      </div>
    );
  }
  return (
    <div className="state-panel" role="alert">
      <h1>기술 분석을 불러오지 못했습니다</h1>
      <p>기존 결과는 변경되지 않았습니다. 서버 연결을 확인한 뒤 다시 시도해 주세요.</p>
      <button type="button" onClick={retry}>다시 시도</button>
    </div>
  );
}

function PageShell({ children }: { children: React.ReactNode }) {
  return <main className="shell workspace"><nav><Link to="/">← DevPath 홈</Link></nav>{children}</main>;
}

function Summary({ label, value }: { label: string; value: number }) {
  return <div><strong>{value}</strong><span>{label}</span></div>;
}

const koreanSkillNames: Record<string, string> = {
  "language-engineering": "언어 활용",
  "framework-application": "프레임워크 적용",
  "database-engineering": "데이터베이스 엔지니어링",
  "architecture-structure": "아키텍처 구조",
  "testing-discipline": "테스트 역량",
  "delivery-operations": "전달 및 운영",
  "technical-documentation": "기술 문서화",
  "development-activity": "개발 활동"
};

function skillName(skill: SkillAssessment) {
  return koreanSkillNames[skill.skillKey] ?? skill.skillName;
}

function categoryLabel(category: SkillAssessment["category"]) {
  return { LANGUAGE: "언어", FRAMEWORK: "프레임워크", DATABASE: "데이터베이스", ARCHITECTURE: "아키텍처", TESTING: "테스트", DEVOPS: "DevOps", DOCUMENTATION: "문서화", ACTIVITY: "활동" }[category];
}

function levelLabel(level: SkillAssessment["level"]) {
  return { NONE: "관찰 없음", BEGINNER: "입문", DEVELOPING: "성장", COMPETENT: "충분", STRONG: "강점" }[level];
}

function growthLabel(trend: SkillAssessment["growthTrend"]) {
  return { UNAVAILABLE: "비교 데이터 없음", IMPROVING: "향상", STABLE: "유지", DECLINING: "감소" }[trend];
}

function formatScore(value: number) {
  return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value);
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
