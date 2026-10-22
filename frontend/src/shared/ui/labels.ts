import type { RuleCategory, ReadinessLevel } from "../../features/readiness/api/careerReadinessApi";
import type { Recommendation } from "../../features/recommendations/api/recommendationApi";
import type { DetectedTechnology, EvidenceCategory, ImportedRepository, RepositorySyncJob } from "../../features/repositories/api/repositoryApi";
import type { KnowledgeChunkSummary, KnowledgeDocument, KnowledgeIngestionJob, KnowledgeSearchResult } from "../../features/knowledge/api/knowledgeApi";
import type { RuleEvidence } from "../../features/analysis/api/analysisApi";
import type { SkillAssessment } from "../../features/skills/api/skillMatrixApi";

const unknownLabel = "알 수 없음";

const categoryLabels = {
  LANGUAGE: "언어", FRAMEWORK: "프레임워크", DATABASE: "데이터베이스", ARCHITECTURE: "아키텍처",
  TESTING: "테스트", DEVOPS: "DevOps", DOCUMENTATION: "문서화", ACTIVITY: "개발 활동",
  COLLABORATION: "협업"
} as const satisfies Record<RuleCategory | EvidenceCategory["category"], string>;

const levelLabels = {
  NONE: "관찰 없음", BEGINNER: "입문", DEVELOPING: "성장", COMPETENT: "충분", STRONG: "강점"
} as const satisfies Record<ReadinessLevel, string>;

const evidenceStatusLabels = {
  NONE: "근거 없음", INSUFFICIENT: "근거 부족", SUFFICIENT: "근거 충분"
} as const satisfies Record<NonNullable<SkillAssessment["evidenceStatus"]>, string>;

const priorityLabels = {
  CRITICAL: "긴급", HIGH: "높음", MEDIUM: "보통", LOW: "낮음"
} as const satisfies Record<Recommendation["priority"], string>;

const recommendationTypeLabels = {
  STUDY: "학습", PROJECT: "프로젝트", ARCHITECTURE: "아키텍처", PORTFOLIO: "포트폴리오"
} as const satisfies Record<Recommendation["type"], string>;

const recommendationStatusLabels = {
  PROPOSED: "제안됨", ACCEPTED: "수락됨", DISMISSED: "제외됨", COMPLETED: "완료"
} as const satisfies Record<Recommendation["status"], string>;

const jobStatusLabels = {
  queued: "대기 중", running: "진행 중", succeeded: "완료", failed: "실패", cancelled: "취소됨", expired: "만료됨"
} as const satisfies Record<RepositorySyncJob["status"] | KnowledgeIngestionJob["status"], string>;

const evidenceTypeLabels = {
  REPOSITORY_PATH: "저장소 파일", LANGUAGE_STATISTIC: "언어 통계", SNAPSHOT_SIGNAL: "스냅샷 지표",
  PROVIDER_LANGUAGE_STATISTICS: "저장소 언어 통계", DEPENDENCY_DECLARATION: "의존성 선언"
} as const satisfies Record<RuleEvidence["evidenceType"] | DetectedTechnology["evidenceType"], string>;

const freshnessLabels = { FRESH: "최신 인덱스" } as const satisfies Record<KnowledgeSearchResult["freshness"], string>;
const knowledgeSourceTypeLabels = { NOTION: "Notion" } as const satisfies Record<KnowledgeDocument["sourceType"], string>;
const chunkStatusLabels = { INDEXED: "색인됨", STALE: "갱신 필요", DELETED: "삭제됨" } as const satisfies Record<KnowledgeChunkSummary["status"], string>;
const repositoryLifecycleLabels = {
  DISCOVERED: "등록됨", ACTIVE: "사용 중", ARCHIVED: "보관됨", DELETED_EXTERNALLY: "GitHub에서 삭제됨"
} as const satisfies Record<ImportedRepository["lifecycle"], string>;
const repositorySyncStatusLabels = {
  NOT_SYNCED: "동기화 전", SYNCHRONIZED: "동기화 완료", FAILED: "동기화 실패"
} as const satisfies Record<ImportedRepository["syncStatus"], string>;

const skillNames: Record<string, string> = {
  "language-engineering": "언어 활용", "framework-application": "프레임워크 적용",
  "database-engineering": "데이터베이스 엔지니어링", "architecture-structure": "아키텍처 구조",
  "testing-discipline": "테스트 역량", "delivery-operations": "전달 및 운영",
  "technical-documentation": "기술 문서화", "development-activity": "개발 활동"
};

function label(labels: Record<string, string>, value: string | null | undefined) {
  return value ? labels[value] ?? unknownLabel : unknownLabel;
}

export const categoryLabel = (value: string | null | undefined) => label(categoryLabels, value);
export const levelLabel = (value: string | null | undefined) => label(levelLabels, value);
export const evidenceStatusLabel = (value: string | null | undefined) => label(evidenceStatusLabels, value);
export const priorityLabel = (value: string | null | undefined) => label(priorityLabels, value);
export const recommendationTypeLabel = (value: string | null | undefined) => label(recommendationTypeLabels, value);
export const recommendationStatusLabel = (value: string | null | undefined) => label(recommendationStatusLabels, value);
export const jobStatusLabel = (value: string | null | undefined) => label(jobStatusLabels, value?.toLowerCase());
export const evidenceTypeLabel = (value: string | null | undefined) => label(evidenceTypeLabels, value);
export const knowledgeFreshnessLabel = (value: string | null | undefined) => label(freshnessLabels, value);
export const knowledgeSourceTypeLabel = (value: string | null | undefined) => label(knowledgeSourceTypeLabels, value);
export const chunkStatusLabel = (value: string | null | undefined) => label(chunkStatusLabels, value);
export const repositoryLifecycleLabel = (value: string | null | undefined) => label(repositoryLifecycleLabels, value);
export const repositorySyncStatusLabel = (value: string | null | undefined) => label(repositorySyncStatusLabels, value);
export const skillNameLabel = (value: string | null | undefined, fallback?: string | null) =>
  skillNames[value ?? ""] ?? (fallback?.trim() || unknownLabel);

export function safeJobErrorMessage(errorCode: string | null, retryable: boolean) {
  if (errorCode === "COLLECTION_LIMIT_EXCEEDED") return "저장소 규모가 현재 수집 한도를 초과했습니다. 수집 범위를 줄인 뒤 다시 시도해 주세요.";
  if (errorCode === "RATE_LIMIT_EXCEEDED") return "외부 서비스 요청 한도에 도달했습니다. 잠시 후 다시 시도해 주세요.";
  return retryable ? "작업을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요." : "작업을 완료하지 못했습니다. 연결과 접근 권한을 확인한 뒤 다시 시도해 주세요.";
}
