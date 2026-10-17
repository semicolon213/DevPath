import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../../test/renderWithProviders";
import { ProfilePanel } from "./ProfilePanel";

afterEach(() => vi.unstubAllGlobals());
it("renders profile and authoritative career target controls", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
    const url = String(input);
    const data = url.endsWith("/careers")
      ? { catalogVersion: "career-v1", careers: [{ careerId: "backend", name: "Backend Engineer", localizedName: "백엔드 엔지니어", status: "SUPPORTED", profileVersion: "career-v1", purpose: "API 구현" }] }
      : url.endsWith("/companies")
        ? { catalogVersion: "company-v1", companies: [{ companyId: "toss", name: "Toss", localizedName: "토스", status: "SUPPORTED", profileVersion: "company-v1", engineeringCulture: "정확성" }] }
      : url.endsWith("/profile")
        ? { profileId: "profile-id", displayName: "DevPath User", careerStage: "JUNIOR", bio: "Building APIs", updatedAt: "2026-08-11T00:00:00Z" }
        : url.endsWith("/connections") ? { connections: [] }
          : { careerId: "backend", companyId: "toss", updatedAt: "2026-08-11T00:00:00Z" };
    return Promise.resolve(Response.json({ data, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" } }));
  }));
  const { container } = renderWithProviders(<ProfilePanel />);
  expect(await screen.findByDisplayValue("DevPath User")).toBeInTheDocument();
  expect(screen.getByDisplayValue("Building APIs")).toBeInTheDocument();
  expect(screen.getByRole("option", { name: "백엔드 엔지니어", selected: true })).toBeInTheDocument();
  expect(screen.getByRole("option", { name: "토스", selected: true })).toBeInTheDocument();
  expect(container.querySelector("#targets")).toContainElement(screen.getByRole("combobox", { name: "목표 직무" }));
  expect(container.querySelector("#targets")).toContainElement(screen.getByRole("combobox", { name: "목표 회사" }));
});

it("requires an explicit confirmed target change and announces success", async () => {
  const fetchMock = profileFetch();
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(<ProfilePanel />);
  const careerTarget = await screen.findByRole("combobox", { name: "목표 직무" });
  fireEvent.change(careerTarget, { target: { value: "frontend" } });
  expect(fetchMock.mock.calls.some(([, init]) => init?.method === "PUT")).toBe(false);
  fireEvent.click(screen.getByRole("button", { name: "목표 직무 변경" }));
  expect(screen.getByRole("dialog")).toHaveTextContent("이 화면에서는 재평가를 시작하지 않습니다");
  expect(fetchMock.mock.calls.some(([, init]) => init?.method === "PUT")).toBe(false);
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "목표 직무 변경" }));
  expect(await screen.findByText("목표 직무를 변경했습니다.")).toHaveAttribute("role", "status");
  expect(fetchMock.mock.calls.some(([input, init]) => String(input).endsWith("/preferences/career") && init?.method === "PUT")).toBe(true);
});

it("announces a failed target change", async () => {
  vi.stubGlobal("fetch", profileFetch(true));
  renderWithProviders(<ProfilePanel />);
  fireEvent.change(await screen.findByRole("combobox", { name: "목표 회사" }), { target: { value: "line" } });
  fireEvent.click(screen.getByRole("button", { name: "목표 회사 변경" }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "목표 회사 변경" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("목표 회사를 변경하지 못했습니다");
});

it("shows field errors and focuses the error summary", async () => {
  vi.stubGlobal("fetch", profileFetch());
  renderWithProviders(<ProfilePanel />);
  fireEvent.change(await screen.findByRole("textbox", { name: "표시 이름" }), { target: { value: " " } });
  fireEvent.change(screen.getByRole("textbox", { name: "자기소개" }), { target: { value: "a".repeat(1001) } });
  fireEvent.click(screen.getByRole("button", { name: "프로필 저장" }));
  const summary = await screen.findByText("입력 내용을 확인해 주세요.");
  expect(screen.getAllByText("표시 이름을 입력해 주세요.")).toHaveLength(2);
  expect(screen.getAllByText("자기소개는 1,000자 이하로 입력해 주세요.")).toHaveLength(2);
  await waitFor(() => expect(summary.closest("[role='alert']")).toHaveFocus());
  fireEvent.click(screen.getByRole("link", { name: "표시 이름을 입력해 주세요." }));
  expect(screen.getByRole("textbox", { name: "표시 이름" })).toHaveFocus();
});

it("guards browser exit while profile edits are unsaved", async () => {
  vi.stubGlobal("fetch", profileFetch());
  renderWithProviders(<ProfilePanel />);
  fireEvent.change(await screen.findByRole("textbox", { name: "자기소개" }), { target: { value: "Unsaved bio" } });
  const beforeUnload = new Event("beforeunload", { cancelable: true });
  window.dispatchEvent(beforeUnload);
  expect(beforeUnload.defaultPrevented).toBe(true);
});

function profileFetch(failCompany = false) {
  const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };
  return vi.fn((input: string | URL | Request, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/csrf")) return Promise.resolve(Response.json({ data: { headerName: "X-CSRF-TOKEN", parameterName: "_csrf", token: "csrf" }, metadata }));
    if (url.endsWith("/preferences/career") && init?.method === "PUT") return Promise.resolve(Response.json({ data: { careerId: "frontend", companyId: "toss", updatedAt: metadata.timestamp }, metadata }));
    if (url.endsWith("/preferences/company") && init?.method === "PUT") return Promise.resolve(failCompany
      ? Response.json({ error: { code: "FAILED", message: "safe" }, metadata }, { status: 503 })
      : Response.json({ data: { careerId: "backend", companyId: "line", updatedAt: metadata.timestamp }, metadata }));
    const data = url.endsWith("/careers")
      ? { catalogVersion: "career-v1", careers: [{ careerId: "backend", localizedName: "백엔드 엔지니어" }, { careerId: "frontend", localizedName: "프론트엔드 엔지니어" }] }
      : url.endsWith("/companies")
        ? { catalogVersion: "company-v1", companies: [{ companyId: "toss", localizedName: "토스" }, { companyId: "line", localizedName: "라인" }] }
        : url.endsWith("/profile")
          ? { profileId: "profile-id", displayName: "DevPath User", careerStage: "JUNIOR", bio: "Building APIs", updatedAt: metadata.timestamp }
          : { careerId: "backend", companyId: "toss", updatedAt: metadata.timestamp };
    return Promise.resolve(Response.json({ data, metadata }));
  });
}
