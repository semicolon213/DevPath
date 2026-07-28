# DevPath Frontend Architecture

## 1. Purpose and Scope

### 1.1 Purpose

This document defines the authoritative frontend architecture for DevPath. It explains how the frontend consumes backend APIs, organizes feature modules and routes, manages server/client/session state, presents deterministic analysis results, presents AI-generated explanations and artifacts, handles asynchronous jobs, supports accessibility and responsive layouts, and maintains secure and observable client behavior.

### 1.2 Scope

This document covers:

- Feature-oriented frontend module architecture.
- Route, navigation, and view architecture.
- Component and design-system architecture.
- Server-state, client-state, and session-state ownership.
- API client and contract mapping.
- Authentication and authorization UX.
- Onboarding, repository, analysis, career, knowledge, AI, portfolio, resume, interview, dashboard, administration, and settings UX.
- Asynchronous job UX.
- Forms, drafts, conflicts, loading, empty, error, and notification patterns.
- Accessibility, responsive design, performance, security, privacy, observability, testing, traceability, open issues, and final review.

### 1.3 Intended Audience

| Audience | Usage |
|---|---|
| Frontend engineers | Implement feature modules, routes, views, state boundaries, and API integrations |
| Backend engineers | Understand frontend expectations for API contracts and job behavior |
| Product designers | Align UI/UX flows with domain and API constraints |
| QA engineers | Derive route, view, accessibility, state, and API contract tests |
| AI engineers | Ensure AI-generated content is presented safely and distinctly |
| Security engineers | Validate client-side trust boundaries and telemetry rules |

### 1.4 Document Authority

This document is the authoritative frontend implementation architecture. Frontend code, routes, feature modules, state usage, API integration, and UI behavior must conform to this document.

### 1.5 Relationship to API and Backend Documents

| Document | Frontend Dependency |
|---|---|
| `10_API_Specification.md` | Defines API resources, schemas, errors, jobs, pagination, idempotency, and versioning consumed by frontend |
| `11_Backend_Architecture.md` | Defines backend module boundaries, jobs, events, authorization, and consistency constraints |
| `07_Domain_Model.md` | Defines domain terminology and immutable/deterministic concepts the UI must preserve |
| `08_System_Data_Model.md` | Defines data ownership, lifecycle, snapshots, versions, and projections |
| `02_Rule_Engine.md` | Defines deterministic score and Skill Matrix boundaries |
| `03_Career_Path_Engine.md` | Defines readiness, gaps, recommendations, and roadmap boundaries |
| `04_AI_Architecture.md` and `05_Prompt_Engineering.md` | Define AI output and prompt context constraints |
| `06_Knowledge_Architecture.md` | Defines knowledge retrieval and source-reference constraints |

### 1.6 Included and Excluded Topics

| Included | Excluded |
|---|---|
| Frontend modules, routes, views, state, API integration, UX states, accessibility, performance, security boundaries, observability, and traceability | Production source code, framework components, hooks, stores, CSS, visual mockups, backend business logic, database structures, SQL, API implementation, cryptographic algorithms, cloud configuration, and complete test cases |

### 1.7 Current Scaffold Evidence

The initial frontend scaffold implements only the shell needed to verify the selected frontend foundation. It does not implement authentication, real dashboard data, repository analysis UI, scoring, AI features, provider integrations, or production design-system components.

| Scaffold Area | Evidence Path | Status |
|---|---|---|
| Vite React TypeScript app | `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tsconfig.json` | Created |
| Application shell | `frontend/src/app/App.tsx`, `frontend/src/app/AppProviders.tsx` | Created |
| Routing foundation | `frontend/src/routes/AppRoutes.tsx` | Created |
| Error boundary | `frontend/src/app/ErrorBoundary.tsx` | Created |
| API client placeholder | `frontend/src/shared/api/apiClient.ts` | Created |
| Frontend validation status | `node scripts/run-frontend.mjs run test -- --run`, `node scripts/run-frontend.mjs run build` | Passed with sandbox escalation |

## 2. Architecture Goals and Principles

### 2.1 Goals

| Goal | Frontend Meaning |
|---|---|
| Correctness | Display authoritative backend results without recalculating official values. |
| Usability | Make complex developer career intelligence understandable and actionable. |
| Trustworthiness | Distinguish deterministic results, evidence, AI explanations, and user-authored content. |
| Maintainability | Organize by features with clear dependencies and shared UI rules. |
| Modularity | Keep features independently evolvable without circular dependencies. |
| Accessibility | Target WCAG 2.2 AA and provide non-visual alternatives for charts and matrices. |
| Responsiveness | Support desktop, tablet, and mobile with route-appropriate layouts. |
| Performance | Optimize large lists, charts, editors, and API usage only where measurable value exists. |
| Testability | Separate view models, API clients, state, and presentation for test coverage. |
| Security | Treat the browser as untrusted and never expose secrets, hidden prompts, or provider credentials. |
| Explainability | Surface evidence, versions, snapshots, and source references. |
| Extensibility | Support future providers, organizations, mobile clients, and new generated artifact types through stable contracts. |

### 2.2 Principles

| Principle | Rule |
|---|---|
| Contract-driven | Frontend MUST consume API contracts from `10_API_Specification.md`. |
| Feature-oriented | Feature modules own route composition, feature-specific state, and view models. |
| Server/client state separation | Server state MUST be managed through server-state mechanisms, not generic global stores. |
| Read-only deterministic outputs | Rule scores, readiness results, snapshots, and historical analyses MUST be read-only. |
| AI transparency | AI-generated content MUST be labeled and separated from deterministic source data. |
| Evidence-first UX | Evidence MUST be reachable from analyses, recommendations, and generated artifacts. |
| Secure by default | UI hiding is never security enforcement; backend authorization remains authoritative. |
| Accessible by default | Interactive and visual components MUST provide keyboard and assistive technology support. |
| Progressive disclosure | Complex results SHOULD start with summaries and allow drill-down into evidence and versions. |

### 2.3 Accepted Frontend Stack Baseline

| Area | Accepted Baseline | Governing ADR |
|---|---|---|
| Framework | React with TypeScript | ADR-021 |
| Rendering Model | Authenticated client-rendered SPA | ADR-021 |
| Routing | React Router-compatible browser routing owned by the frontend application | ADR-021 |
| Server State | React Query baseline for API/server state and polling workflows | ADR-021 |
| Styling Baseline | TailwindCSS as named in Project Context; design-system details remain incremental | ADR-021 |
| Build Tool | Vite category with exact versions pinned during scaffolding | ADR-021, ADR-023 |
| Test Baseline | Vitest, React Testing Library, Playwright, and accessibility checks by layer | ADR-032 |
| Authentication | GitHub OAuth2 Login with backend-managed opaque HttpOnly session cookie | ADR-026 |

### 2.4 Trade-offs

| Trade-off | Decision |
|---|---|
| Rich dashboards vs. freshness | Prefer independent widget loading with freshness indicators over blocking the entire dashboard. |
| Optimistic updates vs. correctness | Use optimistic updates only for user-editable non-authoritative fields. |
| Global state simplicity vs. cache correctness | Use server-state cache for API resources and local state for UI-only concerns. |
| AI convenience vs. trust | Require generated-content labels, source references, and validation warnings even if UI becomes more verbose. |
| Performance vs. premature complexity | Apply code splitting, pagination, and virtualization by default for large views; avoid speculative micro-optimizations. |

## 3. Frontend System Context

### 3.1 External Interactions

| Actor/System | Interaction | Frontend Responsibility | Trust Boundary |
|---|---|---|---|
| End users | Use workspace features, connect providers, review results, generate artifacts. | Provide accessible UI and clear state feedback. | Browser is untrusted. |
| Administrators | Manage configuration and inspect audit/job state. | Gate admin routes and show privileged workflows. | Backend authorization required. |
| Backend API | Source of authoritative resources, jobs, errors, and metadata. | Consume contracts and normalize responses. | Trusted only after authenticated API response. |
| OAuth providers | Redirect through GitHub/Notion connection flow. | Initiate and handle callback UX state. | Provider secrets never exposed. |
| Upload endpoints | Upload knowledge/document sources. | Validate selected files before sending and show scan/ingestion status. | Backend validation authoritative. |
| Download endpoints | Retrieve exports through temporary URLs or export resources. | Show expiration and access status. | URLs are sensitive and temporary. |
| Notification mechanisms | In-app notifications and future real-time updates. | Display non-critical and critical messages appropriately. | Job/API source is authoritative. |
| Future mobile clients | Share API contract semantics. | Keep web architecture compatible with client-version metadata. | Separate client runtime. |
| Future organization users | Organization ownership and roles. | Keep route and permission model extensible. | Not current committed scope. |

### 3.2 Client Responsibilities

The frontend is responsible for presentation, interaction, client-side validation, API orchestration, state display, accessibility, responsiveness, safe telemetry, and user feedback. It is not responsible for official score calculation, career/company evaluation, recommendation priority, prompt business logic, knowledge retrieval authority, or authorization enforcement.

## 4. Frontend Layer Model

### 4.1 Layer Responsibilities

