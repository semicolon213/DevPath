import { screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../../test/renderWithProviders";
import { ConnectionPanel } from "./ConnectionPanel";

afterEach(() => vi.unstubAllGlobals());

it("distinguishes GitHub login from repository access", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({
    data: { connections: [] },
    metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
  })));

  renderWithProviders(<ConnectionPanel />);

  expect(await screen.findAllByText("미연결")).toHaveLength(2);
  expect(screen.getByText(/GitHub 로그인은 완료되었지만/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "GitHub 저장소 연결" })).toBeInTheDocument();
});

it("shows discovered Notion page metadata without page content", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
    const url = String(input);
    const data = url.endsWith("/notion/workspaces")
      ? { workspaces: [{
        connectionId: "4a0b0a84-43ec-4d09-b5d5-7db7cc9369b8",
        workspaceId: "workspace-1",
        workspaceName: "DevPath Notes",
        workspaceIconUrl: null,
        status: "ACTIVE",
        connectedAt: "2026-08-27T00:00:00Z",
        discoveredAt: "2026-08-27T01:00:00Z",
        pages: [{ providerPageId: "page-1", title: "Backend roadmap", objectType: "PAGE",
          url: "https://notion.so/page-1", lastEditedAt: "2026-08-27T00:30:00Z", inTrash: false }]
      }] }
      : { connections: [{ connectionId: "4a0b0a84-43ec-4d09-b5d5-7db7cc9369b8", provider: "NOTION",
        status: "ACTIVE", scopes: ["read_content"], connectedAt: "2026-08-27T00:00:00Z", expiresAt: null }] };
    return Promise.resolve(Response.json({ data, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-27T01:00:00Z" } }));
  }));

  renderWithProviders(<ConnectionPanel />);

  expect(await screen.findByRole("heading", { name: "DevPath Notes" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Backend roadmap" })).toHaveAttribute("href", "https://notion.so/page-1");
  expect(screen.getByText(/페이지 본문은 저장하지 않습니다/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "페이지 목록 새로 고침" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Notion 연결 해제" })).toBeInTheDocument();
});

it("does not expose untrusted provider URLs and explains an all-trashed workspace", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
    const url = String(input);
    const data = url.endsWith("/notion/workspaces")
      ? { workspaces: [{
        connectionId: "4a0b0a84-43ec-4d09-b5d5-7db7cc9369b8", workspaceId: "workspace-1",
        workspaceName: "Private Notes", workspaceIconUrl: null, status: "ACTIVE",
        connectedAt: "2026-08-27T00:00:00Z", discoveredAt: "2026-08-27T01:00:00Z",
        pages: [{ providerPageId: "page-1", title: "Removed note", objectType: "PAGE",
          url: "javascript:alert(1)", lastEditedAt: "2026-08-27T00:30:00Z", inTrash: true }]
      }] }
      : { connections: [{ connectionId: "4a0b0a84-43ec-4d09-b5d5-7db7cc9369b8", provider: "NOTION",
        status: "ACTIVE", scopes: ["read_content"], connectedAt: "2026-08-27T00:00:00Z", expiresAt: null }] };
    return Promise.resolve(Response.json({ data, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-27T01:00:00Z" } }));
  }));

  renderWithProviders(<ConnectionPanel />);

  expect(await screen.findByRole("heading", { name: "Private Notes" })).toBeInTheDocument();
  expect(screen.getByText(/공유된 활성 페이지가 없습니다/)).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Removed note" })).not.toBeInTheDocument();
});

it("labels a Notion rate limit without presenting it as a GitHub failure", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
    const url = String(input);
    if (url.endsWith("/notion/workspaces")) {
      return Promise.resolve(Response.json({
        error: { code: "RATE_LIMIT_EXCEEDED", message: "Retry later." },
        metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-27T01:00:00Z" }
      }, { status: 429, headers: { "Retry-After": "30" } }));
    }
    return Promise.resolve(Response.json({
      data: { connections: [{ connectionId: "4a0b0a84-43ec-4d09-b5d5-7db7cc9369b8", provider: "NOTION",
        status: "ACTIVE", scopes: ["read_content"], connectedAt: "2026-08-27T00:00:00Z", expiresAt: null }] },
      metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-27T01:00:00Z" }
    }));
  }));

  renderWithProviders(<ConnectionPanel />);

  expect(await screen.findByRole("alert")).toHaveTextContent("Notion 요청 한도를 모두 사용했습니다");
  expect(screen.getByRole("alert")).not.toHaveTextContent("GitHub 요청 한도");
});

