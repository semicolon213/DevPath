# DevPath Instructions for Gemini CLI

## 1. Purpose

This is Gemini CLI's standalone repository guide. Use it without relying on another agent instruction file.

DevPath is an AI-assisted developer career intelligence platform that analyzes GitHub and Notion evidence. Deterministic engines evaluate skills, quality, growth, career readiness, and company readiness. AI explains those structured results and generates grounded drafts such as portfolios, resumes, README improvements, interview questions, and learning guidance.

## 2. Non-Negotiable Domain Rules

1. The Rule Engine is the only component allowed to calculate official scores.
2. AI must never calculate, modify, infer, or prioritize scores, weights, readiness, or deterministic recommendations.
3. Career selection changes evaluation policy; company selection changes company-specific policy and recommendations.
4. Official outcomes must be measurable, reproducible, version-aware, and linked to evidence.
5. AI responses are untrusted until validated for structure, grounding, evidence, and safety.
6. PostgreSQL is authoritative for structured business data; caches, embeddings, projections, and prompt context are derived.
7. User data must remain isolated and authorization must be checked at every data and generation boundary.

If a request conflicts with these principles, stop and explain the conflict instead of implementing it.

## 3. Documentation Precedence

Use the following order when specifications differ:

1. SRS and deterministic engines: `docs/01_SRS.md`, `docs/02_Rule_Engine.md`, `docs/03_Career_Path_Engine.md`
2. Domain and system data: `docs/07_Domain_Model.md`, `docs/08_System_Data_Model.md`
3. API: `docs/10_API_Specification.md` and machine-readable contracts
4. Application architecture: `docs/11_Backend_Architecture.md`, `docs/12_Frontend_Architecture.md`
5. Security, observability, testing, database, and deployment specifications
6. `docs/17_Coding_Standards.md`
7. Existing local patterns
8. Agent preference

`docs/00_Project_Context.md` fixes the vision. `docs/18_ADR.md` governs technology: only **Accepted** ADRs are binding choices. `docs/19_Roadmap.md` governs order but does not override requirements. For UI work, `DESIGN.md` defines visual, interaction, responsive, state, content, and accessibility requirements subordinate to the SRS and frontend architecture. `docs/` is authoritative and `docs_ko/` is a convenience translation.

## 4. Architecture Map

| Path | Role |
|---|---|
| `backend/` | Java 21 and Spring Boot 3.3.5 modular monolith |
| `frontend/` | React 18, TypeScript, Vite, TailwindCSS, React Query |
| `contracts/` | OpenAPI and machine-readable interfaces |
| `docs/` | Authoritative requirements and designs |
| `docs_ko/` | Korean translations for reference |
| `fixtures/` | Deterministic test evidence and fixtures |
| `scripts/` | Root automation wrappers |

Backend modules use hexagonal layers under `com.devpath.<module>`:

- `domain`: framework-free entities, values, invariants, and domain services.
- `application`: use cases, transactions, input/output ports.
- `adapter.in`: HTTP, security, event, and job entry points.
- `adapter.out`: JPA and external-provider implementations.
- `config`: dependency and framework wiring.

Domain code cannot import Spring, JPA, HTTP, logging implementations, provider SDKs, or AI SDKs. Controllers delegate to use cases, not JPA repositories. Persistence entities map explicitly to domain objects. Flyway exclusively controls schema changes, and application services control transactions.

Frontend features live under `frontend/src/features`. React Query owns server state. Browser authentication uses GitHub OAuth2 plus an opaque HttpOnly server-side session, CSRF protection, and restrictive credentialed CORS. The browser never stores provider tokens or session credentials and never contacts GitHub, Notion, or AI providers directly.

## 5. Implementation Baseline

Available now:

- Provider-independent internal `User` and GitHub `ExternalIdentity`.
- GitHub OAuth2 login, Spring Security, and JDBC-backed Spring Session.
- JPA adapters separated from domain models and Flyway V1 schema.
- Current-user, CSRF, logout, and internal-health endpoints.
- React Query authentication bootstrap and an OpenAPI subset matching implemented operations.
- Backend domain/application/security/architecture tests and frontend tests.

Planned, not implemented:

- Repository collection/synchronization, Notion integration, and background job execution.
- Rule, Career, Company, Knowledge, Prompt, AI, Recommendation, Learning, Portfolio, Resume, Interview, Dashboard, and Administration modules.
- Production deployment and production secret handling.

Previous evidence includes Java 21 compilation, non-container backend tests/build, frontend tests/build, OpenAPI validation, and successful local PostgreSQL startup. Fresh Docker/Testcontainers verification is still required before persistence completion is claimed.

