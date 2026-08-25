import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
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
import { ApiError } from "../shared/api/apiClient";
import { syncLabel } from "./RepositoriesPage";
import { useAnalysisJob, useRequestAnalysis } from "../features/analysis/model/useAnalysis";
import { currentSkillMatrixKey } from "../features/skills/model/useSkillMatrix";

export function RepositoryDetailPage() {
  const { repositoryId } = useParams();
  const query = useRepository(repositoryId);
  const queryClient = useQueryClient();
  const archive = useArchiveRepository();
  const restore = useRestoreRepository();
  const synchronize = useSynchronizeRepository();
  const requestAnalysis = useRequestAnalysis();
  const [jobId, setJobId] = useState<string | null>(null);
  const [analysisJobId, setAnalysisJobId] = useState<string | null>(null);
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
  const lifecycleMutation = repository.lifecycle === "ARCHIVED" ? restore : archive;
  const isProviderArchived = repository.providerArchived;
  const actionDisabled = lifecycleMutation.isPending || (repository.lifecycle === "ARCHIVED" && isProviderArchived);
  const actionLabel = repository.lifecycle === "ARCHIVED" ? "저장소 복원" : "저장소 보관";
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
          {repository.lifecycle !== "ARCHIVED" ? (
            <button
              type="button"
              disabled={synchronize.isPending || job.data?.status === "queued" || job.data?.status === "running"}
              onClick={() => repositoryId && synchronize.mutate(repositoryId, {
                onSuccess: value => setJobId(value.jobId)
              })}
            >
              {synchronize.isPending ? "동기화 요청 중…" : "GitHub 동기화"}
            </button>
          ) : null}
          <button
            type="button"
            className={repository.lifecycle === "ARCHIVED" ? undefined : "button-danger"}
            disabled={actionDisabled}
            onClick={() => repositoryId && lifecycleMutation.mutate(repositoryId)}
          >
            {lifecycleMutation.isPending ? "처리 중…" : actionLabel}
          </button>
          <a className="button-link" href={repository.htmlUrl} target="_blank" rel="noreferrer">GitHub에서 보기</a>
        </div>
      </header>

      {job.data ? (
        <section className="sync-panel" aria-live="polite">
          <div>
            <strong>{jobLabel(job.data.status, job.data.phase)}</strong>
            <span>시도 {job.data.attemptCount}/{job.data.maxAttempts}</span>
          </div>
          <progress max="100" value={job.data.progressPercent}>{job.data.progressPercent}%</progress>
          {job.data.status === "failed" ? (
            <p role="alert">동기화에 실패했습니다. GitHub 연결과 저장소 권한을 확인한 뒤 다시 요청해 주세요.</p>
          ) : null}
        </section>
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
            <p>분석이 완료되었습니다. <Link to="/skills">스킬 분석 결과 보기</Link> · <Link to="/analyses">분석 이력 보기</Link></p>
          ) : null}
          {analysisJob.data.status === "failed" ? (
            <p role="alert">결정론적 분석에 실패했습니다. 잠시 후 다시 요청해 주세요.</p>
          ) : null}
        </section>
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
            disabled={repository.syncStatus !== "SYNCHRONIZED" || requestAnalysis.isPending
              || analysisJob.data?.status === "queued" || analysisJob.data?.status === "running"}
            onClick={() => repositoryId && requestAnalysis.mutate(repositoryId, {
              onSuccess: value => setAnalysisJobId(value.jobId)
            })}
          >
            {requestAnalysis.isPending ? "분석 요청 중…" : "결정론적 분석 시작"}
          </button>
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
            </dl>
          </article>
        ))}
      </section>
    </main>
  );
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

function evidenceCategoryLabel(category: "ARCHITECTURE" | "DATABASE" | "TESTING" | "DEVOPS" | "DOCUMENTATION" | "ACTIVITY") {
  return { ARCHITECTURE: "아키텍처", DATABASE: "데이터베이스 근거", TESTING: "테스트", DEVOPS: "DevOps", DOCUMENTATION: "문서화", ACTIVITY: "활동" }[category];
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

function syncRequestError(error: Error) {
  if (!(error instanceof ApiError)) return "브라우저에서 동기화 요청을 만들지 못했습니다. 페이지를 새로고침해 주세요.";
  if (error.status === 401) return "로그인 세션이 만료되었습니다. 다시 로그인해 주세요.";
  if (error.status === 403) return "보안 토큰이 만료되었습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.";
  if (error.status === 404) return "현재 계정에서 이 저장소를 찾을 수 없습니다.";
  if (error.status === 400) return "보관되었거나 동기화할 수 없는 저장소입니다.";
  if (error.status === 503) return "GitHub 연결 또는 저장소 권한을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.";
  return `동기화를 요청하지 못했습니다. 서버 응답 코드: ${error.status}`;
}
