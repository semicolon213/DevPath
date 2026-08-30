import { FormEvent, useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { useConnections, useNotionWorkspaces } from "../features/connections/model/useConnections";
import { useImportNotionKnowledge, useKnowledgeDocuments, useKnowledgeJob, useKnowledgeSearch, knowledgeDocumentsKey } from "../features/knowledge/model/useKnowledge";
import type { KnowledgeIngestionJob } from "../features/knowledge/api/knowledgeApi";

export function KnowledgePage() {
  const connections = useConnections();
  const notion = connections.data?.connections.find(connection => connection.provider === "NOTION");
  const workspaces = useNotionWorkspaces(notion?.status === "ACTIVE");
  const documents = useKnowledgeDocuments();
  const importer = useImportNotionKnowledge();
  const [searchParams, setSearchParams] = useSearchParams();
  const jobId = searchParams.get("job");
  const job = useKnowledgeJob(jobId);
  const queryClient = useQueryClient();
  const knowledgeSearch = useKnowledgeSearch();
  const [query, setQuery] = useState("");
  const [documentFilter, setDocumentFilter] = useState("");

  useEffect(() => {
    if (job.data?.status === "succeeded") void queryClient.invalidateQueries({ queryKey: knowledgeDocumentsKey });
  }, [job.data?.status, queryClient]);

  function startImport(connectionId: string, providerPageId: string) {
    importer.mutate({ connectionId, providerPageId }, {
      onSuccess: accepted => setSearchParams({ job: accepted.jobId }, { replace: true })
    });
  }

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    const normalized = query.trim();
    if (normalized) knowledgeSearch.mutate({ query: normalized, documentIds: documentFilter ? [documentFilter] : [] });
  }

  return <Shell>
    <header className="workspace-header"><div><p className="eyebrow">M36 · 지식 수집</p><h1>지식 작업 공간</h1>
      <p>공유된 Notion 페이지를 비공개로 수집하고 버전이 있는 청크와 임베딩으로 보관합니다.</p></div></header>
    <p className="state-panel state-panel--compact">검색 결과는 현재 권한이 있는 Notion 문서의 최신 인덱스만 사용합니다. AI 답변 생성에는 아직 연결하지 않습니다.</p>
    {jobId ? <JobPanel job={job} clear={() => setSearchParams({}, { replace: true })} /> : null}
    <section className="knowledge-section" aria-labelledby="knowledge-search-title"><h2 id="knowledge-search-title">내 지식 검색</h2>
      <form className="knowledge-search-form" onSubmit={submitSearch}>
        <label>검색어<input value={query} required maxLength={500} onChange={event => setQuery(event.target.value)} placeholder="예: 테스트 전략과 회귀 방지" /></label>
        <label>문서 범위<select value={documentFilter} onChange={event => setDocumentFilter(event.target.value)}><option value="">활성 Notion 문서 전체</option>{documents.data?.documents.filter(document => document.status === "ACTIVE").map(document => <option key={document.documentId} value={document.documentId}>{document.title}</option>)}</select></label>
        <button type="submit" disabled={knowledgeSearch.isPending || !query.trim()}>{knowledgeSearch.isPending ? "검색 중…" : "지식 검색"}</button>
      </form>
      {knowledgeSearch.isError ? <p role="alert">지식 검색을 완료하지 못했습니다. 인덱스와 embedding provider 상태를 확인한 뒤 다시 시도해 주세요.</p> : null}
      {knowledgeSearch.data?.results.length === 0 ? <p role="status">현재 권한과 필터 범위에서 관련 지식을 찾지 못했습니다.</p> : null}
      {knowledgeSearch.data?.results.length ? <div className="knowledge-search-results" aria-live="polite"><p>{knowledgeSearch.data.resultCount}개 결과 · 정책 {knowledgeSearch.data.policyVersion} · {knowledgeSearch.data.durationMs}ms</p><ol>{knowledgeSearch.data.results.map(result => <li key={result.chunkId}><article><div><span className="status-badge status-badge--active">관련도 {(result.relevance * 100).toFixed(0)}%</span><span>{result.freshness === "FRESH" ? "최신 인덱스" : result.freshness}</span></div><h3>{result.heading ?? result.documentTitle}</h3><p>{result.excerpt}</p><div className="connection-actions"><Link to={`/knowledge/${result.documentId}`}>문서 메타데이터</Link>{safeNotionUrl(result.sourceUrl) ? <a href={result.sourceUrl!} target="_blank" rel="noreferrer">Notion 원문 열기</a> : <span>Notion 출처 {result.sourceObjectId}</span>}</div></article></li>)}</ol></div> : null}
    </section>
    <section className="knowledge-section" aria-labelledby="notion-import-title"><h2 id="notion-import-title">Notion 페이지 가져오기</h2>
      {connections.isPending ? <p role="status">Notion 연결 상태를 확인하는 중입니다.</p> : null}
      {connections.isError ? <Retry message="연결 상태를 불러오지 못했습니다." retry={() => connections.refetch()} /> : null}
      {!connections.isPending && !connections.isError && notion?.status !== "ACTIVE" ? <div className="state-panel"><p>먼저 Notion을 연결하고 DevPath와 페이지를 공유해 주세요.</p><Link className="button-link" to="/settings/integrations#notion">Notion 연결 설정</Link></div> : null}
      {notion?.status === "ACTIVE" && workspaces.isPending ? <p role="status">공유된 페이지를 확인하는 중입니다.</p> : null}
      {notion?.status === "ACTIVE" && workspaces.isError ? <Retry message="공유된 Notion 페이지를 불러오지 못했습니다." retry={() => workspaces.refetch()} /> : null}
      {workspaces.data ? <div className="knowledge-source-list">{workspaces.data.workspaces.flatMap(workspace => workspace.pages.filter(page => !page.inTrash && page.objectType === "PAGE").map(page =>
        <article key={page.providerPageId}><div><h3>{page.title}</h3><p>{workspace.workspaceName} · {new Date(page.lastEditedAt).toLocaleString("ko-KR")}</p></div>
          <button type="button" disabled={importer.isPending} onClick={() => startImport(workspace.connectionId, page.providerPageId)}>{importer.isPending && importer.variables?.providerPageId === page.providerPageId ? "가져오는 중…" : "지식으로 가져오기"}</button></article>))}</div> : null}
      {importer.isError ? <p role="alert">가져오기 작업을 시작하지 못했습니다. 페이지 공유 권한을 다시 확인해 주세요.</p> : null}
    </section>
    <section className="knowledge-section" aria-labelledby="document-list-title"><h2 id="document-list-title">수집된 문서</h2>
      {documents.isPending ? <p role="status">지식 문서를 불러오는 중입니다.</p> : null}
      {documents.isError ? <Retry message="지식 문서를 불러오지 못했습니다." retry={() => documents.refetch()} /> : null}
      {documents.data?.documents.length === 0 ? <p>아직 수집된 지식 문서가 없습니다.</p> : null}
      {documents.data ? <div className="knowledge-document-grid">{documents.data.documents.map(document => <article key={document.documentId}><span className={document.status === "ACTIVE" ? "status-badge status-badge--active" : "status-badge"}>{document.status === "ACTIVE" ? "활성" : "보관됨"}</span><h3>{document.title}</h3><p>청크 {document.chunkCount}개 · {new Date(document.updatedAt).toLocaleString("ko-KR")}</p><Link to={`/knowledge/${document.documentId}`}>문서 메타데이터 보기</Link></article>)}</div> : null}
    </section>
  </Shell>;
}

