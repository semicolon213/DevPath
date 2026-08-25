import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRoutes } from "../routes/AppRoutes";
import { renderWithProviders } from "../test/renderWithProviders";

const metadata = { requestId: "settings-test", apiVersion: "v1", timestamp: "2026-08-25T06:00:00Z" };
const envelope = (data: unknown) => Response.json({ data, metadata });

describe("Settings workspace", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("separates profile and integration settings into clear destinations", () => {
    renderWithProviders(<AppRoutes />, ["/settings"]);
    expect(screen.getByRole("heading", { name: "내 DevPath 설정" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "프로필 설정 열기" })).toHaveAttribute("href", "/settings/profile");
    expect(screen.getByRole("link", { name: "연결 설정 열기" })).toHaveAttribute("href", "/settings/integrations");
    expect(screen.getByText(/GitHub 토큰이나 DevPath 세션 식별자를 저장하지 않습니다/)).toBeInTheDocument();
  });

  it("loads the authoritative profile, preferences, and supported targets", async () => {
    vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
      const path = new URL(String(input)).pathname;
      if (path.endsWith("/profile")) return Promise.resolve(envelope({ profileId: "profile-id", displayName: "개발자", careerStage: "JUNIOR", bio: "백엔드", updatedAt: metadata.timestamp }));
      if (path.endsWith("/preferences")) return Promise.resolve(envelope({ careerId: "backend", companyId: null, updatedAt: metadata.timestamp }));
      if (path.endsWith("/careers")) return Promise.resolve(envelope({ careers: [{ careerId: "backend", localizedName: "백엔드 개발자", profileVersion: "career-v2" }] }));
      if (path.endsWith("/companies")) return Promise.resolve(envelope({ companies: [{ companyId: "naver", localizedName: "네이버", profileVersion: "company-v1" }] }));
      return Promise.reject(new Error(path));
    }));

    renderWithProviders(<AppRoutes />, ["/settings/profile"]);

    expect(screen.getByRole("heading", { name: "프로필과 평가 목표", level: 1 })).toBeInTheDocument();
    expect(await screen.findByRole("textbox", { name: "표시 이름" })).toHaveValue("개발자");
    expect(screen.getByRole("combobox", { name: "목표 직무" })).toHaveValue("backend");
    expect(screen.getByRole("combobox", { name: "목표 회사" })).toHaveValue("");
  });

  it("shows a disconnected GitHub state without treating login identity as repository access", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(envelope({ connections: [] }))));

    renderWithProviders(<AppRoutes />, ["/settings/integrations"]);

    expect(screen.getByRole("heading", { name: "GitHub 연결과 저장소", level: 1 })).toBeInTheDocument();
    expect(await screen.findByText("미연결")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "GitHub 저장소 연결" })).toBeInTheDocument();
    expect(screen.queryByText("접근 가능한 저장소")).not.toBeInTheDocument();
  });
});
