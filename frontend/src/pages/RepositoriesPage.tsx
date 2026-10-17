import { Link, useSearchParams } from "react-router-dom";
import { useRef, useState, type ReactNode, type RefObject } from "react";
import type { ImportedRepository } from "../features/repositories/api/repositoryApi";
import { useArchiveRepository, useRepositories, useRestoreRepository } from "../features/repositories/model/useRepositories";
import { repositoryLifecycleLabel, repositorySyncStatusLabel } from "../shared/ui/labels";
import { ConfirmDialog } from "../shared/ui/ConfirmDialog";

export function RepositoriesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const includeArchived = searchParams.get("includeArchived") === "true";
  const query = useRepositories(includeArchived);
  const [archiveMessage, setArchiveMessage] = useState("");
  const archiveMessageRef = useRef<HTMLParagraphElement>(null);

  if (query.isPending) {
    return <Workspace><p role="status">내 저장소를 불러오는 중입니다…</p></Workspace>;
  }
  if (query.isError) {
    return (
      <Workspace>
        <div className="state-panel" role="alert">
          <h2>저장소를 불러오지 못했습니다</h2>
          <p>로그인 상태와 서버 연결을 확인한 뒤 다시 시도해 주세요.</p>
          <button type="button" onClick={() => query.refetch()}>다시 시도</button>
        </div>
      </Workspace>
    );
  }

  const repositories = query.data.pages.flatMap(page => page.repositories);
  const totalCount = query.data.pages[0]?.totalCount ?? 0;
  const privateCount = repositories.filter(repository => repository.visibility === "PRIVATE").length;
  const archivedCount = repositories.filter(repository => repository.lifecycle === "ARCHIVED").length;

  return (
    <Workspace>
      <header className="workspace-header">
        <div>
          <p className="eyebrow">저장소 작업 공간</p>
          <h1>내 GitHub 저장소</h1>
          <p>DevPath에 등록한 저장소와 분석 준비 상태를 한곳에서 확인하세요.</p>
        </div>
        <div className="workspace-actions">
          <Link className="button-link button-secondary" to="/skills">기술 분석 보기</Link>
          <Link className="button-link" to="/settings/integrations#github">저장소 추가</Link>
        </div>
      </header>

      <section className="repository-stats" aria-label="저장소 요약">
        <Summary label="현재 표시된 저장소" value={totalCount} />
        <Summary label="현재 표시된 비공개" value={privateCount} />
        <Summary label="현재 표시된 보관 저장소" value={archivedCount} />
      </section>

      <div className="repository-toolbar">
        <label className="archive-filter">
          <input
            type="checkbox"
            checked={includeArchived}
            onChange={event => setSearchParams(current => {
              const next = new URLSearchParams(current);
              if (event.target.checked) next.set("includeArchived", "true");
              else next.delete("includeArchived");
              return next;
            }, { replace: true })}
          />
          보관 저장소 포함
        </label>
      </div>
      <p ref={archiveMessageRef} role="status" tabIndex={-1}>{archiveMessage}</p>

      {repositories.length === 0 ? (
        <div className="state-panel">
          <h2>{includeArchived ? "등록된 저장소가 없습니다" : "표시할 활성 저장소가 없습니다"}</h2>
          <p>{includeArchived ? "GitHub 연결 설정에서 저장소를 선택해 DevPath 작업 공간에 추가해 주세요." : "보관 저장소를 확인하려면 위 필터를 켜 주세요."}</p>
          <Link className="button-link" to="/settings/integrations#github">GitHub 저장소 선택</Link>
        </div>
      ) : (
        <section aria-labelledby="registered-repositories-title">
          <div className="section-heading">
            <div>
              <h2 id="registered-repositories-title">등록된 저장소</h2>
              <p>동기화가 시작되기 전까지는 GitHub 메타데이터만 보관됩니다.</p>
            </div>
          </div>
          <div className="repository-grid">
            {repositories.map(repository => <RepositoryCard key={repository.repositoryId} repository={repository} returnFocusFallback={archiveMessageRef} onArchiveMessage={setArchiveMessage} />)}
          </div>
          {query.hasNextPage ? (
            <button type="button" disabled={query.isFetchingNextPage} onClick={() => query.fetchNextPage()}>
              {query.isFetchingNextPage ? "더 불러오는 중…" : "저장소 더 보기"}
            </button>
          ) : null}
        </section>
      )}
    </Workspace>
  );
}

