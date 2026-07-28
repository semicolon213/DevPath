const DEFAULT_API_BASE_URL = "http://localhost:8080";

export function getApiBaseUrl() {
  return import.meta.env.VITE_DEVPATH_API_BASE_URL ?? DEFAULT_API_BASE_URL;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string
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
      ...init.headers
    }
  });

  if (!response.ok) {
    throw new ApiError(response.status, `DevPath API request failed with status ${response.status}`);
  }

  return response.json() as Promise<ApiEnvelope<T>>;
}
