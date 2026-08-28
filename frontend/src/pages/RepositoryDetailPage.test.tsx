import { fireEvent, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "../test/renderWithProviders";
import { RepositoryDetailPage } from "./RepositoryDetailPage";

afterEach(() => vi.unstubAllGlobals());

it("shows synchronization controls and immutable snapshot history separately from analysis", async () => {
  const repository = {
      repositoryId: "3fd75d74-17d4-4dc5-bf3b-251f611633f2",
      providerRepositoryId: "42",
      name: "devpath",
      fullName: "owner/devpath",
      owner: "owner",
      visibility: "PUBLIC",
      defaultBranch: "main",
      providerArchived: false,
      lifecycle: "DISCOVERED",
      syncStatus: "NOT_SYNCED",
      htmlUrl: "https://github.com/owner/devpath",
      discoveredAt: "2026-08-11T00:00:00Z",
      lastSyncedAt: null,
      currentSnapshotId: null
  };
  const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    const url = input.toString();
    if (url.endsWith("/snapshots")) {
      return Promise.resolve(Response.json({ data: { snapshots: [] }, metadata }));
    }
    return Promise.resolve(Response.json({ data: repository, metadata }));
  }));

  renderWithProviders(
    <Routes><Route path="/repositories/:repositoryId" element={<RepositoryDetailPage />} /></Routes>,
    ["/repositories/3fd75d74-17d4-4dc5-bf3b-251f611633f2"]
  );

  expect(await screen.findByRole("heading", { name: "owner/devpath" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "분석 준비 상태" })).toBeInTheDocument();
  expect(screen.getByText(/버전이 고정된 Rule Engine으로만 계산됩니다/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "결정론적 분석 시작" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "저장소 보관" })).toBeEnabled();
  expect(screen.getByRole("button", { name: "GitHub 동기화" })).toBeEnabled();
  expect(await screen.findByText("아직 완료된 동기화가 없습니다.")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "저장소 보관" }));
  expect(screen.getByRole("heading", { name: "이 저장소를 DevPath에서 보관할까요?" })).toBeInTheDocument();
  expect(screen.getByText(/불변 스냅샷, 공식 분석 결과는 삭제되지 않습니다/)).toBeInTheDocument();
});

