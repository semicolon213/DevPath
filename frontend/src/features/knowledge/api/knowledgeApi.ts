import { apiRequest, withCsrf } from "../../../shared/api/apiClient";

export type KnowledgeIngestionJob = {
  jobId: string;
  jobType: "KNOWLEDGE_INGESTION";
  status: "queued" | "running" | "succeeded" | "failed";
  phase: "QUEUED" | "COLLECTING" | "RETRY_WAIT" | "COMPLETED" | "FAILED";
  progressPercent: number;
  attemptCount: number;
  maxAttempts: number;
  submittedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  pollingUrl: string;
  resultResourceUrl: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  retryable: boolean;
};

export type KnowledgeDocument = {
  documentId: string;
  sourceType: "NOTION";
  sourceObjectId: string;
  title: string;
  status: "ACTIVE" | "ARCHIVED";
  currentVersionId: string | null;
  chunkCount: number;
  createdAt: string;
  updatedAt: string;
};

export type KnowledgeChunkSummary = {
  chunkId: string;
  position: number;
  heading: string | null;
  contentHash: string;
  tokenEstimate: number;
  status: "INDEXED" | "STALE" | "DELETED";
};

export type KnowledgeSearchFilters = { sourceTypes: ["NOTION"]; documentIds: string[] };
export type KnowledgeSearchResult = {
  chunkId: string; documentId: string; documentTitle: string; sourceType: "NOTION";
  sourceObjectId: string; sourceUrl: string | null; heading: string | null; excerpt: string;
  relevance: number; tokenEstimate: number; freshness: "FRESH";
};
export type KnowledgeSearchResponse = {
  retrievalResultId: string; retrievalType: "SEMANTIC"; policyVersion: "knowledge-semantic-v1";
  contextPurpose: "USER_SEARCH"; appliedFilters: KnowledgeSearchFilters;
  results: KnowledgeSearchResult[]; resultCount: number; durationMs: number; generatedAt: string;
};

export async function importNotionKnowledge(connectionId: string, providerPageId: string) {
  return apiRequest<KnowledgeIngestionJob>("/api/v1/knowledge-documents/imports/notion", await withCsrf({
    method: "POST",
    headers: { "Content-Type": "application/json", "Idempotency-Key": crypto.randomUUID() },
    body: JSON.stringify({ connectionId, providerPageId })
  }));
}

export async function getKnowledgeIngestionJob(jobId: string) {
  return apiRequest<KnowledgeIngestionJob>(`/api/v1/knowledge-ingestion-jobs/${jobId}`);
}

export async function getKnowledgeDocuments() {
  return apiRequest<{ documents: KnowledgeDocument[] }>("/api/v1/knowledge-documents");
}

export async function getKnowledgeDocument(documentId: string) {
  return apiRequest<KnowledgeDocument>(`/api/v1/knowledge-documents/${documentId}`);
}

export async function getKnowledgeChunks(documentId: string) {
  return apiRequest<{ chunks: KnowledgeChunkSummary[] }>(
    `/api/v1/knowledge-documents/${documentId}/chunks`
  );
}

export async function archiveKnowledgeDocument(documentId: string) {
  return apiRequest<KnowledgeDocument>(`/api/v1/knowledge-documents/${documentId}/archive`,
    await withCsrf({ method: "POST" }));
}

export async function reindexKnowledgeDocument(documentId: string) {
  return apiRequest<KnowledgeIngestionJob>(`/api/v1/knowledge-documents/${documentId}/reindex`,
    await withCsrf({ method: "POST", headers: { "Idempotency-Key": crypto.randomUUID() } }));
}

export async function searchKnowledge(query: string, documentIds: string[] = []) {
  return apiRequest<KnowledgeSearchResponse>("/api/v1/knowledge-search", await withCsrf({
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, filters: { sourceTypes: ["NOTION"], documentIds }, limit: 5, contextPurpose: "USER_SEARCH" })
  }));
}
