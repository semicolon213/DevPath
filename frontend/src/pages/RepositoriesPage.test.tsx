import { fireEvent, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";
import { RepositoriesPage } from "./RepositoriesPage";

afterEach(() => vi.unstubAllGlobals());

it("renders a visible repository workspace with summaries and registered repositories", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({
    data: {
      repositories: [{
        repositoryId: "3fd75d74-17d4-4dc5-bf3b-251f611633f2",
        providerRepositoryId: "42",
        name: "devpath",
        fullName: "owner/devpath",
        owner: "owner",
        visibility: "PRIVATE",
        defaultBranch: "main",
        providerArchived: false,
        lifecycle: "DISCOVERED",
        syncStatus: "NOT_SYNCED",
        htmlUrl: "https://github.com/owner/devpath",
        discoveredAt: "2026-08-11T00:00:00Z",
        lastSyncedAt: null,
        currentSnapshotId: null
      }],
      limit: 20,
      nextCursor: null,
      totalCount: 1
    },
    metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
  })));

  renderWithProviders(<RepositoriesPage />, ["/repositories"]);

  expect(await screen.findByRole("heading", { name: "내 GitHub 저장소" })).toBeInTheDocument();
  expect(screen.getByText("현재 표시된 저장소")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "등록된 저장소" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "owner/devpath" })).toBeInTheDocument();
  expect(screen.getByText("메타데이터만 등록됨")).toBeInTheDocument();
  expect(screen.getByRole("checkbox", { name: "보관 저장소 포함" })).not.toBeChecked();
});

it("archives, filters, and restores an owned repository from the workspace", async () => {
  let lifecycle: "ACTIVE" | "ARCHIVED" = "ACTIVE";
  const repository = {
    repositoryId: "3fd75d74-17d4-4dc5-bf3b-251f611633f2", providerRepositoryId: "42",
    name: "devpath", fullName: "owner/devpath", owner: "owner", visibility: "PRIVATE",
    defaultBranch: "main", providerArchived: false, lifecycle, syncStatus: "SYNCHRONIZED",
    htmlUrl: "https://github.com/owner/devpath", discoveredAt: "2026-08-11T00:00:00Z",
    lastSyncedAt: "2026-08-11T01:00:00Z", currentSnapshotId: "snapshot-id"
  } as const;
  const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/csrf")) return Promise.resolve(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf" }, metadata }));
    if (init?.method === "POST" && url.endsWith("/archive")) {
      lifecycle = "ARCHIVED";
      return Promise.resolve(Response.json({ data: { ...repository, lifecycle }, metadata }));
    }
    if (init?.method === "POST" && url.endsWith("/restore")) {
      lifecycle = "ACTIVE";
      return Promise.resolve(Response.json({ data: { ...repository, lifecycle }, metadata }));
    }
    const includeArchived = new URL(url).searchParams.get("includeArchived") === "true";
    const repositories = lifecycle === "ARCHIVED" && !includeArchived ? [] : [{ ...repository, lifecycle }];
    return Promise.resolve(Response.json({ data: { repositories, limit: 20, nextCursor: null, totalCount: repositories.length }, metadata }));
  });
  vi.stubGlobal("fetch", fetchMock);

  renderWithProviders(<RepositoriesPage />, ["/repositories"]);

  fireEvent.click(await screen.findByRole("button", { name: "보관" }));
  expect(screen.getByText(/기존 스냅샷과 분석 결과는 유지/)).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "보관 확인" }));
  expect(await screen.findByRole("heading", { name: "표시할 활성 저장소가 없습니다" })).toBeInTheDocument();
  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("/archive"), expect.objectContaining({ method: "POST" }));

  fireEvent.click(screen.getByRole("checkbox", { name: "보관 저장소 포함" }));
  expect(await screen.findByText("DevPath에서 보관됨")).toBeInTheDocument();
  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("includeArchived=true"), expect.anything());

  fireEvent.click(screen.getByRole("button", { name: "복원" }));
  expect(await screen.findByText("동기화됨")).toBeInTheDocument();
  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("/restore"), expect.objectContaining({ method: "POST" }));
});
