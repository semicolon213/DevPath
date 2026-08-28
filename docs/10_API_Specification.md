# DevPath API Specification

## 1. Purpose

### 1.1 Document Purpose

This document defines the contract-first API specification for DevPath. It specifies external and internal API boundaries, REST resources, operations, request contracts, response contracts, error contracts, authentication expectations, pagination, filtering, idempotency, versioning, asynchronous job contracts, and traceability.

This document is implementation-independent. It does not define controllers, services, repositories, ORM mappings, SQL, queue technology, Redis key design, frontend state management, or deployment topology.

### 1.2 Scope

The API specification covers:

- User identity and profile management.
- GitHub and Notion connection workflows.
- Repository synchronization, snapshots, and analysis access.
- Rule evaluation and Skill Matrix access.
- Career readiness and company readiness access.
- Recommendations and learning roadmaps.
- Knowledge ingestion, document access, retrieval, and re-indexing.
- Prompt templates, prompt contexts, and prompt execution metadata.
- AI generation jobs and generated artifacts.
- Portfolio, resume, and interview question resources.
- Dashboard read models.
- Administration and configuration APIs.
- Common request, response, error, job, pagination, idempotency, versioning, rate limiting, upload/export, observability, and traceability contracts.

### 1.3 Intended Audience

| Audience | Usage |
|---|---|
| Backend engineers | Implement API contracts without changing domain semantics |
| Frontend engineers | Build client integrations against stable request and response contracts |
| AI engineers | Integrate generation workflows without crossing deterministic boundaries |
| Prompt engineers | Manage prompt metadata and prompt context contracts |
| QA engineers | Derive contract, validation, authorization, and regression tests |
| Security engineers | Validate authentication, authorization, redaction, and callback behavior |
| Product owners | Confirm API support for functional requirements |

### 1.4 Authority of This Document

This document is the authoritative source for API behavior. OpenAPI artifacts, backend controllers, frontend clients, SDKs, and integration tests must be derived from this specification.

### 1.5 Relationship to Previous Architecture Documents

| Source | API Usage |
|---|---|
| `01_SRS.md` | Defines functional requirement coverage and measurable behavior |
| `02_Rule_Engine.md` | Defines deterministic score outputs and rule evaluation boundary |
| `03_Career_Path_Engine.md` | Defines career/company readiness, recommendations, and roadmaps |
| `04_AI_Architecture.md` | Defines AI task, model execution, response validation, and generated artifact rules |
| `05_Prompt_Engineering.md` | Defines prompt templates, contexts, validation, and versioning |
| `06_Knowledge_Architecture.md` | Defines knowledge ingestion, retrieval, chunking, and privacy rules |
| `07_Domain_Model.md` | Defines domain resources, aggregate boundaries, events, and invariants |
| `08_System_Data_Model.md` | Defines canonical data ownership, lifecycle, snapshot, and version strategy |
| `09_Database_Design.md` | Defines persistence responsibilities without leaking database details into API contracts |

### 1.6 Relationship to Backend and Frontend Implementation

The backend must implement these contracts while preserving domain ownership. The frontend must consume the API through documented resources, schemas, job models, and error models. Neither layer may infer business calculations from transport fields.

## 2. API Scope and Boundaries

### 2.1 API Boundary Types

| Boundary | Audience | Purpose | Examples |
|---|---|---|---|
| Public Client API | Browser and future mobile clients | User-facing product operations | Profile, repository, analysis, dashboard, portfolio |
| Authenticated User API | Authenticated DevPath users | User-owned resource management | Sync repository, create roadmap, generate resume |
| Administrative API | Privileged operators | Configuration, inspection, audit, support | Rule version metadata, prompt activation, audit access |
| Internal Service API | DevPath services only | Cross-service orchestration where REST is used internally | Job callbacks, internal validation status |
| Webhook Receiver | External providers | OAuth callbacks and future provider events | GitHub callback, Notion callback, future webhooks |
| Third-party Integration Contract | External provider adapters | Provider-specific translation boundary | GitHub/Notion adapter behavior |

### 2.2 External, Internal, Provider, and Job Contracts

| Contract Type | Definition | Exposure |
|---|---|---|
| External API | Stable API consumed by DevPath web client or future public clients. | Documented in endpoint catalog. |
| Internal API | Service-to-service API not intended for public clients. | Marked internal and protected by service authorization. |
| Provider Integration Contract | Contract between DevPath and external providers. | Provider details hidden from normal user API responses. |
| Asynchronous Job Contract | Standard job representation for long-running operations. | Exposed to clients for polling and future notification support. |

### 2.3 Excluded API Concerns

The API must not expose:

- Database table names or storage topology.
- ORM entity names or repository implementations.
- Raw provider-specific AI payloads.
- Internal cache keys, queue names, or worker topology.
- Vector database internals or raw embedding vectors.
- Secrets, OAuth tokens, refresh tokens, provider secrets, or signing secrets.
- Mutable fields for immutable historical resources.

## 3. API Architecture Overview

### 3.1 Standard API Flow

| Step | Flow | Responsibility |
|---|---|---|
| 1 | Client → API Edge | Receive HTTPS request, enforce content and size rules. |
| 2 | API Edge → Authentication | Validate opaque application-session cookie or verified callback state/signature. |
| 3 | Authentication → Authorization | Validate user ownership, role, and resource permissions. |
| 4 | Authorization → Application API | Validate request contract and idempotency. |
| 5 | Application API → Domain/Application Services | Execute command or query without placing business logic in transport layer. |
| 6 | Domain/Application Services → Persistence/Providers | Persist canonical state or call provider adapters. |
| 7 | Application API → Client | Return resource, collection, job, or error contract. |

### 3.2 AI Request Flow

| Step | Flow | API Contract Meaning |
|---|---|---|
| 1 | Client Request | Client requests supported AI generation task using validated source references. |
| 2 | Validated API Contract | API rejects client-supplied official scores, readiness values, or recommendation priorities. |
| 3 | Context Builder | Structured outputs and retrieval results are selected. |
| 4 | Prompt Builder | PromptContext is assembled from PromptTemplateVersion and variables. |
| 5 | LLM | AI provider generates natural-language response. |
| 6 | Response Validator | Output format, grounding, unsupported claims, and safety are validated. |
| 7 | Generated Artifact Response | Client receives job status, validation metadata, and generated artifact reference. |

### 3.3 Command and Query Separation

| API Type | Examples | Behavior |
|---|---|---|
| Command | Create analysis, request synchronization, generate portfolio, archive artifact | May create jobs, mutate state, require idempotency |
| Query | Get repository detail, list recommendations, retrieve dashboard summary | Must not mutate business state except audit/read tracking where policy allows |
| Action Endpoint | `/repositories/{repositoryId}/sync`, `/recommendations/{recommendationId}/accept` | Used only when operation is not natural CRUD |

## 4. Resource Model

### 4.1 Canonical API Resources

| Resource | Purpose | Owner | Identifier | Mutability | Lifecycle | Related Resources | Create | Read | Update | Delete/Archive | Authorization |
|---|---|---|---|---|---|---|---|---|---|---|---|
| User | Current authenticated account. | Identity | userId | Limited mutable profile/account state | Active, Suspended, DeletionRequested, Deleted | UserProfile, connections | OAuth provisioning | `/users/me` | Profile/preferences APIs | Deletion request | Self or admin |
| UserProfile | Developer profile metadata. | Identity/Profile | profileId | Mutable by owner | Draft, Complete, Archived | User, preferences | Provisioned with user | `/users/me/profile` | PATCH profile | Archive via account lifecycle | Self |
| GitHubConnection | GitHub OAuth connection. | Identity | connectionId | Status mutable | Pending, Active, Expired, Revoked, Disconnected | Repositories | Initiate OAuth | List connections | Permission refresh by provider | Disconnect | Self |
| NotionConnection | Notion OAuth connection. | Identity/Knowledge | connectionId | Status mutable | Pending, Active, Revoked, Disconnected | KnowledgeDocuments | Initiate OAuth | List connections | Permission refresh by provider | Disconnect | Self |
| Repository | Canonical repository. | Repository | repositoryId | Current metadata mutable by sync | Discovered, Active, Archived | Snapshots, technologies, evidence | Import/register | List/detail | Archive/restore | Archive | Owner |
| RepositorySnapshot | Immutable repository state. | Repository | snapshotId | Immutable | Capturing, Ready, Failed, Superseded | Repository, Analysis | Created by sync/snapshot command | List/detail | None | Retention archive only | Owner |
| RepositorySyncJob | Synchronization job. | Repository | jobId | Status mutable | queued, running, succeeded, failed, cancelled, expired | Repository | Sync action | Job detail | Cancel where supported | Expire | Owner |
| Analysis | Completed analysis resource. | Rule | analysisId | Immutable after completion | Requested, Running, Completed, Failed | Evaluation, SkillMatrix | Create analysis request | Detail/history | None | Archive history only | Owner |
| AnalysisJob | Long-running analysis job. | Rule | jobId | Status mutable | queued, running, succeeded, failed, cancelled, expired | Analysis | Create analysis | Poll | Cancel where supported | Expire | Owner |
| RuleEvaluation | Deterministic rule result. | Rule | evaluationId | Immutable after completion | Completed, Superseded | Analysis, evidence | Created by Rule Engine | Detail/breakdown | None | Retention archive only | Owner |
| SkillMatrix | Evidence-backed skill matrix. | Rule | skillMatrixId | Immutable historical versions | Generated, Published, Superseded | Evaluation, skills, evidence | Generated by Rule Engine | Current/history/detail | None | Archive history only | Owner |
| CareerProfile | Supported career definition. | Career/Admin | careerProfileVersionId | Immutable version | Draft, Active, Superseded | CareerReadiness | Admin only | List/detail | Admin versioning | Deprecate | Public/authenticated read; admin write |
| CareerReadiness | Career assessment result. | Career | careerReadinessId | Immutable after completion | Completed, Superseded | SkillMatrix, SkillGap | Trigger assessment | Current/detail | None | Archive history only | Owner |
| CompanyProfile | Supported company definition. | Company/Admin | companyProfileVersionId | Immutable version | Draft, Active, Superseded | CompanyReadiness | Admin only | List/detail | Admin versioning | Deprecate | Authenticated read; admin write |
| CompanyReadiness | Company assessment result. | Company | companyReadinessId | Immutable after completion | Completed, Superseded | SkillMatrix, company profile | Trigger assessment | Current/detail/compare | None | Archive history only | Owner |
| Recommendation | Deterministic recommended action. | Recommendation | recommendationId | Status mutable only | Proposed, Accepted, Dismissed, Completed | Evidence, roadmap | Generate set | Current/detail/history | Accept/dismiss/complete | Archive by history | Owner |
| LearningRoadmap | Ordered learning plan. | Learning | roadmapId | Progress mutable | Created, InProgress, Completed, Archived | RoadmapSteps | Create from recommendations | Active/history/detail | Progress/status | Archive | Owner |
| RoadmapStep | Measurable learning step. | Learning | roadmapStepId | Progress mutable | NotStarted, InProgress, Completed, Skipped | LearningRoadmap | Created with roadmap | Detail | Progress | Skip/archive with roadmap | Owner |
| LearningProgress | Progress read resource. | Learning | progressId | Derived from step updates | Active, Completed | RoadmapStep | Step update | Detail | Step progress APIs | Archive with roadmap | Owner |
| KnowledgeDocument | Knowledge source. | Knowledge | knowledgeDocumentId | Metadata/status mutable | Discovered, Ingested, Indexed, Stale, Archived | Chunks, ingestion jobs | Create/upload/import | List/detail | Archive/reindex | Archive/delete by policy | Owner |
| KnowledgeChunkSummary | Chunk metadata/excerpt. | Knowledge | chunkId | Immutable per document version | Indexed, Stale, Deleted | KnowledgeDocument | Created by ingestion | List/detail summary | None | Deleted with source | Owner |
| KnowledgeIngestionJob | Knowledge ingestion job. | Knowledge | jobId | Status mutable | queued, running, succeeded, failed, cancelled, expired | KnowledgeDocument | Create ingestion | Poll | Cancel where supported | Expire | Owner |
| KnowledgeSearchResult | Retrieval response. | Knowledge | retrievalResultId | Immutable when retained | Completed, Expired | KnowledgeDocument, chunks | Search knowledge | Response/detail if retained | None | Expire | Owner |
| PromptTemplate | Prompt metadata. | Prompt/Admin | promptTemplateId | Versioned | Draft, Active, Deprecated | PromptContext | Admin create | List/detail metadata | Admin versioning | Deprecate | Admin write; restricted read |
| PromptContext | Immutable prompt package. | Prompt | promptContextId | Immutable after creation | Created, Validated, Locked, Rejected | GenerationRequest | Create | Detail | Validate only before lock | Expire/archive | Owner; admin metadata |
| GenerationRequest | Request to create AI output. | AI | generationRequestId | Immutable request | Submitted, Accepted | GenerationJob | Create | Detail | Cancel via job | Expire | Owner |
| GenerationJob | AI generation job. | AI | jobId | Status mutable | queued, running, succeeded, failed, cancelled, expired | GeneratedArtifact | Create generation | Poll | Cancel/retry | Expire | Owner |
| GeneratedArtifact | AI-generated output. | AI | artifactId | Review status mutable | Draft, Validated, Reviewed, Approved, Published | Portfolio, Resume, Interview | Created by AI | Detail/history | Review/approve | Archive | Owner |
| Portfolio | Career portfolio artifact. | Portfolio | portfolioId | Draft metadata mutable | Draft, Generated, Reviewed, Published, Archived | GeneratedArtifact, projects | Create/generate | Detail/versions | Editable metadata | Archive/unpublish | Owner; public if published |
| Resume | Resume artifact. | Portfolio | resumeId | Draft metadata mutable | Draft, Generated, Reviewed, Published, Archived | GeneratedArtifact, projects | Create/generate | Detail/versions | Editable fields | Archive | Owner |
| InterviewQuestionSet | Generated interview preparation set. | Portfolio/AI | questionSetId | Practice state mutable | Generated, Reviewed, Practiced, Archived | Questions | Generate | List/detail | Practice answer/feedback | Archive | Owner |
| DashboardSummary | Aggregated read model. | Dashboard projection | none or userId | Read-only | Current projection | Many resources | None | `/dashboard/summary` | None | None | Owner |
| Notification | User notification. | Notification | notificationId | Read state mutable | Created, Delivered, Read, Archived | Source event | System | List/detail | Mark read/archive | Archive | Owner |
| AuditRecord | Restricted audit record. | Audit/Admin | auditRecordId | Immutable | Recorded, Retained, Purged | Protected resources | System | Admin list/detail | None | Retention purge | Privileged admin |
| AdminConfiguration | Administrative config view. | Administration | configurationId | Versioned | Draft, Active, Deprecated | Rules, careers, companies, prompts | Admin create | Admin read | Admin activate/deprecate | Deprecate | Privileged admin |

## 5. URL and Naming Conventions

### 5.1 Base Path and Versioning

