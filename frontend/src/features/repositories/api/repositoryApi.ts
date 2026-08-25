import { apiRequest } from "../../../shared/api/apiClient";

export type ImportedRepository = {
  repositoryId: string;
  providerRepositoryId: string;
  name: string;
  fullName: string;
  owner: string;
  visibility: "PUBLIC" | "PRIVATE";
  defaultBranch: string;
  providerArchived: boolean;
  lifecycle: "DISCOVERED" | "ACTIVE" | "ARCHIVED" | "DELETED_EXTERNALLY";
  syncStatus: "NOT_SYNCED" | "SYNCHRONIZED" | "FAILED";
  htmlUrl: string;
  discoveredAt: string;
  lastSyncedAt: string | null;
  currentSnapshotId: string | null;
};

export type RepositoryPage = {
  repositories: ImportedRepository[];
  limit: number;
  nextCursor: string | null;
  totalCount: number;
};

export type RepositorySyncJob = {
  jobId: string;
  jobType: "REPOSITORY_SYNC";
  status: "queued" | "running" | "succeeded" | "failed" | "cancelled" | "expired";
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

export type RepositorySnapshot = {
  snapshotId: string;
  repositoryId: string;
  capturedAt: string;
  sourceRevision: string;
  status: "READY" | "FAILED" | "SUPERSEDED" | "DELETED_BY_POLICY";
  immutable: boolean;
  contentHash: string;
  branchCount: number;
  commitCount: number;
};

export type DetectedTechnology = {
  name: string;
  category: "LANGUAGE" | "FRAMEWORK" | "DATABASE";
  evidenceLabel: string;
  byteCount: number | null;
  percentage: number | null;
  taxonomyStatus: "SUPPORTED" | "UNSUPPORTED";
  evidenceType: "PROVIDER_LANGUAGE_STATISTICS" | "DEPENDENCY_DECLARATION";
  evidencePaths: string[];
};

export type TechnologySummary = {
  repositoryId: string;
  snapshotId: string;
  extractorVersion: string;
  taxonomyVersion: string;
  primaryLanguage: string | null;
  technologies: DetectedTechnology[];
};

export type EvidenceSignal = {
  signalKey: string;
  label: string;
  present: boolean;
  count: number;
  observedValue: string | null;
  evidencePaths: string[];
};

export type EvidenceCategory = {
  category: "ARCHITECTURE" | "DATABASE" | "TESTING" | "DEVOPS" | "DOCUMENTATION" | "ACTIVITY";
  label: string;
  signals: EvidenceSignal[];
};

export type RepositoryEvidenceSummary = {
  repositoryId: string;
  snapshotId: string;
  extractorVersion: string;
  categories: EvidenceCategory[];
};

type CsrfToken = { headerName: string; token: string };

export async function getRepositories(cursor: string | null = null, includeArchived = false) {
  const query = new URLSearchParams({ limit: "20", includeArchived: String(includeArchived) });
  if (cursor) query.set("cursor", cursor);
  return (await apiRequest<RepositoryPage>(`/api/v1/repositories?${query}`)).data;
}

export async function getRepository(repositoryId: string) {
  return (await apiRequest<ImportedRepository>(`/api/v1/repositories/${repositoryId}`)).data;
}

export async function importRepository(providerRepositoryId: string) {
  const csrf = await apiRequest<CsrfToken>("/api/v1/csrf");
  return (await apiRequest<ImportedRepository>("/api/v1/repositories/imports", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [csrf.data.headerName]: csrf.data.token
    },
    body: JSON.stringify({ providerRepositoryId })
  })).data;
}

export async function archiveRepository(repositoryId: string) {
  return changeRepositoryLifecycle(repositoryId, "archive");
}

export async function restoreRepository(repositoryId: string) {
  return changeRepositoryLifecycle(repositoryId, "restore");
}

export async function synchronizeRepository(repositoryId: string) {
  const csrf = await apiRequest<CsrfToken>("/api/v1/csrf");
  return (await apiRequest<RepositorySyncJob>(`/api/v1/repositories/${repositoryId}/sync`, {
    method: "POST",
    headers: {
      [csrf.data.headerName]: csrf.data.token,
      "Idempotency-Key": createIdempotencyKey()
    }
  })).data;
}

function createIdempotencyKey() {
  const randomUUID = globalThis.crypto?.randomUUID;
  if (typeof randomUUID === "function") {
    return randomUUID.call(globalThis.crypto);
  }
  return `sync-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export async function getRepositorySyncJob(jobId: string) {
  return (await apiRequest<RepositorySyncJob>(`/api/v1/repository-sync-jobs/${jobId}`)).data;
}

export async function getRepositorySnapshots(repositoryId: string) {
  return (await apiRequest<{ snapshots: RepositorySnapshot[] }>(
    `/api/v1/repositories/${repositoryId}/snapshots`
  )).data.snapshots;
}

export async function getRepositoryTechnologies(repositoryId: string) {
  return (await apiRequest<TechnologySummary>(
    `/api/v1/repositories/${repositoryId}/technologies`
  )).data;
}

export async function getRepositoryEvidence(repositoryId: string) {
  return (await apiRequest<RepositoryEvidenceSummary>(
    `/api/v1/repositories/${repositoryId}/evidence`
  )).data;
}

async function changeRepositoryLifecycle(repositoryId: string, action: "archive" | "restore") {
  const csrf = await apiRequest<CsrfToken>("/api/v1/csrf");
  return (await apiRequest<ImportedRepository>(`/api/v1/repositories/${repositoryId}/${action}`, {
    method: "POST",
    headers: { [csrf.data.headerName]: csrf.data.token }
  })).data;
}
