import { ProfilePanel } from "../features/profile/ui/ProfilePanel";
import { SettingsLayout } from "../features/settings/ui/SettingsLayout";

export function ProfileSettingsPage() {
  return <SettingsLayout title="프로필과 평가 목표" description="내 기본 정보와 결정론적 평가에 적용할 직무·회사 기준을 관리합니다."><ProfilePanel /></SettingsLayout>;
}
