# DevPath Feature Status

## 1. Purpose and Authority

This document summarizes which DevPath capabilities are implemented, which await approval, and which remain planned. It
is a navigational status report, not a requirement source. `docs/01_SRS.md`, the engine specifications, and
`docs/19_Roadmap.md` remain authoritative for scope and milestone completion; `docs/20_MVP_Release_Evidence.md` remains the
MVP evidence index. Nothing here declares a milestone complete or replaces owner approval.

Snapshot: commit `661302a` on `main`, 2026-10-15.

Status vocabulary:

| Status | Meaning |
|---|---|
| Implemented | Code, contract, and automated tests exist; no open approval gate is recorded for the capability itself. |
| Implemented, approval pending | Code and automated evidence exist, but the roadmap records owner approval, live-provider exercises, or production decisions as still open. |
| Not implemented | Specified but absent from the code and contract. |

## 2. Verification Snapshot (2026-10-15)

`npm run verify:mvp` was run locally on a working tree identical to commit `661302a`, with
`JAVA_HOME` set to JDK 21, `DOCKER_API_VERSION=1.44`, and `VITE_DEVPATH_API_BASE_URL=http://localhost:8080` (Docker 29.6.2).

| Check | Result |
|---|---|
| Security boundary tests and repository scan | PASS (3 checks) |
| Backend clean build with JUnit, architecture, and Testcontainers suites | PASS; 226 tests, 224 passed, 0 failed, 2 environment-gated skips |
| Frontend clean install | PASS |
| Frontend Vitest | PASS; 44 files, 114 tests |
| Playwright Chromium with axe | PASS; 7 browser journeys |
| Frontend production build | PASS |
| Frontend `npm audit` | FAIL; 1 high-severity advisory in transitive `source-map-js` (GHSA-68fv-2mgg-jv7q), fix available via `npm audit fix` |
| OpenAPI lint (`npm run contract:lint`, run separately) | PASS; 0 errors, 9 recommendation warnings |

The `npm audit` failure comes from a dependency advisory, not from application changes; resolving it requires a lockfile
update. Local environment notes from the same run: a shell `JAVA_HOME` pointing to JDK 11 makes the Gradle fallback fail,
and `scripts/run-gradle.mjs` may not resolve `gradlew.bat` from its working directory when the current directory is
excluded from executable lookup, in which case it falls back to the cached Gradle distribution.

## 3. Capability Status

| Area | Capability | Status | Roadmap | Frontend routes |
|---|---|---|---|---|
| Identity | GitHub OAuth2 login, opaque server session, CSRF, logout, current user, provider-independent `User` | Implemented | M16 | `/`, `/settings` |
| Profile | User profile and Career/Company preference | Implemented | M17 | `/settings/profile`, `/onboarding` |
| GitHub | Connection, repository discovery and import | Implemented | M18 | `/settings/integrations`, `/repositories` |
| Repositories | Registration, archive/restore, durable synchronization, immutable snapshots, evidence and activity extraction | Implemented | M19–M22 | `/repositories`, `/repositories/:repositoryId`, `/repositories/:repositoryId/snapshots/:snapshotId` |
| Analysis | Deterministic Rule Engine analysis, history, score detail, evidence links, comparison | Implemented | M23–M25, M32 | `/analyses`, `/analyses/:analysisId`, `/analyses/compare` |
| Skills | Skill Matrix creation, retrieval, comparison, per-skill immutable history | Implemented | M26 | `/skills`, `/skills/:skillId`, `/skills/compare` |
| Careers | Backend/Frontend career catalog, `readiness-v1` Career Readiness and Skill Gap | Implemented | M27–M28, M30 | `/careers`, `/careers/:careerId`, `/career-readiness`, `/career-readiness/:careerReadinessId` |
| Companies | Company catalog | Implemented | M29 | `/companies`, `/companies/:id` |
| Companies | Company-specific readiness and recommendation modifiers | Not implemented (post-MVP) | M29–M30 | — |
| Recommendations | `recommendation-v1` deterministic recommendations and `roadmap-v1` learning roadmap | Implemented | M31 | `/recommendations`, `/recommendations/:recommendationId`, `/roadmap` |
| Dashboard | Owner dashboard summary | Implemented | M33 | `/dashboard` |
| MVP gate | Combined MVP completion evidence | Implemented, approval pending | M34 | — |
| Knowledge | Notion OAuth connection and shared page metadata discovery | Implemented, approval pending | M35 | `/settings/integrations` |
| Knowledge | Notion ingestion: normalization, chunking, Ollama embeddings, pgvector, archive and reindex | Implemented, approval pending | M36 | `/knowledge`, `/knowledge/:documentId` |
| Knowledge | Owner-filtered semantic retrieval with citations | Implemented, approval pending | M37 | `/knowledge` |
| AI | Provider port with Ollama generation adapter, cancellation, bounded retries, validated artifacts | Implemented, approval pending | M38–M42 | — |
| AI | Skill analysis explanation | Implemented, approval pending | M42 | `/skills` |
| AI | Repository review and repository summary | Implemented, approval pending | M43 | `/analyses/:analysisId` |
| AI | Career coaching and learning recommendation | Implemented, approval pending | M43 | `/career-readiness`, `/roadmap` |
| AI | Generated artifact list, manual retry, editing and export | Not implemented | M43–M44 | — |
| Artifacts | Portfolio, Resume, Interview preparation | Not implemented | M45–M47 | — |
| Operations | Administration workflows | Not implemented | — | — |
| Operations | Production observability, deployment platform, secrets management | Not implemented; blocked by Proposed ADR-031, ADR-033, ADR-034 | M52–M53 | — |

