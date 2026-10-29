# DevPath 에이전트 공통 규칙

이 파일은 저장소에서 작업하는 모든 에이전트(Codex, Claude Code 등)의 공통 규칙이다. 다른 지침 파일을 읽었다고 가정하지 않는다. 하위 디렉터리에 더 구체적인 `AGENTS.md`가 있으면 그 파일이 우선한다. 현재 하위 규칙 파일은 없다.

DevPath는 AI 보조 개발자 커리어 분석 플랫폼이다. GitHub와 Notion 근거를 분석하고 결정론 엔진이 측정 가능한 결과를 계산한다. AI는 근거에 기반한 설명, 추천 설명, 커리어 문서 초안 생성에만 쓴다.

## 1. 제품 불변 규칙

1. 공식 점수는 Rule Engine만 계산한다.
2. AI는 점수, 가중치, 준비도(readiness), 추천 우선순위, 결정론적 비즈니스 결과를 계산·변경·추측·순위화하지 않는다.
3. 직무(Career) 선택은 적용되는 평가 규칙을 바꾼다.
4. 회사(Company) 선택은 회사별 평가·추천 정책을 바꾼다.
5. 모든 공식 결과는 측정 가능하고 재현 가능하며 필요한 곳에서는 버전이 관리되고 근거까지 추적된다.
6. AI 출력은 형식·근거(grounding)·증거·안전 검증을 통과하기 전까지 신뢰하지 않는다.
7. PostgreSQL이 구조화 데이터의 유일한 기준 저장소다. 캐시, 벡터, projection, LLM context는 원본이 아니다.
8. 사용자 소유 데이터는 사용자별로 격리한다. 수집, 저장, 조회, 변경·삭제, 프롬프트 조립, 생성, 내보내기, 게시 전에 권한을 확인한다.

이 규칙과 충돌하는 변경 요청은 수행하지 말고 중단한 뒤 보고한다.

## 2. 문서 우선순위

문서가 서로 충돌하면 다음 순서로 판단한다.

| 순위 | 문서 | 다루는 내용 |
|---|---|---|
| 1 | `docs/SRS.md`, `docs/SCORING_RULES.md` | 요구사항, 점수 공식·가중치·밴드·정책 버전 |
| 2 | `docs/ARCHITECTURE.md`, `docs/SECURITY.md` | 구조, 모듈 경계, 데이터 모델, 보안 규칙 |
| 3 | `contracts/openapi/devpath-openapi.yaml` | 엔드포인트와 스키마의 원본 |
| 4 | `docs/TEST_PLAN.md` | 테스트 전략과 검증 기준 |
| 5 | `DESIGN.md` | UI 시각·상호작용·반응형·상태·문구·접근성 (UI 작업에 한함) |
| 6 | 기존 코드 관례 | 모듈 내부 패턴 |

- 기술 선택은 `docs/ADR.md`에서 상태가 **Accepted**인 결정만 따른다.
- `docs/STATUS.md`는 구현 상태 안내일 뿐 증거가 아니다. 작업 전에 실제 코드, 마이그레이션, 계약, 새로 실행한 테스트로 확인하고 과거 테스트 결과를 새 결과처럼 보고하지 않는다.
- 문서는 한국어가 원본이다.
- 문서와 코드가 다르면 추측해서 맞추지 말고 보고한다.

## 3. 아키텍처 기준

| 영역 | 기준 |
|---|---|
| 백엔드 | Java 21, Spring Boot 3.3.5, 모듈러 모놀리스, 헥사고날 경계 |
| 프론트엔드 | React 18, TypeScript, Vite, React Router, React Query |
| 데이터 | PostgreSQL, Spring Data JPA(도메인 모델과 분리된 persistence 모델), Flyway, pgvector |
| 비동기 작업 | PostgreSQL durable job과 transactional outbox (ADR-027) |
| 인증 | GitHub OAuth2, opaque HttpOnly 서버 세션, CSRF, 제한적 CORS |
| 계약 | 계약 우선 REST, 원본은 `contracts/openapi/devpath-openapi.yaml` |
| AI | provider port 뒤의 Ollama adapter (생성·임베딩). 구조화된 엔진 결과를 입력으로 받고 검증된 설명·초안만 내보낸다 |

