# DevPath Frontend

This is the React + TypeScript + Vite frontend for DevPath.

## Scope

Implemented:

- React Router 7.18.2 declarative routing, React Query session bootstrap, and shared credentialed API boundary.
- Persisted onboarding progress and the connected first-analysis journey across target selection, GitHub connection,
  repository import, synchronization, deterministic analysis, and dashboard/result workspaces.
- Dedicated `/settings`, `/settings/profile`, and `/settings/integrations` workspaces for profile/target management,
  explicit active/expired/revoked GitHub connection recovery, disconnect, and repository registration. Inactive
  connections never trigger a provider repository read before reauthorization. GitHub quota exhaustion retains the
  connection, suppresses automatic retry bursts, and displays the provider-safe reset time.
- The integrations workspace also manages Notion OAuth connection/recovery/disconnect and bounded shared page/data-source
  metadata discovery with loading, empty, refresh, rate-limit, and error states. It never receives provider tokens or
  page body content, and only renders external links for validated HTTPS Notion hosts.
- Repository, synchronization, analysis, Skill Matrix, Career/Company catalog, readiness, dashboard, recommendation,
  and roadmap routes, including immutable analysis/Skill Matrix comparison, current skill detail/evidence and
  cursor-paginated per-skill history composed only from stored immutable Matrix assessments,
  current/historical career readiness and Skill Gaps, recommendation history/detail/evidence, and current
  roadmap/history/archive states.
- The repository workspace provides the full owner lifecycle: URL-restorable archived filtering, accessible archive
  impact confirmation, immediate archive/restore actions, provider-archived/deleted recovery guidance, and retained
  access to historical snapshots/results while new sync and analysis commands are disabled. Analysis job IDs also stay
  in the route query across refresh and successful jobs link directly to their official stored result.
- Repository detail renders immutable PR/issue/document counts, deterministic PR-review, issue-lifecycle, and README
  section evidence, plus a bounded newest-first current-snapshot activity timeline. It displays measured elapsed days
  without inventing a staleness threshold. These are observable facts only; the browser does not calculate official scores.
- Repository synchronization keeps its owner-scoped job ID in the route query so polling survives a browser refresh.
  Completed jobs and snapshot-history cards lead to an API-REP-008 detail route with full revision/content hashes,
  measured collection counts, current-versus-historical context, and uniform unavailable-resource handling.
  The same route walks cursor-paginated API-REP-012 history to show only completed analyses that reference the selected
  snapshot, while isolating analysis-history failures from immutable provenance metadata.
- Large repositories that cross a server collection ceiling display a non-retryable, actionable failure and explicitly
  state that no partial snapshot was created.
- Loading, empty/anonymous, success, partial where applicable, and error states for implemented asynchronous views.
- Vite 8/Vitest 4 build-test foundation, React Testing Library feature tests, and Playwright Chromium critical browser journeys.
- Automated WCAG 2.0 A/AA and WCAG 2.1 AA checks with axe on critical success and anonymous failure paths.
- A reduced-motion Chromium journey verifies that nonessential transitions are removed while skip-link, keyboard,
  route-focus, analysis actions, and accessible semantics remain operable.

Not implemented:

- Notion content ingestion/analysis, AI generation, artifacts, portfolio, resume, interview, and administration workflows.
- Company-specific readiness/recommendation calculations and production deployment.

## Commands

Node.js 20.19+ or 22.12+ is required by the pinned build, routing, and browser-test toolchain.

```text
npm ci
npm run dev
npm run test
npm run test:e2e
npm run build
npm run quality
```

Install the pinned Playwright browser once before the E2E command:

```text
npx playwright install chromium
```

Copy `.env.example` only for local environment setup. Do not commit real `.env` files.
