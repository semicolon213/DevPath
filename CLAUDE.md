# DevPath Instructions for Claude Code

## 1. Mission

This is Claude Code's standalone repository instruction file. Do not assume that another agent guide has been read.

DevPath is an AI-assisted developer career intelligence platform. It analyzes GitHub and Notion evidence, evaluates technical growth through deterministic engines, and uses AI only for grounded explanations, recommendations, and generated career documents.

## 2. Immutable Product Rules

- The Rule Engine alone calculates official scores.
- AI never calculates, modifies, guesses, or ranks scores, weights, readiness, recommendation priority, or deterministic business results.
- Career choice changes the applicable evaluation rules; company choice changes company-specific evaluation and recommendation policy.
- Every official result is measurable, reproducible, versioned where required, and traceable to evidence.
- AI output remains untrusted until format, grounding, evidence, and safety validation pass.
- PostgreSQL is the authoritative structured-data store. Caches, vectors, projections, and LLM context are not sources of truth.
- All user-owned data is isolated by user and authorization is enforced before retrieval, prompt assembly, generation, export, or publication.

Stop and report any requested change that violates these rules.

## 3. Authoritative Sources

When sources disagree, apply this precedence:

1. `docs/01_SRS.md`, `docs/02_Rule_Engine.md`, `docs/03_Career_Path_Engine.md`
2. `docs/07_Domain_Model.md`, `docs/08_System_Data_Model.md`
3. `docs/10_API_Specification.md`, then machine-readable contracts
4. `docs/11_Backend_Architecture.md`, `docs/12_Frontend_Architecture.md`
5. Security, observability, testing, database, and deployment specifications
6. `docs/17_Coding_Standards.md`
7. Existing module conventions
8. Personal implementation preference

`docs/00_Project_Context.md` defines the vision. Only **Accepted** decisions in `docs/18_ADR.md` are selected technologies. `docs/19_Roadmap.md` controls sequence but does not override requirements. For UI work, `DESIGN.md` is the repository-level visual, interaction, responsive, state, content, and accessibility guide subordinate to the SRS and frontend architecture. English documents in `docs/` are authoritative; `docs_ko/` is a translation aid.

## 4. Technical Architecture

| Area | Baseline |
|---|---|
| Backend | Java 21, Spring Boot 3.3.5, modular monolith, hexagonal boundaries |
| Frontend | React 18, TypeScript, Vite, TailwindCSS, React Query |
| Data | PostgreSQL, Spring Data JPA, separate persistence/domain models, Flyway |
| Authentication | GitHub OAuth2, opaque HttpOnly server session, CSRF and restrictive CORS |
| Contracts | Contract-first REST; canonical specification in `docs/10_API_Specification.md` |
| AI boundary | Structured engine outputs in; validated explanations and drafts out; no scoring logic |

Repository paths are `backend/`, `frontend/`, `contracts/`, authoritative `docs/`, translated `docs_ko/`, deterministic `fixtures/`, and command wrappers in `scripts/`.

Backend packages under `com.devpath.<module>` use:

- `domain` for pure business concepts and invariants.
- `application` for use cases, transactions, and ports.
- `adapter.in` for HTTP, security, event, and job entry points.
- `adapter.out` for persistence and provider integrations.
- `config` for framework wiring.

Domain code must not depend on Spring, JPA, HTTP, logging implementations, provider SDKs, or AI SDKs. Controllers never access JPA repositories directly. Application services own transactions. External calls remain outside long database transactions. Flyway exclusively owns schema evolution.

Frontend code is feature-oriented. React Query owns server state. `GET /api/v1/users/me` is the browser-session bootstrap. A `401` means anonymous; network failure is a different state. Components do not calculate official results or call providers directly.

## 5. Current Implementation State

Implemented today:

- Internal `User` with GitHub `ExternalIdentity` linking.
- GitHub OAuth2 login and PostgreSQL-backed Spring Session.
- Separated JPA persistence models and Flyway V1 identity/session schema.
- Current-user, CSRF, logout, and internal-health endpoints.
- React Query session bootstrap and OpenAPI subset for implemented operations.
- Backend domain/application/security/architecture tests and frontend tests.

Not yet implemented:

- GitHub repository collection/synchronization, Notion integration, and background job runtime.
- Rule, Career, Company, Knowledge, Prompt, AI, Recommendation, Learning, Portfolio, Resume, Interview, Dashboard, and Administration workflows.
- Production deployment and production secret management.

