import { screen } from "@testing-library/react";
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
  renderWithProviders(<ProfilePanel />);
  expect(await screen.findByDisplayValue("DevPath User")).toBeInTheDocument();
  expect(screen.getByDisplayValue("Building APIs")).toBeInTheDocument();
  expect(screen.getByRole("option", { name: "백엔드 엔지니어", selected: true })).toBeInTheDocument();
  expect(screen.getByRole("option", { name: "토스", selected: true })).toBeInTheDocument();
});
