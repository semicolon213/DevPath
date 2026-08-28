import { ConnectionPanel } from "../features/connections/ui/ConnectionPanel";
import { SettingsLayout } from "../features/settings/ui/SettingsLayout";

export function IntegrationSettingsPage() {
  return <SettingsLayout title="외부 서비스 연결" description="GitHub 저장소와 Notion 워크스페이스 권한을 서버에서 안전하게 관리합니다."><ConnectionPanel /></SettingsLayout>;
}
