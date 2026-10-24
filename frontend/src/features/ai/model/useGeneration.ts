import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cancelGenerationJob, getGenerationJob, type GenerationJob } from "../api/generationApi";

const storagePrefix = "devpath:generation:";

export function useGeneration<T>(subjectKey: string, requestGeneration: () => Promise<GenerationJob>, getArtifact: (url: string) => Promise<T>) {
  const queryClient = useQueryClient();
  const subjectQueryKey = ["generation-subjects", subjectKey] as const;
  const [jobId, setJobId] = useState(() => queryClient.getQueryData<string>(subjectQueryKey) ?? readStoredJob(subjectKey));
  useEffect(() => {
    const trackedJobId = queryClient.getQueryData<string>(subjectQueryKey) ?? readStoredJob(subjectKey);
    setJobId(trackedJobId);
    if (trackedJobId) queryClient.setQueryData(subjectQueryKey, trackedJobId);
  }, [queryClient, subjectKey]);
  const request = useMutation({
    mutationFn: requestGeneration,
    onMutate: () => {
      setJobId(undefined);
      clearStoredJob(subjectKey, queryClient);
    },
    onSuccess: value => {
      setJobId(value.jobId);
      queryClient.setQueryData(subjectQueryKey, value.jobId);
      writeStoredJob(subjectKey, value.jobId);
      queryClient.setQueryData(["generation-jobs", value.jobId], value);
    }
  });
  const job = useQuery({
    queryKey: ["generation-jobs", jobId],
    queryFn: () => getGenerationJob(jobId!),
    enabled: Boolean(jobId),
    refetchInterval: query => ["QUEUED", "RUNNING"].includes(query.state.data?.status ?? "") ? 1000 : false,
    refetchIntervalInBackground: true
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
  useEffect(() => {
    if (job.data && (queryClient.getQueryData<string>(subjectQueryKey) === jobId || readStoredJob(subjectKey) === jobId)
      && (["FAILED", "CANCELED"].includes(job.data.status) || job.data.validationStatus === "REJECTED")) clearStoredJob(subjectKey, queryClient);
  }, [job.data, jobId, queryClient, subjectKey]);
  return { request, job, artifact, cancel };
}

function readStoredJob(subjectKey: string) {
  try {
    const value = JSON.parse(sessionStorage.getItem(`${storagePrefix}${subjectKey}`) ?? "null") as { subjectKey?: string; jobId?: string } | null;
    return value?.subjectKey === subjectKey ? value.jobId : undefined;
  } catch {
    return undefined;
  }
}

function writeStoredJob(subjectKey: string, jobId: string) {
  try { sessionStorage.setItem(`${storagePrefix}${subjectKey}`, JSON.stringify({ subjectKey, jobId })); } catch { /* storage is an optional resume aid */ }
}

function clearStoredJob(subjectKey: string, queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.removeQueries({ queryKey: ["generation-subjects", subjectKey], exact: true });
  try { sessionStorage.removeItem(`${storagePrefix}${subjectKey}`); } catch { /* storage is an optional resume aid */ }
}
