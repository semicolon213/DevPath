# DevPath 기능 상태

## 1. 목적과 권한

이 문서는 DevPath의 구현 상태와 최신 검증 결과를 기록하는 **유일한 상태 문서**다. 에이전트 가이드(`AGENT.md`,
`AGENTS.md`, `CLAUDE.md`)와 다른 문서는 상태·버전·테스트 수를 따로 적지 않고 이 문서를 가리킨다. 이 문서는 요구사항
원천이 아니며, 범위와 마일스톤 완료는 `docs/01_SRS.md`, 엔진 명세, `docs/19_Roadmap.md`가 정한다.
`docs/20_MVP_Release_Evidence.md`는 2026-09-28 MVP 게이트 기록으로 고정되어 있다. 이 문서는 마일스톤 완료를 선언하거나
소유자 승인을 대신하지 않는다.

기준 시점: `main` 커밋 `f354132`(커밋일 2026-10-24), 문서 갱신일 2026-10-26.

상태 용어:

| 상태 | 의미 |
|---|---|
| Implemented | 코드, 계약, 자동 테스트가 있고 해당 기능 자체에 열린 승인 게이트가 없다. |
| Implemented, approval pending | 코드와 자동 검증 근거는 있으나 로드맵에 소유자 승인, live provider 검증, 운영 결정이 아직 열려 있다. |
| Not implemented | 명세는 있으나 코드와 계약에 없다. |

## 2. 검증 결과 (2026-10-26)

코디네이터가 2026-10-26에 `main` `f354132`에서 `npm run verify:mvp`를 실행했다(Windows, JDK 21.0.12, Docker 실행 중,
`DOCKER_API_VERSION=1.44`).

| 검사 | 결과 |
|---|---|
| 보안 경계 검사(scripts security check) | PASS, 3건 |
| 백엔드 clean build와 테스트(JUnit, 아키텍처, Testcontainers PostgreSQL) | BUILD SUCCESSFUL. 261건, 실패 0, 오류 0, 환경 조건부 catalog 검사 SKIP 2 |
| 프론트엔드 clean install | PASS |
| 프론트엔드 Vitest | PASS, 55개 파일 261건 |
| Playwright e2e(Chromium + axe) | 게이트 안에서는 중단: Windows가 TCP 4149–4248을 예약(Hyper-V/WinNAT)했고 e2e dev server가 4173 포트에 고정되어 있어 EACCES 발생(코드 결함 아님). 같은 suite를 임시 설정(포트 5199)으로 따로 실행: PASS, 15건(임시 설정 삭제, 저장소 변경 없음) |
| 프론트엔드 production build | PASS |
| `npm audit` | 취약점 0건 |
| `npm run contract:lint`(별도 실행) | OpenAPI 유효, 오류 0, 경고 10 |

## 3. 기능 상태