| Layer | Responsibilities | MUST NOT |
|---|---|---|
| Presentation Layer | Rendering, interaction, layout, accessibility, status feedback. | Calculate scores, call APIs directly from low-level UI primitives, own server truth. |
| Feature Layer | Feature orchestration, route composition, use-case flow, feature state, API coordination. | Duplicate backend business rules or own unrelated feature state. |
| View Model Layer | Display-safe transformations, formatting, labels, grouping, presentation-only derived values. | Implement Rule Engine, Career Engine, recommendation priority, or AI validation logic. |
| Data Access Layer | API transport, authentication headers, cancellation, response validation, error normalization, cache integration. | Leak raw API response shapes into presentation components. |
| Platform Layer | Routing, session, authorization context, telemetry, localization, feature flags, browser storage abstraction. | Store authoritative domain data globally without cache policy. |

### 4.2 Allowed Dependency Direction

| From | May Depend On | Rule |
|---|---|---|
| Presentation | View models, shared UI primitives, feature callbacks | No direct API transport dependency. |
| Feature | Data access, view models, shared UI, platform services | Owns feature orchestration. |
| View Model | API contract types, formatting utilities, domain terminology constants | Must remain deterministic presentation mapping only. |
| Data Access | API schema contracts, platform session/correlation utilities | Does not depend on presentation components. |
| Platform | Shared utilities and browser abstractions | Must not import feature internals except route registration metadata where approved. |

## 5. Feature Module Architecture

### 5.1 Feature Catalog

| Feature | Purpose | Owned Routes | Primary Views | API Dependencies | Owned Client State | Server-State Resources | Permissions | Shared Dependencies | Prohibited Responsibilities |
|---|---|---|---|---|---|---|---|---|---|
| Authentication | Login, logout, session restoration, OAuth callback UX. | `/login`, `/oauth/*` | Login, OAuth Callback | API-ID, API-INT | Redirect state, callback status | Current user, connections | Public/self | Platform session, feedback | Token cryptography, provider secrets |
| Onboarding | Guide first-time setup. | `/onboarding/*` | Onboarding Progress | Identity, integration, repository, analysis APIs | Current step, skipped steps | Preferences, connections, sync/analysis jobs | Self | Job UX, forms | AI generation requirement |
| User Profile | Manage profile and preferences. | `/settings/profile` | Profile Settings | API-ID | Form drafts | UserProfile, preferences | Self | Forms | Career readiness calculation |
| Integrations | Connect/disconnect GitHub/Notion. | `/settings/integrations` | Integration Settings | API-INT | OAuth pending state | Connections, provider repository/page summaries | Self | Auth, job UX | Raw credential access |
| Repositories | Repository list and registration. | `/repositories` | Repository List | API-REP, API-INT | Filters, selection | Repository list | Owner | Cards, tables | Snapshot mutation |
| Repository Snapshots | Show snapshot history. | `/repositories/:id/snapshots` | Snapshot List/Detail | API-REP | Selected snapshot tab | Snapshot list/detail | Owner | Version display | Snapshot editing |
| Analysis | Request and inspect analyses. | `/analyses`, `/analyses/:id` | Analysis Progress/Result | API-ANA | Compare selection | Analysis jobs/results | Owner | Job UX, evidence | Score calculation |
| Skill Matrix | Display skills and evidence. | `/skills`, `/skills/:id` | Skill Matrix, Skill Detail | API-SKL | Filters, grouping, selected skill | SkillMatrix, evidence | Owner | Charts, matrix grid | Editing skill levels |
| Career Readiness | Career target and readiness views. | `/careers`, `/career-readiness` | Career Readiness | API-CAR | Selected comparison | Career profiles/readiness | Owner/auth read | Readiness components | Career evaluation logic |
| Company Readiness | Company target and readiness views. | `/companies`, `/company-readiness` | Company Readiness | API-CMP | Selected company comparison | Company profiles/readiness | Owner/auth read | Readiness components | Company score logic |
| Recommendations | Actionable recommendations. | `/recommendations` | Recommendation List/Detail | API-REC | Status filter | Recommendations, evidence | Owner | Cards, evidence | Priority calculation |
| Learning Roadmap | Roadmap and progress tracking. | `/roadmap` | Roadmap Timeline | API-LRN | Step UI state, notes draft | Roadmap, steps, resources | Owner | Timeline, forms | Editing generated reasoning |
| Knowledge | Knowledge library and search. | `/knowledge` | Library, Search, Document Detail | API-KNW | Search query, filters | Documents, chunks, ingestion jobs | Owner | Search UI, job UX | Raw embedding display |
| AI Generation | Generic AI tools and job status. | `/ai-tools`, `/generation-jobs/:id` | Generation Request/Progress | API-AI, API-PRM | Task form state | PromptContext, GenerationJob, Artifact | Owner | Job UX, artifact display | Authoritative scoring |
| Generated Artifacts | Artifact history/detail. | `/artifacts`, `/artifacts/:id` | Artifact Detail | API-AI | Version selection | GeneratedArtifact | Owner | Version selector | Treating AI as source truth |
| Portfolio | Portfolio creation, editing, publish/export. | `/portfolio`, `/portfolio/:id` | Portfolio Editor | API-PRT | Editor draft state | Portfolio, versions, exports | Owner/public where published | Editor, evidence | Silently overwriting facts |
| Resume | Resume creation, editing, export. | `/resume`, `/resume/:id` | Resume Editor | API-RSM | Editor draft state | Resume, versions, exports | Owner | Editor, evidence | Fabricating experience |
| Interview Practice | Question sets and practice answers. | `/interview` | Interview Practice | API-ITV | Answer drafts | Question sets, feedback jobs | Owner | Forms, job UX | Deterministic grading claims |
| Dashboard | Aggregated workspace overview. | `/dashboard` | Dashboard | API-DSH | Widget layout preference | DashboardSummary, cards | Owner | Cards, charts | Source-of-truth writes |
| Notifications | User notifications. | global route/panel | Notification Feed | Notification APIs/future | Panel open state | Notifications | Owner | Feedback | Critical toast-only errors |
| Settings | User settings and privacy. | `/settings/*` | Settings | API-ID, API-INT | Form state | Profile, settings, connections | Self | Forms | Backend security enforcement |
| Administration | Admin configuration and audit. | `/admin/*` | Admin Console | API-ADM | Admin filters | Configs, jobs, audit | Admin | Tables, filters | Private content exposure |
| Shared UI | Reusable UI primitives and domain display components. | none | none | none | Component-local state | none | n/a | Design tokens | Feature orchestration |
| Platform Core | Routing, session, telemetry, localization, feature flags. | app shell | App Shell | Common APIs | Session shell state | Current user/session | All | Core utilities | Business workflows |

### 5.2 Feature Dependency Matrix

| Feature | May Depend On | Must Not Depend On |
|---|---|---|
| All features | Shared UI, Platform Core, Data Access | Other feature internals |
| Dashboard | Reusable view models from domain features or dashboard-specific API responses | Feature private state |
| AI Generation | Prompt, Knowledge, Artifact public feature contracts | Rule calculation internals |
| Portfolio/Resume | Generated Artifacts, Evidence display, Project references | AI provider internals |
| Administration | Admin API contracts and Shared UI | User-owned private content modules |
| Knowledge | Integrations public status, Shared UI | Prompt/AI generation internals |

Circular dependencies are prohibited. Cross-feature reuse must go through shared UI, shared view-model utilities, or explicit feature public APIs.

## 6. Route and Navigation Architecture

### 6.1 Route Catalog

