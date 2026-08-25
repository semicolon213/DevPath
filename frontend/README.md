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
- Repository, synchronization, analysis, Skill Matrix, Career/Company catalog, readiness, dashboard, recommendation,
  and roadmap routes, including immutable analysis/Skill Matrix comparison, current skill detail/evidence,
  current/historical career readiness and Skill Gaps, recommendation history/detail/evidence, and current
  roadmap/history/archive states.
- Repository detail renders immutable PR/issue/document counts plus deterministic PR-review, issue-lifecycle, and
  README section evidence. These are observable facts only; the browser does not calculate official scores.
- Loading, empty/anonymous, success, partial where applicable, and error states for implemented asynchronous views.
- Vite 8/Vitest 4 build-test foundation, React Testing Library feature tests, and Playwright Chromium critical browser journeys.
- Automated WCAG 2.0 A/AA and WCAG 2.1 AA checks with axe on critical success and anonymous failure paths.

Not implemented:

- Notion integration, AI generation, artifacts, portfolio, resume, interview, and administration workflows.
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
