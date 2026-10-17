<!--
한국어판 문서입니다. 원본 기준 문서: docs/21_Feature_Status.md
요구사항 ID, 엔진명, 기술명, 스키마명은 추적성을 위해 필요한 경우 원문 표기를 유지합니다.
-->

# DevPath 기능 현황

## 1. 목적과 문서 권한

이 문서는 DevPath의 기능 중 무엇이 구현됐고, 무엇이 승인을 기다리며, 무엇이 아직 계획 단계인지를 요약한다. 현황을
파악하기 위한 보고서이며 요구사항의 출처가 아니다. 범위와 마일스톤 완료 여부는 `docs/01_SRS.md`, 엔진 명세,
`docs/19_Roadmap.md`가 계속 기준이 되고, MVP 검증 근거의 색인은 `docs/20_MVP_Release_Evidence.md`가 맡는다. 이 문서는
어떤 마일스톤의 완료도 선언하지 않으며 owner 승인을 대신하지 않는다.

기준 시점: `main`의 커밋 `661302a`, 2026-10-15.

상태 용어:

| 상태 | 의미 |
|---|---|
| 구현됨 | 코드, 계약, 자동화 테스트가 있고 해당 기능 자체에 열린 승인 게이트가 기록되어 있지 않다. |
| 구현됨, 승인 대기 | 코드와 자동화 근거는 있지만, roadmap에 owner 승인, live provider 검증, production 결정이 아직 열려 있다고 기록되어 있다. |
| 미구현 | 명세는 있으나 코드와 계약에 없다. |

## 2. 검증 결과 (2026-10-15)

커밋 `661302a`와 동일한 작업 트리에서 `JAVA_HOME`을 JDK 21로, `DOCKER_API_VERSION=1.44`,
`VITE_DEVPATH_API_BASE_URL=http://localhost:8080`으로 지정해 `npm run verify:mvp`를 로컬 실행했다(Docker 29.6.2).

| 검사 | 결과 |
|---|---|
| 보안 경계 테스트와 저장소 스캔 | PASS (검사 3건) |
| 백엔드 clean build, JUnit·아키텍처·Testcontainers 테스트 | PASS. 226건 중 224건 통과, 실패 0건, 환경변수 조건부 SKIP 2건 |
| 프론트엔드 clean install | PASS |
| 프론트엔드 Vitest | PASS. 44개 파일, 114건 |
| Playwright Chromium + axe | PASS. 브라우저 사용자 여정 7건 |
| 프론트엔드 production build | PASS |
| 프론트엔드 `npm audit` | FAIL. 간접 의존성 `source-map-js`에 고위험 보안 권고 1건(GHSA-68fv-2mgg-jv7q). `npm audit fix`로 수정 가능 |
| OpenAPI lint (`npm run contract:lint`, 별도 실행) | PASS. 오류 0건, 권고 경고 9건 |

`npm audit` 실패는 애플리케이션 변경이 아니라 의존성 보안 권고 때문이며, 해결하려면 lockfile을 갱신해야 한다. 같은
실행에서 확인된 로컬 환경 참고 사항: 셸의 `JAVA_HOME`이 JDK 11을 가리키면 Gradle 대체 실행이 실패한다. 또 실행 파일
탐색에서 현재 디렉터리를 제외하는 환경에서는 `scripts/run-gradle.mjs`가 작업 디렉터리의 `gradlew.bat`을 찾지 못하고,
이 경우 캐시된 Gradle 배포판으로 대체 실행한다.

## 3. 기능 현황

