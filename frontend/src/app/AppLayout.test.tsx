import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";
import { AppLayout, appNavigation } from "./AppLayout";

afterEach(() => vi.unstubAllGlobals());

it("shows exactly the implemented D-1 navigation and marks the current area", () => {
  renderWithProviders(<AppLayout />, ["/skills/skill-1"]);
  const navigation = screen.getByRole("navigation", { name: "주요 메뉴" });
  const links = Array.from(navigation.querySelectorAll("a"));
  expect(links.map(link => [link.textContent, link.getAttribute("href")])).toEqual(appNavigation.map(item => [...item]));
  expect(screen.getByRole("link", { name: "기술 역량" })).toHaveAttribute("aria-current", "true");
  expect(screen.getByRole("button", { name: "로그아웃" })).toHaveClass("button-secondary");
});

it("marks ancestor navigation without calling it the exact page", () => {
  renderWithProviders(<AppLayout />, ["/settings/profile"]);
  expect(screen.getByRole("link", { name: "설정" })).toHaveAttribute("aria-current", "true");
});

it.each(["/careers", "/careers/backend", "/companies", "/companies/toss"])("maps %s to the settings section", (path) => {
  renderWithProviders(<AppLayout />, [path]);
  expect(screen.getByRole("link", { name: "설정" })).toHaveAttribute("aria-current", "true");
});

it("does not highlight global navigation during onboarding", () => {
  renderWithProviders(<AppLayout />, ["/onboarding"]);
  expect(screen.getByRole("navigation", { name: "주요 메뉴" }).querySelector("[aria-current]")).not.toBeInTheDocument();
});

it("toggles the mobile menu with its native button and returns focus after Escape", () => {
  renderWithProviders(<AppLayout />, ["/dashboard"]);
  const button = screen.getByRole("button", { name: "메뉴" });
  button.focus();
  fireEvent.keyDown(button, { key: "Enter" });
  fireEvent.click(button);
  expect(button).toHaveAttribute("aria-expanded", "true");
  expect(screen.getByRole("navigation", { name: "주요 메뉴" })).toHaveClass("app-navigation--open");
  fireEvent.keyDown(document, { key: "Escape" });
  expect(button).toHaveAttribute("aria-expanded", "false");
  expect(button).toHaveFocus();
});

it("keeps the menu open when another handler prevents Escape", () => {
  renderWithProviders(<AppLayout />, ["/dashboard"]);
  const button = screen.getByRole("button", { name: "메뉴" });
  fireEvent.click(button);
  const escape = new KeyboardEvent("keydown", { key: "Escape", bubbles: true, cancelable: true });
  escape.preventDefault();
  document.dispatchEvent(escape);
  expect(button).toHaveAttribute("aria-expanded", "true");
});

it("closes an already-current mobile link and moves focus to main", () => {
  renderWithProviders(<AppLayout />, ["/dashboard"]);
  const button = screen.getByRole("button", { name: "메뉴" });
  fireEvent.click(button);
  fireEvent.click(screen.getByRole("link", { name: "대시보드" }));
  expect(button).toHaveAttribute("aria-expanded", "false");
  return waitFor(() => expect(screen.getByRole("main")).toHaveFocus());
});

it("closes the menu whenever the pathname changes", async () => {
  const { router } = renderWithProviders(<AppLayout />, ["/dashboard"]);
  const button = screen.getByRole("button", { name: "메뉴" });
  fireEvent.click(button);
  await router.navigate("/skills");
  await waitFor(() => expect(button).toHaveAttribute("aria-expanded", "false"));
});

it("returns to the public home after logout", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => String(input).endsWith("/csrf")
    ? Promise.resolve(Response.json({ data: { headerName: "X-CSRF-TOKEN", parameterName: "_csrf", token: "csrf" } }))
    : Promise.resolve(new Response(null, { status: 204 }))));
  const { router } = renderWithProviders(<AppLayout />, ["/dashboard"]);
  fireEvent.click(screen.getByRole("button", { name: "로그아웃" }));
  await waitFor(() => expect(router.state.location.pathname).toBe("/"));
});

it("confirms unsaved profile changes before calling logout", async () => {
  const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };
  let logoutAttempts = 0;
  const fetchMock = vi.fn((input: string | URL | Request, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/csrf")) return Promise.resolve(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf" }, metadata }));
    if (url.endsWith("/session/logout") && init?.method === "POST") return Promise.resolve(new Response(null, { status: ++logoutAttempts === 1 ? 500 : 204 }));
    const data = url.endsWith("/profile")
      ? { profileId: "profile-id", displayName: "DevPath User", careerStage: "JUNIOR", bio: "Saved bio", updatedAt: metadata.timestamp }
      : url.endsWith("/preferences")
        ? { careerId: null, companyId: null, updatedAt: metadata.timestamp }
        : url.endsWith("/careers")
          ? { catalogVersion: "career-v1", careers: [] }
          : { catalogVersion: "company-v1", companies: [] };
    return Promise.resolve(Response.json({ data, metadata }));
  });
  vi.stubGlobal("fetch", fetchMock);
  const { router } = renderWithProviders(undefined, ["/settings/profile"]);
  fireEvent.change(await screen.findByRole("textbox", { name: "자기소개" }), { target: { value: "Unsaved bio" } });

  const menu = screen.getByRole("button", { name: "메뉴" });
  fireEvent.click(menu);
  fireEvent.click(screen.getByRole("button", { name: "로그아웃" }));
  const dialog = screen.getByRole("dialog", { name: "저장하지 않은 변경사항이 있습니다" });
  expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith("/session/logout"))).toBe(false);
  fireEvent.keyDown(document, { key: "Escape" });
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(menu).toHaveAttribute("aria-expanded", "true");
  expect(router.state.location.pathname).toBe("/settings/profile");

  fireEvent.click(screen.getByRole("button", { name: "로그아웃" }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "변경사항 버리고 로그아웃" }));
  await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith("/session/logout"))).toBe(true));
  await waitFor(() => expect(screen.getAllByRole("alert")).toHaveLength(1));
  expect(within(screen.getByRole("dialog")).getByRole("alert")).toHaveTextContent("로그아웃에 실패했습니다");
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "변경사항 버리고 로그아웃" }));
  await waitFor(() => expect(router.state.location.pathname).toBe("/"));
});
