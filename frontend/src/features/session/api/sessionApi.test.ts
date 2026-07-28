import { afterEach, describe, expect, it, vi } from "vitest";

import { getCurrentUser, logout } from "./sessionApi";

describe("session API", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("maps an unauthenticated response to an anonymous session", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));

    await expect(getCurrentUser()).resolves.toBeNull();
  });

  it("returns the authenticated current user", async () => {
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
            requestId: "request-1",
            apiVersion: "v1",
            timestamp: "2026-07-27T00:00:00Z"
          }
        })
      )
    );

    await expect(getCurrentUser()).resolves.toMatchObject({
      displayName: "DevPath User",
      authenticationProvider: "GITHUB"
    });
  });

  it("uses the server-issued CSRF token for logout", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(
        Response.json({
          data: {
            headerName: "X-XSRF-TOKEN",
            parameterName: "_csrf",
            token: "csrf-token"
          },
          metadata: {
            requestId: "request-2",
            apiVersion: "v1",
            timestamp: "2026-07-27T00:00:00Z"
          }
        })
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await logout();

    expect(fetchMock).toHaveBeenLastCalledWith(
      "http://localhost:8080/api/v1/session/logout",
      expect.objectContaining({
        method: "POST",
        credentials: "include",
        headers: { "X-XSRF-TOKEN": "csrf-token" }
      })
    );
  });
});
