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
| UX-3 | 공용 표시 문구 | **해결 (2026-10-16):** 공용 `shared/ui/labels.ts`가 enum마다 한국어 표시 하나와 중립적인 "알 수 없음" 대체 문구를 제공하고, 화면마다 중복되던 변환 함수를 대체했다. 저장소 상태와 동기화 상태를 서로 다른 말로 구분하고, 동기화 상태는 텍스트 배지로 표시한다. 내부 용어(M36, MVP, embedding provider, HTTP 상태 코드 그대로 표시, 영어 eyebrow)를 없앴고, 작업 오류 코드는 안전한 한국어 메시지로 바꾸며, 빈 상태 문구는 특정 직무를 지칭하지 않는다(UI-03, UI-07, UI-08, UI-21, UI-22, UI-26). 근거: 독립 QA PASS, 프론트엔드 45개 파일 / 테스트 129개 PASS, 빌드 PASS. 남은 Low 후속 과제: 향후 카탈로그에 추가될 기술 이름은 API 이름으로 대체 표시되지 않고, 일부 헤더 문장에 엔진 이름(Rule Engine, Skill Matrix, Skill Gap)이 남아 있으며, 저장소 카드에 동기화 상태가 두 번 표시되고, `RATE_LIMIT_EXCEEDED`로 최종 실패한 작업에 재시도 문구가 나올 수 있다 |
| UX-4 | 확인창과 폼 | 로드맵 보관에 확인 단계가 없고, 확인창 방식이 세 가지로 나뉘며, 항목별 폼 오류 표시가 없음 |
| UX-5 | 디자인 토큰과 접근성 | 색상 토큰이 없고, 어두운 배경에서 포커스가 보이지 않으며, 비교 표의 스크롤 영역에 키보드로 접근할 수 없음 |
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
| C-1 | snapshot 읽기 경로 | snapshot 목록이 개수를 세려고 모든 snapshot의 하위 데이터를 전부 불러오고, 기술·근거 화면도 snapshot 전체를 불러옴 |
| C-2 | 일괄 insert | ID를 미리 정하는 하위 entity 때문에 snapshot과 지식 저장 시 INSERT마다 SELECT가 한 번씩 더 실행됨 |
| C-3 | 일괄 조회 | Skill Matrix, rule evaluation, 추천 이력 조회와 커리어 코칭 요청 경로에 N+1 쿼리가 있음 |
| C-4 | AI 작업 lease와 산출물 index | **해결 (2026-10-16):** V29가 `ai_tasks.next_attempt_at`을 추가했다. 실행 시각이 된 QUEUED 작업과 15분 lease가 만료된 RUNNING 작업을 가져가고, 재시도 간격은 30초에서 최대 5분까지 늘어나며, 시도 한도에서 lease가 만료되면 `WORKER_LEASE_EXPIRED`로 실패한다. 산출물 조회는 사용자 범위로 제한되고 index가 있다. 근거: 독립 QA PASS, 백엔드 테스트 230개 실패 0 |
| C-5 | worker 작업 선점과 실패 로그 | 잠긴 행을 건너뛰지 않고 대기하며, worker 세 곳이 예상치 못한 실패를 로그에 남기지 않고, 지식 worker에 단위 테스트가 없음 |
| C-6 | 프론트엔드 캐시 갱신 | **해결 (2026-10-16):** 분석이 성공하면 분석 이력과 대시보드 query도 무효화한다. 근거: 커밋 `6e078ac`, 독립 QA PASS |

무제한 이력 endpoint의 페이지 처리 같은 계약 변경, 오류 상태 코드 재매핑, Proposed ADR-031·ADR-033·ADR-034에 의존하는
작업은 이 묶음에서 제외했다.
