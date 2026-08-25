# DevPath MVP Release Evidence

## 1. Purpose

This document records implementation evidence for the M34 MVP Completion Gate. It is an evidence index and release
handoff aid, not a requirements source and not approval that the milestone is complete. Requirements and engine
semantics remain controlled by `01_SRS.md`, `02_Rule_Engine.md`, and `03_Career_Path_Engine.md`.

Evidence date: 2026-08-25.

## 2. Gate Evidence

| Criterion | Status | Evidence | Remaining verification |
|---|---|---|---|
| User can authenticate | PASS | `OAuthLoginApplicationServiceTest`, `IdentitySecurityIntegrationTest`, JDBC-session configuration, and the 2026-08-25 live local GitHub OAuth journey in section 4 | Owner review |
| User can select career target | PASS | `UserProfileApplicationServiceTest`, `ProfilePanel.test.tsx`, career API/UI tests | Owner review |
| User can connect GitHub safely | PARTIAL | GitHub application/adapter/security tests, AES-GCM credential test, token-redaction behavior | Live provider permission and revocation exercise |
| User can register repository | PASS | Repository application/security tests and repository UI tests | Owner review |
| Repository syncs asynchronously | PASS | Repository synchronization lifecycle, retry, idempotency, security, browser-journey tests, and live provider job `f5d15d93-eced-4402-9ae1-2c96ad7b0d06` | Owner review |
| Immutable snapshot is created | PASS | `RepositorySnapshotTest` and PostgreSQL persistence tests | Owner review |
| Features are extracted | PASS | Evidence extractor and technology detector tests with deterministic fixtures plus the live `semicolon213/DevPath` technology/evidence result | Owner review |
| Rule Engine produces versioned results | PASS | Rule golden fixtures, deterministic engine/application/persistence tests | Owner review of fixture coverage |
| Skill Matrix is created | PASS | Deterministic builder, application, persistence, security, UI, and browser tests | Owner review |
| Career Engine produces readiness/gaps | PASS | Deterministic career golden tests, policy persistence, security, UI, and browser tests | Owner review |
| Recommendations are produced | PASS | Deterministic recommendation tests, owner-scoped recommendation history/detail/evidence and roadmap history/archive tests, and PostgreSQL persistence tests | Owner review |
| Historical analyses can be compared | PASS | Owner-scoped comparison API/application/persistence tests and the Chromium side-by-side comparison journey; the UI displays stored official results without deriving a new score or delta | Owner review |
| Historical Skill Matrices can be compared | PASS | API-SKL-003 owner/security/persistence tests and the Chromium side-by-side skill journey; only stored score, level, confidence, evidence, and version values are displayed | Owner review |
| Skill evidence is traceable | PASS | API-SKL-004/005 owner/security/persistence tests and the Chromium skill-detail journey from the current Matrix into normalized snapshot evidence | Owner review |
| Career growth history is navigable | PASS | API-CAR-004/005/006 security/persistence coverage and the Chromium current readiness → recommendation set → immutable historical readiness → Skill Gap/skill-evidence journey | Owner review |
| Frontend displays result | PASS | Vitest feature suite plus Playwright Chromium journey and axe scans | Manual assistive-technology review |
| Authorization is enforced | PASS | Authentication/CSRF tests, owner-ID delegation assertions, cross-owner analysis and recommendation persistence tests | External security review |
| Critical telemetry exists | PARTIAL | Safe request/correlation propagation, response support IDs, bounded request-completion logs, durable audit records | ADR-031 technology decision, metrics/traces/backend verification |
| Critical tests pass | PASS | `npm run verify:mvp`; latest results must be attached to the review | CI execution is not yet configured |
| Deployment smoke passes | PARTIAL | Local PostgreSQL/Testcontainers build and local health smoke | ADR-033/034, staging deployment and smoke |
| Known limitations documented | PASS | Section 4 of this document and roadmap exclusions | Owner acceptance |

## 3. Reproducible Local Verification

Prerequisites:

- Java 21, Node.js supported by `frontend/package.json`, and a running Docker-compatible engine.
- Playwright Chromium installed once with `cd frontend && npx playwright install chromium`.
- Network access for the pinned Redocly CLI when it is not already present in the npm cache.

Run from the repository root:

```text
npm run verify:mvp
```

The command runs a clean backend build with the full JUnit/architecture/Testcontainers suite, a clean frontend install,
Vitest, Playwright Chromium with axe, the production frontend build, npm audit, and OpenAPI lint. Environment-gated tests
that require an explicit `DEVPATH_DB_URL` remain reported as skipped unless that variable is supplied.

Latest local result on 2026-08-25:

- Backend full suite: PASS; 124 tests, 122 passed, 0 failed, 2 environment-gated skips.
- PostgreSQL Testcontainers suites: PASS with Docker API compatibility set to 1.44.
- Frontend Vitest: PASS; 34 files and 73 tests.
- Playwright Chromium/axe: PASS; 5 browser journeys.
- Frontend production build: PASS.
- Frontend npm audit: PASS; 0 vulnerabilities.
- OpenAPI lint: PASS with 0 errors and 8 non-blocking pre-existing recommendation warnings.
- Local PostgreSQL-backed startup smoke on port 8080: PASS; Flyway validated and applied 20 migrations, health returned 200,
  and the same bounded request/correlation identifiers appeared in response headers and the completion log.

