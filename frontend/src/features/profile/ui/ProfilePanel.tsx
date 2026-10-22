import { FormEvent, useEffect, useRef, useState } from "react";
import { Link, useBlocker } from "react-router-dom";
import { CareerStage } from "../api/profileApi";
import { usePreferences, useProfile, useSetCareer, useSetCompany, useUpdateProfile } from "../model/useProfile";
import { useCareers } from "../../careers/model/useCareers";
import { useCompanies } from "../../companies/model/useCompanies";
import { ConfirmDialog } from "../../../shared/ui/ConfirmDialog";
import { useUnsavedChanges } from "../../../app/UnsavedChangesContext";

type ProfileErrors = { displayName?: string; bio?: string };
type TargetKind = "career" | "company";

export function ProfilePanel({ embedded = false }: { embedded?: boolean }) {
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
  const [careerId, setCareerId] = useState("");
  const [companyId, setCompanyId] = useState("");
  const [errors, setErrors] = useState<ProfileErrors>({});
  const [targetKind, setTargetKind] = useState<TargetKind | null>(null);
  const [targetMessage, setTargetMessage] = useState<string | null>(null);
  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const displayNameRef = useRef<HTMLInputElement>(null);
  const bioRef = useRef<HTMLTextAreaElement>(null);
  const targetMessageRef = useRef<HTMLDivElement>(null);
  const profilePanelRef = useRef<HTMLElement>(null);
  const { ignoreUnsavedChanges, setHasUnsavedChanges } = useUnsavedChanges();

  useEffect(() => { if (profile.data) { setDisplayName(profile.data.displayName); setStage(profile.data.careerStage ?? ""); setBio(profile.data.bio ?? ""); } }, [profile.data]);
  useEffect(() => { if (preferences.data) { setCareerId(preferences.data.careerId ?? ""); setCompanyId(preferences.data.companyId ?? ""); } }, [preferences.data]);
  const dirty = Boolean(profile.data) && (displayName !== profile.data!.displayName || stage !== (profile.data!.careerStage ?? "") || bio !== (profile.data!.bio ?? ""));
  const blocker = useBlocker(dirty && !ignoreUnsavedChanges);
  useEffect(() => {
    setHasUnsavedChanges(dirty);
    return () => setHasUnsavedChanges(false);
  }, [dirty, setHasUnsavedChanges]);
  useEffect(() => {
    if (!dirty) return;
    const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = ""; };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  if (profile.isPending || preferences.isPending || careers.isPending || companies.isPending) return <p role="status">프로필을 불러오는 중입니다.</p>;
  if (profile.isError || preferences.isError || careers.isError || companies.isError) return <div className="state-panel" role="alert"><p>프로필과 목표 설정을 불러오지 못했습니다.</p><button type="button" onClick={() => { void profile.refetch(); void preferences.refetch(); void careers.refetch(); void companies.refetch(); }}>다시 시도</button></div>;

  function submit(event: FormEvent) {
    event.preventDefault();
    const nextErrors: ProfileErrors = {};
    if (!displayName.trim()) nextErrors.displayName = "표시 이름을 입력해 주세요.";
    else if (displayName.length > 120) nextErrors.displayName = "표시 이름은 120자 이하로 입력해 주세요.";
    if (bio.length > 1000) nextErrors.bio = "자기소개는 1,000자 이하로 입력해 주세요.";
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      requestAnimationFrame(() => errorSummaryRef.current?.focus());
      return;
    }
    update.mutate({ displayName: displayName.trim(), careerStage: stage || null, bio: bio || null });
  }

  function confirmTarget() {
    if (targetKind === "career") {
      career.mutate(careerId, {
        onSuccess: () => { setTargetKind(null); setTargetMessage("목표 직무를 변경했습니다."); },
      });
    } else if (targetKind === "company") {
      company.mutate(companyId, {
        onSuccess: () => { setTargetKind(null); setTargetMessage("목표 회사를 변경했습니다."); },
      });
    }
  }

  const targetDialog = targetKind === "career" ? {
    title: "목표 직무를 변경할까요?",
    description: "선택한 직무는 이후 분석 흐름에 적용되며 준비도와 추천이 새 직무 기준으로 다시 계산될 수 있습니다. 이 화면에서는 재평가를 시작하지 않습니다.",
    label: "목표 직무 변경",
    pending: career.isPending
  } : {
    title: "목표 회사를 변경할까요?",
    description: "선택한 회사가 목표로 저장됩니다. 현재 회사별 준비도와 추천 정책은 적용되지 않으며, 이 화면에서는 재평가를 시작하지 않습니다.",
    label: "목표 회사 변경",
    pending: company.isPending
  };

  return <section ref={profilePanelRef} id="profile" className="profile-panel" aria-labelledby="profile-title">
    <h2 id="profile-title">커리어 프로필</h2>
    <form noValidate onSubmit={submit}>
      {Object.keys(errors).length > 0 ? <div ref={errorSummaryRef} className="error-summary" role="alert" tabIndex={-1}>
        <strong>입력 내용을 확인해 주세요.</strong>
        <ul>{errors.displayName ? <li><a href="#display-name" onClick={event => { event.preventDefault(); displayNameRef.current?.focus(); }}>{errors.displayName}</a></li> : null}{errors.bio ? <li><a href="#profile-bio" onClick={event => { event.preventDefault(); bioRef.current?.focus(); }}>{errors.bio}</a></li> : null}</ul>
      </div> : null}
      <label htmlFor="display-name">표시 이름</label><p className="field-help" id="display-name-help">DevPath 화면에 표시할 이름입니다.</p><input ref={displayNameRef} id="display-name" value={displayName} maxLength={120} required aria-invalid={Boolean(errors.displayName)} aria-describedby={errors.displayName ? "display-name-help display-name-error" : "display-name-help"} onChange={event => setDisplayName(event.target.value)} />
      {errors.displayName ? <p id="display-name-error" className="form-error">{errors.displayName}</p> : null}
      <label htmlFor="career-stage">경력 단계</label><select id="career-stage" value={stage} onChange={event => setStage(event.target.value as CareerStage | "")}><option value="">선택 안 함</option><option value="STUDENT">학생</option><option value="ENTRY_LEVEL">신입 준비</option><option value="JUNIOR">주니어</option><option value="MID_LEVEL">미드레벨</option><option value="SENIOR">시니어</option></select>
      <label htmlFor="profile-bio">자기소개</label><p className="field-help" id="profile-bio-help">경험과 관심사를 1,000자 이내로 적어 주세요.</p><textarea ref={bioRef} id="profile-bio" value={bio} maxLength={1000} aria-invalid={Boolean(errors.bio)} aria-describedby={errors.bio ? "profile-bio-help profile-bio-error" : "profile-bio-help"} onChange={event => setBio(event.target.value)} />
      {errors.bio ? <p id="profile-bio-error" className="form-error">{errors.bio}</p> : null}
      <button className={embedded ? "button-secondary" : "button-primary"} disabled={update.isPending}>{update.isPending ? "저장 중…" : "프로필 저장"}</button>
      {update.isSuccess ? <p role="status">프로필을 저장했습니다.</p> : null}{update.isError ? <p role="alert">프로필 저장에 실패했습니다.</p> : null}
    </form>
    <div id="targets" className="target-grid">
      <div><label htmlFor="career-target">목표 직무</label><select id="career-target" value={careerId} onChange={event => { setCareerId(event.target.value); setTargetMessage(null); }}><option value="">목표 직무 선택</option>{careers.data.careers.map(value => <option key={value.careerId} value={value.careerId}>{value.localizedName}</option>)}</select><button type="button" disabled={!careerId || careerId === (preferences.data?.careerId ?? "") || career.isPending} onClick={() => { career.reset(); setTargetKind("career"); }}>목표 직무 변경</button><Link to="/careers">직무별 평가 기준 살펴보기</Link></div>
      <div><label htmlFor="company-target">목표 회사</label><select id="company-target" value={companyId} onChange={event => { setCompanyId(event.target.value); setTargetMessage(null); }}><option value="">목표 회사 선택</option>{companies.data.companies.map(value => <option key={value.companyId} value={value.companyId}>{value.localizedName}</option>)}</select><button type="button" disabled={!companyId || companyId === (preferences.data?.companyId ?? "") || company.isPending} onClick={() => { company.reset(); setTargetKind("company"); }}>목표 회사 변경</button><Link to="/companies">회사별 역량 기준 살펴보기</Link></div>
    </div>
    <div ref={targetMessageRef} role="status" tabIndex={-1}>{targetMessage}</div>
    <ConfirmDialog open={targetKind !== null} title={targetDialog.title} description={targetDialog.description} confirmLabel={targetDialog.label} pending={targetDialog.pending} error={targetKind === "career" && career.isError ? "목표 직무를 변경하지 못했습니다. 다시 시도해 주세요." : targetKind === "company" && company.isError ? "목표 회사를 변경하지 못했습니다. 다시 시도해 주세요." : undefined} returnFocusFallback={targetMessageRef} onCancel={() => setTargetKind(null)} onConfirm={confirmTarget} />
    <ConfirmDialog open={blocker.state === "blocked"} title="저장하지 않은 변경사항이 있습니다" description="이 페이지를 떠나면 프로필 변경사항이 저장되지 않습니다." confirmLabel="페이지 나가기" returnFocusFallback={profilePanelRef} onCancel={() => blocker.reset?.()} onConfirm={() => blocker.proceed?.()} />
  </section>;
}
