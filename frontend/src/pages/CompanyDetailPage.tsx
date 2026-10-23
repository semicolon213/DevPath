import { useParams } from "react-router-dom";
import { useCompany } from "../features/companies/model/useCompanies";
import { usePreferences, useSetCompany } from "../features/profile/model/useProfile";
import { PageState } from "../shared/ui/PageState";
import { Breadcrumbs } from "../shared/ui/Breadcrumbs";
import { PageHero } from "../shared/ui/PageHero";
import { StatusChip } from "../shared/ui/StatusChip";
export function CompanyDetailPage() {
  const { id } = useParams();
  const company = useCompany(id),
    prefs = usePreferences(),
    setCompany = useSetCompany();
  if (company.isPending || prefs.isPending)
    return (
      <Shell>
        <PageState state="loading" title="회사 프로필" message="회사 프로필을 불러오는 중입니다." />
      </Shell>
    );
  if (company.isError || prefs.isError)
    return (
      <Shell>
        <PageState
          state="error"
          title="회사 프로필을 표시할 수 없습니다"
          error={company.error ?? prefs.error}
          retry={() => {
            void company.refetch();
            void prefs.refetch();
          }}
          backTo="/companies"
          backLabel="지원 회사로 돌아가기"
        />
      </Shell>
    );
  const v = company.data,
    selected = prefs.data.companyId === v.companyId;
  return (
    <Shell>
      <PageHero>
        <div>
          <p className="eyebrow">프로필 버전 {v.profileVersion} · 공식 지원</p>
          <h1>{v.localizedName}</h1>
          <p>{v.engineeringCulture}</p>
        </div>
        {selected ? (
          <StatusChip variant="info">현재 목표 회사</StatusChip>
        ) : (
          <button className="button-primary" disabled={setCompany.isPending} onClick={() => setCompany.mutate(v.companyId)}>
            이 회사를 목표로 설정
          </button>
        )}
      </PageHero>
      <p className="analysis-notice">이 프로필은 일반적 역량 강조 정책이며 비공개 채용 정보나 합격 예측이 아닙니다.</p>
      <section className="profile-section-list">
        <List title="기술 관심사" values={v.technologyFocus} />
        <List title="선호 역량" values={v.preferredCompetencies} />
        <List title="역량 강조" values={v.skillEmphasis} />
      </section>
      <section>
        <div className="section-heading">
          <div>
            <h2>추천 유형 강조 순서</h2>
            <p>향후 공식 추천 정책이 참조할 구성값이며 현재 추천을 생성하지 않습니다.</p>
          </div>
        </div>
        <ol>
          {v.recommendationPriorities.map((x) => (
            <li key={x}>{x}</li>
          ))}
        </ol>
      </section>
    </Shell>
  );
}
function List({ title, values }: { title: string; values: string[] }) {
  return (
    <article>
      <h2>{title}</h2>
      <ul>
        {values.map((x) => (
          <li key={x}>{x}</li>
        ))}
      </ul>
    </article>
  );
}
function Shell({ children }: { children: React.ReactNode }) {
  return (
    <>
      <Breadcrumbs items={[{ label: "지원 회사", to: "/companies" }, { label: "회사 상세" }]} />
      {children}
    </>
  );
}
