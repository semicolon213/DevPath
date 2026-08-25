import { Link, useParams } from "react-router-dom";
import type { SkillAssessment } from "../features/skills/api/skillMatrixApi";
import { useSkillWorkspace } from "../features/skills/model/useSkillMatrix";
import { ApiError } from "../shared/api/apiClient";

export function SkillDetailPage() {
  const { skillId } = useParams();
  const query = useSkillWorkspace(skillId);
  if (query.isPending) return <Shell><p role="status">기술 상세와 연결된 증거를 불러오는 중입니다.</p></Shell>;
  if (query.isError) return <Shell><ErrorState error={query.error} retry={() => query.refetch()} /></Shell>;

  const { detail, evidence } = query.data;
  const skill = detail.skill;
  return <Shell>
    <header className="workspace-header skill-header"><div><p className="eyebrow">Current Authoritative Skill</p>
      <h1>{skill.skillName}</h1><p>현재 적용 중인 불변 Skill Matrix에서 가져온 공식 평가와 정규화된 증거입니다.</p></div>
      <div className="analysis-version" aria-label="기술 평가 버전"><span>정책 {detail.policyVersion}</span><span>규칙 {detail.ruleSetVersion}</span></div>
    </header>
    <section className="skill-summary" aria-label="현재 기술 평가 요약">
      <Summary label="공식 점수" value={format(skill.score)} /><Summary label="등급" value={levelLabel(skill.level)} />
      <Summary label="신뢰도" value={`${format(skill.confidence)}%`} />
    </section>
    <section className="analysis-notice"><div><h2>결정론적 평가 기준</h2><p>점수와 등급은 서버에 저장된 값입니다. 브라우저는 계산하거나 수정하지 않습니다.</p></div>
      <time dateTime={detail.generatedAt}>{formatDate(detail.generatedAt)} 생성</time></section>
    <section aria-labelledby="skill-facts-title"><div className="section-heading"><div><h2 id="skill-facts-title">평가 추적 정보</h2></div></div>
      <dl className="detail-grid"><Fact label="범주" value={skill.category} /><Fact label="상태" value={stateLabel(skill)} />
        <Fact label="성장 상태" value={growthLabel(skill.growthTrend)} /><Fact label="평가 참조" value={skill.aggregateRuleResultReference} />
        <Fact label="연결 저장소" value={`${skill.repositoryIds.length}개`} /><Fact label="Matrix ID" value={detail.skillMatrixId} /></dl></section>
    <section aria-labelledby="skill-evidence-title"><div className="section-heading"><div><h2 id="skill-evidence-title">연결된 증거</h2>
      <p>사용자 소유의 불변 스냅샷에서 정규화된 관찰 사실만 표시합니다.</p></div></div>
      {evidence.evidence.length === 0 ? <div className="state-panel"><p>직접 연결된 증거가 없습니다.</p></div> :
        <div className="skill-grid">{evidence.evidence.map(item => <article className="skill-card" key={item.evidenceId}>
          <div className="skill-card__heading"><div><span className="skill-category">{item.evidenceType}</span><h3>{item.observedFactSummary}</h3></div></div>
          <dl className="skill-metadata"><div><dt>신뢰도</dt><dd>{format(item.confidence)}%</dd></div><div><dt>스냅샷</dt><dd><code>{item.snapshotId}</code></dd></div></dl>
          <details><summary>증거 참조 보기</summary><code>{item.sourceReference}</code></details>
        </article>)}</div>}
    </section>
    <div className="workspace-actions"><Link to="/skills">전체 기술 역량</Link><Link to="/analyses">분석 이력</Link></div>
  </Shell>;
}

function ErrorState({ error, retry }: { error: Error; retry: () => void }) {
  if (error instanceof ApiError && error.status === 401) return <State title="로그인이 필요합니다." to="/" />;
  if (error instanceof ApiError && error.status === 404) return <State title="현재 Matrix에서 기술 평가를 찾을 수 없습니다." />;
  return <section className="state-panel" role="alert"><h1>기술 상세를 불러오지 못했습니다.</h1><button type="button" onClick={retry}>다시 시도</button></section>;
}
function State({ title, to = "/skills" }: { title: string; to?: string }) { return <section className="state-panel" role="alert"><h1>{title}</h1><Link className="button-link" to={to}>돌아가기</Link></section>; }
function Shell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/dashboard">대시보드</Link></nav>{children}</main>; }
function Summary({ label, value }: { label: string; value: string }) { return <div><strong>{value}</strong><span>{label}</span></div>; }
function Fact({ label, value }: { label: string; value: string }) { return <div><dt>{label}</dt><dd>{value}</dd></div>; }
function stateLabel(skill: SkillAssessment) { return skill.strength ? "강점" : skill.weakness ? "개선 필요" : "성장 중"; }
function levelLabel(level: SkillAssessment["level"]) { return { NONE: "근거 없음", BEGINNER: "입문", DEVELOPING: "성장", COMPETENT: "충분", STRONG: "강점" }[level]; }
function growthLabel(value: SkillAssessment["growthTrend"]) { return { UNAVAILABLE: "비교 데이터 없음", IMPROVING: "향상", STABLE: "유지", DECLINING: "감소" }[value]; }
function format(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
function formatDate(value: string) { return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
