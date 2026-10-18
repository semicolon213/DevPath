import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useArchiveKnowledgeDocument, useKnowledgeChunks, useKnowledgeDocument, useKnowledgeJob, useReindexKnowledgeDocument } from "../features/knowledge/model/useKnowledge";
import { chunkStatusLabel, jobStatusLabel } from "../shared/ui/labels";
import { ConfirmDialog } from "../shared/ui/ConfirmDialog";
import { useRef, useState } from "react";
import { PageState } from "../shared/ui/PageState";

export function KnowledgeDetailPage() {
  const { documentId = "" } = useParams();
  const document = useKnowledgeDocument(documentId);
  const chunks = useKnowledgeChunks(documentId);
  const archive = useArchiveKnowledgeDocument();
  const reindex = useReindexKnowledgeDocument();
  const [searchParams, setSearchParams] = useSearchParams();
  const job = useKnowledgeJob(searchParams.get("job"));
  const navigate = useNavigate();
  const [confirmingArchive, setConfirmingArchive] = useState(false);
  const [archiveMessage, setArchiveMessage] = useState("");
  const archiveMessageRef = useRef<HTMLParagraphElement>(null);
  if (document.isPending || chunks.isPending) return <Shell><PageState state="loading" title="지식 문서" message="지식 문서를 불러오는 중입니다." /></Shell>;
  if (document.isError || chunks.isError) return <Shell><PageState state="error" title="지식 문서를 표시할 수 없습니다" errorTitle="지식 문서를 불러오지 못했습니다" error={document.error ?? chunks.error} retry={() => { void document.refetch(); void chunks.refetch(); }} backTo="/knowledge" backLabel="지식 작업공간으로 이동" /></Shell>;
  const value = document.data;
  return <Shell><header className="workspace-header"><div><p className="eyebrow">Notion 지식 문서</p><h1>{value.title}</h1><p>본문과 임베딩은 노출하지 않고 추적 가능한 버전·청크 메타데이터만 표시합니다.</p></div><span className={value.status === "ACTIVE" ? "status-badge status-badge--info" : "status-badge"}>{value.status === "ACTIVE" ? "활성" : "보관됨"}</span></header>
    <dl className="snapshot-provenance"><div><dt>현재 버전</dt><dd>{value.currentVersionId ?? "없음"}</dd></div><div><dt>Notion 원본 ID</dt><dd>{value.sourceObjectId}</dd></div><div><dt>청크 수</dt><dd>{value.chunkCount}</dd></div><div><dt>마지막 갱신</dt><dd>{new Date(value.updatedAt).toLocaleString("ko-KR")}</dd></div></dl>
    {searchParams.get("job") ? <p role="status">재색인 상태: {job.data ? jobStatusLabel(job.data.status) : "확인 중"} {job.data ? `${job.data.progressPercent}%` : ""}</p> : null}
    <div className="connection-actions"><button type="button" disabled={reindex.isPending} onClick={() => reindex.mutate(documentId, { onSuccess: accepted => setSearchParams({ job: accepted.jobId }, { replace: true }) })}>재색인</button>
      {value.status === "ACTIVE" ? <button className="button-secondary" type="button" disabled={archive.isPending} onClick={() => { archive.reset(); setArchiveMessage(""); setConfirmingArchive(true); }}>문서 보관</button> : null}</div>
    <p ref={archiveMessageRef} role="status" tabIndex={-1}>{archiveMessage}</p>
    <ConfirmDialog open={confirmingArchive} title="이 문서를 보관할까요?" description="문서 메타데이터는 유지되지만 활성 지식 검색 범위에서는 제외됩니다." confirmLabel="문서 보관" destructive pending={archive.isPending} error={archive.isError ? "문서를 보관하지 못했습니다. 다시 시도해 주세요." : undefined} returnFocusFallback={archiveMessageRef} onCancel={() => setConfirmingArchive(false)} onConfirm={() => archive.mutate(documentId, { onSuccess: () => { setArchiveMessage("문서를 보관했습니다."); setConfirmingArchive(false); } })} />
    {reindex.isError ? <p role="alert">재색인 작업을 시작하지 못했습니다.</p> : null}
    <section className="knowledge-section"><h2>청크 메타데이터</h2>{chunks.data.chunks.length === 0 ? <p>현재 버전에 청크가 없습니다.</p> : <ol className="knowledge-chunk-list">{chunks.data.chunks.map(chunk => <li key={chunk.chunkId}><div><strong>{chunk.heading ?? `청크 ${chunk.position + 1}`}</strong><span>{chunkStatusLabel(chunk.status)} · 약 {chunk.tokenEstimate} 토큰</span></div><code>{chunk.contentHash}</code></li>)}</ol>}</section>
    <button className="button-secondary" type="button" onClick={() => navigate("/knowledge")}>지식 작업 공간으로 돌아가기</button>
  </Shell>;
}
function Shell({ children }: { children: React.ReactNode }) { return <main className="shell workspace"><nav><Link to="/knowledge">← 지식 작업 공간</Link></nav>{children}</main>; }
