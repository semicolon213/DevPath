# DevPath Codex 및 OpenAI 에이전트 작업 규칙

## 1. 적용 범위

이 파일은 저장소 전체에 적용한다. 변경 대상 디렉터리 아래에 더 구체적인 `AGENTS.md`가 있으면 해당 파일을 우선 적용한다. 현재 하위 규칙 파일은 없다.

DevPath는 AI 보조 개발자 커리어 인텔리전스 플랫폼이다. GitHub와 향후 연동될 지식 소스의 근거를 분석하고, 결정론 엔진이 측정 가능한 결과를 계산한다. AI는 공식 결과를 계산하지 않으며 설명이나 초안 생성에만 사용한다.

## 2. 절대 변경할 수 없는 제품 원칙

1. 공식 점수는 Rule Engine이 계산한다.
2. AI는 공식 점수, 가중치, 준비도, 추천 우선순위 또는 결정론적 비즈니스 결과를 계산하거나 변경하거나 추측하지 않는다.
3. Career 선택에 따라 적용되는 평가 규칙이 달라진다.
4. Company 선택에 따라 회사별 평가 및 추천 정책이 달라진다.
5. 모든 공식 결과는 측정 가능하고 재현 가능해야 하며, 필요한 경우 버전이 관리되고 근거까지 추적되어야 한다.
6. AI 출력은 검증되기 전까지 신뢰하지 않는다.
7. PostgreSQL이 구조화 데이터의 유일한 기준 저장소다. Redis, 캐시, projection, LLM context는 원본 데이터가 아니다.
8. 사용자 소유 데이터는 사용자별로 격리하며 모든 조회와 변경 경계에서 소유권을 확인한다.

이 원칙과 충돌하면 규칙을 약화하지 말고 작업을 중단한 뒤 충돌 내용을 보고한다.

## 3. 문서 권한 순서

문서가 서로 충돌하면 다음 순서로 판단한다.

1. `docs/01_SRS.md`와 결정론 엔진 명세(`docs/02_Rule_Engine.md`, `docs/03_Career_Path_Engine.md`)
2. `docs/07_Domain_Model.md`와 `docs/08_System_Data_Model.md`
3. `docs/10_API_Specification.md`와 기계 판독 계약
4. `docs/11_Backend_Architecture.md`와 `docs/12_Frontend_Architecture.md`
5. 보안, 관측성, 테스트, 데이터베이스, 배포 문서
6. `docs/17_Coding_Standards.md`
7. 모듈 README와 로컬 관례
8. 개별 구현 선호

추가 규칙:

- `docs/00_Project_Context.md`는 프로젝트 비전과 변경 불가 제품 철학을 정의한다.
- `docs/18_ADR.md`는 기술 결정의 기준이다. 상태가 **Accepted**인 ADR만 선택된 기술로 취급한다.
- `docs/19_Roadmap.md`는 구현 순서와 마일스톤 게이트를 통제하지만 요구사항이나 Accepted ADR을 무효화하지 않는다.
- `DESIGN.md`는 저장소 수준 UI/UX 요구사항이다. 시각 디자인, 상호작용, 반응형, 상태, 콘텐츠, 접근성 작업 전에 읽는다. SRS와 프론트엔드 아키텍처보다 우선하지 않는다.
- `docs_ko/`는 편의를 위한 한국어 번역본이다. 정확한 의미가 필요하면 `docs/` 원문을 사용한다.

## 4. 현재 구현 기준선

구현됨:

- Java 21, Spring Boot 3.3.5 백엔드와 Node.js 20.19+/22.12+, React TypeScript, React Router 7.18.2, Vite 8 프론트엔드
- 모듈러 모놀리스 및 헥사고날 패키지 경계
- GitHub OAuth2 로그인, 서버 관리 opaque session, CSRF, 로그아웃, 현재 사용자 조회
- 공급자 독립 내부 `User` identity와 GitHub `ExternalIdentity` 연결
- 사용자 프로필과 Career/Company preference
- GitHub 연결, 저장소 검색 및 가져오기
- 저장소 등록·보관·복원 수명주기, URL 기반 보관 필터, 동기화 작업과 새로고침 복구, immutable snapshot 목록·상세 추적, 대형 저장소 수집 상한의 비재시도 안전 실패, 언어·dependency·file·PR·review·issue·README 기반 기술·비점수 근거 및 현재 스냅샷 활동 타임라인 추출
- 분석 작업과 새로고침 복구, 완료 결과 직접 이동, 이력, 스냅샷별 공식 분석 추적, 결정론 Rule Engine, 점수 상세, 근거 연결
- Skill Matrix 생성·조회·비교, 기술별 불변 평가 이력 탐색
- Backend/Frontend Career catalog 및 Career Readiness/Skill Gap 계산
- Company catalog 조회. 회사별 readiness와 추천 정책은 아직 포함하지 않음
- `recommendation-v1` 결정론 추천과 `roadmap-v1` 학습 로드맵
- owner-scoped REST API와 구현된 범위의 OpenAPI 계약
- React Query 기반 세션 bootstrap 및 repository, analysis, skill, readiness, roadmap 화면
- PostgreSQL, Spring Data JPA adapter, Flyway V1~V20 migration
- repository sync 및 analysis를 위한 현재 프로세스 내부 작업 실행 기반
- 도메인, 애플리케이션, 영속성, 보안, 아키텍처 및 프론트엔드 테스트

