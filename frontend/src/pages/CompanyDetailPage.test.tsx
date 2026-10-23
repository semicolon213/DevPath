import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { CompanyDetailPage } from "./CompanyDetailPage";

afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-12T00:00:00Z" };
const profile = { companyId: "toss", name: "Toss", localizedName: "토스", status: "SUPPORTED", companyProfileVersionId: "p", profileVersion: "company-v1", engineeringCulture: "빠른 반복과 정확성", technologyFocus: ["테스트"], preferredCompetencies: ["신뢰성"], recommendationPriorities: ["테스트"], skillEmphasis: ["정확성"], weightOverrides: { TESTING: "INCREASE" }, effectiveAt: metadata.timestamp };
function mockDetail({ selected = true, failMutation = false } = {}) {
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/companies/toss")) return Promise.resolve(Response.json({ data: profile, metadata }));
    if (url.endsWith("/csrf")) return Promise.resolve(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf" }, metadata }));
    if (url.endsWith("/preferences/company") && init?.method === "PUT") return Promise.resolve(failMutation
      ? Response.json({ error: { code: "FAILED" } }, { status: 500 })
      : Response.json({ data: { careerId: "backend", companyId: "toss", updatedAt: metadata.timestamp }, metadata }));
    return Promise.resolve(Response.json({ data: { careerId: "backend", companyId: selected ? "toss" : "naver", updatedAt: null }, metadata }));
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}
function renderPage(id = "toss") { renderWithProviders(<Routes><Route path="/companies/:id" element={<CompanyDetailPage />} /></Routes>, [`/companies/${id}`]); }

it("shows generic versioned emphasis and guardrail", async () => {
  mockDetail();
  renderPage();
  expect(await screen.findByRole("heading", { name: "토스" })).toBeInTheDocument();
  expect(screen.getByText(/비공개 채용 정보나 합격 예측이 아닙니다/)).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "기술 관심사" })).toBeInTheDocument();
});

it("confirms the target impact, announces success, and focuses the current target", async () => {
  const fetchMock = mockDetail({ selected: false });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "이 회사를 목표로 설정" }));
  expect(screen.getByRole("dialog")).toHaveTextContent("현재 회사별 준비도와 추천 정책은 적용되지 않으며");
  fireEvent.click(screen.getByRole("button", { name: "목표 회사 변경" }));
  const current = await screen.findByText("현재 목표 회사");
  expect(await screen.findByText(/목표 회사를 변경했습니다/)).toBeInTheDocument();
  await waitFor(() => expect(current.parentElement).toHaveFocus());
  expect(screen.getAllByRole("status")).toHaveLength(1);
  expect(fetchMock.mock.calls.some(([input, init]) => String(input).endsWith("/preferences/company") && init?.method === "PUT")).toBe(true);
});

it("cancels target selection without changing preferences", async () => {
  const fetchMock = mockDetail({ selected: false });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "이 회사를 목표로 설정" }));
  fireEvent.click(screen.getByRole("button", { name: "취소" }));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith("/preferences/company"))).toBe(false);
});

it("keeps a selection failure inside the confirmation dialog", async () => {
  mockDetail({ selected: false, failMutation: true });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "이 회사를 목표로 설정" }));
  fireEvent.click(screen.getByRole("button", { name: "목표 회사 변경" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("목표 회사를 변경하지 못했습니다. 다시 시도해 주세요.");
  expect(screen.getByRole("dialog")).toBeInTheDocument();
});

it("uses existence-safe wording and preserves the companies back link", async () => {
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(Response.json({ error: { code: "NOT_FOUND" } }, { status: 404 }))));
  renderPage("missing");
  expect(await screen.findByRole("alert")).toHaveTextContent("없거나 현재 계정으로 접근할 수 없습니다");
  expect(screen.getByRole("link", { name: "지원 회사로 돌아가기" })).toHaveAttribute("href", "/companies");
});

it("keeps the detail heading visible while loading and retries a non-404 failure", async () => {
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(Response.json({ error: { code: "FAILED" } }, { status: 500 }))));
  renderPage();
  expect(screen.getByRole("heading", { name: "회사 프로필" })).toBeInTheDocument();
  expect(await screen.findByRole("heading", { name: "회사 프로필을 표시할 수 없습니다" })).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
  expect(fetch).toHaveBeenCalled();
});

it("shows the login action for an anonymous detail request", async () => {
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(Response.json({ error: { code: "UNAUTHORIZED" } }, { status: 401 }))));
  renderPage();
  expect(await screen.findByRole("heading", { name: "로그인이 필요합니다." })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "홈으로 이동" })).toHaveAttribute("href", "/");
});
