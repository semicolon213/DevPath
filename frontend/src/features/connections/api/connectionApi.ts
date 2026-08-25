import { apiRequest } from "../../../shared/api/apiClient";

export type ConnectedAccount = {
  connectionId: string;
  provider: "GITHUB";
  status: "ACTIVE" | "EXPIRED" | "REVOKED";
  scopes: string[];
  connectedAt: string;
  expiresAt: string | null;
};

type ConnectedAccountList = {
  connections: ConnectedAccount[];
};

export type GitHubRepository = {
  providerRepositoryId: string;
  name: string;
  fullName: string;
  owner: string;
  privateRepository: boolean;
  archived: boolean;
  defaultBranch: string;
  htmlUrl: string;
};

type GitHubRepositoryList = { repositories: GitHubRepository[] };
type OAuthAuthorization = { authorizationUrl: string };
type CsrfToken = { headerName: string; token: string };

export async function getConnections() {
  return (await apiRequest<ConnectedAccountList>("/api/v1/users/me/connections")).data;
}

export async function authorizeGitHub() {
  const csrf = await apiRequest<CsrfToken>("/api/v1/csrf");
  return (await apiRequest<OAuthAuthorization>("/api/v1/integrations/github/authorize", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [csrf.data.headerName]: csrf.data.token
    },
    body: JSON.stringify({})
  })).data;
}

export async function getGitHubRepositories() {
  return (await apiRequest<GitHubRepositoryList>("/api/v1/integrations/github/repositories")).data;
}

export async function disconnectGitHub() {
  const csrf = await apiRequest<CsrfToken>("/api/v1/csrf");
  return (await apiRequest<ConnectedAccount>("/api/v1/integrations/github", {
    method: "DELETE",
    headers: { [csrf.data.headerName]: csrf.data.token }
  })).data;
}