| 영역 | 기능 | 상태 | Roadmap | 프론트엔드 경로 |
|---|---|---|---|---|
| Identity | GitHub OAuth2 로그인, opaque server session, CSRF, 로그아웃, 현재 사용자 조회, 공급자 독립 `User` | 구현됨 | M16 | `/`, `/settings` |
| 프로필 | 사용자 프로필과 Career/Company preference | 구현됨 | M17 | `/settings/profile`, `/onboarding` |
| GitHub | 연결, 저장소 검색과 가져오기 | 구현됨 | M18 | `/settings/integrations`, `/repositories` |
| 저장소 | 등록, 보관·복원, durable 동기화, immutable snapshot, 근거와 활동 추출 | 구현됨 | M19–M22 | `/repositories`, `/repositories/:repositoryId`, `/repositories/:repositoryId/snapshots/:snapshotId` |
| 분석 | 결정론 Rule Engine 분석, 이력, 점수 상세, 근거 연결, 비교 | 구현됨 | M23–M25, M32 | `/analyses`, `/analyses/:analysisId`, `/analyses/compare` |
| 기술 | Skill Matrix 생성·조회·비교, 기술별 불변 이력 | 구현됨 | M26 | `/skills`, `/skills/:skillId`, `/skills/compare` |
| 직무 | Backend/Frontend 직무 catalog, `readiness-v1` Career Readiness와 Skill Gap | 구현됨 | M27–M28, M30 | `/careers`, `/careers/:careerId`, `/career-readiness`, `/career-readiness/:careerReadinessId` |
| 회사 | 회사 catalog | 구현됨 | M29 | `/companies`, `/companies/:id` |
| 회사 | 회사별 readiness와 추천 modifier | 미구현 (MVP 이후) | M29–M30 | — |
| 추천 | `recommendation-v1` 결정론 추천과 `roadmap-v1` 학습 로드맵 | 구현됨 | M31 | `/recommendations`, `/recommendations/:recommendationId`, `/roadmap` |
| 대시보드 | 사용자 대시보드 요약 | 구현됨 | M33 | `/dashboard` |
| MVP 게이트 | MVP 완료 근거 종합 | 구현됨, 승인 대기 | M34 | — |
| 지식 | Notion OAuth 연결과 공유 페이지 메타데이터 탐색 | 구현됨, 승인 대기 | M35 | `/settings/integrations` |
| 지식 | Notion 수집: 정규화, chunk 분할, Ollama embedding, pgvector, 보관과 재색인 | 구현됨, 승인 대기 | M36 | `/knowledge`, `/knowledge/:documentId` |
| 지식 | owner 필터를 강제하는 semantic retrieval과 출처 표시 | 구현됨, 승인 대기 | M37 | `/knowledge` |
| AI | Ollama generation adapter 기반 provider port, 취소, 재시도 상한, 검증된 산출물 | 구현됨, 승인 대기 | M38–M42 | — |
| AI | 기술 분석 설명 | 구현됨, 승인 대기 | M42 | `/skills` |
| AI | 저장소 리뷰와 저장소 요약 | 구현됨, 승인 대기 | M43 | `/analyses/:analysisId` |
| AI | 커리어 코칭과 학습 추천 | 구현됨, 승인 대기 | M43 | `/career-readiness`, `/roadmap` |
| AI | 생성 산출물 목록, 수동 재시도, 편집과 내보내기 | 미구현 | M43–M44 | — |
| 산출물 | Portfolio, Resume, Interview 준비 | 미구현 | M45–M47 | — |
| 운영 | Administration workflow | 미구현 | — | — |
| 운영 | production observability, 배포 플랫폼, secret 관리 | 미구현. Proposed ADR-031, ADR-033, ADR-034에 막혀 있음 | M52–M53 | — |

## 4. API 계약 범위

`contracts/openapi/devpath-openapi.yaml`은 75개 operation을 정의하며, `docs/10_API_Specification.md`의 정식 API ID
160개 중 70개를 포함한다.

