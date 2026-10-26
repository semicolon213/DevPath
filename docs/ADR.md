# DevPath 아키텍처 결정 기록 (ADR)

DevPath에서 채택했거나 보류한 기술·구조 결정을 모은 문서다. 구현 상태와 검증 결과는 [docs/STATUS.md](STATUS.md)에서만
관리한다. 요구사항은 [docs/SRS.md](SRS.md), 구조는 [docs/ARCHITECTURE.md](ARCHITECTURE.md), 보안 규칙은
[docs/SECURITY.md](SECURITY.md)를 본다.

## 1. 상태 정의

| 상태 | 의미 |
|---|---|
| Accepted | 채택된 결정. 구현은 이 결정을 따른다. |
| Proposed | 문서로만 존재하는 후보. 구현 근거로 쓰지 않는다. 이 결정에 기대는 영역은 막힌다. |
| Deferred | 의도적으로 미룬 결정. 결정 내용은 나중에 도입할 때의 기준으로 남긴다. |

규칙:

- Accepted 결정의 근거를 바꾸려면 새 ADR 또는 기존 ADR 개정이 필요하다.
- Proposed ADR은 해당 영역만 막는다. 관련 없는 작업은 계속할 수 있다.
- Proposed를 Accepted로 바꾸는 일은 소유자만 한다.

## 2. 결정 목록

| ID | 제목 | 상태 | 날짜 |
|---|---|---|---|
| ADR-001 | 초기 백엔드는 모듈형 모놀리스 | Accepted | 2026-07-26 |
| ADR-002 | 헥사고날 경계 적용 | Accepted | 2026-07-26 |
| ADR-003 | PostgreSQL을 주 정형 데이터 저장소로 사용 | Accepted | 2026-07-26 |
| ADR-004 | Redis는 비권위 캐시·조정 용도로만 사용 | Deferred | 2026-07-26 (Deferred 전환 2026-10-26) |
| ADR-005 | 계약 우선 REST API | Accepted | 2026-07-26 |
| ADR-006 | 결정론 엔진과 AI 생성 분리 | Accepted | 2026-07-26 |
| ADR-007 | 외부 서비스는 provider port와 adapter로 연결 | Accepted | 2026-07-26 |
| ADR-008 | 규칙·프로필·프롬프트·검증기 버전을 독립 관리 | Accepted | 2026-07-26 |
| ADR-009 | 저장소 snapshot과 과거 분석 결과는 불변 | Accepted | 2026-07-26 |
| ADR-010 | 오래 걸리는 작업은 비동기 job으로 실행 | Accepted | 2026-07-26 |
| ADR-011 | API·worker·scheduler 실행 책임 분리 | Accepted | 2026-07-26 |
| ADR-012 | 기능 중심 프론트엔드 구조 | Accepted | 2026-07-26 |
| ADR-013 | 프론트엔드 서버 상태와 클라이언트 상태 분리 | Accepted | 2026-07-26 |
| ADR-014 | 권한 필터를 적용한 지식 검색 | Accepted | 2026-07-26 |
| ADR-015 | AI 응답은 검증 전까지 신뢰하지 않음 | Accepted | 2026-07-26 |
| ADR-016 | 한 번 빌드하고 불변 산출물을 승격 | Deferred | 2026-07-26 (Deferred 전환 2026-10-26) |
| ADR-017 | 구조화 로그·메트릭·분산 추적을 별도 신호로 사용 | Deferred | 2026-07-26 (Deferred 전환 2026-10-26) |
| ADR-018 | 감사 기록과 운영 로그 분리 | Accepted | 2026-07-26 |
| ADR-019 | 계층형 테스트와 결정론 golden dataset | Accepted | 2026-07-26 |
| ADR-020 | 백엔드 언어와 프레임워크 | Accepted | 2026-07-26 |
| ADR-021 | 프론트엔드 프레임워크 | Accepted | 2026-07-26 |
| ADR-022 | 저장소 전략(monorepo) | Accepted | 2026-07-26 |
| ADR-023 | 의존성과 빌드 관리 | Accepted | 2026-07-26 |
| ADR-024 | 영속성과 ORM 방식 | Accepted | 2026-07-26 |
| ADR-025 | 데이터베이스 마이그레이션 도구 | Accepted | 2026-07-26 |
| ADR-026 | 인증과 세션 모델 | Accepted | 2026-07-26 |
| ADR-027 | 백그라운드 job 기술 | Accepted | 2026-08-11 |
| ADR-028 | 벡터 데이터베이스 | Accepted | 2026-08-29 |
| ADR-029 | 오브젝트 스토리지 | Accepted | 2026-08-29 |
| ADR-030 | AI provider SDK 전략 | Accepted | 2026-08-29 |
| ADR-031 | 관측 기술 | Proposed | — |
| ADR-032 | 테스트 도구 | Accepted | 2026-07-26 |
| ADR-033 | 배포 플랫폼 | Proposed | — |
| ADR-034 | 운영 secret 관리 | Proposed | — |
| ADR-035 | Git 작업 방식 | Accepted | 2026-07-26 |

