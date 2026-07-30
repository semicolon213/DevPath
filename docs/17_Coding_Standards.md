# DevPath Coding Standards

## 1. Purpose and Scope

### 1.1 Purpose

This document defines the authoritative coding standards for DevPath. It standardizes how code is written, organized, reviewed, tested, observed, secured, and maintained across backend, frontend, domain, integration, AI, knowledge, worker, scheduler, and support code.

These standards define implementation behavior. They do not define new product requirements, business rules, infrastructure decisions, or technology selections.

### 1.2 Scope

| Codebase Area | Included Scope |
|---|---|
| Backend API | Controllers, application services, DTOs, validation, errors, API contracts |
| Domain Modules | Entities, value objects, aggregates, domain services, events, invariants |
| Rule Engine | Deterministic rule evaluation, score calculation, evidence, versioning |
| Career Path Engine | Career/company readiness, skill gaps, roadmaps, recommendations |
| Persistence | Repository ports/adapters, mappings, transactions, query objects |
| Workers/Schedulers | Jobs, retries, idempotency, event handling, progress, observability |
| Integrations | GitHub, Notion, AI providers, Object Storage, notification adapters |
| Knowledge Pipeline | Collection, normalization, chunking, embedding, indexing, retrieval |
| Prompt/AI Pipeline | Context Builder, Prompt Builder, validators, provider adapters, output formatting |
| Frontend | Project structure, components, state, API client, styling, accessibility |
| Tests | Test naming, fixtures, golden datasets, security tests, AI evaluations |
| Support Code | Scripts, generated code, configuration code, migration support code, shared utilities |

### 1.3 Excluded Topics

This document MUST NOT define complete production source code, framework-specific project templates, final dependency selections, CI/CD pipeline files, infrastructure manifests, SQL migrations, complete API implementations, complete test suites, cloud configuration, product requirements, or new business rules.

### 1.4 Intended Audience

This document applies to human developers, Codex, Claude Code, and other AI coding agents working on DevPath. All implementation contributors MUST follow the same architectural, testing, security, and review standards.

### 1.5 Enforcement Expectations

Standards are enforced through automated checks where practical, architecture tests, contract verification, static analysis, test gates, code review, documentation review, and ADR-controlled exceptions.

## 2. Standards Hierarchy

| Priority | Authority | Rule |
|---:|---|---|
| 1 | SRS and deterministic-engine specifications | Product behavior and deterministic calculations are authoritative |
| 2 | Domain and data architecture | Business concepts, invariants, data ownership, and logical models dominate implementation preference |
| 3 | API contracts | Public API behavior MUST align with `10_API_Specification.md` |
| 4 | Backend and frontend architecture | Module and UI architecture determine allowed boundaries |
| 5 | Security, observability, test, and deployment requirements | Cross-cutting requirements are mandatory |
| 6 | Coding Standards | Implementation conventions apply within the above constraints |
| 7 | Module README and local conventions | Local rules MAY refine but not override higher authorities |
| 8 | Individual implementation preference | Personal preference has the lowest authority |

Deviations MUST be approved and recorded through the exception process in Chapter 47. Local conventions MUST NOT override higher-level documents.

## 3. Repository and Source Layout

The accepted repository strategy is a monorepo governed by ADR-022. Monorepo ownership does not permit frontend and backend source-code imports across boundaries; integration occurs through API contracts and generated clients.

| Top-Level Directory | Responsibility | Prohibited Content |
|---|---|---|
| `docs/` | Architecture, requirements, standards, decisions | Production secrets, generated build output |
| `backend/` | Backend API, domain modules, workers, schedulers, adapters | Frontend-only code, cloud manifests unless approved |
| `frontend/` | Web frontend application and frontend tests | Backend business logic, server secrets |
| `contracts/` | OpenAPI contracts and approved generated-client sources | Handwritten business logic |
| `infrastructure/` | Future infrastructure descriptions or approved IaC | Unapproved vendor-specific production config |
| `scripts/` | Development and maintenance support scripts | Hidden production logic, secrets |
| `tests/` | Cross-system or shared test assets if needed | Production data |
| `tools/` | Developer tooling and generated-code support | Business logic |
| `generated/` or equivalent | Generated artifacts when approved | Handwritten domain/business logic |

Documentation directories MUST be clearly separated from implementation directories. Generated-code directories MUST identify generator and source schema.

### 3.1 Current Scaffold Layout Evidence

The initial scaffold establishes the accepted monorepo structure and command surface without implementing business features.

| Area | Evidence Path | Notes |
|---|---|---|
| Root command manifest | `package.json` | Coordinates backend and frontend verification commands |
| Backend source root | `backend/src/main/java/com/devpath/` | Contains Spring Boot entry point, health endpoint, and package-boundary placeholders |
| Frontend source root | `frontend/src/` | Contains React shell, providers, routes, pages, API placeholder, and tests |
| Contract root | `contracts/openapi/devpath-openapi.yaml` | Contains only the internal health contract |
| Fixture root | `fixtures/` | Contains placeholder README files only |
| Support scripts | `scripts/run-gradle.mjs`, `scripts/run-frontend.mjs` | Provide root-level command delegation |

## 4. Backend Package and Module Structure

Backend modules SHOULD follow the conceptual layers from `11_Backend_Architecture.md`.

| Layer | Allowed Responsibilities | Allowed Dependencies | Prohibited Dependencies | Public Surface | Test Placement |
|---|---|---|---|---|---|
| Domain | Entities, value objects, aggregates, invariants, domain services, domain events | Domain-local types, shared kernel value types | Web, ORM, DB, HTTP, AI SDK, logging implementation, cloud SDK | Domain interfaces/types intentionally exported | Domain tests |
| Application | Use-case orchestration, authorization coordination, transactions, ports, result mapping | Domain, application ports, shared contracts | Provider SDKs, controller rendering, direct UI concerns | Commands, queries, handlers, ports | Application/module tests |
| Infrastructure | Persistence, provider adapters, external clients, storage, cache | Application ports, domain types where required | Controller-only concerns, domain rule ownership | Adapter implementations | Integration tests |
| API | Controllers, request/response mapping, transport errors | Application services, API DTOs | Domain calculations, direct DB queries, provider SDK calls | HTTP/API operations | Contract/controller tests |

Shared code MUST be small, stable, and explicit. A generic `shared`, `common`, or `util` package MUST NOT become a dumping ground.

## 5. Dependency Rules

### 5.1 Direction

Conceptual dependency direction:

`API → Application → Domain`

`Infrastructure → Application Ports → Domain Types where required`

### 5.2 Dependency Matrix

