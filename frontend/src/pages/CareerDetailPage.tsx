import { useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { useCareer } from "../features/careers/model/useCareers";
import { usePreferences, useSetCareer } from "../features/profile/model/useProfile";
import { careerStatusLabel, categoryLabel, priorityLabel } from "../shared/ui/labels";
import { PageState } from "../shared/ui/PageState";
import { Breadcrumbs } from "../shared/ui/Breadcrumbs";
import { PageHero } from "../shared/ui/PageHero";
import { StatusChip } from "../shared/ui/StatusChip";
import { ConfirmDialog } from "../shared/ui/ConfirmDialog";

export function CareerDetailPage() {
  const { careerId } = useParams();
  const career = useCareer(careerId);
  const preferences = usePreferences();
  const selection = useSetCareer();
  const [confirming, setConfirming] = useState(false);
  const selectedStatus = useRef<HTMLSpanElement>(null);
  if (career.isPending || preferences.isPending) return <PageShell><PageState state="loading" title="커리어 프로필" message="커리어 프로필을 불러오는 중입니다." /></PageShell>;
  if (career.isError || preferences.isError) return <PageShell><PageState state="error" title="지원하는 커리어를 표시할 수 없습니다" errorTitle="커리어 프로필을 불러오지 못했습니다" error={career.error ?? preferences.error} retry={() => { void career.refetch(); void preferences.refetch(); }} backTo="/careers" backLabel="지원 커리어 보기" /></PageShell>;
  const value = career.data;
  const selected = preferences.data.careerId === value.careerId;

  return <PageShell>
    <PageHero>
      <div><p className="eyebrow">프로필 버전 {value.profileVersion} · {careerStatusLabel(value.status)}</p><h1>{value.localizedName}</h1><p>{value.purpose}</p></div>
      {selected ? <span ref={selectedStatus} tabIndex={-1}><StatusChip variant="info">현재 목표 직무</StatusChip></span> : <button className="button-primary" onClick={() => { selection.reset(); setConfirming(true); }}>이 직무를 목표로 설정</button>}
    </PageHero>
    {selection.isSuccess ? <p className="analysis-notice" role="status">목표 직무를 변경했습니다. 다음 커리어 평가는 새 프로필 기준을 사용합니다.</p> : null}
    <section className="profile-section-list">
      <ProfileList title="핵심 기술" values={value.coreTechnologies} />
      <ProfileList title="필수 역량" values={value.requiredCompetencies} />
      <ProfileList title="우대 역량" values={value.preferredCompetencies} />
    </section>
    <section className="career-roadmap" aria-labelledby="career-roadmap-title"><div className="section-heading"><div><h2 id="career-roadmap-title">권장 학습 순서</h2><p>공식 준비도와 추천은 아직 계산하지 않으며, 활성 프로필에 저장된 순서입니다.</p></div></div><ol>{value.roadmapTemplate.map((step, index) => <li key={step}><span>{index + 1}</span><strong>{step}</strong></li>)}</ol></section>
    <section aria-labelledby="career-policy-title"><div className="section-heading"><div><h2 id="career-policy-title">평가 강조 영역</h2><p>향후 커리어 엔진이 참조할 버전 고정 정책입니다. 기술 점수 자체를 변경하지 않습니다.</p></div></div><dl className="career-weight-list">{Object.entries(value.priorityWeights).map(([category, priority]) => <div key={category}><dt>{categoryLabel(category)}</dt><dd>{priorityLabel(priority)}</dd></div>)}</dl></section>
    <ConfirmDialog open={confirming} title="목표 직무를 변경할까요?" description="선택한 직무는 이후 분석 흐름에 적용되며 준비도와 추천이 새 직무 기준으로 다시 계산될 수 있습니다. 이 화면에서는 재평가를 시작하지 않습니다." confirmLabel="목표 직무 변경" pending={selection.isPending} error={selection.isError ? "목표 직무를 변경하지 못했습니다. 다시 시도해 주세요." : undefined} returnFocusFallback={selectedStatus} onCancel={() => setConfirming(false)} onConfirm={() => selection.mutate(value.careerId, { onSuccess: () => setConfirming(false) })} />
  </PageShell>;
}

function ProfileList({ title, values }: { title: string; values: string[] }) { return <article><h2>{title}</h2><ul>{values.map(value => <li key={value}>{value}</li>)}</ul></article>; }
function PageShell({ children }: { children: React.ReactNode }) { return <><Breadcrumbs items={[{ label: "지원 커리어", to: "/careers" }, { label: "커리어 상세" }]} />{children}</>; }
