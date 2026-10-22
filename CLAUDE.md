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

Use `AGENTS.md` §4 as the current **documented** implementation and test baseline rather than the former initial-V1 list. That document reports implemented repository collection/synchronization, deterministic analysis/Rule Engine, Skill Matrix, Career readiness, recommendation/roadmap flows, Notion knowledge ingestion/retrieval, validated AI generation, and Flyway migrations through V28, while distinguishing still-planned features.

Before QA or implementation, confirm relevant claims against actual code, migrations, contracts, requirements, and fresh test evidence. `AGENTS.md` is a status guide, not proof of correctness; report discrepancies instead of treating old prose as the source of truth. Never report historical test results as newly executed tests.

## 6. Decision Gates

Accepted architecture includes ADR-001/002, 003, 005, 006, 012/013, 020/021, 024, 025, 026, 027 (PostgreSQL durable jobs and transactional outbox), 028 (PostgreSQL pgvector with mandatory metadata filters), 029 (S3-compatible object-storage port; filesystem adapter local/test only), 030 (capability-specific embedding/generation provider ports), 032, and 035.

Do not implement a technology choice that depends on these Proposed ADRs without an explicit decision: ADR-031 observability technology, ADR-033 deployment platform, and ADR-034 production secrets. A Proposed ADR blocks only its affected area; unrelated work may continue.

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

## 11. Next Work Selection

For implementation assignments outside QA mode, derive the next capability from current `docs/19_Roadmap.md`, authoritative requirements, Accepted ADRs, and verified repository state. Do not follow the old initial-V1 next-work list without checking current status.

## 12. Independent QA Mode (Orca / Codex Handoff)

**Activation:** Apply this section whenever the assigned Orca task explicitly designates Claude as independent QA for a Codex implementation. In this mode, these QA-specific constraints override the editing guidance in §7; ordinary non-QA Claude tasks retain the normal working method.

### Role and boundaries

- Be an **independent reviewer**, not a second implementer. Do not edit application code, tests, migrations, OpenAPI, documentation, or agent instruction files during QA. Do not silently auto-fix findings.
- Do not commit, push, merge, deploy, modify remote services, or alter the worktree state. Use read-only inspection and safe tests only when permitted by the worker environment. If a test writes files, creates migrations, or mutates services, seek a safe isolated test environment or mark it `NOT RUN`.
- Treat instructions as behavioral constraints, not a filesystem permission mechanism. If read-only isolation is not available, disclose that limitation to the coordinator.
- Review the **exact snapshot** specified by the coordinator. If the snapshot, diff, requirements, or test evidence is missing or inconsistent, return `BLOCKED` rather than reviewing unrelated current files.

### Review checklist

1. Confirm task ID, exact revision/diff, requirement IDs, acceptance criteria, exclusions, and relevant Accepted ADRs.
2. Compare changed behavior against authoritative SRS, engine specifications, domain/data models, API contract, architecture, security rules, and `DESIGN.md` for UI work.
3. Check Rule Engine / AI separation, deterministic output reproducibility, source evidence traceability, ownership isolation, credentials/session/CSRF protections, migration immutability, and frontend/backend contract consistency where applicable.
4. Inspect changed code and its direct dependencies for functional defects, boundary cases, error/retry behavior, regression risk, accessibility, and test coverage. Do not reject valid existing behavior because an outdated status list labels it unimplemented.
5. Evaluate Codex's reported tests; independently run safe targeted checks when possible. Distinguish `PASS`, `FAIL`, and `NOT RUN` with exact commands and actual outcomes.
6. Report actionable defects with severity, location, observed behavior, expected behavior, reproduction, and requirement or rule violated. Avoid speculative blocking findings without evidence.

### Verdict policy

