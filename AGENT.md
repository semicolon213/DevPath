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

## 5. Current Implementation Baseline

`AGENTS.md` §4 is the repository's current *documented* implementation and verification baseline. It reports GitHub repository collection/synchronization, deterministic analysis and Rule Engine, Skill Matrix, Career readiness, recommendation/roadmap flows, and Flyway migrations through V20, among other capabilities. It also distinguishes unimplemented Notion, AI generation, company-specific readiness, and other planned workflows.

Do **not** use the former initial-V1 baseline as current status. Before making or reviewing a change, verify the relevant claims against the actual source tree, migrations, contracts, Git status, and fresh test output. If `AGENTS.md` disagrees with the repository, report the discrepancy and use verified code and authoritative specifications rather than guessing. Prior test results are historical evidence, not proof that a new change passes.

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
| Coordinator (Claude Code preferred, subject to Orca support) | Scope, dependency graph, ownership, decision gates, integration, final evidence | Shared files and merge order |
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

## 13. Next Work Selection

Choose the next capability from the current `docs/19_Roadmap.md`, confirmed requirements, Accepted ADRs, and actual repository status. Do not reuse historical next-slice recommendations without verification. A Proposed ADR blocks only work that requires that decision.

## 14. Codex Implementation → Claude Independent QA

This section governs the **Orca-managed Codex/Claude workflow**. It does not assert that Orca automatically discovers `AGENT.md`, reuses an existing terminal session, or implements a state machine merely because this document exists. The coordinator must explicitly load these instructions, inspect the installed Orca orchestration skill, and verify supported CLI/API operations before starting workers. If orchestration capabilities are missing, stop and report rather than invent commands or silently substitute manual work.

### Roles and isolation

- **Coordinator:** owns task scope, worktree strategy, handoff, evidence, QA verdict handling, retry count, integration, and user-facing completion. A coordinator session must not simultaneously be treated as an independent reviewer.
- **Codex implementation worker:** owns the assigned capability's implementation, relevant tests, and fixes. Follow `AGENTS.md` and the scoped task specification.
- **Claude QA worker:** independently checks requirements, diff, tests, security, architecture, and regressions. Follow `CLAUDE.md` in **Independent QA Mode**. It must not modify application source or shared configuration during QA. Enforce read-only permissions at the worker/tool/worktree level where supported; instructions alone are not a permission boundary.
- Use separate worker contexts. A QA worker must see the **same immutable commit/diff or explicitly identified integrated snapshot** that is being reviewed. Do not review a stale worktree or an unintegrated branch by accident.

### Capability lifecycle

1. **Intake:** coordinator defines one coherent end-to-end capability, requirement IDs, acceptance criteria, exclusions, dependencies, accepted/proposed ADR gates, ownership, verification commands, and stop conditions.
2. **Dispatch:** coordinator creates/assigns an Orca-managed implementation task to Codex using commands verified against the installed orchestration skill. Record task/worker IDs and target worktree/revision.
3. **Implementation:** Codex checks existing changes, implements only owned scope, runs targeted then relevant broader tests, performs the repository's existing `@ponytail-review` step when available, and hands off exact changed files, diff/revision, tests, and risks. An unavailable `@ponytail-review` must be reported, not claimed as run.
4. **Review readiness:** coordinator checks worker completion **from an actual terminal state/event**, validates ownership and snapshot visibility, and marks the task `REVIEW_READY`. Do not infer completion from elapsed time or chat prose alone.
5. **Independent QA:** coordinator dispatches Claude with the original requirements, acceptance criteria, exact implementation snapshot, changed-file list, verification evidence, and read-only scope. Claude returns exactly one of `PASS`, `FAIL`, `BLOCKED`, with supporting evidence.
6. **PASS:** coordinator verifies QA evidence and required acceptance criteria, then dispatches a separate Claude documentation-maintenance task if documentation requires updates. Do not declare `DONE` or select the next capability until documentation consistency is checked and coordinator approval is complete. A Claude PASS is necessary but not sufficient for final acceptance.
7. **FAIL:** coordinator assigns actionable findings to Codex, preserving task scope and ownership. Codex fixes and reruns affected tests; Claude reviews the **new** snapshot independently. Allow at most **three QA cycles total**, counting the first review. After the third FAIL, stop and escalate with unresolved findings; never loop indefinitely.
8. **BLOCKED:** pause the capability, record missing environment/evidence/decision, and request resolution. Do not turn BLOCKED into PASS or automatically start the next feature.

### Required handoff protocol

Implementation handoff:

```text
TASK_ID:
STATUS: REVIEW_READY | BLOCKED
REQUIREMENTS:
ACCEPTANCE_CRITERIA:
SNAPSHOT: <commit, diff, or worktree revision identifier>
FILES_CHANGED:
IMPLEMENTATION_SUMMARY:
TESTS: <exact command and PASS | FAIL | NOT RUN>
API_DATA_SECURITY_IMPACT:
KNOWN_RISKS:
BLOCKERS:
```

Independent QA handoff:

```text
TASK_ID:
SNAPSHOT:
STATUS: PASS | FAIL | BLOCKED
REQUIREMENTS_CHECKED:
TESTS: <exact command and PASS | FAIL | NOT RUN>
FINDINGS: <severity, file/line, evidence, reproduction>
REQUIRED_FIXES:
UNVERIFIED_ITEMS:
```

### Safety and completion rules

- Preserve user changes and enforce one writer per file; review must not race against an implementation still changing files.
- Never automatically commit, push, merge, deploy, remove worktrees, change ADR status, or update roadmap completion without explicit authorization and evidence.
- Do not run destructive tests, real provider mutations, or production-facing operations without explicit approval.
- If worker completion events, worker type selection, read-only isolation, or revision handoff are unsupported, state the limitation and obtain a different verified mechanism before claiming automation.
- A feature advances only after verified QA PASS, required post-PASS documentation synchronization, and coordinator acceptance; report exact tests not run and any residual risk.


## 15. Post-PASS Claude Documentation Maintenance

Documentation maintenance is a **separate Orca task after independent QA PASS**, never part of the read-only QA dispatch.