| From / To | Domain | Application | Infrastructure | API | Shared Kernel | Other Module Internal |
|---|---:|---:|---:|---:|---:|---:|
| Domain | Yes | No | No | No | Limited | No |
| Application | Yes | Yes | No | No | Yes | Public contract only |
| Infrastructure | Yes where needed | Ports only | Yes | No | Yes | No direct internal access |
| API | No direct domain mutation | Yes | No | Yes | DTO/shared API types | Public API only |
| Tests | Yes | Yes | Yes | Yes | Yes | Test-only with care |

Domain MUST NOT depend on web frameworks, ORM frameworks, database drivers, AI SDKs, HTTP clients, logging implementations, or cloud-provider SDKs.

Cross-module imports MUST use approved public contracts. Circular dependencies MUST be detected and removed. Dependency exceptions require an ADR or documented deviation.

## 6. Naming Conventions

Names MUST reflect the ubiquitous language from `07_Domain_Model.md`.

| Item | Convention |
|---|---|
| Modules/packages | Domain capability name such as `repository`, `rule`, `career`, `knowledge`, `ai` |
| Files | Match primary type or feature responsibility |
| Classes/types | Noun or role-specific name with domain meaning |
| Interfaces/ports | Capability-oriented names such as `RepositorySnapshotStore` or `GitHubRepositoryPort` |
| Functions/methods | Verb phrase describing behavior |
| Variables | Meaningful domain or use-case role |
| Constants | Descriptive immutable domain/config names |
| Enums | Domain vocabulary with stable values |
| DTOs | Suffix or category indicating request/response/contract role |
| Commands | Imperative state-change name |
| Queries | Read-intent name |
| Handlers | Command/query/job/event handler role |
| Domain events | Past-tense fact name |
| Integration events | Past-tense serialized contract name |
| Jobs | Action + resource + `Job` concept |
| Errors | Category + domain context |
| Frontend components | UI role or domain presentation name |
| Hooks/stores | State ownership or behavior name |
| Route identifiers | Stable route domain/action names |
| Tests | Behavior statement in domain language |

Vague names such as Manager, Helper, Util, Common, Processor, Data, Item, and Info SHOULD be avoided unless responsibility is precise and documented.

## 7. File and Unit Size Guidelines

Size thresholds are review signals, not automatic architecture decisions.

| Area | Guidance |
|---|---|
| File responsibility | A file SHOULD have one cohesive reason to change |
| Class/type responsibility | Types SHOULD represent a clear domain, application, adapter, or UI role |
| Function length | Long functions SHOULD be reviewed for extraction when they mix responsibilities |
| Parameter count | Many parameters SHOULD trigger command/value object review |
| Nesting depth | Deep nesting SHOULD be refactored using guard clauses, policies, or extracted behavior |
| Branch complexity | Repeated branching by type SHOULD trigger polymorphism/policy/rule review |
| Module size | Large modules SHOULD be split by bounded capability |
| Component size | Large UI components SHOULD be split by state, layout, and presentation roles |

Refactoring triggers include multiple unrelated reasons to change, repeated type branching, large constructor dependency count, duplicated validation, duplicated mapping, unclear ownership, and untestable private logic.

## 8. Domain Model Coding Standards

| Rule | Standard |
|---|---|
| Entities | MUST protect identity, lifecycle, and invariants |
| Value objects | MUST be immutable where practical and validated at creation |
| Aggregates | MUST expose explicit consistency boundaries |
| Aggregate roots | MUST control mutations of included entities |
| Domain services | SHOULD contain domain behavior that does not naturally belong to one entity |
| Domain policies | SHOULD express variable domain decision rules explicitly |
| Domain events | SHOULD represent meaningful past-tense domain facts |
| Invariants | MUST be enforced in domain behavior where practical |
| Lifecycle transitions | MUST reject invalid transitions |
| Immutable history | RepositorySnapshot, PromptContext, and historical AnalysisResult MUST remain immutable |

Domain code MUST use meaningful constructors or factory methods, prevent invalid states where practical, keep behavior with the owning concept, use domain-language errors, and avoid infrastructure concerns. Domain code MUST NOT invoke LLMs.

## 9. Value Object and Entity Rules

| Concept | Use When | Standards |
|---|---|---|
| Entity | Identity and lifecycle matter | Identity equality, lifecycle validation, protected mutation |
| Value Object | Meaning is defined by value | Value equality, immutability, validation, safe serialization boundary |

| Domain Meaning | Rule |
|---|---|
| Identifiers | Use dedicated identifier types or explicit wrappers where domain meaning matters |
| Timestamps | Store in UTC and convert at boundaries |
| Scores | Use deterministic precision and rounding from engine specifications |
| Percentages | Avoid ambiguous raw numbers; define range and precision |
| Version identifiers | Treat as explicit value objects or typed strings |
| External-provider IDs | Keep provider identity scoped and labeled |
| Nullable values | Distinguish missing, unknown, not applicable, empty, zero, and false |

Raw strings or numbers SHOULD NOT be passed across domain boundaries when they carry domain meaning.

## 10. Deterministic Engine Standards

This chapter applies to Rule Engine, Career Path Engine, company readiness, skill gaps, roadmaps, and recommendation rules.

| Requirement | Standard |
|---|---|
| Inputs/outputs | MUST be explicit and versioned |
| Calculations | MUST be deterministic and side-effect-controlled |
| Ordering | MUST be stable when aggregation or tie-breaking occurs |
| Rounding | MUST use explicit rounding and precision rules |
| Boundary handling | MUST test threshold boundaries |
| Evidence | MUST be explainable and linked |
| Repeatability | Same input/config version MUST produce same output |
| Network/LLM calls | MUST NOT occur in deterministic engines |
| Clock dependency | MUST be injected if time is required |
| Randomness | MUST NOT be used unless seeded and justified |
| Organization | Evaluators SHOULD be grouped by rule category and named by evaluated capability |

## 11. Application Service Standards

Application services coordinate use cases.

| Responsibility | Standard |
|---|---|
| Use-case orchestration | Application service owns flow across domain and ports |
| Authorization coordination | MUST enforce or invoke backend authorization checks |
| Transaction boundary | SHOULD define transaction scope for the use case |
| Domain invocation | MUST delegate domain decisions to domain objects/services |
| Port invocation | MUST use application ports for external dependencies |
| Event publication | SHOULD publish events at defined transaction boundary |
| Result mapping | SHOULD map application results to API/frontend-safe models |

Application services MUST NOT duplicate domain rules, parse provider-specific responses, render framework responses, expose persistence entities, silently swallow partial failure, or become generic workflow containers.

## 12. Command and Query Standards

DevPath uses CQRS-lite conventions.

| Type | Standard |
|---|---|
| Command | Represents requested state change, uses imperative name, includes actor and target context, returns minimal meaningful result |
| Query | Represents read intent, performs no business-state mutation, may use optimized projections, preserves authorization |
| Handler | Owns execution path for command/query and returns explicit result |

