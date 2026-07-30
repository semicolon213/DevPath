# DevPath Backend Architecture

## 1. Purpose

### 1.1 Document Purpose

This document defines the backend architecture for DevPath. It explains how the backend realizes the domain model, system data model, database design, and API contracts while preserving deterministic rule evaluation, deterministic career intelligence, AI boundaries, snapshot immutability, prompt immutability, and evidence-based recommendations.

### 1.2 Scope

This document covers backend architectural style, module boundaries, layers, application services, domain service integration, ports and adapters, persistence interaction, transaction boundaries, jobs, events, scheduling, pipelines, caching, external integrations, authorization, validation, errors, observability, package structure, scalability, deployment-unit boundaries, and traceability.

### 1.3 Intended Audience

| Audience | Usage |
|---|---|
| Backend engineers | Implement modules, use cases, ports, adapters, jobs, and event handlers |
| Architects | Validate module boundaries, dependency rules, consistency model, and extraction path |
| AI engineers | Integrate AI generation without crossing deterministic boundaries |
| Data engineers | Align persistence adapters with canonical data and database ownership |
| QA engineers | Derive architecture-level tests and contract tests |
| Security engineers | Validate backend authorization, sensitive data, and audit controls |

### 1.4 Authority

This document is the authoritative backend implementation architecture. Backend source code, module structure, application services, adapters, and tests must conform to this document.

### 1.5 Relationship to Previous Documents

| Source | Backend Relevance |
|---|---|
| `01_SRS.md` | Functional and non-functional requirement authority |
| `02_Rule_Engine.md` | Rule Engine determinism and score ownership |
| `03_Career_Path_Engine.md` | Career/company readiness and recommendation rules |
| `04_AI_Architecture.md` | AI provider invocation and validation boundaries |
| `05_Prompt_Engineering.md` | Prompt context, template, and validation boundaries |
| `06_Knowledge_Architecture.md` | Knowledge ingestion, retrieval, chunking, embedding metadata |
| `07_Domain_Model.md` | Bounded contexts, aggregates, services, events, invariants |
| `08_System_Data_Model.md` | Canonical objects, lifecycle, versioning, ownership |
| `09_Database_Design.md` | Persistence responsibilities and logical schemas |
| `10_API_Specification.md` | API resources, endpoints, schemas, jobs, errors |

### 1.6 Relationship to Implementation

This document guides implementation but does not define executable code, controllers, entity classes, repositories, SQL, migrations, annotations, framework configuration, or deployment manifests.

### 1.6.1 Current Scaffold Evidence

The initial implementation scaffold now provides only foundation-level backend evidence. It does not implement business features, authentication, persistence, provider adapters, AI execution, queues, migrations, or production infrastructure.

| Scaffold Area | Evidence Path | Status |
|---|---|---|
| Spring Boot entry point | `backend/src/main/java/com/devpath/DevPathApplication.java` | Created |
| Internal health endpoint | `backend/src/main/java/com/devpath/platform/health/InternalHealthController.java` | Created |
| Modular monolith placeholders | `backend/src/main/java/com/devpath/*/{domain,application,adapter}/package-info.java` | Created |
| Backend build configuration | `backend/build.gradle`, `backend/settings.gradle`, `backend/gradle/wrapper/` | Created |
| Backend architecture tests | `backend/src/test/java/com/devpath/architecture/` | Created |
| Backend validation status | `node scripts/run-gradle.mjs test` | Blocked locally by missing Java 21 toolchain |

### 1.7 Excluded Topics

Excluded topics include UI behavior, frontend state management, infrastructure provisioning, Kubernetes manifests, Terraform, complete security threat modeling, complete observability dashboards, and detailed test cases.

## 2. Architecture Goals

| Goal | Backend Meaning | Trade-off |
|---|---|---|
| Correctness | Preserve domain invariants and source-of-truth boundaries. | More explicit validation and orchestration. |
| Deterministic evaluation | Ensure Rule and Career Engines produce official results without LLM involvement. | AI features must consume results rather than calculate them. |
| Modularity | Separate bounded-context modules with clear public module APIs. | More disciplined dependency management. |
| Maintainability | Keep business logic in domain/application layers, not controllers or adapters. | Requires consistent architecture reviews. |
| Testability | Isolate domain logic, ports, adapters, jobs, and API contracts. | More test layers than a simple CRUD app. |
| Scalability | Use jobs and workload isolation for sync, analysis, knowledge, AI, and export workloads. | Slightly more operational complexity. |
| Security | Enforce ownership, authorization, redaction, and provider isolation. | More checks across layers. |
| Traceability | Preserve snapshot/version/source references throughout workflows. | Larger metadata model. |
| Resilience | Handle provider failures, retries, partial sync, and job recovery. | Requires idempotency discipline. |
| Provider independence | Hide GitHub, Notion, LLM, storage, and vector details behind adapters. | Adapter normalization effort. |
| Observability | Capture request, job, event, provider, and validation metadata. | Logging and metric design overhead. |
| Evolvability | Allow future extraction to services without premature microservices. | Requires stable module contracts now. |

The initial architecture favors simplicity through a modular monolith while preserving boundaries that allow later extraction when scale or organizational constraints justify it.

## 3. Architecture Style

### 3.1 Selected Style

DevPath initially uses a modular monolith with Domain-Driven Design, Hexagonal Architecture, Clean Architecture principles, Ports and Adapters, event-driven processing where appropriate, and CQRS-lite for read models.

### 3.2 Why Modular Monolith Initially

| Reason | Explanation |
|---|---|
| Product complexity is domain-heavy | Correct boundaries matter more than distributed deployment at the beginning. |
| Team velocity | One deployable codebase avoids premature distributed coordination. |
| Transactional clarity | Aggregate transactions remain simpler inside one runtime. |
| Operational simplicity | Fewer independently deployed services reduce monitoring, network, and incident complexity. |
| Extraction readiness | Strong module boundaries, ports, and events preserve future extraction options. |

### 3.3 Adopted Patterns

| Pattern | Usage |
|---|---|
| Domain-Driven Design | Modules align with bounded contexts and aggregates. |
| Hexagonal Architecture | Domain/application layers depend on ports, not infrastructure. |
| Clean Architecture | Dependencies point inward toward domain rules. |
| Ports and Adapters | Persistence, providers, cache, object storage, vector search, notification, and clock are adapters. |
| Event-driven Processing | Long-running and cross-module updates use events/jobs. |
| CQRS-lite | Commands update aggregates; queries use optimized projections where useful. |

### 3.4 Accepted Backend Stack Baseline

| Area | Accepted Baseline | Governing ADR |
|---|---|---|
| Language | Java 21 LTS | ADR-020 |
| Framework | Spring Boot modular-monolith backend | ADR-020 |
| Runtime Style | One backend codebase with API, worker, and scheduler runtime responsibilities separated by application mode or entrypoint policy | ADR-011, ADR-020 |
| Build Tool | Gradle with committed wrapper and reproducible dependency versions when scaffolding is created | ADR-023 |
| Module Boundary Enforcement | Domain-first packages with architecture-boundary tests | ADR-002, ADR-032 |
| Framework Boundary | Spring annotations and framework dependencies remain outside pure domain model and deterministic engine code | ADR-002, ADR-020 |
| Persistence | Spring Data JPA/Hibernate inside outbound adapters with separate persistence models | ADR-024 |
| Migration | Flyway immutable versioned SQL; production migration is a deployment step | ADR-025 |
| Authentication | Spring Security OAuth2 Login with GitHub and opaque server-managed session cookie | ADR-026 |

### 3.5 Deliberately Not Adopted Initially

| Not Adopted | Reason |
|---|---|
| Full microservices | Premature operational complexity and distributed transaction risk. |
| Distributed transactions | Not needed for current scale; use outbox and idempotent consumers. |
| Event sourcing for all entities | Append-only history is needed for selected concepts, not every entity. |
| Excessive CQRS | Most use cases can share persistence with projections where needed. |
| Framework-centered domain models | Domain objects must not depend on infrastructure frameworks. |

## 4. Backend System Context

| Actor/System | Role | Trust Boundary |
|---|---|---|
| Web Client | Primary user interface consuming REST API. | Untrusted client; all requests validated and authorized. |
| Future Mobile Client | Future client consuming same API contracts. | Untrusted client. |
| GitHub | External source provider for repositories and OAuth. | External provider boundary; adapter required. |
| Notion | External source provider for workspace documents. | External provider boundary; adapter required. |
| AI Providers | LLM providers for natural-language generation. | External provider boundary; prompt and response validation required. |
| PostgreSQL | Authoritative relational persistence. | Trusted infrastructure boundary with least privilege. |
| Redis | Cache/session/rate-limit/job state support. | Non-authoritative; no business truth. |
| Vector Database | Semantic retrieval index. | Derived index; no business entity ownership. |
| Object Storage | Large content and export storage. | Content storage behind secure references. |
| Email/Notification Provider | Delivers notifications where configured. | External provider boundary. |
| Administrative User | Privileged operator. | Requires privileged authorization and audit. |
| Background Workers | Execute long-running jobs. | Trusted internal runtime; authorization context must be carried. |

## 5. Backend Container Model

| Container | Purpose | Responsibilities | Owned Workloads | Inbound | Outbound | Scaling | Failure Impact | Security Boundary |
|---|---|---|---|---|---|---|---|---|
| API Application | Serve synchronous REST API. | Auth, validation, command/query dispatch, response mapping. | User requests, admin requests, job creation. | HTTPS API. | DB/cache/adapters/jobs. | Horizontally stateless. | Users cannot access API while down. | Public edge to trusted backend. |
| Background Worker | Execute async jobs. | Sync, analysis, knowledge ingestion, embedding, AI, export jobs. | Long-running operations. | Job queue or internal job store. | Providers, DB, object/vector/cache. | Horizontally by workload. | Jobs delayed or fail. | Internal worker identity. |
| Scheduler | Trigger periodic tasks. | Refresh, cleanup, reindex, retention, health checks. | Scheduled commands. | Time-based triggers. | Job enqueue, DB. | Active/passive or single leader. | Scheduled work delayed. | Internal trusted runtime. |
| Database | Persist canonical data. | Authoritative state and history. | Transactional data. | Backend adapters. | Backup/recovery. | Read replicas/partitioning. | Authoritative write unavailable. | Protected persistence boundary. |
| Cache | Store ephemeral data. | Sessions, rate limits, dashboard cache, job progress cache. | Rebuildable data. | Backend adapters. | Expiry events where supported. | Clusterable. | Performance degradation or session impact. | No secrets/business truth. |
| Vector Store | Retrieve knowledge semantically. | Similarity search and retrieval index. | Embedding search. | Knowledge adapter. | None external. | Index scaling. | Retrieval degraded. | User-isolated metadata filters. |
| Object Storage | Store large content. | Documents, archives, generated files, exports. | Blob-like content. | Storage adapter. | Temporary access URLs. | Storage expansion. | Content unavailable. | Secure reference boundary. |
| External Provider Adapters | Isolate external APIs. | Normalize GitHub, Notion, AI, notification, storage provider responses. | Provider requests. | Ports. | External services. | Per provider limits. | Provider-specific features degraded. | Secrets isolated. |

## 6. Layered Architecture

### 6.1 Interface Layer

| Responsibility | Rule |
|---|---|
| HTTP handling | Accept requests and route to application services. |
| Request parsing | Parse headers, path/query params, and JSON bodies. |
| Authentication context extraction | Build authenticated actor context. |
| Schema validation | Validate transport shape and basic field constraints. |
| Response mapping | Map application outputs to API response schemas. |
| Status code selection | Apply `10_API_Specification.md` status rules. |
| Prohibited | No business rules, score calculations, provider logic, or persistence decisions. |

### 6.2 Application Layer

| Responsibility | Rule |
|---|---|
| Use case orchestration | Coordinate one business use case. |
| Transaction coordination | Define transaction boundary and after-commit actions. |
| Authorization checks | Enforce ownership and role policy beyond interface checks. |
| Domain object loading | Load aggregates through ports. |
| Domain service invocation | Call domain services with prepared inputs. |
| Event publication | Record and publish domain/integration events reliably. |
| Output mapping | Produce application DTO-like outputs without leaking persistence models. |

### 6.3 Domain Layer

| Responsibility | Rule |
|---|---|
| Entities and value objects | Represent business concepts and validation. |
| Aggregates | Protect consistency boundaries. |
| Domain services | Implement business operations not owned by a single aggregate. |
| Policies and specifications | Encapsulate reusable decisions. |
| Domain events | Represent business-significant facts. |
| Prohibited | No framework dependency, database calls, HTTP calls, cache calls, or provider calls. |

### 6.4 Infrastructure Layer

| Responsibility | Rule |
|---|---|
| Persistence adapters | Implement repository ports against storage. |
| Provider adapters | Implement GitHub, Notion, AI, notification, and storage ports. |
| Job adapters | Enqueue and execute background work. |
| Cache adapters | Implement cache ports. |
| Observability adapters | Emit logs, metrics, traces, and audits. |

### 6.5 Dependency Direction

Dependencies point inward:

Interface → Application → Domain

Infrastructure implements ports defined by Application or Domain-facing abstractions. Domain must not depend on Interface or Infrastructure.

## 7. Module Architecture

### 7.1 Module Catalog

| Module | Purpose | Owned Concepts | Owned Aggregates | Application Services | Inbound Ports | Outbound Ports | Published Events | Consumed Events | Database Ownership | Cache Usage | External Dependencies | Prohibited Responsibilities | Extraction Potential |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Identity | User identity and ownership. | User, external accounts, consent. | User | GetCurrentUser, UpdateUserProfile, SetCareerTarget, SetCompanyTarget, DisconnectExternalAccount | HTTP, admin command | User repository, audit, clock, ID generator | UserRegistered, CareerChanged, CompanyChanged | None | Identity schema | Session metadata | OAuth adapters indirectly | Scoring, sync, AI | Medium |
| Integration | OAuth and provider connection workflows. | GitHubConnection, NotionConnection, sync permissions. | Integration connection | InitiateOAuth, HandleCallback, DisconnectProvider | HTTP callback, webhook | GitHub/Notion adapter, cache, audit | GitHubConnected, NotionConnected, PermissionChanged | UserDeleted | Identity/integration records | OAuth state | GitHub, Notion | Repository analysis | Medium |
| Repository | Repository metadata and snapshots. | Repository, Snapshot, commits, branches, PRs. | Repository, RepositorySnapshot | RegisterRepository, SynchronizeRepository, CreateRepositorySnapshot, ArchiveRepository, RestoreRepository | HTTP, job handler | Repository metadata, snapshot storage, GitHub adapter, job queue | RepositoryRegistered, RepositorySynchronized, RepositorySnapshotCreated | GitHubConnected | Repository schema | Repository summary cache | GitHub | Rule scoring | High |
| Analysis | Analysis orchestration and feature extraction. | Analysis job/result prep. | Analysis process | RequestRepositoryAnalysis, ExecuteRepositoryAnalysis, RetrieveAnalysisResult, CompareAnalyses | HTTP, job handler | Analysis result, rule module port, snapshot port | AnalysisRequested, AnalysisCompleted | RepositorySnapshotCreated | Analysis records | Job progress cache | None | LLM scoring | High |
| Rule | Deterministic scoring and Skill Matrix. | RuleSet, Evaluation, Evidence, SkillMatrix. | RuleSet, Evaluation, SkillMatrix | GenerateSkillMatrix, ExecuteRuleEvaluation | Internal application port | Rule config, evaluation result, audit | SkillMatrixGenerated, EvaluationCompleted | AnalysisCompleted | Analysis/rule schema | Reference-data cache | None | AI explanation | High |
| Career | Career readiness. | Career, CareerProfile, CareerReadiness, SkillGap. | Career, CareerAssessment | EvaluateCareerReadiness, GenerateSkillGap | HTTP/internal event | Career profile port, skill matrix port | CareerReadinessEvaluated | SkillMatrixGenerated, CareerChanged | Career schema | Career profile cache | None | Score calculation | Medium |
| Company | Company readiness. | Company, CompanyProfile, CompanyReadiness. | Company, CompanyAssessment | EvaluateCompanyReadiness | HTTP/internal event | Company profile port, skill matrix port | CompanyReadinessEvaluated | SkillMatrixGenerated, CompanyChanged | Career/company schema | Company profile cache | None | Mutating Rule scores | Medium |
| Recommendation | Evidence-based recommendations. | RecommendationSet, Recommendation. | Recommendation | GenerateRecommendations, AcceptRecommendation, DismissRecommendation, CompleteRecommendation | HTTP/event handler | Recommendation store, audit | RecommendationGenerated | CareerReadinessEvaluated, CompanyReadinessEvaluated | Recommendation schema | Active recommendations cache | None | AI narrative generation | Medium |
| Learning | Roadmaps and progress. | LearningRoadmap, RoadmapStep. | LearningRoadmap | CreateLearningRoadmap, UpdateRoadmapProgress | HTTP/event handler | Roadmap store, notification | LearningRoadmapCreated, RoadmapStepCompleted | RecommendationGenerated | Recommendation/learning schema | Roadmap cache | None | Recommendation priority | Low |
| Knowledge | Long-term memory and retrieval. | KnowledgeDocument, chunks, embeddings. | KnowledgeDocument | ImportKnowledgeDocument, NormalizeKnowledgeDocument, ChunkKnowledgeDocument, IndexKnowledgeDocument, SearchKnowledge | HTTP, job handler | Knowledge store, vector search, object storage | KnowledgeDocumentImported, KnowledgeIndexed | RepositorySynchronized, NotionConnected | Knowledge schema | Retrieval cache | Notion, vector store, object storage | Final AI response generation | High |
| Prompt | Prompt templates and contexts. | PromptTemplate, PromptContext. | PromptTemplate, PromptExecution | CreatePromptContext, ValidatePromptContext, ComposePrompt | HTTP/internal | Prompt template, knowledge retrieval, cache | PromptContextCreated | KnowledgeIndexed | Prompt schema | Temporary prompt candidate cache | None | Business rules or scoring | Medium |
| AI | AI invocation and response validation. | AITask, ModelExecution, AIResponse. | AITask, GeneratedArtifact | RequestGeneration, InvokeAIProvider, ValidateAIResponse, PersistGeneratedArtifact | HTTP, job handler | AI provider, object storage, artifact store | GenerationRequested, GenerationCompleted, GenerationFailed | PromptContextCreated | Artifact/AI schema | Generation status cache | LLM providers | Authoritative scores | High |
| Artifact | Generated artifact envelope and exports. | GeneratedArtifact, exports. | GeneratedArtifact | PersistGeneratedArtifact, ExportArtifact | HTTP/job handler | Object storage, audit | ArtifactGenerated, ArtifactExported | GenerationCompleted | Artifact schema | Export job cache | Object storage | Portfolio publication policy | Medium |
| Portfolio | Portfolio lifecycle. | Portfolio, PortfolioVersion. | Portfolio | GeneratePortfolio, ReviewPortfolio, PublishPortfolio | HTTP/job handler | Artifact, object storage, audit | PortfolioGenerated, ArtifactPublished | ArtifactGenerated | Artifact schema | Portfolio cache | Object storage | AI provider calls | Medium |
| Resume | Resume lifecycle. | Resume, ResumeVersion. | Resume | GenerateResume, ReviewResume, ExportResume | HTTP/job handler | Artifact, object storage, audit | ResumeGenerated, ArtifactPublished | ArtifactGenerated | Artifact schema | Resume cache | Object storage | Score calculation | Medium |
| Interview | Interview question sets. | InterviewQuestionSet, questions. | InterviewQuestionSet | GenerateInterviewQuestions, SubmitPracticeAnswer, RequestAnswerFeedback | HTTP/job handler | AI generation, artifact store | InterviewQuestionsGenerated | GenerationCompleted | Artifact schema | Question set cache | AI module | Deterministic grading | Low |
| Dashboard | Read model APIs. | DashboardSummary and projections. | None | RetrieveDashboardSummary, RetrieveLatestAnalysis, RetrieveSkillOverview | HTTP/query handler | Projection stores, cache | None | Most domain events | Projection records | Dashboard cache | None | Authoritative writes | Low |
| Administration | Configuration governance. | Rule/career/company/prompt configuration views. | Configuration | ActivateRuleVersion, ActivateCareerProfile, ActivateCompanyProfile, ActivatePromptTemplate | Admin HTTP | Config stores, audit | ConfigurationChanged | None | Admin/config schema | Reference-data cache invalidation | None | Reading private source content | Medium |
| Audit | Audit records. | AuditRecord, deletion records. | AuditRecord | RecordAuditEvent, RetrieveAuditRecords | Event handler/admin HTTP | Audit store | AuditRecorded | Security/domain events | Audit schema | None | None | Business decisions | Low |
| Notification | User notifications. | Notification. | Notification | CreateNotification, MarkNotificationRead | Event handler/HTTP | Notification provider, notification store | NotificationCreated | Job/domain events | Notification records | Notification feed cache | Email/notification provider | Business rule decisions | Medium |
| Shared Kernel | Minimal common primitives. | IDs, time, pagination, result metadata. | None | None | None | Clock, ID generator | None | None | None | None | None | Business-specific concepts | Not extracted |

## 8. Module Dependency Rules

### 8.1 Allowed Dependency Flow

Repository → Analysis → Rule → Career/Company → Recommendation → Learning → Prompt → AI → Artifact/Portfolio/Resume/Interview.

Knowledge may be consumed by Prompt. Identity is referenced for authorization by all modules. Audit and Notification consume events.

### 8.2 Dependency Matrix

| From \ To | Identity | Repository | Analysis | Rule | Career | Company | Recommendation | Learning | Knowledge | Prompt | AI | Artifact | Admin | Audit |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Identity | - | Event | No | No | Command | Command | No | No | No | No | No | No | No | Event |
| Repository | Auth | - | Event | No | No | No | No | No | Event | No | No | No | No | Event |
| Analysis | Auth | Read | - | Port | No | No | No | No | No | No | No | No | No | Event |
| Rule | Auth | Ref | Event | - | Event | Event | No | No | No | No | No | No | Admin | Event |
| Career | Auth | No | No | Read | - | Event | Event | No | No | PromptRef | No | No | Admin | Event |
| Company | Auth | No | No | Read | Read | - | Event | No | No | PromptRef | No | No | Admin | Event |
| Recommendation | Auth | No | No | Ref | Ref | Ref | - | Event | No | Ref | No | No | No | Event |
| Learning | Auth | No | No | No | Ref | Ref | Ref | - | No | Ref | No | No | No | Event |
| Knowledge | Auth | Ref | No | Ref | Ref | Ref | Ref | No | - | Service | No | No | No | Event |
| Prompt | Auth | Ref | Ref | Ref | Ref | Ref | Ref | Ref | Query | - | Event | No | Admin | Event |
| AI | Auth | Ref | Ref | Ref | Ref | Ref | Ref | Ref | Ref | Read | - | Event | No | Event |
| Artifact | Auth | Ref | No | Ref | Ref | Ref | No | No | No | Ref | Ref | - | No | Event |
| Admin | Auth | No | No | Config | Config | Config | No | No | No | Config | No | No | - | Event |

Legend: `Read` means query through an approved port, `Event` means event-based dependency, `Ref` means stable identifier/reference only, `Config` means controlled administrative configuration, `No` means forbidden direct dependency.

### 8.3 Forbidden Dependencies

| Forbidden Dependency | Reason |
|---|---|
| Domain layer to infrastructure adapters | Violates clean architecture and testability. |
| Interface layer business rule execution | Controllers must not contain business logic. |
| AI module to mutate Rule Evaluation | LLM cannot calculate or modify official scores. |
| Prompt module to execute career or rule logic | Prompt Builder only assembles prompts. |
| Knowledge module to generate final AI responses | Knowledge retrieves only. |
| Dashboard module to write authoritative state | Dashboard is read-model only. |
| Shared Kernel containing business entities | Prevents shared mutable ownership. |

## 9. Application Service Catalog

