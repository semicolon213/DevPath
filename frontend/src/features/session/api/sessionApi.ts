import { ApiError, apiRequest, getApiBaseUrl, requestContextHeaders } from "../../../shared/api/apiClient";

export type CurrentUser = {
  userId: string;
  displayName: string;
  avatarUrl: string | null;
  status: "ACTIVE";
  authenticationProvider: "GITHUB";
  createdAt: string;
};

type CsrfToken = {
  headerName: string;
  parameterName: string;
  token: string;
};

export async function getCurrentUser(): Promise<CurrentUser | null> {
  try {
    const response = await apiRequest<CurrentUser>("/api/v1/users/me");
    return response.data;
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export async function logout(): Promise<void> {
  const csrf = await apiRequest<CsrfToken>("/api/v1/csrf");
  const response = await fetch(`${getApiBaseUrl()}/api/v1/session/logout`, {
    method: "POST",
    credentials: "include",
    headers: {
      ...requestContextHeaders(),
      [csrf.data.headerName]: csrf.data.token
    }
  });

  if (!response.ok) {
    throw new ApiError(response.status, `DevPath logout failed with status ${response.status}`,
      response.headers.get("X-Request-Id"), response.headers.get("X-Correlation-Id"));
  }
}
