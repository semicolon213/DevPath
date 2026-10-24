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

Later tooling changes (the table above is kept as the 2026-10-15 record): FE-GATE (`e0bc4a7`) pinned the Playwright
API base URL and updated Vite to 8.3.5, and FE-AUDIT (`30d2cad`) updated Vitest to 4.1.12, which brought the frontend
`npm audit` to 0 advisories. The last full `npm run verify:mvp` run passed at commit `1860f10` (CSS-SPLIT). TEST-NOISE (`2a279dd`)
handles the intentional render errors inside the `AppRoutes` tests, so the frontend test output no longer contains about
80 lines of jsdom stack traces. After the visual redesign (UI-5, `14076b5`): frontend 53 files / 232 tests PASS; build
PASS; Playwright 13 tests (including axe, reduced motion and scrollbar-visible overflow checks) PASS. After CSS-SPLIT
(`1860f10`): `npm run verify:mvp` PASS (backend build and tests, frontend 53 files / 233 tests, Playwright 14 tests,
`npm audit` 0 advisories, contract lint).

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

The contract discrepancies D1–D15 listed in `docs/22_API_Reference.md` Appendix A were resolved on 2026-10-19 (docs/10
aligned to the implementation) and 2026-10-21 (API-FIX code/OpenAPI alignment); see that appendix for the remaining
Low items.

## 5. Open Gates

- Owner approval remains open for the MVP gate (M34) and for the implemented knowledge (M35–M37) and AI (M38–M43)
  capabilities; live-provider exercises listed in `docs/19_Roadmap.md` are still required.
- Production object storage (RM-OI-007), deployment and secrets (RM-OI-008), and the demonstration environment
  (RM-OI-010) remain open in roadmap §76.
- Proposed ADR-031, ADR-033, and ADR-034 block production observability, deployment, and secrets work.
- The frontend dependency advisory recorded in §2 was resolved by lockfile updates (FE-GATE, FE-AUDIT); see the note
  after the §2 table.

## 6. Quality Backlog (2026-10-15 Audits)

Two independent read-only audits reviewed commit `9331fa0`, whose application code is identical to `661302a`. Their
findings are candidates, not approved work; each slice still needs owner approval, implementation, and independent QA.

### 6.1 UI/UX

The UI/UX audit compared all 27 routes with `DESIGN.md` and captured 25 routes at 1280px and 375px with axe checks. It
recorded 26 findings: 6 high, 15 medium, and 5 low. Every route has one h1, no route overflows horizontally at 375px,
and axe reported one violation type on two comparison routes.