it("shows an active repository-access connection reported by the server", async () => {
  vi.stubGlobal("fetch", vi.fn((input: string | URL | Request) => {
    const url = String(input);
    const data = url.endsWith("/repositories")
      ? { repositories: [{
        providerRepositoryId: "1",
        name: "devpath",
        fullName: "devpath-user/devpath",
        owner: "devpath-user",
        privateRepository: true,
        archived: false,
        defaultBranch: "main",
        htmlUrl: "https://github.com/devpath-user/devpath"
      }] }
      : { connections: [{
        connectionId: "e046a279-9c82-4bbf-9d8f-0737b222fa97",
        provider: "GITHUB",
        status: "ACTIVE",
        scopes: ["repo"],
        connectedAt: "2026-08-11T00:00:00Z",
        expiresAt: null
      }] };
    return Promise.resolve(Response.json({
    data,
    metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
    }));
  }));

  renderWithProviders(<ConnectionPanel />);

  expect(await screen.findByText("연결됨")).toBeInTheDocument();
  expect(screen.getByText(/저장소를 불러올 수 있도록/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "권한 다시 승인" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "연결 해제" })).toBeInTheDocument();
  expect(await screen.findByRole("link", { name: "devpath-user/devpath" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "DevPath에 추가" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "내 저장소 보기" })).toBeInTheDocument();
});

it.each([
  ["EXPIRED", "만료됨", /접근 권한이 만료되었습니다/],
  ["REVOKED", "권한 해제됨", /접근 권한이 해제되었습니다/]
] as const)("offers recovery for a %s GitHub connection without loading repositories", async (status, label, description) => {
  const fetchMock = vi.fn().mockResolvedValue(Response.json({
    data: { connections: [{
      connectionId: "e046a279-9c82-4bbf-9d8f-0737b222fa97",
      provider: "GITHUB",
      status,
      scopes: [],
      connectedAt: "2026-08-11T00:00:00Z",
      expiresAt: "2026-08-12T00:00:00Z"
    }] },
    metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
  }));
  vi.stubGlobal("fetch", fetchMock);

  renderWithProviders(<ConnectionPanel />);

  expect(await screen.findByText(label)).toBeInTheDocument();
  expect(screen.getByText(description)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "GitHub 다시 연결" })).toBeInTheDocument();
  expect(screen.queryByText("접근 가능한 저장소")).not.toBeInTheDocument();
  expect(fetchMock).toHaveBeenCalledTimes(1);
});

it("keeps an active connection and shows the provider reset when repository discovery is rate limited", async () => {
  const fetchMock = vi.fn((input: string | URL | Request) => {
    const url = String(input);
    if (url.endsWith("/repositories")) {
      return Promise.resolve(Response.json({
        error: { code: "RATE_LIMIT_EXCEEDED", message: "Retry after reset." },
        metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
      }, { status: 429, headers: { "X-RateLimit-Reset": "1786410000" } }));
    }
    return Promise.resolve(Response.json({
      data: { connections: [{
        connectionId: "e046a279-9c82-4bbf-9d8f-0737b222fa97",
        provider: "GITHUB",
        status: "ACTIVE",
        scopes: ["repo"],
        connectedAt: "2026-08-11T00:00:00Z",
        expiresAt: null
      }] },
      metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
    }));
  });
  vi.stubGlobal("fetch", fetchMock);

  renderWithProviders(<ConnectionPanel />);

  expect(await screen.findByText("연결됨")).toBeInTheDocument();
  expect(await screen.findByRole("alert")).toHaveTextContent("GitHub 요청 한도를 모두 사용했습니다");
  expect(screen.getByRole("button", { name: "GitHub 저장소 다시 확인" })).toBeInTheDocument();
  expect(fetchMock).toHaveBeenCalledTimes(2);
});

it("shows a separate error state when connection lookup fails", async () => {
  vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("network unavailable")));

  renderWithProviders(<ConnectionPanel />);

  expect(await screen.findByRole("alert")).toHaveTextContent("외부 서비스 연결 상태를 불러오지 못했습니다");
});