## 4. Live Local Provider Journey

Run on 2026-08-25 against the latest local frontend on port 5173, backend on port 8080, PostgreSQL 16.14, and the
real GitHub provider:

- PASS: the browser followed the DevPath GitHub OAuth entry point and returned to an authenticated opaque server
  session as the expected internal user; no provider token or session identifier was exposed in the UI or logs.
- PASS: live GitHub repository discovery returned the account's accessible repositories and the six repositories
  already registered in DevPath remained owner-scoped and readable.
- PASS: `semicolon213/DevPath` synchronization returned HTTP 202, progressed through durable job
  `f5d15d93-eced-4402-9ae1-2c96ad7b0d06`, and completed in approximately four seconds.
- PASS: a new immutable snapshot appeared at 2026-08-25 12:51 KST with the current source revision, branch, commit,
  technology, and engineering-evidence summaries.
- PASS: deterministic analysis returned HTTP 202, progressed through job
  `994768a6-7fa4-4e96-b3ab-6e9095b9afc8`, and produced analysis result
  `79f97065-012a-468f-a1f0-9d367d30afca` at 2026-08-25 12:52 KST.
- PASS: the result used policy `skill-matrix-v2` and rule version `baseline-v2`; the UI displayed eight evaluated
  skills, evidence counts, confidence, and official scores calculated by the Rule Engine.
- PASS: the dashboard reflected the new result without browser-side recalculation: overall score 72.96, career
  readiness 78.56, four strengths, two improvement areas, and the latest sync/analysis jobs as `SUCCEEDED`.
- PASS: the recommendation and learning-roadmap view showed the current DevOps priority and its prerequisite-linked
  16-hour learning step.
- PASS: the controlled Chromium journey showed the current roadmap alongside owner-scoped history and the archive
  action; automated server tests verified CSRF, idempotency-key, authorization, durable audit, and repeated-archive
  behavior. The archive action was not invoked against the live user-owned roadmap during this evidence run.
- PASS: the controlled Chromium journey showed the recommendation-set history, individual deterministic recommendation
  detail, completion criteria, and linked observed evidence with automated axe checks; PostgreSQL tests verified that
  recommendation and evidence retrieval remain owner-scoped.
- PASS: the controlled Chromium journey selected two immutable analysis results and displayed their stored overall and
  category scores, rule/formula/extractor versions, and evidence facts side by side with automated axe checks. The
  comparison deliberately derives no new official score, improvement score, or trend value.
- PASS: the same journey continued into API-SKL-003 and displayed two owner-scoped immutable Skill Matrices side by side
  with automated axe checks. The application path emits the durable comparison audit event and neither server nor
  browser derived a delta, replacement level, or new growth trend.
- PASS: the controlled Chromium journey opened a current Matrix skill, displayed its stored score/level/confidence and
  reproduction metadata, and drilled into normalized owner-scoped snapshot evidence with automated axe checks. Detail
  and evidence reads emit separate durable audit events.
- PASS: the controlled Chromium journey connected current readiness, recommendation-set history, immutable historical
  readiness, the canonical Skill Gap list, and skill evidence with automated axe checks. The historical view displays
  stored values and version metadata without deriving a new readiness score or recommendation priority.
- PASS: the `/onboarding` capability displayed the eight server-derived setup steps, optional company-target semantics,
  completed first-analysis state, and direct handoff to dashboard and result workspaces with automated axe checks.
  API-ID-010 reads owner-scoped persisted resources and records `ONBOARDING_PROGRESS_VIEWED`; it does not create a
  duplicate progress table or derive any official score or readiness value.
- PASS: the settings capability separated profile/target management from GitHub connection and repository registration
  across `/settings/profile` and `/settings/integrations`, while onboarding reused the same components. Chromium and
  axe verified the settings hub, labeled target controls, disconnected provider state, and responsive navigation.
- PASS: GitHub connection recovery tests verify that disconnect and provider permission withdrawal produce `REVOKED`,
  unusable refresh expiry produces `EXPIRED`, actual provider secrets and scopes are discarded, inactive connections do
  not trigger repository reads, and reauthorization can rotate the retained owner connection back to `ACTIVE`. A live
  provider revocation exercise remains pending and the release gate therefore remains `PARTIAL`.
- PASS: controlled GitHub rate-limit tests distinguish quota exhaustion from permission withdrawal, retain the active
  credential, normalize retry/reset headers into `429 RATE_LIMIT_EXCEEDED`, write a restricted audit event, persist
  `RETRY_WAIT` until the provider reset for background synchronization, and prevent browser retry bursts. A real-provider
  quota-exhaustion exercise was intentionally not performed.