1. The coordinator validates the QA verdict, task/dispatch IDs, implementation snapshot, acceptance evidence, and remaining risks. `FAIL` and `BLOCKED` must not trigger documentation edits.
2. The coordinator identifies Markdown files affected by the verified implementation, and creates a **documentation-only** task with exact allowed paths, source snapshot, requirement IDs, scope, and acceptance criteria. If no Markdown changes are necessary, record `DOCS_NOT_NEEDED` with a reason and proceed to final review.
3. Dispatch Claude in **Documentation Maintenance Mode**, preferably as a separate worker/dispatch from the QA reviewer. The coordinator must explicitly grant ownership of each permitted file.
4. Claude updates only assigned Markdown files using verified code, migrations, API contracts, tests, and authoritative specifications. Keep implemented/planned status accurate; preserve product invariants and document precedence.
5. Changes to `AGENT.md`, `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `DESIGN.md`, `docs/18_ADR.md`, `docs/19_Roadmap.md`, or authoritative requirement documents require explicit coordinator/owner approval and assigned ownership. Do not silently approve Proposed ADRs, invent requirements, or declare roadmap milestones complete.
6. The coordinator verifies the documentation diff, checks for unauthorized non-Markdown/source changes, resolves conflicts with other workers, and confirms that documentation reflects the **same accepted implementation snapshot**. If changes are invalid, reassign documentation correction without silently changing the QA verdict. If documentation reveals a code/requirement defect, stop and reopen implementation/QA rather than editing code under the documentation task.
7. Only after the documentation task is accepted (or documented as unnecessary) may the coordinator mark the capability `DONE` and proceed.

Documentation handoff:

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

Documentation workers must not modify source code, tests, migrations, machine-readable OpenAPI, dependency manifests, or lockfiles. A behavioral instruction is not an OS permission boundary; enforce file ownership and verify the diff. Never automatically commit, push, merge, or deploy.


## 16. Claude-First Coordinator and Token Budget Policy

### Default role allocation

- **Claude Coordinator (preferred):** intake, targeted repository inspection, requirement/ADR lookup, task planning, Orca Run/Task/Dispatch management, worker completion/event verification, QA triage, documentation dispatch, final review and concise user report. This preference is conditional on verifying that the installed Orca version supports a Claude coordinator; never assume a session can take over an existing Run.
- **Codex implementation worker:** implement a tightly scoped, self-contained change, execute relevant tests, and address actionable QA FAIL findings. Do not use Codex as the default planner, coordinator, documentation editor, or independent QA reviewer.
- **Claude independent QA worker:** separate Claude session/dispatch from the Coordinator, read-only review of the exact implementation snapshot. The Coordinator must not self-approve its own work.
- **Claude documentation worker:** separate post-PASS task, limited to explicitly owned Markdown files, followed by Coordinator diff review.
- Keep worker count minimal. Do not start a second worker merely to reduce tokens if it creates a conflict or duplicates exploration.

### Context and budget discipline

1. Coordinator prepares a compact implementation packet: task objective, requirement IDs, Accepted ADRs, relevant file paths and exact symbols, expected behavior, out-of-scope constraints, acceptance criteria, tests, and known existing changes. Include sufficient source references for the worker to verify claims; do not paste entire documents or the full repository.
2. Codex worker reads only relevant files and follows the packet. Do not repeat broad architecture discovery already completed by the Coordinator unless evidence conflicts.
3. Prefer targeted tests first; run broader tests/build only at agreed gates. Do not repeat passing full suites on unchanged snapshots without a reason.
4. Summarize findings once per handoff, with exact paths/lines and snapshot identifiers. Avoid repeated status polling, verbose duplicated summaries, and redundant worker restarts.
5. A usage-limit warning or quota exhaustion is a checkpoint, not permission to change scope or silently replace the implementation agent. Preserve Run/Task/Dispatch state, report remaining work and `BLOCKED` when progress cannot safely continue.
6. Do not assume Claude and Codex subscriptions share quotas or that a Claude Coordinator can use Codex's subscription. Verify each CLI session independently.

### Codex worker prompt delivery and submit recovery

On Windows, Orca 1.4.226 with Codex CLI 0.162.1 can leave a large `worker-start` prompt pasted but unsubmitted in the Codex composer as `[Pasted Content N chars]` (upstream stablyai/orca #11343, #14822). Apply these rules until an Orca release verifiably fixes it:

1. Keep each Codex `worker-start --spec` under about 800 characters: role, absolute path of a spec file, and completion condition. Put the detailed §8 packet in that spec file, stored outside the repository where the worker can read it. Never create spec files in tracked DevPath paths.
2. If `worker-start` returns `ready` or an observed turn start, send no further terminal input.
3. Send one bare Enter (`orca terminal send --terminal <handle> --enter --json`) only when all checks pass:
   1. the `worker-start` receipt is `outcome_unknown` with stage `turn_start_unobserved`;
   2. `worker-show` reports the Dispatch `pending`, no observed turn start, and an `assigneeHandle` equal to the receipt's agent terminal (re-read the handle from the Dispatch);
   3. `terminal read --screen` shows the Codex composer (`draft` or the `› ` line) holding only one or more `[Pasted Content N chars]` markers;
   4. the screen shows no `Working`, `esc to interrupt`, confirmation, selection, or permission prompt (the static `permissions: <mode>` header is not a prompt);
   5. two screen reads 5 seconds apart are identical;
   6. Dispatch state and screen are re-checked immediately before sending.
4. Do not use `terminal wait --for tui-idle` as a recovery precondition: Codex reports no agent status before its first turn, so the wait times out in exactly this state.
5. After the Enter, confirm that the marker cleared and `Working` appeared, then require a `worker_done` with matching Task and Dispatch IDs. Orca may not record `turn_started` after a manual Enter; screen state plus `worker_done` is the evidence.
6. If any check fails, the screen is unexpected, or no turn starts, stop with `BLOCKED`. Never repeat Enter, resend the prompt, or patch Orca.
7. `scripts/orca/codex-submit-guard.mjs <worker-start-receipt.json>` implements checks 3–6. It is a dry run by default; only `--execute` sends, at most one Enter per Dispatch, guarded by an exclusive per-Dispatch marker (exit 0: `RECOVERED`, `NO_ACTION`, or `WOULD_SEND`; exit 2: `BLOCKED`). Re-validate it after every Orca or Codex upgrade.

### Coordinator migration safety

- Before moving from a Codex Coordinator to a Claude Coordinator, inspect the installed `orca skills get orchestration --full`, current Run/Task/Dispatch state, pending deliveries, ownership and worker terminals.
- If a Run is active, do not silently transfer ownership or abandon outstanding deliveries. Either finish the current lifecycle or use an explicitly documented Orca handoff procedure.
- Start a fresh Run under Claude only after the installed CLI confirms the supported coordinator lifecycle and the user approves the transition. Preserve old Run history and existing user worktrees.
- If Claude coordinator support cannot be confirmed, keep the existing supported Coordinator and reduce Codex consumption through narrowly scoped worker prompts; do not invent CLI commands or unsupported role flags.

### Final completion gate

Codex implementation evidence → separate Claude QA PASS → separate Claude Markdown maintenance DONE (or explicitly documented no-change-needed) → Coordinator validates exact snapshots, tests, diff and protected files → final acceptance. QA PASS remains recorded if documentation is BLOCKED, but the overall capability must not be reported DONE. Never auto-commit, push, merge, or deploy.