Deferred 전환(ADR-004, 016, 017)은 2026-10-26 코드 확인 결과에 따른 것이다. 결정 내용 자체는 바꾸지 않았다.

## 3. 결정 상세

### ADR-001 초기 백엔드는 모듈형 모놀리스

- 상태: Accepted · 2026-07-26
- 맥락: 도메인 모듈은 많지만 개발 인원은 적다. 마이크로서비스의 운영 부담을 질 이유가 없다.
- 결정: 하나의 Spring Boot 애플리케이션 안에 업무 모듈(`com.devpath.<module>`)을 나눈 모듈형 모놀리스로 만든다.
- 결과: 배포와 트랜잭션이 단순하다. 경계가 무너지지 않도록 아키텍처 테스트와 리뷰가 필요하고, 모듈별 독립 확장은 할 수 없다.

### ADR-002 헥사고날 경계 적용

- 상태: Accepted · 2026-07-26
- 맥락: 도메인 규칙과 결정론 엔진이 프레임워크, provider, 저장소 세부 사항에 오염되면 안 된다.
- 결정: 모듈마다 `domain`, `application`, `adapter.in`, `adapter.out`, `config` 패키지를 둔다. port는 경계가 실제로 필요한 곳에만 만든다.
- 결과: 테스트와 provider 교체가 쉽다. 매핑 코드가 늘어난다. 도메인이 Spring·JPA·SDK에 의존하지 않는지는 `ArchitectureBoundaryTest`가 검사한다.

### ADR-003 PostgreSQL을 주 정형 데이터 저장소로 사용

- 상태: Accepted · 2026-07-26
- 맥락: 소유권, 감사, 불변 결과, 설정 버전, 트랜잭션 일관성이 필요하다.
- 결정: PostgreSQL을 정형 데이터의 유일한 원천(Source of Truth)으로 쓴다. 캐시, 벡터, projection, LLM context는 원천이 아니다.
- 결과: 트랜잭션과 관계 무결성이 강하다. 스키마는 마이그레이션으로 관리해야 하고 수평 확장은 자동이 아니다.

### ADR-004 Redis는 비권위 캐시·조정 용도로만 사용

- 상태: Deferred (2026-10-26, 이전 Accepted) · 원 결정 2026-07-26
- 맥락: Redis는 캐시, 잠금, rate limit에 쓸모가 있지만 원천 데이터로 쓰면 위험하다.
- 결정(도입 시 기준): Redis를 쓰게 되면 비권위 캐시·조정 용도로만, TTL을 두고, cache/coordination port 뒤에서만 쓴다.
- 결과: 현재 미도입이다. 백엔드 의존성에 Redis가 없고, 세션(ADR-026)과 job(ADR-027)은 PostgreSQL을 쓴다.

### ADR-005 계약 우선 REST API

