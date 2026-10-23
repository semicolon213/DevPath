import { Link } from "react-router-dom";
import { useCareers } from "../features/careers/model/useCareers";
import { usePreferences } from "../features/profile/model/useProfile";
import { PageState } from "../shared/ui/PageState";
import { PageHero } from "../shared/ui/PageHero";

export function CareersPage() {
  const careers = useCareers();
  const preferences = usePreferences();
  if (careers.isPending || preferences.isPending) return <PageState state="loading" title="지원 커리어" message="커리어 프로필을 불러오는 중입니다." />;
  if (careers.isError || preferences.isError) return <PageState state="error" title="커리어 목록을 불러오지 못했습니다" error={careers.error ?? preferences.error} retry={() => { void careers.refetch(); void preferences.refetch(); }} />;

  return <>
    <PageHero>
      <div><p className="eyebrow">버전이 고정된 평가 기준</p><h1>지원 커리어</h1><p>목표 직무별 핵심 기술과 기대 역량을 먼저 확인하세요. 이 화면은 준비도를 계산하지 않으며, 활성 커리어 프로필의 기준만 보여줍니다.</p></div>
      <Link className="button-link button-primary" to="/settings/profile#targets">목표 직무 설정</Link>
    </PageHero>
    <section className="career-catalog-notice" aria-label="커리어 카탈로그 정보">
      <strong>{careers.data.careers.length}개 지원 직무</strong><span>카탈로그 버전 {careers.data.catalogVersion}</span>
    </section>
    <div className="catalog-list" role="list">
      {careers.data.careers.map(career => <article role="listitem" aria-current={preferences.data?.careerId === career.careerId ? "true" : undefined} className={preferences.data?.careerId === career.careerId ? "catalog-tile catalog-tile--selected" : "catalog-tile"} key={career.careerId}>
        <div><span className="status-badge status-badge--info">{preferences.data?.careerId === career.careerId ? "현재 목표" : "지원됨"}</span><small>프로필 버전 {career.profileVersion}</small></div>
        <h2>{career.localizedName}</h2><p>{career.purpose}</p>
        <Link to={`/careers/${career.careerId}`}>프로필 자세히 보기 →</Link>
      </article>)}
    </div>
  </>;
}
