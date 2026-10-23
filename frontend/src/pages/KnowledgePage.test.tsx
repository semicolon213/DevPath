import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";

afterEach(() => vi.unstubAllGlobals());

it("connects an authorized Notion page to a recoverable ingestion job", async () => {
  vi.stubGlobal("crypto", { randomUUID: () => "9edac08e-95ba-49d7-a70a-e3adce31fdb1" });
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/users/me/connections")) return Promise.resolve(json({ connections: [{ connectionId: "connection-1", provider: "NOTION", status: "ACTIVE", scopes: ["read_content"], connectedAt: "2026-08-30T00:00:00Z", expiresAt: null }] }));
    if (url.endsWith("/integrations/notion/workspaces")) return Promise.resolve(json({ workspaces: [{ connectionId: "connection-1", workspaceId: "workspace-1", workspaceName: "내 워크스페이스", workspaceIconUrl: null, status: "ACTIVE", connectedAt: "2026-08-30T00:00:00Z", discoveredAt: "2026-08-30T00:00:00Z", pages: [{ providerPageId: "page-1", title: "회고 노트", objectType: "PAGE", url: null, lastEditedAt: "2026-08-30T00:00:00Z", inTrash: false }] }] }));
    if (url.endsWith("/knowledge-documents")) return Promise.resolve(json({ documents: [] }));
    if (url.endsWith("/csrf")) return Promise.resolve(json({ headerName: "X-CSRF-TOKEN", token: "csrf" }));
    if (url.endsWith("/knowledge-documents/imports/notion") && init?.method === "POST") return Promise.resolve(json(job("queued")));
    if (url.includes("/knowledge-ingestion-jobs/job-1")) return Promise.resolve(json(job("succeeded")));
    return Promise.reject(new Error(url));
  });
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(undefined, ["/knowledge"]);

  expect(await screen.findByRole("heading", { name: "지식" })).toBeInTheDocument();
  expect(await screen.findByRole("heading", { name: "회고 노트" })).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "지식으로 가져오기" }));
  await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("/knowledge-documents/imports/notion"),
    expect.objectContaining({ method: "POST", body: JSON.stringify({ connectionId: "connection-1", providerPageId: "page-1" }) })));
  expect(await screen.findByText("지식 문서 수집 완료")).toBeInTheDocument();
});

it("searches an owner-scoped document and presents bounded evidence with its source", async () => {
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/users/me/connections")) return Promise.resolve(json({ connections: [{ connectionId: "connection-1", provider: "NOTION", status: "ACTIVE", scopes: ["read_content"], connectedAt: "2026-08-30T00:00:00Z", expiresAt: null }] }));
    if (url.endsWith("/integrations/notion/workspaces")) return Promise.resolve(json({ workspaces: [] }));
    if (url.endsWith("/knowledge-documents")) return Promise.resolve(json({ documents: [{ documentId: "document-1", sourceType: "NOTION", sourceObjectId: "page-1", title: "테스트 전략", status: "ACTIVE", currentVersionId: "version-1", chunkCount: 1, createdAt: "2026-08-30T00:00:00Z", updatedAt: "2026-08-30T00:00:00Z" }] }));
    if (url.endsWith("/csrf")) return Promise.resolve(json({ headerName: "X-CSRF-TOKEN", token: "csrf" }));
    if (url.endsWith("/knowledge-search") && init?.method === "POST") return Promise.resolve(json({ retrievalResultId: "result-1", retrievalType: "SEMANTIC", policyVersion: "knowledge-semantic-v1", contextPurpose: "USER_SEARCH", appliedFilters: { sourceTypes: ["NOTION"], documentIds: ["document-1"] }, resultCount: 1, durationMs: 4, generatedAt: "2026-08-30T00:00:00Z", results: [{ chunkId: "chunk-1", documentId: "document-1", documentTitle: "테스트 전략", sourceType: "NOTION", sourceObjectId: "page-1", sourceUrl: "https://www.notion.so/page-1", heading: "회귀 방지", excerpt: "변경 전후 자동화 테스트를 실행합니다.", relevance: 0.91, tokenEstimate: 20, freshness: "FRESH" }] }));
    return Promise.reject(new Error(url));
  });
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(undefined, ["/knowledge"]);

  fireEvent.change(await screen.findByRole("textbox", { name: "검색어" }), { target: { value: "회귀 방지" } });
  fireEvent.change(screen.getByRole("combobox", { name: "문서 범위" }), { target: { value: "document-1" } });
  fireEvent.click(screen.getByRole("button", { name: "지식 검색" }));

  expect(await screen.findByText("변경 전후 자동화 테스트를 실행합니다.")).toBeInTheDocument();
  expect(screen.getByText("관련도 91%")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Notion 원문 열기" })).toHaveAttribute("href", "https://www.notion.so/page-1");
  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("/knowledge-search"), expect.objectContaining({
    body: JSON.stringify({ query: "회귀 방지", filters: { sourceTypes: ["NOTION"], documentIds: ["document-1"] }, limit: 5, contextPurpose: "USER_SEARCH" })
  }));
});

