import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getGenerationJob } from "../api/generationApi";
import { useGeneration } from "./useGeneration";

vi.mock("../api/generationApi", () => ({ getGenerationJob: vi.fn(), cancelGenerationJob: vi.fn() }));

const subjectKey = "REPOSITORY_REVIEW:analysis-id";
const storageKey = `devpath:generation:${subjectKey}`;

describe("useGeneration", () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  it("keeps active job polling enabled in the background", async () => {
    sessionStorage.setItem(storageKey, JSON.stringify({ subjectKey, jobId: "job-id" }));
    vi.mocked(getGenerationJob).mockResolvedValue(job("RUNNING"));
    const queryClient = client();
    renderHook(() => useGeneration(subjectKey, vi.fn(), vi.fn()), { wrapper: wrapper(queryClient) });
    await waitFor(() => expect(getGenerationJob).toHaveBeenCalledWith("job-id"));
    expect((queryClient.getQueryCache().find({ queryKey: ["generation-jobs", "job-id"] })?.options as { refetchIntervalInBackground?: boolean }).refetchIntervalInBackground).toBe(true);
  });

  it("resumes a stored job after remount and fetches its artifact", async () => {
    sessionStorage.setItem(storageKey, JSON.stringify({ subjectKey, jobId: "job-id" }));
    vi.mocked(getGenerationJob).mockResolvedValue(job("SUCCEEDED"));
    const getArtifact = vi.fn().mockResolvedValue({ artifactId: "artifact-id" });
    const first = renderHook(() => useGeneration(subjectKey, vi.fn(), getArtifact), { wrapper: wrapper(client()) });
    await waitFor(() => expect(first.result.current.artifact.data).toEqual({ artifactId: "artifact-id" }));
    first.unmount();
    const second = renderHook(() => useGeneration(subjectKey, vi.fn(), getArtifact), { wrapper: wrapper(client()) });
    await waitFor(() => expect(second.result.current.artifact.data).toEqual({ artifactId: "artifact-id" }));
    expect(getArtifact).toHaveBeenCalledWith("/artifacts/artifact-id");
  });

  it("switches tracking when the subject changes without a remount", async () => {
    const nextSubjectKey = "REPOSITORY_REVIEW:next-analysis";
    sessionStorage.setItem(storageKey, JSON.stringify({ subjectKey, jobId: "first-job" }));
    sessionStorage.setItem(`devpath:generation:${nextSubjectKey}`, JSON.stringify({ subjectKey: nextSubjectKey, jobId: "next-job" }));
    vi.mocked(getGenerationJob).mockImplementation(id => Promise.resolve({ ...job(id === "first-job" ? "FAILED" : "RUNNING"), jobId: id }));
    const view = renderHook(({ key }) => useGeneration(key, vi.fn(), vi.fn()), {
      initialProps: { key: subjectKey }, wrapper: wrapper(client())
    });
    await waitFor(() => expect(getGenerationJob).toHaveBeenCalledWith("first-job"));
    view.rerender({ key: nextSubjectKey });
    await waitFor(() => expect(getGenerationJob).toHaveBeenCalledWith("next-job"));
    expect(sessionStorage.getItem(`devpath:generation:${nextSubjectKey}`)).not.toBeNull();
  });

  it("falls back without a stored job when session storage is unavailable", () => {
    const getItem = vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => { throw new Error("blocked"); });
    expect(() => renderHook(() => useGeneration(subjectKey, vi.fn(), vi.fn()), { wrapper: wrapper(client()) })).not.toThrow();
    getItem.mockRestore();
  });

  it.each([
    ["FAILED", "PENDING"],
    ["CANCELED", "PENDING"],
    ["FAILED", "REJECTED"]
  ] as const)("clears a stored %s/%s job", async (status, validationStatus) => {
    sessionStorage.setItem(storageKey, JSON.stringify({ subjectKey, jobId: "job-id" }));
    vi.mocked(getGenerationJob).mockResolvedValue(job(status, validationStatus));
    const queryClient = client();
    renderHook(() => useGeneration(subjectKey, vi.fn(), vi.fn()), { wrapper: wrapper(queryClient) });

    await waitFor(() => expect(sessionStorage.getItem(storageKey)).toBeNull());
    expect(queryClient.getQueryData(["generation-subjects", subjectKey])).toBeUndefined();
  });

  it("clears a terminal job after the subject cache has been garbage-collected", async () => {
    sessionStorage.setItem(storageKey, JSON.stringify({ subjectKey, jobId: "job-id" }));
    let finish!: (value: ReturnType<typeof job>) => void;
    vi.mocked(getGenerationJob).mockReturnValue(new Promise(resolve => { finish = resolve; }));
    const queryClient = client();
    renderHook(() => useGeneration(subjectKey, vi.fn(), vi.fn()), { wrapper: wrapper(queryClient) });
    await waitFor(() => expect(getGenerationJob).toHaveBeenCalledWith("job-id"));

    act(() => queryClient.removeQueries({ queryKey: ["generation-subjects", subjectKey], exact: true }));
    expect(queryClient.getQueryData(["generation-subjects", subjectKey])).toBeUndefined();
    act(() => finish(job("FAILED")));

    await waitFor(() => expect(sessionStorage.getItem(storageKey)).toBeNull());
  });

  it("replaces the stored job when generation is requested again", async () => {
    sessionStorage.setItem(storageKey, JSON.stringify({ subjectKey, jobId: "old-job" }));
    vi.mocked(getGenerationJob).mockImplementation(id => Promise.resolve({ ...job("RUNNING"), jobId: id }));
    const requestGeneration = vi.fn().mockResolvedValue({ ...job("RUNNING"), jobId: "new-job" });
    const view = renderHook(() => useGeneration(subjectKey, requestGeneration, vi.fn()), { wrapper: wrapper(client()) });
    await waitFor(() => expect(getGenerationJob).toHaveBeenCalledWith("old-job"));

    act(() => view.result.current.request.mutate());

    await waitFor(() => expect(JSON.parse(sessionStorage.getItem(storageKey)!).jobId).toBe("new-job"));
  });
});

function job(status: "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELED", validationStatus: "PENDING" | "PASSED" | "REJECTED" = status === "SUCCEEDED" ? "PASSED" : "PENDING") {
  return { jobId: "job-id", status, validationStatus,
    artifactUrl: status === "SUCCEEDED" ? "/artifacts/artifact-id" : null, failureCode: null,
    createdAt: new Date().toISOString(), completedAt: status === "SUCCEEDED" ? new Date().toISOString() : null };
}

function client() { return new QueryClient({ defaultOptions: { queries: { retry: false } } }); }
function wrapper(queryClient: QueryClient) { return ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>; }