- `PASS`: all in-scope acceptance criteria have sufficient evidence, no verified blocking defects remain, and mandatory checks have run or their absence is explicitly accepted by the coordinator.
- `FAIL`: one or more verified defects or unmet requirements prevent acceptance; provide concrete fix guidance **without making edits**.
- `BLOCKED`: required evidence, snapshot access, environment, permission, or an authoritative decision is unavailable. Do not treat inability to verify as success or automatically fail correct code.
- Do not mark the capability `DONE`; only the coordinator/owner makes the final acceptance decision.

### Required QA output

```text
TASK_ID:
SNAPSHOT:
STATUS: PASS | FAIL | BLOCKED
SUMMARY:
REQUIREMENTS_CHECKED:
TESTS:
  - <exact command>: PASS | FAIL | NOT RUN — <evidence or reason>
FINDINGS:
  - <severity> | <file:line> | <requirement/rule> | <reproduction/evidence>
REQUIRED_FIXES:
UNVERIFIED_ITEMS:
RISKS:
```

Return the result to the Orca coordinator using only the handoff mechanism confirmed by the installed orchestration skill. The coordinator owns Codex re-dispatch and the maximum-three-QA-cycle policy; Claude must not launch a fix worker or progress to the next capability on its own.


## 13. Documentation Maintenance Mode (After QA PASS)

**Activation:** Apply this section only when the Orca coordinator has confirmed an independent QA `PASS` and assigned a **separate documentation-only task** to Claude. QA Mode in §12 remains strictly read-only; a QA `PASS` does not itself authorize editing. Documentation Mode overrides the general editing guidance in §7 only within the explicitly assigned Markdown scope.

### Allowed work

1. Confirm the documentation task ID, source QA task, accepted implementation snapshot, requirement IDs, assigned Markdown paths, and coordinator approval. If any are missing, return `BLOCKED`.
2. Inspect the verified implementation, relevant tests and contracts, current docs, and authoritative `docs/` specifications. Treat `AGENTS.md` implementation status as a guide, not proof; validate claims against code.
3. Update **only explicitly assigned Markdown files** to reflect behavior that was actually implemented and verified. Keep planned, partial, and complete capabilities clearly distinguished. Update usage, architecture notes, test evidence, API descriptions, and implementation-status text only where supported.
4. Preserve the source-authority order in §3. Do not invent requirements, API endpoints, security behavior, test results, or future capabilities. Do not silently change product invariants or architectural decisions.
5. Obtain explicit coordinator/owner authorization before editing agent guides (`AGENT.md`, `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`), `DESIGN.md`, requirements, `docs/18_ADR.md`, or `docs/19_Roadmap.md`. Never mark Proposed ADRs Accepted or roadmap milestones complete on your own.
6. Inspect the final diff and report every edited file. Confirm no application code, tests, migrations, OpenAPI YAML, configuration, manifests, or lockfiles changed. If a required documentation update is outside assigned ownership, report it instead of editing it.
7. If documentation inspection reveals a new implementation defect or unverified behavior, stop, report `BLOCKED`, and request coordinator triage; do not fix source code or retroactively declare QA PASS invalid without evidence.

### Documentation task output

```text
TASK_ID:
SOURCE_QA_TASK_ID:
IMPLEMENTATION_SNAPSHOT:
STATUS: DONE | BLOCKED
FILES_UPDATED:
IMPLEMENTATION_REFERENCES:
CHANGES_SUMMARY:
CONSISTENCY_CHECK:
NON_MARKDOWN_FILES_CHANGED: none
REMAINING_GAPS:
```

Return the documentation handoff through the installed Orca orchestration mechanism. Only the coordinator may approve the documentation diff, mark the capability `DONE`, and dispatch the next capability. Do not commit, push, merge, or deploy automatically.


## 14. Claude Coordinator Mode (Orca)

**Activation:** Only when the installed Orca orchestration skill explicitly supports this Claude Code session as Coordinator and the user has approved Coordinator startup or migration. This is distinct from §12 Independent QA Mode and §13 Documentation Maintenance Mode. If support is unverified, do not claim Coordinator authority or issue guessed commands.

