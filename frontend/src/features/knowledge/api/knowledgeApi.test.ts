import { afterEach, expect, it, vi } from "vitest";
import { importNotionKnowledge, searchKnowledge } from "./knowledgeApi";

afterEach(() => vi.unstubAllGlobals());

it("imports Notion knowledge through the credentialed CSRF boundary", async () => {
  vi.stubGlobal("crypto", { randomUUID: () => "9edac08e-95ba-49d7-a70a-e3adce31fdb1" });
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf" }, metadata: metadata() }))
    .mockResolvedValueOnce(Response.json({ data: job(), metadata: metadata() }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(importNotionKnowledge("connection-1", "page-1")).resolves.toMatchObject({ jobType: "KNOWLEDGE_INGESTION" });
  expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/knowledge-documents/imports/notion"),
    expect.objectContaining({ method: "POST", credentials: "include", headers: expect.objectContaining({
      "X-CSRF-TOKEN": "csrf", "Idempotency-Key": "9edac08e-95ba-49d7-a70a-e3adce31fdb1"
    }), body: JSON.stringify({ connectionId: "connection-1", providerPageId: "page-1" }) }));
});

it("searches only through the backend CSRF boundary with explicit filters", async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf" }, metadata: metadata() }))
    .mockResolvedValueOnce(Response.json({ data: { retrievalResultId: "result-1", retrievalType: "SEMANTIC",
      policyVersion: "knowledge-semantic-v1", contextPurpose: "USER_SEARCH",
      appliedFilters: { sourceTypes: ["NOTION"], documentIds: ["document-1"] }, results: [],
      resultCount: 0, durationMs: 3, generatedAt: "2026-08-30T00:00:00Z" }, metadata: metadata() }));
  vi.stubGlobal("fetch", fetchMock);

  await searchKnowledge("testing strategy", ["document-1"]);

  expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/knowledge-search"),
    expect.objectContaining({ method: "POST", credentials: "include", headers: expect.objectContaining({
      "X-CSRF-TOKEN": "csrf", "Content-Type": "application/json"
    }), body: JSON.stringify({ query: "testing strategy", filters: { sourceTypes: ["NOTION"],
      documentIds: ["document-1"] }, limit: 5, contextPurpose: "USER_SEARCH" }) }));
});

function metadata() { return { requestId: "r", apiVersion: "v1", timestamp: "2026-08-30T00:00:00Z" }; }
function job() { return { jobId: "job-1", jobType: "KNOWLEDGE_INGESTION", status: "queued", phase: "QUEUED",
  progressPercent: 0, attemptCount: 0, maxAttempts: 3, submittedAt: "2026-08-30T00:00:00Z", startedAt: null,
  completedAt: null, pollingUrl: "/api/v1/knowledge-ingestion-jobs/job-1", resultResourceUrl: null,
  errorCode: null, errorMessage: null, retryable: true }; }
