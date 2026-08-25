import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { authorizeGitHub, disconnectGitHub, getConnections, getGitHubRepositories } from "../api/connectionApi";

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
    enabled
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
