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
| UX-3 | Shared label vocabulary | Raw API enums and internal terms reach the UI; one concept has several Korean labels |
| UX-4 | Confirmation dialogs and forms | Roadmap archive has no confirmation; three different confirmation patterns; no field-level form errors |
| UX-5 | Design tokens and accessibility | No color tokens; invisible focus on dark surfaces; unfocusable scrollable comparison tables |
| UX-6 | Shared page states | Inconsistent loading and error states; no data freshness on the dashboard |

Navigation items, AI panel placement and labeling, and the per-skill insufficient-evidence rule need product decisions
before UX-1 and UX-2 start.

### 6.2 Backend and Frontend Code

The code audit found the hexagonal boundaries, transaction scopes around provider calls, error responses, and logging
clean. It recorded 19 findings, mostly read and write amplification at realistic data sizes.

| Slice | Scope | Findings |
|---|---|---|
| C-1 | Snapshot read paths | Snapshot lists fully hydrate every snapshot's children to compute counts; technology and evidence views load whole snapshots |
| C-2 | Bulk inserts | Assigned-UUID child entities trigger a SELECT before every INSERT during snapshot and knowledge persistence |
| C-3 | Batched hydration | N+1 queries in Skill Matrix, rule evaluation, and recommendation history reads, including the career-coaching request path |
| C-4 | AI job lease and artifact index | Crashed RUNNING AI tasks are never reclaimed; artifact lookup by prompt context has no index (new migration required) |
| C-5 | Worker claims and failure logs | Claims block instead of skipping locked rows; three workers log nothing on unexpected failures; knowledge worker lacks unit tests |
| C-6 | Frontend cache invalidation | Analysis history and dashboard stay stale after an analysis completes |

Contract changes such as pagination for unbounded history endpoints, error-status remapping, and work that depends on
Proposed ADR-031, ADR-033, or ADR-034 are excluded from these slices.
