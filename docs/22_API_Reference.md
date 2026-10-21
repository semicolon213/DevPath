# DevPath API Reference (Implemented Endpoints)

> **Authority notice.** `docs/10_API_Specification.md` remains the canonical, authoritative API specification and
> `contracts/openapi/devpath-openapi.yaml` (info.version `0.27.0`) remains the machine-readable contract. This document is a
> non-authoritative, implementation-verified **reference** derived from those two sources plus the backend web adapters
> (`backend/src/main/java/com/devpath/**/adapter/in/web/**`, `identity/config/SecurityConfiguration.java`,
> `platform/health/InternalHealthController.java`, `shared/api/**`) at commit `d6753b9`, updated on 2026-10-21 for the
> API-FIX change set (backend, OpenAPI, frontend; reviewed snapshot based on `5eaf85b`). Where this reference and the
> authoritative sources disagree, the authoritative sources win; observed differences and their resolution status are
> listed in [Appendix A](#appendix-a-contract-verification).

## 1. Overview

### 1.1 Base path and versioning

| Item | Implemented behaviour |
|---|---|
| Business API base path | `/api/v1` (URI versioning, docs/10 §5.1, §29) |
| Framework/infrastructure routes | `/internal/health`, `/oauth2/authorization/github`, `/login/oauth2/code/github` (outside `/api/v1`) |
| Content type | `application/json` request and response bodies |
| `metadata.apiVersion` | Always `"v1"` (`shared/api/ApiMetadata.java:7`) |
| Compatibility | Additive fields/endpoints are compatible; clients must tolerate unknown enum values and ignore unknown response fields (docs/10 §29) |

### 1.2 Authentication

| Item | Implemented behaviour |
|---|---|
| Login | GitHub OAuth2 Login owned by Spring Security: browser navigates to `GET /oauth2/authorization/github`; GitHub returns to `GET /login/oauth2/code/github`; success/failure redirect to the configured frontend URLs (`SecurityConfiguration.java:67-74`). |
| Session | Opaque server-side session stored in PostgreSQL (`spring.session.store-type: jdbc`). Cookie `DEVPATH_SESSION` (configurable), `HttpOnly`, `Secure` by default, `SameSite=Lax`, path `/` (`application.yml:36-51`). Session ID is rotated on login (`migrateSession`). |
| Timeouts | Idle timeout default `30m`; absolute timeout default `12h` enforced by `AbsoluteSessionTimeoutFilter` (`application.yml:38,81`). |
| Session bootstrap | `GET /api/v1/users/me`: `200` = authenticated, `401` = anonymous. A network failure is a separate client state. |
| Logout | `POST /api/v1/session/logout` (CSRF required) invalidates the session, clears authentication, deletes the session cookie and returns `204` (`SecurityConfiguration.java:75-81`, `AuditedLogoutSuccessHandler.java:45`). |
| Unauthenticated access | Any route except the public list below returns `401` with `ErrorEnvelope` code `AUTHENTICATION_REQUIRED` (`SecurityConfiguration.java:83-94`). |
| Public (no session) routes | `/internal/health`, `/oauth2/authorization/github`, `/login/oauth2/code/github`, `/api/v1/integrations/github/callback`, `/error`, and `GET /api/v1/csrf` (`SecurityConfiguration.java:57-64`). |
| Bearer tokens | Not used. Provider (GitHub/Notion) tokens are never returned and never accepted as DevPath credentials. |

### 1.3 CSRF

| Item | Implemented behaviour |
|---|---|
| Scope | Spring Security CSRF protection is enabled for all state-changing methods (`POST`, `PUT`, `PATCH`, `DELETE`). |
| Token issue | `GET /api/v1/csrf` returns `{headerName, parameterName, token}`; the token is also held in a cookie-backed repository (`CookieCsrfTokenRepository`, cookie path `/`) (`SecurityConfiguration.java:45-47`, `CsrfController.java:14-20`). |
| Header | Send the token in `X-CSRF-TOKEN`. |
| Failure | Missing/invalid token returns `403` with code `AUTHORIZATION_DENIED` (`SecurityConfiguration.java:95-106`). |

### 1.4 CORS

| Item | Implemented behaviour (`SecurityConfiguration.java:114-134`) |
|---|---|
| Allowed origins | Exactly one configured frontend origin (`devpath.security.frontend-origin`); no wildcard. |
| Credentials | Allowed (`allowCredentials=true`). |
| Methods | `GET, POST, PUT, PATCH, DELETE, OPTIONS` |
| Request headers | `Accept`, `Content-Type`, `Idempotency-Key`, `X-CSRF-TOKEN`, `X-Request-Id`, `X-Correlation-Id` |
| Exposed headers | `X-Request-Id`, `X-Correlation-Id` |
| Preflight cache | `3600` seconds |

### 1.5 Request/correlation identifiers

`X-Request-Id` and `X-Correlation-Id` are optional. Values must match `[A-Za-z0-9][A-Za-z0-9._-]{0,63}`; invalid or
absent values are replaced (request ID by a generated UUID, correlation ID by the request ID). Both resolved values are
returned as response headers, and `metadata.requestId` uses the same request ID (`shared/api/RequestIds.java`,
`RequestCorrelationFilter.java:47-52`).

### 1.6 Response envelope

Every JSON business response uses `{ "data": <payload>, "metadata": ApiMetadata }` (`shared/api/ApiResponse.java`).

| `metadata` field | Type | Notes |
|---|---|---|
| `requestId` | string | Resolved request identifier |
| `apiVersion` | string | `"v1"` |
| `timestamp` | date-time | Server time of the response |

Exceptions: `GET /internal/health` returns a bare `{status, application, timestamp}` object; OAuth callbacks return
`302`; logout returns `204` with no body.

### 1.7 Error envelope and error codes

Errors use `{ "error": { "code", "message" }, "metadata": ApiMetadata }` (`shared/api/ApiErrorResponse.java`). Messages
are fixed, user-safe strings; provider payloads, SQL errors, and stack traces are never returned.

| Code | HTTP | Produced by (implemented) |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | Bean-validation failures, unreadable JSON, missing or invalid headers, non-UUID path/query IDs, unsupported values, invalid cursor/limit |
| `AUTHENTICATION_REQUIRED` | 401 | No valid session; disabled or missing account on identity endpoints |
| `AUTHORIZATION_DENIED` | 403 | CSRF failure / Spring Security access denial |
| `RESOURCE_NOT_FOUND` | 404 | Resource absent **or owned by another user** (ownership never revealed) |
| `DUPLICATE_REQUEST` | 409 | `Idempotency-Key` reused by the same owner for a different command (sync, analysis, knowledge import/re-index, AI generation) |
| `RESOURCE_CONFLICT` | 409 | Concurrent write races (`DataIntegrityViolationException`, optimistic locking) |
| `RATE_LIMIT_EXCEEDED` | 429 | GitHub or Notion provider quota exhausted; `Retry-After` and (GitHub) `X-RateLimit-Reset` headers when known |
| `DEPENDENCY_UNAVAILABLE` | 503 | GitHub/Notion unavailable, configuration missing, reconnection required |
| `KNOWLEDGE_RETRIEVAL_FAILED` | 503 | Embedding, vector index, or retained object content unavailable during knowledge search |

Durable job resources additionally carry safe job-level `errorCode` values (for example `COLLECTION_LIMIT_EXCEEDED`,
`RATE_LIMIT_EXCEEDED`, `SOURCE_PERMISSION_CHANGED`, `SOURCE_UNAVAILABLE`, `INGESTION_FAILED`) — see §3.

Missing or invalid headers and non-UUID path/query IDs return `400 VALIDATION_ERROR` with `ErrorEnvelope` through the
shared `shared/api/ApiValidationExceptionHandler.java` (lowest precedence; a module advice that maps the same exception
first returns the same status and code). OpenAPI declares these through the reusable `ValidationError` response.

### 1.8 Pagination

Cursor pagination is implemented only on repository and analysis history lists:

| Parameter | Type | Default | Constraint |
|---|---|---|---|
| `limit` | integer | 20 | 1–100 |
| `cursor` | string | none | Opaque; taken from the previous page's `nextCursor` |

Paged payloads contain `<items array>`, `limit`, `nextCursor` (null on the last page) and `totalCount`. All other list
endpoints return a complete, bounded array without pagination.

### 1.9 Idempotency

`Idempotency-Key` (1–128 chars, `^[A-Za-z0-9._:-]+$` per OpenAPI) is required on:
`POST /repositories/{id}/sync`, `POST /analyses`, `POST /knowledge-documents/imports/notion`,
`POST /knowledge-documents/{id}/reindex`, `POST /generation-requests`, `POST /learning-roadmaps/{id}/archive`.

| Situation | Implemented result |
|---|---|
| Same owner + key + same command | Original job returned (`202`) |
| Same owner + key, different command | `409 DUPLICATE_REQUEST`; the frontend shows Korean guidance to refresh and retry |
| Roadmap archive | Key validated (1–128 chars, pattern) but not stored; idempotency comes from the archived state |
| Equivalent active command with a different key | Existing active job reused (sync, analysis, knowledge import) |
| Concurrent race on unique constraints | `409 RESOURCE_CONFLICT` |

## 2. Endpoint catalogue

Conventions: **Auth** = session cookie required unless marked *public*. **CSRF** = `X-CSRF-TOKEN` required.
**Owner** = resources are scoped to the authenticated user; foreign IDs return `404`. All path IDs are UUIDs unless
stated. Every authenticated endpoint can also return `401 AUTHENTICATION_REQUIRED`, every CSRF endpoint
`403 AUTHORIZATION_DENIED`, and every endpoint with a required header or UUID path/query ID `400 VALIDATION_ERROR`;
these are omitted from the "Errors" column for brevity.

### 2.1 Session, identity, and onboarding

| API ID | Method + path | Purpose | Request | Success (`data`) | Errors |
|---|---|---|---|---|---|
| — | `GET /internal/health` *(public)* | Non-sensitive health | — | `200` bare `{status:"ok", application, timestamp}` | — |
| — | `GET /oauth2/authorization/github` *(public)* | Start GitHub login | — | `302` to GitHub | — |
| — | `GET /login/oauth2/code/github` *(public)* | Complete login; provisions user, rotates session | query `code`, `state` (req.), `error` | `302` to frontend success/failure URL | — |
| — | `GET /api/v1/csrf` *(public)* | Issue CSRF token | — | `200` `{headerName, parameterName, token}` | — |
| — | `POST /api/v1/session/logout` (CSRF) | End session, delete cookie | — | `204` | 401, 403 |
| API-ID-001 | `GET /api/v1/users/me` | Session bootstrap | — | `200` `{userId, displayName, avatarUrl?, status: ACTIVE, authenticationProvider: GITHUB, createdAt}` | 401 (also for disabled/missing account) |
| API-ID-002 | `GET /api/v1/users/me/profile` | Read profile | — | `200` `UserProfile {profileId, displayName, careerStage?, bio?, updatedAt}` | 404 `RESOURCE_NOT_FOUND` |
| API-ID-003 | `PATCH /api/v1/users/me/profile` (CSRF) | Update profile | `displayName` string, required, 1–120; `careerStage` enum `STUDENT\|ENTRY_LEVEL\|JUNIOR\|MID_LEVEL\|SENIOR`, nullable; `bio` string ≤1000, nullable | `200` `UserProfile` | 400, 404 |
| API-ID-004 | `GET /api/v1/users/me/connections` | Provider connections | — | `200` `{connections: [{connectionId, provider: GITHUB\|NOTION, status: ACTIVE\|EXPIRED\|REVOKED, scopes[], connectedAt, expiresAt?}]}` (inactive → empty `scopes`) | — |
| API-ID-005 | `GET /api/v1/users/me/preferences` | Career/company targets | — | `200` `{careerId?, companyId?, updatedAt?}` | — |
| API-ID-006 | `PUT /api/v1/users/me/preferences/career` (CSRF) | Set career target (validated against career catalog) | `careerId` string, required, 1–64 | `200` `UserPreference` | 400 unsupported target |
| API-ID-007 | `PUT /api/v1/users/me/preferences/company` (CSRF) | Set company target (validated against company catalog) | `companyId` string, required, 1–64 | `200` `UserPreference` | 400 unsupported target |
| API-ID-010 | `GET /api/v1/users/me/onboarding-progress` | Deterministic onboarding projection (no scoring) | — | `200` `{status: GETTING_STARTED\|IN_PROGRESS\|DASHBOARD_READY, completedStepCount, totalStepCount: 8, nextStep, steps[8] {step, requirement: REQUIRED\|RECOMMENDED\|OPTIONAL, status: COMPLETE\|INCOMPLETE, resourceId?, actionPath}, generatedAt}` | — |

### 2.2 Integrations (GitHub, Notion)

| API ID | Method + path | Purpose | Request | Success (`data`) | Errors |
|---|---|---|---|---|---|
| API-INT-001 | `POST /api/v1/integrations/github/authorize` (CSRF) | Start GitHub App user authorization; state bound to session+user, 10-minute TTL | — | `200` `{authorizationUrl}` | 503 config unavailable |
| API-INT-002 | `GET /api/v1/integrations/github/callback` *(public, `permitAll`; uses session state)* | Complete authorization; single-use state | query `state` (OpenAPI: required), `code`, `error` | Always `302` to `{frontend}/settings/integrations?githubConnection=success\|failed\|installation-required` | missing/invalid state or missing session → `302 …=failed` (never 401) |
| API-INT-003 | `DELETE /api/v1/integrations/github` (CSRF) | Disconnect; local credential removed, remote revocation attempted | — | `200` `ConnectedAccount` (status `REVOKED`) | 404 no active connection, 503 |
| API-INT-004 | `GET /api/v1/integrations/github/repositories` | List repositories accessible through installed GitHub Apps | — | `200` `{repositories: [{providerRepositoryId, name, fullName, owner, privateRepository, archived, defaultBranch, htmlUrl}]}` | 429 (+`Retry-After`, `X-RateLimit-Reset`), 503 |
| API-INT-008 | `POST /api/v1/integrations/notion/authorize` (CSRF) | Start Notion authorization (10-minute state) | — | `200` `{authorizationUrl}` | 503 |
| API-INT-009 | `GET /api/v1/integrations/notion/callback` | Complete Notion authorization (requires session) | query `state` (req.), `code`, `error` | `302` to `{frontend}/settings/integrations?notionConnection=success\|failed` | 401 without session |
| API-INT-010 | `DELETE /api/v1/integrations/notion` (CSRF) | Disconnect Notion | — | `200` `ConnectedAccount` | 404 |
| API-INT-011 | `GET /api/v1/integrations/notion/workspaces` | Discover/replace workspace page metadata (no content) | — | `200` `{workspaces: [{connectionId, workspaceId, workspaceName, workspaceIconUrl?, status: ACTIVE, connectedAt, discoveredAt, pages[≤1000] {providerPageId, title, objectType: PAGE\|DATA_SOURCE, url?, lastEditedAt, inTrash}}]}` | 404, 429 (+`Retry-After`), 503 |

### 2.3 Repositories, synchronization, and snapshots

`Repository` = `{repositoryId, providerRepositoryId, name, fullName, owner, visibility: PUBLIC|PRIVATE, defaultBranch,
providerArchived, lifecycle: DISCOVERED|ACTIVE|ARCHIVED|DELETED_EXTERNALLY, syncStatus: NOT_SYNCED|SYNCHRONIZED|FAILED,
htmlUrl, discoveredAt, lastSyncedAt?, currentSnapshotId?}`.

| API ID | Method + path | Purpose | Request | Success (`data`) | Errors |
|---|---|---|---|---|---|
| API-INT-005 | `POST /api/v1/repositories/imports` (CSRF) | Import an accessible GitHub repository (re-verified server-side; duplicates return existing) | `providerRepositoryId` string, required, `^[1-9][0-9]{0,63}$` | `201` `Repository` | 400 invalid/inaccessible, 409, 429, 503 |
| API-REP-001 | `GET /api/v1/repositories` | Owner repository page | query `limit`, `cursor`, `includeArchived` (bool, default `false`) | `200` `{repositories[], limit, nextCursor?, totalCount}` | 400 limit/cursor |
| API-REP-002 | `GET /api/v1/repositories/{repositoryId}` | Repository detail | — | `200` `Repository` | 404 |
| API-REP-004 | `POST /api/v1/repositories/{repositoryId}/archive` (CSRF) | Archive locally | — | `200` `Repository` | 404, 409 |
| API-REP-005 | `POST /api/v1/repositories/{repositoryId}/restore` (CSRF) | Restore local archive (not provider-archived) | — | `200` `Repository` | 400, 404, 409 |
| API-REP-006 | `POST /api/v1/repositories/{repositoryId}/sync` (CSRF, Idempotency-Key) | Queue durable full sync; no GitHub call on request thread | — | `202` `RepositorySyncJob` (§3) | 400 key/lifecycle, 404, 409 key reused, 503 |
| API-INT-007 | `GET /api/v1/repository-sync-jobs/{jobId}` | Poll sync job | — | `200` `RepositorySyncJob` | 404 |
| API-REP-007 | `GET /api/v1/repositories/{repositoryId}/snapshots` | Snapshot history (newest first) | — | `200` `{snapshots: RepositorySnapshot[]}` | 404 |
| API-REP-008 | `GET /api/v1/repositories/{repositoryId}/snapshots/{snapshotId}` | Immutable snapshot metadata | — | `200` `{snapshotId, repositoryId, capturedAt, sourceRevision, status: READY\|FAILED\|SUPERSEDED\|DELETED_BY_POLICY, immutable, contentHash, branchCount, commitCount, pullRequestCount, issueCount, documentCount}` | 404 |
| API-REP-009 | `GET /api/v1/repositories/{repositoryId}/technologies` | Detected technologies from current snapshot (no score) | — | `200` `{repositoryId, snapshotId, extractorVersion, taxonomyVersion, primaryLanguage?, technologies[] {name, category: LANGUAGE\|FRAMEWORK\|DATABASE, evidenceLabel, byteCount?, percentage?, taxonomyStatus, evidenceType, evidencePaths[]}}` | 404 |
| API-REP-011 | `GET /api/v1/repositories/{repositoryId}/evidence` | Engineering evidence signals + activity timeline (no score) | — | `200` `{repositoryId, snapshotId, extractorVersion, categories[] {category, label, signals[] {signalKey, label, present, count, observedValue?, evidencePaths[≤20]}}, activityTimeline {extractorVersion, scope, measuredAt, latestActivityAt?, daysSinceLatestActivity?, totalEventCount, truncated, events[≤100]}}` | 404 |

### 2.4 Analysis and rule evaluation

| API ID | Method + path | Purpose | Request | Success (`data`) | Errors |
|---|---|---|---|---|---|
| API-ANA-001 | `POST /api/v1/analyses` (CSRF, Idempotency-Key) | Queue deterministic analysis | `repositoryId` uuid, required; `snapshotId` uuid, nullable; `analysisScope` `REPOSITORY_BASELINE` (default); `targetCareerId` must be null (non-null → 400) | `202` `AnalysisJob` (§3) | 400, 404, 409 |
| API-ANA-002 | `GET /api/v1/analysis-jobs/{jobId}` | Poll analysis job | — | `200` `AnalysisJob` | 404 |
| API-ANA-003 | `GET /api/v1/analyses/{analysisId}` | Completed immutable analysis | — | `200` `{analysisId, repositoryId, snapshotId, evaluationId, skillMatrixId, analysisScope, currentForRepository, completedAt}` | 404 |
| API-ANA-007 | `GET /api/v1/analyses` | Owner analysis history (newest first) | query `limit`, `cursor` | `200` `{analyses[] (AnalysisResult + repositoryFullName, overallScore, confidence, ruleSetVersion, policyVersion), limit, nextCursor?, totalCount}` | 400 |
| API-REP-012 | `GET /api/v1/repositories/{repositoryId}/analyses` | Repository analysis history | query `limit`, `cursor` | `200` same as API-ANA-007 | 400, 404 |
| API-ANA-008 | `GET /api/v1/analyses/compare` | Two stored analyses side by side (no delta) | query `analysisId` repeated exactly twice, distinct | `200` `{analyses[2]}` in request order | 400, 404 |
| API-ANA-004 | `GET /api/v1/rule-evaluations/{evaluationId}` | Immutable rule evaluation | — | `200` `{evaluationId, snapshotId, ruleSetVersionId, ruleSetVersion, formulaLibraryVersion, extractorVersion, overallScore, confidence, evidenceSummary, categoryScores[], warnings[], completedAt}` | 404 |
| API-ANA-005 | `GET /api/v1/rule-evaluations/{evaluationId}/score-breakdown` | Category/rule breakdown with trace | — | `200` `{evaluationId, overallScore, confidence, categories[] {category, score, weight, confidence, ruleResults[] {ruleId, ruleVersion, status, rawValue, score, weight, formulaId, trace, evidenceReferences[]}, missingEvidence[]}}` | 404 |
| API-ANA-006 | `GET /api/v1/rule-evaluations/{evaluationId}/evidence` | Evidence linked to rules | — | `200` `{evaluationId, evidence[] {evidenceId, ruleId, contributionRole: DIRECT\|SUPPORTING\|MISSING, evidenceType, sourceReference, observedFactSummary, confidence}}` | 404 |

### 2.5 Skill Matrix and skills

| API ID | Method + path | Purpose | Request | Success (`data`) | Errors |
|---|---|---|---|---|---|
| API-SKL-001 | `GET /api/v1/skill-matrices/current` | Current Skill Matrix | — | `200` `SkillMatrix {skillMatrixId, evaluationId, policyVersion, ruleSetVersion, status: CURRENT\|SUPERSEDED\|ARCHIVED, skills[] SkillAssessment, strengths[], weaknesses[], generatedAt}` | 404 none yet |
| API-SKL-002 | `GET /api/v1/skill-matrices/{skillMatrixId}` | Historical matrix | — | `200` `SkillMatrix` | 404 |
| API-SKL-003 | `GET /api/v1/skill-matrices/compare` | Two stored matrices (no delta) | query `skillMatrixId` repeated exactly twice, distinct | `200` `{matrices[2]}` | 400, 404 |
| API-SKL-004 | `GET /api/v1/skills/{skillId}` | Current assessment for a stable skill | — | `200` `{skillMatrixId, evaluationId, policyVersion, ruleSetVersion, matrixStatus: CURRENT, generatedAt, skill: SkillAssessment}` | 404 |
| API-SKL-005 | `GET /api/v1/skills/{skillId}/evidence` | Evidence for current assessment | — | `200` `{skillId, skillAssessmentId, skillMatrixId, evidence[] {evidenceId, snapshotId, evidenceType, sourceReference, observedFactSummary, confidence}}` | 404 |

`SkillAssessment` = `{assessmentId, skillId, skillKey, skillName, category, score, level: NONE|BEGINNER|DEVELOPING|COMPETENT|STRONG,
confidence, evidenceStatus: NONE|INSUFFICIENT|SUFFICIENT|null, strength, weakness,
growthTrend: UNAVAILABLE|IMPROVING|STABLE|DECLINING, aggregateRuleResultReference,
evidenceIds[], repositoryIds[], recommendationInputFacts[], ruleSetVersion}`. `evidenceStatus` (D-4) is calculated by the
Rule Engine under `skill-matrix-v3` and is `null` for matrices generated under earlier policy versions.

### 2.6 Careers, companies, and readiness

| API ID | Method + path | Purpose | Request | Success (`data`) | Errors |
|---|---|---|---|---|---|
| API-CAR-001 | `GET /api/v1/careers` | Supported career catalog | — | `200` `{catalogVersion, careers[] {careerId, name, localizedName, status: SUPPORTED, profileVersion, purpose}}` | — |
| API-CAR-002 | `GET /api/v1/careers/{careerId}` | Active career profile | path `careerId` `^[a-z][a-z0-9-]{1,63}$` | `200` summary + `{careerProfileVersionId, coreTechnologies[], requiredCompetencies[], preferredCompetencies[], evaluationCategories[], priorityWeights{cat: HIGH\|MEDIUM\|LOW}, roadmapTemplate[], effectiveAt}` | 400, 404 |
| API-CAR-004 | `GET /api/v1/career-readiness/current` | Current readiness for selected career | — | `200` `CareerReadiness` | 404 |
| API-CAR-005 | `GET /api/v1/career-readiness/{careerReadinessId}` | Historical readiness | — | `200` `CareerReadiness` | 404 |
| API-CAR-006 | `GET /api/v1/career-readiness/{careerReadinessId}/skill-gaps` | Ordered gaps | — | `200` `{careerReadinessId, skillGaps[]}` | 404 |
| API-CMP-001 | `GET /api/v1/companies` | Supported company catalog | — | `200` `{catalogVersion, companies[] {companyId, name, localizedName, status: SUPPORTED, profileVersion, engineeringCulture}}` | — |
| API-CMP-002 | `GET /api/v1/companies/{companyId}` | Active company profile (generic emphasis, not hiring criteria) | path `companyId` `^[a-z][a-z0-9-]{1,63}$` | `200` summary + `{companyProfileVersionId, technologyFocus[], preferredCompetencies[], recommendationPriorities[], skillEmphasis[], weightOverrides{cat: INCREASE}, effectiveAt}` | 400, 404 |

`CareerReadiness` = `{careerReadinessId, skillMatrixId, careerId, careerProfileVersionId, careerProfileVersion,
readinessPolicyVersion, ruleSetVersion, status: COMPLETED|INSUFFICIENT_EVIDENCE, readinessScore?, readinessLevel?,
confidence, unavailableCategories[], skillGaps[], assessedAt}`. `SkillGap` = `{skillGapId, skillId, skillKey, category,
actualScore, actualLevel, expectedMinimum (60), gapState: MISSING|WEAK|PARTIAL|SUFFICIENT|STRONG, careerWeight, evidenceIds[]}`.

### 2.7 Recommendations and learning roadmaps

| API ID | Method + path | Purpose | Request | Success (`data`) | Errors |
|---|---|---|---|---|---|
| API-REC-002 | `GET /api/v1/recommendations/current` | Current recommendation set | — | `200` `{recommendationSetId, careerReadinessId, policyVersion, status: PUBLISHED\|SUPERSEDED, recommendations[], generatedAt}` | 404 |
| API-REC-004 | `GET /api/v1/recommendations` | Set history (newest first) | — | `200` `{recommendationSets[]}` | — |
| API-REC-003 | `GET /api/v1/recommendations/{recommendationId}` | Recommendation detail | — | `200` `{recommendationId, skillGapId, category, type: STUDY\|PROJECT\|ARCHITECTURE\|PORTFOLIO, priority: CRITICAL\|HIGH\|MEDIUM\|LOW, rationaleCode, title, completionCriteria, expectedEvidence[], evidenceIds[], effortHours, position, status}` | 404 |
| API-REC-008 | `GET /api/v1/recommendations/{recommendationId}/evidence` | Linked evidence | — | `200` `{recommendationId, evidence[] {evidenceId, evidenceType, sourceReference, observedFactSummary, confidence, createdAt}}` | 404 |
| API-LRN-003 | `GET /api/v1/learning-roadmaps` | Roadmap history incl. archived | — | `200` `{roadmaps[]}` | — |
| API-LRN-002 | `GET /api/v1/learning-roadmaps/active` | Active roadmap | — | `200` `LearningRoadmap` | 404 |
| API-LRN-004 | `GET /api/v1/learning-roadmaps/{roadmapId}` | Roadmap detail | — | `200` `LearningRoadmap` | 404 |
| API-LRN-009 | `POST /api/v1/learning-roadmaps/{roadmapId}/archive` (CSRF, Idempotency-Key) | Archive; repeat returns unchanged archived state | — | `200` `LearningRoadmap` | 400 invalid key, 404 |

`LearningRoadmap` = `{roadmapId, recommendationSetId, policyVersion, status: CREATED|IN_PROGRESS|COMPLETED|ARCHIVED,
progressPercent, milestones[] {milestoneId, position, category, title, status}, steps[] {roadmapStepId, milestoneId,
recommendationId, position, category, title, difficulty, effortHours, prerequisiteStepIds[], completionCriteria,
expectedEvidence[], status}, generatedAt, updatedAt}`. There is no public endpoint that creates recommendations or roadmaps.

### 2.8 Knowledge

| API ID | Method + path | Purpose | Request | Success (`data`) | Errors |
|---|---|---|---|---|---|
| API-KNW-003 | `POST /api/v1/knowledge-documents/imports/notion` (CSRF, Idempotency-Key) | Import an authorized Notion page | `connectionId` uuid, required; `providerPageId` string, required, 1–255 | `202` `KnowledgeIngestionJob` (§3) | 400, 404 connection or accessible page not found (403 is CSRF only), 409, 503 |
| API-KNW-008 | `GET /api/v1/knowledge-ingestion-jobs/{jobId}` | Poll ingestion job | — | `200` `KnowledgeIngestionJob` | 404 |
| API-KNW-004 | `GET /api/v1/knowledge-documents` | Owner documents | — | `200` `{documents[] {documentId, sourceType: NOTION, sourceObjectId, title, status: ACTIVE\|ARCHIVED, currentVersionId?, chunkCount, createdAt, updatedAt}}` | — |
| API-KNW-005 | `GET /api/v1/knowledge-documents/{documentId}` | Document metadata | — | `200` `KnowledgeDocument` | 404 |
| API-KNW-010 | `GET /api/v1/knowledge-documents/{documentId}/chunks` | Chunk metadata (no content, no vectors) | — | `200` `{chunks[] {chunkId, position, heading?, contentHash, tokenEstimate, status: INDEXED\|STALE\|DELETED}}` | 404 |
| API-KNW-006 | `POST /api/v1/knowledge-documents/{documentId}/archive` (CSRF) | Archive and remove from retrieval | — | `200` `KnowledgeDocument` | 404 |
| API-KNW-011 | `POST /api/v1/knowledge-documents/{documentId}/reindex` (CSRF, Idempotency-Key) | Re-index from Notion | — | `202` `KnowledgeIngestionJob` | 400, 404, 409, 503 |
| API-KNW-009 | `POST /api/v1/knowledge-search` (CSRF) | Owner-scoped semantic search | `query` string, required, 1–500; `filters.sourceTypes` ≤1 of `NOTION`; `filters.documentIds` ≤20 uuid; `limit` 1–20 (default 5); `contextPurpose` `USER_SEARCH` (default) | `200` `{retrievalResultId, retrievalType: SEMANTIC, policyVersion: knowledge-semantic-v1, contextPurpose, appliedFilters, results[≤20] {chunkId, documentId, documentTitle, sourceType, sourceObjectId, sourceUrl?, heading?, excerpt (≤500), relevance 0–1, tokenEstimate, freshness: FRESH}, resultCount, durationMs, generatedAt}` | 400, 503 `KNOWLEDGE_RETRIEVAL_FAILED` |

### 2.9 AI generation

| API ID | Method + path | Purpose | Request | Success (`data`) | Errors |
|---|---|---|---|---|---|
| API-AI-001 | `POST /api/v1/generation-requests` (CSRF, Idempotency-Key) | Request grounded generation | `taskType` + `outputType` (required, must be a supported pair, below); `sourceResourceRefs` uuid[], exactly 1 | `202` `GenerationJob` | 400, 404 source not owned, 409 |
| API-AI-002 | `GET /api/v1/generation-jobs/{jobId}` | Poll generation job | — | `200` `GenerationJob` | 404 |
| API-AI-003 | `POST /api/v1/generation-jobs/{jobId}/cancel` (CSRF) | Cancel; terminal jobs returned unchanged | body `{}` (required, no fields) | `200` `GenerationJob` | 404 |
| API-AI-005 | `GET /api/v1/generated-artifacts/{artifactId}` | Validated artifact | — | `200` `{artifactId, type, status: VALIDATED, provenance {skillMatrixId, analysisId?, careerReadinessId?, learningRoadmapId?, promptContextId, templateVersion, provider: OLLAMA, model, contextHash, generatedAt}, validation, contentRef, content}` | 404 |
| API-AI-007 | `GET /api/v1/generated-artifacts/{artifactId}/validation` | Validation provenance | — | `200` `{status: PASSED\|REJECTED, validatorVersion, validatedAt, violations[]}` | 404 |

Supported `taskType` → `outputType` pairs (`AiGenerationApplicationService.java:363-368`):
`SKILL_ANALYSIS_EXPLANATION→SKILL_EXPLANATION`, `REPOSITORY_REVIEW→REPOSITORY_REVIEW`,
`REPOSITORY_SUMMARY→REPOSITORY_SUMMARY`, `CAREER_COACHING→CAREER_ADVICE`,
`LEARNING_RECOMMENDATION→LEARNING_RECOMMENDATION`. `GenerationJob` = `{jobId, status, validationStatus:
PENDING|PASSED|REJECTED, artifactUrl?, failureCode?, createdAt, completedAt?}`. The `content` schema per output type is
defined in OpenAPI (`SkillExplanationContent`, `RepositoryReviewContent`, `RepositorySummaryContent`,
`CareerAdviceContent`, `LearningRecommendationContent`). AI output never contains or changes official scores.

### 2.10 Dashboard

| API ID | Method + path | Purpose | Request | Success (`data`) | Errors |
|---|---|---|---|---|---|
| API-DSH-001 | `GET /api/v1/dashboard/summary` | Read-only composition of authoritative sources | — | `200` `{generatedAt, targets, repositories, analyses, skillOverview, readiness, recommendations (≤3 items), roadmap, recentJobs (≤8 items)}`; each section has `status: AVAILABLE\|EMPTY\|UNAVAILABLE` | — |

## 3. Asynchronous jobs

| Job (`jobType`) | Created by | Poll | Cancel | `resultResourceUrl` on success |
|---|---|---|---|---|
| `REPOSITORY_SYNC` | API-REP-006 | API-INT-007 | Not exposed | `/api/v1/repositories/{repositoryId}/snapshots/{snapshotId}` |
| `REPOSITORY_ANALYSIS` | API-ANA-001 | API-ANA-002 | Not exposed | `/api/v1/analyses/{analysisId}` |
| `KNOWLEDGE_INGESTION` | API-KNW-003, API-KNW-011 | API-KNW-008 | Not exposed | `/api/v1/knowledge-documents/{documentId}` |
| AI generation (no `jobType` field) | API-AI-001 | API-AI-002 | API-AI-003 | `artifactUrl` = `/api/v1/generated-artifacts/{artifactId}` |

**Common job fields** (sync, analysis, knowledge): `jobId, jobType, status, phase, progressPercent, attemptCount,
maxAttempts, submittedAt, startedAt?, completedAt?, pollingUrl, resultResourceUrl?, errorCode?, errorMessage?, retryable`.
`pollingUrl` is the type-specific job path in the table above.

| Job | `status` values | `phase` values (OpenAPI) | `retryable` (derivation) |
|---|---|---|---|
| Repository sync | `queued, running, succeeded, failed, cancelled, expired` (lower-case) | `QUEUED, COLLECTING, RETRY_WAIT, COMPLETED, FAILED` | `failed` and `errorCode ≠ COLLECTION_LIMIT_EXCEEDED` (`RepositorySyncJobView.java:31-32`) |
| Analysis | same as sync | `QUEUED, EVALUATING_RULES, RETRY_WAIT, COMPLETED, FAILED` | `failed` (`AnalysisJobView.java:53`) |
| Knowledge ingestion | `queued, running, succeeded, failed` | `QUEUED, COLLECTING, RETRY_WAIT, COMPLETED, FAILED` | queued/running or attempts remaining (`KnowledgeIngestionJobView.java:17-18`) |
| AI generation | `QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELED` (upper-case) | — | — |

Client guidance:

1. Submit the command with a stable `Idempotency-Key`; treat `202` as acceptance, not completion.
2. Poll the returned `pollingUrl` (or `/generation-jobs/{jobId}`) until a terminal status (`succeeded`, `failed`,
   `cancelled`, `expired`, or `SUCCEEDED`, `FAILED`, `CANCELED`), then fetch `resultResourceUrl` / `artifactUrl`.
3. Workers use a maximum of 3 attempts by default for sync, analysis, and knowledge jobs (`maxAttempts`), with
   `RETRY_WAIT` between transient failures; GitHub rate limits delay the next attempt until the provider reset.
4. `COLLECTION_LIMIT_EXCEEDED` is terminal and non-retryable; no partial snapshot is stored.
5. Only AI generation jobs can be cancelled. Cancelling a `SUCCEEDED`, `FAILED`, or `CANCELED` job returns it unchanged
   (`GenerationJob.java:72-78`).

## Appendix A. Contract verification

Endpoint count: OpenAPI declares **75** operations — **70** with `x-api-id` and 5 infrastructure/session operations
(`/internal/health`, `/oauth2/authorization/github`, `/login/oauth2/code/github`, `GET /api/v1/csrf`,
`POST /api/v1/session/logout`). The code exposes **72** controller mappings (70 ID'd + health + CSRF); the other 3 are
Spring Security-owned. Every OpenAPI operation maps to an implementation and no undocumented controller endpoint was
found. docs/10 additionally catalogues many planned endpoints (for example API-REP-003/010, API-SKL-006/007,
API-REC-001/005–007, API-LRN-001/005–008/010, API-KNW-001/002/007, API-AI-004/006, API-DSH-002–010, API-ID-008/009,
Prompt, Portfolio, Resume, Interview, and Admin APIs); these are **not implemented** and are not listed above.

Discrepancies found at `d6753b9`. All fifteen are now resolved: D3–D8, D10 and D14 by aligning docs/10 to the
implementation (2026-10-19), and D1, D2, D9–D13 and D15 by the API-FIX code/OpenAPI change (2026-10-21). Evidence
paths and line numbers in the table refer to `d6753b9`.

| # | Area | Observation (at `d6753b9`) | Evidence | Status |
|---|---|---|---|---|
| D1 | Error code (403) | docs/10 defines `AUTHORIZATION_DENIED` for 403; the implementation returns `ACCESS_DENIED` (CSRF/access denial). OpenAPI does not name 403 codes. | `docs/10_API_Specification.md:373`; `backend/.../identity/config/SecurityConfiguration.java:100-101` | Resolved (2026-10-21): code/OpenAPI aligned |
| D2 | Idempotency conflict | docs/10 specifies `409 DUPLICATE_REQUEST` and OpenAPI lists 409 "idempotency conflict"; reusing a key for a different command actually throws `IllegalArgumentException` → `400 VALIDATION_ERROR`. 409 is produced only for concurrent constraint races. | `docs/10_API_Specification.md:376`; `contracts/openapi/devpath-openapi.yaml:1579,1661,1707`; `ai/application/AiGenerationApplicationService.java:108`; `knowledge/application/KnowledgeApplicationService.java:34`; `analysis/application/AnalysisApplicationService.java:49`; `repository/application/RepositorySynchronizationTransaction.java:46` | Resolved (2026-10-21): code/OpenAPI aligned (409 `DUPLICATE_REQUEST`; the same request replays the original job) |
| D3 | Envelope shape | docs/10 examples use `data.id/type/attributes`, `links`, `metadata.correlationId`, and collection `items/pagination`; implementation and OpenAPI use `data` payloads with `metadata {requestId, apiVersion, timestamp}` only and resource-specific list keys. Correlation ID is a response header only. | `docs/10_API_Specification.md:271-313`; `shared/api/ApiMetadata.java:7`; `contracts/openapi/devpath-openapi.yaml:1793-1805` | Resolved (2026-10-19): docs/10 aligned to the implementation |
| D4 | Error shape | docs/10 error model includes `userSafeDetail`, `fieldErrors`, `retryable`, `providerCategory`, `documentationRef`; implementation and OpenAPI return only `code` and `message`. | `docs/10_API_Specification.md:342-364`; `shared/api/ApiErrorResponse.java:8`; `contracts/openapi/devpath-openapi.yaml:3482-3490` | Resolved (2026-10-19): docs/10 aligned to the implementation |
| D5 | Job polling URL | docs/10 example uses generic `/api/v1/jobs/{jobId}`; implemented polling is type-specific and no `/api/v1/jobs` endpoint exists. | `docs/10_API_Specification.md:323`; `repository/application/RepositorySyncJobView.java:28`; `analysis/application/AnalysisJobView.java:51` | Resolved (2026-10-19): docs/10 aligned to the implementation |
| D6 | Job status casing/value | docs/10 job states are lower-case incl. `cancelled`; AI generation jobs use upper-case `QUEUED…CANCELED` (single L), and the dashboard job item uses upper-case statuses and `jobType` `ANALYSIS` while the analysis job resource uses lower-case and `REPOSITORY_ANALYSIS`. Internally inconsistent but OpenAPI matches code for each resource. | `docs/10_API_Specification.md:867`; `contracts/openapi/devpath-openapi.yaml:2127-2128,3056-3058,3276`; `dashboard/application/DashboardApplicationService.java:173-177` | Resolved (2026-10-19): docs/10 aligned to the implementation |
| D7 | Duplicate API IDs in docs/10 | The same method+path carries two IDs in docs/10: sync `API-INT-006`/`API-REP-006`, Notion import `API-INT-012`/`API-KNW-003`, career target `API-CAR-003`/`API-ID-006`, company target `API-CMP-003`/`API-ID-007`. OpenAPI uses `API-REP-006`, `API-KNW-003`, `API-ID-006`, `API-ID-007`. | `docs/10_API_Specification.md:463,474,618,624`; `contracts/openapi/devpath-openapi.yaml:660,1560,195,230` | Resolved (2026-10-19): docs/10 aligned to the implementation |
| D8 | Stale implementation statement | docs/10 §43 states that all catalog operations other than the identity foundation "remain specification-only", contradicting the 70 implemented ID'd operations in OpenAPI. | `docs/10_API_Specification.md:1460` | Resolved (2026-10-19): docs/10 aligned to the implementation |
| D9 | GitHub integration callback auth | OpenAPI marks API-INT-002 as `sessionCookie`-secured with a `401` response; the route is `permitAll` and the controller converts every failure (including missing session/state) into a `302 …githubConnection=failed` redirect, so no 401 is produced. The Notion callback is not `permitAll` and does return 401 without a session. | `contracts/openapi/devpath-openapi.yaml:334,342-343`; `identity/config/SecurityConfiguration.java:61`; `integration/adapter/in/web/GitHubIntegrationController.java:75-87` | Resolved (2026-10-21): code/OpenAPI aligned (`security: []`, 302 only). Open (Low): OpenAPI still marks `state` required although a missing state yields `…=failed` |
| D10 | Notion import 403 | OpenAPI lists `403` for "missing CSRF or page permission"; a page that is not owned/shared resolves to `NotionConnectionNotFoundException` → `404`, so 403 occurs only for CSRF. | `contracts/openapi/devpath-openapi.yaml:1577`; `integration/adapter/out/notion/NotionApiAdapter.java:138-143`; `knowledge/adapter/in/web/KnowledgeExceptionHandler.java:22-25` | Resolved (2026-10-19): docs/10 aligned to the implementation; Resolved (2026-10-21): code/OpenAPI aligned (403 = CSRF only, inaccessible page 404) |
| D11 | Profile 404 undocumented | API-ID-002/003 can return `404 RESOURCE_NOT_FOUND` (docs/10 lists it for API-ID-002) but OpenAPI declares only 200/401 (GET) and 200/400/401/403 (PATCH). | `docs/10_API_Specification.md:436`; `contracts/openapi/devpath-openapi.yaml:111-121,135-155`; `identity/adapter/in/web/IdentityApiExceptionHandler.java:30-33` | Resolved (2026-10-21): code/OpenAPI aligned (GET and PATCH declare 404) |
| D12 | Roadmap archive Idempotency-Key | Header is required (OpenAPI pattern `^[A-Za-z0-9._:-]+$`) but the controller validates only `@Size(1,128)` and does not pass the key to the service; idempotency comes from archive state. No 400 is declared, and the mapped advice handles only not-found, so missing/invalid headers get the framework default error body. | `contracts/openapi/devpath-openapi.yaml:1508,1509-1521`; `learning/adapter/in/web/LearningRoadmapController.java:27`; `recommendation/adapter/in/web/RecommendationApiExceptionHandler.java:15-16` | Resolved (2026-10-21): code/OpenAPI aligned (key size and pattern validated; `ErrorEnvelope` 400 declared) |
| D13 | Missing-header error body | `RepositoryExceptionHandler` does not map `MissingRequestHeaderException`, so `POST …/sync` without `Idempotency-Key` returns the framework default 400 body instead of `ErrorEnvelope` (analysis, knowledge, and AI advices do map it). Header constraint annotations on non-`@Validated` controllers (analysis, knowledge, AI) are enforced by Spring MVC method validation, whose exception is not mapped by those advices; service-level validation still returns `VALIDATION_ERROR` for keys that reach the service. Inferred from code; not executed. | `repository/adapter/in/web/RepositoryExceptionHandler.java:55-61`; `repository/adapter/in/web/RepositorySynchronizationController.java:37`; `analysis/adapter/in/web/AnalysisController.java:28-42`; `repository/application/RepositorySynchronizationApplicationService.java:50-51` | Resolved (2026-10-21): code/OpenAPI aligned (shared `ApiValidationExceptionHandler`; reusable OpenAPI `ValidationError`) |
| D14 | Idempotency scope | docs/10 requires `Idempotency-Key` for API-INT-001/005/008 and other commands; the implementation and OpenAPI require it only on the six commands in §1.9 (import and authorize have none). | `docs/10_API_Specification.md:458,462,470`; `repository/adapter/in/web/RepositoryController.java:33-44` | Resolved (2026-10-19): docs/10 aligned to the implementation |
| D15 | Non-UUID path/query IDs | Invalid UUIDs in path or query parameters fail type conversion before any mapped advice and return the framework default 400 body, not `ErrorEnvelope`. OpenAPI declares no 400 for most UUID-path GETs. Inferred from code; not executed. | e.g. `rule/adapter/in/web/RuleEvaluationController.java:26-28`; `contracts/openapi/devpath-openapi.yaml:1010-1026` | Resolved (2026-10-21): code/OpenAPI aligned (`400 VALIDATION_ERROR` declared via `ValidationError`) |

Verification method: OpenAPI read in full; all web controllers, exception advices, `SecurityConfiguration`, `shared/api`
classes, and the job view records were read at `d6753b9`. No tests or HTTP calls were executed for this reference; items
marked "inferred" were derived from Spring framework behaviour. The 2026-10-21 resolutions were verified against the
API-FIX diff and its evidence: independent reviews PASS (2 cycles, 3 lenses); backend 255 tests (253 PASS, 2
environment-conditional skips; Testcontainers with Docker); frontend 53 files / 214 tests PASS; builds PASS; contract
lint 0 errors. Open (Low): five module exception advices have no explicit `@Order` relative to the shared validation
advice (same status and code either way; only the message text could differ).