| Route ID | Path Pattern | Auth | Authorization | Owning Feature | Primary API Resources | Loading Strategy | Error Boundary | Deep Link |
|---|---|---|---|---|---|---|---|---|
| R-PUB-001 | `/` | Public | None | Platform Core | None | Static/marketing | Public boundary | Yes |
| R-PUB-002 | `/login` | Public | None | Authentication | OAuth authorization | Immediate | Auth boundary | Yes |
| R-PUB-003 | `/oauth/github/callback` | Callback | OAuth state | Authentication | OAuthCallbackResponse | Blocking callback state | Auth boundary | Yes |
| R-PUB-004 | `/oauth/notion/callback` | Callback | OAuth state | Authentication | OAuthCallbackResponse | Blocking callback state | Auth boundary | Yes |
| R-PUB-005 | `/privacy` | Public | None | Platform Core | None | Static | Public boundary | Yes |
| R-PUB-006 | `/terms` | Public | None | Platform Core | None | Static | Public boundary | Yes |
| R-AUTH-001 | `/onboarding` | Required | Self | Onboarding | User, preferences, connections | Step prefetch | Onboarding boundary | Yes |
| R-AUTH-002 | `/dashboard` | Required | Owner | Dashboard | DashboardSummary | Independent widgets | Dashboard boundary | Yes |
| R-AUTH-003 | `/repositories` | Required | Owner | Repositories | RepositoryList | Paginated query | Repository boundary | Yes |
| R-AUTH-004 | `/repositories/:repositoryId` | Required | Owner | Repositories | RepositoryDetail | Detail prefetch | Repository boundary | Yes |
| R-AUTH-005 | `/repositories/:repositoryId/snapshots` | Required | Owner | Repository Snapshots | SnapshotList | Paginated query | Snapshot boundary | Yes |
| R-AUTH-006 | `/repositories/:repositoryId/snapshots/:snapshotId` | Required | Owner | Repository Snapshots | SnapshotDetail | Detail query | Snapshot boundary | Yes |
| R-AUTH-007 | `/analyses` | Required | Owner | Analysis | AnalysisHistory | Paginated query | Analysis boundary | Yes |
| R-AUTH-008 | `/analyses/:analysisId` | Required | Owner | Analysis | AnalysisResult | Detail query | Analysis boundary | Yes |
| R-AUTH-009 | `/skills` | Required | Owner | Skill Matrix | SkillMatrix | Current matrix query | Skill boundary | Yes |
| R-AUTH-010 | `/skills/:skillId` | Required | Owner | Skill Matrix | SkillDetail, Evidence | Detail query | Skill boundary | Yes |
| R-AUTH-011 | `/careers` | Required | Authenticated | Career Readiness | CareerList | Reference query | Career boundary | Yes |
| R-AUTH-012 | `/career-readiness` | Required | Owner | Career Readiness | CareerReadiness | Current readiness query | Career boundary | Yes |
| R-AUTH-013 | `/companies` | Required | Authenticated | Company Readiness | CompanyList | Reference query | Company boundary | Yes |
| R-AUTH-014 | `/company-readiness` | Required | Owner | Company Readiness | CompanyReadiness | Current readiness query | Company boundary | Yes |
| R-AUTH-015 | `/recommendations` | Required | Owner | Recommendations | RecommendationList | Current/history tabs | Recommendation boundary | Yes |
| R-AUTH-016 | `/recommendations/:recommendationId` | Required | Owner | Recommendations | RecommendationDetail, Evidence | Detail query | Recommendation boundary | Yes |
| R-AUTH-017 | `/roadmap` | Required | Owner | Learning Roadmap | ActiveRoadmap | Detail query | Roadmap boundary | Yes |
| R-AUTH-018 | `/knowledge` | Required | Owner | Knowledge | KnowledgeDocuments | Paginated query | Knowledge boundary | Yes |
| R-AUTH-019 | `/knowledge/search` | Required | Owner | Knowledge | KnowledgeSearch | Search query | Knowledge boundary | Yes |
| R-AUTH-020 | `/knowledge/:documentId` | Required | Owner | Knowledge | KnowledgeDocument | Detail query | Knowledge boundary | Yes |
| R-AUTH-021 | `/ai-tools` | Required | Owner | AI Generation | Generation schemas/options | Lazy task forms | AI boundary | Yes |
| R-AUTH-022 | `/generation-jobs/:jobId` | Required | Owner | AI Generation | GenerationJob | Polling | Job boundary | Yes |
| R-AUTH-023 | `/artifacts` | Required | Owner | Generated Artifacts | ArtifactList | Paginated query | Artifact boundary | Yes |
| R-AUTH-024 | `/artifacts/:artifactId` | Required | Owner | Generated Artifacts | ArtifactDetail | Detail query | Artifact boundary | Yes |
| R-AUTH-025 | `/portfolio` | Required | Owner | Portfolio | PortfolioList/current | Lazy editor | Portfolio boundary | Yes |
| R-AUTH-026 | `/portfolio/:portfolioId` | Required/public if published | Owner/public token | Portfolio | Portfolio | Detail/editor query | Portfolio boundary | Yes |
| R-AUTH-027 | `/resume` | Required | Owner | Resume | ResumeList/current | Lazy editor | Resume boundary | Yes |
| R-AUTH-028 | `/resume/:resumeId` | Required | Owner | Resume | Resume | Detail/editor query | Resume boundary | Yes |
| R-AUTH-029 | `/interview` | Required | Owner | Interview Practice | QuestionSetList | Paginated query | Interview boundary | Yes |
| R-AUTH-030 | `/interview/:questionSetId` | Required | Owner | Interview Practice | QuestionSet | Detail query | Interview boundary | Yes |
| R-AUTH-031 | `/settings/profile` | Required | Self | User Profile | UserProfile | Form query | Settings boundary | Yes |
| R-AUTH-032 | `/settings/integrations` | Required | Self | Integrations | Connections | Query | Settings boundary | Yes |
| R-AUTH-033 | `/settings/privacy` | Required | Self | Settings | Settings | Form query | Settings boundary | Yes |
| R-ADM-001 | `/admin/users` | Required | Admin | Administration | Admin user/support resources | Paginated query | Admin boundary | Yes |
| R-ADM-002 | `/admin/rules` | Required | Admin | Administration | Rule metadata | Paginated query | Admin boundary | Yes |
| R-ADM-003 | `/admin/careers` | Required | Admin | Administration | Career config | Query | Admin boundary | Yes |
| R-ADM-004 | `/admin/companies` | Required | Admin | Administration | Company config | Query | Admin boundary | Yes |
| R-ADM-005 | `/admin/prompt-templates` | Required | Admin | Administration | Prompt metadata | Query | Admin boundary | Yes |
| R-ADM-006 | `/admin/providers` | Required | Admin | Administration | Provider status | Query | Admin boundary | Yes |
| R-ADM-007 | `/admin/jobs` | Required | Admin | Administration | Job inspection | Paginated query | Admin boundary | Yes |
| R-ADM-008 | `/admin/audit` | Required | Privileged admin | Administration | Audit records | Paginated query | Admin boundary | Yes |

### 6.2 Navigation Rules

| Navigation Type | Rule |
|---|---|
| Global navigation | Shows Dashboard, Repositories, Skills, Recommendations, Roadmap, Knowledge, Artifacts, Portfolio, Resume, Interview, Settings. |
| Contextual navigation | Repository, analysis, artifact, portfolio, resume, and admin pages provide resource-specific tabs. |
| Breadcrumbs | Required for nested resources and immutable historical detail pages. |
| Mobile navigation | Collapses global navigation into accessible menu with current route context. |
| Admin navigation | Separated from user workspace and visible only to authorized administrators. |

## 7. View Catalog

| View | Primary User Goal | Displayed Resources | Actions | Editable Fields | Read-only Fields | Loading | Empty | Partial | Error | Auth Behavior | Responsive | Accessibility |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Dashboard | Understand current career state quickly. | DashboardSummary widgets. | Navigate, refresh widgets. | Widget preference if supported. | Scores, readiness, summaries. | Independent widget skeletons. | Setup prompts. | Per-widget fallback. | Widget-level error. | Owner only. | Card stack on mobile. | Landmarks and heading hierarchy. |
| Repository List | Select and manage repositories. | RepositoryList. | Import, sync, archive. | Filters. | Repository metadata. | Table/card skeleton. | Connect GitHub/import prompt. | Some repos failed sync. | Inline/provider error. | Owner. | Cards on mobile. | Sort/filter controls labeled. |
| Repository Detail | Inspect repository state. | RepositoryDetail, sync status. | Sync, archive, view snapshots. | Archive reason. | Current metadata. | Detail skeleton. | No snapshots prompt. | Missing optional stats. | Resource boundary. | Owner. | Tabs collapse. | Status text beyond color. |
| Repository Synchronization | Track sync progress. | RepositorySyncJob. | Cancel/retry if allowed. | None. | Job phases. | Polling state. | No active job. | Partial completion. | Failed job panel. | Owner. | Progress stack. | Live region for status. |
| Analysis Progress | Track analysis job. | AnalysisJob. | Cancel/retry if allowed. | None. | Job phases. | Polling. | No job. | Stage failure. | Job error panel. | Owner. | Compact timeline. | Progress announced. |
| Analysis Result | Review deterministic analysis. | AnalysisResult, RuleEvaluation, evidence. | View evidence, compare. | None. | Scores and evidence. | Result skeleton. | No completed analysis. | Missing optional sections. | Safe error. | Owner. | Sections stacked. | Score text equivalents. |
| Skill Matrix | Understand skills. | SkillMatrix. | Filter, compare, open skill. | Filters only. | Skill levels, confidence. | Matrix skeleton. | No SkillMatrix. | Some evidence unavailable. | Inline error. | Owner. | Grid to list. | Table alternative and labels. |
| Skill Detail | Inspect one skill. | SkillDetail, evidence. | Filter evidence. | None. | Skill score/level/evidence. | Detail skeleton. | No evidence. | Some sources unavailable. | Inline error. | Owner. | Single-column. | Evidence list semantic grouping. |
| Career Readiness | Understand target-career fit. | CareerReadiness, gaps. | Change target, view gaps. | Target preference. | Readiness result. | Summary skeleton. | Select career prompt. | Company missing okay. | Inline error. | Owner. | Cards stack. | Clear labels for readiness. |
| Company Readiness | Understand target-company fit. | CompanyReadiness. | Change company, compare. | Target preference. | Readiness result. | Summary skeleton. | Select company prompt. | Missing optional company data. | Inline error. | Owner. | Cards stack. | Non-color status indicators. |
| Recommendations | Choose actions. | RecommendationList. | Accept, dismiss, complete. | User decision state. | Priority/reason basis. | List skeleton. | No active recommendations. | Some evidence unavailable. | Inline error. | Owner. | Cards. | Buttons descriptive. |
| Learning Roadmap | Track learning progress. | Roadmap, steps, resources. | Complete, skip, note, archive. | Notes, progress, dates. | Generated reasoning/order basis. | Timeline skeleton. | Create roadmap prompt. | Resource missing. | Conflict handling. | Owner. | Timeline to list. | Keyboard timeline navigation. |
| Knowledge Library | Manage knowledge. | Documents, ingestion jobs. | Upload/import/archive/reindex. | Metadata where allowed. | Source/version facts. | List skeleton. | Import prompt. | Some docs stale. | Provider/index error. | Owner. | Cards. | File status accessible. |
| Knowledge Search | Find source knowledge. | SearchResult, chunks. | Search/filter/open source. | Query/filter. | Relevance/source refs. | Search loading. | No results. | Some sources excluded. | Retrieval error. | Owner. | Results list. | Search form labels. |
| AI Generation | Request generation. | Prompt options, source refs. | Select sources, generate. | Task options. | Deterministic source refs. | Lazy task panels. | No eligible sources. | Some sources stale. | Validation error. | Owner. | Stepper. | Warnings announced. |
| Generation Progress | Track AI job. | GenerationJob. | Cancel/retry. | None. | Job status/validation. | Polling. | No job. | Validation warnings. | AI failure panel. | Owner. | Compact status. | Live region. |
| Artifact Detail | Review generated output. | GeneratedArtifact. | Approve/archive/export/regenerate. | Review state, user notes. | Source refs, validation. | Detail skeleton. | Artifact unavailable. | Warning state. | Validation failure. | Owner. | Version selector. | Generated label text. |
| Portfolio Editor | Build portfolio. | Portfolio, versions, evidence. | Edit sections, publish, export. | Draft sections. | Published versions/source refs. | Editor skeleton. | Create prompt. | Some evidence missing. | Conflict/error panel. | Owner/public read if published. | Editor stacks. | Accessible editor controls. |
| Resume Editor | Build resume. | Resume, versions, sources. | Edit, export, archive. | Draft/user-authored fields. | Source-derived facts. | Editor skeleton. | Create prompt. | Some sources missing. | Conflict/error panel. | Owner. | Editor stacks. | Section headings. |
| Interview Practice | Practice questions. | QuestionSet, questions, feedback jobs. | Answer, request feedback, archive. | Practice answer. | Question/source context. | Question skeleton. | Generate prompt. | Feedback pending. | Feedback failure. | Owner. | Single-question mobile. | Form labels and focus. |
| User Settings | Manage profile/privacy. | Profile, settings, connections. | Update, disconnect, deletion request. | Settings/profile. | Connection status. | Form skeleton. | n/a | Provider partial failures. | Validation errors. | Self. | Form stack. | Error focus. |
| Administration | Manage config and inspect. | Admin configs, jobs, audit. | Activate/deprecate, inspect. | Config drafts where supported. | Audit records. | Table skeleton. | No records. | Provider status degraded. | Admin error boundary. | Admin. | Dense tables adapt. | Table summaries. |