| 그룹 | 계약 포함 | 명세 | 주요 미구현 |
|---|---:|---:|---|
| Identity (`API-ID`) | 8 | 10 | API-ID-008, API-ID-009 |
| Integrations (`API-INT`) | 10 | 12 | API-INT-006, API-INT-012 |
| Repositories (`API-REP`) | 10 | 12 | API-REP-003, API-REP-010 |
| Analysis (`API-ANA`) | 8 | 8 | — |
| Skills (`API-SKL`) | 5 | 7 | 기술·프레임워크 숙련도 |
| Careers (`API-CAR`) | 5 | 6 | API-CAR-003 |
| Companies (`API-CMP`) | 2 | 5 | 회사 preference, 회사 readiness와 비교 |
| Recommendations (`API-REC`) | 4 | 8 | 추천 요청과 관련 operation |
| Learning (`API-LRN`) | 4 | 10 | 로드맵 생성과 진행 관리 operation |
| Dashboard (`API-DSH`) | 1 | 10 | 대시보드 영역별 endpoint |
| Knowledge (`API-KNW`) | 8 | 11 | 수동 문서 메타데이터 operation |
| AI (`API-AI`) | 5 | 7 | API-AI-004 재시도, API-AI-006 산출물 목록 |
| Prompt, Portfolio, Resume, Interview, Admin, Open (`API-PRM`, `API-PRT`, `API-RSM`, `API-ITV`, `API-ADM`, `API-OPEN`) | 0 | 54 | 미착수 |

## 5. 열린 게이트

- MVP 게이트(M34)와 구현된 지식(M35–M37)·AI(M38–M43) 기능의 owner 승인이 아직 열려 있다. `docs/19_Roadmap.md`에
  기록된 live provider 검증도 아직 필요하다.
- roadmap §76의 production object storage(RM-OI-007), 배포와 secret(RM-OI-008), 시연 환경(RM-OI-010) 항목이 열려 있다.
- Proposed ADR-031, ADR-033, ADR-034가 production observability, 배포, secret 작업을 막고 있다.
- §2의 프론트엔드 의존성 보안 권고를 lockfile 갱신으로 해결해야 `npm run verify:mvp`가 다시 통과한다.

## 6. 품질 개선 목록 (2026-10-15 진단)

두 개의 독립 읽기 전용 진단이 커밋 `9331fa0`을 검토했다. 이 커밋의 애플리케이션 코드는 `661302a`와 같다. 여기의 항목은
승인된 작업이 아니라 후보이며, 각 묶음마다 owner 승인, 구현, 독립 QA가 필요하다.

### 6.1 UI/UX

UI/UX 진단은 27개 경로 전체를 `DESIGN.md`와 대조했고, 25개 경로를 1280px와 375px 폭에서 캡처하며 axe 검사를 함께
실행했다. 발견 사항은 26건이다(High 6건, Medium 15건, Low 5건). 모든 경로에 h1이 하나씩 있고, 375px에서 가로로 넘치는
경로가 없으며, axe 위반은 비교 화면 두 곳에서 한 종류만 나왔다.