## 6. Technology Decision Boundaries

Accepted baseline decisions include ADR-001/002, ADR-003, ADR-005, ADR-006, ADR-012/013, ADR-020/021, ADR-024, ADR-025, ADR-026, ADR-032, and ADR-035.

Do not select or add technology controlled by these Proposed ADRs:

- ADR-027 background jobs.
- ADR-028 vector database.
- ADR-029 object storage.
- ADR-030 AI provider SDK strategy.
- ADR-031 observability technology.
- ADR-033 deployment platform.
- ADR-034 production secrets management.

Treat each as a local gate, not a global freeze. Continue unrelated domain, contract, or UI work when it does not depend on the undecided technology.

## 7. Gemini Execution Protocol

Before making changes:

1. Inspect repository and Git status without modifying user work.
2. Identify requirement IDs, roadmap slice, Accepted ADRs, affected modules, and measurable acceptance criteria.
3. Read the smallest relevant authoritative document sections.
4. Search the repository for existing types, routes, tests, and conventions.
5. Identify contract, migration, security, and multi-agent ownership impacts.

During changes:

- Keep the patch narrow and solve the root cause.
- Match the ubiquitous language and existing naming.
- Do not invent APIs, fields, states, roles, business logic, integrations, or future features.
- Do not add speculative abstractions, broad refactors, dependency upgrades, or formatting sweeps.
- Never weaken validation, authorization, architecture tests, or security controls.
- Never write secrets, credentials, access tokens, private keys, or real user data into source, logs, fixtures, or documentation.
- Use concise progress notes before meaningful tool operations and report uncertainty as an assumption.

## 8. API, Persistence, and Security Constraints

- Preserve API IDs and exact normalized method/path pairs from `docs/10_API_Specification.md`.
- Update machine-readable OpenAPI only when its endpoint is implemented; keep handler and frontend client behavior aligned.
- Require an existing requirement ID for new product behavior.
- Create a new immutable Flyway migration for every schema change and never edit an applied migration.
- Keep normal runtime schema validation at `ddl-auto=validate`.
- Do not leak JPA entities or provider SDK payloads into domain, application, API, or frontend models.
- Do not expose tokens, session IDs, raw OAuth payloads, private repository content, embeddings, SQL details, or stack traces.
- Operational logs do not replace durable audit records.

## 9. Orca Multi-Agent Rules

If Gemini is an Orca worker:

- Use only the assigned Orca worktree and paths.
- One worker owns one file at a time.
- Shared OpenAPI, migrations, lockfiles, build configuration, ADRs, roadmap status, and root instructions require coordinator ownership.
- Settle contracts before backend/frontend consumers and migration ownership before persistence adapters.
- Never undo another worker's changes or broaden the assignment without coordinator approval.
- Do not commit, push, merge, delete worktrees, or rewrite history unless explicitly directed.
- Worker states are `READY`, `IN_PROGRESS`, `BLOCKED`, and `REVIEW_READY`; only the coordinator marks `DONE`.

Stop and escalate for a genuinely required Proposed ADR, a material authoritative contradiction, missing API/schema/security policy, unsupported functionality, ownership conflict, concurrent shared-file changes, unavailable mandatory verification, or potential secret/private-data exposure.

## 10. Verification

Repository-root commands:

```text
npm run backend:test
npm run backend:build
npm run frontend:install
npm run frontend:test
npm run frontend:build
npm run test
npm run verify
```

Use focused tests while iterating and broader relevant verification before handoff. PostgreSQL/Testcontainers tests require Docker. Do not replace PostgreSQL behavior with H2. Java 21 is required. Spring Boot does not automatically load `backend/.env.example`; export required variables in the process environment.

Classify results only as `PASS`, `FAIL`, or `NOT RUN`. Include commands, exact failures, skipped checks, and environment blockers. Never claim an unexecuted check passed.

## 11. Handoff Format

Every final worker report states:

- Objective, requirements, and Accepted ADRs.
- Files created, modified, and deleted.
- Implemented behavior and excluded scope.
- API, data, security, and documentation impact.
- Verification with `PASS`, `FAIL`, or `NOT RUN`.
- Assumptions, deviations, blockers, integration order, and next task.

Do not mark a roadmap milestone complete; provide evidence for coordinator review.

## 12. Recommended Next Work

1. Re-run Docker/PostgreSQL Testcontainers and startup evidence.
2. Define secure server-side GitHub credential storage for private repositories.
3. Confirm repository connection/list/import/synchronization API contracts.
4. Implement repository identity and metadata without queue coupling.
5. Resolve ADR-027 before implementing asynchronous workers or queues.
