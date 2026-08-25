import { screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { CareersPage } from "./CareersPage";
afterEach(() => vi.unstubAllGlobals());
it("shows the selected career from the authoritative catalog", async () => {
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => Promise.resolve(Response.json({ data: input.toString().endsWith("/careers") ? { catalogVersion: "career-v1", careers: [{ careerId: "backend", name: "Backend Engineer", localizedName: "백엔드 엔지니어", status: "SUPPORTED", profileVersion: "career-v1", purpose: "서버 측 API를 구현합니다." }] } : { careerId: "backend", companyId: null, updatedAt: "2026-08-12T00:00:00Z" }, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-12T00:00:00Z" } }))));
  renderWithProviders(<Routes><Route path="/careers" element={<CareersPage />} /></Routes>, ["/careers"]);
  expect(await screen.findByRole("heading", { name: "지원 커리어" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "백엔드 엔지니어" })).toBeInTheDocument();
  expect(screen.getByText("현재 목표")).toBeInTheDocument();
});
