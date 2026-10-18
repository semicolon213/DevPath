import { act, fireEvent, screen, waitFor, within } from "@testing-library/react";
import { useLocation } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import { AppRoutes } from "../routes/AppRoutes";
import { renderWithProviders } from "../test/renderWithProviders";

afterEach(() => vi.unstubAllGlobals());

it("renders document provenance and chunk metadata without raw content or embeddings", async () => {
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const path = new URL(String(input)).pathname;
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111") return Promise.resolve(json({
      ...knowledgeDocument,
      chunkCount: 7,
      content: "TOP SECRET DOCUMENT BODY",
      embedding: [0.123456789, 0.987654321]
    }));
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111/chunks") return Promise.resolve(json({ chunks: [
      { chunkId: "33333333-3333-4333-8333-333333333333", position: 0, heading: "API 설계", contentHash: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", tokenEstimate: 21, status: "INDEXED", text: "PRIVATE CHUNK TEXT", embedding: [0.123456789] },
      { chunkId: "44444444-4444-4444-8444-444444444444", position: 1, heading: null, contentHash: "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", tokenEstimate: 13, status: "STALE", content: "SECOND PRIVATE BODY" }
    ] }));
    return Promise.reject(new Error(path));
  });
  renderPage(fetchMock);

  expect(await screen.findByRole("heading", { name: "테스트 문서" })).toBeInTheDocument();
  expect(metadataValue("현재 버전")).toHaveTextContent(/^22222222-2222-4222-8222-222222222222$/);
  expect(metadataValue("Notion 원본 ID")).toHaveTextContent(/^notion-page-42$/);
  expect(metadataValue("청크 수")).toHaveTextContent(/^7$/);
  expect(metadataValue("마지막 갱신").textContent).toBe(new Date(knowledgeDocument.updatedAt).toLocaleString("ko-KR"));
  expect(screen.getByText("API 설계")).toBeInTheDocument();
  expect(screen.getByText("색인됨 · 약 21 토큰")).toBeInTheDocument();
  expect(screen.getByText("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")).toBeInTheDocument();
  expect(screen.getByText("청크 2")).toBeInTheDocument();
  expect(screen.getByText("갱신 필요 · 약 13 토큰")).toBeInTheDocument();
  expect(screen.getByText("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")).toBeInTheDocument();
  expect(globalThis.document.body).not.toHaveTextContent("TOP SECRET DOCUMENT BODY");
  expect(globalThis.document.body).not.toHaveTextContent("PRIVATE CHUNK TEXT");
  expect(globalThis.document.body).not.toHaveTextContent("SECOND PRIVATE BODY");
  expect(globalThis.document.body).not.toHaveTextContent("0.123456789");
  expect(globalThis.document.body).not.toHaveTextContent("0.987654321");
  const markup = globalThis.document.body.innerHTML;
  for (const secret of ["TOP SECRET DOCUMENT BODY", "PRIVATE CHUNK TEXT", "SECOND PRIVATE BODY", "0.123456789", "0.987654321"]) {
    expect(markup).not.toContain(secret);
  }
});