저장소 구조:

| 경로 | 내용 |
|---|---|
| `backend/` | Spring Boot 애플리케이션과 테스트 |
| `frontend/` | React 애플리케이션과 테스트 |
| `contracts/` | OpenAPI 계약 |
| `docs/` | 요구사항·아키텍처 문서 |
| `fixtures/` | 결정론 엔진 테스트 fixture |
| `scripts/` | npm 명령 wrapper, 보안 경계 검사, Orca 보조 스크립트 |

백엔드와 프론트엔드는 API 계약으로만 통합한다. 서로의 소스를 가져오지 않는다.

### 3.1 백엔드

패키지는 `com.devpath.<module>.{domain,application,adapter.in,adapter.out,config}`이다. 모듈은 `ai`, `analysis`, `audit`, `career`, `company`, `dashboard`, `identity`, `integration`, `knowledge`, `learning`, `onboarding`, `platform`, `prompt`, `recommendation`, `repository`, `rule`, `shared`이다.

| 계층 | 책임 | 금지 |
|---|---|---|
| `domain` | 비즈니스 개념, 값 객체, 불변식 | Spring, JPA, HTTP, 로깅 구현, provider SDK, AI SDK 의존 |
| `application` | use case, 트랜잭션, port | controller, JPA entity 의존 |
| `adapter.in` | HTTP, 보안, 이벤트, job 진입점 | JPA repository 직접 접근 |
| `adapter.out` | persistence, 외부 provider 구현 | - |
| `config` | 프레임워크 연결 | 비즈니스 로직 |

- Controller는 application use case에 위임한다.
- JPA entity는 outbound persistence adapter에 두고 도메인 객체와 명시적으로 변환한다.
- 트랜잭션 경계는 application service가 소유한다. 외부 네트워크 호출을 긴 DB 트랜잭션 안에서 실행하지 않는다.
- 스키마는 Flyway만 바꾼다. 실행 설정은 `ddl-auto=validate`이며 `update`, `create`, `create-drop`을 쓰지 않는다.
- lost update 위험이 있는 가변 aggregate는 동시성 정책을 명시한다. 불변 스냅샷과 완료된 과거 결과의 변경은 거부한다.
- 공식 점수, readiness, 추천 우선순위, 로드맵 순서는 버전이 지정된 결정론 정책으로만 계산한다. 결과 생성은 재시도에 안전해야 하고 필요한 곳은 정책과 원천 결과 조합으로 멱등성을 보장한다.
- provider SDK 타입을 domain, application 계약, 공개 API에 노출하지 않는다.
- 승인된 ADR 없이 JWT, Redis, jOOQ, Liquibase, 외부 queue 프레임워크, 별도 vector client, object storage client, AI SDK를 추가하지 않는다. Redis(ADR-004)는 Deferred 상태이며 현재 쓰지 않는다.

### 3.2 프론트엔드

- 기능 코드는 `frontend/src/features/<기능>/{api,model,ui}`, 화면은 `frontend/src/pages`에 둔다.
- 서버 상태는 React Query가 소유한다. 인증 사용자, 저장소, job, 점수 같은 서버 자원을 별도 전역 store에 중복 저장하지 않는다.
- `GET /api/v1/users/me`가 브라우저 세션 bootstrap이다. `401`은 익명 상태이고 네트워크 실패는 별도 오류 상태다.
- 자격 증명이 필요한 요청은 공용 API 경계를 거친다.
- 세션 ID, provider 토큰, refresh 토큰, 자격 증명을 `localStorage`·`sessionStorage`에 저장하지 않는다.
- 컴포넌트는 공식 결과를 계산하거나 재계산하거나 의미를 바꾸지 않는다. GitHub, Notion, AI provider를 브라우저에서 직접 호출하지 않는다.
- 비동기 화면은 loading, empty/anonymous, success, 해당 시 partial, error 상태를 접근성 있게 표현한다.

## 4. ADR 게이트

`docs/ADR.md`에서 Accepted인 주요 결정:

| ADR | 내용 |
|---|---|
| 001/002 | 모듈러 모놀리스, 헥사고날 경계 |
| 003 | PostgreSQL 기본 저장소 |
| 005 | 계약 우선 REST |
| 006 | 결정론 엔진과 AI 생성 분리 |
| 012/013 | 기능 중심 React 구조, 서버/클라이언트 상태 분리 |
| 020/021 | Java 21 Spring Boot, React TypeScript |
| 024 | persistence 모델을 분리한 Spring Data JPA |
| 025 | 변경 불가 SQL Flyway 마이그레이션 |
| 026 | GitHub OAuth2 + opaque 서버 세션 |
| 027 | PostgreSQL durable job과 transactional outbox (외부 queue 기술 선정이 아니다) |
| 028 | owner/source/document/model-version 필터를 강제하는 pgvector |
| 029 | S3 호환 object storage port. filesystem adapter는 local/test 전용 |
| 030 | 기능별 embedding/generation provider port |
| 032 | 테스트 도구 체계 |
| 035 | 단순화된 GitHub Flow |

다음 Proposed ADR의 기술은 명시적 결정 없이 고르지 않는다.

| ADR | 영역 |
|---|---|
| ADR-031 | 관측성(observability) 기술 |
| ADR-033 | 배포 플랫폼 |
| ADR-034 | 운영 secret 관리 |

Proposed ADR은 그 결정이 실제로 필요한 작업만 막는다. 무관한 작업은 계속한다. Accepted ADR의 근거를 바꾸거나 Proposed ADR을 Accepted로 표시하지 않는다.

## 5. 작업 방법

수정 전:

1. `git status`로 기존 변경을 확인하고 보존한다. 기존 dirty 변경은 사용자 소유로 간주한다.
2. 요구사항 ID, 관련 Accepted ADR, 영향 모듈, API·데이터·보안 영향, 제외 범위, 완료 기준을 확인한다.
3. 필요한 문서의 관련 부분만 읽는다. 새 추상화를 만들기 전에 기존 패턴을 검색한다.
4. Proposed ADR이 필요한 기술 선택, 요구사항에 없는 기능, 정해지지 않은 계약·스키마·보안 규칙, 상위 문서 충돌이 있으면 중단하고 보고한다.

수정 중:

- 구현 단위는 사용자가 하나의 목표를 처음부터 끝까지 달성하는 수직 기능(capability)이다. 필요한 계약, 백엔드, 프론트엔드, 권한, loading/empty/error/retry 상태, 접근성, 자동화 테스트, 문서 근거를 함께 포함한다.
- 사용자 여정이 실제로 연결·검증되기 전에는 기능 완료를 보고하지 않는다. 계약이나 요구사항이 비어 완성할 수 없으면 임의로 범위를 줄이지 말고 가능한 범위와 blocker를 밝힌다.
- 원인을 고치는 가장 작은 응집된 patch를 만든다. 범용 추상화보다 명확한 도메인 용어를 쓴다.
- 경로, 필드, 역할, provider 동작, 비즈니스 규칙, 미래 기능을 지어내지 않는다.
- 계획된 모듈·기능을 구현된 것처럼 표현하지 않는다.
- 관련 없는 refactor, 대규모 rename, 포맷 정리, 의존성 업그레이드, 문서 재작성을 하지 않는다.
- 테스트는 가장 가까운 적절한 수준에 추가한다. 통과를 위해 테스트, 아키텍처 규칙, 권한, 검증, 보안 통제를 약화하지 않는다.
- 문서는 구현 근거나 결정이 바뀐 경우에만 갱신한다.
- 계약 작업이 백엔드·프론트엔드 작업보다 먼저다. 마이그레이션을 먼저 확정한 뒤 persistence adapter를 수정한다.
- secret, OAuth 자격 증명, 개인 키, 토큰, 실제 사용자 데이터, 생성된 secret 파일을 커밋하지 않는다.

인계 전:

- 영향이 있으면 계약·마이그레이션·secret·아키텍처 검사(`contract:lint`, `security:check` 등)를 실행하고 diff에서 관련 없는 변경과 생성 산출물(generated artifact)을 확인한다.

구현·정리 반복:

- 한 번의 반복은 `하나의 capability 구현 → targeted/broader 테스트와 build → @ponytail-review` 순서다. `@ponytail-review`는 해당 capability diff만 검토하며 적용할 지적은 같은 범위에서 고치고 영향받은 테스트를 다시 실행한다. 쓸 수 없으면 실행했다고 쓰지 말고 미실행으로 보고한다.
- 저장소 전체를 검사하는 `@ponytail-audit`는 큰 정리 시점이나 소유자·코디네이터 요청 때만 실행한다. audit 결과 수정은 범위를 정한 뒤 별도 capability로 처리한다.

다음 작업 선택: 소유자가 지정하지 않았다면 `docs/STATUS.md`의 계획(미구현) 항목, 요구사항, Accepted ADR, 실제 저장소 상태를 보고 고른다. 과거의 다음 작업 목록을 확인 없이 재사용하지 않는다.

## 6. 계약·데이터·보안 규칙

- OpenAPI의 operationId와 method/path 조합을 보존한다.
- `contracts/openapi/devpath-openapi.yaml`에는 실제 구현된 엔드포인트만 넣고 백엔드 handler와 프론트엔드 client를 함께 맞춘다.
- 새 동작은 기존 요구사항 ID(`docs/SRS.md`)로 추적되어야 한다. 없으면 중단하고 요구사항 변경을 요청한다.
- 새 필드·테이블은 `docs/ARCHITECTURE.md`의 데이터 모델에 근거하고 새 immutable Flyway 마이그레이션으로 추가한다. 적용된 마이그레이션은 수정하지 않는다.
- 브라우저 인증은 opaque HttpOnly 서버 세션, CSRF 보호, 제한적 credentialed CORS, session fixation 보호를 쓴다. 보안 판단은 백엔드가 한다.
- 조회, 프롬프트 조립, 생성, 내보내기, 게시 전에 백엔드에서 권한을 검사한다. 캐시나 화면 상태를 권한·정합성 기준으로 쓰지 않는다.
- provider 토큰, 세션 ID, 자격 증명, raw OAuth payload, 비공개 저장소 내용, 임베딩, SQL 오류, stack trace를 노출하지 않는다.
- 운영 로그와 영구 audit 기록은 다르다. 같은 것으로 표현하지 않는다.

## 7. 멀티에이전트 / Orca 수칙

1인 프로젝트다. 역할별 담당자 개념은 없고 작업 범위와 수정 가능 파일은 작업 명세가 정한다.

- Orca가 관리하는 worktree와 작업 인계를 쓴다. 임의의 raw Git worktree를 만들지 않는다.
- 배정된 worktree와 명세에 적힌 경로만 수정한다. 한 파일은 한 번에 한 worker만 수정하고 다른 worker의 변경을 덮어쓰거나 되돌리지 않는다.
- OpenAPI, 마이그레이션 순번, lockfile, 루트 빌드 파일, `docs/ADR.md`, 에이전트 지침(`AGENTS.md`, `CLAUDE.md`)은 명세에 포함된 경우에만 수정한다.
- 공유 계약·마이그레이션·lockfile·공유 커널(shared kernel) 파일이 겹치는 작업은 병렬로 돌리지 않는다.
- 명시적 요청 없이 commit, push, merge, worktree 삭제, history rewrite, 배포를 하지 않는다. capability가 검증되어 보존할 가치가 생기면 commit 시점임을 알리기만 한다.
- 파괴적 테스트, 실제 provider 변경 호출, 운영 대상 작업은 명시적 승인 없이 하지 않는다.
- 상태는 `READY`, `IN_PROGRESS`, `BLOCKED`, `REVIEW_READY`, `DONE`이다. worker는 `REVIEW_READY`까지만 선언하고 `DONE`은 코디네이터 또는 소유자만 선언한다.

중단하고 보고하는 경우: 필요한 ADR이 Proposed이거나 없음, 문서 간 실질적 충돌, 정해지지 않은 API·스키마·보안 계약, 범위 밖 기능 발명, 파일 소유 충돌, 계약·마이그레이션 동시 변경, 필수 검증 불가, secret·개인 데이터 노출 가능성.

기본 통합 순서: 요구사항·ADR 확인 → 도메인 용어·불변식 → API·데이터 계약 → 마이그레이션·persistence 경계 → domain·application → inbound/outbound adapter → 프론트엔드 → 보안·아키텍처·통합·계약 검사 → 문서 갱신.