| Use Case ID | Application Service | Purpose | Caller | Input Contract | Output Contract | Authorization | Aggregates | Domain Services | Repository Ports | External Ports | Transaction Boundary | Events | Failure Scenarios | Retry | Idempotency |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| UC-ID-001 | GetCurrentUser | Retrieve current user. | API | User identity | UserResponse | Authenticated self | User | None | UserRepositoryPort | None | Read-only | None | Not authenticated | No | Safe |
| UC-ID-002 | UpdateUserProfile | Update profile fields. | API | Update profile request | UserProfileResponse | Self | User/Profile | Profile policy | UserRepositoryPort | AuditPort | Single user transaction | UserProfileUpdated | Validation conflict | No | ETag |
| UC-ID-003 | SetCareerTarget | Set target career. | API | CareerId | Preference response | Self | User/Profile | Career selection policy | UserRepositoryPort, CareerProfilePort | AuditPort | Profile transaction | CareerChanged | Unsupported career | No | By careerId |
| UC-ID-004 | SetCompanyTarget | Set target company. | API | CompanyId | Preference response | Self | User/Profile | Company selection policy | UserRepositoryPort, CompanyProfilePort | AuditPort | Profile transaction | CompanyChanged | Unsupported company | No | By companyId |
| UC-ID-005 | DisconnectExternalAccount | Disconnect provider. | API | Provider/connection ID | Connection response | Self | User/Integration | Permission policy | IntegrationPort | ProviderAdapter, AuditPort | Connection transaction after provider action | IntegrationDisconnected | Provider unavailable | Conditional | Required |
| UC-REP-001 | RegisterRepository | Import repository metadata. | API | Repository source reference | RepositoryResponse | Owner/provider permission | Repository | Repository eligibility | RepositoryMetadataPort | GitHubAdapter, AuditPort | Repository transaction | RepositoryRegistered | Duplicate/permission denied | Yes | Required |
| UC-REP-002 | SynchronizeRepository | Enqueue repository sync. | API | RepositoryId, scope | JobStatusResponse | Owner/provider permission | RepositorySyncJob | Sync policy | RepositoryMetadataPort, JobPort | JobQueuePort | Job creation transaction | RepositorySynchronizationRequested | Rate limit, duplicate | Yes | Required |
| UC-REP-003 | CreateRepositorySnapshot | Persist immutable snapshot. | Worker | Collected source facts | RepositorySnapshot | Internal worker | RepositorySnapshot | Repository Analysis Service | SnapshotStoragePort | ObjectStoragePort | Snapshot transaction | RepositorySnapshotCreated | Invalid source facts | Yes | Snapshot revision |
| UC-REP-004 | ArchiveRepository | Archive repository. | API | RepositoryId | RepositoryResponse | Owner | Repository | Archive policy | RepositoryMetadataPort | AuditPort | Repository transaction | RepositoryArchived | Active job conflict | No | Required |
| UC-REP-005 | RestoreRepository | Restore repository. | API | RepositoryId | RepositoryResponse | Owner | Repository | Restore policy | RepositoryMetadataPort | AuditPort | Repository transaction | RepositoryRestored | Permission revoked | No | Required |
| UC-ANA-001 | RequestRepositoryAnalysis | Enqueue analysis. | API/event | SnapshotId, options | JobStatusResponse | Owner | AnalysisJob | Repository Analysis Service | AnalysisResultPort, SnapshotPort | JobQueuePort | Job transaction | AnalysisRequested | Snapshot unavailable | Yes | Required |
| UC-ANA-002 | ExecuteRepositoryAnalysis | Extract features and invoke Rule. | Worker | AnalysisJob | AnalysisResult | Internal worker | Analysis | Repository Analysis Service | AnalysisResultPort | None | Stage transaction per phase | AnalysisCompleted | Feature extraction failure | Yes | Job id |
| UC-ANA-003 | RetrieveAnalysisResult | Read completed analysis. | API | AnalysisId | AnalysisResultResponse | Owner | Analysis | None | AnalysisResultPort | None | Read-only | None | Not found | No | Safe |
| UC-ANA-004 | CompareAnalyses | Compare completed analyses. | API | Analysis IDs | AnalysisComparisonResponse | Owner | Analysis | Comparison policy | AnalysisResultPort | None | Read-only | None | Invalid comparison | No | Safe |
| UC-RUL-001 | ExecuteRuleEvaluation | Calculate official scores. | Worker/internal | Normalized facts, RuleSetVersion | EvaluationResult | Internal worker | Evaluation | Skill Evaluation Service | RuleConfigPort, AnalysisResultPort | None | Evaluation transaction | EvaluationCompleted | Rule config invalid | No unsafe retry | Input/version |
| UC-RUL-002 | GenerateSkillMatrix | Create Skill Matrix. | Worker/event | EvaluationResult | SkillMatrix | Internal worker | SkillMatrix | Skill Evaluation Service | AnalysisResultPort | None | Matrix transaction | SkillMatrixGenerated | Missing evidence | Retry after fix | EvaluationId |
| UC-CAR-001 | EvaluateCareerReadiness | Assess career readiness. | Event/API | SkillMatrix, CareerProfileVersion | CareerReadiness | Owner/internal | CareerAssessment | Career Recommendation Service | CareerProfilePort | None | Assessment transaction | CareerReadinessEvaluated | Missing profile | Retry after config | Matrix/profile |
| UC-CMP-001 | EvaluateCompanyReadiness | Assess company readiness. | Event/API | SkillMatrix, CompanyProfileVersion | CompanyReadiness | Owner/internal | CompanyAssessment | Career Recommendation Service | CompanyProfilePort | None | Assessment transaction | CompanyReadinessEvaluated | No company selected | No | Matrix/profile |
| UC-REC-001 | GenerateRecommendations | Generate deterministic recommendations. | Event/API | Readiness, gaps | RecommendationSet | Owner/internal | Recommendation | Recommendation service | RecommendationPort | None | Recommendation transaction | RecommendationGenerated | No actionable gap | No | Assessment key |
| UC-LRN-001 | CreateLearningRoadmap | Create roadmap. | API/event | RecommendationSet | LearningRoadmap | Owner | LearningRoadmap | Learning Roadmap Service | RoadmapPort | NotificationPort | Roadmap transaction | LearningRoadmapCreated | Conflict unresolved | Conditional | RecommendationSet |
| UC-LRN-002 | UpdateRoadmapProgress | Update step progress. | API | Step status | RoadmapStep | Owner | LearningRoadmap | Progress policy | RoadmapPort | AuditPort | Roadmap transaction | RoadmapStepCompleted | Invalid transition | No | ETag/action |
| UC-KNW-001 | ImportKnowledgeDocument | Create ingestion job. | API/event | Source reference | JobStatusResponse | Owner/provider permission | KnowledgeDocument | Knowledge ingestion policy | KnowledgeStorePort | ObjectStoragePort, JobQueuePort | Job/document transaction | KnowledgeDocumentImported | Unsupported source | Yes | Source/hash |
| UC-KNW-002 | NormalizeKnowledgeDocument | Normalize source document. | Worker | Document version | Normalized document | Internal worker | KnowledgeDocument | KnowledgeIngestionService | KnowledgeStorePort | ObjectStoragePort | Stage transaction | KnowledgeNormalized | Invalid content | Yes | VersionId |
| UC-KNW-003 | ChunkKnowledgeDocument | Create chunks. | Worker | Document version | Chunks | Internal worker | KnowledgeDocument | KnowledgeIngestionService | KnowledgeStorePort | None | Chunk batch transaction | KnowledgeChunked | Token/format issue | Yes | VersionId |
| UC-KNW-004 | IndexKnowledgeDocument | Generate embeddings and index. | Worker | Chunks, model version | Indexed chunks | Internal worker | KnowledgeDocument | KnowledgeIngestionService | KnowledgeStorePort | VectorSearchPort | Chunk/index transaction per batch | KnowledgeIndexed | Provider/index failure | Yes | Chunk/model |
| UC-KNW-005 | SearchKnowledge | Retrieve knowledge. | API/internal | Query/filter | KnowledgeSearchResponse | Owner | Retrieval operation | Knowledge Retrieval Service | KnowledgeStorePort | VectorSearchPort | Read-only | None | Index unavailable | Yes | Optional |
| UC-PRM-001 | CreatePromptContext | Build immutable context. | API/internal | Task/source refs | PromptContext | Owner | PromptExecution | Prompt Composition Service | PromptTemplatePort | KnowledgeRetrievalPort | Prompt transaction | PromptContextCreated | Missing context/token overflow | No | Required |
| UC-PRM-002 | ValidatePromptContext | Validate context. | API/internal | PromptContextId | Validation result | Owner | PromptExecution | Prompt validation policy | PromptTemplatePort | None | Read/update validation transaction | PromptContextValidated | Invalid variables | No | ContextId |
| UC-PRM-003 | ComposePrompt | Prepare provider-ready prompt package. | Internal | Locked PromptContext | PromptExecution | Internal | PromptExecution | Prompt Composition Service | PromptTemplatePort | None | Execution transaction | PromptComposed | Template incompatibility | No | Context/version |
| UC-AI-001 | RequestGeneration | Enqueue AI generation. | API | Generation request | JobStatusResponse | Owner | AITask | AI request policy | PromptExecutionPort | JobQueuePort | Job transaction | GenerationRequested | Prompt invalid | No | Required |
| UC-AI-002 | InvokeAIProvider | Call LLM provider. | Worker | PromptExecution | AIResponse | Internal worker | AITask | None | AIExecutionPort | AIProviderPort | Attempt transaction excluding provider call | AIResponseReceived | Timeout/provider error | Yes | Attempt key |
| UC-AI-003 | ValidateAIResponse | Validate grounding and format. | Worker | AIResponse | ValidationResult | Internal worker | AITask | AI Response Validation Service | AIExecutionPort | None | Validation transaction | GenerationCompleted/Failed | Unsupported claims | Retry only if provider failure | ResponseId |
| UC-AI-004 | PersistGeneratedArtifact | Store generated artifact. | Worker | Validated response | GeneratedArtifact | Owner/internal | GeneratedArtifact | Artifact composition | ArtifactPort | ObjectStoragePort | Artifact transaction | ArtifactGenerated | Storage failure | Yes | Response/artifact |
| UC-PRT-001 | GeneratePortfolio | Generate portfolio draft. | API/job | Portfolio request | JobStatusResponse/Portfolio | Owner | Portfolio | Portfolio Generation Service | PortfolioPort | AI module, ObjectStoragePort | Job then artifact transaction | PortfolioGenerated | Missing project evidence | Yes | Required |
| UC-PRT-002 | ReviewPortfolio | Update review state. | API | User edits | Portfolio | Owner | Portfolio | Artifact publication policy | PortfolioPort | AuditPort | Portfolio transaction | PortfolioReviewed | Version conflict | No | ETag |
| UC-PRT-003 | PublishPortfolio | Publish immutable version. | API | PortfolioId/version | Portfolio | Owner | Portfolio | Artifact publication policy | PortfolioPort | AuditPort | Publication transaction | ArtifactPublished | Invalid state | No | Required |
| UC-RSM-001 | GenerateResume | Generate resume draft. | API/job | Resume request | JobStatusResponse/Resume | Owner | Resume | Portfolio Generation Service | ResumePort | AI module, ObjectStoragePort | Job then artifact transaction | ResumeGenerated | Missing evidence | Yes | Required |
| UC-ART-001 | ExportArtifact | Generate export file. | API/job | Artifact/version | JobStatusResponse | Owner | GeneratedArtifact | Export policy | ArtifactPort | ObjectStoragePort | Export metadata transaction | ArtifactExported | Storage failure | Yes | Required |

## 10. Domain Service Integration

| Domain Service | Why Domain Service | Invoked By | Inputs | Determinism | Side-effect Policy | Dependencies | Test Strategy |
|---|---|---|---|---|---|---|---|
| Repository Analysis Service | Decides analysis eligibility and extracts canonical features across snapshot facts. | RequestRepositoryAnalysis, ExecuteRepositoryAnalysis | RepositorySnapshot, source facts, permissions | Deterministic | Returns decisions/facts; persistence by application service | Snapshot facts | Golden snapshot feature tests |
| Skill Evaluation Service | Executes rules and creates Skill Matrix not owned by one entity. | ExecuteRuleEvaluation, GenerateSkillMatrix | Normalized facts, RuleSetVersion, Evidence | Deterministic | No provider calls; application persists result | Rule config | Golden deterministic tests |
| Career Recommendation Service | Compares SkillMatrix to career/company expectations. | EvaluateCareerReadiness, EvaluateCompanyReadiness | SkillMatrix, profile versions | Deterministic | No persistence; returns assessments/gaps | Profile definitions | Regression tests per profile |
| Learning Roadmap Service | Orders recommendations into measurable roadmap. | CreateLearningRoadmap | RecommendationSet, learning policy | Deterministic policy-based | No external calls | Recommendation data | Ordering and invariant tests |
| Knowledge Retrieval Service | Ranks retrievable knowledge for grounding. | SearchKnowledge, CreatePromptContext | Query intent, filters, user scope | Deterministic where index/model fixed | Does not generate responses | Knowledge metadata, vector results | Retrieval contract tests |
| Prompt Composition Service | Assembles prompt context from structured inputs. | CreatePromptContext, ComposePrompt | Template version, variables, retrieval results | Deterministic | Does not call LLM | Prompt metadata | Variable/token validation tests |
| Portfolio Generation Service | Maps validated generated artifacts to user artifact drafts. | GeneratePortfolio, GenerateResume | GeneratedArtifact, source refs | Deterministic mapping | Does not call LLM directly | Artifact metadata | Provenance tests |

Domain services must not directly call databases, HTTP providers, caches, object storage, or vector stores.

## 11. Ports and Adapters

### 11.1 Inbound Ports

| Port | Purpose | Owning Module | Operation Categories | Consistency | Implementation Candidates | Failure Behavior |
|---|---|---|---|---|---|---|
| HTTP API | Receive REST commands/queries. | Interface layer/modules | User/admin API operations | Per endpoint | Spring MVC or equivalent | Map to API error contract |
| Background Job Handler | Execute queued work. | Worker modules | Sync, analysis, ingestion, AI, export | Job-state consistent | Worker runtime | Persist job failure |
| Scheduler | Trigger periodic work. | Scheduler runtime | Refresh, cleanup, reindex | Idempotent | Scheduling framework | Skip/retry with audit |
| Webhook Receiver | Receive callbacks/events. | Integration | OAuth callbacks, future webhooks | Idempotent | HTTP callback handler | Reject invalid signature/state |
| Administrative Command | Handle privileged commands. | Administration | Config activation, support actions | Strong | Admin API | Audit every action |
| Internal Event Handler | React to domain/integration events. | Consumer modules | Projection/update workflows | Eventually consistent | In-process or job-backed handler | Idempotent retry |

### 11.2 Outbound Ports

