import { Link, useSearchParams } from "react-router-dom";
import type { SkillAssessment, SkillMatrix } from "../features/skills/api/skillMatrixApi";
import { useSkillMatrixComparison } from "../features/skills/model/useSkillMatrix";
import { ApiError } from "../shared/api/apiClient";

export function SkillMatrixComparisonPage() {
  const [params] = useSearchParams();
  const ids = params.getAll("skillMatrixId");
  const valid = ids.length === 2 && ids[0] !== ids[1];
  const query = useSkillMatrixComparison(ids);

  if (!valid) return <Shell><State title="비교할 Skill Matrix 두 개를 선택해 주세요." /></Shell>;
  if (query.isPending) return <Shell><p role="status">저장된 기술 역량 결과를 불러오는 중입니다.</p></Shell>;
  if (query.isError) return <Shell><ErrorState error={query.error} retry={() => query.refetch()} /></Shell>;

  const [left, right] = query.data.matrices;
  const skillKeys = Array.from(new Set([...left.skills, ...right.skills].map(skill => skill.skillKey)));

  return <Shell>
    <header className="workspace-header">
      <div><p className="eyebrow">Authoritative Skill Comparison</p><h1>기술 역량 비교</h1>
        <p>불변 Skill Matrix 두 개의 공식 점수와 등급을 나란히 표시합니다. 이 화면은 차이 점수나 성장 추세를 새로 계산하지 않습니다.</p></div>
      <Link className="button-link" to="/analyses">다른 결과 선택</Link>
    </header>
    <section className="comparison-hero" aria-label="Skill Matrix 비교 대상">
      <MatrixSummary label="Matrix A" matrix={left} /><div aria-hidden="true">VS</div><MatrixSummary label="Matrix B" matrix={right} />
    </section>
    <section aria-labelledby="skill-comparison-title">
      <div className="section-heading"><div><h2 id="skill-comparison-title">역량별 저장 결과</h2>
        <p>점수, 등급, 신뢰도, 증거 개수는 각 Matrix에 저장된 값을 그대로 보여줍니다.</p></div></div>
      <div className="comparison-table-wrap"><table className="comparison-table"><thead><tr><th scope="col">역량</th><th scope="col">Matrix A</th><th scope="col">Matrix B</th></tr></thead>
        <tbody>{skillKeys.map(key => <SkillRow key={key} skillKey={key} left={findSkill(left, key)} right={findSkill(right, key)} />)}</tbody>
      </table></div>
    </section>
    <section className="comparison-rule-grid" aria-label="비교 재현 정보"><MatrixFacts label="Matrix A" matrix={left} /><MatrixFacts label="Matrix B" matrix={right} /></section>
  </Shell>;
}

function MatrixSummary({ label, matrix }: { label: string; matrix: SkillMatrix }) {
  return <article><span>{label}</span><h2>{matrix.status === "CURRENT" ? "현재 적용" : "과거 결과"}</h2>
    <div><strong>{matrix.skills.length}</strong><span>개 역량</span></div><time dateTime={matrix.generatedAt}>{formatDate(matrix.generatedAt)}</time></article>;
}
function SkillRow({ skillKey, left, right }: { skillKey: string; left?: SkillAssessment; right?: SkillAssessment }) {
  const name = left?.skillName ?? right?.skillName ?? skillKey;
  return <tr><th scope="row"><strong>{name}</strong><small>{left?.category ?? right?.category}</small></th><SkillCell skill={left} /><SkillCell skill={right} /></tr>;
}
function SkillCell({ skill }: { skill?: SkillAssessment }) {
  return <td>{skill ? <><strong>{formatScore(skill.score)}</strong><small>{levelLabel(skill.level)} · 신뢰도 {formatScore(skill.confidence)}% · 증거 {skill.evidenceIds.length}개</small></> : <span>없음</span>}</td>;
}
function MatrixFacts({ label, matrix }: { label: string; matrix: SkillMatrix }) {
  return <article><h2>{label} 재현 정보</h2><dl><div><dt>Matrix ID</dt><dd><code>{matrix.skillMatrixId}</code></dd></div><div><dt>정책 버전</dt><dd>{matrix.policyVersion}</dd></div><div><dt>규칙 버전</dt><dd>{matrix.ruleSetVersion}</dd></div><div><dt>강점</dt><dd>{matrix.strengths.length}개</dd></div><div><dt>개선 영역</dt><dd>{matrix.weaknesses.length}개</dd></div></dl></article>;
}
function ErrorState({ error, retry }: { error: Error; retry: () => void }) {
  if (error instanceof ApiError && error.status === 401) return <State title="로그인이 필요합니다." to="/" />;
  if (error instanceof ApiError && error.status === 404) return <State title="비교할 Skill Matrix를 찾을 수 없습니다." />;
  return <section className="state-panel" role="alert"><h1>기술 역량 비교를 불러오지 못했습니다.</h1><button type="button" onClick={retry}>다시 시도</button></section>;
}
function State({ title, to = "/analyses" }: { title: string; to?: string }) { return <section className="state-panel" role="alert"><h1>{title}</h1><Link className="button-link" to={to}>분석 이력으로 이동</Link></section>; }
function Shell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/dashboard">대시보드</Link></nav>{children}</main>; }
function findSkill(matrix: SkillMatrix, key: string) { return matrix.skills.find(skill => skill.skillKey === key); }
function levelLabel(level: SkillAssessment["level"]) { return { NONE: "근거 없음", BEGINNER: "입문", DEVELOPING: "성장", COMPETENT: "충분", STRONG: "강점" }[level]; }
function formatScore(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function formatDate(value: string) { return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