- 상태: Accepted · 2026-07-26
- 맥락: 프론트엔드·백엔드 통합과 비동기 job API에 안정된 계약이 필요하다.
- 결정: v1은 REST로 하고 계약을 먼저 정한다. 기계 판독 계약은 `contracts/openapi/devpath-openapi.yaml`이다.
- 결과: 계약 검사는 `npm run contract:lint`(Redocly)로 한다. 생성된 클라이언트는 쓰지 않고, 프론트엔드 API 클라이언트는 직접 작성한다.

### ADR-006 결정론 엔진과 AI 생성 분리

- 상태: Accepted · 2026-07-26
- 맥락: 제품 원칙은 "Rule Engine이 계산하고 AI는 설명한다"이다.
- 결정: Rule Engine과 Career Path Engine만 공식 점수·가중치·준비도·추천 우선순위를 계산한다. LLM은 설명과 초안 생성만 한다.
- 결과: 점수가 재현 가능하고 테스트할 수 있다. AI로 점수 공백을 메울 수 없다. 이 결정은 SRS를 바꾸지 않는 한 되돌리지 않는다.

### ADR-007 외부 서비스는 provider port와 adapter로 연결

- 상태: Accepted · 2026-07-26
- 맥락: GitHub, Notion, AI provider, 오브젝트 스토리지는 바뀔 수 있다.
- 결정: 외부 서비스는 기능별 port와 adapter로 연결한다. provider 모델과 오류는 adapter 밖으로 나가지 않는다.
- 결과: 오류 정규화와 테스트가 쉽다. adapter 매핑 비용이 든다.

### ADR-008 규칙·프로필·프롬프트·검증기 버전을 독립 관리

- 상태: Accepted · 2026-07-26
- 맥락: 규칙, 직무·회사 프로필, 프롬프트, 검증기는 서로 다른 속도로 바뀐다.
- 결정: 각각 독립 버전을 둔다(예: `baseline-v2`, `skill-matrix-v3`, `readiness-v1`, `recommendation-v1`, `roadmap-v1`, `skill-explanation-validator-v5`).
- 결과: 과거 결과가 어떤 버전으로 계산·생성됐는지 알 수 있다. 버전 메타데이터 관리가 늘어난다. 정책 버전 목록은 [docs/SCORING_RULES.md](SCORING_RULES.md)에 있다.

### ADR-009 저장소 snapshot과 과거 분석 결과는 불변

- 상태: Accepted · 2026-07-26
- 맥락: 성장 이력과 과거 준비도는 재현 가능해야 한다.
- 결정: `RepositorySnapshot`과 완료된 `AnalysisResult`는 수정하지 않는다. 정정은 새 결과로 만든다.
- 결과: 이력을 신뢰할 수 있다. 저장 공간이 늘어난다.

### ADR-010 오래 걸리는 작업은 비동기 job으로 실행

- 상태: Accepted · 2026-07-26
- 맥락: 저장소 동기화, 분석, 지식 수집, embedding, AI 생성은 오래 걸린다.
- 결정: 영속 비동기 job으로 실행하고 API는 job 상태를 돌려준다.
- 결과: 재시도, 진행 표시, 새로고침 후 이어보기가 가능하다. job 상태 모델과 중복 실행 방지가 필요하다. 기술 선택은 ADR-027.

### ADR-011 API·worker·scheduler 실행 책임 분리

- 상태: Accepted · 2026-07-26
- 맥락: 요청 처리, 백그라운드 job, 예약 작업은 생명주기가 다르다.
- 결정: 실행 책임을 나누되, 초기에는 같은 산출물 하나에 실행 모드를 두는 방식을 허용한다.
- 결과: worker는 `devpath.runtime.worker-enabled` 설정으로 켜고 끄며 종료 대기 시간(`devpath.jobs.shutdown-timeout`)이 있다. 현재는 기본값(켜짐)으로 한 프로세스에서 함께 실행한다. 별도 프로세스 배포는 배포 환경이 없어 검증되지 않았다.

### ADR-012 기능 중심 프론트엔드 구조