### 7.1 작업 명세 (코디네이터 → worker)

```text
목표 / 요구사항 ID / Accepted ADR / Proposed ADR 게이트
영향 모듈 / API·데이터·보안 영향
수정 가능 경로 / 제외 범위
완료 기준 / 검증 명령 / 중단 조건 / 통합 순서
```

"백엔드 완성", "AI 시스템 구축", "보안 수정"처럼 모호한 작업은 배정하지 않는다. 하나의 use case, 계약 묶음, 마이그레이션 단계, provider adapter, 프론트엔드 흐름, 보안 통제처럼 측정 가능한 단위로 나눈다.

### 7.2 worker 역할 배정

| 작업 | 담당 |
|---|---|
| 기능 구현, QA 지적에 따른 코드 수정 | Codex |
| 테스트만 추가·강화 (운영 코드·설정·마이그레이션·계약 변경 없음) | 별도 Claude 구현 worker 가능 (소유자 결정 2026-10-18) |
| Codex 구현 뒤 QA 지적에 따른 프론트엔드 표현 수정 (CSS, JSX 마크업·class, 접근 이름, 표시 문구와 그 테스트만. 상태·데이터 흐름·API 호출·라우팅·백엔드·설정·의존성·마이그레이션·계약 변경 없음) | 별도 Claude 구현 worker 가능 (소유자 결정 2026-10-23) |
| 독립 QA, 코드 리뷰 | 별도 Claude 세션 (작성자와 다른 세션) |
| QA 통과 후 문서 갱신 | 별도 Claude 문서 worker |

테스트 전용 Claude worker 조건:
- 새 테스트마다 변이 증거를 낸다. 대상 운영 동작을 잠시 깨뜨려 테스트 실패를 확인하고 복구한 뒤, 최종 diff에 테스트만 남았는지 확인한다.
- 발견한 운영 결함은 고치지 않고 Codex 작업으로 보고한다.

프론트엔드 표현 수정 Claude worker 조건:
- 리뷰 지적 범위 안에서만 수정하고 경계를 벗어나는 항목은 Codex 작업으로 보고한다.

두 경우 모두 그 수정을 작성하지 않은 별도 Claude 리뷰 세션이 검토한다.

## 8. 검증과 보고

저장소 루트에서 실행한다.

| 명령 | 내용 |
|---|---|
| `npm run backend:test` / `backend:build` | Gradle test / build |
| `npm run frontend:install` | `npm ci` |
| `npm run frontend:test` / `frontend:build` | Vitest / Vite build |
| `npm run frontend:e2e` / `frontend:quality` | Playwright / 품질 묶음 |
| `npm run contract:lint` | Redocly OpenAPI lint |
| `npm run docker:check` | Docker 엔진 확인 |
| `npm run security:check` | 보안 경계 검사 |
| `npm run test` | 백엔드 + 프론트엔드 테스트 |
| `npm run verify` | clean build + 프론트엔드 ci·test·build |
| `npm run verify:mvp` | docker·security 검사 + clean build + 프론트엔드 quality + contract lint |

- targeted 검사를 먼저 하고 관련 범위의 broader suite를 실행한다. 바뀌지 않은 스냅샷에 통과한 전체 suite를 이유 없이 반복하지 않는다.
- PostgreSQL/Testcontainers 테스트에는 실행 중인 Docker가 필요하다. PostgreSQL 동작을 H2로 대체하지 않는다. Docker Desktop과 Testcontainers 1.20.3 조합에서 API 호환 문제가 있으면 PowerShell에서 `$env:DOCKER_API_VERSION='1.44'`를 설정한 뒤 실행한다.
- `DEVPATH_DB_URL`이 필요한 조건부 통합 테스트는 설정이 없으면 SKIP될 수 있다. SKIP은 PASS와 구분해 보고한다.
- 로컬 실행: `backend/`에서 `.\gradlew.bat bootRun`(PowerShell) 또는 `./gradlew bootRun`, `frontend/`에서 `npm run dev`. Java 21과 `backend/.env.example`의 필수 환경변수가 필요하다. Spring Boot 자체는 `.env`를 읽지 않지만 Gradle `bootRun` 작업은 `backend/.env`가 있으면 읽어 넣고 프로세스 환경변수를 우선한다. 테스트·다른 실행 경로는 프로세스 환경변수가 필요하다.

