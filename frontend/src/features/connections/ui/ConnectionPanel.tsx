import {
  useAuthorizeGitHub,
  useConnections,
  useDisconnectGitHub,
  useGitHubRepositories,
  useAuthorizeNotion,
  useDisconnectNotion,
  useNotionWorkspaces
} from "../model/useConnections";
import { Link } from "react-router-dom";
import { useImportRepository } from "../../repositories/model/useRepositories";
import { rateLimitMessage } from "../../../shared/api/apiClient";

export function ConnectionPanel() {
  const connections = useConnections();
  const authorize = useAuthorizeGitHub();
  const disconnect = useDisconnectGitHub();
  const importRepository = useImportRepository();
  const github = connections.data?.connections.find(connection => connection.provider === "GITHUB");
  const githubActive = github?.status === "ACTIVE";
  const repositories = useGitHubRepositories(githubActive);
  const callbackResult = new URLSearchParams(window.location.search).get("githubConnection");

  if (connections.isPending) {
    return <p role="status">외부 서비스 연결 상태를 확인하는 중입니다…</p>;
  }

  if (connections.isError) {
    return <div className="state-panel" role="alert"><p>외부 서비스 연결 상태를 불러오지 못했습니다.</p><button type="button" onClick={() => connections.refetch()}>다시 시도</button></div>;
  }

  function connect() {
    authorize.mutate(undefined, {
      onSuccess: result => window.location.assign(result.authorizationUrl)
    });
  }

  function reconnect() {
    connect();
  }

  function disconnectConnection() {
    if (window.confirm("GitHub 저장소 연결을 해제할까요? 다시 연결하기 전까지 저장소에 접근할 수 없습니다.")) {
      disconnect.mutate();
    }
  }

  return (
    <>
    <section id="github" className="connection-panel" aria-labelledby="connection-title">
      <h3 id="connection-title">외부 서비스 연결</h3>
      {callbackResult === "installation-required" ? (
        <p role="alert">GitHub App을 저장소에 설치한 뒤 다시 연결해 주세요.</p>
      ) : null}
      {callbackResult === "failed" ? (
        <p role="alert">GitHub 저장소 연결에 실패했습니다. 다시 시도해 주세요.</p>
      ) : null}
      {callbackResult === "success" ? <p role="status">GitHub 저장소 연결을 완료했습니다.</p> : null}
      <div className="connection-status">
        <div>
          <strong>GitHub 저장소 접근</strong>
          <p>{connectionDescription(github?.status)}</p>
        </div>
        <span className={githubActive ? "status-badge status-badge--active" : "status-badge"}>
          {connectionLabel(github?.status)}
        </span>
      </div>
      {!githubActive ? (
        <button type="button" disabled={authorize.isPending} onClick={connect}>
          {authorize.isPending
            ? "GitHub로 이동하는 중…"
            : github ? "GitHub 다시 연결" : "GitHub 저장소 연결"}
        </button>
      ) : (
        <div className="connection-actions">
          <button type="button" disabled={authorize.isPending || disconnect.isPending} onClick={reconnect}>
            {authorize.isPending ? "GitHub로 이동하는 중…" : "권한 다시 승인"}
          </button>
          <button
            className="button-secondary"
            type="button"
            disabled={authorize.isPending || disconnect.isPending}
            onClick={disconnectConnection}
          >
            {disconnect.isPending ? "연결 해제 중…" : "연결 해제"}
          </button>
        </div>
      )}
      {authorize.isError ? <p role="alert">GitHub 연결을 시작하지 못했습니다. 서버 설정을 확인해 주세요.</p> : null}
      {disconnect.isError ? <p role="alert">GitHub 연결을 해제하지 못했습니다. 잠시 후 다시 시도해 주세요.</p> : null}
      {importRepository.isSuccess ? (
        <p role="status">
          <strong>{importRepository.data.fullName}</strong> 저장소를 DevPath에 추가했습니다.{" "}
          <Link to={`/repositories/${importRepository.data.repositoryId}`}>상세 보기</Link>
        </p>
      ) : null}
      {importRepository.isError ? (
        <p role="alert">{rateLimitMessage(importRepository.error) ?? "저장소를 추가하지 못했습니다. 접근 권한을 확인해 주세요."}</p>
      ) : null}
      {githubActive ? (
        <RepositoryList
          repositories={repositories}
          importRepository={importRepository}
          refreshConnection={() => connections.refetch()}
        />
      ) : null}
    </section>
    <NotionConnectionPanel connections={connections} />
    </>
  );
}