Hidden writes inside query handlers are prohibited. Query optimization MUST NOT bypass authorization.

## 13. API Controller Standards

Controllers handle transport concerns only.

| Controllers MUST Handle | Controllers MUST NOT Handle |
|---|---|
| Transport parsing | Domain calculations |
| Authentication context extraction | Direct database queries |
| Request-schema validation | Direct AI provider calls |
| Application-service invocation | Transaction orchestration |
| Response mapping | Cross-module business workflows |
| HTTP status mapping | Complete sensitive payload logging |
| Safe error response | Business rule evaluation |

Controller methods SHOULD be small, route-owned, and named by API operation intent. Controllers MUST align with `10_API_Specification.md`.

## 14. DTO and Contract Standards

| Model Category | Purpose | Rule |
|---|---|---|
| API request DTO | Transport input | Must match API contract |
| API response DTO | Transport output | Must avoid sensitive/internal fields |
| Application command/query | Use-case input | Must include actor/target context where needed |
| Application result | Use-case output | Must be explicit and mappable |
| Domain model | Business concept | Must not be shaped by transport convenience |
| Persistence model | Storage representation | Must not leak to API by default |
| Provider model | External provider schema | Must stay inside adapter boundary |
| Frontend view model | UI state shape | Must not become API/domain source of truth |

Explicit mappings are required between categories. API DTOs MUST define optional fields, null behavior, enums, timestamps, identifiers, version fields, pagination, error objects, compatibility behavior, unknown field handling, and sensitive field exclusions according to the API specification.

## 15. Validation Standards

| Validation Layer | Responsibility |
|---|---|
| Transport Validation | Required fields, data format, payload size, syntactic validity |
| Application Validation | Actor permission prerequisites, use-case preconditions, resource existence |
| Domain Validation | Invariants, legal transitions, domain constraints |
| Provider Response Validation | External schema, expected values, malformed provider responses |
| AI Response Validation | Output schema, source references, prohibited claims, deterministic-score consistency |

Validation SHOULD NOT duplicate the same rule unnecessarily across layers. Backend validation remains authoritative.

## 16. Error and Exception Standards

| Error Category | Owning Layer | Expected? | Logging | Retry | API Mapping | User Message | Correlation |
|---|---|---:|---|---:|---|---|---:|
| Validation error | Transport/application | Yes | INFO/WARN | No | 400 category | Safe | Yes |
| Authentication error | API/security | Yes | WARN | No | 401 category | Safe | Yes |
| Authorization error | Application/security | Yes/security | WARN | No | 403 category | Safe | Yes |
| Resource not found | Application | Yes | INFO | No | 404 category | Safe | Yes |
| Conflict error | Domain/application | Yes | INFO/WARN | Maybe | 409 category | Safe | Yes |
| Rate-limit error | API/security | Yes | WARN | Later | 429 category | Safe | Yes |
| Provider error | Adapter | Often | WARN/ERROR | Maybe | 502/503 category | Normalized | Yes |
| Timeout error | Adapter/worker | Often | WARN/ERROR | Maybe | 504/503 category | Safe | Yes |
| Domain error | Domain | Yes | INFO/WARN | No | 400/409 category | Domain-safe | Yes |
| Persistence error | Infrastructure | No | ERROR | Maybe | 500/503 category | Generic | Yes |
| Internal error | Any | No | ERROR/CRITICAL | No/Maybe | 500 category | Generic | Yes |
| AI validation error | AI validator | Yes | WARN | Maybe | Task failure category | Safe | Yes |
| Job error | Worker | Maybe | WARN/ERROR | Maybe | Job failure status | Safe | Yes |

Empty catch blocks, broad catch-and-ignore, stack trace leakage, provider credential leakage, raw provider errors, and exception-based ordinary branching without justification are prohibited.

## 17. Persistence Coding Standards

| Area | Standard |
|---|---|
| Repository ports | Defined in application layer by use-case need |
| Repository adapters | Implement storage-specific behavior behind ports |
| Transaction handling | Owned by application service or explicit unit of work |
| Persistence entities | Adapter-owned JPA storage models, never domain or API models |
| Domain mapping | Explicit, module-owned, bidirectional only when the use case requires it, and tested |
| Query objects | Use bounded pagination and deterministic ordering |
| Locking/concurrency | Explicit when correctness depends on it |
| Immutable records | Historical records must reject mutation |
| Audit fields | Required for auditable changes |
| Soft deletion | Only where approved by data/security architecture |
| Bulk operations | Must preserve ownership and transaction safety |
| Fetch plans | Use explicit projection/entity graph/fetch join/bounded query; never depend on OSIV or uncontrolled lazy loading |
| Read models | Module-owned read ports may use JPQL or reviewed native SQL without returning persistence entities |

Persistence code MUST NOT contain business scoring rules, bypass ownership constraints, expose ORM entities to APIs, place persistence annotations in framework-independent domain types, create cross-module joins without architectural approval, rely on unspecified ordering, enable Open Session in View, use runtime `ddl-auto=update/create` outside disposable tests, or assume Redis is authoritative.

## 18. Transaction Standards

| Rule | Standard |
|---|---|
| Ownership | Application service SHOULD own transaction boundary |
| Scope | One use case SHOULD map to one transaction where practical |
| Start/end | Transaction boundaries MUST be explicit |
| Nested transactions | SHOULD be avoided unless framework behavior is understood and documented |
| External calls | SHOULD NOT occur inside long database transactions |
| Event publication | Must define before/after commit timing |
| Rollback | Partial failure must leave consistent state |
| Retry | Transaction retries must be idempotent-safe |
| Long-running jobs | Should split external work from state transitions |
| Outbox/equivalent | Required conceptually when reliable event publication and DB change must be coordinated |

### 18.1 Database Migration Standards

| Area | Standard |
|---|---|
| Tool | Flyway under ADR-025 |
| Versioned naming | `V<version>__<lower_snake_case_description>.sql` |
| Repeatable naming | `R__<lower_snake_case_description>.sql`, limited to approved replaceable objects/reference views |
| Immutability | Applied migrations are never edited; corrections use a new migration |
| Ownership | Migration identifies the owning module/schema and does not change another owner without review |
| Ordering | Out-of-order execution is disabled by default |
| Destructive changes | Require expand-and-contract, recovery evidence, and explicit data/security review |
| Data migrations | Large backfills are resumable observable tasks, not long blocking startup migrations |
| Production execution | Privileged deployment step; application startup validates compatibility only |
| Repair/baseline | Exceptional, approved, audited, and never used to hide drift |

Rollback notes MUST distinguish application rollback, forward-fix migration, and disaster recovery. Blind reverse migration is prohibited.