it("keeps search dependency errors actionable and distinguishes an authorized empty result", async () => {
  let searchAttempt = 0;
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/users/me/connections")) return Promise.resolve(json({ connections: [] }));
    if (url.endsWith("/knowledge-documents")) return Promise.resolve(json({ documents: [] }));
    if (url.endsWith("/csrf")) return Promise.resolve(json({ headerName: "X-CSRF-TOKEN", token: "csrf" }));
    if (url.endsWith("/knowledge-search") && init?.method === "POST") {
      searchAttempt += 1;
      if (searchAttempt === 1) return Promise.resolve(Response.json({ error: { code: "KNOWLEDGE_RETRIEVAL_FAILED", message: "safe" }, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-30T00:00:00Z" } }, { status: 503 }));
      return Promise.resolve(json({ retrievalResultId: "result-empty", retrievalType: "SEMANTIC", policyVersion: "knowledge-semantic-v1", contextPurpose: "USER_SEARCH", appliedFilters: { sourceTypes: ["NOTION"], documentIds: [] }, results: [], resultCount: 0, durationMs: 2, generatedAt: "2026-08-30T00:00:00Z" }));
    }
    return Promise.reject(new Error(url));
  });
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(undefined, ["/knowledge"]);

  fireEvent.change(await screen.findByRole("textbox", { name: "검색어" }), { target: { value: "없는 지식" } });
  fireEvent.click(screen.getByRole("button", { name: "지식 검색" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("지식 검색을 완료하지 못했습니다");
  fireEvent.click(screen.getByRole("button", { name: "지식 검색" }));
  expect(await screen.findByRole("status")).toHaveTextContent("관련 지식을 찾지 못했습니다");
});

it("does not promise automatic retry or expose the server error for a failed ingestion job", async () => {
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith("/users/me/connections")) return Promise.resolve(json({ connections: [] }));
    if (url.endsWith("/knowledge-documents")) return Promise.resolve(json({ documents: [] }));
    if (url.endsWith("/knowledge-ingestion-jobs/job-1")) return Promise.resolve(json({
      ...job("failed"), errorCode: "RATE_LIMIT_EXCEEDED", errorMessage: "raw provider failure", retryable: true
    }));
    return Promise.reject(new Error(url));
  }));

  renderWithProviders(undefined, ["/knowledge?job=job-1"]);

  expect(await screen.findByRole("alert")).toHaveTextContent("외부 서비스 요청 한도에 도달했습니다. 잠시 후 다시 시도해 주세요.");
  expect(screen.queryByText(/자동으로 다시 시도/)).not.toBeInTheDocument();
  expect(screen.queryByText("raw provider failure")).not.toBeInTheDocument();
});

it("labels collected documents by lifecycle status and source type without claiming sync state", async () => {
  const document = { sourceType: "NOTION", sourceObjectId: "page-1", currentVersionId: "version-1", chunkCount: 2, createdAt: "2026-08-30T00:00:00Z", updatedAt: "2026-08-30T00:00:00Z" };
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith("/users/me/connections")) return Promise.resolve(json({ connections: [] }));
    if (url.endsWith("/knowledge-documents")) return Promise.resolve(json({ documents: [
      { ...document, documentId: "document-1", title: "활성 문서", status: "ACTIVE" },
      { ...document, documentId: "document-2", title: "보관 문서", status: "ARCHIVED" }
    ] }));
    return Promise.reject(new Error(url));
  }));
  renderWithProviders(undefined, ["/knowledge"]);

  const active = (await screen.findByRole("heading", { name: "활성 문서" })).closest("[role=listitem]") as HTMLElement;
  const archived = screen.getByRole("heading", { name: "보관 문서" }).closest("[role=listitem]") as HTMLElement;
  expect(within(active).getByText("활성")).toBeInTheDocument();
  expect(within(archived).getByText("보관됨")).toBeInTheDocument();
  [active, archived].forEach(item => {
    expect(within(item).getByText("Notion")).toBeInTheDocument();
    expect(within(item).getByText(/^마지막 갱신/)).toBeInTheDocument();
    expect(within(item).queryByText("문서")).not.toBeInTheDocument();
  });
  expect(screen.queryByText("동기화됨")).not.toBeInTheDocument();
  expect(screen.queryByText(/마지막 동기화/)).not.toBeInTheDocument();
});

function json(data: unknown) { return Response.json({ data, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-30T00:00:00Z" } }); }
function job(status: "queued" | "succeeded" | "failed") { return { jobId: "job-1", jobType: "KNOWLEDGE_INGESTION", status,
  phase: status === "succeeded" ? "COMPLETED" : status === "failed" ? "FAILED" : "QUEUED", progressPercent: status === "succeeded" ? 100 : 0,
  attemptCount: status === "succeeded" ? 1 : 0, maxAttempts: 3, submittedAt: "2026-08-30T00:00:00Z", startedAt: null,
  completedAt: status === "succeeded" ? "2026-08-30T00:01:00Z" : null, pollingUrl: "/api/v1/knowledge-ingestion-jobs/job-1",
  resultResourceUrl: status === "succeeded" ? "/api/v1/knowledge-documents/document-1" : null,
  errorCode: null, errorMessage: null, retryable: status !== "succeeded" }; }
