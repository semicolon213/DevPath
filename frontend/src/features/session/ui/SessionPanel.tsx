import { getApiBaseUrl } from "../../../shared/api/apiClient";
import { useLogout, useSession } from "../model/useSession";

export function SessionPanel() {
  const session = useSession();
  const logout = useLogout();

  if (session.isPending) {
    return <p role="status">Checking your DevPath session…</p>;
  }

  if (session.isError) {
    return (
      <section className="session-panel" aria-labelledby="session-error-title">
        <h2 id="session-error-title">Session unavailable</h2>
        <p>DevPath could not reach the authentication service. Please try again.</p>
        <button type="button" onClick={() => session.refetch()}>
          Retry
        </button>
      </section>
    );
  }

  if (session.data === null) {
    return (
      <section className="session-panel" aria-labelledby="sign-in-title">
        <h2 id="sign-in-title">Sign in to DevPath</h2>
        <p>Use GitHub to create or resume your secure DevPath session.</p>
        <a className="button-link" href={`${getApiBaseUrl()}/oauth2/authorization/github`}>
          Continue with GitHub
        </a>
      </section>
    );
  }

  return (
    <section className="session-panel" aria-labelledby="welcome-title">
      <h2 id="welcome-title">Welcome, {session.data.displayName}</h2>
      <p>Your GitHub-backed DevPath session is active.</p>
      <button type="button" disabled={logout.isPending} onClick={() => logout.mutate()}>
        {logout.isPending ? "Signing out…" : "Sign out"}
      </button>
      {logout.isError ? <p role="alert">Sign out failed. Please try again.</p> : null}
    </section>
  );
}
