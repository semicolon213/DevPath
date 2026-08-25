import { screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { CareerDetailPage } from "./CareerDetailPage";
afterEach(() => vi.unstubAllGlobals());
it("shows versioned expectations without claiming readiness", async () => {
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => Promise.resolve(Response.json({ data: input.toString().endsWith("/careers/backend") ? { careerId: "backend", name: "Backend Engineer", localizedName: "백엔드 엔지니어", status: "SUPPORTED", careerProfileVersionId: "profile-id", profileVersion: "career-v1", purpose: "서버 측 API를 구현합니다.", coreTechnologies: ["Java", "Spring Boot"], requiredCompetencies: ["API 설계", "테스트"], preferredCompetencies: ["CI/CD"], evaluationCategories: ["TESTING"], priorityWeights: { TESTING: "HIGH" }, roadmapTemplate: ["언어", "프레임워크", "테스트"], effectiveAt: "2026-08-12T00:00:00Z" } : { careerId: "backend", companyId: null, updatedAt: "2026-08-12T00:00:00Z" }, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-12T00:00:00Z" } }))));
  renderWithProviders(<Routes><Route path="/careers/:careerId" element={<CareerDetailPage />} /></Routes>, ["/careers/backend"]);
  expect(await screen.findByRole("heading", { name: "백엔드 엔지니어" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "필수 역량" })).toBeInTheDocument();
  expect(screen.getByText("API 설계")).toBeInTheDocument();
  expect(screen.getByText("공식 준비도와 추천은 아직 계산하지 않으며, 활성 프로필에 저장된 순서입니다.")).toBeInTheDocument();
});