## 19. Cache Coding Standards

| Area | Standard |
|---|---|
| Cache-aside | Preferred pattern unless explicitly designed otherwise |
| Key ownership | Each module owns its key namespace |
| Key naming | Include environment/module/version where needed |
| TTL | Required for transient data unless justified |
| Invalidation | Must follow source-of-truth changes |
| Serialization | Versioned for non-trivial structures |
| Stale data | Must not violate authorization or correctness |
| Fallback | Cache failure should degrade to source of truth where safe |
| Error handling | Cache errors should not corrupt business state |
| Metrics | Hit/miss/latency/error signals SHOULD be emitted |

Cache MUST NOT be used as source of truth, store secrets unnecessarily, contain unbounded values, use unversioned serialized structures, leak cross-user data, or hide correctness dependence on availability.

## 20. Asynchronous Job Standards

| Area | Standard |
|---|---|
| Job definition | Name by action and resource |
| Payload | Store references, not unnecessary private content |
| Ownership | Include owner/resource references and validate on execution |
| Idempotency | Required where duplicate execution is possible |
| Deduplication | Use stable dedupe keys for repeatable requests |
| Retry/backoff | Bounded and observable |
| Timeout/cancellation | Explicit and safe |
| Stale recovery | Detect and recover stale jobs |
| Dead-letter | Store safe failure category and reference |
| Progress | Report state transitions and result references |
| Observability | Propagate correlation context |
| Version compatibility | Payloads must be versioned or compatible |

Workers MUST revalidate authorization or ownership where required.

## 21. Domain Event and Integration Event Standards

| Event Type | Meaning | Standard |
|---|---|---|
| Domain Event | Internal domain fact produced by domain behavior | Past-tense name, scoped to consistency boundary |
| Integration Event | Cross-module or external notification | Stable serialized contract, independently versioned |

Events MUST define naming, payload rules, metadata, causation, correlation, versioning, duplication handling, ordering assumptions, sensitive-data restrictions, and publication timing. Events MUST NOT be used as an excuse for hidden coupling.

## 22. External Integration Adapter Standards

| Adapter | Standard |
|---|---|
| GitHub | Provider models remain inside adapter; normalized application-facing port |
| Notion | Workspace/page responses normalized and redacted |
| AI providers | Provider-specific requests/responses isolated |
| Object Storage | Storage URLs and provider details hidden behind port |
| Notification providers | Payloads minimized and normalized |
| Future providers | Must follow same adapter isolation rules |

Adapters MUST implement timeout, retry policy, rate-limit handling, circuit-breaker integration, redacted logging, contract tests, and stable application-facing ports. Provider SDK types MUST NOT leak into domain or API layers.

## 23. Knowledge Pipeline Coding Standards

| Stage | Standard |
|---|---|
| Collection | Preserve source, owner, and authorization metadata |
| Normalization | Deterministic where practical and source-labeled |
| Chunking | Deterministic for same input and version |
| Embedding | Track embedding model/version |
| Indexing | Include metadata needed for authorization filtering |
| Retrieval | Apply authorization filters before returning results |
| Source citation | Preserve source references |
| Deletion | Propagate deletion to chunks/indexes |
| Re-indexing | Idempotent and versioned |

Raw embeddings MUST NOT be exposed. Retrieval relevance MUST never override access control.

## 24. Prompt and AI Pipeline Coding Standards

| Component | Responsibility | Prohibited Responsibility |
|---|---|---|
| Context Builder | Select authorized structured context | Score calculation |
| Prompt Builder | Assemble prompts from templates and variables | Business logic, authorization decisions |
| Prompt Validator | Validate prompt completeness/safety | Provider invocation |
| Provider Selector | Select configured provider/model | Deterministic evaluation |
| AI Provider Adapter | Call provider and normalize response | Domain decisions |
| Response Validator | Validate schema, grounding, prohibited claims | UI rendering |
| Output Formatter | Format validated result | Score mutation |
| Artifact Persistence | Persist validated artifacts | Raw unvalidated response trust |

System instruction, retrieved content, deterministic result, user input, prompt template, provider request, provider response, and validated result MUST remain separated. AI responses MUST NOT bypass validation.

## 25. Frontend Project Structure

Conceptual frontend structure follows `12_Frontend_Architecture.md`.

| Area | Responsibility | Public Exports | Prohibited Imports |
|---|---|---|---|
| `src/app/` | App bootstrapping, routing shell, providers | App entry/contracts | Feature internals except route registration |
| `src/platform/` | Cross-cutting platform services | Stable platform APIs | Domain feature internals |
| `src/features/` | Feature modules and route slices | Feature public API | Other feature internal files |
| `src/shared/` | Reusable UI and utilities | Stable shared components | Business feature state |
| `src/api/` | API client and contracts | Typed API functions/models | UI component internals |
| `src/assets/` | Static assets | Asset references | Business code |
| `src/tests/` | Shared frontend test support | Test helpers | Production-only dependencies |

Feature modules MUST NOT import another feature's internal files directly.

## 26. Frontend Component Standards

| Component Category | Standard |
|---|---|
| UI primitive | Domain-neutral, accessible, reusable |
| Shared composite | Reusable composition without feature-specific business logic |
| Domain presentation | Presents domain data without authoritative calculation |
| Feature component | Coordinates feature UI state and actions |
| Route component | Owns route-level loading, guard, and layout |
| Layout component | Defines structural layout and navigation |

Components MUST have one clear responsibility, accessible semantics, explicit input types, minimal hidden side effects, no authoritative business calculations, no direct raw API response dependence, and reusable components should remain domain-neutral where appropriate.

Component conventions MUST cover props, events, loading/empty/error/disabled states, composition, and controlled/uncontrolled behavior.

## 27. Frontend State and API Standards

| State Type | Rule |
|---|---|
| Server state | Owned by API/query layer and invalidated explicitly |
| Client state | UI-only state kept local or in scoped store |
| Session state | Managed through auth/session layer, not ad hoc components |
| Form state | Scoped to form and validated by UI plus backend |
| Route state | Encoded in route/search params where appropriate |

Frontend code MUST define query ownership, cache keys, invalidation, mutation behavior, cancellation, pagination, error normalization, and view-model mapping. Optimistic updates are prohibited for immutable or authoritative resources such as deterministic results. Components MUST NOT directly build authorization headers or provider requests.

## 28. Frontend Styling and Accessibility Standards

DevPath targets WCAG 2.2 AA unless another requirement is approved.