| Port | Purpose | Owning Module | Operation Categories | Consistency | Implementation Candidates | Failure Behavior |
|---|---|---|---|---|---|---|
| User Repository Port | Load/save users and settings. | Identity | Identity persistence | Strong | PostgreSQL adapter | Transaction rollback |
| Repository Metadata Port | Persist repositories and metadata. | Repository | Repository current state | Strong | PostgreSQL adapter | Retry safe writes |
| Snapshot Storage Port | Persist snapshot metadata/content refs. | Repository | Snapshot creation/read | Immutable | PostgreSQL + object storage adapter | Fail snapshot creation |
| Analysis Result Port | Persist analyses/evaluations. | Analysis/Rule | Evaluation history | Strong/append-only | PostgreSQL adapter | Reject partial result |
| Rule Configuration Port | Read active rule versions. | Rule | Config lookup | Strong for activation | PostgreSQL/cache adapter | Fail closed |
| Career Profile Port | Read career profile versions. | Career | Config lookup | Strong for assessment | PostgreSQL/cache adapter | Fail closed |
| Knowledge Store Port | Persist knowledge docs/chunks. | Knowledge | Knowledge metadata | Strong | PostgreSQL/object adapter | Mark job failed |
| Vector Search Port | Search/index embeddings. | Knowledge | Similarity index | Eventually consistent | Vector DB adapter | Retrieval degraded |
| Prompt Template Port | Read prompt versions. | Prompt | Template lookup | Strong for execution | PostgreSQL/cache adapter | Fail closed |
| AI Provider Port | Invoke LLM providers. | AI | Completion/generation | External | OpenAI/Anthropic/Gemini/Ollama adapters | Timeout/fallback |
| Object Storage Port | Store large content/exports. | Artifact/Knowledge | Blob storage | Metadata strong; content external | Object storage adapter | Mark content unavailable |
| Cache Port | Cache sessions/projections/status. | Shared infra | Get/set/expire/invalidate | Non-authoritative | Redis adapter | Cache miss fallback |
| Job Queue Port | Enqueue/lease jobs. | Application/Worker | Async processing | At-least-once | DB-backed or queue adapter | Retry/dead-letter |
| Notification Port | Send notifications. | Notification | Email/in-app notifications | Eventually consistent | Email/provider adapter | Retry or mark failed |
| Audit Port | Record audit. | Audit | Audit append | Strong where required | PostgreSQL adapter | Fail or compensate per policy |
| Clock Port | Provide time. | Shared Kernel | Timestamps/time windows | Deterministic in tests | System clock/test clock | N/A |
| ID Generator Port | Generate IDs. | Shared Kernel | Entity/job/event IDs | Unique | UUID/ULID adapter | Fail command |

## 12. Persistence Architecture

| Area | Backend Rule |
|---|---|
| Repository abstraction | Application services use repository ports, not direct database access. |
| Aggregate-oriented persistence | Save aggregates through owning module ports. |
| Transactions | Transaction boundaries are application-service owned. |
| Logical schema ownership | Aligns with `09_Database_Design.md`; no module writes another module's authoritative tables directly. |
| Read/write separation | Commands use aggregate repositories; queries may use read ports/projections. |
| Immutable snapshot storage | Snapshot content is written once; later changes create new snapshots or retention markers. |
| Append-only history | Evaluations, SkillMatrices, readiness, prompt executions, AI responses, audit records, and artifact versions preserve history. |
| Soft deletion | User-visible deletion excludes records from reads before final retention cleanup. |
| Audit history | Audit writes are append-only and separated from operational logs. |
| Pagination | Repositories return cursor-based pages for large collections. |
| Bulk operations | Chunking, embedding, and sync writes are batched but remain idempotent. |
| Mapping boundaries | Domain objects are not assumed to be identical to persistence records. |
| Primary technology | Spring Data JPA with Hibernate is confined to module-owned outbound persistence adapters. |
| Persistence models | Adapter-owned JPA entities are separate from domain entities and API DTOs; mapping is explicit and tested. |
| ORM schema behavior | Open Session in View and runtime schema update/create are disabled; Flyway owns schema evolution. |
| Fetch policy | Adapters use explicit projections, entity graphs, fetch joins, or bounded queries; uncontrolled lazy loading is prohibited. |
| Read-query policy | Complex reads use module-owned read ports with JPQL or reviewed native SQL; introducing jOOQ requires ADR review. |
| Concurrency | Optimistic locking is required where mutable aggregate lost updates matter; immutable history rejects updates. |
| Identifier/audit policy | Domain identifiers are application-assigned opaque values; auditable changes preserve actor, correlation, and timestamp metadata. |

Repository ports are defined by application use-case need, not by table count. Spring Data repository interfaces and persistence mappers remain private implementation details of the owning adapter.

## 13. Transaction Boundaries

| Use Case | Transaction Owner | Entities | Atomic Operations | External Calls Excluded | Event Publication | Rollback | Idempotency |
|---|---|---|---|---|---|---|---|
| User profile update | Identity application service | User/Profile | Validate, update, audit intent | None | After commit | Revert profile changes | ETag/request key |
| First OAuth login provisioning | Identity application service | User, ExternalIdentity, audit intent | Resolve or atomically create provider-independent User and unique provider link | Provider exchange and user-info lookup before transaction | After commit | No partial User/link | Provider/subject constraint |
| Logout/session revocation | Identity application service | ApplicationSession, audit intent | Revoke selected or affected sessions and record security transition | Provider revocation only when separately required | After commit | Session remains valid only if transaction fails | Session/action key |
| Repository registration | Repository service | Repository | Create or reuse repository record | GitHub lookup before transaction | Outbox after commit | No repository record | Source ref key |
| Repository snapshot creation | Repository worker | RepositorySnapshot | Persist snapshot metadata and content refs | GitHub collection before transaction | Outbox after commit | Snapshot not ready | Repository/revision |
| Analysis result persistence | Analysis/Rule worker | Evaluation, Evidence | Persist evaluation, scores, evidence links | None during final write | Outbox after commit | No partial official result | Input/version |
| Skill matrix generation | Rule worker | SkillMatrix | Persist matrix and assessments | None | Outbox after commit | No matrix published | Evaluation ID |
| Career readiness persistence | Career service | CareerAssessment | Persist readiness and gaps | None | Outbox after commit | No assessment | Matrix/profile |
| Recommendation creation | Recommendation service | RecommendationSet | Persist set, recommendations, evidence links | None | Outbox after commit | No set | Assessment key |
| Roadmap progress update | Learning service | LearningRoadmap | Update step and progress | Notification after commit | After commit | Revert progress | ETag/action key |
| Prompt context creation | Prompt service | PromptContext | Validate and lock context | Knowledge retrieval before transaction or retained result ref | After commit | No locked context | Context hash/key |
| Generated artifact persistence | AI/Artifact worker | GeneratedArtifact | Persist metadata and content ref | LLM call before transaction; object write staged | After commit | Artifact absent or failed | Response/artifact key |
| Portfolio publication | Portfolio service | PortfolioVersion | Validate, publish version, update active pointer | Export generation excluded | After commit | No publication | Version/action key |

Long-running provider calls, LLM calls, embedding calls, and file generation must not run inside long database transactions.

## 14. Domain Events and Integration Events

| Event | Type | Producer | Consumers | Handling | Delivery | Ordering | Duplication | Failure Policy |
|---|---|---|---|---|---|---|---|---|
| RepositoryRegistered | Domain | Repository | Audit, Dashboard | Async | At-least-once | Per repository | Idempotent | Retry |
| RepositorySynchronizationRequested | Integration | Repository | Worker, Notification | Async | At-least-once | Per job | Idempotent | Retry/dead-letter |
| RepositorySynchronized | Integration | Repository worker | Knowledge, Dashboard | Async | At-least-once | Per repository | Idempotent | Retry |
| RepositorySnapshotCreated | Domain | Repository | Analysis, Knowledge | Async | At-least-once | Per snapshot | Idempotent | Retry |
| AnalysisRequested | Domain | Analysis | Worker, Audit | Async | At-least-once | Per analysis | Idempotent | Retry |
| AnalysisCompleted | Domain | Analysis | Rule, Dashboard | Async | At-least-once | Per analysis | Idempotent | Retry |
| EvaluationCompleted | Domain | Rule | SkillMatrix, Audit | Async | At-least-once | Per evaluation | Idempotent | Retry |
| SkillMatrixGenerated | Domain | Rule | Career, Company, Dashboard | Async | At-least-once | Per matrix | Idempotent | Retry |
| CareerReadinessEvaluated | Domain | Career | Recommendation, Prompt, Dashboard | Async | At-least-once | Per assessment | Idempotent | Retry |
| CompanyReadinessEvaluated | Domain | Company | Recommendation, Prompt, Dashboard | Async | At-least-once | Per assessment | Idempotent | Retry |
| RecommendationGenerated | Domain | Recommendation | Learning, Dashboard | Async | At-least-once | Per set | Idempotent | Retry |
| LearningRoadmapCreated | Domain | Learning | Dashboard, Notification | Async | At-least-once | Per roadmap | Idempotent | Retry |
| KnowledgeDocumentImported | Domain | Knowledge | Knowledge worker, Audit | Async | At-least-once | Per document version | Idempotent | Retry |
| KnowledgeIndexed | Domain | Knowledge | Prompt, Dashboard | Async | At-least-once | Per doc/model | Idempotent | Retry |
| PromptContextCreated | Domain | Prompt | AI, Audit | Async/sync handoff | At-least-once | Per context | Idempotent | Retry |
| GenerationRequested | Domain | AI | AI worker | Async | At-least-once | Per job | Idempotent | Retry |
| GenerationCompleted | Domain | AI | Artifact, Portfolio/Resume/Interview, Dashboard | Async | At-least-once | Per task | Idempotent | Retry |
| GenerationFailed | Domain | AI | Notification, Audit | Async | At-least-once | Per task | Idempotent | Retry |
| PortfolioGenerated | Domain | Portfolio | Dashboard, Notification | Async | At-least-once | Per portfolio version | Idempotent | Retry |
| ResumeGenerated | Domain | Resume | Dashboard, Notification | Async | At-least-once | Per resume version | Idempotent | Retry |
| ArtifactPublished | Domain | Portfolio/Resume | Audit, Dashboard | Async | At-least-once | Per artifact version | Idempotent | Retry |

No technology-specific topics, queues, or stream names are defined here.

## 15. Event Publication Reliability

| Concern | Initial Recommendation |
|---|---|
| Transactional outbox | Use a transactional outbox inside the modular monolith for domain/integration events that must not be lost after aggregate changes. |
| After-commit publication | Publish events only after aggregate transaction commits. |
| Event persistence | Store event metadata, type, aggregate reference, version, correlation, causation, and delivery status. |
| Duplicate delivery | Assume at-least-once delivery; consumers must be idempotent. |
| Consumer idempotency | Consumers track processed event identity or use natural idempotency keys. |
| Retry | Retry transient failures with backoff. |
| Dead-letter handling | Persist repeatedly failing events for admin inspection and replay. |
| Replay | Support replay for projections, notifications, and downstream derived records where safe. |
| Event versioning | Include event version and preserve backward-compatible event evolution. |

Kafka or another broker is not mandatory initially. A database-backed outbox plus worker dispatcher is suitable for the modular monolith unless throughput or integration boundaries require broker adoption.

## 16. Asynchronous Processing Architecture

### 16.1 Common Job States

| State | Meaning |
|---|---|
| queued | Accepted and waiting for worker. |
| running | Worker is processing. |
| succeeded | Job completed and result reference is available. |
| failed | Job failed with persisted error. |
| cancelled | Job stopped by user/system where supported. |
| expired | Job metadata is no longer active. |

### 16.2 Job Type Catalog

| Job Type | Owner Module | Enqueue Condition | Worker Responsibility | Progress | Timeout | Retry | Cancellation | Idempotency | Result Reference | Failure Persistence | Notification |
|---|---|---|---|---|---|---|---|---|---|---|---|
| GitHub synchronization | Repository | User sync or scheduled refresh | Collect, normalize, create snapshot | Phase and counts | Configurable | Transient provider failures | Before final snapshot | Repository/scope/key | Job error record | On completion/failure |
| Notion synchronization | Knowledge/Integration | User import or scheduled refresh | Collect pages/docs | Phase and page counts | Configurable | Transient provider failures | Before ingestion finalization | Workspace/source/key | Job error record | On completion/failure |
| Repository analysis | Analysis/Rule | Analysis request or snapshot event | Feature extraction and rule execution | Phase | Configurable | Retry extraction; not deterministic validation errors | Before final result | Snapshot/rule version | Analysis failure | On completion/failure |
| Knowledge ingestion | Knowledge | Import/upload | Normalize, version, chunk | Phase and chunk counts | Configurable | Yes | Before indexed | Source/content hash | Ingestion failure | On completion/failure |
| Document chunking | Knowledge | Document version ready | Create chunks | Chunk counts | Configurable | Yes | Limited | Version ID | Chunking failure | Usually no user notification unless terminal |
| Embedding generation | Knowledge | Chunks ready or reindex | Create embeddings/index | Chunk counts | Configurable | Yes | System-controlled | Chunk/model | Embedding failure | On terminal failure |
| AI generation | AI | Generation request | Invoke LLM and validate response | Phase | Provider-specific | Provider transient failures | Where provider allows | Prompt/task key | Generation failure | On completion/failure |
| Portfolio generation | Portfolio/AI | Portfolio generate action | Use AI artifact and create draft | Phase | Configurable | Yes | Before draft finalization | Portfolio/request key | Generation failure | On completion/failure |
| Resume generation | Resume/AI | Resume generate action | Use AI artifact and create draft | Phase | Configurable | Yes | Before draft finalization | Resume/request key | Generation failure | On completion/failure |
| File export | Artifact | Export request | Render/export file and persist object ref | Phase | Configurable | Yes | Before final file | Artifact/version/format | Export failure | On completion/failure |