## 8. Component and Design System Architecture

### 8.1 Component Categories

| Category | Purpose | Owner | Reuse Rules |
|---|---|---|---|
| Design tokens | Shared color, spacing, typography, motion, z-index, breakpoints. | Shared UI | No domain meaning encoded. |
| UI primitives | Buttons, links, inputs, dialogs, tabs, tooltips. | Shared UI | Accessible by default and domain-neutral. |
| Form components | Field, validation summary, file picker, rich text field wrapper. | Shared UI/Forms | Do not embed feature business validation. |
| Feedback components | Alert, toast, banner, inline error, empty state, skeleton. | Shared UI/Feedback | Critical failures not toast-only. |
| Navigation components | Shell nav, breadcrumbs, mobile menu, tabs. | Platform/Shared UI | Route-aware, permission-aware display only. |
| Layout components | Page, panel, split view, responsive grid. | Shared UI/Layout | No data fetching. |
| Data-display components | Tables, cards, metadata lists, version selectors. | Shared UI | Accept view models, not raw API responses. |
| Chart components | Graphs, trend charts, distribution charts. | Shared UI/Charts | Must provide textual alternatives. |
| Domain presentation components | ScoreDisplay, EvidenceList, SkillMatrixGrid, etc. | Shared UI + owning feature | May encode presentation semantics, not calculations. |
| Feature composition components | Route-level feature screens. | Feature modules | May orchestrate queries and mutations. |

### 8.2 Semantic Visual States

| State | Required Treatment |
|---|---|
| Success | Clear confirmation and next action. |
| Warning | Non-blocking issue with explanation and remediation. |
| Error | Recoverable explanation and action where possible. |
| Progress | State name, phase, and no fabricated percentages. |
| Deterministic result | Label as rule/career/company engine result and show version/evidence. |
| AI-generated output | Label as generated, show validation/source references, avoid authoritative styling. |
| Archived resource | Visually muted with archived label and limited actions. |
| Immutable historical resource | Read-only badge and version/snapshot reference. |

### 8.3 Domain Presentation Components

| Component | Responsibility | Must Not |
|---|---|---|
| RepositoryCard | Summarize repository status and actions. | Compute analysis state from raw facts. |
| RepositoryStatus | Show sync/archive/permission status. | Hide backend errors silently. |
| SnapshotVersion | Show immutable snapshot version/captured time. | Offer editing. |
| AnalysisStatus | Show analysis job/result state. | Invent progress. |
| ScoreDisplay | Render official score and scale. | Calculate score. |
| EvidenceList | Show evidence references and source links. | Fabricate evidence. |
| SkillMatrixGrid | Present skill assessments. | Edit skill levels. |
| ReadinessSummary | Present career/company readiness. | Calculate readiness. |
| SkillGapList | Present gaps and links. | Determine gap priority. |
| RecommendationCard | Show recommendation, priority, evidence, status actions. | Recompute priority. |
| RoadmapTimeline | Present roadmap steps and progress. | Rewrite generated order basis. |
| KnowledgeSourceCard | Show document/source/index state. | Expose raw embeddings. |
| GenerationStatus | Show AI job/validation state. | Hide rejection reasons. |
| ArtifactVersionSelector | Select generated/published versions. | Mutate published versions. |
| JobProgress | Show common job state. | Display unsupported percentages. |

## 9. State Management Architecture

### 9.1 State Categories

| State Category | Examples | Owner | Lifetime | Persistence | Invalidation | Sharing | Synchronization |
|---|---|---|---|---|---|---|---|
| Server State | Repositories, snapshots, analyses, SkillMatrices, readiness, recommendations, roadmaps, knowledge documents, jobs, artifacts. | Data Access/feature query layer | API-defined | Server-state cache only | API mutations, job completion, stale time, route changes | Across routes through cache | Refetch, polling, background refresh |
| Client State | Modal state, selected tabs, local filters, temporary form state, editor draft state. | Feature or component | View/session lifetime | Memory or browser storage only when safe | Route exit, submit, reset | Local by default | No server sync unless submitted |
| Session State | Authenticated user view, permissions, locale, theme, enabled features. | Platform Core | Session lifetime | User view in memory/server-state cache; safe preferences only in browser storage | Login/logout/session expiry/settings change | App-wide | Restore from `GET /api/v1/users/me` |

### 9.2 State Rules

| Rule | Requirement |
|---|---|
| Server state | MUST NOT be stored in a generic global store without cache policy. |
| Client state | SHOULD remain closest to the owning feature or component. |
| Session state | MUST avoid provider credentials and hidden prompt content. |
| Authentication credentials | MUST NOT be read, copied, persisted, or synthesized by frontend code; the browser transports the HttpOnly session cookie. |
| Draft state | MAY use safe local persistence only for user-authored content, not secrets or private source dumps. |
| Derived display state | SHOULD be computed in view models from API data without changing business meaning. |

## 10. Server-State and Cache Strategy

### 10.1 Cache Behavior

| Resource Group | Cache Lifetime | Stale Policy | Refetch Trigger | Optimistic Update | Notes |
|---|---|---|---|---|---|
| Current user/session | Short/session | Refetch on focus or auth events | Login/logout/session expiry | No | `GET /api/v1/users/me` is authoritative; request includes browser credentials. |
| Repositories | Short-medium | Stale after sync events | Sync/archive/import | Archive UI may be pessimistic | Sync status changes often. |
| Snapshots | Long | Immutable once loaded | New snapshot event | No | Historical snapshots read-only. |
| Analysis results | Long | Immutable after completion | New analysis completion | No | Deterministic read-only. |
| SkillMatrix | Medium-long | Historical immutable; current may change by new matrix | SkillMatrixGenerated | No | No direct edits. |
| Readiness | Medium-long | Immutable per result; current changes by new assessment | Career/company target or matrix change | No | Deterministic read-only. |
| Recommendations | Medium | Status can change | Accept/dismiss/complete/generate | Limited for user action state only | Priority immutable. |
| Roadmaps | Medium | Progress mutable | Step updates | Allowed for progress with rollback | Generated reasoning immutable. |
| Knowledge documents | Medium | Stale if ingestion/reindex changes | Ingestion/reindex/archive | No for indexing | Search may refetch. |
| Jobs | Very short | Poll while active | Status interval | No | Common job model. |
| Generated artifacts | Medium-long | Immutable versions; review state mutable | Generation/review/archive | No for generated content | Review actions can refetch. |
| Dashboard | Short | Projection may be stale | Domain event/refetch interval | No | Show freshness. |

### 10.2 Cache and Request Rules