| Slice | Scope | Findings |
|---|---|---|
| UX-1 | App shell, global navigation, broken links | **Resolved (2026-10-19):** the app now uses a data router with one authenticated layout (header, skip link, global navigation with `aria-current` and a non-colour active indicator, secondary logout button); at <=720px the navigation collapses behind a keyboard-operable menu button; per-page back-link shells were removed and detail routes show breadcrumbs; the home page has one primary action and the onboarding next step is a proper link; route render errors show the existing safe Korean error page without message or stack trace; the profile unsaved-changes guard now also blocks in-app navigation and asks before logout through the shared `ConfirmDialog`, in addition to `beforeunload`; URLs are unchanged and the session bootstrap still distinguishes 401 from network failure (UI-01, UI-11, UI-25; UI-02 and UI-12 were resolved earlier, see below). Navigation scope follows D-1 (owner-confirmed (2026-10-19)): the global navigation lists only implemented areas (대시보드, 저장소, 분석, 기술 역량, 커리어 준비도, 추천, 학습 로드맵, 지식, 설정); careers and companies are reached from settings/profile and career readiness; artifacts, portfolio, resume and interview are not shown because they are not implemented. Evidence: independent reviews PASS (3 cycles, 3 lenses); frontend 51 files / 188 tests PASS; build PASS; Playwright 8 tests including axe landmark rules PASS. Follow-up fixed (2026-10-21, API-FIX, independent reviews PASS): the authenticated layout has its own error element, so a page render error keeps the global navigation. Open (Low): anonymous deep links still render the authenticated layout (pages show the sign-in state, unchanged from before), and the router is created at module import |
| UX-2 | AI content separation | **Resolved (2026-10-19):** a shared `AiGeneratedPanel` frame wraps the five AI panels (repository summary, repository review, skill explanation, career coaching, learning recommendation) and keeps their job states, cancel, failure and polling behaviour unchanged; every AI region is a labelled region whose accessible name includes the "AI 생성" badge, uses the AI token surface (never official-result styling), and sits after the official results on its host page; the generation time (`<time>`) and the Korean validation status are shown only when a generated result exists, with state-specific copy otherwise, and prompt, validator and model versions are not shown; evidence references link to existing analysis evidence or rule-result anchors, and route-less evidence renders as labelled non-links; hash navigation now scrolls (reduced-motion aware) and moves focus to the target after async loading, with an h1/main fallback, without breaking route-change focus (UI-04, UI-09; UI-05 and the UX-2C copy were resolved earlier). Placement and labeling follow D-2 and D-3 (owner-confirmed (2026-10-19)): D-2 keeps AI panels inline on official result pages, placed after the official results (no side panel or separate route); D-3 shows an "AI 생성" badge, the generation time and the validation status in each AI region and does not show prompt, validator or model versions to users. Evidence: independent reviews PASS (3 cycles, 3 lenses); frontend 53 files / 212 tests PASS; build PASS; Playwright 8 tests including axe PASS. Open (Low): all rule-result links target one section anchor, and the five panels repeat the same prop wiring. Update (2026-10-23/24, ALIGN-P2, ALIGN-E; see §6.1.2): the badge text is now "AI 생성 · 설명만 제공" in every AI region, and the former index-based evidence link names ("분석 근거 N") were replaced by footnoted evidence resolved from page or analysis-detail data. Follow-up fixed (2026-10-21, API-FIX, independent reviews PASS): skill-explanation artifacts now carry an owner-scoped `provenance.analysisId`, so their evidence references link to the analysis (prompt payload and hash unchanged) |
| UX-3 | Shared label vocabulary | **Resolved (2026-10-16):** a shared `shared/ui/labels.ts` gives each enum one Korean label with a neutral "알 수 없음" fallback and replaces duplicated page helpers; repository lifecycle and sync status are worded differently and sync status is shown as a text badge; internal terms (M36, MVP, embedding provider, raw HTTP status codes, English eyebrows) are removed, job error codes map to safe Korean messages, and empty states no longer name specific careers (UI-03, UI-07, UI-08, UI-21, UI-22, UI-26). Evidence: independent QA PASS; frontend 45 files / 129 tests PASS; build PASS. Follow-ups resolved (2026-10-16): skill names fall back to the API name for skills missing from the local label map, header lines on analysis comparison, career readiness and recommendations are Korean with engine names only in parentheses, repository cards show sync status once as a text badge, and a terminal failed knowledge job no longer promises automatic retry (independent QA PASS; frontend 131 tests PASS). The "Rule Engine 결과" copy in the two AI panels (`RepositoryReviewPanel`, `SkillExplanationPanel`) was also changed to Korean-first "공식 분석 결과(Rule Engine)" on 2026-10-16 (independent QA PASS) |
| UX-4 | Confirmation dialogs and forms | **Resolved (2026-10-18):** a shared accessible `ConfirmDialog` (focus moves into the dialog and stays trapped, including while pending; Escape cancels except while pending; focus returns to the trigger or a fallback; failures are shown inside the dialog with retry) now confirms roadmap archive (previously unconfirmed), knowledge archive and GitHub/Notion disconnect (previously `window.confirm`) and repository archive on the card and detail page, where archive moves into a separated danger area; changing the target career/company is an explicit, confirmed action that only announces its effect (owner decision D-7); the profile form shows field errors, an error summary that takes focus and links to the fields, and a `beforeunload` unsaved-changes guard; success outcomes are announced in always-mounted live regions, and settings and onboarding headings no longer skip levels (UI-06, UI-13, UI-14, UI-15). Evidence: independent reviews PASS (3 cycles, 3 lenses); frontend 48 files / 149 tests PASS; build PASS; Playwright 7 tests PASS. Open (Low, deferred): the repository archive copy says new analysis cannot start although the backend does not reject analysis of an archived repository (pre-existing UI-only guard), and the background is not made inert (relies on `aria-modal` and the focus trap). Follow-ups fixed (2026-10-18, independent QA PASS): `ConfirmDialog` treats `document.body`, disconnected and disabled return targets as unavailable and uses the fallback (covers triggers that never received focus, such as Safari mouse clicks), and the unused `.lifecycle-confirmation` CSS was removed (frontend 49 files / 177 tests PASS; build PASS; Playwright 7 tests PASS). The former open item that in-app navigation with unsaved profile edits was not blocked (`beforeunload` only) was resolved in UX-1 (2026-10-19) |
| UX-5 | Design tokens and accessibility | **Resolved (2026-10-17):** `styles.css` defines the DESIGN §5.1 role tokens (background, text, border, brand, success, warning, danger, info, evidence, AI) and radius tokens on `:root`, and a test keeps raw hex colours inside that token block; green is used only for success states (other badges use Info); the focus ring was a 2px brand outline with offset plus a white separation ring (current values: a 3px amber outline with offset plus an indigo halo, from `--border-focus-inverse` and `--border-focus`), visible on light, dark and brand-filled controls; the analysis and skill comparison tables sit in focusable, distinctly labelled scroll regions with a Korean scroll hint at <=720px (owner decision D-8); card links and quiet buttons reach 44x44px on the dashboard, repositories and skill detail pages (UI-16, UI-17, UI-18, UI-24). Evidence: independent reviews PASS (2 cycles, 3 lenses); frontend 46 files / 132 tests PASS; build PASS; Playwright 7 tests including axe at 375px on the comparison routes PASS. Follow-ups fixed (2026-10-18, independent QA PASS): the two remaining `rgba()` literals moved into `:root` tokens and the style guard also rejects `rgba()`/`hsl()` literals outside `:root`; the focus rule now has higher specificity so single-class component `box-shadow` rules cannot hide the white separation ring (the outline remains primary) (frontend 49 files / 177 tests PASS; build PASS; Playwright 7 tests PASS). Open (Low): a future rule at equal or higher specificity could still override the separation ring, the new tokens `--background-score-detail` and `--shadow-selected` are component/shadow-named rather than DESIGN §5.1 role names, the guard does not check other colour functions or named colours, and manual screen-reader and 200% zoom checks were not performed. Note (2026-10-22, UI-1): the token values changed with the "성장 경로" visual direction (see §6.1.1); the token names were kept and the raw-colour style guard still applies |
| UX-6 | Shared page states | **Resolved (2026-10-18):** a shared `PageState` component is used by 21 pages: loading renders the page header immediately with an `aria-hidden` bounded skeleton and a single status announcement, and errors distinguish 401 (sign-in), existence-safe 404/403, network failure (distinct from 401) and internal errors with retry and a copyable request reference; pages that show a domain "not yet generated" state on 404 (career readiness, learning roadmap, skills) keep it and route other errors through `PageState`; the dashboard shows the data basis time, links each recommendation to its detail with a distinct accessible name, and shows roadmap progress as a progress bar instead of in official-score style (UI-19, UI-20, UI-23). UI-10 (per-skill "근거 부족") was handled separately under owner decision D-4 (2026-10-19), which defines the per-skill evidence status in the Rule Engine (`02_Rule_Engine.md` §16.6) with an API field (`10_API_Specification.md` §14); **resolved: implemented (backend and UI, 2026-10-22)** (see below; open Low: no history-entry NONE/INSUFFICIENT unit test, the NONE re-analysis guidance also appears on past matrices in history and comparison, the indicator's `aria-label` duplicates its visible text, and a pre-existing `aria-label` on the skill detail `.official-score` has no role). Evidence: independent reviews PASS (2 cycles, 3 lenses); frontend 49 files / 174 tests PASS; build PASS; Playwright 7 tests PASS. Open (Low): retry and back-link labels are generic ("다시 시도", "돌아가기"), and no manual screen-reader pass was performed. Resolved (2026-10-24, ALIGN-L): `NetworkError` is now raised only for rejected `fetch` calls instead of for any `TypeError` |

UX-1 follows the navigation decision D-1, and UX-2 follows the product decisions D-2 and D-3 (AI panel placement and
labeling). The owner confirmed all three decisions on 2026-10-19. Owner decision D-4 (2026-10-19) defines a
per-skill evidence status (`NONE`, `INSUFFICIENT`, `SUFFICIENT`) calculated by the Rule Engine under a new versioned
Skill Matrix policy. The backend part is implemented (2026-10-21): Flyway V30 adds `skill-matrix-v3`, which supersedes
`skill-matrix-v2` with the same thresholds and mappings plus minimum evidence count `2` and confidence threshold `60`;
the Rule Engine persists the status per assessment, and the Skill Matrix and skill detail APIs return a nullable
`evidenceStatus` (`null` for earlier matrices, which are not recomputed). Prompts are unchanged. Evidence: independent reviews PASS (QA-A, QA-B, code review);
backend 257 tests PASS (2 environment-conditional skips); frontend 214 tests PASS; builds and contract lint PASS.
UI-10 (per-skill "근거 부족" display) is implemented (backend and UI, 2026-10-22): a shared `EvidenceStatus` indicator
(`role="note"`, text label; labels in `shared/ui/labels.ts`) shows the Rule Engine's `evidenceStatus` on the skills
page, skill detail (current and history), Skill Matrix comparison and analysis detail pages. `NONE` hides the score bar
where one is shown and displays "근거 없음" with guidance; `INSUFFICIENT` keeps the official score and adds a "근거 부족"
indicator; `SUFFICIENT` and `null` (earlier policy versions) display as before. The UI never derives the status, and
e2e fixtures cover `NONE` and `INSUFFICIENT` under axe. Evidence: independent reviews PASS (2 cycles, 3 lenses);
frontend 53 files / 224 tests PASS; build PASS; Playwright 8 tests PASS. Open (Low): see the UX-6 row.

Three individual findings were resolved earlier in commit `6e078ac` after independent QA PASS (UX-1 and UX-2 were
resolved on 2026-10-19):

- UI-02 (UX-1): repository-add links now open `/settings/integrations#github`.
- UI-12 (UX-1): the careers target CTA now opens `/settings/profile#targets`.
- UI-05 (UX-2): the roadmap AI learning guide sits in its own labelled region after the official roadmap steps.

#### 6.1.1 Visual Redesign (UI-1 to UI-5)

After the UX slices, the owner chose the "성장 경로" (growth path) visual direction on 2026-10-22, recorded in
`DESIGN.md` §4–§5 (`25b700e`). The redesign was delivered in five slices. Every slice changed presentation only: pages
still show stored Rule Engine results and API values, and no slice calculates scores, deltas or statuses on the client.

| Slice | Scope | Status |
|---|---|---|
| UI-1 | Tokens, font and app shell | **Resolved (2026-10-22, `7682ee0`):** design token values follow the new direction (token names unchanged), a self-hosted Noto Sans KR variable font (`@fontsource-variable/noto-sans-kr` 5.3.1), an indigo app band with pill navigation, amber for the primary action, and tinted surfaces instead of bordered white cards. Evidence: independent reviews PASS (1 cycle) |
| UI-2 | Dashboard | **Resolved (2026-10-22, `6a38bd1`):** a hero band with the official readiness and one primary action, metric tiles, roadmap progress and recommendation step tiles; an e2e check covers overflow with always-visible scrollbars. Evidence: independent reviews PASS (2 cycles) |
| UI-3 | Skills and career readiness | **Resolved (2026-10-22, `83dcc01`):** skill score rows with the `EvidenceStatus` indicator; readiness gap bars drawn from `actualScore` and `expectedMinimum` through a shared `ReadinessGapList`; a global button hierarchy (the default button is secondary, one primary action per view); the Skill Matrix comparison shows stored values only, without client-side deltas. Evidence: independent reviews PASS (2 cycles) |
| UI-4 | Repositories, analyses, recommendations and roadmap | **Resolved (2026-10-23, `28d2523`):** the repository list as a desktop table and stacked rows on small screens; repository and snapshot detail; analyses with fixed-slot skill rows and category bars; analysis comparison showing stored results side by side without deltas; recommendations as tiles in server order; learning roadmap step tiles labelled from `RoadmapStep.status` (완료, 진행 중, 예정, 건너뜀); hero-band button contrast and overflow e2e checks. Evidence: independent reviews PASS (3 cycles) |
| UI-5 | Home, onboarding, targets, knowledge, settings and shared states | **Resolved (2026-10-23, `14076b5`):** home and onboarding (steps shown as 완료, 지금, 미완료, 예정); career and company tiles; knowledge pages whose labels come only from the API `status` and `sourceType`; settings; the shared `PageState`, `ConfirmDialog`, `Breadcrumbs` and not-found page restyled; unused selectors removed; an e2e check for long names at 375px. Evidence: independent reviews PASS (3 cycles) |

Verification on main after UI-5: frontend 53 files / 232 tests PASS; build PASS; Playwright 13 tests (including axe,
reduced motion and scrollbar-visible overflow) PASS. The full `npm run verify:mvp` run, last passed at `83dcc01` (UI-3)
before UI-4 and UI-5, passed again on main at `1860f10` after CSS-SPLIT (see below). During this work, `CLAUDE.md`/`AGENT.md` §14 added a rule that lets a
separate Claude worker handle frontend presentation fix cycles (`75c7c82`).

CSS-SPLIT (**resolved, 2026-10-23, `1860f10`**): `frontend/src/app/styles.css` is now only an ordered `@import` entry
for 15 part files under `frontend/src/app/styles/` (tokens and base first, shell, the area files, `responsive-shared.css`
last). The cascade order is the same as the previous single file (built CSS and computed styles were compared); the only
intended changes are that long repository names wrap (`overflow-wrap: anywhere`) and the knowledge search controls are
44px tall. The raw-colour style guard now scans every part file and checks that each part is imported exactly once, and
e2e checks were added for the `PageState` error panel, the AI panel heading at 375px and long repository names at 375px.
Evidence: independent reviews PASS (2 cycles, 3 lenses); `npm run verify:mvp` PASS at `1860f10` (backend build and
tests, frontend 53 files / 233 tests, Playwright 14 tests, `npm audit` 0 advisories, contract lint).

Open (Low): some text links are 24–26px tall, which meets WCAG 2.5.8 but not the `DESIGN.md` 44px guidance; some dashed borders are
decorative only; no manual screen-reader or 200% zoom pass was performed.

#### 6.1.2 ALIGN (Design Consistency)

A design-consistency audit on 2026-10-23 compared the redesigned screens with `DESIGN.md`; the owner's decisions D1–D12
were recorded in `DESIGN.md` (`2bcde63`) and delivered in the slices below. Every slice changed presentation only: pages
still show stored Rule Engine results and API values, no slice calculates scores or statuses on the client, and no API,
contract, backend or migration changed.

| Slice | Scope | Status |
|---|---|---|
| ALIGN-P1 | Shared page structure | **Resolved (2026-10-23, `6b85d6a`):** a shared `PageHero` band on every page; a `ResultBasis` basis line at the page end instead of repeated basis text; a shared `StatusChip`; tinted tiles instead of solid-indigo tiles and bordered cards; back actions as secondary actions; danger zones at the page end; 44px targets; named progress indicators; reference-line contrast; a `SafeErrorPage`; `color-mix()` tokens covered by the raw-colour style guard. Review cycles: 2 |
| ALIGN-P2 | Copy, terminology and labels | **Resolved (2026-10-23, `bdfa709`):** every AI region shows the badge "AI 생성 · 설명만 제공"; a `NONE` evidence status shows "근거 없음" instead of a numeral; one term each for evidence (근거) and skills (기술 역량), qualified version labels and one label per destination; jargon removed; page-local label maps moved to `shared/ui/labels.ts`; a shared `formatDate`. Review cycles: 2 |
| ALIGN-L | Layout and state behaviour | **Resolved (2026-10-24, `1733a0a`):** the career and company detail pages use the D-7 confirmation flow with focus handling and a success announcement; readiness results are labelled with the evaluated career, with a mismatch notice on history entries; `NetworkError` is raised only for rejected `fetch` calls; the dashboard shows "—" when there is no readiness; careers and companies map to 설정 in the navigation; onboarding shows 미완료 for a step that is ready but not done. Review cycles: 2 |
| ALIGN-E | Evidence and AI results | **Resolved (2026-10-24, `7f12880`):** a shared `EvidenceItem` (type · source · confidence, the observed fact, then metadata) is used for all evidence; AI results are ordered summary → items → footnoted evidence, resolved from data already on the page or from the existing analysis-detail data, without raw IDs; generation jobs keep polling in background tabs and are remembered per subject in `sessionStorage`, so leaving and returning resumes the job (no API change); elapsed time replaces the progress bar; compact skill rows. Review cycles: 2 |
| ALIGN-E POLISH | Follow-up | **Resolved (2026-10-24, `86bfec3`):** spacing of the elapsed-time line, unique accessible names for footnote regions, and no raw snapshot IDs in the UI. Review cycles: 1 |

Evidence: independent reviews PASS for every slice; frontend 55 files / 261 tests PASS; build PASS; Playwright 15 tests
PASS. A full `npm run verify:mvp` run after these slices is not recorded here.

Resolved by ALIGN (earlier open items): inconsistent AI badge text (now "AI 생성 · 설명만 제공"; see the UX-2 row), mixed
evidence terms, a numeral shown for `NONE` evidence, solid-indigo tiles, bordered cards, duplicated basis text, unnamed
progress indicators, `aria-label` on generic `div` elements, index-based AI evidence names ("분석 근거 N", now footnotes),
treating any `TypeError` as a network failure (UX-6), the missing D-7 confirmation on career/company detail pages, and
readiness results without the evaluated career name.

Open (Low, deferred):

- Dashboard target baseline and step tiles (audit A-24): need an API change.
- Primary technology on the repository list (audit A-19): needs an API change.
- Anonymous deep links still render the authenticated layout, and the router is created at module import (UX-1).
- The background behind `ConfirmDialog` is not made inert (UX-4).
- Retry and back labels are generic (UX-6).
- The analysis button for an archived repository is disabled without an explanation.
- A 404 when looking up a stored generation job does not clear the stored job.
- No manual screen-reader or 200% zoom pass was performed.
- ALIGN-P1 reformatted about 9 files (cosmetic only).

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