| Area | Standard |
|---|---|
| Design tokens | Use shared tokens for color, spacing, typography where available |
| Semantic components | Prefer semantic HTML and accessible roles |
| Responsive behavior | Support approved breakpoints and content reflow |
| Keyboard navigation | All interactive flows keyboard-accessible |
| Focus management | No uncontrolled focus loss |
| Labels/errors | Forms require labels and accessible errors |
| Status announcements | Async and generation status should be announced appropriately |
| Non-color indicators | Status must not rely on color only |
| Reduced motion | Respect reduced-motion preferences |
| Contrast/touch targets | Meet accessibility target requirements |
| Chart alternatives | Provide textual or data alternatives |

Final visual design is out of scope.

## 29. Security Coding Standards

| Area | Rule |
|---|---|
| Authentication context | Extract and propagate safely; never trust client authority |
| Application session | Use backend-managed opaque session cookie; never expose session identifiers to controllers, DTOs, logs, or frontend code |
| Authorization | Enforce near protected use cases and data access |
| Ownership validation | Required for user-owned resources |
| Input handling | Validate at boundaries and use allowlists where constrained |
| Output escaping | Apply context-sensitive encoding |
| Query construction | Avoid unsafe dynamic queries |
| File handling | Validate type, size, path, and content status |
| Temporary URLs | Scope and expire |
| OAuth callbacks | Validate state, redirect, provider, and replay |
| Session fixation | Rotate session identifier after successful login and privilege-sensitive transitions |
| CSRF/CORS | Validate CSRF for cookie-authenticated mutations and allow credentialed CORS only for explicit origins |
| Account linking | Bind linking state to the authenticated User; never merge by email alone |
| Provider credentials | Encrypt server-side with external key material; access only through owning adapter |
| Secret handling | No literals, logs, or frontend exposure |
| Logging | Redact private content and secrets |
| Redirects | Allowlist or validate destinations |
| Serialization | Version and validate untrusted payloads |
| Dependency use | Review provenance and vulnerability status |
| AI output rendering | Treat as untrusted and escape/validate |

Dynamic code execution from user content is prohibited.

## 30. Logging and Observability Coding Standards

| Area | Standard |
|---|---|
| Structured logs | Use stable fields from `14_Observability.md` |
| Severity | Match severity policy |
| Correlation context | Propagate request/job/event correlation |
| Trace spans | Add spans at meaningful boundaries |
| Metrics | Use bounded labels and stable operation names |
| Async context | Carry correlation into jobs and event handlers |
| Provider telemetry | Normalize provider outcomes |
| Deterministic traceability | Include rule/profile/input version references |
| AI traceability | Include PromptContext, template, provider, model, validator references |

Logs MUST NOT include private content by default. Metric labels MUST NOT contain unbounded user or repository IDs. Duplicate error logs at every layer SHOULD be avoided.

## 31. Testing Coding Standards

| Area | Standard |
|---|---|
| Test naming | Describe behavior in domain language |
| Structure | Use arrange-act-assert or equivalent clear structure |
| Fixtures | Use controlled synthetic/golden data |
| Determinism | Avoid time/randomness unless controlled |
| Isolation | Tests must not depend on order or shared mutable state |
| Cleanup | Clean resources created by tests |
| Test doubles | Use appropriate stubs/fakes/simulators without over-mocking domain behavior |
| Assertions | Assert behavior, not implementation details |
| Failure messages | Provide diagnostic context |
| Async testing | Avoid arbitrary sleeps; use deterministic synchronization |
| Security tests | Include isolation and redaction assertions |
| AI evaluations | Validate structure/grounding/safety, not exact prose |

Real production credentials, exact-string assertions for generative output, and silent flaky retries are prohibited.

## 32. Comments and Documentation Standards

Comments SHOULD explain why, non-obvious constraints, ADR references, safety requirements, compatibility concerns, or external-provider behavior. Comments SHOULD NOT restate obvious code.

| Documentation Type | Standard |
|---|---|
| Public API docs | Must align with API contract |
| Module README | Should explain module purpose, boundaries, and local conventions |
| Complex algorithms | Explain assumptions, inputs, and edge cases |
| Configuration docs | Document owner, default, validation, and restart needs |
| Migration notes | Document compatibility and recovery constraints |
| Deprecation notes | Include replacement and timeline if known |
| TODO/FIXME | Include owner or tracking reference where practical |
| ADR references | Link decisions for architectural deviations |

## 33. Configuration and Feature-Flag Coding Standards

| Area | Rule |
|---|---|
| Typed configuration | Config SHOULD be typed and validated |
| Startup validation | Invalid required configuration MUST fail early |
| Defaults | Defaults must be safe and explicit |
| Environment overrides | Must be visible and documented |
| Secret references | Use secret references, not secret values |
| Feature flags | Must have owner, scope, default, expiry, and cleanup plan |
| Rule activation | Must be versioned and audited |
| Prompt activation | Must be versioned, validated, and audited |
| Provider routing | Must be explicit and observable |

No secret fallback values or hidden business behavior based on environment name are allowed.

## 34. Time, Date, Locale, and Numeric Standards

| Area | Standard |
|---|---|
| UTC storage | Store timestamps in UTC |
| Timezone conversion | Convert at presentation or boundary layer |
| Clock injection | Required for deterministic tests and time-dependent logic |
| Date formats | Use explicit serialized formats from contracts |
| Locale | UI formatting should respect selected or default locale |
| Percentages | Define range and precision |
| Decimal precision | Use exact decimal representation where domain precision matters |
| Score rounding | Follow deterministic engine specifications |
| Duration | Use explicit units |
| File size | Use explicit units and bounds |
| Pagination counts | Use bounded integers and stable defaults |

Code MUST NOT rely on local machine timezone. Floating-point arithmetic SHOULD be avoided where exact domain precision is required.

## 35. Nullability, Optionality, and Collection Standards

| State | Meaning |
|---|---|
| Missing value | Field absent or not supplied |
| Unknown value | Value exists conceptually but is unknown |
| Not applicable | Value does not apply to this case |
| Empty collection | Known to contain no items |
| Optional input | Caller may omit |
| Nullable persisted field | Storage allows missing/unknown by design |
| Absent provider field | Provider did not return a field |

Code MUST distinguish null, empty, zero, false, and unknown. Collection-returning functions SHOULD return empty collections rather than null unless contractually required. Mutable collections SHOULD be confined to local construction and exposed as immutable/read-only where practical.

## 36. Concurrency and Thread-Safety Standards

| Area | Standard |
|---|---|
| Shared mutable state | Avoid; protect explicitly if unavoidable |
| Stateless services | Preferred for application and domain services |
| Concurrent jobs | Must use ownership, leasing, idempotency, and version compatibility |
| Optimistic concurrency | Use where concurrent updates may conflict |
| Duplicate requests | Use idempotency keys for retryable commands |
| Locks | Scope carefully and avoid process-local locks for distributed correctness |
| Transaction retries | Must be safe and bounded |
| Cancellation | Long operations should support safe cancellation |
| Resource cleanup | Required for files, streams, provider calls, and jobs |

