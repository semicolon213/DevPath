import { useInfiniteQuery, useMutation, useQuery } from "@tanstack/react-query";
import { getAnalysisDetail, getAnalysisHistory, getAnalysisJob, getRepositoryAnalysisHistory, requestAnalysis } from "../api/analysisApi";

export const analysisHistoryKey = ["analyses", "history"] as const;

export function useRequestAnalysis() {
  return useMutation({ mutationFn: requestAnalysis });
}

export function useAnalysisJob(jobId: string | null) {
  return useQuery({
    queryKey: ["analysis-job", jobId],
    queryFn: () => getAnalysisJob(jobId!),
    enabled: Boolean(jobId),
    refetchInterval: query => {
      const status = query.state.data?.status;
      return status === "queued" || status === "running" ? 1500 : false;
    }
  });
}

export function useAnalysisHistory() {
  return useInfiniteQuery({
    queryKey: analysisHistoryKey,
    queryFn: ({ pageParam }) => getAnalysisHistory(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: page => page.nextCursor ?? undefined
  });
}

export function useRepositoryAnalysisHistory(repositoryId: string | undefined) {
  return useInfiniteQuery({
    queryKey: [...analysisHistoryKey, { repositoryId }],
    queryFn: ({ pageParam }) => getRepositoryAnalysisHistory(repositoryId!, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: page => page.nextCursor ?? undefined,
    enabled: Boolean(repositoryId)
  });
}

export function useAnalysisDetail(analysisId: string | undefined) {
  return useQuery({
    queryKey: ["analyses", analysisId],
    queryFn: () => getAnalysisDetail(analysisId!),
    enabled: Boolean(analysisId)
  });
}
