import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CareerStage } from "../api/profileApi";
import { usePreferences, useProfile, useSetCareer, useSetCompany, useUpdateProfile } from "../model/useProfile";
import { ConnectionPanel } from "../../connections/ui/ConnectionPanel";
import { useCareers } from "../../careers/model/useCareers";
import { useCompanies } from "../../companies/model/useCompanies";

export function ProfilePanel() {
  const profile = useProfile();
  const preferences = usePreferences();
  const careers = useCareers();
  const companies = useCompanies();
  const update = useUpdateProfile();
  const career = useSetCareer();
  const company = useSetCompany();
  const [displayName, setDisplayName] = useState("");
  const [stage, setStage] = useState<CareerStage | "">("");
  const [bio, setBio] = useState("");

  useEffect(() => { if (profile.data) { setDisplayName(profile.data.displayName); setStage(profile.data.careerStage ?? ""); setBio(profile.data.bio ?? ""); } }, [profile.data]);
  if (profile.isPending || preferences.isPending || careers.isPending || companies.isPending) return <p role="status">프로필을 불러오는 중입니다.</p>;
  if (profile.isError || preferences.isError || careers.isError || companies.isError) return <p role="alert">프로필을 불러오지 못했습니다. 다시 시도해 주세요.</p>;

  function submit(event: FormEvent) { event.preventDefault(); update.mutate({ displayName, careerStage: stage || null, bio: bio || null }); }
  return <section className="profile-panel" aria-labelledby="profile-title">
    <h3 id="profile-title">커리어 프로필</h3>
    <form onSubmit={submit}>
      <label>표시 이름<input value={displayName} maxLength={120} required onChange={event => setDisplayName(event.target.value)} /></label>
      <label>경력 단계<select value={stage} onChange={event => setStage(event.target.value as CareerStage | "")}><option value="">선택 안 함</option><option value="STUDENT">학생</option><option value="ENTRY_LEVEL">신입 준비</option><option value="JUNIOR">주니어</option><option value="MID_LEVEL">미드레벨</option><option value="SENIOR">시니어</option></select></label>
      <label>자기소개<textarea value={bio} maxLength={1000} onChange={event => setBio(event.target.value)} /></label>
      <button disabled={update.isPending}>{update.isPending ? "저장 중…" : "프로필 저장"}</button>
      {update.isSuccess ? <p role="status">프로필을 저장했습니다.</p> : null}{update.isError ? <p role="alert">프로필 저장에 실패했습니다.</p> : null}
    </form>
    <div className="target-grid">
      <label>목표 직무<select value={preferences.data?.careerId ?? ""} onChange={event => event.target.value && career.mutate(event.target.value)}><option value="">목표 직무 선택</option>{careers.data.careers.map(value => <option key={value.careerId} value={value.careerId}>{value.localizedName}</option>)}</select><Link to="/careers">직무별 평가 기준 살펴보기</Link></label>
      <label>목표 회사<select value={preferences.data?.companyId ?? ""} onChange={event => event.target.value && company.mutate(event.target.value)}><option value="">목표 회사 선택</option>{companies.data.companies.map(value => <option key={value.companyId} value={value.companyId}>{value.localizedName}</option>)}</select><Link to="/companies">회사별 역량 기준 살펴보기</Link></label>
    </div>
    {career.isError || company.isError ? <p role="alert">목표 선택에 실패했습니다.</p> : null}
    <ConnectionPanel />
  </section>;
}