| Concern | Rule |
|---|---|
| Request deduplication | Concurrent identical queries SHOULD share one in-flight request. |
| Background refresh | SHOULD be used for dashboard and status views without disrupting reading. |
| Polling | MUST use common job state and stop on terminal states. |
| Cancellation | Route changes SHOULD cancel obsolete requests where safe. |
| Mutation rollback | Required for optimistic user-editable state only. |
| Pagination | Cursor pagination SHOULD be used for large collections. |
| Forbidden optimistic updates | Rule results, career readiness, company readiness, analysis completion, repository snapshots, generated artifacts, immutable history. |

## 11. API Client and Contract Mapping

### 11.1 API Client Architecture

| Concern | Rule |
|---|---|
| Authentication header injection | Centralized in data access layer. |
| API version | Default `/api/v1` and version metadata handled centrally. |
| Correlation ID | Generated or propagated per request and job flow. |
| Locale/timezone | Sent from platform session preferences where available. |
| Timeout | Configured by operation category. |
| Retry | Retry only safe or explicitly retryable operations. |
| Cancellation | Supported for obsolete route queries and user-cancelled jobs. |
| Idempotency keys | Generated for required command operations. |
| Uploads/downloads | Use dedicated upload/export contracts; no binary JSON embedding. |
| Error normalization | Map API error contract to UI error model. |
| Response validation | Validate response shape before converting to view models. |

### 11.2 Contract Mapping

| API Schema | Owning Feature | Query/Mutation | Frontend View Model | Cache Policy | Invalidation | Sensitive Fields |
|---|---|---|---|---|---|---|
| UserResponse | Authentication/Profile | Query | CurrentUserVM | Session cache | Login/logout/profile update | Email/user identity |
| RepositorySummaryResponse | Repositories | Query | RepositoryCardVM | Short-medium | Sync/import/archive | Visibility/private metadata |
| RepositorySnapshotResponse | Repository Snapshots | Query | SnapshotVersionVM | Long immutable | New snapshot list only | Source refs |
| AnalysisResultResponse | Analysis | Query | AnalysisResultVM | Long immutable | New analysis | Evidence/source refs |
| SkillMatrixResponse | Skill Matrix | Query | SkillMatrixVM | Medium-long | New matrix | Skill/evidence data |
| CareerReadinessResponse | Career Readiness | Query | ReadinessSummaryVM | Medium-long | Target/matrix change | User career data |
| CompanyReadinessResponse | Company Readiness | Query | CompanyReadinessVM | Medium-long | Target/matrix change | User target company |
| RecommendationResponse | Recommendations | Query/mutation | RecommendationCardVM | Medium | Status/generate | Evidence/gap refs |
| LearningRoadmapResponse | Learning Roadmap | Query/mutation | RoadmapTimelineVM | Medium | Step update | User progress |
| KnowledgeSearchResponse | Knowledge | Mutation-like query | KnowledgeSearchResultVM | Very short | KnowledgeUpdated | Excerpts/source refs |
| PromptContextResponse | AI Generation | Mutation/query | PromptContextStatusVM | Immutable after create | None except status validation | Source refs/prompt metadata |
| GenerationJobResponse | AI Generation | Query/poll | GenerationProgressVM | Very short | Job terminal | Generation metadata |
| GeneratedArtifactResponse | Generated Artifacts | Query/mutation | ArtifactDetailVM | Medium-long | Review/archive/generate | Generated content |
| PortfolioResponse | Portfolio | Query/mutation | PortfolioEditorVM | Medium | Edit/publish/export | User/generated content |
| ResumeResponse | Resume | Query/mutation | ResumeEditorVM | Medium | Edit/export | Personal content |
| InterviewQuestionSetResponse | Interview | Query/mutation | InterviewPracticeVM | Medium | Practice/feedback | User answers |
| DashboardSummaryResponse | Dashboard | Query | DashboardVM | Short | Any domain event/refetch | Aggregated private state |

Components MUST depend on frontend view models, not raw API response objects.

## 12. Authentication and Authorization UX

### 12.1 Authentication Behavior

| Scenario | Frontend Behavior |
|---|---|
| Login | Navigate to the backend-owned GitHub OAuth2 Login initiation route; do not process or store provider tokens. |
| Logout | Submit the backend logout request with credentials and CSRF protection, then clear session view state and every private server-state cache. |
| Session restoration | Call `GET /api/v1/users/me` with credentials included and show a neutral loading state until `200` or `401`. |
| Session expiration | On `401`, clear private caches and redirect to login or show session-expired state. |
| Authorization denial | On `403`, preserve authenticated state and show access denied; on privacy-preserving `404`, do not reveal resource existence. |
| Session renewal failure | Clear private caches and ask the user to reauthenticate; frontend code never handles a refresh token. |
| OAuth redirect/callback | Treat the callback as backend-owned, then bootstrap current user and show provider-safe success/failure state. |
| Disconnected provider | Show disconnected state and reconnect action where supported. |
| Revoked permissions | Show permission revoked state and disable dependent actions. |
| Unauthorized route | Show access-denied or not-found according to API response safety. |
| Account deletion | Show irreversible-impact confirmation and async deletion job state. |

### 12.2 Authorization-Aware UI

| Actor | UI Behavior |
|---|---|
| Resource owner | Full owner actions for supported resource states. |
| Administrator | Admin routes and privileged actions visible after backend-confirmed permissions. |
| Future organization member | Reserved; no current UI assumptions beyond extension points. |
| Future organization administrator | Reserved; admin UI should be extensible. |
| Read-only viewer | May view explicitly shared published resources only. |

Actions may be visible, hidden, disabled, or backend-validated. UI visibility MUST NOT be treated as security enforcement.

## 13. Onboarding Flow

### 13.1 Flow Steps

| Step | Required | Backend Resource | Resumability | Skipped Behavior | Failure Recovery |
|---|---:|---|---|---|---|
| Account Creation | Yes | User | Session restore | Not skippable | Retry login |
| Career Target Selection | Recommended | Career preference | Persist preference | Can skip with limited readiness | Set later |
| Company Target Selection | Optional | Company preference | Persist preference | Career readiness still works | Set later |
| GitHub Connection | Required for repository analysis | GitHubConnection | Connection state persisted | Can explore limited app | Reconnect |
| Repository Selection | Required for initial analysis | Repository import | Persist selected repositories | Can import later | Retry import |
| Initial Synchronization | Required for analysis | Sync job | Pollable job | Analysis unavailable | Retry sync |
| Initial Analysis | Recommended | Analysis job | Pollable job | Dashboard partial | Retry analysis |
| Dashboard Ready | Result state | DashboardSummary | Projection loads | Shows setup cards | Refresh/retry widgets |

AI generation is not mandatory for onboarding.

### 13.2 Incomplete Onboarding State

The frontend SHOULD present progressive setup cards rather than blocking the entire workspace. Missing career, company, GitHub, repository, sync, or analysis state should map to clear next actions.

## 14. Repository and Synchronization UX

| Area | UX Rule |
|---|---|
| Repository list | Show sync status, visibility, last sync time, archive state, and primary actions. |
| Repository registration | Use provider-accessible repository list and avoid raw provider implementation details. |
| Connection status | Show active, revoked, disconnected, and permission-limited states. |
| Sync request | Use idempotent action and show job status. |
| Sync progress | Show `queued`, `running`, `succeeded`, `failed`, `cancelled`, `expired`; do not fabricate percentages. |
| Full/incremental sync | Explain mode when backend provides it; do not infer from client. |
| Partial failure | Show completed and failed portions when API provides safe detail. |
| Failed sync | Provide retry and provider-safe reason. |
| Latest snapshot | Show latest ready snapshot and captured timestamp. |
| Historical snapshots | Provide immutable list/detail with version labels. |
| Archive/restore | Confirm action and show effect on future sync/analysis. |

RepositorySnapshot views MUST be read-only.

## 15. Analysis and Skill UX

### 15.1 Analysis Flow

| Step | UX Requirement |
|---|---|
| Analysis Request | User selects supported repository/snapshot references; no final score input. |
| Job Status | Common async job UI with polling and retry/cancel where supported. |
| Analysis Result | Show deterministic-result label, RuleSetVersion, timestamp, and source snapshot. |
| Evidence | Make evidence references discoverable from score breakdowns and skill details. |
| Skill Matrix | Show categories, levels, confidence/evidence indicators, and immutable history. |
| Career/Company Readiness | Link from Skill Matrix to readiness results. |

### 15.2 Skill Matrix UX

| Concern | Rule |
|---|---|
| Category grouping | Group by language, framework, database, architecture, testing, DevOps, documentation, collaboration, and other Rule Engine categories. |
| Level display | Use labels and explanations from backend-provided scales. |
| Confidence/evidence | Show confidence or evidence availability without recalculating. |
| Filtering | Support category, strength/weakness, technology, evidence availability. |
| Historical comparison | Compare immutable matrices by backend-provided values only. |
| Mobile layout | Transform matrix grid into accessible list/cards. |
| Accessible representation | Provide table semantics or textual summary. |

Direct editing of calculated skill levels is prohibited.

## 16. Career, Company, Recommendation, and Roadmap UX

### 16.1 Readiness Presentation

| Element | UX Rule |
|---|---|
| Readiness summary | Display deterministic readiness label, version, and source SkillMatrix. |
| Category breakdown | Use backend-provided breakdown only. |
| Skill gaps | Link each gap to skill detail and evidence where available. |
| Evidence | Provide drill-down from readiness/gap to evidence references. |
| Historical comparison | Compare versioned readiness results without recalculation. |
| Target selection | Preferences are user-editable; past results remain historical. |
| Result version | Show CareerProfileVersion or CompanyProfileVersion metadata where user-safe. |

