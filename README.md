# DevPath

DevPath is an AI-powered Developer Career Intelligence Platform. The repository now contains its first authenticated Identity and PostgreSQL persistence vertical slice.

## Current Status

The implemented slice establishes GitHub OAuth login, internal user identity, external identity linking, Flyway migrations, JPA persistence, server-side JDBC sessions, current-user/logout APIs, and frontend session bootstrap. GitHub repository synchronization and all scoring, career, knowledge, prompt, AI, and artifact workflows remain unimplemented.

## Accepted Stack

- Backend: Java 21 LTS, Spring Boot, Gradle Wrapper, modular monolith.
- Frontend: React, TypeScript, Vite, React Query, React Router-compatible routing.
- Repository: monorepo with strict frontend/backend/API boundaries.
- Tests: JUnit, Spring Test, ArchUnit-style tests, Vitest, React Testing Library, OpenAPI validation foundation.
- Workflow: simplified GitHub Flow with short-lived branches and review.

## Repository Layout

| Path | Purpose |
|---|---|
| `docs/` | Architecture and requirements documentation. |
| `backend/` | Spring Boot backend scaffold and backend tests. |
| `frontend/` | React frontend scaffold and frontend tests. |
| `contracts/` | Contract artifacts, including the minimal OpenAPI scaffold. |
| `fixtures/` | Deterministic test fixture roots for later Rule and Career engine work. |
| `scripts/` | Root command helpers. |

## Prerequisites

- Java 21 LTS and PostgreSQL are required for backend compilation and runtime verification.
- Node.js with npm is required for frontend installation and verification.
- Exact dependency versions are pinned in backend and frontend manifests.

## Root Commands

```text
npm run backend:build
npm run backend:test
npm run frontend:install
npm run frontend:build
npm run frontend:test
npm run test
npm run verify
```

The root command manifest coordinates commands only. Backend and frontend own their own dependency management.

## Local Run

Backend:

```text
set SPRING_PROFILES_ACTIVE=local
cd backend
gradlew.bat bootRun
```

Frontend:

```text
cd frontend
npm ci
npm run dev
```

Configure a GitHub OAuth App callback as `http://localhost:8080/login/oauth2/code/github` and use `backend/.env.example` for placeholder property names. The frontend bootstraps the authenticated session from `GET /api/v1/users/me`.

## Current Limitations

- Java 21-dependent backend verification is required before this slice is implementation-ready.
- No Redis, queue, GitHub repository SDK, vector DB, object storage, or AI SDK.
- No GitHub, Notion, Rule Engine, Career Engine, Knowledge, Prompt, AI, Portfolio, Resume, or Interview feature implementation.
- No production deployment or CI/CD configuration.

## Architecture References

- `docs/18_ADR.md` for accepted scaffolding ADRs.
- `docs/11_Backend_Architecture.md` for backend boundaries.
- `docs/12_Frontend_Architecture.md` for frontend boundaries.
- `docs/15_Test_Architecture.md` for test strategy.
- `docs/19_Roadmap.md` for implementation sequencing.

## Contribution Workflow

Use short-lived branches such as `feature/<topic>`, `fix/<topic>`, `docs/<topic>`, `adr/<topic>`, `chore/<topic>`, or `codex/<topic>`. Keep changes small, reviewable, and aligned with ADR-035.
