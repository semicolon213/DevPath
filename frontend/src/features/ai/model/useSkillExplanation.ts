import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cancelGenerationJob, getGeneratedSkillExplanation, getGenerationJob, requestSkillExplanation } from "../api/generationApi";

export function useSkillExplanation(skillMatrixId: string) {
  const queryClient = useQueryClient();
  const request = useMutation({ mutationFn: () => requestSkillExplanation(skillMatrixId) });
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
    queryFn: () => getGeneratedSkillExplanation(artifactUrl!),
    enabled: Boolean(artifactUrl)
  });
  const cancel = useMutation({
    mutationFn: () => cancelGenerationJob(jobId!),
    onSuccess: value => queryClient.setQueryData(["generation-jobs", jobId], value)
  });
  return { request, job, artifact, cancel };
}