function NotionConnectionPanel({ connections }: { connections: ReturnType<typeof useConnections> }) {
  const authorize = useAuthorizeNotion();
  const disconnect = useDisconnectNotion();
  const notion = connections.data?.connections.find(connection => connection.provider === "NOTION");
  const active = notion?.status === "ACTIVE";
  const workspaces = useNotionWorkspaces(active);
  const callbackResult = new URLSearchParams(window.location.search).get("notionConnection");

  function connect() {
    authorize.mutate(undefined, { onSuccess: result => window.location.assign(result.authorizationUrl) });
  }

  function disconnectConnection() {
    if (window.confirm("Notion 연결을 해제할까요? 저장된 페이지 메타데이터와 토큰이 더 이상 갱신되지 않습니다.")) {
      disconnect.mutate();
    }
  }

  return (
    <section id="notion" className="connection-panel" aria-labelledby="notion-connection-title">
      <h3 id="notion-connection-title">Notion 워크스페이스</h3>
      <p>연결에 공유된 페이지의 제목과 수정 시각만 탐색합니다. 페이지 본문은 저장하지 않습니다.</p>
      {callbackResult === "failed" ? <p role="alert">Notion 연결에 실패했습니다. 권한과 서버 설정을 확인해 주세요.</p> : null}
      {callbackResult === "success" ? <p role="status">Notion 워크스페이스 연결을 완료했습니다.</p> : null}
      <div className="connection-status">
        <div>
          <strong>Notion 읽기 접근</strong>
          <p>{notionDescription(notion?.status)}</p>
        </div>
        <span className={active ? "status-badge status-badge--active" : "status-badge"}>{connectionLabel(notion?.status)}</span>
      </div>
      {!active ? (
        <button type="button" disabled={authorize.isPending} onClick={connect}>
          {authorize.isPending ? "Notion으로 이동하는 중…" : notion ? "Notion 다시 연결" : "Notion 연결"}
        </button>
      ) : (
        <div className="connection-actions">
          <button type="button" disabled={authorize.isPending || disconnect.isPending} onClick={connect}>
            {authorize.isPending ? "Notion으로 이동하는 중…" : "Notion 권한 다시 승인"}
          </button>
          <button className="button-secondary" type="button" disabled={authorize.isPending || disconnect.isPending} onClick={disconnectConnection}>
            {disconnect.isPending ? "Notion 연결 해제 중…" : "Notion 연결 해제"}
          </button>
        </div>
      )}
      {authorize.isError ? <p role="alert">Notion 연결을 시작하지 못했습니다. 서버 설정을 확인해 주세요.</p> : null}
      {disconnect.isError ? <p role="alert">Notion 연결을 해제하지 못했습니다. 잠시 후 다시 시도해 주세요.</p> : null}
      {active ? <NotionWorkspaceList workspaces={workspaces} /> : null}
    </section>
  );
}

function NotionWorkspaceList({ workspaces }: { workspaces: ReturnType<typeof useNotionWorkspaces> }) {
  if (workspaces.isPending) return <p role="status">공유된 Notion 페이지 메타데이터를 확인하는 중입니다…</p>;
  if (workspaces.isError) {
    const limited = rateLimitMessage(workspaces.error, "Notion");
    return <div role="alert"><p>{limited ?? "Notion 페이지 목록을 불러오지 못했습니다. 연결 권한을 다시 확인해 주세요."}</p><button type="button" onClick={() => workspaces.refetch()}>Notion 다시 확인</button></div>;
  }
  if (workspaces.data.workspaces.length === 0) return <p>연결된 Notion 워크스페이스가 없습니다.</p>;
  return <div className="repository-list notion-page-list">
    {workspaces.data.workspaces.map(workspace => <div key={workspace.workspaceId}>
      <div className="section-heading"><div><h4>{workspace.workspaceName}</h4><p>공유된 활성 항목 {activeNotionPages(workspace.pages).length}개 · 마지막 확인 {new Date(workspace.discoveredAt).toLocaleString("ko-KR")}</p></div>
        <button className="button-secondary" type="button" disabled={workspaces.isFetching} onClick={() => workspaces.refetch()}>
          {workspaces.isFetching ? "새로 고침 중…" : "페이지 목록 새로 고침"}
        </button>
      </div>
      {activeNotionPages(workspace.pages).length === 0 ? <p>이 연결에 공유된 활성 페이지가 없습니다. Notion에서 페이지를 연결에 공유한 뒤 다시 확인하세요.</p> : <ul>
        {activeNotionPages(workspace.pages).map(page => <li key={page.providerPageId}>
          <div>
            {safeNotionUrl(page.url) ? <a href={safeNotionUrl(page.url)!} target="_blank" rel="noreferrer">{page.title}</a> : <strong>{page.title}</strong>}
            <span>{page.objectType === "PAGE" ? "페이지" : "데이터 소스"} · {new Date(page.lastEditedAt).toLocaleString("ko-KR")}</span>
          </div>
        </li>)}
      </ul>}
    </div>)}
  </div>;
}

