import { Link } from "react-router-dom";
import type { SkillAssessment } from "../features/skills/api/skillMatrixApi";
import { useCurrentSkillMatrix } from "../features/skills/model/useSkillMatrix";
import { ApiError } from "../shared/api/apiClient";
import { SkillExplanationPanel } from "../features/ai/ui/SkillExplanationPanel";
import { categoryLabel, growthTrendLabel, levelLabel, skillNameLabel, skillStateLabel } from "../shared/ui/labels";
import { PageState } from "../shared/ui/PageState";
import { PageHero } from "../shared/ui/PageHero";
import { ResultBasis } from "../shared/ui/ResultBasis";
import { EvidenceStatus } from "../shared/ui/EvidenceStatus";

export function SkillsPage() {
  const query = useCurrentSkillMatrix();

  if (query.isPending) {
    return <PageState state="loading" title="기술 역량" message="최신 기술 역량 결과를 불러오는 중입니다." />;
  }
  if (query.isError) {
    return query.error instanceof ApiError && query.error.status === 404
      ? <PageState state="empty" title="아직 기술 역량 결과가 없습니다" description="GitHub 저장소를 동기화하고 분석이 완료되면 이곳에 결과가 표시됩니다." actionTo="/repositories" actionLabel="저장소 확인" />
      : <PageState state="error" title="기술 역량 결과를 표시할 수 없습니다" errorTitle="기술 역량을 불러오지 못했습니다" description="기존 결과는 변경되지 않았습니다." error={query.error} retry={() => query.refetch()} />;
  }

  const matrix = query.data;
  const strengths = matrix.skills.filter(skill => skill.strength);
  const weaknesses = matrix.skills.filter(skill => skill.weakness);

  return (
    <>
      <PageHero>
        <div>
          <p className="eyebrow">공식 기술 역량 결과</p>
          <h1>기술 역량</h1>
          <p>저장소에서 확인된 근거를 버전이 고정된 평가 규칙으로 계산한 공식 결과입니다.</p>
        </div>
        <div>
          <Link className="button-link button-primary" to="/repositories">저장소 확인</Link>
        </div>
      </PageHero>

      <section className="skill-summary" aria-label="기술 분석 요약">
        <Summary label="평가된 역량" value={matrix.skills.length} />
        <Summary label="확인된 강점" value={strengths.length} />
        <Summary label="개선할 영역" value={weaknesses.length} />
      </section>

      <section aria-labelledby="skill-list-title">
        <div className="section-heading">
          <div>
            <h2 id="skill-list-title">역량별 결과</h2>
            <p>각 항목에서 점수, 신뢰도, 연결된 근거를 함께 확인할 수 있습니다.</p>
          </div>
        </div>
        <div className="skill-list">
          {matrix.skills.map(skill => <SkillCard key={skill.assessmentId} skill={skill} />)}
        </div>
      </section>

      <section className="skill-next-action" aria-labelledby="next-action-title">
        <div>
          <h2 id="next-action-title">다음 분석을 준비하세요</h2>
          <p>저장소를 다시 동기화하면 새로운 불변 스냅샷과 평가 결과를 기준으로 분석할 수 있습니다.</p>
        </div>
        <div className="workspace-actions"><Link to="/analyses">분석 이력</Link></div>
      </section>

      <SkillExplanationPanel skillMatrixId={matrix.skillMatrixId} />
      <ResultBasis items={[{ label: "분석 기준 시각", value: <time dateTime={matrix.generatedAt}>{formatDateTime(matrix.generatedAt)}</time> }, { label: "Skill Matrix 정책", value: matrix.policyVersion }, { label: "규칙 버전", value: matrix.ruleSetVersion }]} />
    </>
  );
}

function SkillCard({ skill }: { skill: SkillAssessment }) {
  const state = skillStateLabel(skill.strength, skill.weakness);
  return (
    <article className={`skill-card skill-card--${skill.strength ? "strength" : skill.weakness ? "weakness" : "neutral"}`}>
      <div className="skill-card__heading">
        <div>
          <span className="skill-category">{categoryLabel(skill.category)}</span>
          <h3><Link to={`/skills/${skill.skillId}`}>{skillNameLabel(skill.skillKey, skill.skillName)}</Link></h3>
        </div>
      </div>
      <div className="skill-card__score">{skill.evidenceStatus === "NONE" ? <div className="skill-score-empty"><strong>—</strong><span>관련 저장소나 문서를 연결하면 다음 분석에서 평가됩니다.</span><small>{levelLabel(skill.level)}</small></div> : <div className="skill-score-row">
        <strong>{formatScore(skill.score)}</strong><label className="skill-meter"><span className="visually-hidden">공식 점수</span><progress max="100" value={skill.score}>{skill.score}점</progress></label><span>{levelLabel(skill.level)}</span>
      </div>}</div>
      <div className="skill-card__status"><EvidenceStatus status={skill.evidenceStatus} guidance={false} /><span className="skill-flag">{state}</span></div>
      <details>
        <summary>세부 지표와 근거 {skill.evidenceIds.length}개 보기</summary>
        <dl className="evidence-details">
          <div><dt>신뢰도</dt><dd>{formatScore(skill.confidence)}%</dd></div>
          <div><dt>분석 저장소</dt><dd>{skill.repositoryIds.length}개</dd></div>
          <div><dt>성장 추세</dt><dd>{growthTrendLabel(skill.growthTrend)}</dd></div>
          <div><dt>평가 참조</dt><dd><code>{skill.aggregateRuleResultReference}</code></dd></div>
          <div><dt>규칙 버전</dt><dd>{skill.ruleSetVersion}</dd></div>
          {skill.evidenceStatus === "NONE" ? <div><dt>저장된 공식 점수</dt><dd>{formatScore(skill.score)}</dd></div> : null}
          {skill.evidenceIds.length ? <div><dt>근거 ID</dt><dd>{skill.evidenceIds.join(", ")}</dd></div> : null}
        </dl>
      </details>
    </article>
  );
}

function Summary({ label, value }: { label: string; value: number }) {
  return <div><strong>{value}</strong><span>{label}</span></div>;
}

function formatScore(value: number) {
  return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value);
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
