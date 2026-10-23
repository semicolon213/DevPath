import { screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";

vi.mock("../features/profile/ui/ProfilePanel", () => ({ ProfilePanel: ({ embedded }: { embedded?: boolean }) => <div>설정 도구<button className={embedded ? "button-secondary" : "button-primary"}>프로필 저장</button></div> }));
vi.mock("../features/connections/ui/ConnectionPanel", () => ({ ConnectionPanel: ({ embedded }: { embedded?: boolean }) => <div>연결 도구<button className={embedded ? "button-secondary" : "button-primary"}>GitHub 저장소 연결</button></div> }));
const metadata = { requestId: "request-1", apiVersion: "v1", timestamp: "2026-08-25T06:00:00Z" };
const step = (name: string, status: "COMPLETE" | "INCOMPLETE", requirement = "REQUIRED") => ({ step: name, status, requirement, resourceId: null, actionPath: "/repositories" });

describe("OnboardingPage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("shows the server-determined next step and persisted completion states", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(Response.json({ data: {
      status: "IN_PROGRESS", completedStepCount: 5, totalStepCount: 8, nextStep: "INITIAL_SYNC",
      steps: [step("ACCOUNT", "COMPLETE"), step("PROFILE", "COMPLETE"), step("CAREER_TARGET", "COMPLETE", "RECOMMENDED"), step("COMPANY_TARGET", "INCOMPLETE", "OPTIONAL"), step("GITHUB_CONNECTION", "COMPLETE"), step("REPOSITORY_IMPORT", "COMPLETE"), step("INITIAL_SYNC", "INCOMPLETE"), step("INITIAL_ANALYSIS", "INCOMPLETE", "RECOMMENDED")],
      generatedAt: metadata.timestamp
    }, metadata }))));

    renderWithProviders(undefined, ["/onboarding"]);

    expect(await screen.findByRole("heading", { name: "첫 저장소 동기화", level: 2 })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "DevPath 시작하기" })).toBeInTheDocument();
    expect(screen.getByLabelText("전체 8단계 중 5단계 완료")).toBeInTheDocument();
    expect(screen.getByText("설정 도구")).toBeInTheDocument();
    expect(screen.getByText("선택")).toBeInTheDocument();
    expect(screen.getByText("지금")).toBeInTheDocument();
    const steps = within(screen.getByRole("list", { name: "온보딩 단계" })).getAllByRole("listitem");
    expect(within(steps[3]).getByText("미완료")).toBeInTheDocument();
    expect(within(steps[7]).getByText("예정")).toBeInTheDocument();
    expect(steps[3]).toHaveClass("onboarding-step--incomplete");
    expect(steps[7]).toHaveClass("onboarding-step--upcoming");
    expect(screen.getAllByText("미완료")).toHaveLength(1);
    expect(document.querySelectorAll(".button-primary")).toHaveLength(1);
  });

  it("links a completed first analysis to the result workspaces", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(Response.json({ data: {
      status: "DASHBOARD_READY", completedStepCount: 7, totalStepCount: 8, nextStep: "DASHBOARD_READY",
      steps: [step("ACCOUNT", "COMPLETE"), step("PROFILE", "COMPLETE"), step("CAREER_TARGET", "COMPLETE", "RECOMMENDED"), step("COMPANY_TARGET", "INCOMPLETE", "OPTIONAL"), step("GITHUB_CONNECTION", "COMPLETE"), step("REPOSITORY_IMPORT", "COMPLETE"), step("INITIAL_SYNC", "COMPLETE"), step("INITIAL_ANALYSIS", "COMPLETE", "RECOMMENDED")],
      generatedAt: metadata.timestamp
    }, metadata }))));

    renderWithProviders(undefined, ["/onboarding"]);

    expect(await screen.findByRole("heading", { name: "첫 분석 준비를 마쳤습니다" })).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: "대시보드" })).not.toHaveLength(0);
    expect(within(screen.getByRole("region", { name: "분석 결과에서 이어가기" })).getByRole("link", { name: "기술 역량" })).toHaveAttribute("href", "/skills");
    expect(screen.queryByText("설정 도구")).not.toBeInTheDocument();
    const steps = within(screen.getByRole("list", { name: "온보딩 단계" })).getAllByRole("listitem");
    expect(within(steps[3]).getByText("미완료")).toBeInTheDocument();
    expect(steps[3]).toHaveClass("onboarding-step--incomplete");
  });

  it("distinguishes an anonymous session from a transport failure", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(Response.json(
      { code: "AUTHENTICATION_REQUIRED", message: "Authentication required" }, { status: 401 }))));

    renderWithProviders(undefined, ["/onboarding"]);

    expect(await screen.findByRole("alert")).toHaveTextContent("로그인이 필요합니다");
    expect(screen.getByRole("link", { name: "홈으로 이동" })).toHaveAttribute("href", "/");
  });

  it("uses the shared network failure state", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.reject(new TypeError("offline"))));
    renderWithProviders(undefined, ["/onboarding"]);
    expect(await screen.findByRole("heading", { name: "서버에 연결할 수 없습니다." })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
  });
});