## 17. Scheduling Architecture

| Scheduled Operation | Owner | Cadence Category | Concurrency Policy | Overlap Prevention | Failure Handling | Audit Behavior |
|---|---|---|---|---|---|---|
| Repository refresh | Repository | Configurable periodic | Per user/repository lock | Skip or reschedule if active sync | Retry transient failures | Audit summary |
| Provider token validation | Integration | Configurable periodic | Per connection | Single validation per connection | Mark expired/revoked | Audit permission changes |
| Stale job cleanup | Worker/Admin | Frequent maintenance | Global or partitioned | Single leader preferred | Retry later | Operational log |
| Expired cache cleanup | Infrastructure | Frequent maintenance | Cache-native or scheduled | N/A | Best effort | No audit unless security relevant |
| Artifact expiration | Artifact | Periodic retention | Per artifact/export | Lock per artifact | Mark failed and retry | Audit deletion/expiry |
| Knowledge re-indexing | Knowledge | Configurable/on-demand | Per document/model | Version/model lock | Retry or mark reindex failed | Audit major rebuilds |
| Audit retention processing | Audit | Compliance cadence | Single retention process per scope | Retention lock | Escalate failure | Audit retention record |
| Provider health checks | Integration/AI | Frequent health | Per provider | De-duplicate checks | Mark provider degraded | Operational metric |

Exact schedules are configurable and must be approved by operations.

## 18. Repository Synchronization Pipeline

| Stage | Owner | Description | Transaction Boundary |
|---|---|---|---|
| Repository Sync Request | API/Repository | User requests sync using API contract. | Job creation transaction. |
| Authorization Validation | Application | Validate owner and GitHub permission. | Before job creation. |
| Job Creation | Repository | Persist sync job and idempotency key. | Single transaction. |
| GitHub Adapter | Infrastructure | Fetch provider data through adapter. | Outside DB transaction. |
| Raw Data Collection | Worker | Collect repository metadata/activity/documents. | Provider-call phase. |
| Normalization | Repository | Translate provider data into canonical facts. | In-memory/staged. |
| Snapshot Creation | Repository domain | Build immutable snapshot candidate. | Snapshot write transaction. |
| Snapshot Persistence | Repository infrastructure | Persist metadata and content refs. | Single snapshot transaction. |
| RepositorySynchronized Event | Outbox | Publish event after commit. | After commit. |
| Optional Analysis Request | Analysis | Create analysis job if requested by policy/user. | Separate transaction. |

| Concern | Rule |
|---|---|
| Incremental synchronization | Use provider checkpoint/source update references where available. |
| Full synchronization | Allowed when checkpoint missing, stale, or user explicitly requests. |
| Provider rate limits | Translate to retryable job failure or delayed retry. |
| Partial failure | Persist successful canonical portions only when consistency rules allow; mark partial state. |
| Duplicate prevention | Use repository/source revision idempotency. |
| Branch and commit consistency | Snapshot must capture internally consistent branch/commit references. |
| External normalization | Provider-specific fields remain behind GitHub adapter. |

## 19. Repository Analysis Pipeline

| Stage | Owner | Output | Version/Snapshot Recording |
|---|---|---|---|
| RepositorySnapshot | Repository | Immutable source state | SnapshotId recorded. |
| Feature Extraction | Analysis | Normalized features | Analysis input hash recorded. |
| Evidence Construction | Rule | Evidence records | Source references recorded. |
| Rule Engine | Rule | EvaluationResult | RuleSetVersion recorded. |
| Skill Matrix | Rule | SkillMatrix | EvaluationId recorded. |
| Career Path Engine | Career | CareerReadiness | CareerProfileVersion recorded. |
| Company Readiness | Company | CompanyReadiness | CompanyProfileVersion recorded. |
| Skill Gap | Career | SkillGap records | SkillMatrix and profile version recorded. |
| Recommendation | Recommendation | RecommendationSet | Policy/version references recorded. |
| Learning Roadmap | Learning | LearningRoadmap | RecommendationSet recorded. |

The LLM never participates in score calculation, readiness calculation, skill gap calculation, or recommendation priority calculation.

## 20. Knowledge Ingestion Pipeline

| Stage | Owner | Description | Boundary |
|---|---|---|---|
| Source Import | Knowledge/Integration | Receive GitHub, Notion, upload, or generated-report source reference. | API/job command. |
| Validation | Knowledge | Validate owner, permission, source type, size, and privacy class. | Application precondition. |
| Normalization | Knowledge | Convert source into canonical knowledge document form. | Domain service. |
| Document Versioning | Knowledge | Create content hash and KnowledgeDocumentVersion. | Transaction. |
| Metadata Extraction | Knowledge | Extract source, tags, career/company/project metadata where available. | Deterministic processing. |
| Chunking | Knowledge | Split document version into chunks. | Batch transaction. |
| Embedding Metadata | Knowledge | Create embedding records with provider/model version. | Batch/job transaction. |
| Vector Indexing | Knowledge adapter | Write derived vector index. | External adapter; eventually consistent. |
| Retrieval Eligibility | Knowledge | Mark chunks eligible only if indexed, fresh, and permitted. | Transaction. |

Knowledge does not generate final AI responses.

## 21. Prompt and AI Generation Pipeline

| Stage | Owner | Description | Guardrail |
|---|---|---|---|
| Generation Request | AI/API | Client requests supported task with source references. | Reject client-supplied scores/readiness. |
| Context Selection | Prompt | Select structured outputs and retrieval results. | Respect ownership and token budget. |
| Prompt Context Creation | Prompt | Create immutable PromptContext. | Prompt Builder has no business logic. |
| Prompt Composition | Prompt | Assemble provider-ready prompt package. | Hidden instructions not exposed to normal users. |
| AI Provider Invocation | AI | Call configured provider through port. | Provider details behind adapter. |
| AI Response Capture | AI | Persist model execution and response metadata. | No official score mutation. |
| Response Validation | AI | Validate grounding, format, forbidden claims. | Reject unsupported outputs. |
| Artifact Persistence | AI/Artifact | Persist GeneratedArtifact with source references. | Provenance required. |

## 22. Artifact Generation Pipeline

| Artifact | Pipeline | Required Source Context |
|---|---|---|
| Portfolio | Project/evidence selection → PromptContext → AI generation → validation → portfolio draft → review → publish | Project, SkillMatrix, Evidence, PromptContext |
| Resume | Skill/project selection → PromptContext → AI generation → validation → resume draft → review/export | SkillMatrix, Project, Career/Company context |
| README Improvement | RepositorySnapshot → PromptContext → AI generation → validation → README draft | RepositorySnapshot, README evidence |
| Interview Question Set | Career/company context → PromptContext → AI generation → validation → question set | CareerReadiness, CompanyReadiness, SkillGap |
| Export | Published/reviewed artifact → render/export job → object storage reference | Artifact version |

Generated artifacts must retain source context and distinguish AI-generated content from user edits.

## 23. Response Validation Architecture

| Validation Stage | Owner | Purpose | Failure Outcome |
|---|---|---|---|
| Transport validation | Interface | Validate HTTP shape and content. | API validation error. |
| Schema validation | Interface/Application | Validate request/response contract fields. | API validation error. |
| Grounding validation | AI | Check response against PromptContext sources. | Rejected or valid with warnings. |
| Evidence reference validation | AI/Rule boundary | Ensure cited evidence exists and is accessible. | Rejected. |
| Score consistency validation | AI | Ensure output does not alter official scores. | Rejected. |
| Forbidden claim detection | AI | Detect unsupported facts or policy violations. | Rejected or warning. |
| Output completeness validation | AI | Ensure required sections are present. | Retryable or rejected. |
| Artifact format validation | Artifact | Ensure generated artifact can be stored/rendered. | Artifact failed. |

Response outcomes are `valid`, `valid with warnings`, `rejected`, `retryable failure`, and `non-retryable failure`.

## 24. Read Model Architecture

| Read Model | Source of Truth | Projection Creation | Freshness | Rebuild | Cacheable | Authorization |
|---|---|---|---|---|---|---|
| Dashboard | Multiple domain aggregates | Event handlers | Eventual | From source aggregates | Yes | Owner |
| Repository Summary | Repository, sync jobs | Repository events | Eventual | From repository tables | Yes | Owner |
| Analysis History | Analysis/Evaluation | Analysis events | Eventual | From evaluation history | Yes | Owner |
| Skill Overview | SkillMatrix | SkillMatrixGenerated | Eventual | From matrices | Yes | Owner |
| Career Readiness Summary | CareerReadiness | Assessment events | Eventual | From assessments | Yes | Owner |
| Company Readiness Summary | CompanyReadiness | Assessment events | Eventual | From assessments | Yes | Owner |
| Active Roadmap | LearningRoadmap | Roadmap events | Near real-time | From roadmap | Yes | Owner |
| Recent Recommendations | RecommendationSet | Recommendation events | Eventual | From recommendations | Yes | Owner |
| Generated Artifact List | GeneratedArtifact | Artifact events | Eventual | From artifacts | Yes | Owner |
| Job Status | Job records | Job state updates | Near real-time | From job records | Short-lived | Owner/admin |

Read models must not become authoritative write models.

## 25. CQRS Usage

| Area | Command Path | Query Path | CQRS Rationale |
|---|---|---|---|
| Repository sync | Sync command creates job/snapshot. | Repository summary projection. | Avoid heavy sync reads in command model. |
| Analysis | Analysis command creates immutable results. | Analysis history and dashboard views. | Historical read optimization. |
| Recommendations | Generate/status commands update aggregate. | Active recommendation view. | Fast dashboard card access. |
| Roadmap | Progress commands update roadmap. | Roadmap progress view. | UI responsiveness. |
| Knowledge | Ingestion/reindex commands update documents. | Search/retrieval indexes. | Different query shape. |
| AI artifacts | Generation commands create artifacts. | Artifact history view. | Eventual history acceptable. |

Separate databases or full event sourcing are unnecessary initially.

## 26. Cache Architecture

| Cache Category | Owner | Source of Truth | Key Scope Concept | Expiration | Invalidation Trigger | Consistency | Miss Behavior | Sensitive Data Policy |
|---|---|---|---|---|---|---|---|---|
| Session cache | Identity | JDBC-backed PostgreSQL session store for MVP | User/session | Idle and absolute policy | Logout/expiry/revocation | Strong enough for auth | Reauthenticate | No provider tokens; Redis optional only after review |
| Reference-data cache | Rule/Career/Company/Prompt | Config tables | Version ID | Medium | ConfigurationChanged | Strong on activation | Load from DB | No secrets |
| Repository summary cache | Repository/Dashboard | Repository records | User/repository | Short | Repository events | Eventual | Query DB | No raw private content |
| Dashboard cache | Dashboard | Projections/source aggregates | User | Short | Domain events | Eventual | Rebuild projection | User scoped |
| Provider token metadata cache | Integration | Connection metadata/secret ref | Connection | Short | Permission change | Eventual | Load metadata | No token values |
| Rate-limit state | Integration/AI | Provider/user policy | User/provider | Provider window | Time/window reset | Operational | Conservative deny/delay | No secrets |
| Temporary job progress | Worker | Job records | Job ID | Short | Job state update | Eventual | Load job record | Owner scoped |
| Temporary retrieval result cache | Knowledge | Retrieval result/source docs | User/query/context | Very short | KnowledgeUpdated | Eventual | Execute retrieval | No cross-user data |

Cache must never become the source of truth.

## 27. External Integration Architecture

