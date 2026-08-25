import { Link, useParams } from "react-router-dom";
import { useCareer } from "../features/careers/model/useCareers";
import { usePreferences, useSetCareer } from "../features/profile/model/useProfile";
import { ApiError } from "../shared/api/apiClient";

export function CareerDetailPage() {
  const { careerId } = useParams();
  const career = useCareer(careerId);
  const preferences = usePreferences();
  const selection = useSetCareer();
  if (career.isPending || preferences.isPending) return <PageShell><p role="status">커리어 프로필을 불러오는 중입니다.</p></PageShell>;
  if (career.isError || preferences.isError) return <PageShell><ErrorState error={career.error ?? preferences.error} retry={() => { void career.refetch(); void preferences.refetch(); }} /></PageShell>;
  const value = career.data;
  const selected = preferences.data.careerId === value.careerId;

  return <PageShell>
    <header className="workspace-header">
      <div><p className="eyebrow">{value.profileVersion} · {statusLabel(value.status)}</p><h1>{value.localizedName}</h1><p>{value.purpose}</p></div>
      <button disabled={selected || selection.isPending} onClick={() => selection.mutate(value.careerId)}>{selected ? "현재 목표 직무" : selection.isPending ? "설정 중…" : "이 직무를 목표로 설정"}</button>
    </header>
    {selection.isSuccess ? <p className="analysis-notice" role="status">목표 직무를 변경했습니다. 다음 커리어 평가는 새 프로필 기준을 사용합니다.</p> : null}
    {selection.isError ? <p className="analysis-warning" role="alert">목표 직무를 변경하지 못했습니다.</p> : null}
    <section className="career-profile-grid">
      <ProfileList title="핵심 기술" values={value.coreTechnologies} />
      <ProfileList title="필수 역량" values={value.requiredCompetencies} />
      <ProfileList title="우대 역량" values={value.preferredCompetencies} />
    </section>
    <section className="career-roadmap" aria-labelledby="career-roadmap-title"><div className="section-heading"><div><h2 id="career-roadmap-title">권장 학습 순서</h2><p>공식 준비도와 추천은 아직 계산하지 않으며, 활성 프로필에 저장된 순서입니다.</p></div></div><ol>{value.roadmapTemplate.map((step, index) => <li key={step}><span>{index + 1}</span><strong>{step}</strong></li>)}</ol></section>
    <section aria-labelledby="career-policy-title"><div className="section-heading"><div><h2 id="career-policy-title">평가 강조 영역</h2><p>향후 커리어 엔진이 참조할 버전 고정 정책입니다. 기술 점수 자체를 변경하지 않습니다.</p></div></div><dl className="career-weight-list">{Object.entries(value.priorityWeights).map(([category, priority]) => <div key={category}><dt>{categoryLabel(category)}</dt><dd>{priorityLabel(priority)}</dd></div>)}</dl></section>
  </PageShell>;
}

function ProfileList({ title, values }: { title: string; values: string[] }) { return <article><h2>{title}</h2><ul>{values.map(value => <li key={value}>{value}</li>)}</ul></article>; }
function ErrorState({ error, retry }: { error: Error | null; retry: () => void }) { if (error instanceof ApiError && error.status === 404) return <div className="state-panel" role="alert"><h1>지원하는 커리어가 아닙니다</h1><Link to="/careers">지원 커리어 보기</Link></div>; if (error instanceof ApiError && error.status === 401) return <div className="state-panel" role="alert"><h1>로그인이 필요합니다</h1><Link to="/">홈으로 이동</Link></div>; return <div className="state-panel" role="alert"><h1>커리어 프로필을 불러오지 못했습니다</h1><button onClick={retry}>다시 시도</button></div>; }
function PageShell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/careers">← 지원 커리어</Link></nav>{children}</main>; }
function statusLabel(value: string) { return value === "SUPPORTED" ? "공식 지원" : value; }
function categoryLabel(value: string) { return ({ LANGUAGE: "언어", FRAMEWORK: "프레임워크", TESTING: "테스트", DOCUMENTATION: "문서화", ACTIVITY: "개발 활동" } as Record<string, string>)[value] ?? value; }
function priorityLabel(value: string) { return ({ HIGH: "높음", MEDIUM: "보통", LOW: "낮음" } as Record<string, string>)[value] ?? value; }
