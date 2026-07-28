import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "../../../test/renderWithProviders";
import { SessionPanel } from "./SessionPanel";

describe("SessionPanel", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders an anonymous login action after a 401", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));

    renderWithProviders(<SessionPanel />);

    expect(await screen.findByRole("link", { name: "Continue with GitHub" })).toHaveAttribute(
      "href",
      "http://localhost:8080/oauth2/authorization/github"
    );
  });

  it("renders the authenticated user returned by the server", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json({
          data: {
            userId: "e046a279-9c82-4bbf-9d8f-0737b222fa97",
            displayName: "DevPath User",
            avatarUrl: null,
            status: "ACTIVE",
            authenticationProvider: "GITHUB",
            createdAt: "2026-07-27T00:00:00Z"
          },
          metadata: {
            requestId: "request-3",
            apiVersion: "v1",
            timestamp: "2026-07-27T00:00:00Z"
          }
        })
      )
    );

    renderWithProviders(<SessionPanel />);

    expect(await screen.findByRole("heading", { name: "Welcome, DevPath User" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign out" })).toBeInTheDocument();
  });

  it("distinguishes a network failure from an anonymous session", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("network unavailable")));

    renderWithProviders(<SessionPanel />);

    expect(await screen.findByRole("heading", { name: "Session unavailable" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
  });
});