검사 결과는 정확한 명령과 함께 `PASS`, `FAIL`, `NOT RUN`(사유 포함)으로 보고한다.

구현 인계 형식:

```text
TASK_ID:
STATUS: REVIEW_READY | BLOCKED
REQUIREMENTS / ACCEPTANCE_CRITERIA:
SNAPSHOT: <commit, diff 또는 worktree revision>
FILES_CHANGED:
IMPLEMENTATION_SUMMARY:
TESTS: <정확한 명령과 PASS | FAIL | NOT RUN>
API_DATA_SECURITY_IMPACT:
DOCS_IMPACT: <문서 영향>
KNOWN_RISKS / DEVIATIONS:
INTEGRATION_ORDER / DEPENDENCIES: <권장 통합 순서와 의존성>
BLOCKERS / NEXT:
```

문서 작업이나 검토만 한 경우에도 실제 변경 파일, 검증 여부, 미확인 사항을 짧게 보고한다.

## 9. Codex worker 프롬프트 전달과 미제출 복구

Windows의 Orca 1.4.226 + Codex CLI 0.162.1에서 큰 `worker-start` 프롬프트가 Codex 입력창에 `[Pasted Content N chars]`로 붙여진 채 제출되지 않는 문제가 있다(upstream stablyai/orca #11343, #14822). Orca 릴리스가 고쳤음이 확인될 때까지 다음을 지킨다.

1. Codex `worker-start --spec`은 약 800자 이하로 쓴다. 역할, 명세 파일 절대 경로, 완료 조건만 넣는다. 상세 명세(7.1)는 저장소 밖, worker가 읽을 수 있는 경로의 파일에 둔다. 추적되는 저장소 경로에 명세 파일을 만들지 않는다.
2. `worker-start`가 `ready` 또는 turn 시작 관찰을 반환하면 터미널에 아무 입력도 보내지 않는다.
3. 다음을 모두 만족할 때만 Enter 한 번(`orca terminal send --terminal <handle> --enter --json`)을 보낸다.
   1. `worker-start` 영수증이 `outcome_unknown`이고 stage가 `turn_start_unobserved`다.
   2. `worker-show`에서 Dispatch가 `pending`이고 turn 시작이 관찰되지 않았고 `assigneeHandle`이 영수증의 agent 터미널과 같다(handle은 Dispatch에서 다시 읽는다).
   3. `terminal read --screen`에서 Codex 입력창(`draft` 또는 `› ` 줄)에 `[Pasted Content N chars]` 표시만 있다.
   4. 화면에 `Working`, `esc to interrupt`, 확인·선택·권한 프롬프트가 없다(고정 헤더 `permissions: <mode>`는 프롬프트가 아니다).
   5. 5초 간격 두 번의 화면 읽기 결과가 같다.
   6. 보내기 직전에 Dispatch 상태와 화면을 다시 확인한다.
4. `terminal wait --for tui-idle`을 복구 전제 조건으로 쓰지 않는다. Codex는 첫 turn 전에 agent 상태를 보고하지 않아 이 상태에서 반드시 timeout된다.
5. Enter 후 표시가 사라지고 `Working`이 나타났는지 확인하고 Task·Dispatch ID가 일치하는 `worker_done`을 요구한다. 수동 Enter 뒤에는 Orca가 `turn_started`를 기록하지 않을 수 있으므로 화면 상태와 `worker_done`이 증거다.
6. 확인 항목 하나라도 실패하거나, 화면이 예상과 다르거나, turn이 시작되지 않으면 `BLOCKED`로 멈춘다. Enter 반복, 프롬프트 재전송, Orca 패치를 하지 않는다.
7. `node scripts/orca/codex-submit-guard.mjs <worker-start-receipt.json>`이 3~6을 구현한다. 기본은 dry run이고 `--execute`일 때만 보내며 Dispatch별 배타 marker로 Dispatch당 Enter 한 번만 허용한다(종료 코드 0: `RECOVERED`, `NO_ACTION`, `WOULD_SEND` / 2: `BLOCKED` / 1: 사용법 오류). Orca나 Codex를 업그레이드할 때마다 다시 검증한다.
