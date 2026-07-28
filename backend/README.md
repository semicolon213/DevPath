# DevPath Backend

The backend contains the first Identity and PostgreSQL persistence vertical slice.

## Implemented Scope

- GitHub OAuth2 login mapped to an internal DevPath `User`.
- Stable `(provider, provider_subject)` external identity linking.
- Spring Data JPA persistence models isolated from domain objects.
- Flyway-owned PostgreSQL and Spring Session JDBC schema.
- Opaque server-managed session, current-user API, CSRF, CORS, and logout.
- Domain, application, persistence, security, and architecture test sources.

Repository synchronization and all Rule, Career, Knowledge, AI, and artifact modules remain unimplemented.

## Prerequisites

- Java 21 LTS.
- PostgreSQL 16-compatible local database.
- A GitHub OAuth App with callback URL `http://localhost:8080/login/oauth2/code/github`.
- Frontend origin `http://localhost:5173` for the default local profile.

## Local Configuration

Copy values from `.env.example` into the shell environment. All sample credentials are placeholders.
The `local` profile disables the `Secure` attribute only for local HTTP; shared environments default it to enabled.

Required values:

- `DEVPATH_DB_URL`
- `DEVPATH_DB_USERNAME`
- `DEVPATH_DB_PASSWORD`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`
- `DEVPATH_FRONTEND_ORIGIN`

## Commands

From the repository root:

```text
node scripts/run-gradle.mjs clean test
node scripts/run-gradle.mjs build
```

To run locally after exporting the environment variables:

```text
cd backend
gradlew.bat bootRun
```

Flyway applies `V1__create_identity_and_session_schema.sql`; Hibernate validates but does not mutate the schema.
