import { screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";
import { ProfileSettingsPage } from "./ProfileSettingsPage";
import { IntegrationSettingsPage } from "./IntegrationSettingsPage";
import { OnboardingPage } from "./OnboardingPage";

afterEach(() => vi.unstubAllGlobals());

it.each([
  ["profile", <ProfileSettingsPage />, "커리어 프로필"],
  ["integrations", <IntegrationSettingsPage />, "외부 서비스 연결"],
  ["onboarding", <OnboardingPage />, "커리어 프로필"]
])("does not skip heading levels on the %s settings page", async (_name, page, panelHeading) => {
  vi.stubGlobal("fetch", settingsFetch());
  const { container } = renderWithProviders(page);
  await screen.findByRole("heading", { name: panelHeading });
  const levels = Array.from(container.querySelectorAll("h1, h2, h3, h4, h5, h6")).map(heading => Number(heading.tagName.slice(1)));
  expect(levels[0]).toBe(1);
  levels.slice(1).forEach((level, index) => expect(level).toBeLessThanOrEqual(levels[index] + 1));
});

function settingsFetch() {
  const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };
  return vi.fn((input: string | URL | Request) => {
    const url = String(input);
    const data = url.endsWith("/onboarding-progress")
      ? { status: "IN_PROGRESS", completedStepCount: 0, totalStepCount: 1, nextStep: "PROFILE", steps: [{ step: "PROFILE", requirement: "REQUIRED", status: "INCOMPLETE", resourceId: null, actionPath: "/settings/profile" }], generatedAt: metadata.timestamp }
      : url.endsWith("/careers")
      ? { catalogVersion: "career-v1", careers: [] }
      : url.endsWith("/companies")
        ? { catalogVersion: "company-v1", companies: [] }
        : url.endsWith("/profile")
          ? { profileId: "profile-id", displayName: "DevPath User", careerStage: null, bio: null, updatedAt: metadata.timestamp }
          : url.endsWith("/preferences")
            ? { careerId: null, companyId: null, updatedAt: null }
            : { connections: [] };
    return Promise.resolve(Response.json({ data, metadata }));
  });
}
