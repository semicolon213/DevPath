import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cancelGenerationJob, getGenerationJob, type GenerationJob } from "../api/generationApi";

export function useGeneration<T>(requestGeneration: () => Promise<GenerationJob>, getArtifact: (url: string) => Promise<T>) {
  const queryClient = useQueryClient();
  const request = useMutation({ mutationFn: requestGeneration });
  const jobId = request.data?.jobId;
  const job = useQuery({
    queryKey: ["generation-jobs", jobId],
    queryFn: () => getGenerationJob(jobId!),
    enabled: Boolean(jobId),
    refetchInterval: query => ["QUEUED", "RUNNING"].includes(query.state.data?.status ?? "") ? 1000 : false
  });
  const artifactUrl = job.data?.status === "SUCCEEDED" ? job.data.artifactUrl : null;
  const artifact = useQuery({
    queryKey: ["generated-artifacts", artifactUrl],
    queryFn: () => getArtifact(artifactUrl!),
    enabled: Boolean(artifactUrl)
  });
  const cancel = useMutation({
    mutationFn: () => cancelGenerationJob(jobId!),
    onSuccess: value => queryClient.setQueryData(["generation-jobs", jobId], value)
  });
  return { request, job, artifact, cancel };
}