- PASS: FR-031~FR-036 tests collect and normalize bounded PR/review/issue/README facts, persist them in immutable
  snapshot-local V20 tables, expose non-scoring collaboration and README section signals, and keep baseline-v2's
  official extractor input version unchanged. Testcontainers and repository-detail UI tests passed; a fresh live
  provider sync containing representative collaboration data remains pending.
- PASS: request-completion logs contained bounded per-request IDs and a shared browser correlation ID for the OAuth,
  discovery, synchronization, analysis, and dashboard requests without credential data.
- PASS: the authenticated JDBC session survived two clean backend process restarts and continued to return the
  owner-scoped home and dashboard state after browser refresh.
- PASS: the CSRF-protected logout returned HTTP 204, invalidated the server session, and caused the next protected
  dashboard request to return HTTP 401 with an actionable anonymous UI state.
- PASS: logout now passes through the request-correlation filter and records both a bounded completion log and an
  append-only `LOGOUT_SUCCEEDED` audit record in PostgreSQL; the live record was observed at 2026-08-25 13:02:22 KST.
- PASS: a subsequent GitHub OAuth login restored a new authenticated DevPath session without exposing credentials.
- PASS: with a temporary 10-second absolute timeout, the first request from an older authenticated session returned
  HTTP 401 immediately; the exact-deadline unit test verifies that the session is invalidated and the already-loaded
  SecurityContext is cleared before authorization continues.
- PASS: absolute expiration persisted `SESSION_ABSOLUTE_TIMEOUT` with resource type `SESSION` and outcome `SUCCEEDED`
  in PostgreSQL at 2026-08-25 13:13:43 KST.
- PASS: with a temporary 10-second JDBC idle timeout and a five-minute absolute timeout, an authenticated session left
  without requests expired, the next current-user request returned HTTP 401, and the expired `spring_session` row was
  removed by the JDBC cleanup path.
- PASS: the backend was restored to the configured local 30-minute idle and 12-hour absolute timeouts, and a new OAuth
  session was established after the boundary exercises.
- PASS: keyboard-only Chrome verification exposed a visible `본문으로 건너뛰기` link, moved focus to the main landmark,
  activated route links with Enter, and restored focus to the destination page's `h1`, including asynchronously loaded
  analysis routes.
- PASS: the authenticated home route remained readable and operable at Chrome 200% zoom without horizontal content
  clipping; controls reflowed vertically, and the browser zoom was restored to its original 80% afterward.
- PASS: the Windows accessibility tree exposed main/navigation/region landmarks, heading hierarchy, labeled form
  controls, status and alert semantics, and descriptive link/button names on the exercised home and dashboard routes.

This is local system evidence, not a staging deployment, external security review, spoken screen-reader review,
provider permission-edge-case test, token revocation exercise, or owner approval.

## 5. Known Limitations and Release Blockers

- The core M32 live GitHub OAuth/provider journey is recorded above; organization permission edge cases, explicit
  provider-token revocation, spoken screen-reader output, and an OS-level reduced-motion exercise remain unverified.
- Spring Session JDBC 3.3.3 deletes idle-expired rows without publishing `SessionExpiredEvent`; idle expiry is evidenced
  by the protected-request 401, bounded request log, and session-row cleanup, but a user-attributed durable idle-timeout
  audit event remains unresolved. Absolute timeout and explicit logout do have durable audit records.
- ADR-031 remains Proposed, so no telemetry backend, metrics store, trace backend, retention, dashboard, or alert product is selected.
- ADR-033 and ADR-034 remain Proposed, so staging/production deployment and production secret management are blocked.
- The local request log is operational telemetry only; durable audit records remain the audit authority.
- No Notion, knowledge retrieval, AI generation, portfolio, resume, interview, administration, or production deployment workflow is implemented.
- Dashboard APIs 002 through 010, company readiness, artifact summaries, charts, filters, and export remain outside the implemented MVP subset.
- Two catalog database tests require an explicit `DEVPATH_DB_URL`; Testcontainers-backed PostgreSQL suites cover the portable default path, including the owner-scoped analysis comparison query.

## 6. Approval Record

| Review | State | Owner/Evidence |
|---|---|---|
| Engineering self-review | PENDING | Review diff, test output, contract, migrations, and generated artifacts |
| Security review | PENDING | Review owner isolation, OAuth/token handling, correlation redaction, and dependency results |
| Accessibility manual review | PARTIAL | PASS for keyboard, visible focus, skip link, route focus restoration, 200% zoom/reflow, and accessibility-tree semantics; spoken screen-reader output and OS-level reduced-motion exercise remain NOT RUN |
| Live-provider demonstration | PASS | 2026-08-25 local GitHub OAuth, repository discovery, sync, immutable snapshot, deterministic analysis, readiness, dashboard, and roadmap journey recorded in section 4 |
| Deployment review | BLOCKED | Requires Accepted ADR-031/033/034 and a target environment |
| MVP gate approval | PENDING | Project owner/coordinator decision after required evidence is attached |
