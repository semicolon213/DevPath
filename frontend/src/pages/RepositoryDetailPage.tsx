import { useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import {
  useArchiveRepository,
  useRepository,
  useRepositorySnapshots,
  useRepositoryTechnologies,
  useRepositoryEvidence,
  useRepositorySyncJob,
  useSynchronizeRepository,
  useRestoreRepository
} from "../features/repositories/model/useRepositories";
import { repositoriesKey } from "../features/repositories/model/useRepositories";
import { ApiError, rateLimitMessage } from "../shared/api/apiClient";
import { syncLabel } from "./RepositoriesPage";
import { useAnalysisJob, useRequestAnalysis } from "../features/analysis/model/useAnalysis";
import { currentSkillMatrixKey } from "../features/skills/model/useSkillMatrix";

export function RepositoryDetailPage() {
  const { repositoryId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const [confirmingArchive, setConfirmingArchive] = useState(false);
  const query = useRepository(repositoryId);
  const queryClient = useQueryClient();
  const archive = useArchiveRepository();
  const restore = useRestoreRepository();
  const synchronize = useSynchronizeRepository();
  const requestAnalysis = useRequestAnalysis();
  const jobId = searchParams.get("syncJobId");
  const analysisJobId = searchParams.get("analysisJobId");
  const job = useRepositorySyncJob(jobId);
  const analysisJob = useAnalysisJob(analysisJobId);
  const snapshots = useRepositorySnapshots(repositoryId);
  const technologies = useRepositoryTechnologies(repositoryId, query.data?.syncStatus === "SYNCHRONIZED");
  const evidence = useRepositoryEvidence(repositoryId, query.data?.syncStatus === "SYNCHRONIZED");

  useEffect(() => {
    if (job.data?.status === "succeeded") {
      queryClient.invalidateQueries({ queryKey: repositoriesKey });
    }
  }, [job.data?.status, queryClient]);

  useEffect(() => {
    if (analysisJob.data?.status === "succeeded") {
      queryClient.invalidateQueries({ queryKey: currentSkillMatrixKey });
    }
  }, [analysisJob.data?.status, queryClient]);

  if (query.isPending) {
    return <main className="shell workspace"><p role="status">저장소 상세 정보를 불러오는 중입니다…</p></main>;
  }
  if (query.isError) {
    return (
      <main className="shell workspace">
        <div className="state-panel" role="alert">
          <h1>저장소를 표시할 수 없습니다</h1>
          <p>저장소가 없거나 현재 계정으로 접근할 수 없습니다.</p>
          <Link to="/repositories">내 저장소로 돌아가기</Link>
        </div>
      </main>
    );
  }

  const repository = query.data;
  const activityTimeline = evidence.data?.activityTimeline;
  const lifecycleMutation = repository.lifecycle === "ARCHIVED" ? restore : archive;
  const isProviderArchived = repository.providerArchived;
  const actionDisabled = lifecycleMutation.isPending || (repository.lifecycle === "ARCHIVED" && isProviderArchived);
  const actionLabel = repository.lifecycle === "ARCHIVED" ? "저장소 복원" : "저장소 보관";
  const repositoryUnavailable = repository.lifecycle === "DELETED_EXTERNALLY";
  const repositoryArchived = repository.lifecycle === "ARCHIVED";
  const changeLifecycle = () => repositoryId && lifecycleMutation.mutate(repositoryId, { onSuccess: () => {
    setConfirmingArchive(false);
    setSearchParams(current => {
      const next = new URLSearchParams(current);
      next.delete("syncJobId");
      next.delete("analysisJobId");
      return next;
    }, { replace: true });
  } });
  return (
    <main className="shell workspace">
      <nav><Link to="/repositories">← 내 저장소</Link></nav>
      <header className="repository-detail-header">
        <div>
          <p className="eyebrow">GitHub repository</p>
          <h1>{repository.fullName}</h1>
          <p>GitHub 동기화 상태와 재현 가능한 저장소 스냅샷을 관리합니다.</p>
        </div>
        <div className="repository-detail-actions">
          {!repositoryArchived && !repositoryUnavailable ? (
            <button
              type="button"
              disabled={synchronize.isPending || job.data?.status === "queued" || job.data?.status === "running"}
              onClick={() => repositoryId && synchronize.mutate(repositoryId, {
                onSuccess: value => setSearchParams(current => {
                  const next = new URLSearchParams(current);
                  next.set("syncJobId", value.jobId);
                  return next;
                }, { replace: true })
              })}
            >
              {synchronize.isPending ? "동기화 요청 중…" : "GitHub 동기화"}
            </button>
          ) : null}
          {!repositoryUnavailable ? <button
              type="button"
              className={repositoryArchived ? undefined : "button-danger"}
              disabled={actionDisabled}
              onClick={() => repositoryArchived ? changeLifecycle() : setConfirmingArchive(true)}
            >
              {lifecycleMutation.isPending ? "처리 중…" : actionLabel}
            </button> : null}
          <a className="button-link" href={repository.htmlUrl} target="_blank" rel="noreferrer">GitHub에서 보기</a>
        </div>
      </header>

      {confirmingArchive ? (
        <section className="lifecycle-confirmation lifecycle-confirmation--detail" aria-labelledby="archive-confirmation-title">
          <h2 id="archive-confirmation-title">이 저장소를 DevPath에서 보관할까요?</h2>
          <p>활성 목록에서 숨겨지고 새 동기화와 분석을 시작할 수 없습니다. 기존 메타데이터, 불변 스냅샷, 공식 분석 결과는 삭제되지 않습니다.</p>
          <div>
            <button type="button" className="button-danger" disabled={archive.isPending} onClick={changeLifecycle}>
              {archive.isPending ? "보관 중…" : "저장소 보관 확인"}
            </button>
            <button type="button" disabled={archive.isPending} onClick={() => setConfirmingArchive(false)}>취소</button>
          </div>
        </section>
      ) : null}

      {job.data ? (
        <section className="sync-panel" aria-live="polite">
          <div>
            <strong>{jobLabel(job.data.status, job.data.phase)}</strong>
            <span>시도 {job.data.attemptCount}/{job.data.maxAttempts}</span>
          </div>
          <progress max="100" value={job.data.progressPercent}>{job.data.progressPercent}%</progress>
          {job.data.status === "failed" && job.data.errorCode === "COLLECTION_LIMIT_EXCEEDED" ? (
            <p role="alert">
              저장소가 현재 안전 수집 범위를 초과했습니다. 부분 스냅샷은 생성되지 않았고 서버는 자동으로 재시도하지 않습니다.
              브랜치·파일·PR·이슈 규모를 줄인 뒤 새 동기화를 요청해 주세요.
            </p>
          ) : job.data.status === "failed" ? (
            <p role="alert">동기화에 실패했습니다. GitHub 연결과 저장소 권한을 확인한 뒤 다시 요청해 주세요.</p>
          ) : null}
          {job.data.phase === "RETRY_WAIT" && job.data.errorCode === "RATE_LIMIT_EXCEEDED" ? (
            <p role="status">GitHub 요청 한도가 해제되면 서버가 자동으로 동기화를 다시 시작합니다.</p>
          ) : null}
          {job.data.status === "succeeded" && snapshotRouteFromResult(repositoryId, job.data.resultResourceUrl) ? (
            <p><Link to={snapshotRouteFromResult(repositoryId, job.data.resultResourceUrl)!}>생성된 불변 스냅샷 보기</Link></p>
          ) : null}
        </section>
      ) : job.isError ? (
        <section className="sync-panel" role="alert"><strong>동기화 작업 상태를 불러오지 못했습니다.</strong>
          <p>작업이 없거나 현재 계정으로 접근할 수 없습니다. 저장소 데이터는 변경되지 않았습니다.</p>
          <button type="button" onClick={() => setSearchParams(current => {
            const next = new URLSearchParams(current); next.delete("syncJobId"); return next;
          }, { replace: true })}>작업 표시 닫기</button></section>
      ) : synchronize.isError ? (
        <p role="alert" className="form-error">{syncRequestError(synchronize.error)}</p>
      ) : null}

      {analysisJob.data ? (
        <section className="sync-panel analysis-panel" aria-live="polite">
          <div>
            <strong>{analysisJobLabel(analysisJob.data.status, analysisJob.data.phase)}</strong>
            <span>시도 {analysisJob.data.attemptCount}/{analysisJob.data.maxAttempts}</span>
          </div>
          <progress max="100" value={analysisJob.data.progressPercent}>{analysisJob.data.progressPercent}%</progress>
          {analysisJob.data.status === "succeeded" ? (
            <p>분석이 완료되었습니다. {analysisRouteFromResult(analysisJob.data.resultResourceUrl) ? <><Link to={analysisRouteFromResult(analysisJob.data.resultResourceUrl)!}>완료된 공식 분석 보기</Link> · </> : null}<Link to="/skills">스킬 분석 결과 보기</Link> · <Link to="/analyses">분석 이력 보기</Link></p>
          ) : null}
          {analysisJob.data.status === "failed" ? (
            <p role="alert">결정론적 분석에 실패했습니다. 잠시 후 다시 요청해 주세요.</p>
          ) : null}
        </section>
      ) : analysisJob.isError ? (
        <section className="sync-panel analysis-panel" role="alert"><strong>분석 작업 상태를 불러오지 못했습니다.</strong>
          <p>작업이 없거나 현재 계정으로 접근할 수 없습니다. 공식 분석 결과는 변경되지 않았습니다.</p>
          <button type="button" onClick={() => setSearchParams(current => {
            const next = new URLSearchParams(current); next.delete("analysisJobId"); return next;
          }, { replace: true })}>작업 표시 닫기</button></section>
      ) : requestAnalysis.isError ? (
        <p role="alert" className="form-error">분석 요청을 만들지 못했습니다. 동기화 상태를 확인한 뒤 다시 시도해 주세요.</p>
      ) : null}

      <section className="detail-grid" aria-label="저장소 상세 정보">
        <div className="detail-card">
          <h2>저장소 정보</h2>
          <dl className="metadata-list metadata-list--large">
            <div><dt>소유자</dt><dd>{repository.owner}</dd></div>
            <div><dt>공개 범위</dt><dd>{repository.visibility === "PRIVATE" ? "비공개" : "공개"}</dd></div>
            <div><dt>기본 브랜치</dt><dd>{repository.defaultBranch}</dd></div>
            <div><dt>DevPath 상태</dt><dd>{repository.lifecycle === "ARCHIVED" ? "보관됨" : "사용 가능"}</dd></div>
            <div><dt>GitHub 상태</dt><dd>{repository.providerArchived ? "GitHub에서 보관됨" : "사용 가능"}</dd></div>
          </dl>
          {isProviderArchived && repository.lifecycle === "ARCHIVED" ? (
            <p className="muted">GitHub에서 보관된 저장소는 GitHub에서 먼저 활성화해야 복원할 수 있습니다.</p>
          ) : null}
          {lifecycleMutation.isError ? (
            <p role="alert" className="form-error">저장소 상태를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.</p>
          ) : null}
        </div>
        <div className="detail-card detail-card--accent">
          <h2>분석 준비 상태</h2>
          <p className="large-status">{syncLabel(repository.syncStatus)}</p>
          <p>
            GitHub 기본 브랜치·브랜치 목록·커밋 이력은 백그라운드 작업으로 수집됩니다.
            공식 점수와 기술 분석은 이후 결정론적 Rule Engine 단계에서 생성됩니다.
          </p>
          <p className="muted">공식 결과는 현재 불변 스냅샷과 버전이 고정된 Rule Engine으로만 계산됩니다.</p>
          <button
            type="button"
            disabled={repositoryArchived || repositoryUnavailable || repository.syncStatus !== "SYNCHRONIZED" || requestAnalysis.isPending
              || analysisJob.data?.status === "queued" || analysisJob.data?.status === "running"}
            onClick={() => repositoryId && requestAnalysis.mutate(repositoryId, {
              onSuccess: value => setSearchParams(current => {
                const next = new URLSearchParams(current);
                next.set("analysisJobId", value.jobId);
                return next;
              }, { replace: true })
            })}
          >
            {requestAnalysis.isPending ? "분석 요청 중…" : "결정론적 분석 시작"}
          </button>
          {repositoryArchived ? <p className="muted">보관된 저장소는 새 동기화와 분석을 시작할 수 없습니다. 기존 스냅샷과 분석 결과는 계속 조회할 수 있습니다.</p> : null}
          {repositoryUnavailable ? <p className="muted">GitHub에서 삭제된 저장소는 새 작업을 시작할 수 없습니다. 보존된 과거 결과만 조회할 수 있습니다.</p> : null}
        </div>
      </section>

      <section className="snapshot-section" aria-labelledby="technology-summary-title">
        <div className="section-heading">
          <div>
            <h2 id="technology-summary-title">감지된 기술 스택</h2>
            <p>GitHub 언어 통계를 현재 불변 스냅샷에 연결해 결정론적으로 정규화한 결과입니다.</p>
          </div>
          {technologies.data?.primaryLanguage ? (
            <span className="status-badge status-badge--active">주 언어 {technologies.data.primaryLanguage}</span>
          ) : null}
        </div>
        {repository.syncStatus !== "SYNCHRONIZED" ? (
          <p className="muted">저장소를 동기화하면 언어 통계가 표시됩니다.</p>
        ) : null}
        {technologies.isPending && repository.syncStatus === "SYNCHRONIZED" ? (
          <p role="status">기술 스택을 불러오는 중입니다.</p>
        ) : null}
        {technologies.isError ? (
          <p role="alert">현재 스냅샷에는 언어 통계가 없습니다. 다시 동기화해 주세요.</p>
        ) : null}
        {technologies.data?.technologies.length === 0 ? (
          <p className="muted">GitHub에서 감지한 프로그래밍 언어가 없습니다.</p>
        ) : null}
        <div className="technology-list">
          {technologies.data?.technologies.map(technology => (
            <article className="technology-card" key={`${technology.category}:${technology.name}:${technology.evidenceLabel}`}>
              <div>
                <strong>{technology.name}</strong>
                <span>{technologyCategoryLabel(technology.category)}</span>
              </div>
              {technology.percentage !== null ? (
                <progress max="100" value={technology.percentage}>{technology.percentage}%</progress>
              ) : null}
              <small>
                {technology.byteCount !== null
                  ? `${technology.byteCount.toLocaleString("ko-KR")}바이트 · ${technology.percentage?.toFixed(1)}%`
                  : `의존성 ${technology.evidenceLabel}`}
              </small>
              {technology.evidencePaths.length > 0 ? <small>증거: {technology.evidencePaths.join(", ")}</small> : null}
            </article>
          ))}
        </div>
        {technologies.data ? (
          <p className="muted">추출기 {technologies.data.extractorVersion} · 분류표 {technologies.data.taxonomyVersion}</p>
        ) : null}
      </section>

      <section className="snapshot-section" aria-labelledby="engineering-evidence-title">
        <div className="section-heading">
          <div>
            <h2 id="engineering-evidence-title">엔지니어링 증거</h2>
            <p>파일 구조와 동기화 메타데이터에서 추출한 측정 가능한 신호입니다. 아직 공식 점수는 아닙니다.</p>
          </div>
        </div>
        {repository.syncStatus !== "SYNCHRONIZED" ? <p className="muted">저장소를 동기화하면 증거가 표시됩니다.</p> : null}
        {evidence.isPending && repository.syncStatus === "SYNCHRONIZED" ? <p role="status">엔지니어링 증거를 불러오는 중입니다.</p> : null}
        {evidence.isError ? <p role="alert">현재 스냅샷에는 파일 증거가 없습니다. 다시 동기화해 주세요.</p> : null}
        <div className="evidence-grid">
          {evidence.data?.categories.map(category => (
            <article className="evidence-card" key={category.category}>
              <h3>{evidenceCategoryLabel(category.category)}</h3>
              <ul>
                {category.signals.map(signal => (
                  <li key={signal.signalKey}>
                    <div>
                      <strong>{evidenceSignalLabel(signal.signalKey)}</strong>
                      <span className={`status-badge ${signal.present ? "status-badge--active" : ""}`}>
                        {signal.present ? "감지됨" : "증거 없음"}
                      </span>
                    </div>
                    {signal.count > 0 ? <small>관측 {signal.count.toLocaleString("ko-KR")}건</small> : null}
                    {signal.observedValue ? <small>{signal.observedValue}</small> : null}
                    {signal.evidencePaths.length > 0 ? <small>증거: {signal.evidencePaths.join(", ")}</small> : null}
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
        {evidence.data ? <p className="muted">추출기 {evidence.data.extractorVersion}</p> : null}
      </section>

      <section className="snapshot-section" aria-labelledby="repository-activity-title">
        <div className="section-heading">
          <div>
            <h2 id="repository-activity-title">저장소 활동 타임라인</h2>
            <p>현재 불변 스냅샷에 수집된 커밋, PR, 이슈 활동을 최신순으로 보여줍니다.</p>
          </div>
        </div>
        {repository.syncStatus !== "SYNCHRONIZED" ? <p className="muted">저장소를 동기화하면 활동 기록이 표시됩니다.</p> : null}
        {evidence.data && !activityTimeline ? <p className="muted">활동 타임라인을 불러올 수 없습니다.</p> : null}
        {activityTimeline?.events.length === 0 ? <p className="muted">현재 스냅샷에 수집된 활동이 없습니다.</p> : null}
        {activityTimeline?.latestActivityAt ? (
          <p className="muted">
            마지막 활동은 스냅샷 수집 시점 기준 {activityTimeline.daysSinceLatestActivity?.toLocaleString("ko-KR")}일 전입니다.
            정책 임계값을 적용한 오래됨 판정은 포함하지 않습니다.
          </p>
        ) : null}
        {activityTimeline?.events.length ? (
          <ol className="activity-timeline">
            {activityTimeline.events.map((event, index) => (
              <li key={`${event.eventType}:${event.sourceReference}:${event.occurredAt}:${index}`}>
                <div>
                  <strong>{activityEventLabel(event.eventType)}</strong>
                  <time dateTime={event.occurredAt}>
                    {new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(event.occurredAt))}
                  </time>
                </div>
                <code>{event.sourceReference.slice(0, 16)}</code>
              </li>
            ))}
          </ol>
        ) : null}
        {activityTimeline?.truncated ? (
          <p className="muted">전체 {activityTimeline.totalEventCount.toLocaleString("ko-KR")}건 중 최신 100건을 표시합니다.</p>
        ) : null}
        {activityTimeline ? <p className="muted">추출기 {activityTimeline.extractorVersion}</p> : null}
      </section>

      <section className="snapshot-section" aria-labelledby="snapshot-history-title">
        <div className="section-heading">
          <div>
            <h2 id="snapshot-history-title">동기화 스냅샷</h2>
            <p>완료된 동기화마다 새로운 불변 스냅샷이 생성됩니다.</p>
          </div>
        </div>
        {snapshots.isPending ? <p role="status">스냅샷을 불러오는 중입니다…</p> : null}
        {snapshots.isError ? <p role="alert">스냅샷 기록을 불러오지 못했습니다.</p> : null}
        {snapshots.data?.length === 0 ? <p className="muted">아직 완료된 동기화가 없습니다.</p> : null}
        {snapshots.data?.map(snapshot => (
          <article className="snapshot-card" key={snapshot.snapshotId}>
            <div>
              <strong>{new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(snapshot.capturedAt))}</strong>
              <span className="status-badge status-badge--active">불변 · 준비 완료</span>
            </div>
            <dl className="metadata-list">
              <div><dt>소스 리비전</dt><dd><code>{snapshot.sourceRevision.slice(0, 12)}</code></dd></div>
              <div><dt>브랜치</dt><dd>{snapshot.branchCount}개</dd></div>
              <div><dt>커밋</dt><dd>{snapshot.commitCount}개</dd></div>
              <div><dt>PR</dt><dd>{snapshot.pullRequestCount}개</dd></div>
              <div><dt>이슈</dt><dd>{snapshot.issueCount}개</dd></div>
              <div><dt>문서</dt><dd>{snapshot.documentCount}개</dd></div>
            </dl>
            <Link to={`/repositories/${repository.repositoryId}/snapshots/${snapshot.snapshotId}`}>스냅샷 상세 보기</Link>
          </article>
        ))}
      </section>
    </main>
  );
}

function snapshotRouteFromResult(repositoryId: string | undefined, resultResourceUrl: string | null) {
  if (!repositoryId || !resultResourceUrl) return null;
  const match = resultResourceUrl.match(/^\/api\/v1\/repositories\/([^/]+)\/snapshots\/([^/?#]+)$/);
  if (!match || match[1] !== repositoryId) return null;
  return `/repositories/${repositoryId}/snapshots/${match[2]}`;
}

function analysisRouteFromResult(resultResourceUrl: string | null) {
  if (!resultResourceUrl) return null;
  const match = /^\/api\/v1\/analyses\/([^/?#]+)$/.exec(resultResourceUrl);
  return match ? `/analyses/${match[1]}` : null;
}

function jobLabel(status: string, phase: string) {
  if (status === "succeeded") return "동기화 완료";
  if (status === "failed") return "동기화 실패";
  if (phase === "RETRY_WAIT") return "재시도 대기 중";
  if (status === "running") return "GitHub 데이터 수집 중";
  return "동기화 대기 중";
}

function analysisJobLabel(status: string, phase: string) {
  if (status === "succeeded") return "결정론적 분석 완료";
  if (status === "failed") return "결정론적 분석 실패";
  if (phase === "RETRY_WAIT") return "분석 재시도 대기 중";
  if (status === "running") return "규칙 평가와 스킬 매트릭스 생성 중";
  return "분석 대기 중";
}

function technologyCategoryLabel(category: "LANGUAGE" | "FRAMEWORK" | "DATABASE") {
  if (category === "FRAMEWORK") return "프레임워크";
  if (category === "DATABASE") return "데이터베이스";
  return "언어";
}

function evidenceCategoryLabel(category: "ARCHITECTURE" | "DATABASE" | "TESTING" | "DEVOPS" | "DOCUMENTATION" | "COLLABORATION" | "ACTIVITY") {
  return { ARCHITECTURE: "아키텍처", DATABASE: "데이터베이스 근거", TESTING: "테스트", DEVOPS: "DevOps", DOCUMENTATION: "문서화", COLLABORATION: "협업", ACTIVITY: "활동" }[category];
}

const evidenceLabels: Record<string, string> = {
  HEXAGONAL_BOUNDARIES: "헥사고날 경계",
  LAYERED_BOUNDARIES: "계층형 경계",
  MODULE_LAYOUT: "모듈 구성",
  DATABASE_TECHNOLOGIES: "감지된 데이터베이스 기술",
  DATA_ACCESS_DEPENDENCIES: "데이터 접근 의존성",
  DATABASE_MIGRATIONS: "데이터베이스 마이그레이션",
  PERSISTENCE_CONFIGURATION: "영속성 설정",
  TEST_FILES: "테스트 파일",
  TEST_FRAMEWORKS: "테스트 프레임워크",
  CI_WORKFLOW_METADATA: "CI 워크플로 메타데이터",
  CONTAINER_CONFIGURATION: "컨테이너 설정",
  CI_WORKFLOWS: "CI 워크플로",
  INFRASTRUCTURE_AS_CODE: "인프라 코드",
  DEPLOYMENT_CONFIGURATION: "배포 설정",
  README_PRESENT: "README",
  README_QUALITY_SECTIONS: "README 품질 섹션",
  PULL_REQUEST_COUNT: "수집된 PR",
  MERGED_PULL_REQUEST_COUNT: "병합된 PR",
  PULL_REQUEST_REVIEW_COUNT: "PR 리뷰",
  ISSUE_COUNT: "수집된 이슈",
  CLOSED_ISSUE_COUNT: "종료된 이슈",
  LABELLED_ISSUE_COUNT: "라벨이 있는 이슈",
  API_DOCUMENTATION: "API 문서",
  ARCHITECTURE_DOCUMENTATION: "아키텍처 문서",
  CONTRIBUTING_GUIDE: "기여 가이드",
  LICENSE_PRESENT: "라이선스",
  COMMIT_COUNT: "수집된 커밋",
  CONTRIBUTOR_COUNT: "수집된 기여자",
  BRANCH_COUNT: "수집된 브랜치"
};

function evidenceSignalLabel(signalKey: string) {
  return evidenceLabels[signalKey] ?? signalKey;
}

function activityEventLabel(eventType: "COMMIT" | "PULL_REQUEST_OPENED" | "PULL_REQUEST_CLOSED" | "PULL_REQUEST_MERGED" | "ISSUE_OPENED" | "ISSUE_CLOSED") {
  return {
    COMMIT: "커밋",
    PULL_REQUEST_OPENED: "PR 열림",
    PULL_REQUEST_CLOSED: "PR 종료",
    PULL_REQUEST_MERGED: "PR 병합",
    ISSUE_OPENED: "이슈 열림",
    ISSUE_CLOSED: "이슈 종료"
  }[eventType];
}

function syncRequestError(error: Error) {
  const limited = rateLimitMessage(error);
  if (limited) return limited;
  if (!(error instanceof ApiError)) return "브라우저에서 동기화 요청을 만들지 못했습니다. 페이지를 새로고침해 주세요.";
  if (error.status === 401) return "로그인 세션이 만료되었습니다. 다시 로그인해 주세요.";
  if (error.status === 403) return "보안 토큰이 만료되었습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.";
  if (error.status === 404) return "현재 계정에서 이 저장소를 찾을 수 없습니다.";
  if (error.status === 400) return "보관되었거나 동기화할 수 없는 저장소입니다.";
  if (error.status === 503) return "GitHub 연결 또는 저장소 권한을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.";
  return `동기화를 요청하지 못했습니다. 서버 응답 코드: ${error.status}`;
}
