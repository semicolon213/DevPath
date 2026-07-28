import { screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { AppRoutes } from "../routes/AppRoutes";
import { QueryClientProbe, renderWithProviders } from "../test/renderWithProviders";

describe("DevPath frontend scaffold", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            error: { code: "AUTHENTICATION_REQUIRED", message: "Authentication is required." }
          }),
          { status: 401, headers: { "Content-Type": "application/json" } }
        )
      )
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders the application shell on the root route", () => {
    renderWithProviders(<AppRoutes />);

    expect(screen.getByRole("heading", { name: "DevPath" })).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Checking your DevPath session");
  });

  it("renders the not-found route", () => {
    renderWithProviders(<AppRoutes />, ["/missing"]);

    expect(screen.getByRole("heading", { name: "Page not found" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Return to DevPath home" })).toBeInTheDocument();
  });

  it("provides a React Query client to route-level components", () => {
    renderWithProviders(<QueryClientProbe />);

    expect(screen.getByTestId("query-client-present")).toHaveTextContent("true");
  });
});