it("shows normalized languages frameworks and databases with evidence paths", async () => {
  const repository = {
    repositoryId: "3fd75d74-17d4-4dc5-bf3b-251f611633f2", providerRepositoryId: "42",
    name: "devpath", fullName: "owner/devpath", owner: "owner", visibility: "PUBLIC",
    defaultBranch: "main", providerArchived: false, lifecycle: "ACTIVE", syncStatus: "SYNCHRONIZED",
    htmlUrl: "https://github.com/owner/devpath", discoveredAt: "2026-08-11T00:00:00Z",
    lastSyncedAt: "2026-08-11T01:00:00Z", currentSnapshotId: "snapshot-id"
  };
  const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    const url = input.toString();
    if (url.endsWith("/snapshots")) return Promise.resolve(Response.json({ data: { snapshots: [] }, metadata }));
    if (url.endsWith("/technologies")) return Promise.resolve(Response.json({ data: {
      repositoryId: repository.repositoryId, snapshotId: "snapshot-id",
      extractorVersion: "repository-technology-summary-v1", taxonomyVersion: "technology-taxonomy-v1",
      primaryLanguage: "TypeScript", technologies: [
        { name: "TypeScript", category: "LANGUAGE", evidenceLabel: "TypeScript", byteCount: 900,
          percentage: 90, taxonomyStatus: "SUPPORTED", evidenceType: "PROVIDER_LANGUAGE_STATISTICS", evidencePaths: [] },
        { name: "React", category: "FRAMEWORK", evidenceLabel: "react", byteCount: null,
          percentage: null, taxonomyStatus: "SUPPORTED", evidenceType: "DEPENDENCY_DECLARATION",
          evidencePaths: ["frontend/package.json"] },
        { name: "PostgreSQL", category: "DATABASE", evidenceLabel: "org.postgresql:postgresql", byteCount: null,
          percentage: null, taxonomyStatus: "SUPPORTED", evidenceType: "DEPENDENCY_DECLARATION",
          evidencePaths: ["backend/build.gradle"] }
      ]
    }, metadata }));
    if (url.endsWith("/evidence")) return Promise.resolve(Response.json({ data: {
      repositoryId: repository.repositoryId, snapshotId: "snapshot-id",
      extractorVersion: "engineering-evidence-extractor-v3",
      categories: [
        { category: "DATABASE", label: "Database", signals: [
          { signalKey: "DATABASE_MIGRATIONS", label: "Database migrations", present: true, count: 2,
            observedValue: null, evidencePaths: ["backend/src/main/resources/db/migration/V1__schema.sql"] }
        ] },
        { category: "TESTING", label: "Testing", signals: [
          { signalKey: "TEST_FILES", label: "Test files", present: true, count: 12,
            observedValue: null, evidencePaths: ["backend/src/test/UserTest.java"] }
        ] },
        { category: "DEVOPS", label: "DevOps", signals: [
          { signalKey: "CONTAINER_CONFIGURATION", label: "Container configuration", present: true,
            count: 1, observedValue: null, evidencePaths: ["dockerfile"] }
        ] },
        { category: "DOCUMENTATION", label: "Documentation", signals: [
          { signalKey: "README_QUALITY_SECTIONS", label: "README quality sections", present: true,
            count: 4, observedValue: "OVERVIEW, SETUP, TESTING, USAGE", evidencePaths: ["README.md"] }
        ] },
        { category: "COLLABORATION", label: "Collaboration", signals: [
          { signalKey: "PULL_REQUEST_REVIEW_COUNT", label: "Pull request reviews", present: true,
            count: 8, observedValue: "8", evidencePaths: [] },
          { signalKey: "CLOSED_ISSUE_COUNT", label: "Closed issues", present: true,
            count: 5, observedValue: "5", evidencePaths: [] }
        ] }
      ],
      activityTimeline: {
        extractorVersion: "repository-activity-timeline-v1", scope: "CURRENT_SNAPSHOT",
        measuredAt: "2026-08-11T01:00:00Z", latestActivityAt: "2026-08-10T00:00:00Z",
        daysSinceLatestActivity: 1, totalEventCount: 3, truncated: false,
        events: [
          { eventType: "PULL_REQUEST_MERGED", sourceReference: "501", occurredAt: "2026-08-10T00:00:00Z" },
          { eventType: "COMMIT", sourceReference: "abcdef1234567890abcdef", occurredAt: "2026-08-09T00:00:00Z" },
          { eventType: "ISSUE_CLOSED", sourceReference: "601", occurredAt: "2026-08-08T00:00:00Z" }
        ]
      }
    }, metadata }));
    return Promise.resolve(Response.json({ data: repository, metadata }));
  }));

  renderWithProviders(
    <Routes><Route path="/repositories/:repositoryId" element={<RepositoryDetailPage />} /></Routes>,
    ["/repositories/3fd75d74-17d4-4dc5-bf3b-251f611633f2"]
  );

  expect(await screen.findByRole("heading", { name: "감지된 기술 스택" })).toBeInTheDocument();
  expect(await screen.findByText("React")).toBeInTheDocument();
  expect(screen.getByText("PostgreSQL")).toBeInTheDocument();
  expect(screen.getByText("증거: frontend/package.json")).toBeInTheDocument();
  expect(screen.getByText("프레임워크")).toBeInTheDocument();
  expect(screen.getByText("데이터베이스")).toBeInTheDocument();
  expect(await screen.findByRole("heading", { name: "엔지니어링 증거" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "결정론적 분석 시작" })).toBeEnabled();
  expect(screen.getByText("테스트 파일")).toBeInTheDocument();
  expect(screen.getByText("데이터베이스 근거")).toBeInTheDocument();
  expect(screen.getByText("데이터베이스 마이그레이션")).toBeInTheDocument();
  expect(screen.getByText("컨테이너 설정")).toBeInTheDocument();
  expect(screen.getByText("협업")).toBeInTheDocument();
  expect(screen.getByText("PR 리뷰")).toBeInTheDocument();
  expect(screen.getByText("종료된 이슈")).toBeInTheDocument();
  expect(screen.getByText("README 품질 섹션")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "저장소 활동 타임라인" })).toBeInTheDocument();
  expect(screen.getByText("PR 병합")).toBeInTheDocument();
  expect(screen.getByText("커밋")).toBeInTheDocument();
  expect(screen.getByText("이슈 종료")).toBeInTheDocument();
  expect(screen.getByText(/스냅샷 수집 시점 기준 1일 전/)).toBeInTheDocument();
});

