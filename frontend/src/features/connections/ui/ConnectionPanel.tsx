import {
  useAuthorizeGitHub,
  useConnections,
  useDisconnectGitHub,
  useGitHubRepositories
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
  );
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
