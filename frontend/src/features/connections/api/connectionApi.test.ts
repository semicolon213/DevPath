import { afterEach, expect, it, vi } from "vitest";
import { authorizeGitHub, disconnectGitHub, getConnections, getGitHubRepositories } from "./connectionApi";

afterEach(() => vi.unstubAllGlobals());

it("retrieves only server-reported provider connection summaries", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({
    data: {
      connections: [{
        connectionId: "e046a279-9c82-4bbf-9d8f-0737b222fa97",
        provider: "GITHUB",
        status: "ACTIVE",
        scopes: ["repo"],
        connectedAt: "2026-08-11T00:00:00Z",
        expiresAt: null
      }]
    },
    metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
  })));

  await expect(getConnections()).resolves.toMatchObject({
    connections: [{ provider: "GITHUB", status: "ACTIVE" }]
  });
});

it("uses the server CSRF token to start GitHub authorization", async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(Response.json({
      data: { headerName: "X-CSRF-TOKEN", token: "csrf-token" },
      metadata: { requestId: "r1", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
    }))
    .mockResolvedValueOnce(Response.json({
      data: { authorizationUrl: "https://github.com/login/oauth/authorize?client_id=test" },
      metadata: { requestId: "r2", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
    }));
  vi.stubGlobal("fetch", fetchMock);

  await authorizeGitHub();

  expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/integrations/github/authorize"),
    expect.objectContaining({
      method: "POST",
      credentials: "include",
      headers: expect.objectContaining({
        "Content-Type": "application/json",
        "X-CSRF-TOKEN": "csrf-token"
      }),
      body: "{}"
    }));
});

it("retrieves normalized GitHub repositories through the backend", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({
    data: { repositories: [] },
    metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
  })));

  await expect(getGitHubRepositories()).resolves.toEqual({ repositories: [] });
});

it("uses CSRF protection when disconnecting GitHub", async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(Response.json({
      data: { headerName: "X-CSRF-TOKEN", token: "csrf-token" },
      metadata: { requestId: "r1", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
    }))
    .mockResolvedValueOnce(Response.json({
      data: {
        connectionId: "e046a279-9c82-4bbf-9d8f-0737b222fa97",
        provider: "GITHUB",
        status: "REVOKED",
        scopes: [],
        connectedAt: "2026-08-11T00:00:00Z",
        expiresAt: "2026-08-11T01:00:00Z"
      },
      metadata: { requestId: "r2", apiVersion: "v1", timestamp: "2026-08-11T01:00:00Z" }
    }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(disconnectGitHub()).resolves.toMatchObject({ status: "REVOKED" });
  expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/integrations/github"),
    expect.objectContaining({
      method: "DELETE",
      headers: expect.objectContaining({ "X-CSRF-TOKEN": "csrf-token" })
    }));
});