it("shows automatic provider-reset recovery for a rate-limited synchronization job", async () => {
  const repository = {
    repositoryId: "3fd75d74-17d4-4dc5-bf3b-251f611633f2", providerRepositoryId: "42",
    name: "devpath", fullName: "owner/devpath", owner: "owner", visibility: "PUBLIC",
    defaultBranch: "main", providerArchived: false, lifecycle: "ACTIVE", syncStatus: "NOT_SYNCED",
    htmlUrl: "https://github.com/owner/devpath", discoveredAt: "2026-08-11T00:00:00Z",
    lastSyncedAt: null, currentSnapshotId: null
  };
  const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };
  const job = {
    jobId: "38393675-fd18-410d-9fb8-cff66200fa46", jobType: "REPOSITORY_SYNC",
    status: "queued", phase: "RETRY_WAIT", progressPercent: 0, attemptCount: 1, maxAttempts: 3,
    submittedAt: "2026-08-11T00:00:00Z", startedAt: "2026-08-11T00:00:01Z", completedAt: null,
    pollingUrl: "/api/v1/repository-sync-jobs/38393675-fd18-410d-9fb8-cff66200fa46",
    resultResourceUrl: null, errorCode: "RATE_LIMIT_EXCEEDED",
    errorMessage: "GitHub request limit reached; synchronization will resume after reset.", retryable: false
  };
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    const url = input.toString();
    if (url.endsWith("/csrf")) return Promise.resolve(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "token" }, metadata }));
    if (url.endsWith("/sync") || url.includes("/repository-sync-jobs/")) {
      return Promise.resolve(Response.json({ data: job, metadata }, { status: url.endsWith("/sync") ? 202 : 200 }));
    }
    if (url.endsWith("/snapshots")) return Promise.resolve(Response.json({ data: { snapshots: [] }, metadata }));
    return Promise.resolve(Response.json({ data: repository, metadata }));
  }));

  renderWithProviders(
    <Routes><Route path="/repositories/:repositoryId" element={<RepositoryDetailPage />} /></Routes>,
    ["/repositories/3fd75d74-17d4-4dc5-bf3b-251f611633f2"]
  );

  fireEvent.click(await screen.findByRole("button", { name: "GitHub 동기화" }));

  expect(await screen.findByText("재시도 대기 중")).toBeInTheDocument();
  expect(screen.getByText(/요청 한도가 해제되면 서버가 자동으로/)).toBeInTheDocument();
});

