import { Link } from "react-router-dom";
import { useState, type ReactNode } from "react";
import { useRepositories } from "../features/repositories/model/useRepositories";

export function RepositoriesPage() {
  const [includeArchived, setIncludeArchived] = useState(false);
  const query = useRepositories(includeArchived);

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
          <p className="eyebrow">Repository workspace</p>
          <h1>내 GitHub 저장소</h1>
          <p>DevPath에 등록한 저장소와 분석 준비 상태를 한곳에서 확인하세요.</p>
        </div>
        <div className="workspace-actions">
          <Link className="button-link button-secondary" to="/skills">기술 분석 보기</Link>
          <Link className="button-link" to="/">저장소 추가</Link>
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
            onChange={event => setIncludeArchived(event.target.checked)}
          />
          보관 저장소 포함
        </label>
      </div>

      {repositories.length === 0 ? (
        <div className="state-panel">
          <h2>{includeArchived ? "등록된 저장소가 없습니다" : "표시할 활성 저장소가 없습니다"}</h2>
          <p>{includeArchived ? "홈에서 GitHub 저장소를 선택해 DevPath 작업 공간에 추가해 주세요." : "보관 저장소를 확인하려면 위 필터를 켜 주세요."}</p>
          <Link className="button-link" to="/">GitHub 저장소 선택</Link>
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
            {repositories.map(repository => (
              <article className="repository-card" key={repository.repositoryId}>
                <div className="repository-card__topline">
                  <span className="status-badge">{repository.visibility === "PRIVATE" ? "비공개" : "공개"}</span>
                  <span>{repository.lifecycle === "ARCHIVED" ? "DevPath에서 보관됨" : "등록 완료"}</span>
                </div>
                <h3><Link to={`/repositories/${repository.repositoryId}`}>{repository.fullName}</Link></h3>
                <dl className="metadata-list">
                  <div><dt>기본 브랜치</dt><dd>{repository.defaultBranch}</dd></div>
                  <div><dt>분석 준비</dt><dd>{syncLabel(repository.syncStatus)}</dd></div>
                  <div><dt>등록일</dt><dd>{formatDate(repository.discoveredAt)}</dd></div>
                </dl>
                <div className="repository-card__actions">
                  <Link to={`/repositories/${repository.repositoryId}`}>상세 보기</Link>
                  <a href={repository.htmlUrl} target="_blank" rel="noreferrer">GitHub 열기</a>
                </div>
              </article>
            ))}
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

function Workspace({ children }: { children: ReactNode }) {
  return <main className="shell workspace"><nav><Link to="/">← DevPath 홈</Link></nav>{children}</main>;
}

function Summary({ label, value }: { label: string; value: number }) {
  return <div><strong>{value}</strong><span>{label}</span></div>;
}

export function syncLabel(status: "NOT_SYNCED" | "SYNCHRONIZED" | "FAILED") {
  if (status === "SYNCHRONIZED") return "동기화 완료";
  if (status === "FAILED") return "동기화 확인 필요";
  return "메타데이터만 등록됨";
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium" }).format(new Date(value));
}