### 16.2 Concept Separation

| Concept | UI Treatment |
|---|---|
| Deterministic score | Official result badge, evidence link, read-only. |
| AI explanation | Generated label, validation/source references, non-authoritative styling. |
| User preference | Editable target selection controls. |
| Recommendation | Deterministic action with status and evidence/gap basis. |

### 16.3 Recommendation and Roadmap States

| Resource | States | User Editable |
|---|---|---|
| Recommendation | active, accepted, dismissed, completed, archived | Status actions only. |
| Roadmap | active, completed, archived | Progress, notes, optional dates. |
| RoadmapStep | not started, in progress, completed, skipped | Progress and notes. |

Generated reasoning, priority, source references, and ordering basis are immutable.

## 17. Knowledge UX

| Area | UX Rule |
|---|---|
| Knowledge library | Show documents, source type, freshness, indexing state, privacy class where useful. |
| Source import | Support upload and Notion import through API contracts. |
| Ingestion status | Use common job UX and document state. |
| Document detail | Show metadata, versions, chunks summary, source references. |
| Semantic search | Show query, filters, relevance, excerpt, source reference, and grounding reference. |
| Source filtering | Support source type, repository/project, freshness, tags, career/company where API supports. |
| Relevance display | Use normalized relevance from API; do not expose raw vector values. |
| Archive | Confirm archive and explain retrieval impact. |
| Re-index | Show job status and reindex reason. |

Raw embeddings MUST NOT be exposed.

## 18. AI Generation and Artifact UX

### 18.1 Generic Generation UX

| Task | UX Entry |
|---|---|
| Repository review | Repository or AI tools flow with repository/snapshot source context. |
| Career coaching | Career readiness or AI tools flow. |
| Portfolio | Portfolio editor generation action. |
| Resume | Resume editor generation action. |
| README | Repository artifact action. |
| Interview questions | Interview practice generation flow. |
| Learning recommendations | Roadmap/recommendation explanation action. |
| Technology recommendations | Skill/technology detail action. |
| Architecture review | Repository/analysis action. |

### 18.2 Generation Contract UX

| Concern | Rule |
|---|---|
| Source-context selection | User selects eligible references; UI shows stale/missing source warnings. |
| Generation request | Uses idempotency and job contract. |
| Job progress | Uses common states; no fabricated percentages. |
| Cancellation/retry | Available only when API job indicates support. |
| Validation warnings | Display before user treats output as ready. |
| Evidence/grounding | Provide source links and grounding references. |
| Generated-content label | Always visible on AI-generated text/artifacts. |
| Feedback | Captures user feedback without changing deterministic results. |
| Regeneration | Creates new version or artifact; does not overwrite published versions. |
| Export | Uses export job and temporary download URL contract. |

### 18.3 Content Separation

| Content Type | UI Treatment |
|---|---|
| Deterministic source data | Official/read-only styling and evidence links. |
| System-derived data | Engine-result labels and version references. |
| User-authored content | Editable user-content labels where relevant. |
| AI-generated content | Generated label, validation status, source refs, non-authoritative disclaimer. |

AI-generated content MUST NOT be presented as authoritative fact.

## 19. Portfolio, Resume, and Interview UX

### 19.1 Portfolio

| Area | UX Rule |
|---|---|
| Creation | Start from empty draft or generation flow. |
| Project selection | Select source projects/evidence; show missing evidence warnings. |
| Generation | Create generation job and artifact version. |
| Section editing | Allow user-authored edits while preserving provenance. |
| Source references | Show evidence/source links per generated section where available. |
| Review | Require review state before publication where policy demands. |
| Publication | Published versions are immutable. |
| Version history | Show generated, edited, published, and archived versions. |
| Export | Use async export job. |
| Archive | Confirm and explain public/private access impact. |

### 19.2 Resume

| Area | UX Rule |
|---|---|
| Targeting | Career and company target may influence generated draft but not fabricate facts. |
| Fact separation | User-authored facts, system-derived facts, and AI writing are visually distinct. |
| Editing | AI text must not silently overwrite user-authored facts. |
| Version history | Preserve source references and generated/user edit provenance. |
| Export | Use export job and show expiration. |
| Archive | Keep historical state according to API. |

### 19.3 Interview Practice

| Area | UX Rule |
|---|---|
| Question-set generation | Uses career/company/skill context and job UX. |
| Question navigation | Supports keyboard and mobile-friendly navigation. |
| Answer submission | User-authored draft and submitted answer are distinct. |
| Feedback request | Feedback is AI-generated unless future deterministic rubric exists. |
| Feedback display | Must not be presented as objective hiring decision. |
| Source context | Show career/company/skill context used for generation. |
| History/archive | Allow listing and archiving prior sets. |

## 20. Dashboard Architecture

| Section | Source | Loading | Partial Failure | Empty State | Source Links | Cache Policy |
|---|---|---|---|---|---|---|
| Repository status | Dashboard/repository APIs | Independent widget | Show sync unavailable | Connect/import prompt | Repositories | Short |
| Latest analysis | Dashboard/analysis APIs | Independent widget | Show stale/failed analysis | Request analysis prompt | Analysis detail | Short |
| Skill summary | Dashboard/SkillMatrix | Independent widget | Show current matrix unavailable | No matrix prompt | Skills | Short-medium |
| Career readiness | Dashboard/career | Independent widget | Company missing not fatal | Select career prompt | Career readiness | Short-medium |
| Company readiness | Dashboard/company | Independent widget | Show target missing/degraded | Select company prompt | Company readiness | Short-medium |
| Active roadmap | Dashboard/roadmap | Independent widget | Show roadmap unavailable | Create roadmap prompt | Roadmap | Short |
| Recommendations | Dashboard/recommendations | Independent widget | Show partial list | No active recs | Recommendations | Short |
| Recent artifacts | Dashboard/artifacts | Independent widget | Show artifact history unavailable | Generate prompt | Artifacts | Short |
| Active jobs | Job APIs | Polling widget | Failed jobs visible | No active jobs | Job detail | Very short |
| Sync alerts | Sync/job APIs | Independent widget | Show provider degraded | No alerts | Repository settings | Short |

Dashboard data remains a read projection and MUST NOT be used as source of truth for mutations.

## 21. Asynchronous Job UX

### 21.1 Common Job States

| State | UX Treatment |
|---|---|
| queued | Show accepted state and pending phase. |
| running | Show current phase and safe progress details. |
| succeeded | Show success and result link. |
| failed | Show user-safe reason, retry availability, and support link if needed. |
| cancelled | Show cancellation state and possible restart action. |
| expired | Show expiration and whether result/history remains available. |

### 21.2 Job Type UX

| Job Type | Polling | Navigation Away | Cancellation | Retry | Result Access | Notification |
|---|---|---|---|---|---|---|
| Repository synchronization | Yes | Continues in background | If API supports | Yes | Repository/snapshot | Completion/failure |
| Analysis | Yes | Continues | If API supports | Yes | Analysis/SkillMatrix | Completion/failure |
| Knowledge ingestion | Yes | Continues | If API supports | Yes | KnowledgeDocument | Completion/failure |
| Embedding | Usually background | Continues | Usually no | System/user reindex | Document index status | Failure if terminal |
| AI generation | Yes | Continues | If API supports | Yes | GeneratedArtifact | Completion/failure |
| Portfolio generation | Yes | Continues | If API supports | Yes | Portfolio | Completion/failure |
| Resume generation | Yes | Continues | If API supports | Yes | Resume | Completion/failure |
| Export | Yes | Continues | If API supports | Yes | Download/export resource | Completion/failure |

Progress percentages MUST NOT be invented when unavailable.

## 22. Forms, Drafts, and Conflict Handling

### 22.1 Form Architecture

| Concern | Rule |
|---|---|
| Local validation | Validate syntax, required fields, size, and basic format before submit. |
| Backend validation | Display API field errors as authoritative. |
| Submission | Use idempotency for required command operations. |
| Duplicate submission prevention | Disable or guard submit while request is pending. |
| Unsaved changes | Warn before navigation when user-authored draft would be lost. |
| Accessible error focus | Move focus to validation summary or first invalid field. |
| File uploads | Validate type/size locally and show backend scan/ingestion state. |
| Large-text editing | Autosave only where approved; avoid storing sensitive generated/source content unnecessarily. |

### 22.2 Draft Behavior

| Draft Type | Storage | Rule |
|---|---|---|
| Portfolio | Feature editor state; safe local persistence only if approved | Preserve generated/user-authored provenance. |
| Resume | Feature editor state | AI text must not overwrite user facts silently. |
| Interview answers | Local draft until submitted | User-authored content label. |
| Notes | Local/roadmap state | User-owned and editable. |
| Profile fields | Form state | Backend validation authoritative. |

### 22.3 Conflict Handling

| Conflict | UX Response |
|---|---|
| Stale updates | Show refresh/merge/retry options where applicable. |
| Concurrent roadmap updates | Use latest state and explain conflict. |
| Artifact version conflicts | Require selecting current version before publish. |
| Publication conflicts | Show immutable version rule and refresh. |
| Expired jobs | Show expiration and recreate option. |
| Target changes during generation | Warn that generated output references earlier source context. |