it("requires archive confirmation and sends the confirmed request through the CSRF boundary", async () => {
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const path = new URL(String(input)).pathname;
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111") return Promise.resolve(json(knowledgeDocument));
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111/chunks") return Promise.resolve(json({ chunks: [] }));
    if (path === "/api/v1/csrf") return Promise.resolve(json({ headerName: "X-CSRF-TOKEN", parameterName: "_csrf", token: "csrf-token" }));
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111/archive" && init?.method === "POST") return Promise.resolve(json({ ...knowledgeDocument, status: "ARCHIVED" }));
    return Promise.reject(new Error(path));
  });
  renderPage(fetchMock);

  const archiveButton = await screen.findByRole("button", { name: "문서 보관" });
  fireEvent.click(archiveButton);
  expect(screen.getByRole("dialog", { name: "이 문서를 보관할까요?" })).toHaveTextContent("활성 지식 검색 범위에서는 제외");
  await act(async () => {});
  expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith("/csrf"))).toBe(false);
  expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith("/archive"))).toBe(false);
  expect(archiveButton).toBeEnabled();

  fireEvent.click(screen.getByRole("button", { name: "취소" }));
  fireEvent.click(archiveButton);
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "문서 보관" }));
  await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
    expect.stringMatching(/\/api\/v1\/knowledge-documents\/11111111-1111-4111-8111-111111111111\/archive$/),
    expect.objectContaining({
      method: "POST",
      headers: expect.objectContaining({ "X-CSRF-TOKEN": "csrf-token" })
    })
  ));
  expect(await screen.findByText("문서를 보관했습니다.")).toHaveAttribute("role", "status");
});

it("reindexes through the CSRF boundary and renders the accepted job from the URL", async () => {
  vi.stubGlobal("crypto", { randomUUID: () => "fixed-idempotency-key" });
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const path = new URL(String(input)).pathname;
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111") return Promise.resolve(json(knowledgeDocument));
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111/chunks") return Promise.resolve(json({ chunks: [] }));
    if (path === "/api/v1/csrf") return Promise.resolve(json({ headerName: "X-CSRF-TOKEN", parameterName: "_csrf", token: "csrf-token" }));
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111/reindex" && init?.method === "POST") return Promise.resolve(json(job("queued", 0), { status: 202 }));
    if (path === "/api/v1/knowledge-ingestion-jobs/55555555-5555-4555-8555-555555555555") return Promise.resolve(json(job("succeeded", 100)));
    return Promise.reject(new Error(path));
  });
  renderPage(fetchMock, true);

  fireEvent.click(await screen.findByRole("button", { name: "재색인" }));
  await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
    expect.stringMatching(/\/api\/v1\/knowledge-documents\/11111111-1111-4111-8111-111111111111\/reindex$/),
    expect.objectContaining({
      method: "POST",
      headers: expect.objectContaining({ "X-CSRF-TOKEN": "csrf-token", "Idempotency-Key": "fixed-idempotency-key" })
    })
  ));
  expect(await screen.findByTestId("location")).toHaveTextContent("/knowledge/11111111-1111-4111-8111-111111111111?job=55555555-5555-4555-8555-555555555555");
  expect(await screen.findByText("재색인 상태: 완료 100%")).toHaveAttribute("role", "status");
});

it("announces the pending document and chunk load", () => {
  const fetchMock = vi.fn(() => new Promise<Response>(() => undefined));
  renderPage(fetchMock);

  expect(screen.getByRole("status")).toHaveTextContent("지식 문서를 불러오는 중입니다.");
});

it("announces a load failure and retries both resources", async () => {
  let documentAttempts = 0;
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const path = new URL(String(input)).pathname;
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111") {
      documentAttempts += 1;
      return documentAttempts === 1
        ? Promise.reject(new TypeError("offline"))
        : Promise.resolve(json(knowledgeDocument));
    }
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111/chunks") return Promise.resolve(json({ chunks: [] }));
    return Promise.reject(new Error(path));
  });
  renderPage(fetchMock);

  expect(await screen.findByRole("heading", { name: "서버에 연결할 수 없습니다." })).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
  expect(await screen.findByRole("heading", { name: "테스트 문서" })).toBeInTheDocument();
  expect(documentAttempts).toBe(2);
  expect(fetchMock.mock.calls.filter(([input]) => String(input).endsWith("/chunks"))).toHaveLength(2);
});