| 영역 | 기능 | 상태 | 로드맵 | 프론트엔드 경로 |
|---|---|---|---|---|
| Identity | GitHub OAuth2 로그인, opaque 서버 세션, CSRF, 로그아웃, 현재 사용자, 공급자 독립 `User` | Implemented | M16 | `/`, `/settings` |
| Profile | 사용자 프로필과 Career/Company preference | Implemented | M17 | `/settings/profile`, `/onboarding` |
| GitHub | 연결, 저장소 검색·가져오기 | Implemented | M18 | `/settings/integrations`, `/repositories` |
| Repositories | 등록, 보관·복원, durable 동기화, 불변 snapshot, 근거·활동 추출 | Implemented | M19–M22 | `/repositories`, `/repositories/:repositoryId`, `/repositories/:repositoryId/snapshots/:snapshotId` |
| Analysis | 결정론 Rule Engine 분석, 이력, 점수 상세, 근거 연결, 비교 | Implemented | M23–M25, M32 | `/analyses`, `/analyses/:analysisId`, `/analyses/compare` |
| Skills | Skill Matrix 생성·조회·비교, 기술별 불변 평가 이력 | Implemented | M26 | `/skills`, `/skills/:skillId`, `/skills/compare` |
| Skills | 기술별 근거 상태(`NONE`/`INSUFFICIENT`/`SUFFICIENT`, D-4): Flyway V30 `skill-matrix-v3`, Rule Engine 계산, API `evidenceStatus`, 화면 표시(UI-10) | Implemented | — | `/skills`, `/skills/:skillId`, `/skills/compare`, `/analyses/:analysisId` |
| Careers | Backend/Frontend 직무 catalog, `readiness-v1` Career Readiness와 Skill Gap | Implemented | M27–M28, M30 | `/careers`, `/careers/:careerId`, `/career-readiness`, `/career-readiness/:careerReadinessId` |
| Companies | 회사 catalog | Implemented | M29 | `/companies`, `/companies/:id` |
| Companies | 회사별 readiness와 추천 modifier | Not implemented (MVP 이후) | M29–M30 | — |
| Recommendations | `recommendation-v1` 결정론 추천과 `roadmap-v1` 학습 로드맵 | Implemented | M31 | `/recommendations`, `/recommendations/:recommendationId`, `/roadmap` |
| Dashboard | 사용자 대시보드 요약 | Implemented | M33 | `/dashboard` |
| MVP gate | MVP 완료 근거 묶음 | Implemented, approval pending | M34 | — |
| Knowledge | Notion OAuth 연결과 공유 페이지 메타데이터 | Implemented, approval pending | M35 | `/settings/integrations` |
| Knowledge | Notion 수집: 정규화, chunk, Ollama embedding, pgvector, 보관·재색인 | Implemented, approval pending | M36 | `/knowledge`, `/knowledge/:documentId` |
| Knowledge | owner 필터를 강제하는 semantic retrieval과 인용 | Implemented, approval pending | M37 | `/knowledge` |
| AI | provider port, Ollama generation adapter, 취소, 제한 재시도, 검증된 산출물, 만료 lease 회수(V29) | Implemented, approval pending | M38–M42 | — |
| AI | 기술 분석 설명 | Implemented, approval pending | M42 | `/skills` |
| AI | 저장소 리뷰와 저장소 요약 | Implemented, approval pending | M43 | `/analyses/:analysisId` |
| AI | 커리어 코칭과 학습 추천 | Implemented, approval pending | M43 | `/career-readiness`, `/roadmap` |
| AI | 생성 산출물 목록, 수동 재시도, 편집·내보내기 | Not implemented | M43–M44 | — |
| Frontend | 공통 레이아웃·전역 메뉴·data router(UX-1), AI 패널 공용 틀(UX-2), 공용 라벨(UX-3), 확인창·폼(UX-4), 디자인 토큰·접근성(UX-5), 공용 화면 상태(UX-6) | Implemented | — | 전체 |
| Frontend | '성장 경로' 화면 개편 UI-1~UI-5, 스타일 파일 분리(CSS-SPLIT), 디자인 정합성 ALIGN-P1·P2·L·E | Implemented | — | 전체 |
| API | 계약 불일치 D1–D15 해결(API-FIX, `docs/22_API_Reference.md` 부록 A) | Implemented | — | — |
| Artifacts | Portfolio, Resume, Interview 준비 | Not implemented | M45–M47 | — |
| Operations | Administration workflow | Not implemented | — | — |
| Operations | 운영 관측, 배포 플랫폼, secret 관리 | Not implemented. Proposed ADR-031, ADR-033, ADR-034에 막힘 | M52–M53 | — |

프론트엔드 개편과 정합성 작업은 모두 표시만 바꿨다. 화면은 저장된 Rule Engine 결과와 API 값만 보여주며 점수·차이·상태를
클라이언트에서 계산하지 않는다. 결정 기록: D-1~D-3·D-7·D-8(UX), D-4(근거 상태, `02_Rule_Engine.md` §16.6), 디자인 방향과
ALIGN 결정 D1–D12는 `DESIGN.md`.

## 4. API 계약 범위

`contracts/openapi/devpath-openapi.yaml`은 75개 operation으로 `docs/10_API_Specification.md`의 정식 API ID 160개 중 70개를
다룬다.

