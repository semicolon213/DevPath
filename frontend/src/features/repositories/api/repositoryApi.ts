import { apiRequest, withCsrf } from "../../../shared/api/apiClient";

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
  pullRequestCount: number;
  issueCount: number;
  documentCount: number;
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
  category: "ARCHITECTURE" | "DATABASE" | "TESTING" | "DEVOPS" | "DOCUMENTATION" | "COLLABORATION" | "ACTIVITY";
  label: string;
  signals: EvidenceSignal[];
};

export type RepositoryEvidenceSummary = {
  repositoryId: string;
  snapshotId: string;
  extractorVersion: string;
  categories: EvidenceCategory[];
  activityTimeline: RepositoryActivityTimeline;
};

export type RepositoryActivityEvent = {
  eventType: "COMMIT" | "PULL_REQUEST_OPENED" | "PULL_REQUEST_CLOSED" | "PULL_REQUEST_MERGED" | "ISSUE_OPENED" | "ISSUE_CLOSED";
  sourceReference: string;
  occurredAt: string;
};

export type RepositoryActivityTimeline = {
  extractorVersion: "repository-activity-timeline-v1";
  scope: "CURRENT_SNAPSHOT";
  measuredAt: string;
  latestActivityAt: string | null;
  daysSinceLatestActivity: number | null;
  totalEventCount: number;
  truncated: boolean;
  events: RepositoryActivityEvent[];
};

export async function getRepositories(cursor: string | null = null, includeArchived = false) {
  const query = new URLSearchParams({ limit: "20", includeArchived: String(includeArchived) });
  if (cursor) query.set("cursor", cursor);
  return apiRequest<RepositoryPage>(`/api/v1/repositories?${query}`);
}

export async function getRepository(repositoryId: string) {
  return apiRequest<ImportedRepository>(`/api/v1/repositories/${repositoryId}`);
}

export async function importRepository(providerRepositoryId: string) {
  return apiRequest<ImportedRepository>("/api/v1/repositories/imports", await withCsrf({
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ providerRepositoryId })
  }));
}

export async function archiveRepository(repositoryId: string) {
  return changeRepositoryLifecycle(repositoryId, "archive");
}

export async function restoreRepository(repositoryId: string) {
  return changeRepositoryLifecycle(repositoryId, "restore");
}

export async function synchronizeRepository(repositoryId: string) {
  return apiRequest<RepositorySyncJob>(`/api/v1/repositories/${repositoryId}/sync`, await withCsrf({
    method: "POST",
    headers: {
      "Idempotency-Key": crypto.randomUUID()
    }
  }));
}

export async function getRepositorySyncJob(jobId: string) {
  return apiRequest<RepositorySyncJob>(`/api/v1/repository-sync-jobs/${jobId}`);
}

export async function getRepositorySnapshots(repositoryId: string) {
  return (await apiRequest<{ snapshots: RepositorySnapshot[] }>(
    `/api/v1/repositories/${repositoryId}/snapshots`
  )).snapshots;
}

export async function getRepositorySnapshot(repositoryId: string, snapshotId: string) {
  return apiRequest<RepositorySnapshot>(
    `/api/v1/repositories/${repositoryId}/snapshots/${snapshotId}`
  );
}

export async function getRepositoryTechnologies(repositoryId: string) {
  return apiRequest<TechnologySummary>(
    `/api/v1/repositories/${repositoryId}/technologies`
  );
}

export async function getRepositoryEvidence(repositoryId: string) {
  return apiRequest<RepositoryEvidenceSummary>(
    `/api/v1/repositories/${repositoryId}/evidence`
  );
}

async function changeRepositoryLifecycle(repositoryId: string, action: "archive" | "restore") {
  return apiRequest<ImportedRepository>(`/api/v1/repositories/${repositoryId}/${action}`,
    await withCsrf({ method: "POST" }));
}
