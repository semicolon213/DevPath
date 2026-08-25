import { Link } from "react-router-dom";
import { SettingsLayout } from "../features/settings/ui/SettingsLayout";

export function SettingsPage() {
  return <SettingsLayout title="내 DevPath 설정" description="프로필과 평가 목표, GitHub 저장소 접근 권한을 분리해서 관리합니다.">
    <section className="settings-card-grid" aria-label="설정 영역">
      <article className="settings-card"><p className="eyebrow">Profile</p><h2>프로필과 평가 목표</h2><p>표시 이름과 경력 단계, 결정론적 평가에 적용할 목표 직무와 선택 회사를 관리합니다.</p><Link className="button-link" to="/settings/profile">프로필 설정 열기</Link></article>
      <article className="settings-card"><p className="eyebrow">Integration</p><h2>GitHub 연결과 저장소</h2><p>서버에 보관된 연결 상태를 확인하고 권한 재승인, 연결 해제, 저장소 등록을 진행합니다.</p><Link className="button-link" to="/settings/integrations">연결 설정 열기</Link></article>
    </section>
    <aside className="settings-security-note"><h2>보안 경계</h2><p>브라우저에는 GitHub 토큰이나 DevPath 세션 식별자를 저장하지 않습니다. 모든 연결과 데이터 접근 권한은 서버에서 다시 확인합니다.</p></aside>
  </SettingsLayout>;
}