## 4. API Contract Coverage

`contracts/openapi/devpath-openapi.yaml` defines 75 operations covering 70 of the 160 canonical API IDs in
`docs/10_API_Specification.md`.

| Group | In contract | Specified | Main gaps |
|---|---:|---:|---|
| Identity (`API-ID`) | 8 | 10 | API-ID-008, API-ID-009 |
| Integrations (`API-INT`) | 10 | 12 | API-INT-006, API-INT-012 |
| Repositories (`API-REP`) | 10 | 12 | API-REP-003, API-REP-010 |
| Analysis (`API-ANA`) | 8 | 8 | — |
| Skills (`API-SKL`) | 5 | 7 | Technology and framework proficiency |
| Careers (`API-CAR`) | 5 | 6 | API-CAR-003 |
| Companies (`API-CMP`) | 2 | 5 | Company preference, company readiness and comparison |
| Recommendations (`API-REC`) | 4 | 8 | Recommendation requests and related operations |
| Learning (`API-LRN`) | 4 | 10 | Roadmap creation and progress operations |
| Dashboard (`API-DSH`) | 1 | 10 | Section-level dashboard endpoints |
| Knowledge (`API-KNW`) | 8 | 11 | Manual document metadata operations |
| AI (`API-AI`) | 5 | 7 | API-AI-004 retry, API-AI-006 artifact list |
| Prompt, Portfolio, Resume, Interview, Admin, Open (`API-PRM`, `API-PRT`, `API-RSM`, `API-ITV`, `API-ADM`, `API-OPEN`) | 0 | 54 | Not started |

## 5. Open Gates

- Owner approval remains open for the MVP gate (M34) and for the implemented knowledge (M35–M37) and AI (M38–M43)
  capabilities; live-provider exercises listed in `docs/19_Roadmap.md` are still required.
- Production object storage (RM-OI-007), deployment and secrets (RM-OI-008), and the demonstration environment
  (RM-OI-010) remain open in roadmap §76.
- Proposed ADR-031, ADR-033, and ADR-034 block production observability, deployment, and secrets work.
- The frontend dependency advisory in §2 needs a lockfile update before `npm run verify:mvp` passes again.

## 6. Quality Backlog (2026-10-15 Audits)

Two independent read-only audits reviewed commit `9331fa0`, whose application code is identical to `661302a`. Their
findings are candidates, not approved work; each slice still needs owner approval, implementation, and independent QA.

### 6.1 UI/UX

The UI/UX audit compared all 27 routes with `DESIGN.md` and captured 25 routes at 1280px and 375px with axe checks. It
recorded 26 findings: 6 high, 15 medium, and 5 low. Every route has one h1, no route overflows horizontally at 375px,
and axe reported one violation type on two comparison routes.

