const DEFAULT_API_BASE_URL = "http://localhost:8080";
const browserCorrelationId = crypto.randomUUID();

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

type CsrfToken = { headerName: string; token: string };

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {}
): Promise<T> {
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

  return ((await response.json()) as { data: T }).data;
}

export async function withCsrf(init: RequestInit = {}): Promise<RequestInit> {
  const csrf = await apiRequest<CsrfToken>("/api/v1/csrf");
  return {
    ...init,
    headers: { ...init.headers, [csrf.headerName]: csrf.token }
  };
}

async function safeErrorBody(response: Response) {
  try {
    return await response.json() as { error?: { code?: string; message?: string } };
  } catch {
    return null;
  }
}

export function rateLimitMessage(error: unknown, provider = "GitHub") {
  if (!(error instanceof ApiError) || error.status !== 429) return null;
  const epochSeconds = error.rateLimitReset === null ? Number.NaN : Number(error.rateLimitReset);
  if (Number.isFinite(epochSeconds)) {
    const resetAt = new Date(epochSeconds * 1000);
    if (!Number.isNaN(resetAt.getTime())) {
      return `${provider} 요청 한도를 모두 사용했습니다. ${new Intl.DateTimeFormat("ko-KR", {
        dateStyle: "medium",
        timeStyle: "short"
      }).format(resetAt)} 이후 다시 시도해 주세요.`;
    }
  }
  if (error.retryAfter && /^\d+$/.test(error.retryAfter)) {
    return `${provider} 요청 한도를 모두 사용했습니다. 약 ${error.retryAfter}초 후 다시 시도해 주세요.`;
  }
  return `${provider} 요청 한도를 모두 사용했습니다. 잠시 후 다시 시도해 주세요.`;
}

export function requestContextHeaders() {
  return {
    "X-Request-Id": crypto.randomUUID(),
    "X-Correlation-Id": browserCorrelationId
  };
}