- 상태: Accepted · 2026-07-26
- 맥락: 대시보드, 분석, 커리어, 지식, AI, 설정 등 기능이 많다.
- 결정: `frontend/src/features/<기능>` 단위로 나누고 공용 요소는 `shared`, 앱 틀은 `app`에 둔다.
- 결과: 기능별 소유가 분명하다. 기능 사이 내부 import를 막는 규율이 필요하다.

### ADR-013 프론트엔드 서버 상태와 클라이언트 상태 분리

- 상태: Accepted · 2026-07-26
- 맥락: API 데이터, job polling, 폼, 화면 상태가 섞이면 캐시 무효화가 어렵다.
- 결정: 서버 상태는 React Query(`@tanstack/react-query`)가 맡고 화면 상태는 컴포넌트·라우트가 맡는다.
- 결과: 캐시와 polling이 일관된다. 화면은 공식 점수를 다시 계산하지 않는다.

### ADR-014 권한 필터를 적용한 지식 검색

- 상태: Accepted · 2026-07-26
- 맥락: 지식 검색 대상에 비공개 저장소와 Notion 내용이 포함될 수 있다.
- 결정: 검색은 결과를 돌려주기 전에 소유자 필터를 적용한다. 관련도가 권한을 넘어서지 못한다. 권한 메타데이터가 없으면 검색을 거부한다.
- 결과: 사용자 간 격리가 강하다. 메타데이터와 필터 비용이 든다. 다른 사용자 데이터 검색 차단은 실제 pgvector 테스트로 확인했다.

### ADR-015 AI 응답은 검증 전까지 신뢰하지 않음

- 상태: Accepted · 2026-07-26
- 맥락: LLM 출력은 형식 오류, 환각, prompt injection, 위험한 내용을 포함할 수 있다.
- 결정: 형식, 근거 연결, 금지 주장, 점수 일치, 안전성 검증을 통과한 응답만 산출물로 저장한다.
- 결과: 점수 변조와 시스템 프롬프트 노출을 막는다. 쓸모 있는 응답도 거부될 수 있다. 검증기는 버전을 가진다(ADR-008).

### ADR-016 한 번 빌드하고 불변 산출물을 승격

- 상태: Deferred (2026-10-26, 이전 Accepted) · 원 결정 2026-07-26
- 맥락: 여러 환경에 같은 산출물을 배포해야 재현성과 롤백이 보장된다.
- 결정(도입 시 기준): 산출물을 한 번 빌드해 환경 간에 승격하고 설정과 secret은 산출물 밖에 둔다.
- 결과: 현재 미실현이다. CI 파이프라인, 컨테이너 이미지, 산출물 저장소, 스테이징·운영 환경이 없다. ADR-033 결정 후 다시 본다.

### ADR-017 구조화 로그·메트릭·분산 추적을 별도 신호로 사용

- 상태: Deferred (2026-10-26, 이전 Accepted) · 원 결정 2026-07-26
- 맥락: 동기 요청, 비동기 job, AI 호출, 외부 provider를 함께 진단해야 한다.
- 결정(도입 시 기준): 로그, 메트릭, 추적을 별도 신호로 두고 민감 정보를 가린다.
- 결과: 구현된 것은 `request_id`·`correlation_id`를 포함한 로그 패턴과 요청 완료 로그뿐이다. 메트릭·추적 의존성(Actuator, Micrometer, OpenTelemetry)은 없다. 기술 선택(ADR-031)과 함께 보류한다.

### ADR-018 감사 기록과 운영 로그 분리

- 상태: Accepted · 2026-07-26
- 맥락: 로그인, 연결 변경 같은 민감 행위는 권위 있는 이력이 필요하다.
- 결정: 감사 기록은 운영 로그와 별도로 PostgreSQL에 추가 전용(append-only)으로 저장한다.
- 결과: `audit` 모듈과 Flyway V6 스키마로 구현했다. 로그 보존 정책이 감사 기록을 지우지 않는다. 관리자 기능 감사는 관리자 기능이 없어 해당 없음.

### ADR-019 계층형 테스트와 결정론 golden dataset