| Slice | Scope | Findings |
|---|---|---|
| UX-1 | App shell, global navigation, broken links | No global navigation; repository-add and career-target links lead to dead ends |
| UX-2 | AI content separation | AI panels reuse official-result styling, lack a consistent AI label, and precede official results |
| UX-3 | Shared label vocabulary | **Resolved (2026-10-16):** a shared `shared/ui/labels.ts` gives each enum one Korean label with a neutral "알 수 없음" fallback and replaces duplicated page helpers; repository lifecycle and sync status are worded differently and sync status is shown as a text badge; internal terms (M36, MVP, embedding provider, raw HTTP status codes, English eyebrows) are removed, job error codes map to safe Korean messages, and empty states no longer name specific careers (UI-03, UI-07, UI-08, UI-21, UI-22, UI-26). Evidence: independent QA PASS; frontend 45 files / 129 tests PASS; build PASS. Follow-ups resolved (2026-10-16): skill names fall back to the API name for skills missing from the local label map, header lines on analysis comparison, career readiness and recommendations are Korean with engine names only in parentheses, repository cards show sync status once as a text badge, and a terminal failed knowledge job no longer promises automatic retry (independent QA PASS; frontend 131 tests PASS). The "Rule Engine 결과" copy in the two AI panels (`RepositoryReviewPanel`, `SkillExplanationPanel`) was also changed to Korean-first "공식 분석 결과(Rule Engine)" on 2026-10-16 (independent QA PASS) |
| UX-4 | Confirmation dialogs and forms | **Resolved (2026-10-18):** a shared accessible `ConfirmDialog` (focus moves into the dialog and stays trapped, including while pending; Escape cancels except while pending; focus returns to the trigger or a fallback; failures are shown inside the dialog with retry) now confirms roadmap archive (previously unconfirmed), knowledge archive and GitHub/Notion disconnect (previously `window.confirm`) and repository archive on the card and detail page, where archive moves into a separated danger area; changing the target career/company is an explicit, confirmed action that only announces its effect (owner decision D-7); the profile form shows field errors, an error summary that takes focus and links to the fields, and a `beforeunload` unsaved-changes guard; success outcomes are announced in always-mounted live regions, and settings and onboarding headings no longer skip levels (UI-06, UI-13, UI-14, UI-15). Evidence: independent reviews PASS (3 cycles, 3 lenses); frontend 48 files / 149 tests PASS; build PASS; Playwright 7 tests PASS. Open (Low, deferred): in-app navigation with unsaved profile edits is not blocked (`beforeunload` only; data-router migration deferred to UX-1 by owner decision), the repository archive copy says new analysis cannot start although the backend does not reject analysis of an archived repository (pre-existing UI-only guard), the background is not made inert (relies on `aria-modal` and the focus trap), and unused `.lifecycle-confirmation` CSS remains |
| UX-5 | Design tokens and accessibility | **Resolved (2026-10-17):** `styles.css` defines the DESIGN §5.1 role tokens (background, text, border, brand, success, warning, danger, info, evidence, AI) and radius tokens on `:root`, and a test keeps raw hex colours inside that token block; green is used only for success states (other badges use Info); the focus ring is a 2px brand outline with offset plus a white separation ring, visible on light, dark and brand-filled controls; the analysis and skill comparison tables sit in focusable, distinctly labelled scroll regions with a Korean scroll hint at <=720px (owner decision D-8); card links and quiet buttons reach 44x44px on the dashboard, repositories and skill detail pages (UI-16, UI-17, UI-18, UI-24). Evidence: independent reviews PASS (2 cycles, 3 lenses); frontend 46 files / 132 tests PASS; build PASS; Playwright 7 tests including axe at 375px on the comparison routes PASS. Open (Low): two `rgba()` literals remain outside the tokens (the guard checks hex only), the white focus separation relies on `box-shadow` not being overridden by later component rules, and manual screen-reader and 200% zoom checks were not performed |
| UX-6 | Shared page states | **Resolved (2026-10-18):** a shared `PageState` component is used by 21 pages: loading renders the page header immediately with an `aria-hidden` bounded skeleton and a single status announcement, and errors distinguish 401 (sign-in), existence-safe 404/403, network failure (distinct from 401) and internal errors with retry and a copyable request reference; pages that show a domain "not yet generated" state on 404 (career readiness, learning roadmap, skills) keep it and route other errors through `PageState`; the dashboard shows the data basis time, links each recommendation to its detail with a distinct accessible name, and shows roadmap progress as a progress bar instead of in official-score style (UI-19, UI-20, UI-23). UI-10 (per-skill "근거 부족") is excluded: owner decision D-4 requires its definition to come from the Rule Engine/API. Evidence: independent reviews PASS (2 cycles, 3 lenses); frontend 49 files / 174 tests PASS; build PASS; Playwright 7 tests PASS. Open (Low): retry and back-link labels are generic ("다시 시도", "돌아가기"), some retry tests only assert that a fetch was made, any `TypeError` is treated as a network failure, and no manual screen-reader pass was performed |

Navigation items, AI panel placement and labeling, and the per-skill insufficient-evidence rule need product decisions
before UX-1 and UX-2 start.

Three individual findings were resolved in commit `6e078ac` after independent QA PASS; UX-1 and UX-2 otherwise remain
open:

- UI-02 (UX-1): repository-add links now open `/settings/integrations#github`.
- UI-12 (UX-1): the careers target CTA now opens `/settings/profile#targets`.
- UI-05 (UX-2): the roadmap AI learning guide sits in its own labelled region after the official roadmap steps.