## 23. Loading, Empty, Error, and Notification Patterns

### 23.1 Shared State Patterns

| Pattern | Rule |
|---|---|
| Initial loading | Use skeleton or progress where layout is known. |
| Background refetch | Keep prior content visible with subtle updating indicator. |
| Empty collection | Explain why empty and provide next action. |
| No search results | Show query/filter reset suggestions. |
| Partial data | Show available sections and explicit unavailable sections. |
| Stale data | Show freshness timestamp and refresh action. |
| Offline state | Show connection issue and preserve safe drafts if possible. |
| Expired resources | Explain expiration and regeneration/retry path. |
| Archived resources | Show archived badge and limited actions. |
| Dependency failure | Show provider-safe reason and retry path. |

### 23.2 Error Communication

| Error | UX Pattern |
|---|---|
| Network/timeout | Inline retry, banner if page-wide. |
| Authentication failure | Session expired flow. |
| Authorization failure | Access denied/not found depending on API. |
| Validation errors | Field errors and validation summary. |
| Conflicts | Conflict panel with refresh/retry. |
| Rate limits | Explain wait/retry timing if provided. |
| Provider failure | Provider-safe banner and retry. |
| Sync/analysis/generation failure | Job failure panel with persisted reason. |
| Internal errors | Safe generic message and request ID. |

Critical failures MUST NOT be communicated by toast only.

## 24. Accessibility and Responsive Design

### 24.1 Accessibility Rules

| Area | Requirement |
|---|---|
| Standard | Target WCAG 2.2 AA. |
| Semantic structure | Use meaningful landmarks, headings, lists, tables, and buttons. |
| Keyboard navigation | All interactive controls operable by keyboard. |
| Focus management | Dialogs, route changes, errors, and async completion manage focus predictably. |
| Screen reader support | Dynamic status updates use appropriate announcements. |
| Contrast | Meet contrast requirements for text and indicators. |
| Reduced motion | Respect reduced-motion preferences. |
| Forms | Labels, descriptions, validation, and error summaries required. |
| Charts | Provide textual summaries and data tables. |
| Status indicators | Do not rely on color alone. |
| Touch targets | Mobile controls have adequate touch area. |
| Zoom | Layout supports browser zoom without content loss. |

### 24.2 Responsive Behavior

| Area | Desktop | Tablet | Mobile |
|---|---|---|---|
| Navigation | Persistent sidebar/top nav | Collapsible nav | Drawer/bottom-friendly nav |
| Tables | Full table | Reduced columns | Cards or stacked rows |
| Charts | Full chart plus summary | Responsive chart | Summary-first with expandable chart |
| Skill Matrix | Grid/table | Condensed grid | List/cards |
| Timelines | Horizontal/vertical | Vertical | Single-column list |
| Editors | Multi-pane | Split/stacked | Single-column with section nav |
| Dialogs | Centered/modal | Modal/fullscreen hybrid | Fullscreen sheet/dialog |

## 25. Performance Architecture

| Area | Strategy | Target Category |
|---|---|---|
| Route-level code splitting | Split large authenticated/admin/editor/AI routes. | Required for major features. |
| Feature bundle boundaries | Feature modules lazy-loaded where not needed at startup. | Recommended. |
| Lazy loading | Editors, charts, admin, AI tools, heavy artifact views. | Required for heavy views. |
| Prefetching | Prefetch likely next route data after user intent. | Opportunistic. |
| Large-list virtualization | Repositories, evidence, knowledge chunks, audit, jobs where large. | Required when list size grows. |
| Image optimization | Use optimized assets and lazy image loading. | Required for artifact previews. |
| Request deduplication | Deduplicate concurrent server-state queries. | Required. |
| Pagination | Use cursor pagination for large collections. | Required. |
| Chart loading | Load chart data independently and provide fallback summary. | Required. |
| Editor loading | Lazy-load rich editors only on editor routes. | Required. |
| Rendering performance | Memoize expensive display transformations only when measured. | Measurement-driven. |
| Cache usage | Apply resource-specific stale policies. | Required. |
| Performance measurement | Track route load, API latency, render errors, and job UX latency. | Required. |

Do not introduce optimization without measurable need.

## 26. Frontend Security, Privacy, and Observability

### 26.1 Security Controls

| Control | Rule |
|---|---|
| Secure session handling | Do not expose token values in logs or telemetry. |
| Cookie session | Frontend JavaScript MUST NOT read or store the opaque session identifier; API requests use `credentials: include` only for trusted configured origins. |
| Browser storage | `localStorage`, `sessionStorage`, IndexedDB, URLs, and application state MUST NOT contain session credentials or provider access/refresh tokens. |
| XSS prevention | Treat generated/user content as untrusted unless sanitized by approved path. |
| Safe HTML rendering | Avoid raw HTML rendering; if required, sanitize and restrict. |
| CSRF expectations | Obtain and return the server-issued CSRF token/header for state-changing requests; SameSite cookies supplement but do not replace server validation. |
| CORS | Credentialed requests are limited to the configured backend origin policy; frontend code must not work around denied origins or use wildcard assumptions. |
| OAuth state validation | Callback UX depends on backend OAuth state validation. |
| Open redirect prevention | Redirect targets must be allowlisted or backend-provided safe paths. |
| File validation | Validate type/size before upload; backend remains authoritative. |
| Temporary URLs | Show expiration and avoid storing unnecessarily. |
| Third-party scripts | Minimize and isolate; do not send private content by default. |
| Sensitive redaction | Redact private source contents, prompts, artifacts, tokens, and secrets from telemetry. |

### 26.2 Privacy UX

| Area | Required UX |
|---|---|
| Connected providers | Show connected/disconnected/revoked state and data usage summary. |
| Repository visibility | Indicate public/private where provided. |
| Knowledge-source visibility | Show source type, privacy class, and retrieval eligibility. |
| Public/private artifacts | Clearly show publication state and access implications. |
| Account deletion | Explain irreversible effects and async processing. |
| Data export | Show export status, temporary URL, and expiration. |
| Provider disconnection | Explain future sync/retrieval impact. |

### 26.3 Observability

| Signal | Rule |
|---|---|
| Route failures | Capture route, request ID, and safe error category. |
| API errors | Capture error code, endpoint category, request ID. |
| Unhandled exceptions | Capture safe stack metadata without private content. |
| Page performance | Capture route load and interaction metrics. |
| Job failures | Capture job type/status/error category. |
| Form failures | Capture validation category, not user-entered private content. |
| Correlation IDs | Preserve across API calls and job flows where available. |
| Accessibility issues | Track detected client-side accessibility failures where tooling allows. |

Private repository contents, prompts, generated artifacts, tokens, and secrets MUST NOT be collected in telemetry by default.

## 27. Testing and Implementation Structure

### 27.1 Testing Expectations

| Test Type | Purpose |
|---|---|
| View-model tests | Verify display transformations do not alter business meaning. |
| Component tests | Verify reusable UI states and accessibility behavior. |
| Feature tests | Verify route-level orchestration and state transitions. |
| API contract tests | Verify frontend expectations match `10_API_Specification.md`. |
| Route tests | Verify auth guards, loading/error boundaries, and deep links. |
| Authentication tests | Verify login, logout, callback, expiration flows. |
| Authorization-aware UI tests | Verify hidden/disabled/read-only behavior without assuming security. |
| Form tests | Verify local validation, backend error mapping, focus behavior. |
| Async job tests | Verify polling, terminal states, retry/cancel, navigation away. |
| Accessibility tests | Verify WCAG-oriented behavior for core routes and components. |
| Responsive tests | Verify major views on desktop/tablet/mobile categories. |
| Visual regression tests | Verify design-system and domain presentation states. |
| End-to-end tests | Verify major user flows with contract-backed APIs. |

### 27.2 Conceptual Source Structure

| Path Concept | Purpose |
|---|---|
| `src/app` | App shell, route registration, global providers. |
| `src/platform` | Session, routing, telemetry, localization, feature flags, browser abstractions. |
| `src/features` | Feature modules with public feature APIs and route composition. |
| `src/shared/ui` | UI primitives and design-system components. |
| `src/shared/forms` | Domain-neutral form infrastructure. |
| `src/shared/feedback` | Alerts, banners, toasts, skeletons, empty states. |
| `src/shared/layout` | Layout primitives and responsive containers. |
| `src/api` | Generated or hand-maintained API contract types, transport schemas, and data-access adapters. |
| `src/assets` | Static assets. |
| `src/tests` | Shared test utilities and contract fixtures. |

### 27.3 Structure Rules

| Rule | Requirement |
|---|---|
| Feature public APIs | Cross-feature usage must go through explicit public exports. |
| Internal boundaries | Feature internals must not be imported directly by other features. |
| Shared components | Shared UI must remain domain-neutral unless explicitly categorized as domain presentation. |
| Generated API contracts | Must be isolated from presentation components through view models. |
| Test placement | Tests should live near feature/module or in shared test areas according to scope. |

No production source code is defined in this document.

## 28. Traceability, Open Issues, and Final Review

### 28.1 API to Frontend Traceability

