import { render, screen, waitFor } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import { appRoutes } from "./AppRoutes";

vi.mock("../pages/HomePage", () => ({
  HomePage: () => { throw new Error("sensitive route failure"); }
}));
vi.mock("../pages/DashboardPage", () => ({
  DashboardPage: () => { throw new Error("sensitive authenticated route failure"); }
}));
vi.mock("../features/session/model/useSession", () => ({
  useLogout: () => ({ isPending: false, isError: false, mutate: vi.fn() })
}));

afterEach(() => vi.restoreAllMocks());

it("renders a safe route error without exposing the error message or stack", async () => {
  const consoleError = vi.spyOn(console, "error").mockImplementation(() => undefined);
  const secretMessage = "sensitive route failure";
  const router = createMemoryRouter(appRoutes, { initialEntries: ["/"] });
  const { container } = render(<RouterProvider router={router} />);
  expect(await screen.findByRole("alert")).toHaveTextContent("화면을 표시하지 못했습니다");
  expect(screen.getByRole("button", { name: "페이지 새로고침" })).toBeInTheDocument();
  expect(container).not.toHaveTextContent(secretMessage);
  await waitFor(() => expect(consoleError).toHaveBeenCalledWith("DevPath route error boundary captured an error.", expect.any(Error)));
});

it("keeps the authenticated layout when a page render fails", async () => {
  vi.spyOn(console, "error").mockImplementation(() => undefined);
  const router = createMemoryRouter(appRoutes, { initialEntries: ["/dashboard"] });
  const { container } = render(<RouterProvider router={router} />);
  expect(await screen.findByRole("alert")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "DevPath" })).toHaveAttribute("href", "/dashboard");
  expect(screen.getByRole("navigation")).toBeInTheDocument();
  expect(container).not.toHaveTextContent("sensitive authenticated route failure");
});
