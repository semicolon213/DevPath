# DevPath Backend

The backend contains the first Identity and PostgreSQL persistence vertical slice.

## Implemented Scope

- GitHub OAuth2 login mapped to an internal DevPath `User`.
- Stable `(provider, provider_subject)` external identity linking.
- Spring Data JPA persistence models isolated from domain objects.
- Flyway-owned PostgreSQL and Spring Session JDBC schema.
- Opaque server-managed session, current-user API, CSRF, CORS, and logout.
- Separate GitHub App user authorization with encrypted provider credentials, refresh, full bounded pagination, reauthorization, and disconnect.
- Separate Notion workspace authorization with encrypted rotating credentials, bounded page/data-source metadata
  discovery, permission-loss expiry, metadata deletion, reauthorization, and disconnect. Page body content is not stored.
- Append-only PostgreSQL audit records for authentication and GitHub connection security events.
- Owner-scoped GitHub repository import, duplicate-safe registration, bounded listing, and detail APIs.
- PostgreSQL-backed repository synchronization jobs with request-thread provider isolation, stale-worker lease recovery, bounded jittered retries, non-retryable collection-limit failures, and transactional outbox records.
- GitHub branch/commit collection and immutable, owner-scoped repository snapshots.
- Korean repository selection, workspace, and metadata detail screens in the frontend.
- Domain, application, persistence, security, and architecture test sources.

Notion knowledge-document ingestion, incremental content synchronization, retrieval, AI, and artifact workflows remain
unimplemented. The Notion integration currently registers and manages the authorized source plus bounded metadata only.

## Prerequisites

- Java 21 LTS.
- PostgreSQL 16-compatible local database exposed on `localhost:15432` by default.
- A GitHub OAuth App with callback URL `http://localhost:8080/login/oauth2/code/github`.
- A GitHub App installed on at least one repository with user authorization callback URL `http://localhost:8080/api/v1/integrations/github/callback`.
- Frontend origin `http://localhost:5100` for the default local profile.

## Local Configuration

Copy `.env.example` to `.env` in this directory and replace the placeholder values. The Gradle `bootRun` task reads `backend/.env` automatically; environment variables already set in the launching process take precedence. The file is used only for local `bootRun` and remains excluded from version control.

All sample credentials are placeholders. The `local` profile disables the `Secure` attribute only for local HTTP; shared environments default it to enabled.

`DEVPATH_WORKER_ENABLED=true` enables the PostgreSQL job worker in the shared backend artifact for local development. Set it to `false` for an API-only runtime and run another instance with it enabled for the worker role. Poll and initial-delay values are milliseconds.

Repository and analysis jobs share a dedicated two-thread scheduler. `DEVPATH_WORKER_SHUTDOWN_TIMEOUT` defaults to
`10m`; it must remain positive and no longer than the persisted `15m` worker lease. Shutdown stops new claims and waits
for already claimed work inside that window. PostgreSQL transaction advisory locks make simultaneous equivalent job
requests return the first active job.

GitHub provider calls use bounded `DEVPATH_GITHUB_CONNECT_TIMEOUT` and `DEVPATH_GITHUB_READ_TIMEOUT` duration values;
the defaults are `5s` and `30s`. Zero or negative values fail startup.

Required values:

- `DEVPATH_DB_URL`
- `DEVPATH_DB_USERNAME`
- `DEVPATH_DB_PASSWORD`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`
- `GITHUB_INTEGRATION_CLIENT_ID`
- `GITHUB_INTEGRATION_CLIENT_SECRET`
- `DEVPATH_PROVIDER_CREDENTIAL_KEY` (Base64-encoded 32-byte AES key)
- `DEVPATH_PROVIDER_CREDENTIAL_KEY_VERSION`
- `NOTION_INTEGRATION_CLIENT_ID`, `NOTION_INTEGRATION_CLIENT_SECRET`, `NOTION_INTEGRATION_REDIRECT_URI`
- `DEVPATH_FRONTEND_ORIGIN`

## Commands

From the repository root:

```text
node scripts/run-gradle.mjs clean test
node scripts/run-gradle.mjs build
```

To run locally after creating `backend/.env`:

```text
cd backend
gradlew.bat bootRun
```

Flyway applies the immutable migrations in `db/migration`, including the encrypted provider-credential schema; Hibernate validates but does not mutate the schema.

Docker Engine 29 rejects the legacy API version used by older Docker Java clients. When running Testcontainers against Docker 29, set `DOCKER_API_VERSION=1.44`; the Gradle test task forwards an explicitly configured value to the test JVM.