| API Operation Area | Owning Feature | Route | View | Query/Mutation | Cache Policy | Loading State | Error State | Authorization |
|---|---|---|---|---|---|---|---|---|
| API-ID | Authentication/User Profile/Settings | `/settings/*`, `/login` | Settings/Login | Query/mutation | Session/short | Form/page loading | Field/session errors | Self |
| API-INT | Integrations/Onboarding | `/settings/integrations`, `/onboarding` | Integration Settings | Mutation/job | Short/job | OAuth/job loading | Provider-safe errors | Self/provider permission |
| API-REP | Repositories/Snapshots | `/repositories/*` | Repository views | Query/mutation | Short/immutable snapshot | List/detail/job | Sync/resource errors | Owner |
| API-ANA | Analysis | `/analyses/*` | Analysis views | Query/job | Immutable result/job | Job/result loading | Analysis failure | Owner |
| API-SKL | Skill Matrix | `/skills/*` | Skill views | Query | Medium-long | Matrix skeleton | Evidence/resource errors | Owner |
| API-CAR/CMP | Career/Company | `/career-readiness`, `/company-readiness` | Readiness views | Query/mutation target | Medium-long | Summary skeleton | Missing target/resource error | Owner/auth read |
| API-REC | Recommendations | `/recommendations/*` | Recommendation views | Query/mutation | Medium | List/action pending | Conflict/evidence error | Owner |
| API-LRN | Learning Roadmap | `/roadmap` | Roadmap Timeline | Query/mutation | Medium | Timeline skeleton | Conflict/validation | Owner |
| API-KNW | Knowledge | `/knowledge/*` | Library/Search | Query/mutation/job | Medium/short search | Search/job loading | Retrieval/ingestion error | Owner |
| API-PRM | Prompt | `/ai-tools` | Generation setup | Mutation/query | Immutable context | Context creation | Validation/token error | Owner/admin |
| API-AI | AI Generation/Artifacts | `/ai-tools`, `/artifacts/*` | Generation/Artifact | Mutation/job/query | Job short/artifact medium | Generation progress | Validation/provider error | Owner |
| API-PRT | Portfolio | `/portfolio/*` | Portfolio Editor | Query/mutation/job | Medium | Editor/export loading | Conflict/export error | Owner/public |
| API-RSM | Resume | `/resume/*` | Resume Editor | Query/mutation/job | Medium | Editor/export loading | Conflict/export error | Owner |
| API-ITV | Interview | `/interview/*` | Interview Practice | Query/mutation/job | Medium | Question/feedback loading | Generation/feedback error | Owner |
| API-DSH | Dashboard | `/dashboard` | Dashboard | Query | Short | Widget skeletons | Widget errors | Owner |
| API-ADM | Administration | `/admin/*` | Admin Console | Query/mutation | Short/reference | Table/form loading | Admin/audit errors | Admin |

### 28.2 Requirements to Frontend Traceability

| Requirement Area | Feature | Route or Flow | API Contract | Error Strategy | Accessibility Consideration |
|---|---|---|---|---|---|
| FR-001~FR-020 | Authentication/Profile/Settings | Login, profile, preferences | API-ID | Auth/session/form errors | Form labels and focus |
| FR-021~FR-050 | Integrations/Repositories | GitHub sync flow | API-INT, API-REP | Provider/job errors | Status live regions |
| FR-051~FR-070 | Integrations/Knowledge | Notion import flow | API-INT, API-KNW | Provider/ingestion errors | File/source state text |
| FR-071~FR-180, RR | Analysis/Skill Matrix | Analysis result and skills | API-ANA, API-SKL | Analysis/evidence errors | Score text and table alternatives |
| FR-181~FR-220, CR | Career/Company/Recommendations/Learning | Readiness and roadmap | API-CAR/CMP/REC/LRN | Conflict/readiness errors | Non-color readiness indicators |
| FR-221~FR-280, AI/PR | AI/Artifacts/Portfolio/Resume/Interview | Generation flows | API-PRM, API-AI, artifact APIs | Validation/provider/job errors | Generated labels and warnings |
| FR-281~FR-320 | Dashboard | Dashboard widgets | API-DSH | Widget partial errors | Landmarks, summaries |
| FR-321~FR-340, KR | Knowledge | Library/search | API-KNW | Retrieval/index errors | Search labels and result structure |
| FR-341~FR-360 | Administration | Admin console | API-ADM | Admin/audit errors | Accessible tables |

### 28.3 Open Issues

| Issue ID | Context | Options | Recommendation | Impact | Owner | Status | ADR Candidate |
|---|---|---|---|---|---|---|---|
| FE-OPEN-001 | Frontend framework finalization. | React, other SPA framework. | React with TypeScript accepted by ADR-021. | Tooling and patterns. | Frontend Architecture | Resolved | ADR-021 |
| FE-OPEN-002 | SPA versus SSR. | SPA, SSR, hybrid. | Authenticated SPA accepted by ADR-021. | SEO and hosting. | Frontend/DevOps | Resolved | ADR-021 |
| FE-OPEN-003 | Routing library. | React Router, framework router. | React Router-compatible routing baseline accepted by ADR-021. | Route architecture. | Frontend | Resolved | ADR-021 |
| FE-OPEN-004 | Server-state library. | React Query, SWR, custom. | React Query baseline accepted by ADR-021. | Cache and job polling. | Frontend | Resolved | ADR-021 |
| FE-OPEN-005 | Form library. | Native, React Hook Form, other. | Decide during implementation. | Validation consistency. | Frontend | Open | ADR-FE-005 |
| FE-OPEN-006 | Design-system approach. | Build internal, adopt component library, hybrid. | Hybrid tokens + accessible primitives. | Velocity and consistency. | Design/Frontend | Open | ADR-FE-006 |
| FE-OPEN-007 | Chart library. | Lightweight charts, custom SVG, enterprise chart lib. | Choose after dashboard needs stabilize. | Accessibility/performance. | Frontend/Product | Open | ADR-FE-007 |
| FE-OPEN-008 | Rich-text editor. | Plain text, markdown, rich text editor. | Start simple; upgrade for portfolio/resume needs. | Bundle size/editor UX. | Frontend/Product | Open | ADR-FE-008 |
| FE-OPEN-009 | Streaming generation. | No streaming, validation-safe streaming, staged preview. | No streaming initially. | AI UX and validation. | AI/API/Frontend | Open | ADR-FE-009 |
| FE-OPEN-010 | Mobile strategy. | Responsive web only, PWA, native mobile. | Responsive web first. | Navigation/offline design. | Product | Open | ADR-FE-010 |
| FE-OPEN-011 | Analytics provider. | None, privacy-first analytics, product analytics. | Privacy-first only after review. | Observability/privacy. | Security/Product | Open | ADR-FE-011 |

### 28.4 Final Consistency Review

| Review Item | Status |
|---|---|
| Every user-facing API maps to a feature. | Passed |
| Every feature has one owner. | Passed |
| Business logic is not implemented in components. | Required by architecture |
| Final scores are never calculated in frontend. | Passed |
| Deterministic and AI-generated results are separated. | Passed |
| Immutable resources are read-only. | Passed |
| Evidence is accessible. | Passed |
| Server state and client state are separated. | Passed |
| Authorization is not based only on UI visibility. | Passed |
| Async jobs share one UX model. | Passed |
| Major views define loading, empty, partial, and error states. | Passed |
| Accessibility and responsive behavior are defined. | Passed |
| Sensitive data is excluded from telemetry. | Passed |
| Terminology matches Domain Model. | Passed |
| API contracts match `10_API_Specification.md`. | Passed |
| Backend boundaries match `11_Backend_Architecture.md`. | Passed |
| No unsupported features were introduced. | Passed |

### 28.5 Completion Metrics

| Metric | Count |
|---|---:|
| Feature module count | 24 |
| Major route count | 41 |
| Major view count | 22 |
| API operation coverage summary | All major API operation groups from `10_API_Specification.md` mapped to frontend features |
| Accessibility coverage summary | WCAG 2.2 AA target, semantic structure, keyboard, focus, screen reader, charts, responsive adaptations covered |
| Unresolved issue count | 7 |

### 28.6 Final Completeness Checklist

| Deliverable Requirement | Status |
|---|---|
| Feature catalog complete. | Complete |
| Route catalog complete. | Complete |
| Major view catalog complete. | Complete |
| State model complete. | Complete |
| API integration model complete. | Complete |
| Asynchronous job UX complete. | Complete |
| AI output presentation rules complete. | Complete |
| Accessibility rules complete. | Complete |
| Performance strategy complete. | Complete |
| Security and observability boundaries complete. | Complete |
| Traceability complete. | Complete |
| Final consistency review complete. | Complete |

## 29. Session Foundation Implementation Evidence

| Concern | Actual Path | Status |
|---|---|---|
| Credentialed API client | `frontend/src/shared/api/apiClient.ts` | Implemented |
| Session API | `frontend/src/features/session/api/sessionApi.ts` | Current-user, CSRF, and logout calls implemented |
| Server state | `frontend/src/features/session/model/useSession.ts` | React Query is the sole session-state owner |
| Session UI | `frontend/src/features/session/ui/SessionPanel.tsx` | Loading, anonymous, authenticated, and network-error states implemented |
| Browser credential storage | None | No token or session identifier is stored in browser storage |
| Verification | `node scripts/run-frontend.mjs run test`; `node scripts/run-frontend.mjs run build` | Passed |

The frontend does not call GitHub repository APIs and does not contain dashboard, score, role, or generated-artifact placeholders.
