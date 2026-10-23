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

const gapStateLabels = { MISSING: "관찰 없음", WEAK: "약함", PARTIAL: "일부 충족", SUFFICIENT: "충족", STRONG: "강점" };
const growthTrendLabels = { UNAVAILABLE: "비교 데이터 없음", IMPROVING: "향상", STABLE: "유지", DECLINING: "감소" };
const skillStateLabels = { STRENGTH: "강점", WEAKNESS: "개선 영역", NEUTRAL: "보통" };
const knowledgeStatusLabels = { ACTIVE: "활성", ARCHIVED: "보관됨" };
const ruleStatusLabels = { PASSED: "충족", FAILED: "미충족", PARTIAL: "부분 충족", SKIPPED: "평가 제외", ERROR: "평가 오류" };
const evidenceRoleLabels = { DIRECT: "직접 근거", SUPPORTING: "보조 근거", MISSING: "부족한 근거" };
const ruleLabels: Record<string, string> = {
  LANGUAGE_PRIMARY_SHARE: "주 사용 언어 비중", LANGUAGE_DIVERSITY: "사용 언어 다양성", FRAMEWORK_COUNT: "프레임워크 사용 수",
  TEST_FILES: "테스트 파일", TEST_FRAMEWORKS: "테스트 프레임워크", CI_WORKFLOW_METADATA: "CI 워크플로",
  README_PRESENT: "README", API_DOCUMENTATION: "API 문서", ARCHITECTURE_DOCUMENTATION: "아키텍처 문서",
  CONTRIBUTING_GUIDE: "기여 가이드", LICENSE_PRESENT: "라이선스", COMMIT_COUNT: "커밋 수",
  CONTRIBUTOR_COUNT: "기여자 수", BRANCH_COUNT: "브랜치 수"
};
const formulaLabels = { BOOLEAN_100: "존재하면 100점", LINEAR_0_100: "관찰값을 구간 안에서 0~100점으로 환산", COUNT_CAPPED_100: "개수를 상한까지 0~100점으로 환산" };
const careerStatusLabels = { SUPPORTED: "공식 지원" };
const roadmapStatusLabels = { CREATED: "생성됨", IN_PROGRESS: "진행 중", COMPLETED: "완료", ARCHIVED: "보관됨" };
const roadmapStepStatusLabels = { COMPLETED: "완료", IN_PROGRESS: "진행 중", NOT_STARTED: "예정", SKIPPED: "건너뜀" };
const difficultyLabels = { BEGINNER: "입문", INTERMEDIATE: "중급", ADVANCED: "고급" };

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
export const gapStateLabel = (value: string | null | undefined) => label(gapStateLabels, value);
export const growthTrendLabel = (value: string | null | undefined) => label(growthTrendLabels, value);
export const skillStateLabel = (strength: boolean, weakness: boolean) => label(skillStateLabels, strength ? "STRENGTH" : weakness ? "WEAKNESS" : "NEUTRAL");
export const knowledgeStatusLabel = (value: string | null | undefined) => label(knowledgeStatusLabels, value);
export const ruleStatusLabel = (value: string | null | undefined) => label(ruleStatusLabels, value);
export const evidenceRoleLabel = (value: string | null | undefined) => label(evidenceRoleLabels, value);
export const ruleLabel = (value: string | null | undefined, fallback = unknownLabel) => ruleLabels[value ?? ""] ?? fallback;
export const formulaLabel = (value: string | null | undefined) => label(formulaLabels, value);
export const careerStatusLabel = (value: string | null | undefined) => label(careerStatusLabels, value);
export const roadmapStatusLabel = (value: string | null | undefined) => label(roadmapStatusLabels, value);
export const roadmapStepStatusLabel = (value: string | null | undefined) => label(roadmapStepStatusLabels, value);
export const difficultyLabel = (value: string | null | undefined) => label(difficultyLabels, value);
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
