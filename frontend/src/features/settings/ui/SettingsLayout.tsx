import type { ReactNode } from "react";
import { Link, NavLink } from "react-router-dom";

export function SettingsLayout({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return <main className="shell workspace settings-workspace">
    <nav><Link to="/">← DevPath 홈</Link></nav>
    <header className="workspace-header"><div><p className="eyebrow">Account settings</p><h1>{title}</h1><p>{description}</p></div><Link className="button-link button-secondary" to="/onboarding">온보딩 상태 확인</Link></header>
    <nav className="settings-tabs" aria-label="설정 메뉴">
      <NavLink end to="/settings" className={({ isActive }) => isActive ? "settings-tab settings-tab--active" : "settings-tab"}>설정 홈</NavLink>
      <NavLink to="/settings/profile" className={({ isActive }) => isActive ? "settings-tab settings-tab--active" : "settings-tab"}>프로필과 목표</NavLink>
      <NavLink to="/settings/integrations" className={({ isActive }) => isActive ? "settings-tab settings-tab--active" : "settings-tab"}>외부 서비스 연결</NavLink>
    </nav>
    {children}
  </main>;
}