- 상태: Accepted · 2026-07-26
- 맥락: 결정론 엔진, API 계약, 연동, 화면, AI 출력이 섞여 있다.
- 결정: 단위·통합·계약·브라우저 테스트를 계층으로 두고 Rule/Career 엔진은 버전 있는 golden dataset으로 검증한다.
- 결과: 엔진 테스트는 LLM 없이 돈다. golden fixture는 `fixtures/rule-engine/`(`baseline-v1.json`, `baseline-v2.json`)에 있다. 상세는 [docs/TEST_PLAN.md](TEST_PLAN.md).

### ADR-020 백엔드 언어와 프레임워크

- 상태: Accepted · 2026-07-26
- 맥락: 스캐폴딩 전에 백엔드 스택을 정해야 했다.
- 결정: Java 21과 Spring Boot 3.x(현재 3.3.5, 빌드 파일에 고정)를 쓴다. 도메인과 결정론 엔진은 Spring annotation에 의존하지 않는다.
- 결과: Spring Security, 트랜잭션, 스케줄링을 바로 쓴다. Kotlin보다 장황하고 프론트엔드와 언어가 다르다.

### ADR-021 프론트엔드 프레임워크

- 상태: Accepted · 2026-07-26
- 맥락: 인증이 필요한 단일 페이지 앱이 필요하다.
- 결정: React 18과 TypeScript로 클라이언트 렌더링 SPA를 만든다. 라우팅은 React Router, 빌드는 Vite. SSR은 범위 밖이다.
- 결과: 스타일은 TailwindCSS가 아니라 기능별 일반 CSS 파일(`frontend/src/app/styles/`)과 디자인 토큰으로 구현했다. 화면 규칙은 [DESIGN.md](../DESIGN.md).

### ADR-022 저장소 전략(monorepo)

- 상태: Accepted · 2026-07-26
- 맥락: 백엔드, 프론트엔드, 계약, 문서를 함께 바꾸는 일이 많다.
- 결정: 저장소 하나에 `backend/`, `frontend/`, `contracts/`, `docs/`, `fixtures/`, `scripts/`를 둔다.
- 결과: 교차 변경이 한 커밋으로 끝난다. 프론트엔드와 백엔드는 서로의 소스를 import하지 않고 API 계약으로만 통신한다.

### ADR-023 의존성과 빌드 관리

- 상태: Accepted · 2026-07-26
- 맥락: 빌드가 재현 가능해야 하고 도구는 단순해야 한다.
- 결정: 백엔드는 Gradle Wrapper, 프론트엔드는 npm과 lock 파일을 쓴다. 무거운 monorepo 도구는 쓰지 않고 루트 `package.json` 스크립트가 위임만 한다.
- 결과: 의존성 버전은 빌드 파일과 lock 파일에 고정된다. 의존성 갱신은 테스트 근거가 있는 별도 변경으로 한다.

### ADR-024 영속성과 ORM 방식

- 상태: Accepted · 2026-07-26
- 맥락: 도메인 타입을 ORM에 묶지 않으면서 불변 이력, 소유권, 트랜잭션 규칙을 지켜야 한다.
- 결정: Spring Data JPA(Hibernate)를 persistence adapter 안에서만 쓰고, JPA 엔티티와 도메인 객체를 분리해 명시적으로 매핑한다. 트랜잭션은 application service가 소유한다.
- 결과: Open Session in View는 끄고(`open-in-view: false`) 스키마 자동 생성은 금지한다(`ddl-auto: validate`). 매핑 코드가 중복되고 N+1 위험을 관리해야 한다. 외부 호출은 긴 DB 트랜잭션 밖에서 한다.

### ADR-025 데이터베이스 마이그레이션 도구

- 상태: Accepted · 2026-07-26
- 맥락: 스키마 변경은 검토 가능하고 checksum으로 보호되어야 한다.
- 결정: Flyway로 불변 SQL 마이그레이션(`V<번호>__<설명>.sql`)을 관리한다. 적용된 마이그레이션은 수정하지 않고 새 파일로 고친다.
- 결과: 현재 V1–V30이 있다. 로컬과 테스트는 기동 시 Flyway를 실행한다. 운영 실행 방식은 배포 환경이 없어 정하지 않았다.