구현되지 않음:

- Notion 연동과 지식 수집·검색 파이프라인
- 외부 queue 기반 background job 기술
- vector database, object storage, AI provider SDK
- AI 설명 및 초안 생성 workflow
- 회사별 readiness 및 회사별 추천 modifier
- 외부 강의, 도서, 자격증 자동 추천
- Portfolio, Resume, Interview, Administration workflow
- production 배포와 production secret 관리

현재 검증 근거:

- Docker와 `DOCKER_API_VERSION=1.44`를 사용한 백엔드 테스트: 146건 중 144건 PASS, 실패 0건, 외부 DB 환경변수 조건부 테스트 2건 SKIP
- Testcontainers 기반 PostgreSQL 통합 테스트 14건 PASS
- 백엔드 build PASS
- 프론트엔드 Vitest 35개 파일 83건 PASS, Playwright Chromium 및 axe 사용자 여정 6건 PASS, production build와 전체 npm audit PASS
- 플랫폼 중립 `security:check`가 민감 파일·고신뢰 credential·위험한 JPA schema mode·브라우저 credential 저장·브라우저 provider 직접 호출을 차단
- OpenAPI 유효성 검사 PASS. 기존 권고 경고 8건 존재

계획된 모듈을 구현된 기능으로 표현하지 않는다. 이 기준선 이후의 변경은 실제 코드와 검증 결과에 근거해서만 갱신한다.

## 5. 승인된 결정과 미결정 게이트

중요한 승인 기준:

- ADR-001/002: 헥사고날 경계를 가진 모듈러 모놀리스
- ADR-003: PostgreSQL 기본 구조화 저장소
- ADR-005: 계약 우선 REST API
- ADR-006: AI와 분리된 결정론 엔진
- ADR-012/013: 기능 중심 React 구조와 server/client state 분리
- ADR-020/021: Java 21 Spring Boot 및 React TypeScript
- ADR-024: 별도 persistence model을 사용하는 Spring Data JPA/Hibernate
- ADR-025: 변경 불가 SQL Flyway migration
- ADR-026: opaque server-managed session 기반 GitHub OAuth2
- ADR-032: 현재 테스트 도구 체계
- ADR-035: 단순화된 GitHub Flow

다음 Proposed ADR 대상 기술을 임의로 선택하지 않는다.

- ADR-027 background job 기술
- ADR-028 vector database
- ADR-029 object storage
- ADR-030 AI provider SDK 전략
- ADR-031 observability 기술
- ADR-033 배포 플랫폼
- ADR-034 production secret 관리

Proposed ADR은 해당 결정이 실제로 필요한 작업만 차단한다. 그 결정과 무관한 모듈 작업은 계속할 수 있다. 현재 프로세스 내부 작업 실행기를 외부 queue 기술 선정으로 표현하지 않는다.

## 6. 저장소 구조

| 경로 | 책임 |
|---|---|
| `backend/` | Spring Boot 애플리케이션과 백엔드 테스트 |
| `frontend/` | React 애플리케이션과 프론트엔드 테스트 |
| `contracts/` | OpenAPI와 기계 판독 계약 |
| `docs/` | 권위 있는 요구사항과 아키텍처 문서 |
| `docs_ko/` | 편의를 위한 한국어 번역본 |
| `fixtures/` | 결정론 엔진 테스트 fixture |
| `scripts/` | 저장소 명령 wrapper |

백엔드 소스를 프론트엔드로 가져오거나 프론트엔드 소스를 백엔드로 가져오지 않는다. 두 영역은 API 계약으로만 통합한다.

## 7. 백엔드 규칙

`com.devpath.<module>` 아래에 기능 패키지를 만들고 다음 경계를 지킨다.

- `domain`: entity, value object, invariant, domain service. Spring, JPA, OAuth SDK, HTTP, logging 구현, AI SDK에 의존하지 않는다.
- `application`: use case, transaction, application port. controller나 JPA entity에 의존하지 않는다.
- `adapter.in`: HTTP, security, event, job 진입점
- `adapter.out`: persistence 및 외부 provider 구현
- `config`: framework wiring만 담당

필수 규칙:

- Controller는 application use case에 위임하고 JPA repository에 직접 접근하지 않는다.
- JPA entity는 outbound persistence adapter에 두고 domain object와 명시적으로 변환한다.
- schema 변경은 Flyway만 담당한다. 일반 실행 환경에서는 `ddl-auto=validate`를 사용하고 `update`, `create`, `create-drop`을 사용하지 않는다.
- transaction 경계는 application service가 소유한다.
- 외부 네트워크 호출을 긴 database transaction 내부에서 실행하지 않는다.
- lost update 위험이 있는 mutable aggregate는 동시성 정책을 명시한다.
- immutable snapshot과 완료된 과거 결과의 변경을 거부한다.
- provider SDK type을 domain, application contract 또는 public API에 노출하지 않는다.
- 승인된 범위와 ADR 없이 JWT, Redis session, jOOQ, Liquibase, queue framework, vector client, object storage client, AI SDK를 추가하지 않는다.
- 공식 점수, readiness, recommendation priority와 roadmap ordering은 버전이 지정된 결정론 정책으로만 계산한다.
- 결과 생성은 재시도에 안전하고, 필요한 곳에서는 policy와 source result 조합으로 멱등성을 보장한다.

## 8. 프론트엔드 규칙

- 제품 코드는 `frontend/src/features` 아래에서 기능별로 구성한다.
- server state는 React Query가 소유한다. 인증 사용자, repository, job, score 또는 다른 서버 자원을 별도 global store에 중복 저장하지 않는다.
- `GET /api/v1/users/me`를 브라우저 session bootstrap의 기준으로 사용한다.
- `401`은 익명 상태이고 전송 실패는 별도의 오류 상태다.
- credential이 필요한 요청은 공용 API boundary를 통해 보낸다.
- session ID, provider token, refresh token 또는 애플리케이션 credential을 `localStorage`나 `sessionStorage`에 저장하지 않는다.
- Component에서 공식 점수, readiness, 추천 우선순위 또는 비즈니스 규칙을 계산하지 않는다.
- 모든 비동기 화면은 loading, empty/anonymous, success, 적용 가능한 partial, error 상태를 접근성 있게 표현한다.
- 브라우저에서 GitHub, Notion 또는 AI provider를 직접 호출하지 않는다.
- API가 반환한 공식 결과를 화면 표시 목적으로 다시 계산하거나 의미를 바꾸지 않는다.

## 9. API, 데이터 및 보안 규칙

- `docs/10_API_Specification.md`의 canonical API ID와 method/path 조합을 보존한다.
- 실제 구현된 endpoint만 `contracts/openapi/devpath-openapi.yaml`에 추가한다.
- API 변경, 백엔드 handler, 프론트엔드 client를 항상 동기화한다.
- 새 제품 동작은 기존 requirement ID까지 추적 가능해야 한다. 요구사항이 없으면 작업을 중단하고 요구사항 변경을 요청한다.
- 새 persistence field나 table은 domain/data/database 문서에 근거해야 하며 새로운 immutable Flyway migration으로 추가한다. 적용된 migration을 수정하지 않는다.
- 브라우저 인증은 opaque HttpOnly server session, CSRF 보호, 제한적인 credentialed CORS, session fixation 보호를 사용한다.
- provider token, session ID, credential, raw OAuth payload, private repository content, embedding, SQL 오류 또는 stack trace를 노출하지 않는다.
- 운영 로그는 영구 audit record가 아니다. 두 개를 동일하다고 표현하지 않는다.
- 조회, prompt assembly, 생성, export, publication 전에 백엔드에서 권한을 검사한다.
- 캐시나 화면 상태를 권한 판정 또는 데이터 정합성의 기준으로 사용하지 않는다.

## 10. 변경 작업 절차

수정 전:

1. `git status`를 확인하고 기존 사용자 변경을 보존한다.
2. requirement ID, 현재 roadmap milestone, 관련 Accepted ADR, 영향받는 module, API/data/security 영향, 제외 범위를 확인한다.
3. 작업에 필요한 권위 문서의 관련 부분만 읽는다.
4. Proposed ADR이 필요한 기술을 선택해야 하거나, 요구사항에 없는 비즈니스 기능을 발명하거나, 계약·schema 결정이 없거나, 상위 문서와 충돌하면 중단하고 보고한다.

수정 중:

- 기본 구현 단위는 개별 화면, endpoint 또는 component가 아니라 사용자가 하나의 목표를 처음부터 끝까지 달성할 수 있는 큰 수직 기능(capability)이다.
- 수직 기능에는 필요한 contract, backend, frontend, 권한·보안, loading/empty/error/retry 상태, 접근성, 자동화 test, 문서 근거를 함께 포함한다.
- 이미 구현된 하위 기능은 재사용하되, 최종 사용자 여정이 실제로 연결되고 검증되기 전에는 기능 단위 구현을 완료했다고 보고하지 않는다.
- contract나 상위 요구사항이 비어 있어 전체 기능을 안전하게 완성할 수 없으면 임의의 화면 조각으로 축소하지 말고, 가능한 범위와 결정 blocker를 명시한다.
- 하나의 응집된 capability로 patch 범위를 제한한다.
- 증상이 아니라 원인을 수정한다.
- 관련 없는 refactor, 대규모 rename, formatting sweep, dependency upgrade, 문서 재작성을 하지 않는다.
- 테스트 통과를 위해 test, architecture rule, authorization, validation 또는 security control을 약화하지 않는다.
- 다른 worktree나 agent가 만든 변경을 덮어쓰지 않는다.
- 가장 가까운 적절한 수준에 테스트를 추가하거나 갱신한다.
- 구현 근거나 승인된 결정이 달라진 경우에만 문서를 갱신한다.
- API 작업은 계약을 먼저 확정하고 backend와 frontend를 그 계약에 맞춘다.
- database 작업은 migration을 먼저 소유·확정한 다음 persistence adapter를 수정한다.

인계 전:

1. targeted test를 먼저 실행하고 관련 범위의 broader suite를 실행한다.
2. 영향이 있으면 contract, migration, secret, architecture 검사를 실행한다.
3. diff에서 관련 없는 변경과 generated artifact를 확인한다.
4. PASS, FAIL, NOT RUN을 정확하게 보고하고 blocker를 명시한다.
5. roadmap milestone 완료를 임의로 선언하지 않는다. 완료에는 coordinator 또는 owner의 검토와 근거 승인이 필요하다.

## 11. 표준 명령

저장소 루트에서 실행한다.

```text
npm run backend:test
npm run backend:build
npm run frontend:install
npm run frontend:test
npm run frontend:e2e
npm run frontend:quality
npm run frontend:build
npm run docker:check
npm run security:check
npm run test
npm run verify
npm run verify:mvp
```

Docker Desktop 최신 버전과 Testcontainers 1.20.3 조합에서 Docker API 호환 설정이 필요한 경우 Windows PowerShell에서 다음과 같이 실행한다.

```powershell
$env:DOCKER_API_VERSION='1.44'
npm run backend:test
```

로컬 개발:

```text
cd backend
./gradlew bootRun

cd frontend
npm run dev
```

Windows PowerShell에서는 백엔드 직접 실행에 `.\gradlew.bat bootRun`을 사용한다. 백엔드는 Java 21과 `backend/.env.example`에 정의된 필수 환경변수가 필요하다. 현재 `bootRun` 작업은 `backend/.env`가 존재하면 읽어들이되 프로세스 환경변수를 우선한다.

PostgreSQL/Testcontainers 검증에는 실행 중인 Docker 호환 엔진이 필요하다. `DEVPATH_DB_URL` 조건부 통합 테스트는 별도의 접근 가능한 PostgreSQL 설정이 없으면 SKIP될 수 있으므로 결과를 구분해서 보고한다.

## 12. 다중 에이전트 및 Worktree 안전

- Orca session에서는 Orca가 관리하는 worktree와 task handoff를 사용하고 임의의 raw Git worktree를 만들지 않는다.
- 한 파일은 한 worker만 소유한다. 공유 contract, migration, lockfile, root configuration, ADR은 coordinator의 명시적 소유권이 필요하다.
- coordinator가 소유권 이전을 승인하지 않으면 다른 worker의 task 파일을 수정하지 않는다.
- contract 작업을 관련 backend/frontend 작업보다 먼저 수행한다. migration 소유권을 persistence adapter 작업보다 먼저 확정한다.
- worker는 변경 파일, 결정, 테스트, 생략한 검사, 위험, 권장 통합 순서를 보고한다.
- 사용자나 coordinator의 명시적 요청 없이 commit, push, merge 또는 history rewrite를 하지 않는다.
- 기존 dirty worktree를 사용자 소유 변경으로 간주하고 관련 없는 파일을 정리하거나 되돌리지 않는다.

Orca worker로 작업할 때는 다른 instruction 파일을 읽었다고 가정하지 말고 이 파일의 소유권, 상태, blocker 및 handoff 규칙을 따른다.

## 13. 필수 인계 요약

모든 구현 인계에는 다음 내용을 포함한다.

- 목표와 requirement ID
- 영향받은 module과 파일
- 구현된 동작
- API, data, security, observability, documentation 영향
- 실행한 test와 command 및 PASS/FAIL/NOT RUN 상태
- 가정과 deviation
- 남은 blocker와 다음 권장 작업

문서 작성이나 검토만 수행한 경우에도 실제 변경 파일, 검증 여부, 미확인 사항을 간결하게 보고한다.
