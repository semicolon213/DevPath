import { useEffect, useRef, useState, type ReactNode } from "react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useLogout } from "../features/session/model/useSession";
import { ConfirmDialog } from "../shared/ui/ConfirmDialog";
import { UnsavedChangesContext } from "./UnsavedChangesContext";

export const appNavigation = [
  ["대시보드", "/dashboard"],
  ["저장소", "/repositories"],
  ["분석", "/analyses"],
  ["기술 역량", "/skills"],
  ["커리어 준비도", "/career-readiness"],
  ["추천", "/recommendations"],
  ["학습 로드맵", "/roadmap"],
  ["지식", "/knowledge"],
  ["설정", "/settings"]
] as const;

export function AppLayout({ children }: { children?: ReactNode }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [confirmingLogout, setConfirmingLogout] = useState(false);
  const [ignoreUnsavedChanges, setIgnoreUnsavedChanges] = useState(false);
  const menuButton = useRef<HTMLButtonElement>(null);
  const main = useRef<HTMLElement>(null);
  const logout = useLogout();
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => setMenuOpen(false), [location.pathname]);

  useEffect(() => {
    if (!menuOpen) return;
    const close = (event: KeyboardEvent) => {
      if (event.key !== "Escape" || event.defaultPrevented) return;
      setMenuOpen(false);
      menuButton.current?.focus();
    };
    window.addEventListener("keydown", close);
    return () => window.removeEventListener("keydown", close);
  }, [menuOpen]);

  function requestLogout() {
    if (hasUnsavedChanges) {
      setConfirmingLogout(true);
      return;
    }
    performLogout();
  }

  function performLogout() {
    setIgnoreUnsavedChanges(true);
    logout.mutate(undefined, { onSuccess: () => navigate("/"), onError: () => setIgnoreUnsavedChanges(false) });
  }

  return <UnsavedChangesContext.Provider value={{ hasUnsavedChanges, ignoreUnsavedChanges, setHasUnsavedChanges }}><div className="app-shell">
    <header className="app-header">
      <div className="app-header__inner">
        <Link className="app-brand" to="/dashboard">DevPath</Link>
        <button ref={menuButton} className="app-menu-button button-secondary" type="button" aria-expanded={menuOpen} aria-controls="app-navigation" onClick={() => setMenuOpen(value => !value)}>메뉴</button>
        <nav id="app-navigation" className={`app-navigation${menuOpen ? " app-navigation--open" : ""}`} aria-label="주요 메뉴">
          {appNavigation.map(([label, to]) => {
            const settingsCatalog = to === "/settings" && ["/careers", "/companies"].some(path => location.pathname === path || location.pathname.startsWith(`${path}/`));
            const isActive = settingsCatalog || location.pathname === to || (to !== "/dashboard" && location.pathname.startsWith(`${to}/`));
            return <Link key={to} to={to} aria-current={isActive ? location.pathname === to ? "page" : "true" : undefined} className={`app-navigation__link${isActive ? " app-navigation__link--active" : ""}`} onClick={() => {
              if (menuOpen && location.pathname === to) {
                setMenuOpen(false);
                requestAnimationFrame(() => { main.current?.focus(); });
              }
            }}>{label}</Link>;
          })}
        </nav>
        <button className="button-secondary app-logout" type="button" disabled={logout.isPending} onClick={requestLogout}>{logout.isPending ? "로그아웃 중…" : "로그아웃"}</button>
      </div>
    </header>
    {logout.isError && !confirmingLogout ? <p className="app-logout-error" role="alert">로그아웃에 실패했습니다. 다시 시도해 주세요.</p> : null}
    <main ref={main} className="shell workspace" tabIndex={-1}>{children ?? <Outlet />}</main>
    <ConfirmDialog open={confirmingLogout} title="저장하지 않은 변경사항이 있습니다" description="로그아웃하면 프로필 변경사항이 저장되지 않습니다." confirmLabel="변경사항 버리고 로그아웃" pending={logout.isPending} error={logout.isError ? "로그아웃에 실패했습니다. 다시 시도해 주세요." : undefined} returnFocusFallback={main} onCancel={() => setConfirmingLogout(false)} onConfirm={performLogout} />
  </div></UnsavedChangesContext.Provider>;
}