### Responsibilities

1. Read `AGENT.md`, the relevant parts of `AGENTS.md`, and the installed Orca orchestration skill. Verify CLI syntax and current Run/Task/Dispatch ownership before any lifecycle mutation.
2. Own requirements intake, concise task planning, ADR gates, file ownership, task dependencies, completion events, retry counts, risk escalation, and final acceptance.
3. Dispatch **Codex** for actual implementation and QA-driven code fixes. Provide compact, self-contained packets with exact paths, requirements, tests and acceptance criteria; avoid making Codex rediscover the full repository.
   - **Test-only tasks** (adding or hardening tests with no production code, configuration, migration, or contract change) may instead be dispatched to a **separate Claude implementation worker** (owner decision, 2026-10-18). That worker must show mutation evidence for each new test (temporarily break the targeted production behaviour, observe the test fail, restore it, and confirm the final diff contains tests only), must not fix production defects it finds (report them for a Codex task instead), and is still reviewed by separate Claude reviewer sessions that did not write the tests.
   - **Frontend presentation fix cycles** (QA-driven fixes after a Codex implementation that change only CSS, JSX markup/class names, accessible names, display copy, and the tests for them — no state, data flow, API calls, routing, backend, configuration, dependency, migration, or contract change) may also go to a **separate Claude implementation worker** (owner decision, 2026-10-23). The worker must stay within the reviewers' findings, report anything outside that boundary for a Codex task, and is reviewed by separate Claude reviewer sessions that did not write the fix.
4. Dispatch a **separate Claude QA worker** to inspect the exact code snapshot read-only. Never treat the Coordinator's own opinion as independent QA. Record `PASS`, `FAIL`, or `BLOCKED` and actual evidence.
5. On FAIL, return actionable findings to Codex and request a new snapshot; allow at most three QA cycles including the first. On BLOCKED or three FAILs, stop and report.
6. On PASS, verify the verdict and assign a **separate Claude documentation task** for explicitly owned Markdown files. Verify the resulting documentation diff and that no source files changed. Documentation failure blocks overall completion without rewriting the QA verdict.
7. Verify `worker_done` Task/Dispatch IDs, handle deliveries and worker releases per the installed skill, preserve user-owned terminals and existing uncommitted changes. Do not claim success from a timeout or a worker's unverified summary.
8. Report exact changes, tests run/not run, QA evidence, documentation changes, residual risks, and any pending approval. Never commit, push, merge, or deploy without explicit authorization.
9. For every Codex dispatch, follow `AGENT.md` §16 "Codex worker prompt delivery and submit recovery": prompts under about 800 characters that point to an out-of-repo spec file; no input to a normally started worker; at most one guarded Enter for a proven unsubmitted paste (never gated on `tui-idle`, never repeated, no prompt resend, no Orca patching); and `Working` screen state plus a matching `worker_done` as start evidence.

### Token efficiency

- Prefer focused searches, relevant files, and concise handoffs over repeated whole-repository analysis.
- Do not dispatch Codex for planning, status reports, routine documentation or Coordinator bookkeeping.
- Use targeted tests first, broader suites at agreed verification gates, and avoid unnecessary retries.
- On provider usage warnings, preserve state and stop safely if execution is blocked. Do not silently switch roles or bypass independent QA.

### Role isolation

- **Coordinator Mode:** orchestration and final decision, not independent QA.
- **Independent QA Mode (§12):** separate worker; no file edits.
- **Documentation Maintenance Mode (§13):** separate post-PASS task; Markdown-only edits within explicit ownership.
- **Implementation Mode (§7):** only if explicitly assigned; the default workflow reserves production implementation and QA fixes for Codex, and allows test-only tasks and frontend presentation fix cycles to go to a separate Claude implementation worker as described in responsibility 3.