Concurrency behavior MUST be tested where correctness depends on it.

## 37. Performance Coding Standards

| Area | Standard |
|---|---|
| Queries | Must be bounded and paginated where appropriate |
| Batch operations | Use batches for large operations with limits |
| N+1 access | Avoid through explicit fetching strategy |
| Streaming | Use where large content would exceed memory expectations |
| Memory | Avoid loading large repositories/documents unnecessarily |
| Large files/repos | Apply size limits and streaming/chunking |
| Cache | Use only when correctness does not depend on cache |
| Provider calls | Avoid repeated identical calls; respect rate limits |
| Frontend rendering | Avoid unnecessary re-rendering of large views |
| Bundle loading | Keep route/feature loading intentional |

Do not optimize without evidence. Performance-sensitive code SHOULD document reason, measurement method, expected behavior, and regression test where practical.

## 38. Git and Commit Standards

| Area | Standard |
|---|---|
| Branch naming | Use `feature/<topic>`, `fix/<topic>`, `docs/<topic>`, `adr/<topic>`, `chore/<topic>`, or `codex/<topic>` |
| Commit scope | One coherent change |
| Commit message | Prefer concise Conventional Commit-style messages where practical |
| Atomic commits | Prefer separable commits for reviewable changes |
| Generated files | Include generated diff only when source generation is understood |
| Secret prevention | Secret scanning required before merge |
| Binary files | Avoid unless necessary and justified |
| Merge strategy | Prefer squash merge for short-lived feature branches |
| Rebase policy | Rebase local/private branches when useful; do not rewrite shared or protected branch history |
| Release tags | Should map to release records when adopted |
| Revert commits | Should clearly identify reverted change and reason |

Do not mix large refactors with unrelated functional changes.

## 39. Pull Request Standards

Pull requests MUST include clear purpose, linked requirement/issue where available, architecture impact, API impact, data impact, security impact, test evidence, observability impact, deployment impact, screenshots for UI changes where useful, migration or rollback notes, and unresolved risks.

Large changes SHOULD be divided into reviewable units. No AI-generated pull request may bypass review.

## 40. Code Review Standards

| Review Area | Reviewer Responsibility |
|---|---|
| Correctness | Verify behavior meets requirements |
| Domain consistency | Check ubiquitous language and invariants |
| Architecture boundaries | Enforce module/layer dependency rules |
| Security | Verify auth, redaction, secrets, input/output safety |
| Tests | Ensure appropriate test coverage and quality |
| Observability | Check logs, metrics, tracing, correlation where needed |
| Performance | Identify obvious unbounded behavior |
| Readability | Ensure code is understandable |
| Maintainability | Avoid unnecessary abstraction and duplication |
| Backward compatibility | Verify API/schema/job/event compatibility |

Review comment categories are blocking, required change, suggestion, question, and optional improvement. Reviewers SHOULD explain the violated rule or risk. Style debates handled by tooling SHOULD be avoided.

## 41. Refactoring Standards

| Rule | Standard |
|---|---|
| Preserve behavior | Refactoring MUST preserve externally observable behavior unless explicitly changed |
| Characterization tests | Add when behavior is unclear or risky |
| Separation | Refactoring SHOULD be separate from feature changes |
| API compatibility | Public contracts must remain compatible or follow change process |
| Historical data | Must protect historical results and snapshots |
| Event/job compatibility | Must preserve serialized compatibility or migrate safely |
| Documentation | Update docs when architecture or behavior changes |
| Performance | Measure performance-sensitive refactors |

Large architectural refactoring may require an ADR. Opportunistic rewrites outside task scope are prohibited.

## 42. Generated Code Standards

Generated code includes API clients, schema models, serialization code, migration metadata, and documentation artifacts.

| Rule | Standard |
|---|---|
| Location | Generated code MUST live in explicit directories |
| Generator identity | Generator and version SHOULD be identifiable |
| Source schema | Source schema/reference MUST be identifiable |
| Manual editing | Hand edits SHOULD be avoided |
| Reproducibility | Generation SHOULD be reproducible |
| Review | Review source and generated diff |
| Business logic | Handwritten business logic MUST NOT be placed in generated files |

## 43. AI-Assisted Coding Standards

AI coding agents MUST read relevant Source-of-Truth documents, state implementation scope, identify affected modules, preserve architectural boundaries, avoid inventing requirements, avoid silently selecting unresolved technologies, generate or update tests, preserve security and telemetry rules, report assumptions, report unresolved conflicts, limit changes to requested scope, and summarize created/modified/deleted files.

AI coding agents MUST NOT rewrite unrelated modules, weaken tests to make builds pass, remove authorization checks, log private data, introduce dependencies without approval, invent database fields, alter API contracts silently, calculate authoritative scores through an LLM, bypass response validation, commit secrets, or mark work complete without verification.

All AI-generated code requires human review.

## 44. AI Implementation Task Template

Use this compact, framework-neutral task template for future implementation prompts:

```text
Role:
Task Objective:
Source Documents:
Scope:
Allowed Files:
Prohibited Files:
Architecture Constraints:
API Constraints:
Data Constraints:
Security Constraints:
Observability Constraints:
Testing Requirements:
Acceptance Criteria:
Required Final Report:
Assumptions and Open Questions:
```

The template MUST be adapted to the specific task and MUST NOT be used to bypass Source-of-Truth documents.

## 45. Prohibited Patterns Catalog

| Pattern | Risk | Approved Alternative | Exception Process |
|---|---|---|---|
| Business logic in controllers | Duplicated and untestable behavior | Application/domain service | Architecture approval |
| Business logic in UI components | Frontend becomes business authority | Backend API/domain output | Not allowed for scores |
| Direct cross-module repository access | Hidden coupling and data leaks | Public module contract | ADR required |
| ORM entity as API DTO | Storage leaks into API | Explicit DTO mapping | Architecture approval |
| External SDK model in domain | Provider lock-in | Adapter mapping | Not allowed |
| Global mutable state | Race conditions | Scoped immutable config/state | ADR required |
| Hidden network calls | Latency and reliability surprises | Explicit port/adapter | Review exception |
| Generic catch-and-ignore | Silent failure | Categorized error handling | Not allowed |
| Raw provider-error exposure | Secret/data leakage | Normalized safe errors | Not allowed |
| Secrets in source | Credential compromise | Secret store/reference | Not allowed |
| Sensitive-data logging | Privacy/security incident | Redacted structured logs | Security approval only |
| LLM score calculation | Violates core philosophy | Rule/Career Engine calculation | Not allowed |
| Raw AI response persistence without validation | Unsafe output trust | Response Validator gate | Not allowed |
| Unbounded metric labels | Telemetry cost/cardinality failure | Bounded labels | Observability approval |
| Unversioned job payloads | Deployment incompatibility | Versioned payloads | Architecture approval |
| Exact generated-text tests | Flaky AI tests | Schema/grounding validation | Test lead approval |
| Redis as source of truth | Data loss/corruption | PostgreSQL/source store | Not allowed |
| Raw embeddings in API responses | Privacy/security leakage | Retriever-only access | Not allowed |
| Undocumented feature flags | Hidden behavior | Owner/expiry/audit flag | Product/platform approval |
| Permanent TODO without owner | Technical debt rot | Owner/tracking reference | Review exception |
| Duplicated source of truth | Inconsistent behavior | Single authoritative module/document | Architecture approval |

