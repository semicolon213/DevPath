import { fireEvent, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { renderWithProviders } from "../../test/renderWithProviders";
import { ApiError } from "../api/apiClient";
import { PageState } from "./PageState";

it("renders loading with its page heading and bounded status skeleton", () => {
  renderWithProviders(<PageState state="loading" title="내 저장소" message="불러오는 중입니다." />);
  expect(screen.getByRole("heading", { name: "내 저장소" })).toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent("불러오는 중입니다.");
  screen.getByRole("status").querySelectorAll("i").forEach(bar => expect(bar).toHaveAttribute("aria-hidden", "true"));
});

it("moves loading focus to the final page heading", async () => {
  const view = renderWithProviders(<main><PageState state="loading" title="불러오는 페이지" message="불러오는 중입니다." /></main>);
  const loadingHeading = screen.getByRole("heading", { name: "불러오는 페이지" });
  loadingHeading.tabIndex = -1;
  loadingHeading.focus();
  view.rerender(<main><h1>완료된 페이지</h1></main>);
  await waitFor(() => expect(screen.getByRole("heading", { name: "완료된 페이지" })).toHaveFocus());
});

it("renders an empty state with its next action", () => {
  renderWithProviders(<PageState state="empty" title="결과가 없습니다." actionTo="/repositories" actionLabel="저장소 보기" />);
  expect(screen.getByRole("link", { name: "저장소 보기" })).toHaveAttribute("href", "/repositories");
});

it("sends a 401 response to the home login entry", () => {
  renderWithProviders(<PageState state="error" title="불러오지 못했습니다." error={new ApiError(401, "unauthorized")} />);
  expect(screen.getByRole("heading", { name: "로그인이 필요합니다." })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "홈으로 이동" })).toHaveAttribute("href", "/");
});

it("uses existence-safe wording and a back action for a 404", () => {
  renderWithProviders(<PageState state="error" title="정보를 표시할 수 없습니다." error={new ApiError(404, "missing")} backTo="/items" backLabel="목록으로 이동" />);
  expect(screen.getByRole("alert")).toHaveTextContent("없거나 현재 계정으로 접근할 수 없습니다");
  expect(screen.getByRole("link", { name: "목록으로 이동" })).toHaveAttribute("href", "/items");
});

it("distinguishes a network failure and retries", () => {
  const retry = vi.fn();
  renderWithProviders(<PageState state="error" title="불러오지 못했습니다." error={new TypeError("offline")} retry={retry} />);
  expect(screen.getByRole("heading", { name: "서버에 연결할 수 없습니다." })).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
  expect(retry).toHaveBeenCalledOnce();
});

it("shows an internal error request reference and retries", () => {
  renderWithProviders(<PageState state="error" title="찾을 수 없습니다." errorTitle="불러오지 못했습니다." error={new ApiError(500, "failed", "request-123")} retry={() => undefined} />);
  expect(screen.getByRole("heading", { name: "불러오지 못했습니다." })).toBeInTheDocument();
  expect(screen.getByText("request-123")).toHaveClass("page-state-reference");
  expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
});