| 묶음 | 범위 | 주요 발견 |
|---|---|---|
| UX-1 | 앱 공통 레이아웃, 전역 메뉴, 끊긴 링크 | 전역 메뉴가 없고, 저장소 추가·목표 직무 링크가 막힌 화면으로 이어짐 |
| UX-2 | AI 생성 내용 분리 | AI 패널이 공식 결과와 같은 스타일을 쓰고, AI 표시가 일관되지 않으며, 공식 결과보다 앞에 놓임 |
| UX-3 | 공용 표시 문구 | **해결 (2026-10-16):** 공용 `shared/ui/labels.ts`가 enum마다 한국어 표시 하나와 중립적인 "알 수 없음" 대체 문구를 제공하고, 화면마다 중복되던 변환 함수를 대체했다. 저장소 상태와 동기화 상태를 서로 다른 말로 구분하고, 동기화 상태는 텍스트 배지로 표시한다. 내부 용어(M36, MVP, embedding provider, HTTP 상태 코드 그대로 표시, 영어 eyebrow)를 없앴고, 작업 오류 코드는 안전한 한국어 메시지로 바꾸며, 빈 상태 문구는 특정 직무를 지칭하지 않는다(UI-03, UI-07, UI-08, UI-21, UI-22, UI-26). 근거: 독립 QA PASS, 프론트엔드 45개 파일 / 테스트 129개 PASS, 빌드 PASS. 후속 과제 해결 (2026-10-16): 로컬 표시 목록에 없는 기술은 API 기술 이름으로 대체 표시하고, 분석 비교·커리어 준비도·추천 화면의 헤더 문장은 한국어로 쓰며 엔진 이름은 괄호 안에만 두고, 저장소 카드는 동기화 상태를 텍스트 배지로 한 번만 표시하며, 최종 실패한 지식 수집 작업은 더 이상 자동 재시도를 안내하지 않는다(독립 QA PASS, 프론트엔드 테스트 131개 PASS). 두 AI 패널(`RepositoryReviewPanel`, `SkillExplanationPanel`)의 "Rule Engine 결과" 문구도 2026-10-16에 한국어 우선 표현인 "공식 분석 결과(Rule Engine)"로 바꿨다(독립 QA PASS) |
| UX-4 | 확인창과 폼 | **해결 (2026-10-18):** 접근성을 갖춘 공용 `ConfirmDialog`(포커스가 확인창 안으로 이동해 처리 중에도 갇혀 있고, Escape는 처리 중이 아닐 때 취소하며, 닫히면 포커스가 실행한 버튼이나 대체 대상으로 돌아가고, 실패는 확인창 안에 표시되어 다시 시도할 수 있다)가 로드맵 보관(이전에는 확인 없음), 지식 문서 보관과 GitHub/Notion 연결 해제(이전에는 `window.confirm`), 카드와 상세 화면의 저장소 보관을 확인한다. 저장소 상세 화면에서는 보관을 분리된 위험 영역으로 옮겼다. 목표 직무/기업 변경은 명시적으로 확인하는 동작이며 그 영향만 안내한다(소유자 결정 D-7). 프로필 폼은 항목별 오류, 포커스를 받고 각 항목으로 연결되는 오류 요약, 저장하지 않은 변경에 대한 `beforeunload` 경고를 제공한다. 성공 결과는 항상 마운트된 라이브 영역으로 알리며, 설정과 온보딩 화면의 제목 수준은 더 이상 건너뛰지 않는다(UI-06, UI-13, UI-14, UI-15). 근거: 독립 검토 PASS(3회차, 3개 관점), 프론트엔드 48개 파일 / 테스트 149개 PASS, 빌드 PASS, Playwright 테스트 7개 PASS. 미해결(Low, 보류): 프로필 변경을 저장하지 않은 상태의 앱 내 이동은 막지 않으며(`beforeunload`만 적용, data router 전환은 소유자 결정으로 UX-1로 보류), 저장소 보관 문구는 새 분석을 시작할 수 없다고 안내하지만 백엔드는 보관된 저장소의 분석을 거부하지 않고(기존 UI 전용 제한), 배경을 inert로 만들지 않으며(`aria-modal`과 포커스 트랩에 의존), 사용하지 않는 `.lifecycle-confirmation` CSS가 남아 있다 |
| UX-5 | 디자인 토큰과 접근성 | **해결 (2026-10-17):** `styles.css`가 `:root`에 DESIGN §5.1 역할 토큰(배경, 텍스트, 테두리, 브랜드, 성공, 경고, 위험, 정보, 근거, AI)과 모서리 반경 토큰을 정의하고, 테스트가 hex 색상 값이 이 토큰 블록 밖에 쓰이지 않도록 검사한다. 초록색은 성공 상태에만 쓰고 다른 배지는 Info를 쓴다. 포커스 링은 간격을 둔 2px 브랜드 외곽선과 흰색 분리 링으로, 밝은 배경·어두운 배경·브랜드 색으로 채운 컨트롤에서 모두 보인다. 분석 비교 표와 기술 역량 비교 표는 서로 다른 이름이 붙은 포커스 가능한 스크롤 영역 안에 있으며, 720px 이하에서는 한국어 스크롤 안내를 표시한다(소유자 결정 D-8). 대시보드, 저장소, 기술 상세 화면의 카드 링크와 보조 버튼은 44x44px 크기를 확보한다(UI-16, UI-17, UI-18, UI-24). 근거: 독립 검토 PASS(2회차, 3개 관점), 프론트엔드 46개 파일 / 테스트 132개 PASS, 빌드 PASS, 비교 화면 375px axe 검사를 포함한 Playwright 테스트 7개 PASS. 미해결(Low): 토큰 밖에 `rgba()` 값 두 개가 남아 있고(검사는 hex만 확인), 흰색 포커스 분리 링은 이후 컴포넌트 규칙이 `box-shadow`를 덮어쓰지 않는다는 전제에 의존하며, 화면 낭독기와 200% 확대 수동 점검은 수행하지 않았다 |
| UX-6 | 공용 화면 상태 | 로딩·오류 상태가 일관되지 않고, 대시보드에 데이터 기준 시각이 없음 |