## 46. Enforcement Strategy

| Mechanism | Enforces | Classification |
|---|---|---|
| Formatter | Formatting consistency | Automatically enforceable |
| Linter | Style, simple correctness, unsafe patterns | Automatically enforceable |
| Type checker | Type safety and nullability where supported | Automatically enforceable |
| Architecture tests | Dependency direction and module boundaries | Automatically/partially enforceable |
| Dependency checks | Vulnerabilities, licenses, forbidden dependencies | Automatically enforceable |
| Static analysis | Security and maintainability issues | Partially automatable |
| Secret scanning | Secret prevention | Automatically enforceable |
| Contract verification | API schema compatibility | Automatically enforceable |
| Test gates | Behavior regression | Automatically enforceable |
| Code review | Domain quality, readability, architecture fit | Review-enforced |
| Documentation review | ADRs, README, migration notes | Review-enforced |
| ADR process | Unresolved architecture/technology decisions | ADR-enforced |

Exact tools remain TBD unless approved elsewhere.

## 47. Exception and Deviation Process

| Field | Requirement |
|---|---|
| Rule violated | Identify exact rule or standard |
| Reason | Explain why deviation is necessary |
| Alternatives considered | Document safer options |
| Scope | Define affected modules/files |
| Risk | Security, reliability, maintainability, data impact |
| Duration | Temporary or permanent |
| Owner | Named accountable owner |
| Approver | Required reviewer/authority |
| Remediation plan | Required for temporary deviation |
| ADR requirement | Mark ADR REQUIRED where architectural |
| Expiry | Required for temporary deviations |

Repeated deviations SHOULD trigger architecture review.

## 48. Traceability

### 48.1 Architecture to Coding Rule

| Source Document | Architecture Requirement | Coding Rule | Enforcement | Owner |
|---|---|---|---|---|
| `02_Rule_Engine.md` | Rule Engine calculates scores | No LLM/network in deterministic engines | Tests/review | Rule owner |
| `03_Career_Path_Engine.md` | Career/company readiness deterministic | Career rules tested independently | Tests/review | Career owner |
| `07_Domain_Model.md` | Domain invariants and ubiquitous language | Domain model standards and naming | Review/tests | Architecture |
| `10_API_Specification.md` | API contract authority | DTO/controller contract alignment | Contract tests | API owner |
| `11_Backend_Architecture.md` | Modular monolith boundaries | Dependency matrix and module contracts | Architecture tests | Backend |
| `12_Frontend_Architecture.md` | Frontend must not calculate scores | Component/state restrictions | Review/tests | Frontend |
| `13_Security_Architecture.md` | Backend authz and no secret logging | Security coding standards | Security tests | Security |
| `14_Observability.md` | Structured telemetry | Logging/metrics standards | Observability tests | Ops |
| `15_Test_Architecture.md` | Risk-based tests | Testing coding standards | Test gates | QA |
| `16_Deployment_Guide.md` | Config/secrets outside artifacts | Config/feature flag rules | Deployment review | Platform |

### 48.2 Module to Standards

| Module | Package Structure | Domain Rules | API Rules | Persistence Rules | Security Rules | Testing Rules | Observability Rules |
|---|---|---|---|---|---|---|---|
| Identity | API/application/infrastructure | User/session concepts | Auth DTOs | User/session stores | Auth/session security | Auth/security tests | Auth logs |
| Repository | Domain/application/adapters | Snapshot immutability | Repo APIs | Snapshot/evidence stores | Ownership | Sync/analysis tests | Sync/job traces |
| Rule | Domain/application | Deterministic rules | Result APIs | Result persistence | No AI overwrite | Golden tests | Rule version traces |
| Career | Domain/application | Profile/readiness rules | Career APIs | Profile/result stores | Owner target changes | Regression tests | Profile version traces |
| Knowledge | Pipeline/adapters | Knowledge ownership | Retrieval APIs | Vector/source stores | Retrieval auth | Retrieval tests | Retrieval metrics |
| AI/Prompt | Pipeline/adapters | No business authority | Generation APIs | Artifact/prompt stores | Prompt injection defenses | AI eval tests | Generation traces |
| Portfolio/Resume | Application/frontend | Artifact lifecycle | Artifact APIs | Artifact storage | Publication approval | E2E tests | Artifact logs |
| Admin | API/application | Config activation | Admin APIs | Audit/config stores | Admin auth/audit | Admin security tests | Audit/deploy logs |

### 48.3 Security to Coding Rule

| Security Control | Required Coding Behavior | Prohibited Pattern | Verification |
|---|---|---|---|
| Backend authorization | Check owner/role in use case | Frontend-only authz | Security tests |
| Token secrecy | Keep secrets server-side | Secrets in logs/frontend | Secret scan/redaction tests |
| Prompt injection defense | Separate instructions/data and validate output | Raw AI trust | AI security tests |
| Cross-user isolation | Filter by owner metadata | Direct unscoped queries | IDOR tests |
| Auditability | Emit audit for sensitive changes | Silent admin mutation | Audit tests |

### 48.4 Test Architecture to Coding Rule

| Test Requirement | Test-Code Convention | Execution Expectation | Release Relevance |
|---|---|---|---|
| Deterministic core coverage | Golden datasets and boundary tests | PR/release | Blocking |
| API contract coverage | Schema and error contract tests | PR/release | Blocking |
| Security threat coverage | Authz, IDOR, prompt injection tests | PR/pre-release | Blocking high-risk |
| AI evaluation | Validators and rubric-based checks | PR/scheduled | Conditional |
| Observability verification | Correlation/log/metric assertions | PR/pre-release | Conditional |

## 49. Open Issues and ADR Candidates