| Convention | Rule |
|---|---|
| Base path | `/api/v1` |
| Versioning style | URI versioning for public APIs |
| Content type | JSON over HTTPS unless file upload/download contract states otherwise |
| Resource naming | Plural kebab-case resource names |
| Field naming | camelCase JSON fields |
| Timestamp format | ISO 8601 UTC timestamp with timezone offset |
| Identifier format | Opaque string identifiers; clients must not infer structure |
| Enum naming | Upper snake case values |
| Boolean naming | `is`, `has`, `can`, or explicit state field |

### 5.2 Nesting and Action Policy

| Policy | Rule |
|---|---|
| Nested resources | Use one level of nesting when ownership is clear, such as `/repositories/{repositoryId}/snapshots`. |
| Excessive nesting | Avoid paths deeper than two resource levels unless necessary for clarity. |
| Action endpoints | Use explicit action endpoints for commands not naturally represented as CRUD. |
| Query endpoints | Prefer resource collection filters over custom search actions except for semantic knowledge search. |
| Immutable resources | Do not expose update endpoints for immutable snapshots, evaluations, prompt contexts, and published versions. |

### 5.3 URL Examples

| Purpose | Pattern |
|---|---|
| Current user | `/api/v1/users/me` |
| Repository list | `/api/v1/repositories` |
| Repository detail | `/api/v1/repositories/{repositoryId}` |
| Repository sync action | `/api/v1/repositories/{repositoryId}/sync` |
| Repository snapshots | `/api/v1/repositories/{repositoryId}/snapshots` |
| Analysis creation | `/api/v1/analyses` |
| Recommendations | `/api/v1/recommendations` |
| Generation jobs | `/api/v1/generation-jobs` |

## 6. Common Request Contract

### 6.1 Common Headers

| Header | Required | Scope | Description |
|---|---:|---|---|
| `Content-Type` | Required for body | External/User/Admin | Must be `application/json` except upload endpoints. |
| `Accept` | Optional | External/User/Admin | Defaults to `application/json`. |
| `Cookie` | Required for protected browser APIs | User/Admin | Secure HttpOnly opaque application-session cookie managed by the backend; JavaScript does not read its value. |
| `X-CSRF-TOKEN` | Required for state-changing cookie-authenticated requests | User/Admin commands | Server-issued CSRF token or equivalent Spring Security-supported header; it is not an authentication credential. |
| `Authorization` | Not used by the initial browser SPA | Future external/service clients | Reserved for a separately approved bearer-token contract; provider tokens are never accepted as DevPath application credentials. |
| `X-Request-Id` | Optional | All | Client-supplied request identifier; server generates if absent. |
| `X-Correlation-Id` | Optional | All | Correlates multi-step workflows. |
| `Idempotency-Key` | Required for selected commands | Command endpoints | Prevents duplicate effects for retried commands. |
| `Accept-Language` | Optional | User API | Locale preference for user-facing text. |
| `X-Timezone` | Optional | User API | Client timezone for presentation-sensitive responses. |
| `X-Client-Version` | Optional | Client API | Client application version for compatibility diagnostics. |
| `X-Request-Timestamp` | Optional | Signed callbacks/internal | Request timestamp for replay protection where required. |

The implemented HTTP boundary accepts only bounded opaque request/correlation identifiers matching
`[A-Za-z0-9][A-Za-z0-9._-]{0,63}`. Invalid values are replaced instead of reflected into logs. Every response exposes
the resolved `X-Request-Id` and `X-Correlation-Id`; API error metadata uses the same resolved request identifier.
| `X-Internal-Service` | Internal only | Internal API | Service identity marker; not accepted from public clients. |

### 6.2 Common Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `cursor` | string | none | Cursor for cursor-based pagination. |
| `limit` | integer | 20 | Requested page size. Maximum defined in chapter 27. |
| `sort` | string | resource default | Stable sort expression such as `createdAt.desc`. |
| `status` | string | all active | Filters by lifecycle status. |
| `from` | timestamp/date | none | Inclusive lower date bound. |
| `to` | timestamp/date | none | Exclusive or inclusive upper bound as documented per endpoint. |
| `includeArchived` | boolean | false | Includes archived resources where supported. |
| `fields` | string list | none | Optional sparse field selection when supported. |
| `q` | string | none | Text search query for supported collections. |

### 6.3 Idempotency Key Requirements

`Idempotency-Key` is required for:

- Repository synchronization requests.
- Analysis creation requests.
- Recommendation generation requests.
- Learning roadmap creation requests.
- Knowledge ingestion and upload finalization requests.
- Prompt context creation requests.
- AI generation requests.
- Portfolio/resume/interview generation requests.
- Export generation requests.
- Account deletion requests.

## 7. Common Response Contract

### 7.1 Single Resource Response

Single-resource responses use a consistent envelope unless a standard protocol endpoint requires otherwise.

```json
{
  "data": {
    "id": "res_123",
    "type": "repository",
    "attributes": {}
  },
  "metadata": {
    "requestId": "req_123",
    "correlationId": "cor_123",
    "apiVersion": "v1",
    "timestamp": "2026-07-21T00:00:00Z"
  },
  "links": {
    "self": "/api/v1/repositories/res_123"
  }
}
```

### 7.2 Collection Response

```json
{
  "data": {
    "items": [],
    "pagination": {
      "limit": 20,
      "nextCursor": "cur_next",
      "previousCursor": null,
      "hasMore": false,
      "totalCount": null
    }
  },
  "metadata": {
    "requestId": "req_123",
    "timestamp": "2026-07-21T00:00:00Z"
  },
  "links": {
    "self": "/api/v1/repositories",
    "next": null
  }
}
```

### 7.3 Asynchronous Job Response

```json
{
  "data": {
    "jobId": "job_123",
    "status": "queued",
    "submittedAt": "2026-07-21T00:00:00Z",
    "pollingUrl": "/api/v1/jobs/job_123",
    "resultResourceUrl": null,
    "estimatedCompletion": {
      "policy": "not_guaranteed",
      "estimatedReadyAt": null
    }
  },
  "metadata": {
    "requestId": "req_123",
    "correlationId": "cor_123",
    "timestamp": "2026-07-21T00:00:00Z"
  }
}
```

## 8. Error Contract

