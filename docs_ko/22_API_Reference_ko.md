<!--
한국어판 문서입니다. 원본 기준 문서: docs/22_API_Reference.md
요구사항 ID, API ID, 경로, 필드명, 열거값, 오류 코드는 추적성을 위해 원문 표기를 유지합니다.
-->

# DevPath API 레퍼런스 (구현된 엔드포인트)

> **권위 고지.** 정식·권위 API 명세는 계속 `docs/10_API_Specification.md`이며, 기계 판독용 계약은
> `contracts/openapi/devpath-openapi.yaml`(info.version `0.27.0`)입니다. 이 문서는 그 두 원천과 커밋 `d6753b9`
> 시점의 백엔드 웹 어댑터(`backend/src/main/java/com/devpath/**/adapter/in/web/**`,
> `identity/config/SecurityConfiguration.java`, `platform/health/InternalHealthController.java`, `shared/api/**`)에서
> 도출한 **비권위, 구현 검증 레퍼런스**입니다. 이 문서와 권위 원천이 다르면 권위 원천이 우선하며, 발견된 차이는
> [부록 A](#부록-a-계약-검증)에 기록만 하고 여기서 **수정하지 않습니다**. 영어판 `docs/22_API_Reference.md`가 기준입니다.

## 1. 개요

### 1.1 기본 경로와 버전

| 항목 | 구현 동작 |
|---|---|
| 비즈니스 API 기본 경로 | `/api/v1` (URI 버전 관리, docs/10 §5.1, §29) |
| 프레임워크/인프라 경로 | `/internal/health`, `/oauth2/authorization/github`, `/login/oauth2/code/github` (`/api/v1` 외부) |
| 콘텐츠 타입 | 요청·응답 본문 모두 `application/json` |
| `metadata.apiVersion` | 항상 `"v1"` (`shared/api/ApiMetadata.java:7`) |
| 호환성 | 필드·엔드포인트 추가는 호환. 클라이언트는 알 수 없는 열거값을 허용하고 알 수 없는 응답 필드를 무시해야 함 (docs/10 §29) |

### 1.2 인증

| 항목 | 구현 동작 |
|---|---|
| 로그인 | Spring Security가 소유하는 GitHub OAuth2 Login. 브라우저가 `GET /oauth2/authorization/github`로 이동하고 GitHub가 `GET /login/oauth2/code/github`로 돌아오며, 성공/실패 시 설정된 프론트엔드 URL로 리다이렉트 (`SecurityConfiguration.java:67-74`). |
| 세션 | PostgreSQL에 저장되는 불투명 서버 세션(`spring.session.store-type: jdbc`). 쿠키 `DEVPATH_SESSION`(설정 가능), `HttpOnly`, 기본 `Secure`, `SameSite=Lax`, 경로 `/` (`application.yml:36-51`). 로그인 시 세션 ID 교체(`migrateSession`). |
| 타임아웃 | 유휴 기본 `30m`, 절대 기본 `12h`(`AbsoluteSessionTimeoutFilter`) (`application.yml:38,81`). |
| 세션 부트스트랩 | `GET /api/v1/users/me`: `200` = 인증됨, `401` = 익명. 네트워크 실패는 별도의 클라이언트 상태. |
| 로그아웃 | `POST /api/v1/session/logout`(CSRF 필요)은 세션 무효화, 인증 해제, 세션 쿠키 삭제 후 `204` 반환 (`SecurityConfiguration.java:75-81`, `AuditedLogoutSuccessHandler.java:45`). |
| 미인증 접근 | 아래 공개 경로를 제외한 모든 경로는 `401`과 `ErrorEnvelope` 코드 `AUTHENTICATION_REQUIRED` 반환 (`SecurityConfiguration.java:83-94`). |
| 공개(세션 불필요) 경로 | `/internal/health`, `/oauth2/authorization/github`, `/login/oauth2/code/github`, `/api/v1/integrations/github/callback`, `/error`, `GET /api/v1/csrf` (`SecurityConfiguration.java:57-64`). |
| Bearer 토큰 | 사용하지 않음. 제공자(GitHub/Notion) 토큰은 반환되지 않으며 DevPath 자격 증명으로 받지 않음. |

### 1.3 CSRF

| 항목 | 구현 동작 |
|---|---|
| 범위 | 모든 상태 변경 메서드(`POST`, `PUT`, `PATCH`, `DELETE`)에 Spring Security CSRF 보호 적용. |
| 토큰 발급 | `GET /api/v1/csrf`가 `{headerName, parameterName, token}` 반환. 토큰은 쿠키 기반 저장소(`CookieCsrfTokenRepository`, 쿠키 경로 `/`)에도 보관 (`SecurityConfiguration.java:45-47`, `CsrfController.java:14-20`). |
| 헤더 | 토큰을 `X-CSRF-TOKEN` 헤더로 전송. |
| 실패 | 토큰 누락/불일치 시 `403`, 코드 `ACCESS_DENIED` (`SecurityConfiguration.java:95-106`). |

### 1.4 CORS

| 항목 | 구현 동작 (`SecurityConfiguration.java:114-134`) |
|---|---|
| 허용 Origin | 설정된 프론트엔드 Origin 하나(`devpath.security.frontend-origin`). 와일드카드 없음. |
| 자격 증명 | 허용(`allowCredentials=true`). |
| 메서드 | `GET, POST, PUT, PATCH, DELETE, OPTIONS` |
| 요청 헤더 | `Accept`, `Content-Type`, `Idempotency-Key`, `X-CSRF-TOKEN`, `X-Request-Id`, `X-Correlation-Id` |
| 노출 헤더 | `X-Request-Id`, `X-Correlation-Id` |
| Preflight 캐시 | `3600`초 |

### 1.5 요청/상관 식별자

`X-Request-Id`, `X-Correlation-Id`는 선택 사항입니다. 값은 `[A-Za-z0-9][A-Za-z0-9._-]{0,63}`과 일치해야 하며, 잘못되었거나
없으면 대체됩니다(요청 ID는 생성된 UUID, 상관 ID는 요청 ID). 확정된 두 값은 응답 헤더로 반환되고 `metadata.requestId`도
같은 요청 ID를 사용합니다 (`shared/api/RequestIds.java`, `RequestCorrelationFilter.java:47-52`).

### 1.6 응답 봉투

모든 JSON 비즈니스 응답은 `{ "data": <payload>, "metadata": ApiMetadata }` 형식입니다 (`shared/api/ApiResponse.java`).

| `metadata` 필드 | 타입 | 비고 |
|---|---|---|
| `requestId` | string | 확정된 요청 식별자 |
| `apiVersion` | string | `"v1"` |
| `timestamp` | date-time | 응답 서버 시각 |

예외: `GET /internal/health`는 봉투 없는 `{status, application, timestamp}` 객체를 반환하고, OAuth 콜백은 `302`,
로그아웃은 본문 없는 `204`를 반환합니다.

### 1.7 오류 봉투와 오류 코드

오류는 `{ "error": { "code", "message" }, "metadata": ApiMetadata }` 형식입니다 (`shared/api/ApiErrorResponse.java`).
메시지는 고정된 사용자 안전 문자열이며 제공자 페이로드, SQL 오류, 스택 트레이스는 반환되지 않습니다.

| 코드 | HTTP | 발생 조건(구현) |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | Bean Validation 실패, 읽을 수 없는 JSON, (매핑된 경우) 헤더 누락, 지원하지 않는 값, 다른 명령에 대한 멱등 키 재사용, 잘못된 cursor/limit |
| `AUTHENTICATION_REQUIRED` | 401 | 유효한 세션 없음, identity 엔드포인트에서 비활성/없는 계정 |
| `ACCESS_DENIED` | 403 | CSRF 실패 / Spring Security 접근 거부 |
| `RESOURCE_NOT_FOUND` | 404 | 리소스가 없거나 **다른 사용자 소유**(소유 여부를 노출하지 않음) |
| `RESOURCE_CONFLICT` | 409 | 동시 쓰기 경합(`DataIntegrityViolationException`, 낙관적 잠금) |
| `RATE_LIMIT_EXCEEDED` | 429 | GitHub 또는 Notion 제공자 할당량 소진. 알려진 경우 `Retry-After`, (GitHub) `X-RateLimit-Reset` 헤더 |
| `DEPENDENCY_UNAVAILABLE` | 503 | GitHub/Notion 사용 불가, 설정 누락, 재연결 필요 |
| `KNOWLEDGE_RETRIEVAL_FAILED` | 503 | 지식 검색 중 임베딩, 벡터 인덱스, 보존 객체 콘텐츠 사용 불가 |

지속 작업 리소스는 작업 수준의 안전한 `errorCode`(예: `COLLECTION_LIMIT_EXCEEDED`, `RATE_LIMIT_EXCEEDED`,
`SOURCE_PERMISSION_CHANGED`, `SOURCE_UNAVAILABLE`, `INGESTION_FAILED`)를 추가로 가집니다 — §3 참조.

참고: 매핑된 핸들러에 도달하기 전에 실패한 요청(예: UUID가 아닌 경로 변수, `MissingRequestHeaderException`을 매핑하지 않는
어드바이스를 가진 엔드포인트의 헤더 누락)은 `ErrorEnvelope`가 아닌 프레임워크 기본 오류 본문을 받습니다. 부록 A 참조.

### 1.8 페이지네이션

커서 페이지네이션은 저장소 목록과 분석 이력 목록에만 구현되어 있습니다.

| 파라미터 | 타입 | 기본값 | 제약 |
|---|---|---|---|
| `limit` | integer | 20 | 1–100 |
| `cursor` | string | 없음 | 불투명 값. 이전 페이지의 `nextCursor` 사용 |

페이지 응답은 `<항목 배열>`, `limit`, `nextCursor`(마지막 페이지에서 null), `totalCount`를 포함합니다. 그 외 목록
엔드포인트는 페이지네이션 없이 상한이 있는 전체 배열을 반환합니다.

### 1.9 멱등성

`Idempotency-Key`(1–128자, OpenAPI 기준 `^[A-Za-z0-9._:-]+$`)가 필요한 명령:
`POST /repositories/{id}/sync`, `POST /analyses`, `POST /knowledge-documents/imports/notion`,
`POST /knowledge-documents/{id}/reindex`, `POST /generation-requests`, `POST /learning-roadmaps/{id}/archive`.

| 상황 | 구현 결과 |
|---|---|
| 같은 소유자 + 키 + 같은 명령 | 기존 작업 반환(`202`) |
| 같은 소유자 + 키, 다른 명령 | `400 VALIDATION_ERROR` |
| 다른 키로 동등한 활성 명령 | 기존 활성 작업 재사용(동기화, 분석, 지식 가져오기) |
| 유니크 제약 동시 경합 | `409 RESOURCE_CONFLICT` |

## 2. 엔드포인트 카탈로그

표기 규칙: **Auth** = *public* 표시가 없으면 세션 쿠키 필요. **CSRF** = `X-CSRF-TOKEN` 필요. **소유자** = 리소스는
인증 사용자 범위이며 타인의 ID는 `404`. 별도 표기가 없으면 경로 ID는 UUID. 인증 엔드포인트는 모두
`401 AUTHENTICATION_REQUIRED`, CSRF 엔드포인트는 모두 `403 ACCESS_DENIED`를 반환할 수 있으며 "오류" 열에서는 생략합니다.

### 2.1 세션, identity, 온보딩

| API ID | 메서드 + 경로 | 목적 | 요청 | 성공 (`data`) | 오류 |
|---|---|---|---|---|---|
| — | `GET /internal/health` *(public)* | 비민감 상태 확인 | — | `200` 봉투 없는 `{status:"ok", application, timestamp}` | — |
| — | `GET /oauth2/authorization/github` *(public)* | GitHub 로그인 시작 | — | `302` GitHub로 | — |
| — | `GET /login/oauth2/code/github` *(public)* | 로그인 완료, 사용자 생성/확인, 세션 교체 | query `code`, `state`(필수), `error` | `302` 프론트엔드 성공/실패 URL | — |
| — | `GET /api/v1/csrf` *(public)* | CSRF 토큰 발급 | — | `200` `{headerName, parameterName, token}` | — |
| — | `POST /api/v1/session/logout` (CSRF) | 세션 종료, 쿠키 삭제 | — | `204` | 401, 403 |
| API-ID-001 | `GET /api/v1/users/me` | 세션 부트스트랩 | — | `200` `{userId, displayName, avatarUrl?, status: ACTIVE, authenticationProvider: GITHUB, createdAt}` | 401 (비활성/없는 계정 포함) |
| API-ID-002 | `GET /api/v1/users/me/profile` | 프로필 조회 | — | `200` `UserProfile {profileId, displayName, careerStage?, bio?, updatedAt}` | 404 `RESOURCE_NOT_FOUND` |
| API-ID-003 | `PATCH /api/v1/users/me/profile` (CSRF) | 프로필 수정 | `displayName` string, 필수, 1–120; `careerStage` enum `STUDENT\|ENTRY_LEVEL\|JUNIOR\|MID_LEVEL\|SENIOR`, nullable; `bio` string ≤1000, nullable | `200` `UserProfile` | 400, 404 |
| API-ID-004 | `GET /api/v1/users/me/connections` | 제공자 연결 | — | `200` `{connections: [{connectionId, provider: GITHUB\|NOTION, status: ACTIVE\|EXPIRED\|REVOKED, scopes[], connectedAt, expiresAt?}]}` (비활성 → 빈 `scopes`) | — |
| API-ID-005 | `GET /api/v1/users/me/preferences` | 직무/회사 목표 | — | `200` `{careerId?, companyId?, updatedAt?}` | — |
| API-ID-006 | `PUT /api/v1/users/me/preferences/career` (CSRF) | 직무 목표 설정(직무 카탈로그로 검증) | `careerId` string, 필수, 1–64 | `200` `UserPreference` | 400 지원하지 않는 목표 |
| API-ID-007 | `PUT /api/v1/users/me/preferences/company` (CSRF) | 회사 목표 설정(회사 카탈로그로 검증) | `companyId` string, 필수, 1–64 | `200` `UserPreference` | 400 지원하지 않는 목표 |
| API-ID-010 | `GET /api/v1/users/me/onboarding-progress` | 결정적 온보딩 투영(점수 계산 없음) | — | `200` `{status: GETTING_STARTED\|IN_PROGRESS\|DASHBOARD_READY, completedStepCount, totalStepCount: 8, nextStep, steps[8] {step, requirement: REQUIRED\|RECOMMENDED\|OPTIONAL, status: COMPLETE\|INCOMPLETE, resourceId?, actionPath}, generatedAt}` | — |

### 2.2 연동 (GitHub, Notion)

| API ID | 메서드 + 경로 | 목적 | 요청 | 성공 (`data`) | 오류 |
|---|---|---|---|---|---|
| API-INT-001 | `POST /api/v1/integrations/github/authorize` (CSRF) | GitHub App 사용자 인가 시작. state는 세션+사용자에 바인딩, 유효 10분 | — | `200` `{authorizationUrl}` | 503 설정 없음 |
| API-INT-002 | `GET /api/v1/integrations/github/callback` *(공개 경로, 세션 state 사용)* | 인가 완료. state 1회용 | query `state`(필수), `code`, `error` | `302` `{frontend}/settings/integrations?githubConnection=success\|failed\|installation-required` | 잘못된 state / 세션 없음 → `302 …=failed` |
| API-INT-003 | `DELETE /api/v1/integrations/github` (CSRF) | 연결 해제. 로컬 자격 증명 삭제, 원격 폐기 시도 | — | `200` `ConnectedAccount` (status `REVOKED`) | 404 활성 연결 없음, 503 |
| API-INT-004 | `GET /api/v1/integrations/github/repositories` | 설치된 GitHub App으로 접근 가능한 저장소 | — | `200` `{repositories: [{providerRepositoryId, name, fullName, owner, privateRepository, archived, defaultBranch, htmlUrl}]}` | 429 (+`Retry-After`, `X-RateLimit-Reset`), 503 |
| API-INT-008 | `POST /api/v1/integrations/notion/authorize` (CSRF) | Notion 인가 시작(state 10분) | — | `200` `{authorizationUrl}` | 503 |
| API-INT-009 | `GET /api/v1/integrations/notion/callback` | Notion 인가 완료(세션 필요) | query `state`(필수), `code`, `error` | `302` `{frontend}/settings/integrations?notionConnection=success\|failed` | 세션 없으면 401 |
| API-INT-010 | `DELETE /api/v1/integrations/notion` (CSRF) | Notion 연결 해제 | — | `200` `ConnectedAccount` | 404 |
| API-INT-011 | `GET /api/v1/integrations/notion/workspaces` | 워크스페이스 페이지 메타데이터 발견/교체(콘텐츠 제외) | — | `200` `{workspaces: [{connectionId, workspaceId, workspaceName, workspaceIconUrl?, status: ACTIVE, connectedAt, discoveredAt, pages[≤1000] {providerPageId, title, objectType: PAGE\|DATA_SOURCE, url?, lastEditedAt, inTrash}}]}` | 404, 429 (+`Retry-After`), 503 |

### 2.3 저장소, 동기화, 스냅샷

`Repository` = `{repositoryId, providerRepositoryId, name, fullName, owner, visibility: PUBLIC|PRIVATE, defaultBranch,
providerArchived, lifecycle: DISCOVERED|ACTIVE|ARCHIVED|DELETED_EXTERNALLY, syncStatus: NOT_SYNCED|SYNCHRONIZED|FAILED,
htmlUrl, discoveredAt, lastSyncedAt?, currentSnapshotId?}`.

| API ID | 메서드 + 경로 | 목적 | 요청 | 성공 (`data`) | 오류 |
|---|---|---|---|---|---|
| API-INT-005 | `POST /api/v1/repositories/imports` (CSRF) | 접근 가능한 GitHub 저장소 가져오기(서버에서 재검증, 중복은 기존 반환) | `providerRepositoryId` string, 필수, `^[1-9][0-9]{0,63}$` | `201` `Repository` | 400 잘못됨/접근 불가, 409, 429, 503 |
| API-REP-001 | `GET /api/v1/repositories` | 소유자 저장소 페이지 | query `limit`, `cursor`, `includeArchived`(bool, 기본 `false`) | `200` `{repositories[], limit, nextCursor?, totalCount}` | 400 limit/cursor |
| API-REP-002 | `GET /api/v1/repositories/{repositoryId}` | 저장소 상세 | — | `200` `Repository` | 404 |
| API-REP-004 | `POST /api/v1/repositories/{repositoryId}/archive` (CSRF) | 로컬 보관 | — | `200` `Repository` | 404, 409 |
| API-REP-005 | `POST /api/v1/repositories/{repositoryId}/restore` (CSRF) | 로컬 보관 복원(제공자 보관은 불가) | — | `200` `Repository` | 400, 404, 409 |
| API-REP-006 | `POST /api/v1/repositories/{repositoryId}/sync` (CSRF, Idempotency-Key) | 지속 전체 동기화 대기열 등록. 요청 스레드에서 GitHub 호출 없음 | — | `202` `RepositorySyncJob` (§3) | 400 키/생명주기, 404, 503 |
| API-INT-007 | `GET /api/v1/repository-sync-jobs/{jobId}` | 동기화 작업 폴링 | — | `200` `RepositorySyncJob` | 404 |
| API-REP-007 | `GET /api/v1/repositories/{repositoryId}/snapshots` | 스냅샷 이력(최신순) | — | `200` `{snapshots: RepositorySnapshot[]}` | 404 |
| API-REP-008 | `GET /api/v1/repositories/{repositoryId}/snapshots/{snapshotId}` | 불변 스냅샷 메타데이터 | — | `200` `{snapshotId, repositoryId, capturedAt, sourceRevision, status: READY\|FAILED\|SUPERSEDED\|DELETED_BY_POLICY, immutable, contentHash, branchCount, commitCount, pullRequestCount, issueCount, documentCount}` | 404 |
| API-REP-009 | `GET /api/v1/repositories/{repositoryId}/technologies` | 현재 스냅샷 기술 탐지(점수 없음) | — | `200` `{repositoryId, snapshotId, extractorVersion, taxonomyVersion, primaryLanguage?, technologies[] {name, category: LANGUAGE\|FRAMEWORK\|DATABASE, evidenceLabel, byteCount?, percentage?, taxonomyStatus, evidenceType, evidencePaths[]}}` | 404 |
| API-REP-011 | `GET /api/v1/repositories/{repositoryId}/evidence` | 엔지니어링 증거 신호 + 활동 타임라인(점수 없음) | — | `200` `{repositoryId, snapshotId, extractorVersion, categories[] {category, label, signals[] {signalKey, label, present, count, observedValue?, evidencePaths[≤20]}}, activityTimeline {extractorVersion, scope, measuredAt, latestActivityAt?, daysSinceLatestActivity?, totalEventCount, truncated, events[≤100]}}` | 404 |

### 2.4 분석과 규칙 평가

| API ID | 메서드 + 경로 | 목적 | 요청 | 성공 (`data`) | 오류 |
|---|---|---|---|---|---|
| API-ANA-001 | `POST /api/v1/analyses` (CSRF, Idempotency-Key) | 결정적 분석 대기열 등록 | `repositoryId` uuid, 필수; `snapshotId` uuid, nullable; `analysisScope` `REPOSITORY_BASELINE`(기본); `targetCareerId`는 null이어야 함(non-null → 400) | `202` `AnalysisJob` (§3) | 400, 404, 409 |
| API-ANA-002 | `GET /api/v1/analysis-jobs/{jobId}` | 분석 작업 폴링 | — | `200` `AnalysisJob` | 404 |
| API-ANA-003 | `GET /api/v1/analyses/{analysisId}` | 완료된 불변 분석 | — | `200` `{analysisId, repositoryId, snapshotId, evaluationId, skillMatrixId, analysisScope, currentForRepository, completedAt}` | 404 |
| API-ANA-007 | `GET /api/v1/analyses` | 소유자 분석 이력(최신순) | query `limit`, `cursor` | `200` `{analyses[] (AnalysisResult + repositoryFullName, overallScore, confidence, ruleSetVersion, policyVersion), limit, nextCursor?, totalCount}` | 400 |
| API-REP-012 | `GET /api/v1/repositories/{repositoryId}/analyses` | 저장소 분석 이력 | query `limit`, `cursor` | `200` API-ANA-007과 동일 | 400, 404 |
| API-ANA-008 | `GET /api/v1/analyses/compare` | 저장된 분석 2건 나란히(차이 계산 없음) | query `analysisId`를 정확히 2회, 서로 다르게 | `200` 요청 순서의 `{analyses[2]}` | 400, 404 |
| API-ANA-004 | `GET /api/v1/rule-evaluations/{evaluationId}` | 불변 규칙 평가 | — | `200` `{evaluationId, snapshotId, ruleSetVersionId, ruleSetVersion, formulaLibraryVersion, extractorVersion, overallScore, confidence, evidenceSummary, categoryScores[], warnings[], completedAt}` | 404 |
| API-ANA-005 | `GET /api/v1/rule-evaluations/{evaluationId}/score-breakdown` | 카테고리/규칙 분해와 trace | — | `200` `{evaluationId, overallScore, confidence, categories[] {category, score, weight, confidence, ruleResults[] {ruleId, ruleVersion, status, rawValue, score, weight, formulaId, trace, evidenceReferences[]}, missingEvidence[]}}` | 404 |
| API-ANA-006 | `GET /api/v1/rule-evaluations/{evaluationId}/evidence` | 규칙에 연결된 증거 | — | `200` `{evaluationId, evidence[] {evidenceId, ruleId, contributionRole: DIRECT\|SUPPORTING\|MISSING, evidenceType, sourceReference, observedFactSummary, confidence}}` | 404 |

### 2.5 Skill Matrix와 스킬

| API ID | 메서드 + 경로 | 목적 | 요청 | 성공 (`data`) | 오류 |
|---|---|---|---|---|---|
| API-SKL-001 | `GET /api/v1/skill-matrices/current` | 현재 Skill Matrix | — | `200` `SkillMatrix {skillMatrixId, evaluationId, policyVersion, ruleSetVersion, status: CURRENT\|SUPERSEDED\|ARCHIVED, skills[] SkillAssessment, strengths[], weaknesses[], generatedAt}` | 404 아직 없음 |
| API-SKL-002 | `GET /api/v1/skill-matrices/{skillMatrixId}` | 과거 Matrix | — | `200` `SkillMatrix` | 404 |
| API-SKL-003 | `GET /api/v1/skill-matrices/compare` | 저장된 Matrix 2건(차이 계산 없음) | query `skillMatrixId`를 정확히 2회, 서로 다르게 | `200` `{matrices[2]}` | 400, 404 |
| API-SKL-004 | `GET /api/v1/skills/{skillId}` | 안정 스킬의 현재 평가 | — | `200` `{skillMatrixId, evaluationId, policyVersion, ruleSetVersion, matrixStatus: CURRENT, generatedAt, skill: SkillAssessment}` | 404 |
| API-SKL-005 | `GET /api/v1/skills/{skillId}/evidence` | 현재 평가의 증거 | — | `200` `{skillId, skillAssessmentId, skillMatrixId, evidence[] {evidenceId, snapshotId, evidenceType, sourceReference, observedFactSummary, confidence}}` | 404 |

`SkillAssessment` = `{assessmentId, skillId, skillKey, skillName, category, score, level: NONE|BEGINNER|DEVELOPING|COMPETENT|STRONG,
confidence, strength, weakness, growthTrend: UNAVAILABLE|IMPROVING|STABLE|DECLINING, aggregateRuleResultReference,
evidenceIds[], repositoryIds[], recommendationInputFacts[], ruleSetVersion}`.

### 2.6 직무, 회사, 준비도

| API ID | 메서드 + 경로 | 목적 | 요청 | 성공 (`data`) | 오류 |
|---|---|---|---|---|---|
| API-CAR-001 | `GET /api/v1/careers` | 지원 직무 카탈로그 | — | `200` `{catalogVersion, careers[] {careerId, name, localizedName, status: SUPPORTED, profileVersion, purpose}}` | — |
| API-CAR-002 | `GET /api/v1/careers/{careerId}` | 활성 직무 프로필 | path `careerId` `^[a-z][a-z0-9-]{1,63}$` | `200` 요약 + `{careerProfileVersionId, coreTechnologies[], requiredCompetencies[], preferredCompetencies[], evaluationCategories[], priorityWeights{cat: HIGH\|MEDIUM\|LOW}, roadmapTemplate[], effectiveAt}` | 400, 404 |
| API-CAR-004 | `GET /api/v1/career-readiness/current` | 선택 직무의 현재 준비도 | — | `200` `CareerReadiness` | 404 |
| API-CAR-005 | `GET /api/v1/career-readiness/{careerReadinessId}` | 과거 준비도 | — | `200` `CareerReadiness` | 404 |
| API-CAR-006 | `GET /api/v1/career-readiness/{careerReadinessId}/skill-gaps` | 정렬된 격차 | — | `200` `{careerReadinessId, skillGaps[]}` | 404 |
| API-CMP-001 | `GET /api/v1/companies` | 지원 회사 카탈로그 | — | `200` `{catalogVersion, companies[] {companyId, name, localizedName, status: SUPPORTED, profileVersion, engineeringCulture}}` | — |
| API-CMP-002 | `GET /api/v1/companies/{companyId}` | 활성 회사 프로필(일반적 강조점이며 채용 기준 아님) | path `companyId` `^[a-z][a-z0-9-]{1,63}$` | `200` 요약 + `{companyProfileVersionId, technologyFocus[], preferredCompetencies[], recommendationPriorities[], skillEmphasis[], weightOverrides{cat: INCREASE}, effectiveAt}` | 400, 404 |

`CareerReadiness` = `{careerReadinessId, skillMatrixId, careerId, careerProfileVersionId, careerProfileVersion,
readinessPolicyVersion, ruleSetVersion, status: COMPLETED|INSUFFICIENT_EVIDENCE, readinessScore?, readinessLevel?,
confidence, unavailableCategories[], skillGaps[], assessedAt}`. `SkillGap` = `{skillGapId, skillId, skillKey, category,
actualScore, actualLevel, expectedMinimum (60), gapState: MISSING|WEAK|PARTIAL|SUFFICIENT|STRONG, careerWeight, evidenceIds[]}`.

### 2.7 추천과 학습 로드맵

| API ID | 메서드 + 경로 | 목적 | 요청 | 성공 (`data`) | 오류 |
|---|---|---|---|---|---|
| API-REC-002 | `GET /api/v1/recommendations/current` | 현재 추천 세트 | — | `200` `{recommendationSetId, careerReadinessId, policyVersion, status: PUBLISHED\|SUPERSEDED, recommendations[], generatedAt}` | 404 |
| API-REC-004 | `GET /api/v1/recommendations` | 세트 이력(최신순) | — | `200` `{recommendationSets[]}` | — |
| API-REC-003 | `GET /api/v1/recommendations/{recommendationId}` | 추천 상세 | — | `200` `{recommendationId, skillGapId, category, type: STUDY\|PROJECT\|ARCHITECTURE\|PORTFOLIO, priority: CRITICAL\|HIGH\|MEDIUM\|LOW, rationaleCode, title, completionCriteria, expectedEvidence[], evidenceIds[], effortHours, position, status}` | 404 |
| API-REC-008 | `GET /api/v1/recommendations/{recommendationId}/evidence` | 연결된 증거 | — | `200` `{recommendationId, evidence[] {evidenceId, evidenceType, sourceReference, observedFactSummary, confidence, createdAt}}` | 404 |
| API-LRN-003 | `GET /api/v1/learning-roadmaps` | 로드맵 이력(보관 포함) | — | `200` `{roadmaps[]}` | — |
| API-LRN-002 | `GET /api/v1/learning-roadmaps/active` | 활성 로드맵 | — | `200` `LearningRoadmap` | 404 |
| API-LRN-004 | `GET /api/v1/learning-roadmaps/{roadmapId}` | 로드맵 상세 | — | `200` `LearningRoadmap` | 404 |
| API-LRN-009 | `POST /api/v1/learning-roadmaps/{roadmapId}/archive` (CSRF, Idempotency-Key) | 보관. 반복 요청은 변경 없는 보관 상태 반환 | — | `200` `LearningRoadmap` | 404 |

`LearningRoadmap` = `{roadmapId, recommendationSetId, policyVersion, status: CREATED|IN_PROGRESS|COMPLETED|ARCHIVED,
progressPercent, milestones[] {milestoneId, position, category, title, status}, steps[] {roadmapStepId, milestoneId,
recommendationId, position, category, title, difficulty, effortHours, prerequisiteStepIds[], completionCriteria,
expectedEvidence[], status}, generatedAt, updatedAt}`. 추천이나 로드맵을 생성하는 공개 엔드포인트는 없습니다.

### 2.8 지식

| API ID | 메서드 + 경로 | 목적 | 요청 | 성공 (`data`) | 오류 |
|---|---|---|---|---|---|
| API-KNW-003 | `POST /api/v1/knowledge-documents/imports/notion` (CSRF, Idempotency-Key) | 인가된 Notion 페이지 가져오기 | `connectionId` uuid, 필수; `providerPageId` string, 필수, 1–255 | `202` `KnowledgeIngestionJob` (§3) | 400, 404 연결/페이지 없음, 409, 503 |
| API-KNW-008 | `GET /api/v1/knowledge-ingestion-jobs/{jobId}` | 수집 작업 폴링 | — | `200` `KnowledgeIngestionJob` | 404 |
| API-KNW-004 | `GET /api/v1/knowledge-documents` | 소유자 문서 | — | `200` `{documents[] {documentId, sourceType: NOTION, sourceObjectId, title, status: ACTIVE\|ARCHIVED, currentVersionId?, chunkCount, createdAt, updatedAt}}` | — |
| API-KNW-005 | `GET /api/v1/knowledge-documents/{documentId}` | 문서 메타데이터 | — | `200` `KnowledgeDocument` | 404 |
| API-KNW-010 | `GET /api/v1/knowledge-documents/{documentId}/chunks` | 청크 메타데이터(본문·벡터 제외) | — | `200` `{chunks[] {chunkId, position, heading?, contentHash, tokenEstimate, status: INDEXED\|STALE\|DELETED}}` | 404 |
| API-KNW-006 | `POST /api/v1/knowledge-documents/{documentId}/archive` (CSRF) | 보관 및 검색 대상 제외 | — | `200` `KnowledgeDocument` | 404 |
| API-KNW-011 | `POST /api/v1/knowledge-documents/{documentId}/reindex` (CSRF, Idempotency-Key) | Notion에서 재색인 | — | `202` `KnowledgeIngestionJob` | 400, 404, 409, 503 |
| API-KNW-009 | `POST /api/v1/knowledge-search` (CSRF) | 소유자 범위 시맨틱 검색 | `query` string, 필수, 1–500; `filters.sourceTypes` `NOTION` ≤1; `filters.documentIds` uuid ≤20; `limit` 1–20(기본 5); `contextPurpose` `USER_SEARCH`(기본) | `200` `{retrievalResultId, retrievalType: SEMANTIC, policyVersion: knowledge-semantic-v1, contextPurpose, appliedFilters, results[≤20] {chunkId, documentId, documentTitle, sourceType, sourceObjectId, sourceUrl?, heading?, excerpt (≤500), relevance 0–1, tokenEstimate, freshness: FRESH}, resultCount, durationMs, generatedAt}` | 400, 503 `KNOWLEDGE_RETRIEVAL_FAILED` |

### 2.9 AI 생성

| API ID | 메서드 + 경로 | 목적 | 요청 | 성공 (`data`) | 오류 |
|---|---|---|---|---|---|
| API-AI-001 | `POST /api/v1/generation-requests` (CSRF, Idempotency-Key) | 근거 기반 생성 요청 | `taskType` + `outputType`(필수, 아래 지원 쌍이어야 함); `sourceResourceRefs` uuid[] 정확히 1개 | `202` `GenerationJob` | 400, 404 소유하지 않은 원천, 409 |
| API-AI-002 | `GET /api/v1/generation-jobs/{jobId}` | 생성 작업 폴링 | — | `200` `GenerationJob` | 404 |
| API-AI-003 | `POST /api/v1/generation-jobs/{jobId}/cancel` (CSRF) | 취소. 종료 상태 작업은 그대로 반환 | 본문 `{}`(필수, 필드 없음) | `200` `GenerationJob` | 404 |
| API-AI-005 | `GET /api/v1/generated-artifacts/{artifactId}` | 검증된 산출물 | — | `200` `{artifactId, type, status: VALIDATED, provenance {skillMatrixId, analysisId?, careerReadinessId?, learningRoadmapId?, promptContextId, templateVersion, provider: OLLAMA, model, contextHash, generatedAt}, validation, contentRef, content}` | 404 |
| API-AI-007 | `GET /api/v1/generated-artifacts/{artifactId}/validation` | 검증 출처 | — | `200` `{status: PASSED\|REJECTED, validatorVersion, validatedAt, violations[]}` | 404 |

지원되는 `taskType` → `outputType` 쌍 (`AiGenerationApplicationService.java:363-368`):
`SKILL_ANALYSIS_EXPLANATION→SKILL_EXPLANATION`, `REPOSITORY_REVIEW→REPOSITORY_REVIEW`,
`REPOSITORY_SUMMARY→REPOSITORY_SUMMARY`, `CAREER_COACHING→CAREER_ADVICE`,
`LEARNING_RECOMMENDATION→LEARNING_RECOMMENDATION`. `GenerationJob` = `{jobId, status, validationStatus:
PENDING|PASSED|REJECTED, artifactUrl?, failureCode?, createdAt, completedAt?}`. 출력 유형별 `content` 스키마는 OpenAPI
(`SkillExplanationContent`, `RepositoryReviewContent`, `RepositorySummaryContent`, `CareerAdviceContent`,
`LearningRecommendationContent`)에 정의되어 있습니다. AI 출력은 공식 점수를 포함하거나 변경하지 않습니다.

### 2.10 대시보드

| API ID | 메서드 + 경로 | 목적 | 요청 | 성공 (`data`) | 오류 |
|---|---|---|---|---|---|
| API-DSH-001 | `GET /api/v1/dashboard/summary` | 권위 원천의 읽기 전용 조합 | — | `200` `{generatedAt, targets, repositories, analyses, skillOverview, readiness, recommendations (항목 ≤3), roadmap, recentJobs (항목 ≤8)}`; 각 섹션에 `status: AVAILABLE\|EMPTY\|UNAVAILABLE` | — |

## 3. 비동기 작업

| 작업 (`jobType`) | 생성 | 폴링 | 취소 | 성공 시 `resultResourceUrl` |
|---|---|---|---|---|
| `REPOSITORY_SYNC` | API-REP-006 | API-INT-007 | 노출 안 됨 | `/api/v1/repositories/{repositoryId}/snapshots/{snapshotId}` |
| `REPOSITORY_ANALYSIS` | API-ANA-001 | API-ANA-002 | 노출 안 됨 | `/api/v1/analyses/{analysisId}` |
| `KNOWLEDGE_INGESTION` | API-KNW-003, API-KNW-011 | API-KNW-008 | 노출 안 됨 | `/api/v1/knowledge-documents/{documentId}` |
| AI 생성(`jobType` 필드 없음) | API-AI-001 | API-AI-002 | API-AI-003 | `artifactUrl` = `/api/v1/generated-artifacts/{artifactId}` |

**공통 작업 필드**(동기화, 분석, 지식): `jobId, jobType, status, phase, progressPercent, attemptCount, maxAttempts,
submittedAt, startedAt?, completedAt?, pollingUrl, resultResourceUrl?, errorCode?, errorMessage?, retryable`.
`pollingUrl`은 위 표의 유형별 작업 경로입니다.

| 작업 | `status` 값 | `phase` 값 (OpenAPI) | `retryable` (도출 규칙) |
|---|---|---|---|
| 저장소 동기화 | `queued, running, succeeded, failed, cancelled, expired`(소문자) | `QUEUED, COLLECTING, RETRY_WAIT, COMPLETED, FAILED` | `failed`이고 `errorCode ≠ COLLECTION_LIMIT_EXCEEDED` (`RepositorySyncJobView.java:31-32`) |
| 분석 | 동기화와 동일 | `QUEUED, EVALUATING_RULES, RETRY_WAIT, COMPLETED, FAILED` | `failed` (`AnalysisJobView.java:53`) |
| 지식 수집 | `queued, running, succeeded, failed` | `QUEUED, COLLECTING, RETRY_WAIT, COMPLETED, FAILED` | queued/running이거나 남은 시도 있음 (`KnowledgeIngestionJobView.java:17-18`) |
| AI 생성 | `QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELED`(대문자) | — | — |

클라이언트 지침:

1. 안정적인 `Idempotency-Key`로 명령을 제출하고, `202`는 완료가 아닌 접수로 취급합니다.
2. 반환된 `pollingUrl`(또는 `/generation-jobs/{jobId}`)을 종료 상태(`succeeded`, `failed`, `cancelled`, `expired` 또는
   `SUCCEEDED`, `FAILED`, `CANCELED`)까지 폴링한 뒤 `resultResourceUrl` / `artifactUrl`을 조회합니다.
3. 동기화·분석·지식 작업은 기본 최대 3회 시도(`maxAttempts`)하며 일시적 실패 사이에 `RETRY_WAIT`를 거칩니다. GitHub
   요청 한도 초과 시 다음 시도는 제공자 리셋 시각까지 지연됩니다.
4. `COLLECTION_LIMIT_EXCEEDED`는 종료·재시도 불가 상태이며 부분 스냅샷은 저장되지 않습니다.
5. 취소는 AI 생성 작업만 가능합니다. `SUCCEEDED`, `FAILED`, `CANCELED` 작업을 취소하면 그대로 반환됩니다
   (`GenerationJob.java:72-78`).

## 부록 A. 계약 검증

엔드포인트 수: OpenAPI는 **75**개 operation을 선언합니다 — `x-api-id`가 있는 **70**개와 인프라/세션 operation 5개
(`/internal/health`, `/oauth2/authorization/github`, `/login/oauth2/code/github`, `GET /api/v1/csrf`,
`POST /api/v1/session/logout`). 코드는 컨트롤러 매핑 **72**개(ID 있는 70개 + health + CSRF)를 노출하고 나머지 3개는
Spring Security가 소유합니다. 모든 OpenAPI operation에 구현이 대응하며, 문서화되지 않은 컨트롤러 엔드포인트는 발견되지
않았습니다. docs/10은 계획된 엔드포인트(예: API-REP-003/010, API-SKL-006/007, API-REC-001/005–007,
API-LRN-001/005–008/010, API-KNW-001/002/007, API-AI-004/006, API-DSH-002–010, API-ID-008/009, Prompt, Portfolio,
Resume, Interview, Admin API)도 추가로 정의하지만 이들은 **구현되지 않았으며** 위에 나열하지 않았습니다.

발견된 차이(기록만 하며 변경하지 않음). 심각도는 런타임 위험이 아닌 계약 영향 기준입니다.

| # | 영역 | 관찰 | 근거 |
|---|---|---|---|
| D1 | 오류 코드(403) | docs/10은 403에 `AUTHORIZATION_DENIED`를 정의하지만 구현은 `ACCESS_DENIED`(CSRF/접근 거부)를 반환. OpenAPI는 403 코드를 명시하지 않음. | `docs/10_API_Specification.md:373`; `backend/.../identity/config/SecurityConfiguration.java:100-101` |
| D2 | 멱등 충돌 | docs/10은 `409 DUPLICATE_REQUEST`, OpenAPI는 409 "idempotency conflict"를 명시하지만, 다른 명령에 키를 재사용하면 실제로 `IllegalArgumentException` → `400 VALIDATION_ERROR`. 409는 동시 제약 경합에서만 발생. | `docs/10_API_Specification.md:376`; `contracts/openapi/devpath-openapi.yaml:1579,1661,1707`; `ai/application/AiGenerationApplicationService.java:108`; `knowledge/application/KnowledgeApplicationService.java:34`; `analysis/application/AnalysisApplicationService.java:49`; `repository/application/RepositorySynchronizationTransaction.java:46` |
| D3 | 봉투 형태 | docs/10 예시는 `data.id/type/attributes`, `links`, `metadata.correlationId`, 컬렉션 `items/pagination`을 사용하지만 구현과 OpenAPI는 `data` 페이로드와 `metadata {requestId, apiVersion, timestamp}`만, 목록 키는 리소스별. 상관 ID는 응답 헤더로만 제공. | `docs/10_API_Specification.md:271-313`; `shared/api/ApiMetadata.java:7`; `contracts/openapi/devpath-openapi.yaml:1793-1805` |
| D4 | 오류 형태 | docs/10 오류 모델은 `userSafeDetail`, `fieldErrors`, `retryable`, `providerCategory`, `documentationRef`를 포함하지만 구현과 OpenAPI는 `code`, `message`만 반환. | `docs/10_API_Specification.md:342-364`; `shared/api/ApiErrorResponse.java:8`; `contracts/openapi/devpath-openapi.yaml:3482-3490` |
| D5 | 작업 폴링 URL | docs/10 예시는 범용 `/api/v1/jobs/{jobId}`이지만 구현은 유형별 경로이며 `/api/v1/jobs` 엔드포인트는 없음. | `docs/10_API_Specification.md:323`; `repository/application/RepositorySyncJobView.java:28`; `analysis/application/AnalysisJobView.java:51` |
| D6 | 작업 상태 대소문자/값 | docs/10 작업 상태는 `cancelled`를 포함한 소문자. AI 생성 작업은 대문자 `QUEUED…CANCELED`(L 하나), 대시보드 작업 항목은 대문자 상태와 `jobType` `ANALYSIS`를 쓰는 반면 분석 작업 리소스는 소문자와 `REPOSITORY_ANALYSIS`. 내부적으로 불일치하지만 리소스별로는 OpenAPI와 코드가 일치. | `docs/10_API_Specification.md:867`; `contracts/openapi/devpath-openapi.yaml:2127-2128,3056-3058,3276`; `dashboard/application/DashboardApplicationService.java:173-177` |
| D7 | docs/10 내 중복 API ID | 같은 메서드+경로에 docs/10이 두 ID를 부여: 동기화 `API-INT-006`/`API-REP-006`, Notion 가져오기 `API-INT-012`/`API-KNW-003`, 직무 목표 `API-CAR-003`/`API-ID-006`, 회사 목표 `API-CMP-003`/`API-ID-007`. OpenAPI는 `API-REP-006`, `API-KNW-003`, `API-ID-006`, `API-ID-007` 사용. | `docs/10_API_Specification.md:463,474,618,624`; `contracts/openapi/devpath-openapi.yaml:660,1560,195,230` |
| D8 | 오래된 구현 서술 | docs/10 §43은 identity 기반 외 카탈로그 operation이 "remain specification-only"라고 서술하나, OpenAPI의 구현된 70개 ID operation과 모순. | `docs/10_API_Specification.md:1460` |
| D9 | GitHub 연동 콜백 인증 | OpenAPI는 API-INT-002를 `sessionCookie` 보호와 `401` 응답으로 표시하지만, 경로는 `permitAll`이고 컨트롤러가 모든 실패(세션/state 없음 포함)를 `302 …githubConnection=failed` 리다이렉트로 변환하므로 401이 발생하지 않음. Notion 콜백은 `permitAll`이 아니며 세션 없이 401 반환. | `contracts/openapi/devpath-openapi.yaml:334,342-343`; `identity/config/SecurityConfiguration.java:61`; `integration/adapter/in/web/GitHubIntegrationController.java:75-87` |
| D10 | Notion 가져오기 403 | OpenAPI는 "missing CSRF or page permission"에 `403`을 명시하지만, 소유/공유되지 않은 페이지는 `NotionConnectionNotFoundException` → `404`이므로 403은 CSRF에서만 발생. | `contracts/openapi/devpath-openapi.yaml:1577`; `integration/adapter/out/notion/NotionApiAdapter.java:138-143`; `knowledge/adapter/in/web/KnowledgeExceptionHandler.java:22-25` |
| D11 | 프로필 404 미기재 | API-ID-002/003은 `404 RESOURCE_NOT_FOUND`를 반환할 수 있으나(docs/10은 API-ID-002에 명시) OpenAPI는 200/401(GET), 200/400/401/403(PATCH)만 선언. | `docs/10_API_Specification.md:436`; `contracts/openapi/devpath-openapi.yaml:111-121,135-155`; `identity/adapter/in/web/IdentityApiExceptionHandler.java:30-33` |
| D12 | 로드맵 보관 Idempotency-Key | 헤더는 필수(OpenAPI 패턴 `^[A-Za-z0-9._:-]+$`)이지만 컨트롤러는 `@Size(1,128)`만 검증하고 키를 서비스에 전달하지 않음. 멱등성은 보관 상태로 보장. 400이 선언되지 않았고 매핑된 어드바이스는 not-found만 처리하므로 헤더 누락/오류 시 프레임워크 기본 오류 본문 반환. | `contracts/openapi/devpath-openapi.yaml:1508,1509-1521`; `learning/adapter/in/web/LearningRoadmapController.java:27`; `recommendation/adapter/in/web/RecommendationApiExceptionHandler.java:15-16` |
| D13 | 헤더 누락 오류 본문 | `RepositoryExceptionHandler`가 `MissingRequestHeaderException`을 매핑하지 않아 `Idempotency-Key` 없는 `POST …/sync`는 `ErrorEnvelope` 대신 프레임워크 기본 400 본문 반환(분석·지식·AI 어드바이스는 매핑함). `@Validated`가 아닌 컨트롤러(분석, 지식, AI)의 헤더 제약 어노테이션은 Spring MVC 메서드 검증이 적용하며 그 예외는 해당 어드바이스가 매핑하지 않음. 서비스에 도달한 키는 서비스 검증으로 `VALIDATION_ERROR` 반환. 코드에서 추론했으며 실행하지 않음. | `repository/adapter/in/web/RepositoryExceptionHandler.java:55-61`; `repository/adapter/in/web/RepositorySynchronizationController.java:37`; `analysis/adapter/in/web/AnalysisController.java:28-42`; `repository/application/RepositorySynchronizationApplicationService.java:50-51` |
| D14 | 멱등성 범위 | docs/10은 API-INT-001/005/008 등에도 `Idempotency-Key`를 요구하지만, 구현과 OpenAPI는 §1.9의 6개 명령에만 요구(가져오기·인가에는 없음). | `docs/10_API_Specification.md:458,462,470`; `repository/adapter/in/web/RepositoryController.java:33-44` |
| D15 | UUID가 아닌 경로/쿼리 ID | 경로나 쿼리의 잘못된 UUID는 매핑된 어드바이스 이전에 타입 변환에서 실패해 `ErrorEnvelope`가 아닌 프레임워크 기본 400 본문 반환. OpenAPI는 대부분의 UUID 경로 GET에 400을 선언하지 않음. 코드에서 추론했으며 실행하지 않음. | 예: `rule/adapter/in/web/RuleEvaluationController.java:26-28`; `contracts/openapi/devpath-openapi.yaml:1010-1026` |

검증 방법: OpenAPI 전체를 읽고, `d6753b9` 시점의 모든 웹 컨트롤러, 예외 어드바이스, `SecurityConfiguration`,
`shared/api` 클래스, 작업 view 레코드를 읽었습니다. 이 레퍼런스를 위해 테스트나 HTTP 호출은 실행하지 않았으며,
"추론" 표시 항목은 Spring 프레임워크 동작에서 도출한 것이므로 변경 전에 테스트로 확인해야 합니다.