| Adapter | Responsibilities | Auth Boundary | Request Normalization | Response Normalization | Timeout/Retry | Circuit Breaker | Rate Limiting | Error Translation | Observability | Data Isolation |
|---|---|---|---|---|---|---|---|---|---|---|
| GitHub | OAuth, repo list, metadata, commits, PRs, issues, README, dependencies. | Provider token handle | DevPath sync request to GitHub API. | Provider data to canonical repository facts. | Yes | Yes | Provider/user | GitHub errors to API/job errors. | Provider latency/error metrics. | User permission scope. |
| Notion | OAuth, workspace/page/document import. | Provider token handle | Import request to Notion API. | Blocks/pages to knowledge documents. | Yes | Yes | Provider/user | Notion errors to safe categories. | Page counts/latency. | Workspace/page scope. |
| AI Providers | LLM generation. | Provider credential config | Prompt package to provider request. | Provider response to AIResponse. | Yes | Yes | Quota/model | Timeout/refusal/rate errors. | Token/latency/failure metrics. | Prompt minimization. |
| Object Storage | Store content/exports. | Storage credential boundary | Content refs/upload/download actions. | Secure object reference. | Yes | Yes | Storage limits | Storage errors to artifact errors. | Size/latency metrics. | Owner-scoped refs. |
| Notification Provider | Send email/in-app/push where supported. | Provider credential | Notification command to provider message. | Delivery status. | Yes | Yes | Provider | Delivery failure. | Delivery metrics. | No sensitive content by default. |
| Future GitLab | Repository provider. | Provider token | GitLab to repository canonical. | Canonical facts. | Yes | Yes | Provider | Safe provider errors. | Provider metrics. | User scope. |
| Future Jira | Work item/document source. | Provider token | Jira work items to knowledge/project facts. | Canonical knowledge. | Yes | Yes | Provider | Safe errors. | Provider metrics. | Project permissions. |
| Future Slack | Communication knowledge source. | Provider token | Slack messages to knowledge docs. | Canonical knowledge. | Strict | Yes | Provider/workspace | Safe errors. | Provider metrics. | Strong privacy filters. |

## 28. Resilience and Fault Tolerance

| Mechanism | Policy |
|---|---|
| Timeout | Every external provider call has a bounded timeout category. |
| Retry | Retry transient failures only; do not retry deterministic validation failures. |
| Exponential backoff | Use backoff for provider and temporary infrastructure failures. |
| Jitter | Add jitter to prevent retry storms. |
| Circuit breaker | Protect GitHub, Notion, AI, vector, object storage, and notification providers. |
| Bulkhead isolation | Separate worker pools for sync, analysis, knowledge, AI, and export workloads where needed. |
| Graceful degradation | Dashboard may show stale projections; AI generation may be unavailable without blocking deterministic results. |
| Dependency health | Provider status feeds admin and operational views. |
| Partial failure | Sync and ingestion can record partial completion when domain policy allows. |
| Fallback restrictions | AI provider fallback cannot bypass privacy, prompt, or validation rules. |
| Stale data policy | Stale knowledge is excluded unless explicitly allowed by task policy. |
| Failed job recovery | Failed jobs persist errors and retry eligibility. |

## 29. Concurrency and Idempotency

| Scenario | Handling |
|---|---|
| Duplicate repository sync requests | Return existing active job or create new only when scope differs after idempotency expiry. |
| Concurrent analysis requests | Deduplicate by snapshot, rule version, and analysis scope. |
| Duplicate webhook delivery | Use provider event ID/state and idempotent consumer records. |
| Duplicate AI generation requests | Deduplicate by Idempotency-Key and prompt context/task body. |
| Concurrent roadmap updates | Use optimistic locking/resource version checks. |
| Portfolio publication conflicts | Only one version may become published/current per transaction. |
| Repeated export requests | Return existing export job or completed export when idempotency matches. |
| Worker redelivery | Workers must be idempotent and check terminal job/resource state before applying effects. |

Immutable resources reject mutation attempts with conflict errors.

## 29.1 Authentication Architecture

| Area | Backend Rule |
|---|---|
| Login provider | GitHub is the initial Spring Security OAuth2 Login provider. Notion remains an integration authorization flow. |
| Internal identity | A provider-independent User ID is authoritative; provider plus provider subject forms the unique external identity link. |
| Session | Backend issues an opaque session identifier only through a Secure, HttpOnly, SameSite=Lax host-only cookie in production. |
| Storage stages | Local single-instance development may use memory; MVP and initial persistent deployment use JDBC-backed PostgreSQL sessions; Redis is not required. |
| Session lifecycle | Enforce configurable idle and absolute expiry, rotate session ID after login, renew only within the absolute limit, and revoke on logout, suspension, deletion, or compromise. |
| Authorization context | Security adapter resolves the authenticated User and authorities, then passes an immutable authorization context to application use cases. |
| CSRF/CORS | Cookie-authenticated mutations require CSRF validation; credentialed CORS is allowlist-only and same-origin is preferred. |
| Account linking | Linking requires an authenticated session and fresh state bound to the User; email alone never merges accounts. |
| Provider tokens | Integration adapters own encrypted server-side provider tokens; tokens never enter the application session, domain model, controller response, or frontend. |
| Error/audit | Authentication errors are non-enumerating and provider-safe; login, provisioning, link conflicts, logout, revocation, and suspicious sessions are audited without credentials. |

## 30. Authorization Architecture

| Actor | Authorization Boundary |
|---|---|
| Authenticated user | May access own user-owned resources. |
| Resource owner | Required for private repository, knowledge, prompt, AI, artifact, roadmap, and recommendation resources. |
| Administrator | May manage configuration and audit views under privileged policy. |
| Future organization member | May access organization-scoped resources only when future tenant model defines membership. |
| Future organization administrator | May manage organization configuration within tenant boundaries. |
| Internal worker | Uses system authority plus carried user/resource scope. |
| Provider callback | Validated by OAuth state, signature, or provider verification; does not imply user resource access without mapping. |

Authorization is enforced at interface layer, application service preconditions, repository filtering, and provider adapter permission checks. Controller-level checks alone are insufficient.

## 31. Validation Architecture

| Validation Level | Owner | Examples | Avoided Duplication |
|---|---|---|---|
| Request syntax validation | Interface | JSON shape, path params, headers. | Does not check domain rules. |
| API schema validation | Interface/Application | Required fields, enum values, size limits. | Does not calculate business results. |
| Application precondition validation | Application | Ownership, lifecycle state, idempotency, resource existence. | Delegates invariants to domain. |
| Domain invariant validation | Domain | Score ranges, immutability, evidence required, prompt context lock. | Single source in domain. |
| External provider validation | Adapter/Application | Provider permission, provider response shape. | Does not leak provider model. |
| Generated response validation | AI | Grounding, output format, forbidden claims. | Does not modify deterministic data. |

## 32. Error Handling Architecture

| Internal Category | API Error Mapping | Notes |
|---|---|---|
| Validation | VALIDATION_ERROR | Field-level details where safe. |
| Authentication | AUTHENTICATION_REQUIRED | Missing, expired, invalid, or revoked application session. |
| Authorization | AUTHORIZATION_DENIED or RESOURCE_NOT_FOUND | Hide existence when necessary. |
| Not found | RESOURCE_NOT_FOUND | Owner scope applied. |
| Conflict | RESOURCE_CONFLICT | Immutable mutation or version conflict. |
| Domain invariant | RESOURCE_CONFLICT or VALIDATION_ERROR | Depends on caller action. |
| Dependency failure | DEPENDENCY_UNAVAILABLE | External/internal dependency unavailable. |
| Timeout | DEPENDENCY_UNAVAILABLE or AI_PROVIDER_FAILURE | Retryable flag set when safe. |
| Rate limit | RATE_LIMIT_EXCEEDED | Include retry guidance when known. |
| Job failure | SYNCHRONIZATION_FAILED, ANALYSIS_FAILED, or task-specific code | Persisted on job resource. |
| Provider failure | Provider-safe category | No raw payload/secrets. |
| AI response validation failure | AI_RESPONSE_REJECTED | May be retryable. |
| Persistence failure | INTERNAL_SERVER_ERROR | Safe message only. |
| Internal error | INTERNAL_SERVER_ERROR | No stack trace to client. |

## 33. Logging Architecture

| Log Field | Policy |
|---|---|
| correlationId | Propagate across request, job, event, and provider calls. |
| requestId | Record per API request. |
| traceId | Record tracing correlation. |
| user reference | Use opaque user ID; avoid personal data unless required. |
| job ID | Include for worker logs. |
| analysis ID | Include for analysis/evaluation logs. |
| generation ID | Include for AI generation logs. |
| provider reference | Use safe provider request reference only. |
| event reference | Include event ID and type. |
| severity | Use standard severity categories. |
| sensitive redaction | Redact tokens, secrets, private content, hidden prompts. |
| exception logging | Internal stack traces allowed only in secure logs. |
| audit separation | Audit logs are separate from operational logs. |

Do not log OAuth tokens, API keys, raw secrets, full private repository contents, hidden system prompts, or unnecessary personal data.

## 34. Metrics Architecture

| Metric Area | Examples |
|---|---|
| API requests | Count by endpoint, method, status, domain. |
| Latency | Request latency, adapter latency, DB operation latency. |
| Error rate | Error count by category and module. |
| Active jobs | Queued/running jobs by type. |
| Failed jobs | Failure count and terminal failure rate. |
| Retry count | Retry attempts by job/provider. |
| Queue delay | Time from enqueue to start. |
| Provider latency | GitHub, Notion, AI, vector, object storage. |
| Provider failure | Timeout, rate limit, dependency unavailable. |
| Repository sync duration | Duration by repository size category. |
| Analysis duration | Feature extraction and rule evaluation duration. |
| Knowledge ingestion duration | Normalize/chunk/index timing. |
| Embedding duration | Embedding generation timing. |
| Generation duration | LLM and validation duration. |
| Response validation rejection | Rejection count by reason. |
| Cache hit ratio | Per cache category. |

Detailed dashboards belong to a later Observability document.

## 35. Distributed Tracing

| Segment | Parent/Child Relationship |
|---|---|
| API request | Root trace or child of upstream client trace. |
| Application service | Child span of API request or job handler. |
| Database adapter | Child span of application service. |
| Provider adapter | Child span with provider-safe metadata. |
| Job enqueue | Links current trace to future worker trace. |
| Background worker | Continues trace using correlation/job metadata. |
| Event handler | Continues causation from event metadata. |
| AI provider call | Child span of generation worker with redacted prompt metadata. |

Asynchronous trace continuation uses correlation ID, causation ID, event ID, and job ID.

## 36. Audit Architecture

| Operation | Actor | Target | Audit Metadata |
|---|---|---|---|
| Login and connection changes | User/provider callback | User/connection | Outcome, provider, time. |
| Repository registration | User | Repository | Source reference, outcome. |
| Repository archive | User | Repository | Reason, outcome. |
| Analysis request | User/system | Snapshot/analysis | Snapshot and rule version refs. |
| Target career change | User | User preference | Old/new career refs. |
| Target company change | User | User preference | Old/new company refs. |
| Roadmap status change | User | Roadmap/step | Transition and outcome. |
| Portfolio publication | User | Portfolio version | Version and source refs. |
| Resume export | User | Resume version/export | Format and object ref metadata. |
| Admin configuration change | Admin | Config version | Action, target version, effective time. |
| Prompt template activation | Admin | PromptTemplateVersion | Validation outcome. |
| Rule version activation | Admin | RuleSetVersion | Validation outcome. |

Audit records include actor, action, target, timestamp, outcome, correlation reference, minimized metadata, and retention responsibility.

## 37. Configuration Architecture

| Configuration Category | Owner | Versioning | Validation | Activation | Rollback | Secret Separation | Runtime Reload |
|---|---|---|---|---|---|---|---|
| Environment configuration | Operations | Environment version | Startup/runtime validation | Deploy/config change | Deploy rollback | Secrets external | Restart or reload |
| Feature configuration | Administration/Product | Versioned where needed | Rule checks | Admin activation | Disable/new version | No secrets | Dynamic if safe |
| Provider configuration | Integration/AI | Versioned metadata | Connectivity/limits | Admin/ops | Previous config | Credentials separated | Dynamic with cache invalidation |
| Timeout configuration | Operations | Change history | Bounds validation | Config deployment | Previous value | No secrets | Dynamic where safe |
| Retry configuration | Operations | Change history | Safety validation | Config deployment | Previous value | No secrets | Dynamic |
| Quota configuration | Administration/Ops | Versioned policy | Bounds validation | Admin activation | Previous policy | No secrets | Dynamic |
| Rule configuration | Rule/Admin | RuleSetVersion | Deterministic validation | Admin activation | Supersede | No secrets | Active version pointer |
| Career profile configuration | Career/Admin | CareerProfileVersion | Compatibility validation | Admin activation | Supersede | No secrets | Active version pointer |
| Company profile configuration | Company/Admin | CompanyProfileVersion | Compatibility validation | Admin activation | Supersede | No secrets | Active version pointer |
| Prompt template configuration | Prompt/Admin | PromptTemplateVersion | Variable/token/security validation | Admin activation | Supersede | No secrets | Active version pointer |

Business configuration must not be mixed with infrastructure secrets.

## 38. Feature Flag Architecture

| Use Case | Owner | Evaluation Location | Audit | Default | Removal Lifecycle | Prohibited Use |
|---|---|---|---|---|---|---|
| Provider rollout | Integration/Ops | Application service/adapters | Config change audit | Disabled for unsupported users | Remove after rollout | Hiding provider permission failures |
| New analysis version | Rule/Admin | Rule selection policy | Rule activation audit | Current stable version | Convert to config version | Replacing RuleSetVersion |
| New career profile | Career/Admin | Profile selection policy | Profile activation audit | Current active profile | Supersede old flag | Permanent business rule storage |
| New company profile | Company/Admin | Company profile selection | Profile activation audit | Current active profile | Supersede old flag | Silent score mutation |
| Experimental AI output | AI/Product | Generation request policy | Feature audit | Disabled | Promote or remove | Bypassing validation |
| Portfolio template rollout | Portfolio/Product | Artifact generation policy | Feature audit | Stable template | Promote to template version | Permanent template storage |
| Organization feature rollout | Product/Admin | Authorization/application layer | Audit enabled | Disabled | Replace with tenant config | Weakening user isolation |