### 8.1 Error Response Model

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The request is invalid.",
    "userSafeDetail": "One or more fields need correction.",
    "fieldErrors": [
      {
        "field": "targetCareerId",
        "code": "UNSUPPORTED_VALUE",
        "message": "The selected career is not supported."
      }
    ],
    "retryable": false,
    "providerCategory": null,
    "documentationRef": "/docs/errors/VALIDATION_ERROR"
  },
  "metadata": {
    "requestId": "req_123",
    "correlationId": "cor_123",
    "timestamp": "2026-07-21T00:00:00Z"
  }
}
```

### 8.2 Error Categories

| Category | Error Code | HTTP Status | Retryable | Notes |
|---|---|---:|---:|---|
| Validation error | `VALIDATION_ERROR` | 400 | No | Request contract or business input validation failed. |
| Authentication error | `AUTHENTICATION_REQUIRED` | 401 | No | Missing, expired, invalid, or revoked application session. |
| Authorization error | `AUTHORIZATION_DENIED` | 403 | No | User lacks resource permission. |
| Resource not found | `RESOURCE_NOT_FOUND` | 404 | No | Resource absent or inaccessible. |
| Conflict | `RESOURCE_CONFLICT` | 409 | Sometimes | State conflict or immutable resource mutation attempt. |
| Duplicate request | `DUPLICATE_REQUEST` | 409 | No | Idempotency conflict with different request body. |
| Rate limit | `RATE_LIMIT_EXCEEDED` | 429 | Yes | Client should follow rate-limit headers. |
| Dependency unavailable | `DEPENDENCY_UNAVAILABLE` | 503 | Yes | External provider or internal dependency unavailable. |
| Synchronization failure | `SYNCHRONIZATION_FAILED` | 424 or 500 | Yes | Provider sync or normalization failed. |
| Collection limit exceeded | `COLLECTION_LIMIT_EXCEEDED` | Async job result | No | Repository facts exceed the documented safe collection ceiling; no partial snapshot is created. |
| Analysis failure | `ANALYSIS_FAILED` | 422 or 500 | Sometimes | Analysis could not complete from supplied references. |
| AI provider failure | `AI_PROVIDER_FAILURE` | 502 or 503 | Yes | LLM provider timeout, rate limit, or unavailability. |
| Response validation failure | `AI_RESPONSE_REJECTED` | 422 | Sometimes | Generated response failed validation. |
| Knowledge retrieval failure | `KNOWLEDGE_RETRIEVAL_FAILED` | 500 or 503 | Yes | Retrieval index unavailable or invalid. |
| Internal server error | `INTERNAL_SERVER_ERROR` | 500 | Yes | Unexpected server failure with safe message. |

### 8.3 Provider Error Safety

Provider error categories may be surfaced as safe categories such as `GITHUB_RATE_LIMIT`, `NOTION_PERMISSION_DENIED`, or `LLM_TIMEOUT`. Raw provider payloads, secrets, stack traces, and credential values must not be exposed.

## 9. Authentication and Authorization Contract

### 9.1 Authentication Expectations

| Area | Contract |
|---|---|
| OAuth login | Backend-owned GitHub OAuth2 Login creates or links a provider-independent DevPath User and then creates an opaque application session. |
| Application session | Protected browser APIs authenticate with the backend-managed session cookie defined by ADR-026. |
| Bearer and refresh tokens | The initial SPA receives neither a bearer access token nor a refresh token. Future non-browser clients require a separate approved contract. |
| Session expiration | Idle or absolute expiry returns `AUTHENTICATION_REQUIRED`; renewal never exceeds the configured absolute lifetime. |
| Session revocation | Logout, suspension, deletion, compromise response, and security-sensitive account changes invalidate affected server-side sessions. |
| Provider tokens | GitHub/Notion provider tokens are never returned by API and are never treated as the application session. |
| Current-user bootstrap | `GET /api/v1/users/me` is the authoritative frontend session check: `200` returns the authenticated User, while `401` indicates no valid session. |
| Unauthorized and forbidden | `401` means authentication is absent or invalid. `403` means authentication succeeded but authorization is denied. `404` may hide a private resource's existence. |
| Credentialed requests | The SPA includes credentials for same-origin or explicitly allowlisted origins; wildcard credentialed CORS is prohibited. |
| CSRF | State-changing cookie-authenticated requests require a valid CSRF token; OAuth callbacks independently validate state and PKCE where supported. |

#### 9.1.1 Authentication Boundary Routes

GitHub login initiation and callback are owned by the backend security boundary and may use Spring Security-compatible routes outside the versioned business API. Logout is a backend-owned state-changing security route that invalidates the session and expires the cookie. Exact framework route names are implementation configuration, but they must be documented in the deployed contract and must not be confused with GitHub/Notion integration-authorization callbacks.

The existing `/api/v1/integrations/github/callback` and `/api/v1/integrations/notion/callback` contracts authorize provider API integrations. They do not themselves define the DevPath application session unless the request is explicitly the GitHub login flow governed by ADR-026.

### 9.2 Authorization Expectations

| Area | Contract |
|---|---|
| Resource ownership | User-owned resources require authenticated owner access. |
| GitHub authorization | Repository sync requires active GitHubConnection and source permission. |
| Notion authorization | Knowledge import requires active NotionConnection and page/workspace permission. |
| Administrative access | Admin APIs require privileged authorization. |
| Role-based permissions | User and admin roles are exposed as authorization capabilities, not implementation roles. |
| Future organization permissions | Organization scopes may be added without weakening current user isolation. |

### 9.3 Authorization Error Behavior

Unauthorized private resource access should return `404 RESOURCE_NOT_FOUND` when revealing existence is unsafe, or `403 AUTHORIZATION_DENIED` when existence is already known to the caller.

## 10. Identity and User APIs

### 10.1 Endpoint Contracts

| ID | Method | Path | Purpose | Auth | Authorization | Path Params | Query Params | Request Body | Response Body | Status Codes | Error Codes | Idempotency | Side Effects | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-ID-001 | GET | `/api/v1/users/me` | Retrieve current user. | Required | Self | None | fields | None | UserResponse | 200 | AUTHENTICATION_REQUIRED | Safe/idempotent | None | FR-001~FR-020 |
| API-ID-002 | GET | `/api/v1/users/me/profile` | Retrieve profile. | Required | Self | None | None | None | UserProfileResponse | 200 | RESOURCE_NOT_FOUND | Safe/idempotent | None | FR-006 |
| API-ID-003 | PATCH | `/api/v1/users/me/profile` | Update user profile. | Required | Self | None | None | UpdateUserProfileRequest | UserProfileResponse | 200 | VALIDATION_ERROR | Not idempotent unless same body with ETag | UserProfileUpdated | FR-007 |
| API-ID-004 | GET | `/api/v1/users/me/connections` | List connected accounts. | Required | Self | None | provider,status | None | ConnectedAccountListResponse | 200 | AUTHENTICATION_REQUIRED | Safe/idempotent | None | FR-013 |
| API-ID-005 | GET | `/api/v1/users/me/preferences` | Retrieve target career/company preferences. | Required | Self | None | None | None | UserPreferenceResponse | 200 | AUTHENTICATION_REQUIRED | Safe/idempotent | None | FR-008~FR-009 |
| API-ID-006 | PUT | `/api/v1/users/me/preferences/career` | Set target career. | Required | Self | None | None | SetCareerTargetRequest | UserPreferenceResponse | 200 | VALIDATION_ERROR | Idempotent by careerId | TargetCareerChanged | FR-008, CR-001 |
| API-ID-007 | PUT | `/api/v1/users/me/preferences/company` | Set target company. | Required | Self | None | None | SetCompanyTargetRequest | UserPreferenceResponse | 200 | VALIDATION_ERROR | Idempotent by companyId | TargetCompanyChanged | FR-009, CR-003 |
| API-ID-008 | POST | `/api/v1/users/me/deletion-requests` | Request account deletion. | Required | Self | None | None | CreateDeletionRequest | JobStatusResponse | 202 | RESOURCE_CONFLICT | Required | UserDeletionRequested | FR-012, FR-015 |
| API-ID-009 | GET | `/api/v1/users/me/archive-state` | Retrieve account archival/deletion state. | Required | Self | None | None | None | AccountArchiveStateResponse | 200 | AUTHENTICATION_REQUIRED | Safe/idempotent | None | FR-015 |
| API-ID-010 | GET | `/api/v1/users/me/onboarding-progress` | Retrieve deterministic onboarding progress from persisted owner-scoped resources. | Required | Self | None | None | None | OnboardingProgressResponse | 200 | AUTHENTICATION_REQUIRED | Safe/idempotent | OnboardingProgressViewed | FR-018~FR-019 |

The implemented `API-ID-010` projection reports account, profile, career target, optional company target, active GitHub
connection, repository import, initial synchronization, and initial analysis steps. It reads the authoritative persisted
resources instead of storing a duplicate browser progress model, emits a durable view audit event, and never calculates
an official score, readiness value, or recommendation priority. `DASHBOARD_READY` requires a completed analysis but does
not require the optional company target.

## 11. GitHub and Notion Integration APIs

### 11.1 GitHub Integration Endpoints

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Error Codes | Idempotency | Side Effects | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-INT-001 | POST | `/api/v1/integrations/github/authorize` | Initiate GitHub OAuth connection. | Required | Self | None | OAuthAuthorizationResponse | 200 | VALIDATION_ERROR | Required | Creates OAuth state | FR-021 |
| API-INT-002 | GET | `/api/v1/integrations/github/callback` | Handle GitHub API integration authorization callback; application-login callback remains security-boundary owned under ADR-026. | Callback | OAuth state bound to authenticated account-link context | Provider callback params | OAuthCallbackResponse | 200, 302 | AUTHENTICATION_REQUIRED, DEPENDENCY_UNAVAILABLE | Required by state | GitHubAccountConnected | FR-021 |
| API-INT-003 | DELETE | `/api/v1/integrations/github` | Disconnect GitHub. | Required | Self | None | ConnectedAccountResponse | 200 | RESOURCE_NOT_FOUND | Required | IntegrationDisconnected | FR-014~FR-015 |
| API-INT-004 | GET | `/api/v1/integrations/github/repositories` | List provider-accessible repositories before import. | Required | Self with GitHub permission | None | ProviderRepositoryListResponse | 200 | RATE_LIMIT_EXCEEDED, DEPENDENCY_UNAVAILABLE | Safe/idempotent | None | FR-024, FR-046 |
| API-INT-005 | POST | `/api/v1/repositories/imports` | Import/register a GitHub repository. | Required | Self with GitHub permission | ImportRepositoryRequest | RepositoryResponse | 201 | VALIDATION_ERROR, RESOURCE_CONFLICT, RATE_LIMIT_EXCEEDED | Required | RepositoryDiscovered | FR-025, FR-046 |
| API-INT-006 | POST | `/api/v1/repositories/{repositoryId}/sync` | Synchronize repository. | Required | Repository owner | SyncRepositoryRequest | JobStatusResponse | 202 | RATE_LIMIT_EXCEEDED, COLLECTION_LIMIT_EXCEEDED, SYNCHRONIZATION_FAILED | Required | SynchronizationRequested | FR-026~FR-050 |
| API-INT-007 | GET | `/api/v1/repository-sync-jobs/{jobId}` | Check sync status. | Required | Job owner | None | JobStatusResponse | 200 | RESOURCE_NOT_FOUND | Safe/idempotent | None | FR-049 |

### 11.2 Notion Integration Endpoints

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Error Codes | Idempotency | Side Effects | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-INT-008 | POST | `/api/v1/integrations/notion/authorize` | Initiate Notion OAuth connection. | Required | Self | None | OAuthAuthorizationResponse | 200 | VALIDATION_ERROR | Required | Creates OAuth state | FR-051 |
| API-INT-009 | GET | `/api/v1/integrations/notion/callback` | Handle Notion callback. | Callback | OAuth state | Provider callback params | OAuthCallbackResponse | 200, 302 | AUTHENTICATION_REQUIRED, DEPENDENCY_UNAVAILABLE | Required by state | NotionWorkspaceConnected | FR-052 |
| API-INT-010 | DELETE | `/api/v1/integrations/notion` | Disconnect Notion and remove retained page metadata. | Required | Self | None | ConnectedAccountResponse | 200, 404 | RESOURCE_NOT_FOUND | Required | IntegrationDisconnected | FR-051~FR-070 |
| API-INT-011 | GET | `/api/v1/integrations/notion/workspaces` | Discover and replace accessible workspace/page metadata. | Required | Self with Notion permission | None | NotionWorkspaceListResponse | 200, 404, 429, 503 | RESOURCE_NOT_FOUND, RATE_LIMIT_EXCEEDED, DEPENDENCY_UNAVAILABLE | Safe/idempotent | Metadata replacement | FR-053~FR-070 |
| API-INT-012 | POST | `/api/v1/knowledge-documents/imports/notion` | Import Notion knowledge content. | Required | Self with Notion permission | ImportNotionKnowledgeRequest | JobStatusResponse | 202 | VALIDATION_ERROR | Required | Knowledge ingestion job | FR-051~FR-070, KR-001 |

### 11.3 Provider Adapter Boundary

Provider adapters translate external provider models into canonical DevPath resources. User-facing APIs never return OAuth secrets, raw provider tokens, internal provider refresh data, or unsupported provider payload structures.

The implemented GitHub repository discovery adapter follows provider pages at 100 items per request,
de-duplicates repositories by provider repository ID, and rejects results beyond the safe provider-page
ceiling rather than returning an unmarked partial list. Disconnect removes the local credential before
best-effort remote token revocation and returns `REVOKED`.

The implemented repository-registration subset includes API-INT-005 and API-REP-001 through API-REP-005.
Import re-verifies the provider repository through the current user's server-side GitHub connection,
persists canonical metadata idempotently for normal duplicate requests, and records an audit event.
Repository list, detail, archive, and restore are owner-scoped; mutation endpoints are CSRF-protected and
record durable audit events on actual lifecycle changes. The list uses a bounded opaque cursor, excludes
locally archived repositories by default, and includes them when `includeArchived=true`. The implemented browser
workspace preserves that filter in the route, requires an explicit impact confirmation before archive, permits restore
only when the canonical provider state allows it, and continues to expose retained immutable history while disabling
new synchronization and analysis commands for locally archived or provider-deleted repositories.

The implemented repository-synchronization subset includes API-INT-007 and API-REP-006 through
API-REP-008. A CSRF-protected, idempotent sync command creates or reuses a PostgreSQL-backed durable
job. The worker reuses the same build artifact under an explicit runtime flag, claims queued jobs with
database locking, performs provider calls outside database transactions, retries bounded transient
failures, and writes a new immutable snapshot for every successful execution. The first collection
scope includes the default branch, complete bounded branch pagination, and complete bounded commit
pagination. Results beyond the configured safe provider-page ceiling fail instead of becoming silent
partial snapshots. Job and snapshot reads are owner-scoped and expose only safe failure details.

For NFR-005 and NFR-006, API-REP-006 performs only owner, repository lifecycle, input, idempotency, and active-job
checks before durably accepting the command. It does not call GitHub on the HTTP request thread. The worker performs
the definitive provider credential and repository-access check while collecting; revocation or provider failure is
persisted as bounded retry/final job state. This preserves owner authorization at acceptance without representing a
previously imported repository as currently provider-accessible before the worker verifies it.

Concurrent API-REP-006 requests are serialized by owner/idempotency key and repository basis inside the PostgreSQL
transaction. Equivalent commands return the first active job rather than surfacing a unique-constraint conflict.
Analysis creation uses the same rule for owner/idempotency key and snapshot/scope basis.

For FR-045, branch and commit pagination, recursive tree completeness, normalized file count, supported manifest
count, pull-request count, review pagination, and issue pagination/count have explicit adapter ceilings. Crossing one
of these ceilings produces the terminal, non-retryable `COLLECTION_LIMIT_EXCEEDED` job result on the first attempt.
The job exposes a bounded user-safe message and traceable job identifier, marks repository synchronization as failed,
writes the existing durable failure audit/outbox records, and never persists a partial snapshot or raw provider detail.
Transient provider outages remain separately retryable as `DEPENDENCY_UNAVAILABLE`.

For the implemented `API-ID-004` subset, `ConnectedAccountListResponse.data.connections` contains the owner's current provider API connection record in `ACTIVE`, `EXPIRED`, or `REVOKED` state. Each item contains `connectionId`, `provider`, `status`, a non-secret `scopes` summary, `connectedAt`, and nullable `expiresAt`. Inactive records expose an empty scope list and allow the client to offer reauthorization without attempting provider access. A GitHub login identity that has never established a provider credential is not returned as a repository-access connection.

The implemented GitHub App authorization slice uses `API-INT-001` to create a CSRF-protected, session/user-bound OAuth state with a ten-minute lifetime. `API-INT-002` consumes that state once, verifies the GitHub account matches the login identity, requires at least one accessible GitHub App installation, and stores access/refresh tokens encrypted server-side. Reauthorization rotates the existing owner connection back to `ACTIVE`. Disconnect, detected permission withdrawal, and unusable refresh expiry transition the record to `REVOKED` or `EXPIRED`, discard the actual stored provider secrets, clear the scope summary, and require reauthorization before future provider access. `API-INT-004` returns normalized repository metadata across installations without exposing provider tokens or raw provider payloads.

GitHub `429`, or `403` with zero remaining provider quota, is handled separately from permission withdrawal. The
implemented response uses `429 RATE_LIMIT_EXCEEDED`, retains the active connection, exposes only normalized
`Retry-After` and `X-RateLimit-Reset` timing, and never returns provider payloads. Repository synchronization records
`RATE_LIMIT_EXCEEDED` in its durable job state and waits until the known provider reset before claiming the next attempt;
unknown reset timing uses a bounded conservative delay. The browser suppresses automatic query retries for 429 and
shows an actionable reset-time message.

The implemented repository synchronization subset for FR-031~FR-036 also collects bounded pull requests, review
counts, non-PR issues, and the root README. `API-REP-007` and `API-REP-008` expose additive snapshot counts for pull
requests, issues, and documents. `API-REP-011` exposes deterministic `COLLABORATION` signals and README section
signals without calculating an official score. Private README content is not returned by any API.

## 12. Repository APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-REP-001 | GET | `/api/v1/repositories` | List imported repositories. | Required | Owner | None | RepositoryListResponse | 200 | Safe | FR-024~FR-050 |
| API-REP-002 | GET | `/api/v1/repositories/{repositoryId}` | Retrieve repository detail. | Required | Owner | None | RepositoryDetailResponse | 200 | Safe | FR-025 |
| API-REP-003 | POST | `/api/v1/repositories` | Register repository by supported source reference. | Required | Owner/provider permission | RegisterRepositoryRequest | RepositoryResponse | 201 | Required | FR-025 |
| API-REP-004 | POST | `/api/v1/repositories/{repositoryId}/archive` | Archive repository locally. | Required | Owner | None | RepositoryResponse | 200 | Idempotent by repository state | FR-361 |
| API-REP-005 | POST | `/api/v1/repositories/{repositoryId}/restore` | Restore a locally archived repository. | Required | Owner | None | RepositoryResponse | 200 | Idempotent by repository state | FR-361 |
| API-REP-006 | POST | `/api/v1/repositories/{repositoryId}/sync` | Request sync. | Required | Owner/provider permission | SyncRepositoryRequest | JobStatusResponse | 202 | Required; terminal `COLLECTION_LIMIT_EXCEEDED` is non-retryable | FR-026~FR-050 |
| API-REP-007 | GET | `/api/v1/repositories/{repositoryId}/snapshots` | List repository snapshots. | Required | Owner | None | RepositorySnapshotListResponse | 200 | Safe | FR-026~FR-047 |
| API-REP-008 | GET | `/api/v1/repositories/{repositoryId}/snapshots/{snapshotId}` | Retrieve immutable snapshot metadata. | Required | Owner | None | RepositorySnapshotResponse | 200 | Safe | FR-026~FR-047 |
| API-REP-009 | GET | `/api/v1/repositories/{repositoryId}/technologies` | Retrieve detected technologies. | Required | Owner | None | TechnologySummaryResponse | 200 | Safe | RR-001~RR-003 |
| API-REP-010 | GET | `/api/v1/repositories/{repositoryId}/projects` | Retrieve related projects. | Required | Owner | None | ProjectListResponse | 200 | Safe | AI-005 |
| API-REP-011 | GET | `/api/v1/repositories/{repositoryId}/evidence` | Retrieve repository evidence references. | Required | Owner | None | EvidenceListResponse | 200 | Safe | RR-001~RR-010 |
| API-REP-012 | GET | `/api/v1/repositories/{repositoryId}/analyses` | List repository analysis history. | Required | Owner | None | AnalysisHistoryResponse | 200 | Safe | FR-071~FR-180 |

RepositorySnapshot resources are immutable. Current snapshot access is exposed through filters such as `status=READY&sort=capturedAt.desc&limit=1`, not by mutating historical snapshots.

The Korean repository-detail journey retains the opaque owner-scoped synchronization `jobId` as a `syncJobId` route
query parameter and resumes API-INT-007 polling after a browser refresh. It never stores a session or provider token.
When a successful job returns an API-REP-008 result URL matching the current repository, the browser converts it to the
corresponding application snapshot-detail route. API-REP-007 history items link to the same route. The detail view shows
only stored immutable metadata, full source revision/content hash, measured collection counts, and whether the snapshot
is the repository's current snapshot. Missing and cross-owner resources use the same unavailable state.
The snapshot-detail journey also walks cursor-paginated API-REP-012 pages and selects completed analyses whose stored
`snapshotId` exactly matches the viewed snapshot. It displays only the persisted overall score, confidence, versions,
completion time, and analysis link. No score, delta, trend, or current-result designation is recalculated. A history
failure is isolated from the API-REP-008 provenance view and can be retried independently.

## 13. Analysis and Rule Evaluation APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-ANA-001 | POST | `/api/v1/analyses` | Create analysis request from supported references. | Required | Owner | CreateAnalysisRequest | JobStatusResponse | 202 | Required | FR-071~FR-180 |
| API-ANA-002 | GET | `/api/v1/analysis-jobs/{jobId}` | Retrieve analysis job. | Required | Job owner | None | JobStatusResponse | 200 | Safe | FR-049 |
| API-ANA-003 | GET | `/api/v1/analyses/{analysisId}` | Retrieve completed analysis. | Required | Owner | None | AnalysisResultResponse | 200 | Safe | FR-071~FR-180 |
| API-ANA-004 | GET | `/api/v1/rule-evaluations/{evaluationId}` | Retrieve rule evaluation. | Required | Owner | None | RuleEvaluationResponse | 200 | Safe | RR-001~RR-010 |
| API-ANA-005 | GET | `/api/v1/rule-evaluations/{evaluationId}/score-breakdown` | Retrieve category and overall score breakdown. | Required | Owner | None | ScoreBreakdownResponse | 200 | Safe | RR-010 |
| API-ANA-006 | GET | `/api/v1/rule-evaluations/{evaluationId}/evidence` | Retrieve evidence references. | Required | Owner | None | EvidenceListResponse | 200 | Safe | RR-001~RR-010 |
| API-ANA-007 | GET | `/api/v1/analyses` | List analysis history. | Required | Owner | None | AnalysisHistoryResponse | 200 | Safe | FR-071~FR-180 |
| API-ANA-008 | GET | `/api/v1/analyses/compare` | Compare analyses by IDs. | Required | Owner | None | AnalysisComparisonResponse | 200 | Safe | FR-101~FR-180 |

Clients may submit only supported references such as repository IDs, snapshot IDs, project IDs, or analysis configuration options. Clients must not submit final scores, score overrides, rule outputs, readiness values, or priority values.

## 14. Skill Matrix APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-SKL-001 | GET | `/api/v1/skill-matrices/current` | Retrieve current Skill Matrix. | Required | Owner | None | SkillMatrixResponse | 200 | Safe | RR-009 |
| API-SKL-002 | GET | `/api/v1/skill-matrices/{skillMatrixId}` | Retrieve historical Skill Matrix. | Required | Owner | None | SkillMatrixResponse | 200 | Safe | RR-009 |
| API-SKL-003 | GET | `/api/v1/skill-matrices/compare` | Compare Skill Matrices. | Required | Owner | None | SkillMatrixComparisonResponse | 200 | Safe | RR-009 |
| API-SKL-004 | GET | `/api/v1/skills/{skillId}` | Retrieve skill detail for user. | Required | Owner | None | SkillDetailResponse | 200 | Safe | RR-009 |
| API-SKL-005 | GET | `/api/v1/skills/{skillId}/evidence` | Retrieve evidence by skill. | Required | Owner | None | EvidenceListResponse | 200 | Safe | RR-001~RR-010 |
| API-SKL-006 | GET | `/api/v1/technologies/{technologyId}/proficiency` | Retrieve technology proficiency. | Required | Owner | None | TechnologyProficiencyResponse | 200 | Safe | RR-001~RR-003 |
| API-SKL-007 | GET | `/api/v1/frameworks/{frameworkId}/proficiency` | Retrieve framework proficiency. | Required | Owner | None | FrameworkProficiencyResponse | 200 | Safe | RR-002 |

Skill scores must reference an analysis result and evidence. Historical SkillMatrix resources are immutable.

The implemented read subset is API-SKL-001/002/003/004/005. The browser consumes API-SKL-001 through React Query on the Korean
`/skills` view and treats `404` (no completed matrix), `401` (anonymous session), and transport failure as distinct
states. API-SKL-003 accepts exactly two distinct repeated `skillMatrixId` query parameters, verifies that both immutable
matrices belong to the authenticated owner, and returns them in request order. It does not derive a delta, growth trend,
replacement level, or new official result. A persisted RuleEvaluation invokes idempotent Skill Matrix generation
internally. API-SKL-004/005 treat `skillId` as the stable Skill catalog identity and resolve its assessment and normalized
evidence from the authenticated owner's current immutable Matrix. Historical Matrix assessments remain available through
API-SKL-002. A missing current assessment and a cross-owner resource both return `404`; no public analysis-trigger endpoint
or background job technology is implied by this subset.

The Korean `/skills/{skillId}` browser journey composes API-ANA-007 history pages with the referenced owner-scoped
API-SKL-002 immutable Matrices to present a newest-first per-skill history. The browser filters by stable `skillId` and
displays only each Matrix's stored score, level, confidence, evidence count, policy/rule versions, and generation time.
It does not calculate a delta, growth trend, replacement level, or new official result. History loading and failure are
isolated from API-SKL-004/005 so the current authoritative detail and evidence remain usable when an older page fails.

## 15. Career and Company APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-CAR-001 | GET | `/api/v1/careers` | List supported careers. | Required | Authenticated | None | CareerListResponse | 200 | Safe | CR-001 |
| API-CAR-002 | GET | `/api/v1/careers/{careerId}` | Retrieve career definition. | Required | Authenticated | None | CareerProfileResponse | 200 | Safe | CR-002 |
| API-CAR-003 | PUT | `/api/v1/users/me/preferences/career` | Set target career. | Required | Self | SetCareerTargetRequest | UserPreferenceResponse | 200 | Idempotent | CR-001 |
| API-CAR-004 | GET | `/api/v1/career-readiness/current` | Retrieve current career readiness. | Required | Owner | None | CareerReadinessResponse | 200 | Safe | CR-005~CR-007 |
| API-CAR-005 | GET | `/api/v1/career-readiness/{careerReadinessId}` | Retrieve historical career readiness. | Required | Owner | None | CareerReadinessResponse | 200 | Safe | CR-007 |
| API-CAR-006 | GET | `/api/v1/career-readiness/{careerReadinessId}/skill-gaps` | Retrieve skill gaps. | Required | Owner | None | SkillGapListResponse | 200 | Safe | CR-005 |
| API-CMP-001 | GET | `/api/v1/companies` | List supported companies. | Required | Authenticated | None | CompanyListResponse | 200 | Safe | CR-003 |
| API-CMP-002 | GET | `/api/v1/companies/{companyId}` | Retrieve company profile. | Required | Authenticated | None | CompanyProfileResponse | 200 | Safe | CR-004 |
| API-CMP-003 | PUT | `/api/v1/users/me/preferences/company` | Set target company. | Required | Self | SetCompanyTargetRequest | UserPreferenceResponse | 200 | Idempotent | CR-003 |
| API-CMP-004 | GET | `/api/v1/company-readiness/current` | Retrieve current company readiness. | Required | Owner | None | CompanyReadinessResponse | 200 | Safe | CR-004 |
| API-CMP-005 | GET | `/api/v1/company-readiness/compare` | Compare company readiness results. | Required | Owner | None | CompanyReadinessComparisonResponse | 200 | Safe | CR-004 |

Readiness responses expose calculated results and version references. Transport contracts do not perform readiness calculation.

The implemented API-CAR-001/002 subset reads the nine SRS-supported careers and their immutable active profiles from
PostgreSQL. Backend and Frontend use approved `career-v2`; the other supported catalogs retain `career-v1`. Profile responses expose localized names, purpose, configured technologies,
required and preferred competencies, evaluation categories, priority labels, roadmap-template order, and exact
profile version metadata. Catalog endpoints do not calculate readiness, gaps, technical scores, or recommendation priority.
API-CAR-003 target validation resolves the same authoritative career catalog rather than a separate browser or
configuration list. Extension candidates remain excluded.

The implemented API-CAR-004/005/006 subset returns owner-scoped immutable `readiness-v1` results and ordered category
comparisons for Backend and Frontend. A completed result exposes the separately weighted score and confidence; an
unsupported required category produces `INSUFFICIENT_EVIDENCE` with null score and level. These endpoints never
accept client-supplied weights, scores, levels, gaps, confidence, or recommendation priority.
The browser exposes the current result at `/career-readiness` and historical results at
`/career-readiness/{careerReadinessId}`. Recommendation-set history is the discoverable entry point to its source
readiness result, and every gap links to the current owner-scoped skill/evidence drilldown without recalculating the
historical score or recommendation priority.

The implemented API-CMP-001/002 subset reads the six SRS-supported companies and immutable active `company-v1`
profiles from PostgreSQL. It exposes only generic technology focus, engineering culture, competency emphasis,
recommendation-type ordering, and configured increase-only policy labels. It does not expose or imply confidential
hiring criteria, readiness, interview prediction, or guaranteed outcomes. API-CMP-003 target validation uses this
same catalog; extension candidates remain unavailable.

## 16. Recommendation APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-REC-001 | POST | `/api/v1/recommendation-requests` | Create recommendation request. | Required | Owner | CreateRecommendationRequest | JobStatusResponse or RecommendationSetResponse | 202/201 | Required | CR-009~CR-014 |
| API-REC-002 | GET | `/api/v1/recommendations/current` | Retrieve current recommendations. | Required | Owner | None | RecommendationListResponse | 200 | Safe | CR-009 |
| API-REC-003 | GET | `/api/v1/recommendations/{recommendationId}` | Retrieve recommendation detail. | Required | Owner | None | RecommendationResponse | 200 | Safe | CR-009 |
| API-REC-004 | GET | `/api/v1/recommendations` | List recommendation history. | Required | Owner | None | RecommendationListResponse | 200 | Safe | CR-009~CR-014 |
| API-REC-005 | POST | `/api/v1/recommendations/{recommendationId}/accept` | Accept recommendation. | Required | Owner | RecommendationActionRequest | RecommendationResponse | 200 | Required | CR-009 |
| API-REC-006 | POST | `/api/v1/recommendations/{recommendationId}/dismiss` | Dismiss recommendation. | Required | Owner | RecommendationActionRequest | RecommendationResponse | 200 | Required | CR-009 |
| API-REC-007 | POST | `/api/v1/recommendations/{recommendationId}/complete` | Mark recommendation complete. | Required | Owner | RecommendationActionRequest | RecommendationResponse | 200 | Required | CR-009 |
| API-REC-008 | GET | `/api/v1/recommendations/{recommendationId}/evidence` | Retrieve recommendation evidence. | Required | Owner | None | EvidenceListResponse | 200 | Safe | CR-009 |

Recommendation responses must distinguish deterministic recommendation data from optional AI-generated explanation fields.

The implemented MVP API-REC-002/003/004/008 subset returns the current owner-scoped `recommendation-v1` set, immutable
recommendation-set history, individual recommendation detail, and linked observed evidence generated from an immutable
Backend/Frontend CareerReadiness result. Each item exposes its source gap, category, configured type, deterministic
priority, rationale code, effort, completion criteria, expected evidence, and owner-scoped observed evidence. No AI
prose or company modifier is included. Accept, dismiss, and complete mutations remain unimplemented until their allowed
state-transition contract is approved.

## 17. Learning Roadmap APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-LRN-001 | POST | `/api/v1/learning-roadmaps` | Create roadmap from recommendations. | Required | Owner | CreateLearningRoadmapRequest | LearningRoadmapResponse | 201/202 | Required | CR-006 |
| API-LRN-002 | GET | `/api/v1/learning-roadmaps/active` | Retrieve active roadmap. | Required | Owner | None | LearningRoadmapResponse | 200 | Safe | CR-006 |
| API-LRN-003 | GET | `/api/v1/learning-roadmaps` | List historical roadmaps. | Required | Owner | None | LearningRoadmapListResponse | 200 | Safe | CR-006 |
| API-LRN-004 | GET | `/api/v1/learning-roadmaps/{roadmapId}` | Retrieve roadmap detail. | Required | Owner | None | LearningRoadmapResponse | 200 | Safe | CR-006 |
| API-LRN-005 | PATCH | `/api/v1/learning-roadmaps/{roadmapId}` | Update user-editable roadmap status. | Required | Owner | UpdateRoadmapRequest | LearningRoadmapResponse | 200 | ETag recommended | CR-006 |
| API-LRN-006 | PATCH | `/api/v1/learning-roadmaps/{roadmapId}/steps/{stepId}` | Update step progress. | Required | Owner | UpdateRoadmapStepRequest | RoadmapStepResponse | 200 | ETag recommended | CR-006 |
| API-LRN-007 | POST | `/api/v1/learning-roadmaps/{roadmapId}/steps/{stepId}/complete` | Complete step. | Required | Owner | RoadmapStepActionRequest | RoadmapStepResponse | 200 | Required | CR-006 |
| API-LRN-008 | POST | `/api/v1/learning-roadmaps/{roadmapId}/steps/{stepId}/skip` | Skip step. | Required | Owner | RoadmapStepActionRequest | RoadmapStepResponse | 200 | Required | CR-006 |
| API-LRN-009 | POST | `/api/v1/learning-roadmaps/{roadmapId}/archive` | Archive roadmap. | Required | Owner | None | LearningRoadmapResponse | 200 | Required | CR-006 |
| API-LRN-010 | GET | `/api/v1/learning-resources` | Retrieve learning resources. | Required | Owner | None | LearningResourceListResponse | 200 | Safe | CR-006 |

User-editable roadmap fields include display title, user notes, target dates, and progress state. Generated reasoning, source recommendation references, and deterministic ordering basis are immutable.

The implemented MVP API-LRN-002/003/004/009 subset returns the active roadmap, owner-scoped roadmap history, and
roadmap detail, and supports CSRF-protected idempotent archival. `roadmap-v1` milestones and steps expose deterministic
order, prerequisite step IDs, configured effort and difficulty, completion criteria, expected evidence, and lifecycle
status. Archival changes only the roadmap lifecycle state and updated timestamp; generated structure, official progress,
and source references remain unchanged. Step-progress and other user-editable mutations remain unimplemented because
their official progress formula and transition rules require an approved deterministic contract.

## 18. Knowledge APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-KNW-001 | POST | `/api/v1/knowledge-documents` | Create knowledge document metadata. | Required | Owner | CreateKnowledgeDocumentRequest | KnowledgeDocumentResponse | 201 | Required | KR-001~KR-006 |
| API-KNW-002 | POST | `/api/v1/knowledge-documents/uploads` | Upload knowledge source metadata/content handle. | Required | Owner | KnowledgeUploadRequest | FileUploadResponse | 201/202 | Required | KR-001 |
| API-KNW-003 | POST | `/api/v1/knowledge-documents/imports/notion` | Import Notion knowledge. | Required | Owner/Notion permission | ImportNotionKnowledgeRequest | JobStatusResponse | 202 | Required | KR-001 |
| API-KNW-004 | GET | `/api/v1/knowledge-documents` | List knowledge documents. | Required | Owner | None | KnowledgeDocumentListResponse | 200 | Safe | KR-001~KR-020 |
| API-KNW-005 | GET | `/api/v1/knowledge-documents/{documentId}` | Retrieve knowledge document metadata. | Required | Owner | None | KnowledgeDocumentResponse | 200 | Safe | KR-001~KR-020 |
| API-KNW-006 | POST | `/api/v1/knowledge-documents/{documentId}/archive` | Archive knowledge document. | Required | Owner | ArchiveResourceRequest | KnowledgeDocumentResponse | 200 | Required | KR-016~KR-020 |
| API-KNW-007 | POST | `/api/v1/knowledge-ingestion-jobs` | Create ingestion job. | Required | Owner | CreateKnowledgeIngestionJobRequest | JobStatusResponse | 202 | Required | KR-001~KR-012 |
| API-KNW-008 | GET | `/api/v1/knowledge-ingestion-jobs/{jobId}` | Retrieve ingestion status. | Required | Owner | None | JobStatusResponse | 200 | Safe | KR-001~KR-012 |
| API-KNW-009 | POST | `/api/v1/knowledge-search` | Search knowledge. | Required | Owner | KnowledgeSearchRequest | KnowledgeSearchResponse | 200 | Optional | KR-013~KR-015 |
| API-KNW-010 | GET | `/api/v1/knowledge-documents/{documentId}/chunks` | Retrieve chunk summaries. | Required | Owner | None | KnowledgeChunkSummaryListResponse | 200 | Safe | KR-007~KR-009 |
| API-KNW-011 | POST | `/api/v1/knowledge-documents/{documentId}/reindex` | Re-index document. | Required | Owner | ReindexKnowledgeRequest | JobStatusResponse | 202 | Required | KR-010~KR-020 |

Knowledge APIs must not expose raw embedding vectors and must not generate final AI responses.

## 19. Prompt APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-PRM-001 | GET | `/api/v1/prompt-templates` | List prompt template metadata. | Required | Admin or permitted metadata consumer | None | PromptTemplateListResponse | 200 | Safe | PR-001~PR-015 |
| API-PRM-002 | GET | `/api/v1/prompt-templates/{templateId}` | Retrieve prompt template metadata. | Required | Admin or permitted metadata consumer | None | PromptTemplateResponse | 200 | Safe | PR-001~PR-015 |
| API-PRM-003 | POST | `/api/v1/prompt-contexts` | Create prompt context. | Required | Owner | CreatePromptContextRequest | PromptContextResponse | 201 | Required | PR-006~PR-010 |
| API-PRM-004 | GET | `/api/v1/prompt-contexts/{promptContextId}` | Retrieve prompt context metadata and permitted source refs. | Required | Owner/Admin metadata | None | PromptContextResponse | 200 | Safe | PR-006 |
| API-PRM-005 | POST | `/api/v1/prompt-contexts/{promptContextId}/validate` | Validate prompt context. | Required | Owner | ValidatePromptContextRequest | PromptValidationResponse | 200 | Required | PR-008 |
| API-PRM-006 | POST | `/api/v1/prompt-contexts/preview` | Preview composition metadata. | Required | Owner | PromptContextPreviewRequest | PromptContextPreviewResponse | 200 | Optional | PR-006~PR-010 |
| API-PRM-007 | GET | `/api/v1/prompt-templates/{templateId}/versions/{versionId}` | Retrieve prompt version metadata. | Required | Admin | None | PromptTemplateVersionResponse | 200 | Safe | PR-009 |
| API-PRM-008 | POST | `/api/v1/admin/prompt-templates/{templateId}/versions/{versionId}/activate` | Activate prompt template version. | Required | Admin | AdminActivationRequest | PromptTemplateVersionResponse | 200 | Required | PR-009 |
| API-PRM-009 | POST | `/api/v1/admin/prompt-templates/{templateId}/versions/{versionId}/deprecate` | Deprecate prompt template version. | Required | Admin | AdminDeprecationRequest | PromptTemplateVersionResponse | 200 | Required | PR-009 |

Normal users must not receive hidden system prompt text or sensitive internal instructions.

## 20. AI Generation APIs

### 20.1 Supported Generation Task Types

| Task Type | Supported Purpose |
|---|---|
| `REPOSITORY_REVIEW` | Review repository based on deterministic analysis and evidence. |
| `SKILL_ANALYSIS_EXPLANATION` | Explain Skill Matrix without recalculating scores. |
| `CAREER_COACHING` | Generate grounded career coaching. |
| `PORTFOLIO_GENERATION` | Generate portfolio draft. |
| `RESUME_GENERATION` | Generate resume draft. |
| `README_GENERATION` | Generate README improvement draft. |
| `INTERVIEW_QUESTION_GENERATION` | Generate interview question set. |
| `LEARNING_RECOMMENDATION` | Explain learning roadmap and recommendations. |
| `TECHNOLOGY_RECOMMENDATION` | Explain technology learning suggestions based on deterministic context. |
| `ARCHITECTURE_REVIEW` | Explain architecture signals and improvement ideas. |
| `PROJECT_EXPLANATION` | Generate project explanation from grounded project facts. |

### 20.2 Endpoint Contracts

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-AI-001 | POST | `/api/v1/generation-requests` | Create generation request. | Required | Owner | CreateGenerationRequest | GenerationJobResponse | 202 | Required | AI-001~AI-015 |
| API-AI-002 | GET | `/api/v1/generation-jobs/{jobId}` | Retrieve generation job status. | Required | Owner | None | GenerationJobResponse | 200 | Safe | AI-001~AI-015 |
| API-AI-003 | POST | `/api/v1/generation-jobs/{jobId}/cancel` | Cancel generation job when supported. | Required | Owner | CancelJobRequest | GenerationJobResponse | 200 | Required | AI-001~AI-015 |
| API-AI-004 | POST | `/api/v1/generation-jobs/{jobId}/retry` | Retry failed generation using same source context or new policy. | Required | Owner | RetryJobRequest | GenerationJobResponse | 202 | Required | AI-001~AI-015 |
| API-AI-005 | GET | `/api/v1/generated-artifacts/{artifactId}` | Retrieve generated artifact. | Required | Owner | None | GeneratedArtifactResponse | 200 | Safe | AI-005~AI-007 |
| API-AI-006 | GET | `/api/v1/generated-artifacts` | List generated artifacts. | Required | Owner | None | GeneratedArtifactListResponse | 200 | Safe | AI-005~AI-007 |
| API-AI-007 | GET | `/api/v1/generated-artifacts/{artifactId}/validation` | Retrieve validation status. | Required | Owner | None | ResponseValidationResponse | 200 | Safe | AI-001~AI-015 |

The API must never accept client-supplied authoritative Rule Score, Career Readiness Score, Company Readiness Score, recommendation priority, or rule weight.

## 21. Portfolio APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-PRT-001 | POST | `/api/v1/portfolios` | Create portfolio. | Required | Owner | CreatePortfolioRequest | PortfolioResponse | 201 | Required | AI-005 |
| API-PRT-002 | POST | `/api/v1/portfolios/{portfolioId}/generate` | Generate portfolio content. | Required | Owner | GeneratePortfolioRequest | JobStatusResponse | 202 | Required | AI-005 |
| API-PRT-003 | GET | `/api/v1/portfolios/{portfolioId}` | Retrieve portfolio. | Required or public token if published | Owner/public permission | None | PortfolioResponse | 200 | Safe | AI-005 |
| API-PRT-004 | PATCH | `/api/v1/portfolios/{portfolioId}` | Update user-editable metadata. | Required | Owner | UpdatePortfolioRequest | PortfolioResponse | 200 | ETag recommended | AI-005 |
| API-PRT-005 | GET | `/api/v1/portfolios/{portfolioId}/versions` | Retrieve versions. | Required | Owner | None | PortfolioVersionListResponse | 200 | Safe | AI-005 |
| API-PRT-006 | POST | `/api/v1/portfolios/{portfolioId}/publish` | Publish portfolio. | Required | Owner | PublishArtifactRequest | PortfolioResponse | 200 | Required | AI-005 |
| API-PRT-007 | POST | `/api/v1/portfolios/{portfolioId}/unpublish` | Unpublish portfolio. | Required | Owner | ArtifactActionRequest | PortfolioResponse | 200 | Required | AI-005 |
| API-PRT-008 | POST | `/api/v1/portfolios/{portfolioId}/exports` | Export portfolio. | Required | Owner | ExportArtifactRequest | JobStatusResponse | 202 | Required | AI-005 |
| API-PRT-009 | POST | `/api/v1/portfolios/{portfolioId}/archive` | Archive portfolio. | Required | Owner | ArchiveResourceRequest | PortfolioResponse | 200 | Required | AI-005 |
| API-PRT-010 | GET | `/api/v1/portfolios/{portfolioId}/evidence` | Retrieve generation evidence. | Required | Owner | None | EvidenceListResponse | 200 | Safe | AI-005 |

Portfolio states are `DRAFT`, `GENERATED`, `REVIEWED`, `PUBLISHED`, and `ARCHIVED`.

## 22. Resume APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-RSM-001 | POST | `/api/v1/resumes` | Create resume. | Required | Owner | CreateResumeRequest | ResumeResponse | 201 | Required | AI-006 |
| API-RSM-002 | POST | `/api/v1/resumes/{resumeId}/generate` | Generate resume. | Required | Owner | GenerateResumeRequest | JobStatusResponse | 202 | Required | AI-006 |
| API-RSM-003 | GET | `/api/v1/resumes/{resumeId}` | Retrieve resume. | Required | Owner | None | ResumeResponse | 200 | Safe | AI-006 |
| API-RSM-004 | PATCH | `/api/v1/resumes/{resumeId}` | Update user-editable fields. | Required | Owner | UpdateResumeRequest | ResumeResponse | 200 | ETag recommended | AI-006 |
| API-RSM-005 | GET | `/api/v1/resumes/{resumeId}/versions` | Retrieve resume versions. | Required | Owner | None | ResumeVersionListResponse | 200 | Safe | AI-006 |
| API-RSM-006 | POST | `/api/v1/resumes/{resumeId}/exports` | Export resume. | Required | Owner | ExportArtifactRequest | JobStatusResponse | 202 | Required | AI-006 |
| API-RSM-007 | POST | `/api/v1/resumes/{resumeId}/archive` | Archive resume. | Required | Owner | ArchiveResourceRequest | ResumeResponse | 200 | Required | AI-006 |
| API-RSM-008 | PATCH | `/api/v1/resumes/{resumeId}/target` | Update target career/company context for resume draft. | Required | Owner | ResumeTargetRequest | ResumeResponse | 200 | ETag recommended | AI-006 |

Resume responses must identify user-authored content and generated content separately.

## 23. Interview Question APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-ITV-001 | POST | `/api/v1/interview-question-sets/generation-requests` | Create interview question generation request. | Required | Owner | GenerateInterviewQuestionSetRequest | JobStatusResponse | 202 | Required | AI-007 |
| API-ITV-002 | GET | `/api/v1/interview-question-sets/{questionSetId}` | Retrieve question set. | Required | Owner | None | InterviewQuestionSetResponse | 200 | Safe | AI-007 |
| API-ITV-003 | GET | `/api/v1/interview-question-sets` | List question sets. | Required | Owner | None | InterviewQuestionSetListResponse | 200 | Safe | AI-007 |
| API-ITV-004 | GET | `/api/v1/interview-question-sets/{questionSetId}/questions/{questionId}` | Retrieve question detail. | Required | Owner | None | InterviewQuestionResponse | 200 | Safe | AI-007 |
| API-ITV-005 | POST | `/api/v1/interview-question-sets/{questionSetId}/questions/{questionId}/practice-answers` | Submit practice answer. | Required | Owner | SubmitPracticeAnswerRequest | PracticeAnswerResponse | 201 | Required | AI-007 |
| API-ITV-006 | POST | `/api/v1/interview-question-sets/{questionSetId}/questions/{questionId}/feedback-requests` | Request feedback on answer. | Required | Owner | RequestAnswerFeedbackRequest | JobStatusResponse | 202 | Required | AI-007 |
| API-ITV-007 | POST | `/api/v1/interview-question-sets/{questionSetId}/archive` | Archive question set. | Required | Owner | ArchiveResourceRequest | InterviewQuestionSetResponse | 200 | Required | AI-007 |

Question generation and answer feedback are separate. The API must not imply deterministic grading unless a future deterministic evaluation requirement exists.

## 24. Dashboard APIs

| ID | Method | Path | Purpose | Auth | Authorization | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|
| API-DSH-001 | GET | `/api/v1/dashboard/summary` | Retrieve dashboard summary. | Required | Owner | DashboardSummaryResponse | 200 | Safe | FR-281~FR-320 |
| API-DSH-002 | GET | `/api/v1/dashboard/repositories/status` | Retrieve repository sync/status summary. | Required | Owner | RepositoryStatusSummaryResponse | 200 | Safe | FR-281~FR-320 |
| API-DSH-003 | GET | `/api/v1/dashboard/analyses/latest` | Retrieve latest analysis summary. | Required | Owner | AnalysisSummaryResponse | 200 | Safe | FR-281~FR-320 |
| API-DSH-004 | GET | `/api/v1/dashboard/skills/overview` | Retrieve skill overview. | Required | Owner | SkillOverviewResponse | 200 | Safe | FR-281~FR-320 |
| API-DSH-005 | GET | `/api/v1/dashboard/career-readiness` | Retrieve career readiness card. | Required | Owner | CareerReadinessSummaryResponse | 200 | Safe | FR-281~FR-320 |
| API-DSH-006 | GET | `/api/v1/dashboard/company-readiness` | Retrieve company readiness card. | Required | Owner | CompanyReadinessSummaryResponse | 200 | Safe | FR-281~FR-320 |
| API-DSH-007 | GET | `/api/v1/dashboard/roadmap/active` | Retrieve active roadmap card. | Required | Owner | RoadmapSummaryResponse | 200 | Safe | FR-281~FR-320 |
| API-DSH-008 | GET | `/api/v1/dashboard/recommendations/recent` | Retrieve recent recommendations. | Required | Owner | RecommendationListResponse | 200 | Safe | FR-281~FR-320 |
| API-DSH-009 | GET | `/api/v1/dashboard/artifacts/recent` | Retrieve recent generated artifacts. | Required | Owner | GeneratedArtifactListResponse | 200 | Safe | FR-281~FR-320 |
| API-DSH-010 | GET | `/api/v1/dashboard/synchronization/status` | Retrieve synchronization status. | Required | Owner | SynchronizationStatusResponse | 200 | Safe | FR-281~FR-320 |

Dashboard APIs are read models only and must not become sources of truth.

Implemented API-DSH-001 scope: the owner-scoped summary composes selected career/company targets, active repository
counts and recent repository state, completed analysis count/latest/current results, current Skill Matrix, current career
readiness, the top three deterministic recommendations, active learning-roadmap progress, and the eight most recent
repository-sync/analysis jobs. Every section reports `AVAILABLE`, `EMPTY`, or `UNAVAILABLE`; a failed optional source
does not hide other authoritative results. The endpoint records a durable dashboard-view audit event. It does not
calculate official scores, persist a dashboard projection, introduce a cache, or implement API-DSH-002 through 010,
company readiness, artifacts, charts, filters, export, or AI-generated dashboard content.

## 25. Administration APIs

| ID | Method | Path | Purpose | Auth | Authorization | Request Body | Response Body | Status Codes | Idempotency | Requirements |
|---|---|---|---|---|---|---|---|---|---|---|
| API-ADM-001 | GET | `/api/v1/admin/careers` | List career configuration. | Required | Admin | None | AdminCareerConfigListResponse | 200 | Safe | FR-341~FR-360 |
| API-ADM-002 | POST | `/api/v1/admin/careers/{careerId}/versions` | Create career profile version metadata. | Required | Admin | AdminCareerProfileVersionRequest | CareerProfileResponse | 201 | Required | FR-341~FR-360 |
| API-ADM-003 | GET | `/api/v1/admin/companies` | List company configuration. | Required | Admin | None | AdminCompanyConfigListResponse | 200 | Safe | FR-341~FR-360 |
| API-ADM-004 | POST | `/api/v1/admin/companies/{companyId}/versions` | Create company profile version metadata. | Required | Admin | AdminCompanyProfileVersionRequest | CompanyProfileResponse | 201 | Required | FR-341~FR-360 |
| API-ADM-005 | GET | `/api/v1/admin/rule-sets` | List rule version metadata. | Required | Admin | None | AdminRuleSetListResponse | 200 | Safe | FR-341~FR-360 |
| API-ADM-006 | POST | `/api/v1/admin/rule-sets/{ruleSetId}/versions/{versionId}/activate` | Activate rule version. | Required | Admin | AdminActivationRequest | RuleSetVersionResponse | 200 | Required | RR-001~RR-010 |
| API-ADM-007 | GET | `/api/v1/admin/prompt-templates` | List prompt template metadata. | Required | Admin | None | PromptTemplateListResponse | 200 | Safe | PR-001~PR-015 |
| API-ADM-008 | GET | `/api/v1/admin/provider-status` | Retrieve provider status. | Required | Admin | None | ProviderStatusResponse | 200 | Safe | FR-341~FR-360 |
| API-ADM-009 | GET | `/api/v1/admin/jobs` | Inspect jobs. | Required | Admin | None | AdminJobListResponse | 200 | Safe | FR-341~FR-360 |
| API-ADM-010 | GET | `/api/v1/admin/audit-records` | Access audit records. | Required | Privileged admin | None | AuditRecordListResponse | 200 | Safe | FR-341~FR-360 |
| API-ADM-011 | POST | `/api/v1/admin/users/{userId}/support-actions` | Perform user support operation. | Required | Privileged admin | AdminUserSupportActionRequest | AdminUserSupportActionResponse | 202/200 | Required | FR-341~FR-360 |

Administrative APIs must not expose infrastructure secrets, provider tokens, hidden system prompt content to unauthorized users, or raw private user content unless explicitly authorized by policy.

## 26. Asynchronous Job Contract

### 26.1 Common Job Model

| Field | Type | Required | Description |
|---|---|---:|---|
| jobId | string | Yes | Opaque job identifier. |
| jobType | enum | Yes | Type such as `REPOSITORY_SYNC`, `ANALYSIS`, `KNOWLEDGE_INGESTION`, `EMBEDDING_GENERATION`, `AI_GENERATION`, `PORTFOLIO_GENERATION`, `RESUME_GENERATION`, `EXPORT_GENERATION`. |
| status | enum | Yes | `queued`, `running`, `succeeded`, `failed`, `cancelled`, `expired`. |
| progress | object | Optional | Percentage or phase-based progress. |
| submittedAt | timestamp | Yes | Submission timestamp. |
| startedAt | timestamp | Optional | Start timestamp. |
| completedAt | timestamp | Optional | Completion timestamp. |
| resultResourceUrl | string | Optional | URL for result resource when available. |
| error | ErrorResponse | Optional | Error reference for failed job. |
| retryPolicy | object | Optional | Whether retry is allowed and retry constraints. |
| cancellation | object | Optional | Whether cancellation is supported in current state. |
| expiresAt | timestamp | Optional | Job metadata expiration timestamp. |

### 26.2 Job State Rules

| State | Meaning | Valid Next States |
|---|---|---|
| queued | Accepted but not started. | running, cancelled, expired |
| running | Work is in progress. | succeeded, failed, cancelled |
| succeeded | Work completed successfully. | expired |
| failed | Work failed. | queued by retry, expired |
| cancelled | User or system cancelled before completion. | expired |
| expired | Job metadata is no longer active. | None |

### 26.3 Job Types

| Job Type | Result Reference | Cancellation | Retry |
|---|---|---|---|
| Repository synchronization | Repository or RepositorySnapshot | Supported before finalization | Supported |
| Repository analysis | Analysis or SkillMatrix | Supported before rule execution completes | Supported |
| Knowledge ingestion | KnowledgeDocument | Supported before indexing finalization | Supported |
| Embedding generation | KnowledgeDocument or chunk index status | Usually system-controlled | Supported |
| AI generation | GeneratedArtifact or validation failure | Supported while queued/running where provider allows | Supported |
| Portfolio generation | Portfolio or GeneratedArtifact | Supported while queued/running | Supported |
| Resume generation | Resume or GeneratedArtifact | Supported while queued/running | Supported |
| Export generation | Artifact export URL | Supported before file creation | Supported |

Long-running operations should return `202 Accepted` with `JobStatusResponse`.

## 27. Pagination, Filtering, and Sorting

### 27.1 Pagination Rules

| Rule | Contract |
|---|---|
| Default style | Cursor pagination for large or changing collections. |
| Offset exceptions | Offset pagination may be used for small stable admin reference lists only. |
| Default page size | 20 items. |
| Maximum page size | 100 items unless endpoint states lower limit. |
| Total count | Optional; may be null for expensive large collections. |
| Stable sort | Required for cursor pagination. |

### 27.2 Filter and Sort Syntax

| Feature | Contract |
|---|---|
| Status filter | `status=ACTIVE` or comma-separated statuses where supported. |
| Date range | `from` and `to` ISO 8601 values. |
| Archived resources | `includeArchived=true` where supported. |
| Search query | `q` for text search; `query` field for semantic knowledge search body. |
| Sort | `field.direction`, such as `createdAt.desc`. |
| Field selection | `fields=id,name,status` where supported. |

## 28. Idempotency and Concurrency

### 28.1 Idempotency Behavior

| Operation Type | Idempotency Requirement | Duplicate Handling |
|---|---|---|
| Synchronization request | Required | Same key and body returns original job/resource. |
| Analysis request | Required | Same snapshot/rule basis returns existing job or completed analysis. |
| Generation request | Required | Same key and body returns original generation job. |
| Export request | Required | Same key and body returns original export job. |
| Connection callback | Required by OAuth state | Duplicate callback is ignored or returns connection result. |
| Destructive request | Required | Repeated request returns current deletion/archive state. |

### 28.2 Concurrency Contract

| Area | Contract |
|---|---|
| Mutable resources | Use resource version or ETag where concurrent user edits are possible. |
| Immutable resources | Updates return `RESOURCE_CONFLICT`. |
| Job resources | Status updates are server-owned; clients poll or cancel through documented actions. |
| Duplicate request with different body | Return `DUPLICATE_REQUEST` or `RESOURCE_CONFLICT`. |
| Idempotency expiration | Expiration policy is documented per operation class; default is TBD. |

## 29. API Versioning and Compatibility

| Topic | Contract |
|---|---|
| URI versioning | Initial public API uses `/api/v1`. |
| Backward compatibility | Additive fields and endpoints are compatible. |
| Breaking changes | Require new API version or explicit migration period. |
| Field deprecation | Deprecated fields remain for published sunset window. |
| Endpoint deprecation | Deprecation metadata and documentation must identify replacement. |
| Sunset communication | Use response headers and release notes where applicable. |
| Client version compatibility | `X-Client-Version` may be used for diagnostics and warnings. |
| Enum evolution | Clients must tolerate unknown enum values. |
| Unknown fields | Clients should ignore unknown response fields; servers should ignore unknown request fields only where safe. |

## 30. Rate Limiting and Quotas

### 30.1 Rate Limit Categories

| Category | Applies To | Contract Behavior |
|---|---|---|
| Authenticated user limits | User API requests | Return `429 RATE_LIMIT_EXCEEDED` when exceeded. |
| Anonymous limits | Public and callback endpoints where applicable | Conservative limits and replay protection. |
| Integration provider limits | GitHub/Notion sync | Surface provider-safe rate limit status without secrets. |
| AI generation quotas | Generation requests | Reject or queue according to quota policy. |
| Export quotas | Portfolio/resume export | Limit concurrent and daily export generation. |
| Administrative exceptions | Admin APIs | Higher limits may apply but must be audited. |

### 30.2 Rate Limit Headers

| Header | Description |
|---|---|
| `X-RateLimit-Limit` | Request limit for current window when disclosed. |
| `X-RateLimit-Remaining` | Remaining quota when disclosed. |
| `X-RateLimit-Reset` | Reset time when disclosed. |
| `Retry-After` | Required for 429 when retry timing is known. |

## 31. Webhook and Callback Contracts

| Callback Type | Endpoint Pattern | Signature Expectation | Replay Protection | Idempotency | Acknowledgement |
|---|---|---|---|---|---|
| GitHub application-login callback | Backend security route configured for Spring Security OAuth2 Login | OAuth state and PKCE where supported | State, authorization-code reuse prevention, session rotation | State/code based | Safe frontend redirect or authentication failure |
| GitHub integration callback | `/api/v1/integrations/github/callback` | OAuth state validation bound to authenticated account-link context | State and timestamp where applicable | State-based | 200 or redirect result |
| Notion OAuth callback | `/api/v1/integrations/notion/callback` | OAuth state validation | State and timestamp where applicable | State-based | 200 or redirect result |
| Future GitHub webhook | `/api/v1/webhooks/github` | Provider signature required | Event ID and timestamp | Event ID | 2xx after accepted |
| Future Notion events | `/api/v1/webhooks/notion` | Provider signature required where supported | Event ID and timestamp | Event ID | 2xx after accepted |
| Generation completion notification | Future callback or webhook | DevPath signed event | Event ID | Event ID | Consumer acknowledgement |
| Export completion notification | Future callback or webhook | DevPath signed event | Event ID | Event ID | Consumer acknowledgement |

Secret values and signing keys are not defined in this document.

## 32. API Security Constraints

| Constraint | Contract |
|---|---|
| HTTPS only | All production APIs require HTTPS. |
| Session cookie | Production session cookie is Secure, HttpOnly, SameSite=Lax, host-only by default, and never exposed through response bodies. |
| CSRF | State-changing cookie-authenticated requests require server validation of the configured CSRF token/header. |
| CORS | Credentialed CORS is deny-by-default and limited to explicit frontend origins; wildcard origins are prohibited. |
| Request validation | Requests must validate content type, schema, size, enum values, IDs, and authorization scope. |
| Payload size limits | Default limits apply; upload endpoints define separate policies. |
| File upload validation | Uploaded files require category, size, type, ownership, and malware scanning expectations. |
| Authorization checks | Every user-owned resource requires ownership validation. |
| Sensitive field redaction | Tokens, secrets, internal prompts, raw provider payloads, and stack traces are never returned. |
| Safe errors | Error messages must be user-safe and must not leak private resource existence when unsafe. |
| Webhook verification | Provider callback or webhook requests require replay protection and signature/state validation. |
| Rate limiting | Public, user, admin, provider, AI, and export limits are enforced by contract. |
| Replay protection | Idempotency keys, timestamps, provider event IDs, and OAuth state support replay resistance. |

## 33. File Upload and Export Contract

### 33.1 Upload Contract

| Upload Type | Endpoint | Allowed Categories | Response |
|---|---|---|---|
| Knowledge document upload | `/api/v1/knowledge-documents/uploads` | Learning note, retrospective, project document, architecture document, portfolio/resume source where supported | FileUploadResponse or JobStatusResponse |
| Repository archive handling | Internal or future endpoint | Repository archive where explicitly supported | Object reference metadata |

Uploaded binary content must not be embedded directly in JSON. APIs return object references, upload session metadata, or job status.

### 33.2 Export Contract

| Export Type | Endpoint | Result |
|---|---|---|
| Portfolio export | `/api/v1/portfolios/{portfolioId}/exports` | JobStatusResponse then temporary download URL or artifact export resource. |
| Resume export | `/api/v1/resumes/{resumeId}/exports` | JobStatusResponse then temporary download URL or artifact export resource. |
| Generated PDF access | Export result URL | Temporary download URL with expiration metadata. |

### 33.3 File Metadata and Safety

| Field | Description |
|---|---|
| fileId | Opaque file reference. |
| fileName | User-safe display name. |
| fileCategory | Supported category. |
| contentType | Validated media type. |
| sizeBytes | File size. |
| expiresAt | Expiration for temporary access. |
| scanStatus | Malware scanning status when applicable. |
| ownerScope | User or future organization ownership scope. |

## 34. Observability Metadata

| Metadata | Purpose | Exposure |
|---|---|---|
| requestId | Identifies one API request. | Response metadata and error responses. |
| correlationId | Connects related requests/jobs. | Response metadata and job resources. |
| traceId | Internal tracing reference. | May be exposed in safe form. |
| jobId | Identifies async job. | Job responses. |
| providerRequestReference | Safe external provider request category/reference. | Admin or diagnostic responses only. |
| clientVersion | Client compatibility diagnostics. | Request metadata and logs. |
| apiVersion | API version used. | Response metadata. |
| durationMs | Execution duration where appropriate. | Response metadata for diagnostics. |

Internal stack traces and sensitive provider payloads must not be exposed.

## 35. Endpoint Catalog

### 35.1 Consolidated Endpoint Catalog

| ID | Domain | Method | Path | Operation Name | Auth | Async | Idempotency | Request Schema | Response Schema | Status Codes | Requirements |
|---|---|---|---|---|---|---:|---|---|---|---|---|
| API-ID-001 | Identity | GET | `/api/v1/users/me` | GetCurrentUser | Required | No | Safe | None | UserResponse | 200 | FR-001~FR-020 |
| API-ID-002 | Identity | GET | `/api/v1/users/me/profile` | GetUserProfile | Required | No | Safe | None | UserProfileResponse | 200 | FR-006 |
| API-ID-003 | Identity | PATCH | `/api/v1/users/me/profile` | UpdateUserProfile | Required | No | ETag | UpdateUserProfileRequest | UserProfileResponse | 200 | FR-007 |
| API-ID-004 | Identity | GET | `/api/v1/users/me/connections` | ListConnections | Required | No | Safe | None | ConnectedAccountListResponse | 200 | FR-013 |
| API-ID-005 | Identity | GET | `/api/v1/users/me/preferences` | GetPreferences | Required | No | Safe | None | UserPreferenceResponse | 200 | FR-008~FR-009 |
| API-ID-006 | Identity | PUT | `/api/v1/users/me/preferences/career` | SetCareerTarget | Required | No | Required | SetCareerTargetRequest | UserPreferenceResponse | 200 | CR-001 |
| API-ID-007 | Identity | PUT | `/api/v1/users/me/preferences/company` | SetCompanyTarget | Required | No | Required | SetCompanyTargetRequest | UserPreferenceResponse | 200 | CR-003 |
| API-ID-008 | Identity | POST | `/api/v1/users/me/deletion-requests` | RequestDeletion | Required | Yes | Required | CreateDeletionRequest | JobStatusResponse | 202 | FR-012 |
| API-ID-010 | Identity | GET | `/api/v1/users/me/onboarding-progress` | GetOnboardingProgress | Required | No | Safe | None | OnboardingProgressResponse | 200 | FR-018~FR-019 |
| API-INT-001 | Integration | POST | `/api/v1/integrations/github/authorize` | InitiateGitHubOAuth | Required | No | Required | None | OAuthAuthorizationResponse | 200 | FR-021 |
| API-INT-002 | Integration | GET | `/api/v1/integrations/github/callback` | GitHubIntegrationCallback | Callback | No | State | ProviderCallback | OAuthCallbackResponse | 200/302 | FR-021 |
| API-INT-004 | Integration | GET | `/api/v1/integrations/github/repositories` | ListGitHubRepositories | Required | No | Safe | None | ProviderRepositoryListResponse | 200 | FR-024 |
| API-REP-001 | Repository | GET | `/api/v1/repositories` | ListRepositories | Required | No | Safe | None | RepositoryListResponse | 200 | FR-024~FR-050 |
| API-REP-002 | Repository | GET | `/api/v1/repositories/{repositoryId}` | GetRepository | Required | No | Safe | None | RepositoryDetailResponse | 200 | FR-025 |
| API-REP-006 | Repository | POST | `/api/v1/repositories/{repositoryId}/sync` | SyncRepository | Required | Yes | Required | SyncRepositoryRequest | JobStatusResponse | 202 | FR-026~FR-050 |
| API-REP-007 | Repository | GET | `/api/v1/repositories/{repositoryId}/snapshots` | ListSnapshots | Required | No | Safe | None | RepositorySnapshotListResponse | 200 | FR-026 |
| API-ANA-001 | Analysis | POST | `/api/v1/analyses` | CreateAnalysis | Required | Yes | Required | CreateAnalysisRequest | JobStatusResponse | 202 | FR-071~FR-180 |
| API-ANA-003 | Analysis | GET | `/api/v1/analyses/{analysisId}` | GetAnalysis | Required | No | Safe | None | AnalysisResultResponse | 200 | FR-071~FR-180 |
| API-ANA-004 | Rule | GET | `/api/v1/rule-evaluations/{evaluationId}` | GetRuleEvaluation | Required | No | Safe | None | RuleEvaluationResponse | 200 | RR-001~RR-010 |
| API-SKL-001 | Skill | GET | `/api/v1/skill-matrices/current` | GetCurrentSkillMatrix | Required | No | Safe | None | SkillMatrixResponse | 200 | RR-009 |
| API-CAR-001 | Career | GET | `/api/v1/careers` | ListCareers | Required | No | Safe | None | CareerListResponse | 200 | CR-001 |
| API-CAR-004 | Career | GET | `/api/v1/career-readiness/current` | GetCareerReadiness | Required | No | Safe | None | CareerReadinessResponse | 200 | CR-005~CR-007 |
| API-CMP-001 | Company | GET | `/api/v1/companies` | ListCompanies | Required | No | Safe | None | CompanyListResponse | 200 | CR-003 |
| API-CMP-004 | Company | GET | `/api/v1/company-readiness/current` | GetCompanyReadiness | Required | No | Safe | None | CompanyReadinessResponse | 200 | CR-004 |
| API-REC-001 | Recommendation | POST | `/api/v1/recommendation-requests` | GenerateRecommendations | Required | Yes | Required | CreateRecommendationRequest | JobStatusResponse | 202 | CR-009~CR-014 |
| API-REC-002 | Recommendation | GET | `/api/v1/recommendations/current` | GetCurrentRecommendations | Required | No | Safe | None | RecommendationListResponse | 200 | CR-009 |
| API-LRN-001 | Learning | POST | `/api/v1/learning-roadmaps` | CreateRoadmap | Required | Optional | Required | CreateLearningRoadmapRequest | LearningRoadmapResponse | 201/202 | CR-006 |
| API-LRN-006 | Learning | PATCH | `/api/v1/learning-roadmaps/{roadmapId}/steps/{stepId}` | UpdateRoadmapStep | Required | No | ETag | UpdateRoadmapStepRequest | RoadmapStepResponse | 200 | CR-006 |
| API-KNW-001 | Knowledge | POST | `/api/v1/knowledge-documents` | CreateKnowledgeDocument | Required | No | Required | CreateKnowledgeDocumentRequest | KnowledgeDocumentResponse | 201 | KR-001 |
| API-KNW-009 | Knowledge | POST | `/api/v1/knowledge-search` | SearchKnowledge | Required | No | Optional | KnowledgeSearchRequest | KnowledgeSearchResponse | 200 | KR-013~KR-015 |
| API-PRM-003 | Prompt | POST | `/api/v1/prompt-contexts` | CreatePromptContext | Required | No | Required | CreatePromptContextRequest | PromptContextResponse | 201 | PR-006~PR-010 |
| API-AI-001 | AI | POST | `/api/v1/generation-requests` | CreateGenerationRequest | Required | Yes | Required | CreateGenerationRequest | GenerationJobResponse | 202 | AI-001~AI-015 |
| API-AI-005 | AI | GET | `/api/v1/generated-artifacts/{artifactId}` | GetGeneratedArtifact | Required | No | Safe | None | GeneratedArtifactResponse | 200 | AI-005~AI-007 |
| API-PRT-001 | Portfolio | POST | `/api/v1/portfolios` | CreatePortfolio | Required | No | Required | CreatePortfolioRequest | PortfolioResponse | 201 | AI-005 |
| API-PRT-008 | Portfolio | POST | `/api/v1/portfolios/{portfolioId}/exports` | ExportPortfolio | Required | Yes | Required | ExportArtifactRequest | JobStatusResponse | 202 | AI-005 |
| API-RSM-001 | Resume | POST | `/api/v1/resumes` | CreateResume | Required | No | Required | CreateResumeRequest | ResumeResponse | 201 | AI-006 |
| API-RSM-006 | Resume | POST | `/api/v1/resumes/{resumeId}/exports` | ExportResume | Required | Yes | Required | ExportArtifactRequest | JobStatusResponse | 202 | AI-006 |
| API-ITV-001 | Interview | POST | `/api/v1/interview-question-sets/generation-requests` | GenerateInterviewQuestions | Required | Yes | Required | GenerateInterviewQuestionSetRequest | JobStatusResponse | 202 | AI-007 |
| API-DSH-001 | Dashboard | GET | `/api/v1/dashboard/summary` | GetDashboardSummary | Required | No | Safe | None | DashboardSummaryResponse | 200 | FR-281~FR-320 |
| API-ADM-001 | Administration | GET | `/api/v1/admin/careers` | ListAdminCareers | Required | No | Safe | None | AdminCareerConfigListResponse | 200 | FR-341~FR-360 |
| API-ADM-010 | Administration | GET | `/api/v1/admin/audit-records` | ListAuditRecords | Required | No | Safe | None | AuditRecordListResponse | 200 | FR-341~FR-360 |

### 35.2 Endpoint Count by Domain

| Domain | Count |
|---|---:|
| Identity | 9 |
| Integration | 12 |
| Repository | 12 |
| Analysis and Rule | 8 |
| Skill Matrix | 7 |
| Career and Company | 11 |
| Recommendation | 8 |
| Learning | 10 |
| Knowledge | 11 |
| Prompt | 9 |
| AI Generation | 7 |
| Portfolio | 10 |
| Resume | 8 |
| Interview | 7 |
| Dashboard | 10 |
| Administration | 11 |
| Total defined across detailed chapters | 150 |

## 36. Schema Catalog

### 36.1 Common Schemas

| Schema | Field | Type | Required | Nullable | Description | Validation | Example | Sensitivity | Source Domain |
|---|---|---|---:|---:|---|---|---|---|---|
| PaginationMetadata | limit | integer | Yes | No | Page size. | 1 to max page size. | 20 | Public metadata | API |
| PaginationMetadata | nextCursor | string | No | Yes | Cursor for next page. | Opaque. | `cur_next` | Public metadata | API |
| PaginationMetadata | totalCount | integer | No | Yes | Total count when available. | Non-negative. | null | Public metadata | API |
| ErrorResponse | code | string | Yes | No | Platform error code. | Must be documented error code. | `VALIDATION_ERROR` | Public safe | API |
| ErrorResponse | message | string | Yes | No | Developer-readable message. | No secrets. | `The request is invalid.` | Public safe | API |
| ErrorResponse | fieldErrors | array | No | No | Field validation errors. | Field names must be request fields. | `[]` | Public safe | API |
| JobStatusResponse | jobId | string | Yes | No | Job identifier. | Opaque. | `job_123` | User private | Job |
| JobStatusResponse | status | enum | Yes | No | Job state. | queued/running/succeeded/failed/cancelled/expired. | `running` | User private | Job |
| JobStatusResponse | resultResourceUrl | string | No | Yes | URL of created result. | Must be URL path. | `/api/v1/analyses/ana_123` | User private | Job |

### 36.2 Domain Response Schemas

| Schema | Key Fields | Description | Sensitivity | Source Domain |
|---|---|---|---|---|
| UserResponse | userId, email, status, roles, createdAt | Current user account summary. | User private | User |
| UserProfileResponse | profileId, displayName, careerStage, bio, updatedAt | Developer profile. | User private | UserProfile |

For API-ID-002/003, `careerStage` is nullable and one of `STUDENT`, `ENTRY_LEVEL`, `JUNIOR`, `MID_LEVEL`, or `SENIOR`; `bio` is nullable with a 1,000-character maximum. `UpdateUserProfileRequest` is a complete editable-profile payload: `displayName` is required, while `careerStage` and `bio` are required nullable properties so null explicitly clears them. For API-ID-005/006/007, `UserPreferenceResponse` contains nullable `careerId` and `companyId` stable slugs plus `updatedAt`. `SetCareerTargetRequest` and `SetCompanyTargetRequest` contain required `careerId` and `companyId` respectively. Unsupported and extension-candidate slugs return `VALIDATION_ERROR`.
| RepositorySummaryResponse | repositoryId, fullName, visibility, syncStatus, lastSyncedAt | Repository list item. | Repository private if private | Repository |
| RepositoryDetailResponse | repositoryId, fullName, defaultBranch, visibility, currentSnapshotId, lifecycle | Repository detail. | Repository private if private | Repository |
| RepositorySnapshotResponse | snapshotId, repositoryId, capturedAt, sourceRevision, status, isImmutable | Snapshot metadata. | Repository private if private | RepositorySnapshot |
| TechnologySummaryResponse | repositoryId, snapshotId, extractorVersion, taxonomyVersion, primaryLanguage, technologies | Current-snapshot deterministic technology evidence without an official score. | Repository private if private | RepositorySnapshot/Technology |
| RepositoryEvidenceSummaryResponse | repositoryId, snapshotId, extractorVersion, categories, activityTimeline | Current-snapshot engineering signals and a bounded measured activity timeline without official scores or staleness classification. | Repository private if private | RepositorySnapshot/Evidence |

For the implemented API-REP-009 subset, each detected technology contains a category of `LANGUAGE`,
`FRAMEWORK`, or `DATABASE`, a provider/dependency evidence label, nullable language byte/percentage
statistics, taxonomy support state, evidence type, and repository-relative manifest evidence paths.
Dependency declarations prove detection only; they do not produce an official score or prove meaningful usage.

For the implemented API-REP-011 subset, evidence is grouped into `ARCHITECTURE`, `DATABASE`, `TESTING`,
`DEVOPS`, `DOCUMENTATION`, `COLLABORATION`, and `ACTIVITY`. Database evidence reports normalized detected database technologies,
data-access dependencies, migration assets, and explicit persistence-configuration paths. Each signal reports presence, count, an optional observed
value, and at most 20 owner-authorized repository-relative evidence paths. Absence is explicit and no
score, confidence, readiness, or recommendation priority is calculated by this endpoint.

The response also includes the FR-043 `activityTimeline`, derived only from the current immutable snapshot's commit,
pull-request, and issue timestamps. It reports the snapshot capture time, latest observed activity, whole elapsed days,
the complete measured event count, a truncation flag, and at most 100 newest-first normalized events. Events expose only
type, provider source reference, and occurrence time. Empty snapshots return an empty timeline with nullable latest-time
and elapsed-day fields. No FR-044 staleness label or policy threshold is inferred until that policy is approved.
Successful reads remain owner-scoped and create a durable `REPOSITORY_EVIDENCE_VIEWED` audit record.

The API-REP-011 evidence view uses `engineering-evidence-extractor-v3`; its timeline uses
`repository-activity-timeline-v1`. The active `baseline-v2` Rule Engine remains deliberately bound to
`engineering-evidence-extractor-v2`, so this additive read model cannot change an official score.
The immutable superseded `baseline-v1` remains bound to its original `engineering-evidence-extractor-v1` facts, so
historical results remain reproducible.

The implemented API-ANA-004/005/006 read subset exposes only completed, immutable, owner-scoped Rule Engine results.
`RuleEvaluationResponse` includes the exact snapshot, rule-set version, formula-library version, extractor version,
official overall/category/rule scores, confidence, evidence summary, warnings, and completion time.
`ScoreBreakdownResponse` exposes deterministic raw values, weights, formula IDs, and bounded calculation traces.
`EvidenceListResponse` exposes normalized evidence IDs and snapshot references linked to contributing rules; it does
not expose raw repository content. A missing evaluation and an evaluation owned by another user both return `404`.
API-ANA-001/002/003 now provide the owner-scoped PostgreSQL durable analysis-job subset for repository-baseline
analysis. The worker invokes the versioned deterministic Rule Engine, persists immutable evaluation and Skill Matrix
references, and returns only safe job state and result resource references. A request with the same owner, immutable
snapshot, analysis scope, and active rule-set basis reuses its completed analysis job instead of creating another
official result. Career-specific analysis remains excluded.
API-ANA-007 and API-REP-012 expose cursor-paginated, newest-first completed analysis history. Each item contains
owner-authorized immutable result references plus the official persisted score, confidence, rule-set version, Skill
Matrix policy version, repository display name, and `currentForRepository`. The newest completed result per repository
is derived as current; older results remain immutable history. The history read model never recalculates official results.
Successful history and result-detail retrievals create durable `AUDIT_RESTRICTED` audit records; operational logs are
not used as an audit substitute.
API-ANA-008 accepts exactly two distinct `analysisId` query parameters and returns the corresponding owner-scoped
immutable history items in request order. The comparison UI retrieves each referenced immutable RuleEvaluation and
places official overall/category values, confidence, versions, and evidence counts side by side. Neither server nor
browser calculates or persists a delta, trend, improvement score, or replacement official result. Successful comparison
reads create a durable `ANALYSES_COMPARED` audit record; a missing or cross-owner input returns `404`.

The implemented API-SKL-001/002/003/004/005 subset returns the current or a historical owner-scoped immutable Skill Matrix and
compares exactly two matrices using their stored values only. Successful comparisons create a durable
`SKILL_MATRICES_COMPARED` audit record; a missing or cross-owner input returns `404`. Current skill detail includes its
stored assessment plus Matrix/evaluation/policy reproduction metadata. Current skill evidence exposes only normalized,
owner-scoped evidence linked through `skill_evidence_links`; successful reads emit `SKILL_DETAIL_VIEWED` or
`SKILL_EVIDENCE_VIEWED` durable audit records.
Each assessment includes its stable skill identity, source category, authoritative score, policy-derived level,
confidence, strength/weakness flags, explicit growth state, aggregate rule-result reference, Evidence IDs,
Repository IDs, structured downstream facts, and exact rule-set version. The browser displays these values without
recalculation. A missing matrix and a matrix owned by another user both return `404`.
| CreateAnalysisRequest | repositoryId, snapshotId, analysisScope, targetCareerId | Request analysis from supported references. | User private | Analysis |
| AnalysisJobResponse | jobId, status, progress, resultResourceUrl | Analysis job status. | User private | AnalysisJob |
| AnalysisResultResponse | analysisId, snapshotId, evaluationId, skillMatrixId, completedAt | Completed analysis. | User private | Analysis |
| RuleEvaluationResponse | evaluationId, ruleSetVersionId, categoryScores, overallScore, evidenceSummary | Deterministic evaluation. | User private | Evaluation |
| SkillMatrixResponse | skillMatrixId, evaluationId, skills, strengths, weaknesses, generatedAt | Skill Matrix. | User private | SkillMatrix |
| CareerReadinessResponse | careerReadinessId, skillMatrixId, careerId, careerProfileVersionId, careerProfileVersion, readinessPolicyVersion, ruleSetVersion, status, readinessScore, readinessLevel, confidence, unavailableCategories, skillGaps, assessedAt | Immutable versioned career readiness. Score and level are absent for insufficient evidence. | User private | CareerReadiness |
| SkillGapResponse | skillGapId, skillId, skillKey, category, actualScore, actualLevel, expectedMinimum, gapState, careerWeight, evidenceIds | Deterministic category comparison ordered by severity, weight, and category. It does not contain recommendation priority. | User private | SkillGap |
| CompanyReadinessResponse | companyReadinessId, companyId, companyProfileVersionId, readinessLevel, focusAreas | Company readiness. | User private | CompanyReadiness |
| RecommendationResponse | recommendationId, type, priority, reason, status, evidenceRefs | Recommendation detail. | User private | Recommendation |
| LearningRoadmapResponse | roadmapId, status, steps, milestones, progress | Roadmap detail. | User private | LearningRoadmap |
| KnowledgeSearchRequest | query, filters, limit, contextPurpose | Semantic search request. | User private | Knowledge |
| KnowledgeSearchResponse | results, sourceReferences, relevance, excerpts | Retrieval result without raw vectors. | User/private source-sensitive | Knowledge |
| CreatePromptContextRequest | taskType, sourceRefs, templateSelector, tokenBudget | Prompt context creation request. | User private | Prompt |
| PromptContextResponse | promptContextId, taskType, sourceRefs, status, tokenBudget, templateVersionId | Prompt context metadata. | User private | PromptContext |
| CreateGenerationRequest | taskType, promptContextId, sourceResourceRefs, outputType | AI generation request. | User private | AITask |
| GenerationJobResponse | jobId, status, validationStatus, artifactUrl | AI generation job. | User private | AITask |
| GeneratedArtifactResponse | artifactId, type, status, provenance, validation, contentRef | Generated artifact metadata. | Generated personal content | GeneratedArtifact |
| PortfolioResponse | portfolioId, status, activeVersionId, sections, sourceRefs | Portfolio resource. | User private/public if published | Portfolio |
| ResumeResponse | resumeId, status, activeVersionId, sections, sourceRefs | Resume resource. | Generated personal content | Resume |
| InterviewQuestionSetResponse | questionSetId, careerId, companyId, questions, generatedAt | Interview question set. | User private | Interview |
| DashboardSummaryResponse | generatedAt, targets, repositories, analyses, skillOverview, readiness, recommendations, roadmap, recentJobs | Owner-scoped summary with per-section availability; read-only composition, not a source of truth. | User private | Dashboard |

### 36.3 Schema Count by Domain

| Domain | Schema Count |
|---|---:|
| Common and Error | 3 |
| Identity | 5 |
| Integration | 6 |
| Repository | 8 |
| Analysis and Rule | 7 |
| Skill | 5 |
| Career and Company | 8 |
| Recommendation and Learning | 8 |
| Knowledge | 7 |
| Prompt | 6 |
| AI and Artifact | 8 |
| Portfolio and Resume | 8 |
| Interview | 4 |
| Dashboard | 6 |
| Administration | 8 |
| Total conceptual schemas | 92 |

## 37. Example API Flows

### 37.1 Flow A: Repository Analysis

| Step | Endpoint Reference | Expected Result |
|---|---|---|
| 1 | API-INT-001 | Client receives GitHub OAuth authorization URL. |
| 2 | API-INT-002 | GitHub connection is completed. |
| 3 | API-INT-004 | Client lists accessible repositories. |
| 4 | API-INT-005 or API-REP-003 | Repository is imported into DevPath. |
| 5 | API-REP-006 | Repository synchronization job is accepted. |
| 6 | API-INT-007 | Client polls sync job until succeeded. |
| 7 | API-ANA-001 | Analysis job is created from repository/snapshot reference. |
| 8 | API-ANA-002 | Client polls analysis job until succeeded. |
| 9 | API-ANA-003 | Client retrieves completed analysis. |
| 10 | API-SKL-001 | Client retrieves current Skill Matrix. |
| 11 | API-CAR-004 | Client retrieves career readiness. |
| 12 | API-REC-002 | Client retrieves current recommendations. |

### 37.2 Flow B: Knowledge-Based Portfolio Generation

| Step | Endpoint Reference | Expected Result |
|---|---|---|
| 1 | API-KNW-001 or API-KNW-003 | Knowledge document is created or imported from Notion. |
| 2 | API-KNW-008 | Client polls ingestion job until indexed. |
| 3 | API-KNW-009 | Client searches knowledge to confirm retrievable context. |
| 4 | API-PRM-003 | PromptContext is created from structured sources and retrieval results. |
| 5 | API-PRT-001 | Portfolio resource is created. |
| 6 | API-PRT-002 | Portfolio generation job is accepted. |
| 7 | API-AI-002 | Client polls generation job. |
| 8 | API-PRT-003 | Client retrieves generated portfolio. |
| 9 | API-PRT-008 | Client requests portfolio export. |

### 37.3 Flow C: Company Readiness Evaluation

| Step | Endpoint Reference | Expected Result |
|---|---|---|
| 1 | API-CAR-003 | User sets target career. |
| 2 | API-CMP-003 | User sets target company. |
| 3 | API-SKL-001 | Client retrieves latest Skill Matrix. |
| 4 | API-CMP-004 | Client retrieves company readiness. |
| 5 | API-CAR-006 | Client retrieves related skill gaps. |
| 6 | API-REC-001 | Recommendation generation request is created if needed. |
| 7 | API-LRN-001 | Learning roadmap is created from recommendations. |

## 38. Requirement Traceability

### 38.1 Functional Requirement Mapping

| Requirement Range | API Operations | Status |
|---|---|---|
| FR-001~FR-020 | API-ID-001~API-ID-010, API-INT-001~API-INT-003 | Covered |
| FR-021~FR-050 | API-INT-001~API-INT-007, API-REP-001~API-REP-012 | Covered |
| FR-051~FR-070 | API-INT-008~API-INT-012, API-KNW-001~API-KNW-011 | Covered |
| FR-071~FR-100 | API-REP-006~API-REP-012, API-ANA-001~API-ANA-008 | Covered |
| FR-101~FR-180 | API-ANA-003~API-ANA-008, API-SKL-001~API-SKL-007 | Covered |
| FR-181~FR-220 | API-CAR-001~API-CAR-006, API-CMP-001~API-CMP-005, API-REC-001~API-LRN-010 | Covered |
| FR-221~FR-280 | API-PRM-003~API-AI-007, API-PRT-001~API-ITV-007 | Covered |
| FR-281~FR-320 | API-DSH-001~API-DSH-010 | Covered |
| FR-321~FR-340 | API-KNW-004~API-KNW-011 | Covered |
| FR-341~FR-360 | API-ADM-001~API-ADM-011 | Covered |
| FR-361 | API-REP-001, API-REP-002, API-REP-004, API-REP-005 | Covered |

### 38.2 Architecture Requirement Mapping

| Requirement Group | API Mapping | Domain/Data/Storage Mapping |
|---|---|---|
| RR-001~RR-010 | Analysis, RuleEvaluation, SkillMatrix, Evidence APIs | Rule Context, Evaluation, SkillMatrix, PostgreSQL authoritative storage |
| CR-001~CR-020 | Career, Company, Recommendation, Learning APIs | Career/Company/Recommendation contexts, readiness and roadmap objects |
| AI-001~AI-015 | Prompt, Generation, GeneratedArtifact, Portfolio, Resume, Interview APIs | AI Context, PromptContext, GeneratedArtifact, object storage content refs |
| PR-001~PR-015 | Prompt template, prompt context, validation APIs | Prompt Context, PromptTemplateVersion, PromptExecution |
| KR-001~KR-020 | Knowledge document, ingestion, search, chunk, reindex APIs | Knowledge Context, KnowledgeDocument, Vector Database retrieval index |

### 38.3 Internal-only Requirements

| Requirement Area | Reason |
|---|---|
| Direct rule execution internals | Exposed only through analysis/evaluation results, not arbitrary rule execution APIs. |
| Prompt hidden system instruction content | Admin metadata may be visible, but sensitive hidden prompts are not exposed to normal users. |
| Raw embedding vectors | Internal retrieval implementation only. |
| Provider token refresh internals | Exposed as connection status, not token details. |

### 38.4 Requirement Traceability Summary

Every major functional requirement range maps to at least one API operation. Deterministic engine responsibilities are exposed as resources and results, not as client-controlled calculations.

## 39. Non-Functional API Requirements

| Area | Requirement | Target Classification | Unresolved Values |
|---|---|---|---|
| Availability | User-facing API should be highly available according to MVP operational target. | High | Exact percentage TBD by Operations. |
| Latency | Read APIs should be optimized for interactive dashboard use. | Low to moderate | Exact p95 values TBD by Performance Plan. |
| Throughput | Sync, analysis, and AI generation use asynchronous jobs. | Scalable | Exact throughput TBD by load testing. |
| Payload size | JSON payloads must remain bounded. | Controlled | Exact default maximum TBD by Security Architecture. |
| Upload size | File uploads must enforce category-specific maximums. | Controlled | Exact maximums TBD by Storage and Security. |
| Timeout | Long-running operations must not block HTTP until completion. | Async by default | Exact request timeout TBD. |
| Retry | Retryable failures must indicate retryability and safe retry timing where possible. | Standardized | Provider-specific retry policy TBD. |
| Concurrency | Mutable resources should support ETag or resource version. | Optimistic | Exact header policy TBD. |
| Consistency | Authoritative resources require strong consistency; projections may be eventual. | Explicit | Projection freshness indicators TBD. |
| Compatibility | Additive API changes must remain backward compatible. | Required | Sunset window TBD. |
| Security | HTTPS, validation, authorization, redaction, rate limit, and replay controls required. | Required | Detailed threat model TBD. |
| Auditability | Sensitive commands and admin operations must create audit records. | Required | Retention duration TBD. |
| Observability | Responses include request metadata and jobs include status metadata. | Required | Trace propagation standard TBD. |

## 40. Open Issues and Decisions

| Issue ID | Description | Impact | Options | Recommended Direction | Status | ADR Candidate |
|---|---|---|---|---|---|---|
| API-OPEN-001 | Public API availability for external developers is not decided. | Affects auth, quotas, docs, support. | Private only, limited public, full public. | Start private/authenticated only. | Open | ADR-API-001 |
| API-OPEN-002 | GraphQL adoption for dashboard read models is undecided. | Affects frontend data fetching. | REST only, GraphQL read API, hybrid. | REST first; GraphQL future extension. | Open | ADR-API-002 |
| API-OPEN-003 | Webhook scope beyond OAuth callbacks is undecided. | Affects provider freshness. | Polling only, provider webhooks, hybrid. | Polling first; provider webhooks later. | Open | ADR-API-003 |
| API-OPEN-004 | Organization APIs are future scope. | Affects ownership and authorization. | User-only, organization beta, full enterprise. | Preserve user-only v1. | Open | ADR-API-004 |
| API-OPEN-005 | API gateway selection is not defined. | Affects deployment and rate limits. | App-level, managed gateway, reverse proxy. | Decide in backend/deployment architecture. | Open | ADR-API-005 |
| API-OPEN-006 | Real-time notification strategy is not defined. | Affects jobs and dashboard freshness. | Polling, SSE, WebSocket, push. | Polling v1; real-time later. | Open | ADR-API-006 |
| API-OPEN-007 | Streaming AI responses are not committed. | Affects AI UX and response validation. | Non-streaming, validated streaming, staged streaming. | Non-streaming v1 for validation safety. | Open | ADR-API-007 |
| API-OPEN-008 | Client-generated IDs are not decided. | Affects idempotency and offline support. | Server IDs only, client IDs for drafts, hybrid. | Server IDs plus Idempotency-Key. | Open | ADR-API-008 |
| API-OPEN-009 | Export retention duration is not finalized. | Affects file APIs and object storage. | Short-lived only, user-managed, policy-based. | Policy-based with expiry metadata. | Open | ADR-API-009 |

## 41. Future Extensions

| Future Extension | Compatibility Approach | Current Scope Status |
|---|---|---|
| GitLab | Add integration endpoints and map to existing Repository resources. | Future |
| Bitbucket | Add provider namespace and repository import contract. | Future |
| Jira | Add knowledge import and future work-item evidence APIs. | Future |
| Slack | Add permission-scoped knowledge import APIs. | Future |
| Figma | Add design document import and portfolio source references. | Future |
| Baekjoon | Add coding-practice source integration only after Rule requirements exist. | Future |
| Programmers | Same as coding-practice integration. | Future |
| LeetCode | Same as coding-practice integration. | Future |
| Team workspaces | Add organization-scoped resources and membership authorization. | Future |
| Mentors | Add mentor review resources without changing Rule scores. | Future |
| Recruiters | Add controlled portfolio sharing and public profile APIs. | Future |
| Enterprise organizations | Add tenant ownership, admin roles, and organization audit. | Future |
| Public developer API | Add OAuth scopes, public documentation, and stricter quotas. | Future |
| Mobile clients | Preserve REST/JSON compatibility and client version headers. | Future |
| Real-time notifications | Add SSE/WebSocket or webhook delivery without changing job model. | Future |
| Streaming generation | Add streaming status/content events after validation strategy is approved. | Future |
| GraphQL read API | Add as read-only projection API; command APIs remain REST. | Future |

## 42. Final Consistency Review

### 42.1 Checklist

| Review Item | Status |
|---|---|
| Every endpoint belongs to a bounded context. | Passed |
| No API leaks database structure. | Passed |
| No API performs Rule Engine logic. | Passed |
| No API performs Career Engine logic. | Passed |
| AI endpoints reference validated source data. | Passed |
| PromptContext is immutable. | Passed |
| RepositorySnapshot is immutable. | Passed |
| Historical analysis resources are immutable. | Passed |
| Generated outputs reference source context. | Passed |
| Errors use the common error model. | Passed |
| Long-running operations use the common job model. | Passed |
| Authorization is documented. | Passed |
| Pagination is documented. | Passed |
| Idempotency is documented. | Passed |
| Traceability is complete at requirement-group level. | Passed |
| Terminology matches `07_Domain_Model.md`. | Passed |
| Data ownership matches `08_System_Data_Model.md`. | Passed |
| Persistence assumptions match `09_Database_Design.md`. | Passed |
| Authentication/session contract matches ADR-026 and `13_Security_Architecture.md`. | Passed |

### 42.2 Final Document Completeness Checklist

| Deliverable Requirement | Status |
|---|---|
| Resource definitions expanded. | Complete |
| Endpoint contracts expanded by domain. | Complete |
| Schema catalog included. | Complete |
| Error model included with JSON example. | Complete |
| Asynchronous job model included. | Complete |
| API flows included. | Complete |
| Traceability included. | Complete |
| Non-functional API requirements included. | Complete |
| Open decisions included. | Complete |
| Endpoint count by domain included. | Complete |
| Schema count by domain included. | Complete |
| Unresolved issue count included. | 9 |

## 43. Identity Foundation Implementation Evidence

| Method | Path | Canonical ID | Implementation Status |
|---|---|---|---|
| GET | `/internal/health` | Internal | Implemented |
| GET | `/oauth2/authorization/github` | Framework-owned | Implemented by Spring Security |
| GET | `/login/oauth2/code/github` | Framework-owned | Implemented by Spring Security |
| GET | `/api/v1/csrf` | Session foundation | Implemented |
| GET | `/api/v1/users/me` | `API-ID-001` | Implemented |
| POST | `/api/v1/session/logout` | Session foundation | Implemented by Spring Security |

The machine-readable subset is `contracts/openapi/devpath-openapi.yaml`. It documents opaque cookie authentication, the `X-CSRF-TOKEN` header, safe current-user projection, and authentication errors. All other catalog operations remain specification-only.