| Issue ID | Context | Options | Recommendation | Impact | Owner | Status | ADR |
|---|---|---|---|---|---|---|---|
| CODE-OI-001 | Programming languages | Java/Kotlin/TypeScript/Python variants | Java 21 LTS and TypeScript accepted by ADR-020/021. | Tooling | Engineering | Resolved | ADR-020, ADR-021 |
| CODE-OI-002 | Backend framework | Spring Boot selected by context; details TBD | Spring Boot accepted by ADR-020. | Backend | Backend | Resolved | ADR-020 |
| CODE-OI-003 | Frontend framework | React selected by context; details TBD | React accepted by ADR-021. | Frontend | Frontend | Resolved | ADR-021 |
| CODE-OI-004 | Package naming style | By domain, by layer, hybrid | Domain-first with layers inside modules | Structure | Architecture | Open | Yes |
| CODE-OI-005 | Formatter | TBD | Select per language | Consistency | Engineering | Open | Yes |
| CODE-OI-006 | Linter | TBD | Select per language | Quality | Engineering | Open | Yes |
| CODE-OI-007 | Static-analysis tools | TBD | Add security and dependency checks | Security | Security | Open | Yes |
| CODE-OI-008 | Dependency-rule tooling | Architecture tests, linter, custom | Prefer automated boundary tests | Architecture | Backend | Open | Yes |
| CODE-OI-009 | API code generation | Generate clients/models or manual | Evaluate after API workflow | Contract | API/Frontend | Open | Yes |
| CODE-OI-010 | ORM selection | JPA/other/TBD | Spring Data JPA/Hibernate accepted by ADR-024 | Persistence | Data | Resolved | ADR-024 |
| CODE-OI-011 | Mapping approach | Manual, mapper library, generated | Explicit adapter-owned mapping accepted by ADR-024; mapper library remains implementation detail | Maintainability | Backend | Resolved | ADR-024 |
| CODE-OI-021 | Migration tool and conventions | Flyway, Liquibase | Flyway versioned SQL accepted by ADR-025 | Persistence/Deployment | Data/Ops | Resolved | ADR-025 |
| CODE-OI-022 | Authentication credential handling | Cookie session, bearer token, hybrid | Opaque HttpOnly server session accepted by ADR-026 | Security/Frontend | Security | Resolved | ADR-026 |
| CODE-OI-012 | Error-handling library | Framework-native/custom | Define stable categories first | API | Backend | Open | Yes |
| CODE-OI-013 | Date/time library | Platform standard/TBD | Use platform standard with UTC rules | Correctness | Engineering | Open | Yes |
| CODE-OI-014 | Test frameworks | TBD by stack | Layered JUnit/Spring/Vitest/Playwright baseline accepted by ADR-032. | Quality | QA | Resolved | ADR-032 |
| CODE-OI-015 | Git workflow | Trunk, GitFlow, GitHub Flow | Simplified GitHub Flow accepted by ADR-035. | Release | Engineering | Resolved | ADR-035 |
| CODE-OI-016 | Commit convention | Conventional commits/custom | Conventional Commit-style messages preferred by ADR-035. | Review/release | Engineering | Resolved | ADR-035 |
| CODE-OI-017 | Branching model | TBD | Short-lived feature branches accepted by ADR-035. | Deployment | Platform | Resolved | ADR-035 |
| CODE-OI-018 | Code coverage policy | Advisory/blocking thresholds | No percentage until baseline | Quality | QA | Open | Yes |
| CODE-OI-019 | Generated-code policy | Manual vs generated clients | Use explicit generated directories if adopted | Maintainability | API | Open | Yes |
| CODE-OI-020 | AI coding-agent workflow | Prompt template/review rules | Use Chapter 44 and human review | Governance | Engineering | Open | Yes |

## 50. Final Consistency Review

### 50.1 Completeness Checklist

| Check | Result |
|---|---|
| All rules align with previous architecture documents | Complete |
| Module dependency direction is explicit | Complete |
| Domain code is framework-independent | Complete |
| Deterministic engines prohibit LLM calls | Complete |
| Controllers contain no business logic | Complete |
| Frontend components contain no authoritative calculations | Complete |
| DTO, domain, persistence, provider, and view models are separated | Complete |
| Authorization remains backend-enforced | Complete |
| Validation responsibilities are separated by layer | Complete |
| Exceptions and errors have stable categories | Complete |
| Transactions have clear ownership | Complete |
| Async jobs have idempotency and versioning rules | Complete |
| Provider-specific types remain inside adapters | Complete |
| Knowledge retrieval preserves authorization | Complete |
| AI output cannot bypass validation | Complete |
| Secrets and private content are excluded from logs | Complete |
| Metrics avoid unbounded cardinality | Complete |
| Test-code standards align with Test Architecture | Complete |
| Deployment configuration rules align with Deployment Guide | Complete |
| AI-assisted code requires review | Complete |
| Prohibited patterns have approved alternatives | Complete |
| Standards have enforcement methods | Complete |
| Unresolved technology decisions are ADR candidates | Complete |
| No unsupported technology was selected | Complete |
| No duplicate Source of Truth was introduced | Complete |

### 50.2 Final Metrics

| Metric | Count or Summary |
|---|---|
| Mandatory-rule count | 118 normative MUST/MUST NOT rules |
| Prohibited-pattern count | 21 |
| Automatically enforceable rule count | 9 mechanism categories |
| Review-enforced rule count | 2 mechanism categories plus domain/security/code review responsibilities |
| ADR-required issue count | 22 |
| Module coverage summary | Identity, Repository, Rule, Career, Knowledge, AI/Prompt, Portfolio/Resume, Admin covered |
| Security-rule coverage summary | Authorization, token secrecy, prompt injection, cross-user isolation, auditability mapped |
| AI-assisted coding control summary | Source review, scope control, tests, security, telemetry, no LLM scoring, human review required |
| Unresolved issue count | 11 |

### 50.3 Final Architectural Assertion

DevPath coding standards enforce architecture before convenience. Domain and deterministic engines remain framework-independent and deterministic. Controllers and frontend components cannot become business-rule owners. Persistence, provider, DTO, domain, and view models remain separate. AI output is always untrusted until validated. Security, observability, testing, deployment, and AI-assisted coding rules apply equally to human and AI-generated implementation.

## 51. Identity Foundation Conformance Evidence

The Identity slice demonstrates the standards through:

- framework-free domain objects under `identity/domain`;
- application ports and use cases under `identity/application`;
- JPA entities and Spring Data repositories confined to `identity/adapter/out/persistence`;
- explicit domain/persistence mapping;
- inbound OAuth and HTTP adapters that depend on application use cases rather than JPA;
- typed security configuration and externalized secrets;
- React Query-owned frontend session state without browser token storage;
- architecture tests for domain, adapter, persistence, and repository boundaries.

Java 21 compilation, architecture tests, backend tests, and production build passed. PostgreSQL-dependent integration enforcement remains unverified because Docker is unavailable.