Prior checks passed for Java 21 compilation, non-container backend tests/build, frontend tests/build, and OpenAPI validation. Local PostgreSQL startup has been observed. Full Docker/Testcontainers evidence must be rerun before declaring persistence complete.

## 6. Decision Gates

Accepted architecture includes ADR-001/002, 003, 005, 006, 012/013, 020/021, 024, 025, 026, 032, and 035.

Do not implement a technology choice that depends on these Proposed ADRs without an explicit decision: ADR-027 background jobs, ADR-028 vector database, ADR-029 object storage, ADR-030 AI SDK strategy, ADR-031 observability technology, ADR-033 deployment platform, and ADR-034 production secrets. A Proposed ADR blocks only its affected area; unrelated work may continue.

## 7. Claude Working Method

Before editing:

1. Inspect Git status and preserve user or agent changes.
2. Identify requirement IDs, roadmap slice, relevant Accepted ADRs, affected modules, and acceptance criteria.
3. Read only the authoritative chapters needed for the task.
4. Search for existing patterns before creating abstractions.
5. State assumptions and stop on unresolved decisions that genuinely block implementation.

While editing:

- Make the smallest coherent patch that solves the root cause.
- Prefer explicit domain language over generic abstractions.
- Do not invent routes, fields, roles, provider behavior, business rules, or future functionality.
- Do not perform unrelated refactors, dependency upgrades, mass renames, or formatting sweeps.
- Do not alter Accepted ADR rationale or mark Proposed ADRs as settled.
- Never commit secrets, OAuth credentials, private keys, tokens, real user data, or generated secret files.
- Add tests at the nearest appropriate level and never weaken security or tests to obtain a pass.

## 8. Contract, Data, and Security Rules

- Preserve canonical API IDs and normalized method/path combinations.
- Change `contracts/openapi/devpath-openapi.yaml` only for implemented endpoints and synchronize backend/frontend consumers.
- Trace new behavior to an existing requirement ID; otherwise request a specification change.
- Add a new immutable Flyway migration for schema changes; never edit an applied migration.
- Keep runtime schema mode at `ddl-auto=validate`.
- Keep provider payloads and SDK types out of domain, application contracts, and public APIs.
- Never expose provider tokens, session IDs, credentials, private repository content, embeddings, SQL errors, or stack traces.
- Treat operational logs separately from durable audit records.

## 9. Multi-Agent and Orca Conduct

When Claude runs as an Orca worker:

- Work only in the assigned Orca worktree and owned paths.
- Treat OpenAPI, migrations, lockfiles, root build files, ADRs, roadmap status, and agent guides as coordinator-owned unless assigned.
- Do not overwrite another worker's changes or guess an unsettled contract.
- Contract work precedes dependent backend/frontend work; migration ownership precedes persistence work.
- Do not commit, push, merge, delete worktrees, or rewrite history unless explicitly requested.
- Use `READY`, `IN_PROGRESS`, `BLOCKED`, and `REVIEW_READY`; only the coordinator declares `DONE`.

Stop for an actual blocking Proposed ADR, authoritative contradiction, undefined API/schema/security contract, scope invention, ownership conflict, concurrent migration/contract change, unavailable required verification, or possible secret/private-data exposure.

## 10. Verification and Handoff

From the repository root:

```text
npm run backend:test
npm run backend:build
npm run frontend:install
npm run frontend:test
npm run frontend:build
npm run test
npm run verify
```

Use targeted checks first, then the broader relevant suite. Docker is required for PostgreSQL/Testcontainers tests; never replace required PostgreSQL behavior with H2. Spring Boot does not automatically load `backend/.env.example`; required values must be exported into the process environment.

Report every check as `PASS`, `FAIL`, or `NOT RUN` with the exact command and reason. A final handoff includes objective, requirements, files, behavior, API/data/security impact, verification, deviations, blockers, integration order, and next task. Never declare a roadmap milestone complete without coordinator review.

## 11. Near-Term Delivery Order

1. Re-run full Docker/PostgreSQL persistence verification.
2. Define secure server-side GitHub credential handling for private repository access.
3. Settle repository connection/list/import/sync contracts.
4. Implement repository identity and metadata independently of queue technology.
5. Resolve ADR-027 before asynchronous queue/worker execution.
