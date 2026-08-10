# DevPath Orca Multi-Agent Instructions

## 1. Purpose and Scope

This is the standalone operating guide for every coordinator and worker running DevPath tasks through Orca. An Orca agent must be able to understand the project, its architectural boundaries, and its delivery rules from this file alone.

DevPath is an AI-assisted developer career intelligence platform. It analyzes GitHub and Notion evidence, produces deterministic engineering evaluations, and uses AI to explain results or generate career documents.

## 2. Product Invariants

1. The Rule Engine alone calculates official scores.
2. AI never calculates, changes, guesses, or prioritizes scores, weights, career readiness, company readiness, or deterministic recommendations.
3. Career selection changes the applicable evaluation rules.
4. Company selection changes company-specific evaluation and recommendation policy.
5. Official results must be measurable, reproducible, versioned where required, and traceable to evidence.
6. AI output is untrusted until format, grounding, evidence, and safety validation pass.
7. PostgreSQL is the authoritative structured-data store; caches, projections, embeddings, and LLM context are not sources of truth.
8. User-owned data is isolated by user at collection, storage, retrieval, prompt assembly, generation, export, and publication boundaries.

Stop and escalate rather than weakening an invariant.

## 3. Source Authority

Resolve conflicts in this order:

1. `docs/01_SRS.md`, `docs/02_Rule_Engine.md`, and `docs/03_Career_Path_Engine.md`
2. `docs/07_Domain_Model.md` and `docs/08_System_Data_Model.md`
3. `docs/10_API_Specification.md` and machine-readable contracts
4. `docs/11_Backend_Architecture.md` and `docs/12_Frontend_Architecture.md`
5. Security, observability, test, database, and deployment documents
6. `docs/17_Coding_Standards.md`
7. Module-local conventions
8. Agent preference

`docs/00_Project_Context.md` fixes the product vision. `docs/18_ADR.md` controls technology decisions; only **Accepted** ADRs are authoritative. `docs/19_Roadmap.md` controls delivery order but cannot override requirements or Accepted ADRs. For UI work, `DESIGN.md` defines the visual, interaction, responsive, state, content, and accessibility requirements subordinate to the SRS and frontend architecture. `docs/` is authoritative; `docs_ko/` is a translation aid.

## 4. Repository and Architecture

| Path | Responsibility |
|---|---|
| `backend/` | Java 21, Spring Boot 3.3.5 modular-monolith backend |
| `frontend/` | React 18, TypeScript, Vite, React Query frontend |
| `contracts/` | OpenAPI and machine-readable contracts |
| `docs/` | Authoritative requirements and architecture |
| `docs_ko/` | Korean reference translations |
| `fixtures/` | Deterministic engine test fixtures |
| `scripts/` | Repository command wrappers |

Backend modules follow hexagonal boundaries under `com.devpath.<module>`:

- `domain`: business concepts and invariants; no Spring, JPA, HTTP, provider SDK, logging implementation, or AI SDK dependencies.
- `application`: use cases, transactions, and ports; no controllers or JPA entities.
- `adapter.in`: HTTP, security, event, and job entry points.
- `adapter.out`: persistence and external-provider implementations.
- `config`: framework wiring only.

Controllers call application use cases, JPA entities remain separate from domain models, Flyway owns schema evolution, and application services own transaction boundaries. Provider calls must not run inside long database transactions.

Frontend code is feature-oriented. React Query owns server state. Components never calculate official business results and never call GitHub, Notion, or LLM providers directly. Authentication uses GitHub OAuth2 and an opaque HttpOnly server-managed session; browser storage must never contain provider tokens, session IDs, refresh tokens, or credentials.

## 5. Current Baseline

Implemented:

- Internal `User` and GitHub `ExternalIdentity` foundation.
- GitHub OAuth2 login and PostgreSQL-backed Spring Session.
- JPA adapters with separated persistence models and Flyway V1 migration.
- Current-user, CSRF, logout, and internal-health endpoints.
- React Query session bootstrap and the implemented OpenAPI subset.
- Backend domain, application, security, architecture tests and frontend tests.

Not implemented:

- GitHub repository collection and synchronization, Notion integration, and background jobs.
- Rule, Career, Company, Knowledge, Prompt, AI, Recommendation, Learning, Portfolio, Resume, Interview, Dashboard, and Administration workflows.
- Production deployment and production secret management.

Java 21 compilation, non-container backend tests/build, frontend tests/build, and OpenAPI validation have passed previously. Local PostgreSQL-backed startup has been observed. Re-run Docker/Testcontainers verification before claiming the persistence foundation complete; do not present planned modules as implemented.

## 6. ADR Gates

Accepted baseline includes modular monolith and hexagonal architecture (ADR-001/002), PostgreSQL (003), contract-first REST (005), deterministic/AI separation (006), React architecture (012/013), Java 21/Spring Boot and React/TypeScript (020/021), separated JPA persistence models (024), Flyway (025), OAuth2 with opaque server session (026), testing stack (032), and simplified GitHub Flow (035).

Do not silently choose technologies governed by Proposed ADRs:

