# DevPath Instructions for Codex and OpenAI Agents

## 1. Scope

This file applies to the entire repository. Every human or AI contributor must follow it unless a more specific `AGENTS.md` exists below the file being changed. No nested instruction file currently exists.

DevPath is an AI-assisted developer career intelligence platform. It analyzes GitHub and Notion evidence, calculates measurable results through deterministic engines, and uses AI only to explain results or generate drafts.

## 2. Non-Negotiable Product Rules

1. The Rule Engine calculates official scores.
2. AI never calculates, changes, or guesses official scores, weights, readiness, recommendation priority, or deterministic business results.
3. Career selection changes the applicable evaluation rules.
4. Company selection changes company-specific evaluation and recommendation policy.
5. Every official result must be measurable, reproducible, versioned where required, and traceable to evidence.
6. AI output is untrusted until validated.
7. PostgreSQL is the authoritative structured-data store. Redis, caches, projections, and LLM context are never sources of truth.
8. User-owned data must be isolated by user and checked at every retrieval and mutation boundary.

Stop and report a conflict rather than weakening any of these rules.

## 3. Authority Hierarchy

When documents disagree, use this order:

1. `docs/01_SRS.md` and deterministic-engine specifications (`02`, `03`)
2. `docs/07_Domain_Model.md` and `docs/08_System_Data_Model.md`
3. `docs/10_API_Specification.md` and machine-readable contracts
4. `docs/11_Backend_Architecture.md` and `docs/12_Frontend_Architecture.md`
5. Security, observability, test, database, and deployment documents
6. `docs/17_Coding_Standards.md`
7. Module README files and local conventions
8. Individual implementation preference

Additional rules:

- `docs/00_Project_Context.md` defines project vision and immutable product philosophy.
- `docs/18_ADR.md` is authoritative for technology decisions. Only **Accepted** ADRs may be treated as selected technology.
- `docs/19_Roadmap.md` controls implementation order and milestone gates, but does not override requirements or accepted ADRs.
- `DESIGN.md` is the repository-level UI/UX requirements guide. Read it for visual, interaction, responsive, state, content, and accessibility work; it remains subordinate to the SRS and frontend architecture.
- Korean translations in `docs_ko/` are convenience copies. Use `docs/` when exact semantics matter.

## 4. Current Implementation Baseline

Implemented:

- Java 21 and Spring Boot 3.3.5 backend scaffold.
- Modular-monolith and hexagonal package boundaries.
- GitHub OAuth2 login foundation.
- Provider-independent internal `User` identity and GitHub `ExternalIdentity` link.
- Spring Data JPA persistence adapters separated from domain models.
- Flyway migration `V1__create_identity_and_session_schema.sql`.
- PostgreSQL-backed Spring Session configuration.
- Current-user, CSRF, logout, and internal-health endpoints.
- React session bootstrap using React Query.
- Backend domain/application/security/architecture tests and frontend tests.
- OpenAPI subset for implemented endpoints.

Not implemented:

- GitHub repository collection or synchronization.
- Notion integration.
- Background job runtime.
- Rule, Career, Company, Knowledge, Prompt, AI, Recommendation, Learning, Portfolio, Resume, Interview, Dashboard, or Administration product workflows.
- Production deployment and production secret management.

Current verification note:

- Java 21 compile, non-container backend tests, backend build, frontend tests/build, and OpenAPI validation have passed.
- A local PostgreSQL-backed `bootRun` has been observed to start successfully.
- Re-run the complete backend suite with Docker available to produce fresh Testcontainers evidence before declaring the Identity/Persistence foundation complete.

Do not present planned modules as implemented functionality.

## 5. Accepted and Open Decision Gates

Important accepted baseline:

- ADR-001/002: modular monolith with hexagonal boundaries.
- ADR-003: PostgreSQL primary structured store.
- ADR-005: contract-first REST APIs.
- ADR-006: deterministic engines separated from AI.
- ADR-012/013: feature-oriented React with server/client state separation.
- ADR-020/021: Java 21 Spring Boot and React TypeScript.
- ADR-024: Spring Data JPA/Hibernate with separate persistence models.
- ADR-025: Flyway immutable SQL migrations.
- ADR-026: GitHub OAuth2 with opaque server-managed session.
- ADR-032: current testing toolchain.
- ADR-035: simplified GitHub Flow.

Do not silently select technologies covered by Proposed ADRs:

- ADR-027 background job technology.
- ADR-028 vector database.
- ADR-029 object storage.
- ADR-030 AI provider SDK strategy.
- ADR-031 observability technology.
- ADR-033 deployment platform.
- ADR-034 production secrets management.

A Proposed ADR blocks only work that genuinely requires that decision. Unrelated modules may continue.

## 6. Repository Layout

| Path | Responsibility |
|---|---|
| `backend/` | Spring Boot application and backend tests |
| `frontend/` | React application and frontend tests |
| `contracts/` | OpenAPI and machine-readable contracts |
| `docs/` | Authoritative requirements and architecture |
| `docs_ko/` | Korean reference translations |
| `fixtures/` | Deterministic test fixtures for future engines |
| `scripts/` | Repository command wrappers |

Do not import backend source into frontend or frontend source into backend. Integration occurs through API contracts.

## 7. Backend Rules

Use feature packages under `com.devpath.<module>` with these boundaries:

- `domain`: entities, value objects, invariants, domain services; no Spring, JPA, OAuth SDK, HTTP, logging implementation, or AI SDK dependencies.
- `application`: use cases, transactions, application ports; no controller or JPA entity dependency.
- `adapter.in`: HTTP, security, event, or job entry points.
- `adapter.out`: persistence and provider implementations.
- `config`: framework wiring only.

Mandatory behavior:

- Controllers delegate to application use cases and never access JPA repositories directly.
- JPA entities remain in outbound persistence adapters and map explicitly to domain objects.
- Flyway owns schema changes. Normal runtime uses `ddl-auto=validate`, never `update`, `create`, or `create-drop`.
- Application services own transaction boundaries.
- External network calls must not run inside long database transactions.
- Mutable aggregates use explicit concurrency behavior where lost updates matter.
- Immutable snapshots and completed historical results reject mutation.
- Provider SDK types must not leak into domain, application contracts, or public APIs.
- Never add JWT, Redis sessions, jOOQ, Liquibase, queue frameworks, vector clients, object storage clients, or AI SDKs without approved scope and ADR support.

## 8. Frontend Rules

- Organize product code by feature under `frontend/src/features`.
- React Query owns server state. Do not duplicate authenticated user, repositories, jobs, scores, or other server resources in a second global store.
- Treat `GET /api/v1/users/me` as the authoritative browser-session bootstrap.
- A `401` is anonymous state; transport failure is a separate error state.
- Send credentialed requests through the shared API boundary.
- Never store session IDs, provider tokens, refresh tokens, or application credentials in `localStorage` or `sessionStorage`.
- Components do not calculate official scores, readiness, recommendation priority, or business rules.
- Every asynchronous view must represent loading, empty/anonymous, success, partial where applicable, and error states accessibly.
- Do not call GitHub, Notion, or AI providers directly from the browser.

## 9. API, Data, and Security Rules

- Preserve canonical API IDs and method/path pairs from `docs/10_API_Specification.md`.
- Update `contracts/openapi/devpath-openapi.yaml` only for endpoints actually implemented.
- API changes, backend handlers, and frontend clients must remain synchronized.
- New product behavior must trace to an existing requirement ID. If no requirement exists, stop and request a requirement change.
- New persistence fields or tables must trace to the domain/data/database documents and use a new immutable Flyway migration. Never edit an applied migration.
- Browser authentication uses an opaque HttpOnly server session, CSRF protection, restrictive credentialed CORS, and session-fixation protection.
- Never expose provider tokens, session IDs, credentials, raw OAuth payloads, private repository content, embeddings, SQL errors, or stack traces.
- Operational logs are not durable audit records. Do not claim otherwise.
- Authorization is backend-authoritative and must be enforced before retrieval, prompt assembly, generation, export, or publication.

## 10. Change Workflow

Before editing:

1. Inspect repository status and preserve existing user changes.
2. Identify the requirement IDs, current roadmap milestone, relevant Accepted ADRs, affected modules, API/data/security impact, and excluded scope.
3. Read only the authoritative chapters needed for the task.
4. Stop if the task requires a Proposed ADR, invents business functionality, lacks a contract/schema decision, or conflicts with a higher authority.

While editing:

- Keep the patch limited to one coherent capability.
- Fix root causes rather than symptoms.
- Do not perform unrelated refactors, mass renames, formatting sweeps, dependency upgrades, or documentation rewrites.
- Do not weaken tests, architecture rules, authorization, validation, or security controls to make verification pass.
- Do not overwrite changes made by another worktree or agent.
- Add or update tests at the closest appropriate level.
- Update documentation only when implementation evidence or an approved decision changed.

Before handoff:

1. Run targeted tests first, then the broader relevant suite.
2. Run contract, migration, secret, and architecture checks when affected.
3. Review the diff for unrelated changes and generated artifacts.
4. Report PASS, FAIL, or NOT RUN honestly; include exact blockers.
5. Do not declare a roadmap milestone complete. Completion requires coordinator/owner review and evidence.

## 11. Commands

From repository root:

```text
npm run backend:test
npm run backend:build
npm run frontend:install
npm run frontend:test
npm run frontend:build
npm run test
npm run verify
```

Direct local development:

```text
cd backend
./gradlew bootRun

cd frontend
npm run dev
```

Windows PowerShell uses `\.\gradlew.bat bootRun`. Backend requires Java 21 and mandatory environment variables from `backend/.env.example`. Spring Boot does not automatically load `.env`; export the values in the process environment or use an approved local launcher.

PostgreSQL/Testcontainers verification requires a running Docker-compatible engine.

## 12. Multi-Agent and Worktree Safety

- In an Orca session, use Orca-managed worktrees and task handoffs; do not create ad hoc raw Git worktrees.
- One worker owns a file at a time. Shared contracts, migrations, lockfiles, root configuration, and ADRs require explicit coordinator ownership.
- Workers must not edit another worker's task files without a coordinator-approved ownership transfer.
- Contract work precedes dependent backend/frontend work. Migration ownership precedes persistence adapter work.
- Workers report changed files, decisions, tests, skipped checks, risks, and recommended integration order.
- Do not commit, push, merge, or rewrite history unless the user or coordinator explicitly requests it.

When working as an Orca worker, follow the ownership, status, blocker, and handoff rules in this file without assuming another instruction file has been read.

## 13. Required Handoff Summary

Every implementation handoff must state:

- Objective and requirement IDs.
- Affected modules and files.
- Implemented behavior.
- API, data, security, observability, and documentation impact.
- Tests and commands with PASS/FAIL/NOT RUN.
- Assumptions and deviations.
- Remaining blockers and the next recommended task.