### ADR-026 인증과 세션 모델

- 상태: Accepted · 2026-07-26
- 맥락: GitHub 로그인, 공급자 독립 사용자 ID, 로그아웃·폐기, 브라우저 토큰 노출 방지가 필요하다.
- 결정: Spring Security OAuth2 Login(GitHub)과 서버가 관리하는 opaque 세션을 HttpOnly 쿠키로 전달한다. 세션은 Spring Session JDBC로 PostgreSQL에 저장한다. 상태 변경 요청은 CSRF 토큰을 요구하고 CORS는 지정된 프론트엔드 origin만 허용한다.
- 결과: SPA는 세션·provider 토큰을 보관하지 않는다. 유휴 30분, 절대 12시간 만료(설정 가능). provider 토큰은 AES-GCM으로 암호화해 서버에만 둔다. Redis 없이 동작한다. 상세는 [docs/SECURITY.md](SECURITY.md).

### ADR-027 백그라운드 job 기술

- 상태: Accepted · 2026-08-11
- 맥락: job은 API 재시작에도 살아남아야 하고 Redis나 브로커를 새로 들이고 싶지 않다.
- 결정: PostgreSQL 기반 영속 job과 transactional outbox를 쓴다. 같은 Spring Boot 산출물에서 worker 설정으로 실행한다.
- 결과: 저장소 동기화, 분석, 지식 수집, AI 생성 job이 이 방식으로 돈다. 중복 실행은 소유자별 idempotency key, 활성 job 유일성, 비관적 claim, lease 회수로 막는다. DB polling 부하가 생긴다.

### ADR-028 벡터 데이터베이스

- 상태: Accepted · 2026-08-29
- 맥락: 벡터 유사도 검색이 필요하지만 두 번째 원천 저장소를 만들거나 권한 필터를 약하게 하면 안 된다.
- 결정: PostgreSQL pgvector를 쓴다. 문서·chunk·소유권·embedding 메타데이터는 일반 관계형 테이블에 두고 벡터 인덱스는 재생성 가능한 파생 데이터로 본다. 검색은 port 뒤에 둔다.
- 결과: 로컬·테스트·운영이 같은 저장소를 쓴다. 모든 검색은 owner, 문서, 버전, embedding model 필터를 강제한다. 벡터 부하가 PostgreSQL 용량을 나눠 쓴다.

### ADR-029 오브젝트 스토리지

- 상태: Accepted · 2026-08-29
- 맥락: 수집 원문과 생성 산출물 같은 비정형 내용을 저장해야 한다.
- 결정: S3 호환 오브젝트 스토리지 port를 둔다. 소유권과 참조 메타데이터는 PostgreSQL에 두고 내용은 기본 비공개다. 로컬·테스트는 파일시스템 adapter를 쓴다.
- 결과: 현재 구현은 `ObjectContentPort`와 `FilesystemObjectContentAdapter`뿐이다. 운영용 S3 호환 adapter와 공급자는 ADR-033 결정 전까지 없다.

### ADR-030 AI provider SDK 전략

- 상태: Accepted · 2026-08-29
- 맥락: AI provider마다 기능과 오류가 다르다.
- 결정: 기능별 port(embedding과 generation 분리)와 provider별 adapter를 둔다. SDK 타입과 오류는 port 밖으로 나가지 않는다. 범용 다중 provider 라이브러리는 쓰지 않는다.
- 결과: 현재 provider는 Ollama 하나다(`EmbeddingPort` 기본 `nomic-embed-text`, `GenerationProviderPort` 기본 `qwen2.5:7b`, HTTP 호출). 모델은 설정으로 바꾼다.

### ADR-031 관측 기술

