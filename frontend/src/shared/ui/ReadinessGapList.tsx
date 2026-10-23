import { Link } from "react-router-dom";
import type { SkillGap } from "../../features/readiness/api/careerReadinessApi";
import { categoryLabel, gapStateLabel } from "./labels";

export function ReadinessGapList({ gaps }: { gaps: SkillGap[] }) {
  return <>
    <div className="readiness-legend" role="group" aria-label="역량 막대 범례"><span><i className="readiness-legend__fill" />현재 점수</span><span><i className="readiness-legend__reference" />기준선</span><span><i className="readiness-legend__empty" />근거 없음</span></div>
    <div className="readiness-gap-list">{gaps.map(gap => <GapRow key={gap.skillGapId} gap={gap} />)}</div>
  </>;
}

function GapRow({ gap }: { gap: SkillGap }) {
  const missing = gap.gapState === "MISSING";
  return <article className={`readiness-gap readiness-gap--${gap.gapState.toLowerCase()}`}>
    <div><span>{categoryLabel(gap.category)}</span><strong>{gapStateLabel(gap.gapState)}</strong></div>
    <p>{missing ? <>기준 {format(gap.expectedMinimum)} · {gapStateLabel(gap.gapState)}</> : <><b>{format(gap.actualScore)}</b> / 기준 {format(gap.expectedMinimum)} · {gapStateLabel(gap.gapState)}</>}</p>
    {missing
      ? <><div className="readiness-gap__track readiness-gap__track--empty" role="img" aria-label={`${categoryLabel(gap.category)}: ${gapStateLabel(gap.gapState)}`} /><span className="readiness-gap__guidance">관련 근거를 연결하면 평가됩니다.</span></>
      : <div className="readiness-gap__track" role="img" aria-label={`${categoryLabel(gap.category)} 실제 점수 ${format(gap.actualScore)}점, 기준 ${format(gap.expectedMinimum)}점`}><span className="readiness-gap__fill" style={{ width: `${gap.actualScore}%` }} /><span className={`readiness-gap__reference${gap.expectedMinimum >= 100 ? " readiness-gap__reference--end" : ""}`} style={gap.expectedMinimum < 100 ? { left: `${gap.expectedMinimum}%` } : undefined} /></div>}
    <small>직무 가중치 {format(gap.careerWeight)}% · 근거 {gap.evidenceIds.length}개</small>
    <Link to={`/skills/${gap.skillId}`}>기술 평가와 근거 보기</Link>
  </article>;
}

function format(value: number) { return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value); }
