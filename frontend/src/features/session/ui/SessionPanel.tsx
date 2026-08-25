import { getApiBaseUrl } from "../../../shared/api/apiClient";
import { Link } from "react-router-dom";
import { ProfilePanel } from "../../profile/ui/ProfilePanel";
import { useLogout, useSession } from "../model/useSession";

export function SessionPanel() {
  const session = useSession();
  const logout = useLogout();
  if (session.isPending) return <p role="status">DevPath 로그인 상태를 확인하는 중입니다.</p>;
  if (session.isError) return <section className="session-panel" aria-labelledby="session-error-title"><h2 id="session-error-title">로그인 상태를 확인할 수 없습니다</h2><p>인증 서비스에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.</p><button type="button" onClick={() => session.refetch()}>다시 시도</button></section>;
  if (session.data === null) return <section className="session-panel" aria-labelledby="sign-in-title"><h2 id="sign-in-title">DevPath 로그인</h2><p>GitHub 계정으로 안전하게 로그인하고 개발 근거 분석을 시작하세요.</p><a className="button-link" href={`${getApiBaseUrl()}/oauth2/authorization/github`}>GitHub로 로그인</a></section>;
  return <section className="session-panel" aria-labelledby="welcome-title"><h2 id="welcome-title">{session.data.displayName}님, 환영합니다</h2><p>DevPath 세션이 활성화되었습니다.</p><nav className="workspace-actions" aria-label="주요 작업"><Link className="button-link" to="/dashboard">내 대시보드</Link><Link className="button-link button-secondary" to="/repositories">저장소 작업 공간</Link><Link className="button-link button-secondary" to="/analyses">분석 이력</Link><Link className="button-link button-secondary" to="/skills">기술 역량</Link><Link className="button-link button-secondary" to="/careers">지원 커리어</Link><Link className="button-link button-secondary" to="/companies">지원 회사</Link></nav><button type="button" disabled={logout.isPending} onClick={() => logout.mutate()}>{logout.isPending ? "로그아웃 중…" : "로그아웃"}</button>{logout.isError ? <p role="alert">로그아웃에 실패했습니다. 다시 시도해 주세요.</p> : null}<ProfilePanel /></section>;
}
