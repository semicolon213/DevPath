import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { CareerDetailPage } from "./CareerDetailPage";
afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-12T00:00:00Z" };
const profile = { careerId: "backend", name: "Backend Engineer", localizedName: "백엔드 엔지니어", status: "SUPPORTED", careerProfileVersionId: "profile-id", profileVersion: "career-v1", purpose: "서버 측 API를 구현합니다.", coreTechnologies: ["Java", "Spring Boot"], requiredCompetencies: ["API 설계", "테스트"], preferredCompetencies: ["CI/CD"], evaluationCategories: ["TESTING"], priorityWeights: { TESTING: "HIGH" }, roadmapTemplate: ["언어", "프레임워크", "테스트"], effectiveAt: metadata.timestamp };
function mockDetail({ selected = true, failMutation = false } = {}) {
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/careers/backend")) return Promise.resolve(Response.json({ data: profile, metadata }));
    if (url.endsWith("/csrf")) return Promise.resolve(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf" }, metadata }));
    if (url.endsWith("/preferences/career") && init?.method === "PUT") return Promise.resolve(failMutation
      ? Response.json({ error: { code: "FAILED" } }, { status: 500 })
      : Response.json({ data: { careerId: "backend", companyId: null, updatedAt: metadata.timestamp }, metadata }));
    return Promise.resolve(Response.json({ data: { careerId: selected ? "backend" : "frontend", companyId: null, updatedAt: metadata.timestamp }, metadata }));
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}
function renderPage() { renderWithProviders(<Routes><Route path="/careers/:careerId" element={<CareerDetailPage />} /></Routes>, ["/careers/backend"]); }
it("shows versioned expectations without claiming readiness", async () => {
  mockDetail();
  renderPage();
  expect(await screen.findByRole("heading", { name: "백엔드 엔지니어" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "필수 역량" })).toBeInTheDocument();
  expect(screen.getByText("API 설계")).toBeInTheDocument();
  expect(screen.getByText("공식 준비도와 추천은 아직 계산하지 않으며, 활성 프로필에 저장된 순서입니다.")).toBeInTheDocument();
});

it("confirms the target impact, announces success, and focuses the current target", async () => {
  const fetchMock = mockDetail({ selected: false });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "이 직무를 목표로 설정" }));
  expect(screen.getByRole("dialog")).toHaveTextContent("준비도와 추천이 새 직무 기준으로 다시 계산될 수 있습니다");
  fireEvent.click(screen.getByRole("button", { name: "목표 직무 변경" }));
  const current = await screen.findByText("현재 목표 직무");
  expect(await screen.findByText(/목표 직무를 변경했습니다/)).toBeInTheDocument();
  await waitFor(() => expect(current.parentElement).toHaveFocus());
  expect(screen.getAllByRole("status")).toHaveLength(1);
  expect(fetchMock.mock.calls.some(([input, init]) => String(input).endsWith("/preferences/career") && init?.method === "PUT")).toBe(true);
});

it("cancels target selection without changing preferences", async () => {
  const fetchMock = mockDetail({ selected: false });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "이 직무를 목표로 설정" }));
  fireEvent.click(screen.getByRole("button", { name: "취소" }));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith("/preferences/career"))).toBe(false);
});

it("keeps a selection failure inside the confirmation dialog", async () => {
  mockDetail({ selected: false, failMutation: true });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "이 직무를 목표로 설정" }));
  fireEvent.click(screen.getByRole("button", { name: "목표 직무 변경" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("목표 직무를 변경하지 못했습니다. 다시 시도해 주세요.");
  expect(screen.getByRole("dialog")).toBeInTheDocument();
});
