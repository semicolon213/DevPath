import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { authorizeGitHub, authorizeNotion, disconnectGitHub, disconnectNotion, getConnections, getGitHubRepositories, getNotionWorkspaces } from "../api/connectionApi";
import { ApiError } from "../../../shared/api/apiClient";

export const connectionsKey = ["identity", "connections"] as const;

export function useConnections() {
  return useQuery({ queryKey: connectionsKey, queryFn: getConnections });
}

export function useAuthorizeGitHub() {
  return useMutation({ mutationFn: authorizeGitHub });
}

export function useGitHubRepositories(enabled: boolean) {
  return useQuery({
    queryKey: ["integration", "github", "repositories"],
    queryFn: getGitHubRepositories,
    enabled,
    retry: (failureCount, error) => !(error instanceof ApiError && error.status === 429) && failureCount < 2
  });
}

export function useDisconnectGitHub() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: disconnectGitHub,
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: ["integration", "github", "repositories"] });
      await queryClient.invalidateQueries({ queryKey: connectionsKey });
    }
  });
}

export function useAuthorizeNotion() {
  return useMutation({ mutationFn: authorizeNotion });
}

export function useNotionWorkspaces(enabled: boolean) {
  return useQuery({
    queryKey: ["integration", "notion", "workspaces"],
    queryFn: getNotionWorkspaces,
    enabled,
    retry: (failureCount, error) => !(error instanceof ApiError && error.status === 429) && failureCount < 2
  });
}

export function useDisconnectNotion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: disconnectNotion,
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: ["integration", "notion", "workspaces"] });
      await queryClient.invalidateQueries({ queryKey: connectionsKey });
    }
  });
}
