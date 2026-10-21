import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, apiRequest, mutationErrorMessage } from "./apiClient";

describe("apiRequest correlation", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("adds safe request context headers to every credentialed API request", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({
      data: { ok: true },
      metadata: { requestId: "server-request", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" }
    }));
    vi.stubGlobal("fetch", fetchMock);

    await apiRequest<{ ok: boolean }>("/api/v1/test");

    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/test", expect.objectContaining({
      credentials: "include",
      headers: expect.objectContaining({
        "X-Request-Id": expect.stringMatching(/^[A-Za-z0-9._-]+$/),
        "X-Correlation-Id": expect.stringMatching(/^[A-Za-z0-9._-]+$/)
      })
    }));
  });

  it("retains server support references on API failures", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, {
      status: 503,
      headers: { "X-Request-Id": "server-request", "X-Correlation-Id": "server-journey" }
    })));

    await expect(apiRequest("/api/v1/test")).rejects.toMatchObject({
      status: 503,
      requestId: "server-request",
      correlationId: "server-journey"
    });
  });

  it("retains safe error codes and provider retry headers", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({
      error: { code: "RATE_LIMIT_EXCEEDED", message: "Retry after reset." },
      metadata: { requestId: "server-request", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" }
    }, {
      status: 429,
      headers: { "Retry-After": "120", "X-RateLimit-Reset": "1786406400" }
    })));

    await expect(apiRequest("/api/v1/test")).rejects.toMatchObject({
      status: 429,
      code: "RATE_LIMIT_EXCEEDED",
      message: "Retry after reset.",
      retryAfter: "120",
      rateLimitReset: "1786406400"
    });
  });
});

it("maps duplicate mutation requests to safe Korean guidance", () => {
  expect(mutationErrorMessage(new ApiError(409, "internal", null, null, "DUPLICATE_REQUEST"), "fallback"))
    .toBe("같은 요청 키가 다른 작업에 이미 사용되었습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.");
});