function activeNotionPages(pages: import("../api/connectionApi").NotionWorkspacePage[]) {
  return pages.filter(page => !page.inTrash);
}

function safeNotionUrl(value: string | null) {
  if (!value) return null;
  try {
    const url = new URL(value);
    const notionHost = url.hostname === "notion.so" || url.hostname.endsWith(".notion.so")
      || url.hostname === "notion.site" || url.hostname.endsWith(".notion.site");
    return url.protocol === "https:" && notionHost ? url.toString() : null;
  } catch {
    return null;
  }
}

function RepositoryList({
  repositories,
  importRepository,
  refreshConnection
}: {
  repositories: ReturnType<typeof useGitHubRepositories>;
  importRepository: ReturnType<typeof useImportRepository>;
  refreshConnection: () => void;
}) {
  if (repositories.isPending) {
    return <p role="status">GitHub 저장소를 불러오는 중입니다…</p>;
  }
  if (repositories.isError) {
    const limited = rateLimitMessage(repositories.error);
    if (limited) {
      return <div role="alert"><p>{limited}</p><button type="button" onClick={() => repositories.refetch()}>GitHub 저장소 다시 확인</button></div>;
    }
    return <div role="alert"><p>GitHub 저장소를 불러오지 못했습니다. 권한이 만료되거나 해제되었을 수 있습니다.</p><button type="button" onClick={refreshConnection}>연결 상태 확인</button></div>;
  }
  if (repositories.data.repositories.length === 0) {
    return <p>GitHub App에 허용된 저장소가 없습니다.</p>;
  }
  return (
    <div className="repository-list">
      <div className="section-heading">
        <div>
          <h4>접근 가능한 저장소</h4>
          <p>분석할 저장소를 DevPath 작업 공간에 먼저 추가하세요.</p>
        </div>
        <Link to="/repositories">내 저장소 보기</Link>
      </div>
      <ul>
        {repositories.data.repositories.map(repository => (
          <li key={repository.providerRepositoryId}>
            <div>
              <a href={repository.htmlUrl} target="_blank" rel="noreferrer">{repository.fullName}</a>
              <span>{repository.privateRepository ? "비공개" : "공개"}</span>
            </div>
            <button
              type="button"
              disabled={importRepository.isPending}
              onClick={() => importRepository.mutate(repository.providerRepositoryId)}
            >
              {importRepository.isPending && importRepository.variables === repository.providerRepositoryId
                ? "추가 중…"
                : "DevPath에 추가"}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

function connectionLabel(status: "ACTIVE" | "EXPIRED" | "REVOKED" | undefined) {
  if (status === "ACTIVE") return "연결됨";
  if (status === "EXPIRED") return "만료됨";
  if (status === "REVOKED") return "권한 해제됨";
  return "미연결";
}

function connectionDescription(status: "ACTIVE" | "EXPIRED" | "REVOKED" | undefined) {
  if (status === "ACTIVE") return "저장소를 불러올 수 있도록 안전하게 연결되어 있습니다.";
  if (status === "EXPIRED") return "GitHub 접근 권한이 만료되었습니다. 다시 연결하면 저장소 접근을 복구할 수 있습니다.";
  if (status === "REVOKED") return "GitHub 접근 권한이 해제되었습니다. 다시 연결하기 전에는 저장소에 접근하지 않습니다.";
  return "GitHub 로그인은 완료되었지만 저장소 접근 권한은 아직 연결되지 않았습니다.";
}

function notionDescription(status: "ACTIVE" | "EXPIRED" | "REVOKED" | undefined) {
  if (status === "ACTIVE") return "공유된 페이지 메타데이터를 서버에서 읽을 수 있습니다.";
  if (status === "EXPIRED") return "Notion 접근 권한이 만료되었습니다. 다시 연결해 주세요.";
  if (status === "REVOKED") return "Notion 접근 권한이 해제되었습니다.";
  return "아직 Notion 워크스페이스를 연결하지 않았습니다.";
}
