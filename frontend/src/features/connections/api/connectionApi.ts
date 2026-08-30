import { apiRequest, withCsrf } from "../../../shared/api/apiClient";

export type ConnectedAccount = {
  connectionId: string;
  provider: "GITHUB" | "NOTION";
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
export type NotionWorkspacePage = {
  providerPageId: string;
  title: string;
  objectType: "PAGE" | "DATA_SOURCE";
  url: string | null;
  lastEditedAt: string;
  inTrash: boolean;
};
export type NotionWorkspace = {
  connectionId: string;
  workspaceId: string;
  workspaceName: string;
  workspaceIconUrl: string | null;
  status: "ACTIVE";
  connectedAt: string;
  discoveredAt: string;
  pages: NotionWorkspacePage[];
};
type NotionWorkspaceList = { workspaces: NotionWorkspace[] };
type OAuthAuthorization = { authorizationUrl: string };
export async function getConnections() {
  return apiRequest<ConnectedAccountList>("/api/v1/users/me/connections");
}

export async function authorizeGitHub() {
  return apiRequest<OAuthAuthorization>("/api/v1/integrations/github/authorize",
    await withCsrf({ method: "POST" }));
}

export async function getGitHubRepositories() {
  return apiRequest<GitHubRepositoryList>("/api/v1/integrations/github/repositories");
}

export async function disconnectGitHub() {
  return apiRequest<ConnectedAccount>("/api/v1/integrations/github",
    await withCsrf({ method: "DELETE" }));
}

export async function authorizeNotion() {
  return apiRequest<OAuthAuthorization>("/api/v1/integrations/notion/authorize",
    await withCsrf({ method: "POST" }));
}

export async function getNotionWorkspaces() {
  return apiRequest<NotionWorkspaceList>("/api/v1/integrations/notion/workspaces");
}

export async function disconnectNotion() {
  return apiRequest<ConnectedAccount>("/api/v1/integrations/notion",
    await withCsrf({ method: "DELETE" }));
}
