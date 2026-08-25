import { Link } from "react-router-dom";
import { useCareers } from "../features/careers/model/useCareers";
import { usePreferences } from "../features/profile/model/useProfile";
import { ApiError } from "../shared/api/apiClient";

export function CareersPage() {
  const careers = useCareers();
  const preferences = usePreferences();
  if (careers.isPending || preferences.isPending) return <PageShell><p role="status">커리어 프로필을 불러오는 중입니다.</p></PageShell>;
  if (careers.isError || preferences.isError) return <PageShell><ErrorState error={careers.error ?? preferences.error} retry={() => { void careers.refetch(); void preferences.refetch(); }} /></PageShell>;

  return <PageShell>
    <header className="workspace-header">
      <div><p className="eyebrow">버전이 고정된 평가 기준</p><h1>지원 커리어</h1><p>목표 직무별 핵심 기술과 기대 역량을 먼저 확인하세요. 이 화면은 준비도를 계산하지 않으며, 활성 커리어 프로필의 기준만 보여줍니다.</p></div>
      <Link className="button-link" to="/">목표 직무 설정</Link>
    </header>
    <section className="career-catalog-notice" aria-label="커리어 카탈로그 정보">
      <strong>{careers.data.careers.length}개 지원 직무</strong><span>프로필 버전 {careers.data.catalogVersion}</span>
    </section>
    <div className="career-grid">
      {careers.data.careers.map(career => <article className={preferences.data?.careerId === career.careerId ? "career-card career-card--selected" : "career-card"} key={career.careerId}>
        <div><span className="status-badge status-badge--active">{preferences.data?.careerId === career.careerId ? "현재 목표" : "지원됨"}</span><small>{career.profileVersion}</small></div>
        <h2>{career.localizedName}</h2><p>{career.purpose}</p>
        <Link to={`/careers/${career.careerId}`}>프로필 자세히 보기 →</Link>
      </article>)}
    </div>
  </PageShell>;
}

function ErrorState({ error, retry }: { error: Error | null; retry: () => void }) {
  if (error instanceof ApiError && error.status === 401) return <div className="state-panel" role="alert"><h1>로그인이 필요합니다</h1><Link className="button-link" to="/">홈으로 이동</Link></div>;
  return <div className="state-panel" role="alert"><h1>커리어 목록을 불러오지 못했습니다</h1><button onClick={retry}>다시 시도</button></div>;
}
function PageShell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/">← DevPath 홈</Link></nav>{children}</main>; }
