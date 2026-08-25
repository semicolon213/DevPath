import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  archiveRepository,
  getRepositories,
  getRepository,
  getRepositorySnapshots,
  getRepositoryTechnologies,
  getRepositoryEvidence,
  getRepositorySyncJob,
  importRepository,
  restoreRepository,
  synchronizeRepository
} from "../api/repositoryApi";

export const repositoriesKey = ["repositories"] as const;

export function useRepositories(includeArchived = false) {
  return useInfiniteQuery({
    queryKey: [...repositoriesKey, { includeArchived }],
    queryFn: ({ pageParam }) => getRepositories(pageParam, includeArchived),
    initialPageParam: null as string | null,
    getNextPageParam: lastPage => lastPage.nextCursor ?? undefined
  });
}

export function useArchiveRepository() {
  return useRepositoryLifecycleMutation(archiveRepository);
}

export function useRestoreRepository() {
  return useRepositoryLifecycleMutation(restoreRepository);
}

export function useSynchronizeRepository() {
  return useMutation({ mutationFn: synchronizeRepository });
}

export function useRepositorySyncJob(jobId: string | null) {
  return useQuery({
    queryKey: ["repository-sync-job", jobId],
    queryFn: () => getRepositorySyncJob(jobId!),
    enabled: Boolean(jobId),
    refetchInterval: query => {
      const status = query.state.data?.status;
      return status === "queued" || status === "running" ? 1500 : false;
    }
  });
}

export function useRepositorySnapshots(repositoryId: string | undefined) {
  return useQuery({
    queryKey: [...repositoriesKey, repositoryId, "snapshots"],
    queryFn: () => getRepositorySnapshots(repositoryId!),
    enabled: Boolean(repositoryId)
  });
}

export function useRepositoryTechnologies(repositoryId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: [...repositoriesKey, repositoryId, "technologies"],
    queryFn: () => getRepositoryTechnologies(repositoryId!),
    enabled: Boolean(repositoryId) && enabled
  });
}

export function useRepositoryEvidence(repositoryId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: [...repositoriesKey, repositoryId, "evidence"],
    queryFn: () => getRepositoryEvidence(repositoryId!),
    enabled: Boolean(repositoryId) && enabled
  });
}

function useRepositoryLifecycleMutation(mutationFn: (repositoryId: string) => Promise<import("../api/repositoryApi").ImportedRepository>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: repository => {
      queryClient.setQueryData([...repositoriesKey, repository.repositoryId], repository);
      return queryClient.invalidateQueries({ queryKey: repositoriesKey });
    }
  });
}

export function useRepository(repositoryId: string | undefined) {
  return useQuery({
    queryKey: [...repositoriesKey, repositoryId],
    queryFn: () => getRepository(repositoryId!),
    enabled: Boolean(repositoryId)
  });
}

export function useImportRepository() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: importRepository,
    onSuccess: repository => {
      queryClient.setQueryData([...repositoriesKey, repository.repositoryId], repository);
      return queryClient.invalidateQueries({ queryKey: repositoriesKey });
    }
  });
}