UX-1과 UX-2를 시작하기 전에 메뉴 항목, AI 패널 위치와 표시 문구, 기술별 근거 부족 기준을 제품 차원에서 결정해야 한다.

다음 세 발견 사항은 독립 QA PASS 후 커밋 `6e078ac`에서 해결했다. 그 밖의 UX-1, UX-2 항목은 아직 열려 있다.

- UI-02 (UX-1): 저장소 추가 링크가 `/settings/integrations#github`로 이동한다.
- UI-12 (UX-1): 커리어 화면의 목표 직무 설정 버튼이 `/settings/profile#targets`로 이동한다.
- UI-05 (UX-2): 로드맵의 AI 학습 가이드는 공식 로드맵 단계 뒤의 별도 표시 영역에 놓인다.

### 6.2 백엔드와 프론트엔드 코드

코드 진단에서 헥사고날 경계, provider 호출 주변의 transaction 범위, 오류 응답, 로깅은 문제가 없었다. 발견 사항은
19건이며, 대부분 실제 데이터 규모에서 읽기·쓰기 쿼리가 과도하게 늘어나는 문제다.

| 묶음 | 범위 | 주요 발견 |
|---|---|---|
| C-1 | snapshot 읽기 경로 | **해결 (2026-10-17):** snapshot 목록은 모든 snapshot을 불러오지 않고, 사용자 범위의 요약 쿼리 하나(헤더 컬럼과 개수 하위 쿼리, join 없음)로 조회한다. 기술 화면은 언어와 의존성만, 근거 화면은 실제로 쓰는 하위 데이터(언어 제외 전부)만 사용자 범위 쿼리로 읽는다. snapshot 상세와 분석은 전체를 그대로 불러오며, 응답은 바뀌지 않았다(golden 통합 테스트). prepared statement 수: snapshot 2개 목록 18개에서 2개, 기술 화면 각각 10개에서 4개. 근거: 독립 QA PASS(2회), 백엔드 테스트 240개 실패 0, 환경 조건부 건너뜀 2개. 남은 과제(Low): 다른 사용자 접근 거부는 목록만 테스트하고, 같은 검사를 쓰는 기술·근거 화면은 테스트하지 않음. 목록 endpoint는 여전히 개수 제한이 없음(계약 문제) |
| C-2 | 일괄 insert | **해결 (2026-10-17):** 변경되지 않는 snapshot 하위 entity와 지식 chunk가 `Persistable`을 구현해, `saveAll`이 merge 대신 persist로 저장하므로 INSERT마다 SELECT가 실행되지 않는다. embedding은 같은 SQL로 JDBC batch 한 번에 기록한다. 이전 chunk를 stale로 표시하는 작업은 기존 반복문과 같은 범위의 집합 UPDATE 한 번으로 처리한다. Hibernate JDBC batch를 켰다(`batch_size` 100, `order_inserts`). 트랜잭션, rollback 원자성, 순서, 사용자·snapshot 외래 키, vector 값은 바뀌지 않았다. prepared statement 수: 파일 500개 snapshot은 3개, chunk 200개 수집은 8개와 JDBC embedding batch 1회. 근거: 독립 QA PASS, 백엔드 테스트 248개 실패 0, 환경 조건부 건너뜀 2개. 해결 (2026-10-17, 독립 QA PASS): `isNew()`가 `true` 고정값 대신 load·persist 시 해제되는 transient flag를 반환하며, 재수집과 보관 후 chunk의 `STALE` 상태를 확인하는 테스트를 추가했다. 남은 과제(Low): 외래 키가 일반 UUID 컬럼이라 이후 부모와 하위 항목을 함께 flush하면 `order_inserts`가 순서를 바꿀 수 있음 |
| C-3 | 일괄 조회 | **해결 (2026-10-16):** Skill Matrix, rule evaluation의 누락 근거, 추천 세트/로드맵 이력 조회가 하위 데이터를 `IN` 일괄 쿼리로 가져와 메모리에서 묶고 `ORDER BY`를 명시하므로 쿼리 수가 일정하다(Hibernate 통계: Skill Matrix는 평가 1개나 2개에서 6개, 세트와 로드맵은 1개나 2개에서 4개). fact 저장도 일괄로 처리한다. AI 커리어 코칭의 두 경로는 필요한 세트나 로드맵 하나만 사용자 범위의 대상 쿼리로 읽으며, `generatedAt` 내림차순, `id` 내림차순 선택과 기존 예외는 그대로다. 근거: 독립 QA PASS, 백엔드 테스트 235개 실패 0, 환경 조건부 건너뜀 2개, 새 Skill Matrix 저장/재조회 통합 테스트. 남은 과제(Low): 하위 항목이 여러 개인 세트 내부 순서와 2개 이상 카테고리의 비어 있지 않은 누락 근거는 테스트로 확인하지 않음. 이력 endpoint는 여전히 개수 제한이 없음(계약 문제) |
| C-4 | AI 작업 lease와 산출물 index | **해결 (2026-10-16):** V29가 `ai_tasks.next_attempt_at`을 추가했다. 실행 시각이 된 QUEUED 작업과 15분 lease가 만료된 RUNNING 작업을 가져가고, 재시도 간격은 30초에서 최대 5분까지 늘어나며, 시도 한도에서 lease가 만료되면 `WORKER_LEASE_EXPIRED`로 실패한다. 산출물 조회는 사용자 범위로 제한되고 index가 있다. 근거: 독립 QA PASS, 백엔드 테스트 230개 실패 0 |
| C-5 | worker 작업 선점과 실패 로그 | **해결 (2026-10-16):** 분석, 저장소 동기화, AI 작업 선점 쿼리가 잠긴 행을 건너뛴다(`FOR NO KEY UPDATE SKIP LOCKED`). 취소에 쓰는 ID별 잠금은 그대로 대기한다. 분석, 동기화, AI worker는 예상치 못한 실패 시 작업 ID와 예외 클래스 이름만 담은 WARN 로그를 남긴다. 새 지식 worker 단위 테스트가 성공과 실패 매핑 네 가지를 검증한다. 근거: 독립 QA PASS, 백엔드 테스트 239개 실패 0, 환경 조건부 건너뜀 2개. 남은 과제(Low): skip-locked 테스트에 다음 행으로 넘어가는 경우를 명시적으로 확인하는 사례가 없음 |
| C-6 | 프론트엔드 캐시 갱신 | **해결 (2026-10-16):** 분석이 성공하면 분석 이력과 대시보드 query도 무효화한다. 근거: 커밋 `6e078ac`, 독립 QA PASS |

무제한 이력 endpoint의 페이지 처리 같은 계약 변경, 오류 상태 코드 재매핑, Proposed ADR-031·ADR-033·ADR-034에 의존하는
작업은 이 묶음에서 제외했다.
