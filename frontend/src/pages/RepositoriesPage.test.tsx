import { screen } from "@testing-library/react";
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