| ADR | Area |
|---|---|
| ADR-027 | Background job technology |
| ADR-028 | Vector database |
| ADR-029 | Object storage |
| ADR-030 | AI provider SDK strategy |
| ADR-031 | Observability technology |
| ADR-033 | Deployment platform |
| ADR-034 | Production secrets management |

A Proposed ADR blocks only work that genuinely requires that decision. Unrelated scaffolding or domain work may continue.

## 7. Multi-Agent Roles

| Role | Responsibility | Typical Ownership |
|---|---|---|
| Coordinator | Scope, dependency graph, ownership, decision gates, integration, final evidence | Shared files and merge order |
| Domain/Backend Worker | Domain model, use cases, ports, backend adapters | One module or use case |
| Data/Contract Worker | OpenAPI, schema mapping, migration compatibility | One contract or migration stage |
| Frontend Worker | React Query flow and accessible UI states | One frontend feature |
| Security/Test Worker | Security controls, architecture and integration verification | Focused cross-cutting checks |
| Documentation/Review Worker | Traceability and evidence consistency | Assigned documents only |

Use the fewest agents that create real parallelism. One agent owns each file at a time.

## 8. Coordinator Intake and Dispatch

Before dispatch, define:

```text
Objective:
Requirement IDs:
Roadmap milestone or vertical slice:
Accepted ADRs:
Proposed ADR gates:
Affected modules:
API and data impact:
Security and privacy impact:
Owned files or directories:
Out of scope:
Acceptance criteria:
Verification commands:
Stop conditions:
Integration order:
```

Good tasks cover one measurable capability: one use case, contract group, aggregate behavior, migration stage, provider adapter, frontend flow, security control, or documentation consistency check. Never dispatch vague tasks such as “finish the backend,” “build the AI system,” or “fix security.”

## 9. Ownership and Integration Rules

- Use Orca-managed worktrees; do not create competing raw Git worktrees.
- Contracts precede backend and frontend consumers.
- Migration ownership precedes persistence adapter work.
- Assign one coordinator-owned writer for OpenAPI, Flyway sequence, lockfiles, root build files, ADRs, roadmap status, and agent instruction files.
- Parallelize separate bounded contexts only when they do not share contracts, migrations, lockfiles, or shared-kernel files.
- Workers edit only assigned paths and never undo another worker's changes.
- Workers do not commit, push, merge, delete worktrees, or rewrite history unless explicitly requested.

Default integration order:

1. Confirm requirements and Accepted ADRs.
2. Confirm domain terminology and invariants.
3. Settle API and data contracts.
4. Add migration and persistence boundary where required.
5. Implement domain and application behavior.
6. Implement inbound and outbound adapters.
7. Integrate frontend behavior.
8. Run security, architecture, integration, and contract checks.
9. Update evidence and review roadmap status.

## 10. Mandatory Engineering Rules

- Trace new behavior to an existing requirement ID; otherwise stop for a requirement change.
- Preserve canonical API IDs and method/path pairs from `docs/10_API_Specification.md`.
- Update machine OpenAPI only for implemented endpoints and keep backend/frontend consumers synchronized.
- Add a new immutable Flyway migration for schema changes; never edit an applied migration.
- Use `ddl-auto=validate`, never automatic schema creation or update.
- Keep security backend-authoritative with CSRF protection, restrictive credentialed CORS, and session-fixation protection.
- Never expose provider tokens, private repository content, raw OAuth payloads, embeddings, SQL errors, or stack traces.
- Keep patches focused; avoid speculative features, mass formatting, unrelated refactors, and unapproved dependencies.
- Do not weaken tests, architecture rules, validation, or authorization to make checks pass.

## 11. Status, Blocking, and Handoff

States are `READY`, `IN_PROGRESS`, `BLOCKED`, `REVIEW_READY`, and `DONE`. Workers may declare `REVIEW_READY`; only the coordinator or owner declares `DONE`.

Stop and report when a required ADR is Proposed or missing, authoritative documents materially conflict, a contract/schema/security rule is undefined, scope would invent functionality, another worker owns a required file, a shared migration or contract changed concurrently, required verification is unavailable, or secret/private-data exposure is found.

Every worker handoff must contain:

```text
Task and status:
Requirements and Accepted ADRs:
Files created, modified, or deleted:
Behavior implemented:
API, data, security, and documentation impact:
Tests with PASS, FAIL, or NOT RUN:
Known risks or deviations:
Dependencies and recommended integration order:
Remaining blocker and next task:
```

## 12. Verification Commands

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

For local development, run `.\gradlew.bat bootRun` from `backend/` in PowerShell and `npm run dev` from `frontend/`. Java 21 and environment variables described by `backend/.env.example` are required. Spring Boot does not automatically load `.env`. Docker must run for PostgreSQL/Testcontainers verification.

## 13. Recommended Next Slice

1. Re-run full Docker/PostgreSQL Testcontainers and startup verification.
2. Define server-side encrypted GitHub provider credential handling required for private repository access.
3. Settle the repository connection, list, import, and synchronization contract subset.
4. Implement repository identity and metadata without coupling it to a queue.
5. Resolve ADR-027 before implementing asynchronous queue or worker execution.

Repository discovery and metadata modeling may proceed before ADR-027; asynchronous synchronization execution may not.
