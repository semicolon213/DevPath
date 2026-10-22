import { getApiBaseUrl } from "../../../shared/api/apiClient";
import { Link } from "react-router-dom";
import { useLogout, useSession } from "../model/useSession";
import { useOnboardingProgress } from "../../onboarding/model/useOnboarding";

export function SessionPanel() {
  const session = useSession();
  const logout = useLogout();
  if (session.isPending) return <p role="status">DevPath 로그인 상태를 확인하는 중입니다.</p>;
  if (session.isError) return <section className="session-panel" aria-labelledby="session-error-title"><h2 id="session-error-title">로그인 상태를 확인할 수 없습니다</h2><p>인증 서비스에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.</p><button type="button" onClick={() => session.refetch()}>다시 시도</button></section>;
  if (session.data === null) return <section className="session-panel" aria-labelledby="sign-in-title"><h2 id="sign-in-title">DevPath 로그인</h2><p>GitHub 계정으로 안전하게 로그인하고 개발 근거 분석을 시작하세요.</p><a className="button-link button-primary" href={`${getApiBaseUrl()}/oauth2/authorization/github`}>GitHub로 로그인</a></section>;
  return <AuthenticatedSession displayName={session.data.displayName} logout={logout} />;
}

function AuthenticatedSession({ displayName, logout }: { displayName: string; logout: ReturnType<typeof useLogout> }) {
  const progress = useOnboardingProgress();
  const ready = progress.data?.status === "DASHBOARD_READY";
  return <section className="session-panel" aria-labelledby="welcome-title"><h2 id="welcome-title">{displayName}님, 환영합니다</h2><p>DevPath 세션이 활성화되었습니다.</p><div className="workspace-actions"><Link className="button-link button-primary" to={ready ? "/dashboard" : "/onboarding"}>{ready ? "내 대시보드 열기" : "첫 분석 시작"}</Link><button className="button-secondary" type="button" disabled={logout.isPending} onClick={() => logout.mutate()}>{logout.isPending ? "로그아웃 중…" : "로그아웃"}</button></div>{progress.isError ? <p role="status">온보딩 상태를 확인하지 못해 시작 화면으로 안내합니다.</p> : null}{logout.isError ? <p role="alert">로그아웃에 실패했습니다. 다시 시도해 주세요.</p> : null}</section>;
}
