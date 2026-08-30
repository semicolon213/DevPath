import { ApiError, apiRequest, getApiBaseUrl, requestContextHeaders, withCsrf } from "../../../shared/api/apiClient";

export type CurrentUser = {
  userId: string;
  displayName: string;
  avatarUrl: string | null;
  status: "ACTIVE";
  authenticationProvider: "GITHUB";
  createdAt: string;
};

export async function getCurrentUser(): Promise<CurrentUser | null> {
  try {
    return await apiRequest<CurrentUser>("/api/v1/users/me");
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export async function logout(): Promise<void> {
  const response = await fetch(`${getApiBaseUrl()}/api/v1/session/logout`, await withCsrf({
    method: "POST",
    credentials: "include",
    headers: requestContextHeaders()
  }));

  if (!response.ok) {
    throw new ApiError(response.status, `DevPath logout failed with status ${response.status}`,
      response.headers.get("X-Request-Id"), response.headers.get("X-Correlation-Id"));
  }
}