| 그룹 | 계약에 있음 | 명세 | 주요 공백 |
|---|---:|---:|---|
| Identity (`API-ID`) | 8 | 10 | API-ID-008, API-ID-009 |
| Integrations (`API-INT`) | 10 | 12 | API-INT-006, API-INT-012 |
| Repositories (`API-REP`) | 10 | 12 | API-REP-003, API-REP-010 |
| Analysis (`API-ANA`) | 8 | 8 | — |
| Skills (`API-SKL`) | 5 | 7 | Technology and framework proficiency |
| Careers (`API-CAR`) | 5 | 6 | API-CAR-003 |
| Companies (`API-CMP`) | 2 | 5 | Company preference, company readiness and comparison |
| Recommendations (`API-REC`) | 4 | 8 | Recommendation requests and related operations |
| Learning (`API-LRN`) | 4 | 10 | Roadmap creation and progress operations |
| Dashboard (`API-DSH`) | 1 | 10 | Section-level dashboard endpoints |
| Knowledge (`API-KNW`) | 8 | 11 | Manual document metadata operations |
| AI (`API-AI`) | 5 | 7 | API-AI-004 retry, API-AI-006 artifact list |
| Prompt, Portfolio, Resume, Interview, Admin, Open (`API-PRM`, `API-PRT`, `API-RSM`, `API-ITV`, `API-ADM`, `API-OPEN`) | 0 | 54 | Not started |

`docs/22_API_Reference.md` 부록 A의 계약 불일치 D1–D15는 2026-10-19(docs/10을 구현에 맞춤)와 2026-10-21(API-FIX 코드·OpenAPI
정렬)에 해결되었다. 남은 Low 항목은 그 부록을 본다.

## 5. 열린 게이트

- MVP 게이트(M34)와 구현된 지식(M35–M37)·AI(M38–M43) 기능의 소유자 승인이 남아 있고, `docs/19_Roadmap.md`의 live provider
  검증이 필요하다.
- 운영 object storage(RM-OI-007), 배포와 secret(RM-OI-008), 시연 환경(RM-OI-010)은 로드맵 §76에서 열려 있다.
- Proposed ADR-031, ADR-033, ADR-034가 운영 관측, 배포, secret 작업을 막는다.

## 6. 남은 과제

2026-10-15 감사(UI/UX 26건, 코드 19건)의 슬라이스(UX-1~UX-6, C-1~C-6)와 이후 UI-1~UI-5, CSS-SPLIT, ALIGN은 모두 독립 리뷰 PASS로
해결되었다. 슬라이스별 상세 기록은 커밋 `f354132`의 이 문서에 있다. 남은 항목은 후보이며, 각각 소유자 승인·구현·독립 QA가 필요하다.

- e2e dev server 포트 4173이 `frontend/playwright.config.ts`에 고정되어 Windows 예약 포트와 충돌할 수 있다(설정 가능하게 만드는 후속 작업 예정).
- 스냅샷 목록과 이력 endpoint에 페이지 제한이 없다(계약 결정 필요, C-1·C-3).
- 대시보드 목표 기준선·단계 타일(감사 A-24)과 저장소 목록의 주 기술 표시(A-19)는 API 변경이 필요하다.
- 비로그인 deep link가 인증 레이아웃을 그리고, router가 모듈 import 시점에 생성된다(UX-1).
- `ConfirmDialog` 뒤 배경이 inert 처리되지 않는다(UX-4).
- 보관된 저장소: 분석 시작 불가 문구가 백엔드 동작과 다르고, 분석 버튼이 설명 없이 비활성화된다(UX-4, ALIGN).
- 저장된 생성 작업 조회가 404여도 저장된 작업 정보가 지워지지 않는다(ALIGN-E).
- 근거 상태 Low: 이력 항목 `NONE`/`INSUFFICIENT` 단위 테스트 없음, 과거 matrix에도 `NONE` 재분석 안내 표시, 표시 텍스트와 중복된 `aria-label`(UX-6/D-4).
- 다시 시도·돌아가기 라벨이 일반적이다(UX-6). 일부 텍스트 링크 높이 24–26px는 WCAG 2.5.8은 충족하지만 `DESIGN.md` 44px 지침에 못 미친다.
- 규칙 결과 링크가 모두 한 섹션 anchor를 가리키고, 다섯 AI 패널이 같은 prop 연결을 반복한다(UX-2).
- 포커스 링·색상 style guard 한계: 같은 이상 specificity 규칙이 분리 링을 덮을 수 있고, guard가 다른 색상 함수·이름 색상을 검사하지 않는다(UX-5).
- `order_inserts`가 향후 부모/자식 혼합 flush 순서를 바꿀 수 있다(외래 키가 일반 UUID 컬럼, C-2).
- 수동 스크린 리더와 200% 확대 점검을 하지 않았다.