function RepositoryCard({ repository, returnFocusFallback, onArchiveMessage }: { repository: ImportedRepository; returnFocusFallback: RefObject<HTMLElement | null>; onArchiveMessage: (message: string) => void }) {
  const [confirmingArchive, setConfirmingArchive] = useState(false);
  const archive = useArchiveRepository();
  const restore = useRestoreRepository();
  const mutation = repository.lifecycle === "ARCHIVED" ? restore : archive;
  const canChangeLifecycle = repository.lifecycle !== "DELETED_EXTERNALLY"
    && !(repository.lifecycle === "ARCHIVED" && repository.providerArchived);

  return (
    <article className={`repository-card${repository.lifecycle === "ARCHIVED" ? " repository-card--archived" : ""}`}>
      <div className="repository-card__topline">
        <span className="status-badge">{repository.visibility === "PRIVATE" ? "비공개" : "공개"}</span>
        <span>{repositoryLifecycleLabel(repository.lifecycle)}</span>
        <span className="status-badge">동기화: {repositorySyncStatusLabel(repository.syncStatus)}</span>
      </div>
      <h3><Link to={`/repositories/${repository.repositoryId}`}>{repository.fullName}</Link></h3>
      <dl className="metadata-list">
        <div><dt>기본 브랜치</dt><dd>{repository.defaultBranch}</dd></div>
        <div><dt>등록일</dt><dd>{formatDate(repository.discoveredAt)}</dd></div>
      </dl>
      {repository.lifecycle === "ARCHIVED" && repository.providerArchived ? (
        <p className="muted">GitHub에서 먼저 저장소 보관을 해제해야 DevPath에서도 복원할 수 있습니다.</p>
      ) : repository.lifecycle === "DELETED_EXTERNALLY" ? (
        <p className="muted">GitHub에서 삭제된 저장소는 로컬에서 복원할 수 없습니다.</p>
      ) : null}
      {repository.lifecycle === "ARCHIVED" && mutation.isError ? <p className="form-error" role="alert">저장소 상태를 변경하지 못했습니다. 다시 시도해 주세요.</p> : null}
      <ConfirmDialog open={confirmingArchive} title={`${repository.fullName} 저장소를 보관할까요?`} description="활성 목록에서 숨겨지고 새 동기화와 분석을 시작할 수 없습니다. 기존 메타데이터, 불변 스냅샷과 공식 분석 결과는 유지됩니다." confirmLabel="저장소 보관" destructive pending={archive.isPending} error={archive.isError ? "저장소를 보관하지 못했습니다. 다시 시도해 주세요." : undefined} returnFocusFallback={returnFocusFallback} onCancel={() => setConfirmingArchive(false)} onConfirm={() => archive.mutate(repository.repositoryId, { onSuccess: () => { onArchiveMessage(`${repository.fullName} 저장소를 보관했습니다.`); setConfirmingArchive(false); } })} />
      <div className="repository-card__actions">
        <Link to={`/repositories/${repository.repositoryId}`}>상세 보기</Link>
        <button
          type="button"
          className={repository.lifecycle === "ARCHIVED" ? "button-quiet" : "button-quiet button-quiet--danger"}
          disabled={!canChangeLifecycle || mutation.isPending}
          onClick={() => {
            if (repository.lifecycle === "ARCHIVED") mutation.mutate(repository.repositoryId);
            else { archive.reset(); onArchiveMessage(""); setConfirmingArchive(true); }
          }}
        >
          {mutation.isPending ? "처리 중…" : repository.lifecycle === "ARCHIVED" ? "복원" : "보관"}
        </button>
        <a href={repository.htmlUrl} target="_blank" rel="noreferrer">GitHub 열기</a>
      </div>
    </article>
  );
}

function Workspace({ children }: { children: ReactNode }) {
  return <main className="shell workspace"><nav><Link to="/">← DevPath 홈</Link></nav>{children}</main>;
}

function Summary({ label, value }: { label: string; value: number }) {
  return <div><strong>{value}</strong><span>{label}</span></div>;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium" }).format(new Date(value));
}