- 상태: Proposed
- 맥락: 로그·메트릭·추적 저장소와 화면 도구를 골라야 한다.
- 후보: 개방형 표준 기반 스택, 관리형 플랫폼, 혼합, 로그만 사용. 배포 플랫폼(ADR-033)이 정해진 뒤 고르는 것이 권고안이다.
- 영향: 결정 전까지 운영 관측 작업은 막힌다. 로그 상관 ID 같은 계측은 진행할 수 있다.
- 포트폴리오 범위에서는 보류.

### ADR-032 테스트 도구

- 상태: Accepted · 2026-07-26
- 맥락: 모든 핵심 경계에 실행 가능한 검증이 필요하다.
- 결정: 백엔드는 JUnit 5, Spring Boot Test, ArchUnit, Testcontainers PostgreSQL. 프론트엔드는 Vitest와 React Testing Library. 브라우저는 Playwright(Chromium)와 axe.
- 결과: 보안 경계 검사(`npm run security:check`)와 `npm audit`을 함께 돌린다. PostgreSQL 테스트를 H2로 대신하지 않으며 Docker가 필요하다. 상세는 [docs/TEST_PLAN.md](TEST_PLAN.md).

### ADR-033 배포 플랫폼

- 상태: Proposed
- 맥락: API, worker, PostgreSQL(pgvector), 오브젝트 스토리지, secret을 운영할 환경이 필요하다.
- 후보: 관리형 앱 플랫폼, 컨테이너 관리형 서비스, VM, Kubernetes, serverless. 가장 단순한 플랫폼을 고르는 것이 권고안이다.
- 영향: 결정 전까지 스테이징·운영 배포, 운영 오브젝트 스토리지, ADR-016 실현이 막힌다. 로컬 개발은 계속할 수 있다.
- 포트폴리오 범위에서는 보류.

### ADR-034 운영 secret 관리

- 상태: Proposed
- 맥락: 운영 secret의 저장과 주입 방식을 정해야 한다.
- 후보: 배포 플랫폼 secret 기능, 전용 secret manager. 환경 파일은 로컬 개발에만 쓴다.
- 영향: 결정 전까지 운영 환경 구성이 막힌다. 코드에는 운영 secret을 넣지 않는다.
- 포트폴리오 범위에서는 보류.

### ADR-035 Git 작업 방식

- 상태: Accepted · 2026-07-26
- 맥락: 작은 팀과 AI 보조 작업에 맞는 단순한 흐름이 필요하다.
- 결정: 단순화한 GitHub Flow. `main`이 통합 브랜치이고 짧은 작업 브랜치를 쓴다. 변경은 PR로 병합하고(squash 선호) `main` 직접 push는 하지 않으며, 사람 리뷰 또는 self-review 체크리스트를 거친다. 공유 브랜치 이력 재작성, 보호 브랜치 force-push, secret 커밋을 금지한다.
- 결과: 커밋 메시지는 Conventional Commit 형식(`feat:`, `fix:`, `docs:` 등)을 쓴다.
- 현재 실무와의 차이: 1인 개발이라 실제로는 작업 브랜치(`<사용자>/<작업>`)의 변경을 PR 없이 `main`에 선형으로 반영하고, 독립 리뷰 세션 3개가 PR 리뷰를 대신한다. 결정을 이 실무에 맞게 바꿀지는 소유자 결정이 필요하다.

## 4. 계획(미구현)

| 항목 | 관련 ADR | 상태 |
|---|---|---|
| Redis 캐시·조정 도입 | ADR-004 | Deferred |
| CI 파이프라인과 불변 산출물 승격 | ADR-016 | Deferred |
| 메트릭·분산 추적 | ADR-017, ADR-031 | Deferred / Proposed |
| 운영용 S3 호환 오브젝트 스토리지 adapter | ADR-029, ADR-033 | 미구현 |
| worker·scheduler 별도 프로세스 배포 | ADR-011, ADR-033 | 미구현 |
| 스테이징·운영 배포 환경 | ADR-033 | Proposed |
| 운영 secret 관리 | ADR-034 | Proposed |
