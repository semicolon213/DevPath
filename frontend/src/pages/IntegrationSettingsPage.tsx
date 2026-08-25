import { ConnectionPanel } from "../features/connections/ui/ConnectionPanel";
import { SettingsLayout } from "../features/settings/ui/SettingsLayout";

export function IntegrationSettingsPage() {
  return <SettingsLayout title="GitHub 연결과 저장소" description="GitHub App 권한과 DevPath에 등록할 저장소를 안전하게 관리합니다."><ConnectionPanel /></SettingsLayout>;
}