function JobPanel({ job, clear }: { job: ReturnType<typeof useKnowledgeJob>; clear: () => void }) {
  if (job.isPending) return <section className="state-panel" aria-live="polite"><p role="status">수집 작업 상태를 복구하는 중입니다.</p></section>;
  if (job.isError) return <section className="state-panel" role="alert"><p>수집 작업 상태를 불러오지 못했습니다.</p><button type="button" onClick={() => job.refetch()}>다시 시도</button></section>;
  const value = job.data!;
  return <section className="knowledge-job" aria-live="polite"><div><strong>{jobLabel(value)}</strong><span>{value.progressPercent}% · 시도 {value.attemptCount}/{value.maxAttempts}</span></div><progress max={100} value={value.progressPercent}>{value.progressPercent}%</progress>
    {value.status === "succeeded" && value.resultResourceUrl ? <Link className="button-link" to={value.resultResourceUrl.replace("/api/v1/knowledge-documents/", "/knowledge/")}>수집된 문서 보기</Link> : null}
    {value.status === "failed" ? <p role="alert">{value.errorMessage ?? "지식 수집에 실패했습니다."}</p> : null}
    {(value.status === "succeeded" || value.status === "failed") ? <button className="button-secondary" type="button" onClick={clear}>작업 알림 닫기</button> : null}</section>;
}

function jobLabel(job: KnowledgeIngestionJob) {
  if (job.status === "queued") return job.phase === "RETRY_WAIT" ? "수집 작업 재시도 대기 중" : "수집 작업 대기 중";
  if (job.status === "running") return "Notion 내용을 정규화하고 색인하는 중";
  if (job.status === "succeeded") return "지식 문서 수집 완료";
  return "지식 문서 수집 실패";
}

function Retry({ message, retry }: { message: string; retry: () => void }) { return <div className="state-panel" role="alert"><p>{message}</p><button type="button" onClick={retry}>다시 시도</button></div>; }
function Shell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/">← DevPath 홈</Link></nav>{children}</main>; }
function safeNotionUrl(value: string | null) { if (!value) return false; try { const url = new URL(value); return url.protocol === "https:" && (url.hostname === "notion.so" || url.hostname.endsWith(".notion.so")); } catch { return false; } }