Feature flags must not permanently replace domain configuration.

## 39. Package and Namespace Structure

### 39.1 Conceptual Structure

```text
src/
  identity/{interface,application,domain,infrastructure}
  integration/{interface,application,domain,infrastructure}
  repository/{interface,application,domain,infrastructure}
  analysis/{interface,application,domain,infrastructure}
  rule/{application,domain,infrastructure}
  career/{interface,application,domain,infrastructure}
  company/{interface,application,domain,infrastructure}
  recommendation/{interface,application,domain,infrastructure}
  learning/{interface,application,domain,infrastructure}
  knowledge/{interface,application,domain,infrastructure}
  prompt/{interface,application,domain,infrastructure}
  ai/{interface,application,domain,infrastructure}
  artifact/{interface,application,domain,infrastructure}
  portfolio/{interface,application,domain,infrastructure}
  resume/{interface,application,domain,infrastructure}
  interview/{interface,application,domain,infrastructure}
  dashboard/{interface,application,infrastructure}
  administration/{interface,application,domain,infrastructure}
  audit/{interface,application,domain,infrastructure}
  notification/{interface,application,domain,infrastructure}
  shared/{kernel,observability,security}
```

### 39.2 Structure Rules

| Area | Rule |
|---|---|
| Module boundaries | Modules expose only public application/module APIs. |
| Dependency rules | Domain does not depend on infrastructure or interface packages. |
| Public module API | Cross-module calls use application ports, events, or read ports. |
| Internal visibility | Module internals are not imported directly by other modules. |
| Shared kernel | Contains only stable primitives and abstractions. |
| Test placement | Tests mirror module and layer boundaries. |

No framework-specific classes are defined by this structure.

## 40. Shared Kernel

| Allowed Candidate | Rationale |
|---|---|
| Identifiers | Common opaque ID primitives. |
| Time abstractions | Testable time and timestamp handling. |
| Pagination primitives | Shared cursor/page metadata concepts. |
| Common result types | Standard success/failure metadata where domain-neutral. |
| Domain event base concepts | Event identity, correlation, causation, version metadata. |
| Generic value abstractions | Only when not business-specific. |
| Correlation metadata | Request/job/event correlation. |

Prohibited in Shared Kernel:

- User, Repository, Rule, Skill, Career, Company, Recommendation, Knowledge, Prompt, AI, Portfolio, Resume, or Interview entities.
- Business services.
- Provider-specific adapters.
- Persistence models.
- Mutable shared state.

## 41. API-to-Application Mapping

| API Area | Representative Operations | Owning Module | Application Service | Authorization Policy | Transaction | Async Job | Aggregate | Result Resource | Event |
|---|---|---|---|---|---|---|---|---|---|
| Identity | API-ID-001~009 | Identity | GetCurrentUser, UpdateUserProfile, SetCareerTarget, SetCompanyTarget | Self/admin | User/profile transaction | Deletion job for delete | User | User/Profile | UserProfileUpdated |
| GitHub/Notion Integration | API-INT-001~012 | Integration/Repository/Knowledge | InitiateOAuth, HandleCallback, DisconnectProvider, ImportKnowledge | Self/provider permission | Connection/job transaction | Sync/ingestion jobs | User/Integration | Connection/Job | GitHubConnected, NotionConnected |
| Repository | API-REP-001~012 | Repository | RegisterRepository, SynchronizeRepository, ArchiveRepository | Owner | Repository transaction | Sync job | Repository/Snapshot | Repository/Snapshot | RepositorySynchronized |
| Analysis/Rule | API-ANA-001~008 | Analysis/Rule | RequestRepositoryAnalysis, ExecuteRuleEvaluation | Owner/internal | Analysis/evaluation transaction | Analysis job | Evaluation | Analysis/RuleEvaluation | EvaluationCompleted |
| Skill | API-SKL-001~007 | Rule | GenerateSkillMatrix/RetrieveSkillMatrix | Owner | Matrix transaction/read | Event-driven | SkillMatrix | SkillMatrix | SkillMatrixGenerated |
| Career/Company | API-CAR/CMP | Career/Company | EvaluateCareerReadiness, EvaluateCompanyReadiness | Owner | Assessment transaction | Optional event-driven | CareerAssessment/CompanyAssessment | Readiness | CareerReadinessEvaluated |
| Recommendation | API-REC-001~008 | Recommendation | GenerateRecommendations, Accept/Dismiss/Complete | Owner | Recommendation transaction | Optional | RecommendationSet | Recommendation | RecommendationGenerated |
| Learning | API-LRN-001~010 | Learning | CreateLearningRoadmap, UpdateRoadmapProgress | Owner | Roadmap transaction | Optional creation | LearningRoadmap | Roadmap | RoadmapStepCompleted |
| Knowledge | API-KNW-001~011 | Knowledge | Import/Chunk/Index/SearchKnowledge | Owner/provider permission | Document/job transaction | Ingestion/index jobs | KnowledgeDocument | Knowledge/Search | KnowledgeIndexed |
| Prompt | API-PRM-001~009 | Prompt | CreatePromptContext, ValidatePromptContext | Owner/admin | Prompt transaction | No | PromptExecution | PromptContext | PromptContextCreated |
| AI | API-AI-001~007 | AI | RequestGeneration, InvokeAIProvider, ValidateAIResponse | Owner | Job/response/artifact transaction | Generation job | AITask | Artifact/Job | GenerationCompleted |
| Portfolio | API-PRT-001~010 | Portfolio | GeneratePortfolio, ReviewPortfolio, PublishPortfolio | Owner/public where published | Portfolio transaction | Generation/export jobs | Portfolio | Portfolio | PortfolioGenerated |
| Resume | API-RSM-001~008 | Resume | GenerateResume, ExportArtifact | Owner | Resume/export transaction | Generation/export jobs | Resume | Resume | ResumeGenerated |
| Interview | API-ITV-001~007 | Interview | GenerateInterviewQuestions, RequestAnswerFeedback | Owner | Question set transaction | Generation/feedback jobs | InterviewQuestionSet | QuestionSet | InterviewQuestionsGenerated |
| Dashboard | API-DSH-001~010 | Dashboard | RetrieveDashboardSummary and read services | Owner | Read-only | No | None | DashboardSummary | None |
| Administration | API-ADM-001~011 | Administration | ActivateRule/Career/Company/Prompt, SupportActions | Admin | Config transaction | Optional support job | Configuration | Admin resources | ConfigurationChanged |

## 42. Data-to-Module Mapping

| Canonical Object | Owning Module | Write Authority | Read Consumers | Persistence Adapter | Mutability | Cache Eligibility | Event Publication |
|---|---|---|---|---|---|---|---|
| User | Identity | Identity services | All authorized modules by reference | User Repository Port | Mutable status/profile prefs | Session/profile cache | UserRegistered |
| Repository | Repository | Repository services | Analysis, Knowledge, Dashboard | Repository Metadata Port | Current state mutable | Repository summary cache | RepositoryRegistered |
| RepositorySnapshot | Repository | Snapshot service | Analysis, Rule, Knowledge, AI | Snapshot Storage Port | Immutable | Metadata cache only | RepositorySnapshotCreated |
| Evaluation | Rule | Rule services | Skill, Career, Dashboard | Analysis Result Port | Immutable after completion | Summary cache | EvaluationCompleted |
| SkillMatrix | Rule | Rule services | Career, Company, Recommendation, Prompt | Analysis Result Port | Immutable historical | Current matrix cache | SkillMatrixGenerated |
| CareerProfile | Career/Admin | Admin/Career | Career, Prompt | Career Profile Port | Versioned | Reference cache | CareerProfileActivated |
| CompanyProfile | Company/Admin | Admin/Company | Company, Prompt | Career/Profile Port | Versioned | Reference cache | CompanyProfileActivated |
| CareerReadiness | Career | Career services | Recommendation, Dashboard, Prompt | Career Profile/Assessment Port | Immutable after completion | Summary cache | CareerReadinessEvaluated |
| CompanyReadiness | Company | Company services | Recommendation, Dashboard, Prompt | Company Assessment Port | Immutable after completion | Summary cache | CompanyReadinessEvaluated |
| Recommendation | Recommendation | Recommendation services | Learning, Dashboard, Prompt | Recommendation Port | Status mutable | Active recommendation cache | RecommendationGenerated |
| LearningRoadmap | Learning | Learning services | Dashboard, Prompt | Roadmap Port | Progress mutable | Roadmap cache | LearningRoadmapCreated |
| KnowledgeDocument | Knowledge | Knowledge services | Prompt, Search | Knowledge Store Port | Metadata/freshness mutable | Source summary cache | KnowledgeDocumentImported |
| KnowledgeChunk | Knowledge | Knowledge services | Retrieval, Prompt | Knowledge Store/Vector Port | Derived/index status mutable | Retrieval cache | KnowledgeIndexed |
| PromptTemplate | Prompt/Admin | Admin/Prompt | Prompt | Prompt Template Port | Versioned | Reference cache | PromptTemplateActivated |
| PromptContext | Prompt | Prompt services | AI, Audit | Prompt Template/Execution Port | Immutable after creation | Temporary candidate only | PromptContextCreated |
| AIResponse | AI | AI services | Artifact, Audit | AI Execution Port | Validation state then immutable | No raw content cache | GenerationCompleted |
| GeneratedArtifact | AI/Artifact | AI/Artifact services | Portfolio, Resume, Dashboard | Artifact Port | Review status mutable | Artifact list cache | ArtifactGenerated |
| Portfolio | Portfolio | Portfolio services | Dashboard/export/public | Portfolio Port | Draft mutable; published immutable | Portfolio cache | PortfolioGenerated |
| Resume | Resume | Resume services | Dashboard/export | Resume Port | Draft mutable; published immutable | Resume cache | ResumeGenerated |

No canonical object has multiple write owners.

## 43. Requirement Traceability

| Requirement Group | Owning Module | Application Service/Internal Process | Validation Mechanism | Persistence or Output Responsibility | API Operations |
|---|---|---|---|---|---|
| FR-001~FR-020 | Identity | GetCurrentUser, UpdateUserProfile, SetCareer/CompanyTarget | Auth, ownership, profile validation | User/profile/connection state | API-ID-* |
| FR-021~FR-050 | Integration/Repository | OAuth, RegisterRepository, SynchronizeRepository | Provider permission, sync validation | Repository and snapshots | API-INT-*, API-REP-* |
| FR-051~FR-070 | Integration/Knowledge | Notion connect/import | Workspace permission, source validation | Knowledge documents | API-INT-*, API-KNW-* |
| FR-071~FR-100 | Repository/Analysis | Snapshot and analysis orchestration | Snapshot readiness | Analysis inputs/history | API-ANA-* |
| FR-101~FR-180, RR-001~RR-010 | Rule | ExecuteRuleEvaluation, GenerateSkillMatrix | Rule validation, score/evidence invariants | Evaluation, scores, SkillMatrix | API-ANA-*, API-SKL-* |
| FR-181~FR-220, CR-001~CR-020 | Career/Company/Recommendation/Learning | Evaluate readiness, generate recommendations, create roadmap | Profile version, gap, recommendation invariants | Readiness, recommendations, roadmap | API-CAR/CMP/REC/LRN |
| FR-221~FR-280, AI-001~AI-015 | Prompt/AI/Artifact/Portfolio/Resume/Interview | CreatePromptContext, RequestGeneration, ValidateAIResponse | Prompt validation, grounding validation | AIResponse, GeneratedArtifact, artifacts | API-PRM/AI/PRT/RSM/ITV |
| PR-001~PR-015 | Prompt/Admin | Template management, context creation | Variable/token/security validation | PromptTemplateVersion, PromptContext | API-PRM-* |
| KR-001~KR-020 | Knowledge | Import/chunk/index/search | Permission, metadata, freshness, retrieval validation | KnowledgeDocument, chunks, embeddings | API-KNW-* |
| FR-281~FR-320 | Dashboard | Projection/query services | Owner filtering | Dashboard read models | API-DSH-* |
| FR-341~FR-360 | Administration/Audit | Config and audit services | Admin auth, config validation | ConfigurationChange, AuditRecord | API-ADM-* |

## 44. Testing Implications

| Test Type | Architecture Expectation |
|---|---|
| Domain unit tests | Test aggregates, value objects, policies, invariants, deterministic services without infrastructure. |
| Application service tests | Test orchestration, authorization, transactions, event publication, idempotency. |
| Adapter contract tests | Verify provider/persistence/cache/vector/object adapters satisfy port contracts. |
| Repository integration tests | Validate persistence adapter behavior against database design. |
| Provider adapter tests | Validate normalization and error translation for GitHub, Notion, AI providers. |
| API contract tests | Verify `10_API_Specification.md` request/response/error contracts. |
| Async job tests | Verify job states, retries, cancellation, failure persistence. |
| Event idempotency tests | Verify duplicate event delivery does not duplicate side effects. |
| Rule Engine golden tests | Verify deterministic identical input/version outputs. |
| Career Engine regression tests | Verify readiness/gap/recommendation outputs per profile version. |
| AI response validation tests | Verify unsupported claims, score mutation attempts, and format failures are rejected. |
| Architecture dependency tests | Verify module and layer dependency rules. |