it("shows the internal error state with the request reference for a server failure", async () => {
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const path = new URL(String(input)).pathname;
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111") {
      return Promise.resolve(Response.json({ error: { code: "INTERNAL_ERROR", message: "safe" } }, { status: 500, headers: { "X-Request-Id": "knowledge-request-500" } }));
    }
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111/chunks") return Promise.resolve(json({ chunks: [] }));
    return Promise.reject(new Error(path));
  });
  renderPage(fetchMock);

  expect(await screen.findByRole("heading", { name: "지식 문서를 불러오지 못했습니다" })).toBeInTheDocument();
  expect(screen.getByText("knowledge-request-500")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
});

it("announces reindex and archive failures", async () => {
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const path = new URL(String(input)).pathname;
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111") return Promise.resolve(json(knowledgeDocument));
    if (path === "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111/chunks") return Promise.resolve(json({ chunks: [] }));
    if (path === "/api/v1/csrf") return Promise.resolve(json({ headerName: "X-CSRF-TOKEN", parameterName: "_csrf", token: "csrf-token" }));
    if ((path.endsWith("/reindex") || path.endsWith("/archive")) && init?.method === "POST") {
      return Promise.resolve(Response.json({ error: { code: "FAILED", message: "safe" } }, { status: 503 }));
    }
    return Promise.reject(new Error(path));
  });
  renderPage(fetchMock);

  fireEvent.click(await screen.findByRole("button", { name: "재색인" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("재색인 작업을 시작하지 못했습니다.");
  fireEvent.click(screen.getByRole("button", { name: "문서 보관" }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "문서 보관" }));
  await waitFor(() => expect(screen.getAllByRole("alert")).toHaveLength(2));
  expect(screen.getByRole("dialog")).toBeInTheDocument();
  expect(within(screen.getByRole("dialog")).getByRole("alert")).toHaveTextContent("문서를 보관하지 못했습니다.");
  expect(within(screen.getByRole("dialog")).getByRole("button", { name: "문서 보관" })).toBeEnabled();
});

function renderPage(fetchMock: ReturnType<typeof vi.fn>, showLocation = false) {
  vi.stubGlobal("fetch", fetchMock);
  renderWithProviders(<><AppRoutes />{showLocation ? <LocationProbe /> : null}</>, ["/knowledge/11111111-1111-4111-8111-111111111111"]);
}

function metadataValue(label: string) {
  const value = screen.getByText(label, { selector: "dt" }).nextElementSibling;
  expect(value?.tagName).toBe("DD");
  return value as HTMLElement;
}

function LocationProbe() {
  const location = useLocation();
  return <span data-testid="location">{location.pathname}{location.search}</span>;
}

function json(data: unknown, init?: ResponseInit) {
  return Response.json({ data, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-30T00:00:00Z" } }, init);
}

const knowledgeDocument = {
  documentId: "11111111-1111-4111-8111-111111111111",
  sourceType: "NOTION",
  sourceObjectId: "notion-page-42",
  title: "테스트 문서",
  status: "ACTIVE",
  currentVersionId: "22222222-2222-4222-8222-222222222222",
  chunkCount: 2,
  createdAt: "2026-08-29T00:00:00Z",
  updatedAt: "2026-08-30T00:00:00Z"
};

function job(status: "queued" | "succeeded", progressPercent: number) {
  return {
    jobId: "55555555-5555-4555-8555-555555555555", jobType: "KNOWLEDGE_INGESTION", status,
    phase: status === "succeeded" ? "COMPLETED" : "QUEUED", progressPercent,
    attemptCount: status === "succeeded" ? 1 : 0, maxAttempts: 3,
    submittedAt: "2026-08-30T00:00:00Z", startedAt: null,
    completedAt: status === "succeeded" ? "2026-08-30T00:01:00Z" : null,
    pollingUrl: "/api/v1/knowledge-ingestion-jobs/55555555-5555-4555-8555-555555555555",
    resultResourceUrl: status === "succeeded" ? "/api/v1/knowledge-documents/11111111-1111-4111-8111-111111111111" : null,
    errorCode: null, errorMessage: null, retryable: status !== "succeeded"
  };
}