it("restores a completed synchronization from the URL and links its immutable result", async () => {
  const repositoryId = "3fd75d74-17d4-4dc5-bf3b-251f611633f2";
  const snapshotId = "59cf3b41-73fb-4669-8c3f-a0d3c8053e89";
  const jobId = "38393675-fd18-410d-9fb8-cff66200fa46";
  const repository = { repositoryId, providerRepositoryId: "42", name: "devpath", fullName: "owner/devpath",
    owner: "owner", visibility: "PUBLIC", defaultBranch: "main", providerArchived: false, lifecycle: "ACTIVE",
    syncStatus: "SYNCHRONIZED", htmlUrl: "https://github.com/owner/devpath", discoveredAt: "2026-08-11T00:00:00Z",
    lastSyncedAt: "2026-08-11T01:00:00Z", currentSnapshotId: snapshotId };
  const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };
  const snapshot = { snapshotId, repositoryId, capturedAt: metadata.timestamp, sourceRevision: "abcdef123456",
    status: "READY", immutable: true, contentHash: "sha256:content", branchCount: 2, commitCount: 42,
    pullRequestCount: 8, issueCount: 5, documentCount: 1 };
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/repository-sync-jobs/")) return Promise.resolve(Response.json({ data: {
      jobId, jobType: "REPOSITORY_SYNC", status: "succeeded", phase: "COMPLETED", progressPercent: 100,
      attemptCount: 1, maxAttempts: 3, submittedAt: metadata.timestamp, startedAt: metadata.timestamp,
      completedAt: metadata.timestamp, pollingUrl: `/api/v1/repository-sync-jobs/${jobId}`,
      resultResourceUrl: `/api/v1/repositories/${repositoryId}/snapshots/${snapshotId}`,
      errorCode: null, errorMessage: null, retryable: false
    }, metadata }));
    if (url.endsWith("/snapshots")) return Promise.resolve(Response.json({ data: { snapshots: [snapshot] }, metadata }));
    if (url.endsWith("/technologies")) return Promise.resolve(Response.json({ data: { repositoryId, snapshotId,
      extractorVersion: "technology-v1", taxonomyVersion: "taxonomy-v1", primaryLanguage: null, technologies: [] }, metadata }));
    if (url.endsWith("/evidence")) return Promise.resolve(Response.json({ data: { repositoryId, snapshotId,
      extractorVersion: "evidence-v1", categories: [], activityTimeline: { extractorVersion: "repository-activity-timeline-v1",
        scope: "CURRENT_SNAPSHOT", measuredAt: metadata.timestamp, latestActivityAt: null, daysSinceLatestActivity: null,
        totalEventCount: 0, truncated: false, events: [] } }, metadata }));
    return Promise.resolve(Response.json({ data: repository, metadata }));
  });
  vi.stubGlobal("fetch", fetchMock);

  renderWithProviders(<Routes><Route path="/repositories/:repositoryId" element={<RepositoryDetailPage />} /></Routes>,
    [`/repositories/${repositoryId}?syncJobId=${jobId}`]);

  expect(await screen.findAllByText("동기화 완료")).toHaveLength(2);
  expect(screen.getByRole("link", { name: "생성된 불변 스냅샷 보기" })).toHaveAttribute("href", `/repositories/${repositoryId}/snapshots/${snapshotId}`);
  expect(screen.getByRole("link", { name: "스냅샷 상세 보기" })).toHaveAttribute("href", `/repositories/${repositoryId}/snapshots/${snapshotId}`);
  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining(`/repository-sync-jobs/${jobId}`), expect.anything());
});

it("explains a non-retryable large-repository failure without claiming a partial snapshot", async () => {
  const repository = {
    repositoryId: "3fd75d74-17d4-4dc5-bf3b-251f611633f2", providerRepositoryId: "42",
    name: "devpath", fullName: "owner/devpath", owner: "owner", visibility: "PUBLIC",
    defaultBranch: "main", providerArchived: false, lifecycle: "ACTIVE", syncStatus: "NOT_SYNCED",
    htmlUrl: "https://github.com/owner/devpath", discoveredAt: "2026-08-11T00:00:00Z",
    lastSyncedAt: null, currentSnapshotId: null
  };
  const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };
  const failedJob = {
    jobId: "38393675-fd18-410d-9fb8-cff66200fa46", jobType: "REPOSITORY_SYNC",
    status: "failed", phase: "FAILED", progressPercent: 10, attemptCount: 1, maxAttempts: 3,
    submittedAt: metadata.timestamp, startedAt: metadata.timestamp, completedAt: metadata.timestamp,
    pollingUrl: "/api/v1/repository-sync-jobs/38393675-fd18-410d-9fb8-cff66200fa46",
    resultResourceUrl: null, errorCode: "COLLECTION_LIMIT_EXCEEDED",
    errorMessage: "Repository facts exceed the current safe collection limit; no partial snapshot was created.",
    retryable: false
  };
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    const url = input.toString();
    if (url.endsWith("/csrf")) return Promise.resolve(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "token" }, metadata }));
    if (url.endsWith("/sync") || url.includes("/repository-sync-jobs/")) {
      return Promise.resolve(Response.json({ data: failedJob, metadata }, { status: url.endsWith("/sync") ? 202 : 200 }));
    }
    if (url.endsWith("/snapshots")) return Promise.resolve(Response.json({ data: { snapshots: [] }, metadata }));
    return Promise.resolve(Response.json({ data: repository, metadata }));
  }));

  renderWithProviders(
    <Routes><Route path="/repositories/:repositoryId" element={<RepositoryDetailPage />} /></Routes>,
    ["/repositories/3fd75d74-17d4-4dc5-bf3b-251f611633f2"]
  );

  fireEvent.click(await screen.findByRole("button", { name: "GitHub 동기화" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("안전 수집 범위를 초과");
  expect(screen.getByRole("alert")).toHaveTextContent("부분 스냅샷은 생성되지 않았고");
  expect(screen.getByRole("alert")).toHaveTextContent("자동으로 재시도하지 않습니다");
});
