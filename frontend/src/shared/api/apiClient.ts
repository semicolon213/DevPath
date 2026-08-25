const DEFAULT_API_BASE_URL = "http://localhost:8080";
const browserCorrelationId = createOpaqueId("journey");

export function getApiBaseUrl() {
  return import.meta.env.VITE_DEVPATH_API_BASE_URL ?? DEFAULT_API_BASE_URL;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly requestId: string | null = null,
    public readonly correlationId: string | null = null,
    public readonly code: string | null = null,
    public readonly retryAfter: string | null = null,
    public readonly rateLimitReset: string | null = null
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type ApiEnvelope<T> = {
  data: T;
  metadata: {
    requestId: string;
    apiVersion: string;
    timestamp: string;
  };
};

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {}
): Promise<ApiEnvelope<T>> {
  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      Accept: "application/json",
      ...requestContextHeaders(),
      ...init.headers
    }
  });

  if (!response.ok) {
    const body = await safeErrorBody(response);
    throw new ApiError(
      response.status,
      body?.error?.message ?? `DevPath API request failed with status ${response.status}`,
      response.headers.get("X-Request-Id"),
      response.headers.get("X-Correlation-Id"),
      body?.error?.code ?? null,
      response.headers.get("Retry-After"),
      response.headers.get("X-RateLimit-Reset")
    );
  }

  return response.json() as Promise<ApiEnvelope<T>>;
}

async function safeErrorBody(response: Response) {
  try {
    return await response.json() as { error?: { code?: string; message?: string } };
  } catch {
    return null;
  }
}

export function rateLimitMessage(error: unknown) {
  if (!(error instanceof ApiError) || error.status !== 429) return null;
  const epochSeconds = error.rateLimitReset === null ? Number.NaN : Number(error.rateLimitReset);
  if (Number.isFinite(epochSeconds)) {
    const resetAt = new Date(epochSeconds * 1000);
    if (!Number.isNaN(resetAt.getTime())) {
      return `GitHub 요청 한도를 모두 사용했습니다. ${new Intl.DateTimeFormat("ko-KR", {
        dateStyle: "medium",
        timeStyle: "short"
      }).format(resetAt)} 이후 다시 시도해 주세요.`;
    }
  }
  if (error.retryAfter && /^\d+$/.test(error.retryAfter)) {
    return `GitHub 요청 한도를 모두 사용했습니다. 약 ${error.retryAfter}초 후 다시 시도해 주세요.`;
  }
  return "GitHub 요청 한도를 모두 사용했습니다. 잠시 후 다시 시도해 주세요.";
}

export function requestContextHeaders() {
  return {
    "X-Request-Id": createOpaqueId("request"),
    "X-Correlation-Id": browserCorrelationId
  };
}

function createOpaqueId(prefix: string) {
  const randomUUID = globalThis.crypto?.randomUUID;
  return typeof randomUUID === "function"
    ? randomUUID.call(globalThis.crypto)
    : `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
