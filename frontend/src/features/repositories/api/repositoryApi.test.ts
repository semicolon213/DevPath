import { afterEach, expect, it, vi } from "vitest";
import {
  archiveRepository,
  getRepositories,
  getRepository,
  getRepositorySnapshot,
  getRepositorySnapshots,
  getRepositoryTechnologies,
  getRepositoryEvidence,
  getRepositorySyncJob,
  importRepository,
  restoreRepository,
  synchronizeRepository
} from "./repositoryApi";

afterEach(() => vi.unstubAllGlobals());

const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };

it("loads a bounded repository page and detail through the backend", async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(Response.json({
      data: { repositories: [], limit: 20, nextCursor: null, totalCount: 0 }, metadata
    }))
    .mockResolvedValueOnce(Response.json({
      data: { repositoryId: "repo-id", fullName: "owner/devpath" }, metadata
    }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(getRepositories()).resolves.toMatchObject({ totalCount: 0 });
  await expect(getRepository("repo-id")).resolves.toMatchObject({ fullName: "owner/devpath" });
  expect(fetchMock).toHaveBeenNthCalledWith(1, expect.stringContaining("includeArchived=false"), expect.anything());
  expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/repositories/repo-id"), expect.anything());
});

it("queues and observes a repository synchronization with immutable snapshots", async () => {
  const job = {
    jobId: "38393675-fd18-410d-9fb8-cff66200fa46",
    status: "queued",
    jobType: "REPOSITORY_SYNC"
  };
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf-token" }, metadata }))
    .mockResolvedValueOnce(Response.json({ data: job, metadata }))
    .mockResolvedValueOnce(Response.json({ data: { ...job, status: "succeeded" }, metadata }))
    .mockResolvedValueOnce(Response.json({ data: { snapshots: [{ snapshotId: "snapshot-id", immutable: true }] }, metadata }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(synchronizeRepository("repo-id")).resolves.toMatchObject({ status: "queued" });
  await expect(getRepositorySyncJob(job.jobId)).resolves.toMatchObject({ status: "succeeded" });
  await expect(getRepositorySnapshots("repo-id")).resolves.toEqual([
    expect.objectContaining({ snapshotId: "snapshot-id", immutable: true })
  ]);
  const requestHeaders = fetchMock.mock.calls[1]?.[1]?.headers as Record<string, string>;
  expect(requestHeaders["Idempotency-Key"]).toEqual(expect.any(String));
  expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/repositories/repo-id/sync"),
    expect.objectContaining({
      method: "POST",
      headers: expect.objectContaining({
        "X-CSRF-TOKEN": "csrf-token",
        "Idempotency-Key": expect.any(String)
      })
  }));
});

it("loads one owner-scoped immutable snapshot by repository and snapshot ID", async () => {
  const fetchMock = vi.fn().mockResolvedValue(Response.json({ data: {
    snapshotId: "snapshot-id", repositoryId: "repo-id", immutable: true, contentHash: "sha256:content"
  }, metadata }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(getRepositorySnapshot("repo-id", "snapshot-id")).resolves.toMatchObject({ immutable: true });
  expect(fetchMock).toHaveBeenCalledWith(
    "http://localhost:8080/api/v1/repositories/repo-id/snapshots/snapshot-id",
    expect.objectContaining({ credentials: "include" })
  );
});

it("loads deterministic language evidence from the current snapshot", async () => {
  const fetchMock = vi.fn().mockResolvedValueOnce(Response.json({
    data: {
      repositoryId: "repo-id",
      snapshotId: "snapshot-id",
      extractorVersion: "github-language-extractor-v1",
      taxonomyVersion: "technology-taxonomy-v1",
      primaryLanguage: "Java",
      technologies: [{
        name: "Java", category: "LANGUAGE", evidenceLabel: "Java", byteCount: 7500,
        percentage: 75, taxonomyStatus: "SUPPORTED", evidenceType: "PROVIDER_LANGUAGE_STATISTICS",
        evidencePaths: []
      }]
    },
    metadata
  }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(getRepositoryTechnologies("repo-id")).resolves.toMatchObject({
    primaryLanguage: "Java",
    technologies: [expect.objectContaining({ percentage: 75 })]
  });
  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("/repositories/repo-id/technologies"), expect.anything());
});

it("loads deterministic engineering evidence without scores", async () => {
  const fetchMock = vi.fn().mockResolvedValueOnce(Response.json({ data: {
    repositoryId: "repo-id", snapshotId: "snapshot-id", extractorVersion: "engineering-evidence-extractor-v1",
    categories: [{ category: "TESTING", label: "Testing", signals: [{
      signalKey: "TEST_FILES", label: "Test files", present: true, count: 3,
      observedValue: null, evidencePaths: ["src/test/UserTest.java"]
    }] }]
  }, metadata }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(getRepositoryEvidence("repo-id")).resolves.toMatchObject({
    categories: [expect.objectContaining({ category: "TESTING" })]
  });
  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("/repositories/repo-id/evidence"), expect.anything());
});

it("archives and restores through CSRF-protected lifecycle commands", async () => {
  const repository = { repositoryId: "repo-id", lifecycle: "ARCHIVED" };
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf-token" }, metadata }))
    .mockResolvedValueOnce(Response.json({ data: repository, metadata }))
    .mockResolvedValueOnce(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "csrf-token" }, metadata }))
    .mockResolvedValueOnce(Response.json({ data: { ...repository, lifecycle: "DISCOVERED" }, metadata }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(archiveRepository("repo-id")).resolves.toMatchObject({ lifecycle: "ARCHIVED" });
  await expect(restoreRepository("repo-id")).resolves.toMatchObject({ lifecycle: "DISCOVERED" });
  expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/repositories/repo-id/archive"),
    expect.objectContaining({
      method: "POST",
      headers: expect.objectContaining({ "X-CSRF-TOKEN": "csrf-token" })
    }));
  expect(fetchMock).toHaveBeenNthCalledWith(4, expect.stringContaining("/repositories/repo-id/restore"),
    expect.objectContaining({
      method: "POST",
      headers: expect.objectContaining({ "X-CSRF-TOKEN": "csrf-token" })
    }));
});

it("reverifies an import through a CSRF-protected server command", async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(Response.json({
      data: { headerName: "X-CSRF-TOKEN", token: "csrf-token" }, metadata
    }))
    .mockResolvedValueOnce(Response.json({
      data: { repositoryId: "repo-id", providerRepositoryId: "42", fullName: "owner/devpath" }, metadata
    }));
  vi.stubGlobal("fetch", fetchMock);

  await expect(importRepository("42")).resolves.toMatchObject({ repositoryId: "repo-id" });
  expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/repositories/imports"),
    expect.objectContaining({
      method: "POST",
      headers: expect.objectContaining({ "X-CSRF-TOKEN": "csrf-token" }),
      body: "{\"providerRepositoryId\":\"42\"}"
    }));
});