### 6.2 Backend and Frontend Code

The code audit found the hexagonal boundaries, transaction scopes around provider calls, error responses, and logging
clean. It recorded 19 findings, mostly read and write amplification at realistic data sizes.

| Slice | Scope | Findings |
|---|---|---|
| C-1 | Snapshot read paths | **Resolved (2026-10-17):** the snapshot list now uses one owner-scoped summary query (header columns plus count subqueries, no joins) instead of hydrating every snapshot; the technologies views read only languages and dependencies, and the evidence view reads only the child sets it uses (all except languages), through owner-scoped queries; full hydration is kept for snapshot detail and analysis, and responses are unchanged (golden integration test). Prepared statements: two-snapshot list 18 to 2, each technologies view 10 to 4. Evidence: independent QA PASS (2 cycles); backend 240 tests, 0 failures, 2 environment-conditional skips. Test follow-ups added (2026-10-18, independent QA PASS): cross-owner denial is now also asserted for both technologies views and the evidence view. Open (Low): the list endpoint remains unbounded (contract question) |
| C-2 | Bulk inserts | **Resolved (2026-10-17):** immutable snapshot child entities and knowledge chunks implement `Persistable`, so `saveAll` persists instead of merging and no longer issues a SELECT before each INSERT; embeddings are written in one JDBC batch with the same SQL; marking previous chunks stale is one set-based UPDATE with the same scope as the old loop; Hibernate JDBC batching is enabled (`batch_size` 100, `order_inserts`). Transactions, rollback atomicity, ordering, owner and snapshot foreign keys, and vector values are unchanged. Prepared statements: 3 for a 500-file snapshot, 8 for a 200-chunk ingestion plus one JDBC embedding batch. Evidence: independent QA PASS; backend 248 tests, 0 failures, 2 environment-conditional skips. Fixed (2026-10-17, independent QA PASS): `isNew()` is now a transient flag cleared on load and persist instead of a constant `true`, and tests assert `STALE` chunk status after re-ingestion and archive. Open (Low): `order_inserts` could reorder a future mixed parent/child flush because foreign keys are plain UUID columns |
| C-3 | Batched hydration | **Resolved (2026-10-16):** Skill Matrix, rule-evaluation missing-evidence, and recommendation set/roadmap history reads now load children with batched `IN` queries grouped in memory with explicit `ORDER BY`, so the query count is constant (Hibernate statistics: Skill Matrix 6 queries for 1 or 2 assessments, sets and roadmaps 4 queries for 1 or 2); fact saves are batched; both AI career-coaching paths read only the needed set or roadmap with owner-scoped targeted queries that keep the `generatedAt` desc, `id` desc selection and existing exceptions. Evidence: independent QA PASS; backend 235 tests, 0 failures, 2 environment-conditional skips; new Skill Matrix save/reload round-trip integration test. Test follow-ups added (2026-10-18, independent QA PASS): recommendation sets with several recommendations and evidence IDs and roadmaps with several milestones and steps, inserted unsorted, assert ordering through single-entity and batch list hydration; rule-evaluation missing evidence across two categories asserts ordering and an exact statement count. Open (Low): history endpoints remain unbounded (contract question) |
| C-4 | AI job lease and artifact index | **Resolved (2026-10-16):** V29 adds `ai_tasks.next_attempt_at`; claims take due QUEUED tasks or RUNNING tasks whose 15-minute lease expired, retries back off from 30s up to 5 min, expiry at the attempt limit fails with `WORKER_LEASE_EXPIRED`, and artifact lookup is owner-scoped and indexed. Evidence: independent QA PASS; backend 230 tests, 0 failures |
| C-5 | Worker claims and failure logs | **Resolved (2026-10-16):** analysis, repository sync, and AI claim queries now skip locked rows (`FOR NO KEY UPDATE SKIP LOCKED`), while the per-ID locks used by cancellation still block; the analysis, sync, and AI workers log a WARN with only the job ID and exception class on unexpected failures; new knowledge worker unit tests cover success and four failure mappings. Evidence: independent QA PASS; backend 239 tests, 0 failures, 2 environment-conditional skips. Test follow-ups added (2026-10-18, independent QA PASS): with the oldest claimable row locked, a second analysis, repository sync, and AI claim each takes the next row without waiting |
| C-6 | Frontend cache invalidation | **Resolved (2026-10-16):** a successful analysis now also invalidates analysis history and dashboard queries. Evidence: commit `6e078ac`, independent QA PASS |

Contract changes such as pagination for unbounded history endpoints, error-status remapping, and work that depends on
Proposed ADR-031, ADR-033, or ADR-034 are excluded from these slices.
