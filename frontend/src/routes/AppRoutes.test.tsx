import { render, screen, waitFor } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import { appRoutes } from "./AppRoutes";

vi.mock("../pages/HomePage", () => ({
  HomePage: () => { throw new Error("sensitive route failure"); }
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