## 45. Scalability Strategy

| Area | Strategy | Constraint |
|---|---|---|
| API instances | Stateless horizontal scaling. | Shared persistence and cache bottlenecks remain. |
| Background workers | Scale by job type and workload queue/lease partitions. | Provider quotas limit throughput. |
| Repository synchronization | Partition by user/repository/provider; respect rate limits. | GitHub quota and repository size. |
| Analysis workload | Worker pool for CPU-bound deterministic processing. | Rule complexity and snapshot size. |
| Embedding workload | Batch chunks and isolate from interactive API. | Embedding provider limits and vector index throughput. |
| AI generation workload | Dedicated worker pool and quota policy. | LLM latency/cost/provider limits. |
| Dashboard queries | Projections, cache, read replicas. | Freshness lag. |
| PostgreSQL | Indexing, partitioning, read replicas, query tuning. | Write bottlenecks for append-heavy data. |
| Redis | Cache clustering and TTL discipline. | Non-authoritative only. |
| Vector Database | Metadata filtering, index partitioning, reindex strategy. | Privacy and model-version compatibility. |
| Object Storage | Offload large content and exports. | Content availability and retention policy. |

## 46. Microservice Extraction Strategy

| Candidate | Extraction Trigger | Required Contract Stability | Data Ownership Change | Event Boundary | Operational Cost | Risks | Why Not Yet |
|---|---|---|---|---|---|---|---|
| Repository Collector | Sync volume or provider isolation pressure. | Repository sync job and snapshot contracts stable. | Repository remains owner or service owns sync subset. | RepositorySynchronized, SnapshotCreated. | Medium | Provider consistency, duplication. | Modular monolith sufficient initially. |
| Analysis Engine | CPU-bound rule workload scales independently. | Analysis input and EvaluationResult contracts stable. | Rule data ownership may move with service. | AnalysisRequested/Completed. | Medium/high | Distributed reproducibility. | Initial workload unknown. |
| Knowledge Ingestion | Chunk/embedding volume grows. | KnowledgeDocument and chunk contracts stable. | Knowledge service owns ingestion/index. | KnowledgeImported/Indexed. | Medium | Privacy/index drift. | Simpler in monolith first. |
| AI Generation | LLM latency/cost isolation required. | PromptContext and AIResponse contracts stable. | AI service owns generation records. | GenerationRequested/Completed. | Medium | Prompt leakage, validation coupling. | Can isolate worker first. |
| Artifact Export | Rendering workload scales separately. | Artifact/export contract stable. | Artifact metadata remains owner. | ExportRequested/Generated. | Low/medium | File retention mismatch. | Worker role enough initially. |
| Notification | Delivery reliability/scale requires isolation. | Notification event contract stable. | Notification service owns delivery. | NotificationCreated. | Low | Delivery duplication. | Low initial complexity. |

## 47. Deployment Unit Boundaries

| Deployment Unit | Modules Included | Purpose | Notes |
|---|---|---|---|
| API Runtime | Interface and application modules for synchronous APIs. | Serve REST contracts. | Same codebase as workers; different runtime role. |
| Worker Runtime | Job-capable modules. | Execute sync, analysis, ingestion, AI, export jobs. | Can be scaled by job category. |
| Scheduler Runtime | Scheduling application services. | Trigger periodic maintenance and refresh. | Can share codebase with API/worker. |
| Optional AI Worker | Prompt/AI/Artifact subset. | Isolate LLM latency and quota workloads. | Future optimization, not required initially. |
| Optional Ingestion Worker | Repository/Knowledge ingestion subset. | Isolate provider and embedding workloads. | Future optimization, not required initially. |

Detailed infrastructure belongs to DevOps Architecture.

## 48. Security Boundaries

| Boundary | Architectural Control |
|---|---|
| Application session | Resolve opaque cookie at the interface/security layer; never expose or log the session identifier. |
| Provider tokens | Store encrypted server-side with externally managed key material; adapter boundary only. |
| Private repository data | Owner scope enforced in API, application services, persistence filters, and knowledge retrieval. |
| Notion content | Workspace/page permission enforced before ingestion and prompt inclusion. |
| Prompt content | Hidden system prompts restricted; PromptContext owner-scoped. |
| Generated artifacts | Owner access by default; publication requires explicit action. |
| Administrative configuration | Privileged access and audit required. |
| Uploaded files | Validate type, size, malware scan expectation, owner scope. |
| Audit records | Restricted admin/compliance access; metadata minimization. |

## 49. Open Issues and ADR Candidates

| Issue ID | Decision Context | Options | Recommendation | Impact | Decision Owner | Status | ADR Candidate |
|---|---|---|---|---|---|---|---|
| BE-OPEN-001 | Backend language/framework confirmation. | Java/Spring Boot, alternative JVM, other stack. | Java 21 LTS with Spring Boot accepted by ADR-020. | Implementation standards. | Architecture | Resolved | ADR-020 |
| BE-OPEN-011 | Persistence and ORM approach. | JPA/Hibernate, jOOQ, Spring Data JDBC, hybrid. | JPA/Hibernate with explicit adapter mapping accepted by ADR-024. | Persistence boundaries. | Data/Backend | Resolved | ADR-024 |
| BE-OPEN-012 | Migration tool. | Flyway, Liquibase. | Flyway immutable versioned SQL accepted by ADR-025. | Schema evolution. | Data/Ops | Resolved | ADR-025 |
| BE-OPEN-013 | Authentication and session model. | Server session, token model, hybrid. | GitHub OAuth2 Login with opaque server-managed session accepted by ADR-026. | Identity/security. | Security/Backend | Resolved | ADR-026 |
| BE-OPEN-002 | Job processing technology. | DB-backed jobs, message broker, managed queue. | DB-backed/outbox first. | Reliability and operations. | Backend/DevOps | Open | ADR-BE-002 |
| BE-OPEN-003 | Message broker necessity. | None, later broker, immediate broker. | Avoid broker until throughput requires. | Complexity. | Architecture | Open | ADR-BE-003 |
| BE-OPEN-004 | Transactional outbox implementation. | DB table dispatcher, library, broker transaction. | DB outbox first. | Event reliability. | Backend | Open | ADR-BE-004 |
| BE-OPEN-005 | Vector database selection. | pgvector, dedicated vector DB, hybrid. | Decide in storage implementation. | Knowledge retrieval. | Data/AI | Open | ADR-BE-005 |
| BE-OPEN-006 | AI streaming support. | Non-streaming, streaming with validation, staged streaming. | Non-streaming initially. | UX and validation. | AI/API | Open | ADR-BE-006 |
| BE-OPEN-007 | Application-level encryption. | Storage encryption only, app encryption for sensitive fields, hybrid. | Evaluate in Security Architecture. | Privacy and complexity. | Security | Open | ADR-BE-007 |
| BE-OPEN-008 | Event schema registry. | Documented schemas, lightweight registry, formal registry. | Documented schemas initially. | Event compatibility. | Backend | Open | ADR-BE-008 |
| BE-OPEN-009 | Multi-tenancy. | User-only, organization beta, enterprise tenant. | User-only v1 with tenant-ready references. | Data ownership. | Product/Architecture | Open | ADR-BE-009 |
| BE-OPEN-010 | Service extraction timing. | Extract early, extract by triggers, remain monolith. | Trigger-based extraction. | Operations. | Architecture | Open | ADR-BE-010 |

## 50. Future Extensions

| Extension | Backend Compatibility |
|---|---|
| GitLab | Add provider adapter and repository normalization without changing Rule Engine. |
| Bitbucket | Add provider adapter behind Integration/Repository ports. |
| Jira | Add knowledge/project adapter and later evidence rules if approved. |
| Slack | Add permission-scoped knowledge ingestion adapter. |
| Figma | Add design artifact ingestion and portfolio references. |
| Baekjoon | Add coding-practice source after deterministic rules are defined. |
| Programmers | Same coding-practice extension model. |
| LeetCode | Same coding-practice extension model. |
| Organization workspaces | Add tenant/organization module and ownership policy. |
| Team analysis | Add aggregate projections only with explicit permission model. |
| Mentor workflows | Add mentor review context without mutating official scores. |
| Recruiter workflows | Add sharing/public portfolio module. |
| Enterprise tenancy | Add tenant isolation, admin roles, organization audit. |
| Mobile clients | Reuse REST contracts and client version metadata. |
| Public APIs | Add OAuth scopes, quotas, public documentation, and stronger compatibility policy. |
| Real-time notifications | Add SSE/WebSocket/push adapter while preserving job model. |
| Streaming AI responses | Add streaming only after validation-safe architecture is approved. |

Future extensions are not current committed scope.

## 51. Final Consistency Review

### 51.1 Checklist

| Review Item | Status |
|---|---|
| Every API operation maps to an application service. | Passed |
| Every application service has one owning module. | Passed |
| Every canonical object has one write owner. | Passed |
| Domain modules do not depend on infrastructure. | Passed |
| Controllers contain no business logic. | Required by architecture |
| Rule Engine remains deterministic. | Passed |
| Career Engine remains deterministic. | Passed |
| LLM does not calculate authoritative scores. | Passed |
| Prompt Builder contains no business logic. | Passed |
| Knowledge performs retrieval only. | Passed |
| RepositorySnapshot remains immutable. | Passed |
| PromptContext remains immutable. | Passed |
| Historical results remain immutable. | Passed |
| External providers are behind adapters. | Passed |
| Long-running operations use jobs. | Passed |
| Transaction boundaries are explicit. | Passed |
| Retry and idempotency rules are defined. | Passed |
| Domain and integration events are distinguished. | Passed |
| Cache is never source of truth. | Passed |
| API errors map consistently. | Passed |
| Sensitive data is not logged. | Passed |
| Terminology matches `07_Domain_Model.md`. | Passed |
| Data ownership matches `08_System_Data_Model.md`. | Passed |
| Persistence responsibilities match `09_Database_Design.md`. | Passed |
| API contracts match `10_API_Specification.md`. | Passed |
| No unsupported functionality was introduced. | Passed |

### 51.2 Completion Metrics

| Metric | Count |
|---|---:|
| Module count | 20 |
| Application service count | 35 |
| Inbound port count | 6 |
| Outbound port count | 17 |
| Domain event count | 18 |
| Integration event count | 3 |
| Asynchronous job type count | 10 |
| Unresolved decision count | 9 |

### 51.3 Requirement Coverage Summary

| Requirement Source | Coverage |
|---|---|
| SRS `FR-001~FR-360` | Covered through modules, application services, API mapping, persistence, and jobs. |
| Rule `RR-001~RR-010` | Covered by Rule Module, Skill Evaluation Service, deterministic tests, and evaluation pipeline. |
| Career `CR-001~CR-020` | Covered by Career, Company, Recommendation, and Learning modules. |
| AI `AI-001~AI-015` | Covered by Prompt, AI, Artifact, Portfolio, Resume, and Interview modules. |
| Prompt `PR-001~PR-015` | Covered by Prompt Module, Prompt Composition Service, prompt ports, and validation. |
| Knowledge `KR-001~KR-020` | Covered by Knowledge Module, ingestion/retrieval pipeline, vector port, and privacy rules. |

### 51.4 Final Completeness Checklist

| Deliverable Requirement | Status |
|---|---|
| Module catalog expanded. | Complete |
| Application service catalog expanded. | Complete |
| Port catalog expanded. | Complete |
| Transaction boundaries expanded. | Complete |
| Event catalog expanded. | Complete |
| Asynchronous processing expanded. | Complete |
| Pipeline descriptions included. | Complete |
| API mapping included. | Complete |
| Data ownership mapping included. | Complete |
| Requirement traceability included. | Complete |
| Consistency review included. | Complete |

## 52. Identity Foundation Implementation Evidence

| Boundary | Actual Path | Status |
|---|---|---|
| Domain | `backend/src/main/java/com/devpath/identity/domain` | Framework-independent identity model created |
| Application | `backend/src/main/java/com/devpath/identity/application` | OAuth login and current-user use cases created |
| Inbound adapters | `backend/src/main/java/com/devpath/identity/adapter/in` | OAuth security and HTTP adapters created |
| Outbound persistence | `backend/src/main/java/com/devpath/identity/adapter/out/persistence` | Spring Data repositories remain adapter-local |
| Transaction boundary | `OAuthLoginApplicationService` | Atomic first-login provisioning with database uniqueness as concurrency guard |
| Runtime verification | Gradle Java 21 commands | `clean test` and `build` passed; PostgreSQL-dependent tests skipped because Docker is unavailable |

No repository, analysis, Rule, Career, Knowledge, Prompt, AI, or artifact backend use case is implemented by this slice.
