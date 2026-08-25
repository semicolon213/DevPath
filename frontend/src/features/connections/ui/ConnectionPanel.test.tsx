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

  expect(await screen.findByText("미연결")).toBeInTheDocument();
  expect(screen.getByText(/GitHub 로그인은 완료되었지만/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "GitHub 저장소 연결" })).toBeInTheDocument();
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

it("shows a separate error state when connection lookup fails", async () => {
  vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("network unavailable")));

  renderWithProviders(<ConnectionPanel />);

  expect(await screen.findByRole("alert")).toHaveTextContent("외부 서비스 연결 상태를 불러오지 못했습니다");
});
