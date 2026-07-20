<!--
한국어판 문서입니다. 원본 기준 문서: docs/03_Career_Path_Engine.md
요구사항 ID, 엔진명, 기술명, 스키마명은 추적성을 위해 필요한 경우 원문 표기를 유지합니다.
-->

# DevPath Career Path Engine 아키텍처

- **문서 ID:** DevPath-ARCH-CPE-001
- **버전:** 1.0
- **상태:** 초안
- **관련 문서:** `docs/00_Project_Context.md`, `docs/01_SRS.md`, `docs/02_Rule_Engine.md`
- **작성일:** 2026-07-20

## 1. 목적

Career Path Engine은 Rule Engine의 결정적 평가 출력을 커리어 지향 의사결정 객체로 변환한다. 이 엔진은 커리어 준비도, 회사 준비도, 스킬 갭, 학습 우선순위, 기술 우선순위, 프로젝트 추천, Prompt Context fact를 생성한다.

Career Path Engine은 기술 점수를 계산하지 않고, LLM을 호출하지 않으며, 자연어 문장을 생성하지 않는다. 모든 기술 평가의 source of truth는 Rule Engine 출력이다.

## 2. 범위

범위에는 Career Engine 아키텍처, workflow, 입력/출력 모델, 커리어/회사 규칙 선택, 스킬 갭 분석, 로드맵 생성, 준비도 모델, 추천 우선순위화, 프로필, 요구사항, 로깅, 모니터링, 오류 처리, 확장 전략이 포함된다. 구현 코드, API, ERD, UML, LLM orchestration, 자연어 코칭 생성은 범위 밖이다.

SRS의 활성 커리어 범위는 Backend, Frontend, AI Engineer, DevOps, Security, Game, Embedded, Mobile, Data Engineer이다. 추가 커리어와 회사는 SRS 개정 전까지 확장 후보로만 취급한다.

## 3. 설계 원칙

| 원칙 | 설명 |
|---|---|
| 결정성 | 동일 Rule Engine 출력, 커리어, 회사, profile version은 동일 Career Engine 출력을 생성해야 한다. |
| Rule Engine 권위 | 기술 점수는 Rule Engine에서만 온다. |
| 구조화 출력 | 추천은 machine-readable object로 생성하고 자연어 렌더링은 AI/UI가 담당한다. |
| 설정 기반 profile | 커리어와 회사 우선순위는 versioned profile로 외부화한다. |
| 추적 가능성 | 모든 gap과 recommendation은 증거, profile, Rule Engine 결과를 참조한다. |
| 준비도 분리 | 커리어 준비도와 회사 준비도는 독립적으로 분류한다. |

## 4. Career Engine 아키텍처

| 구성요소 | 책임 |
|---|---|
| Career Invocation Facade | 요청, 권한, 선택 커리어/회사, Rule Engine 결과 참조를 검증한다. |
| Rule Output Reader | Skill Matrix, 점수, 증거, 신뢰도, 완전성을 로드한다. |
| Career Profile Resolver | 커리어 profile, 필수/선호 역량, roadmap template를 해석한다. |
| Company Profile Resolver | 회사 profile, 역량 강조, weight override를 해석한다. |
| Gap Analyzer | 현재 스킬과 기대 역량을 비교한다. |
| Readiness Classifier | gap과 Rule Engine fact를 기반으로 준비도 level을 분류한다. |
| Recommendation Object Builder | 기술, 프로젝트, 학습, 포트폴리오, 면접 등 구조화 추천 객체를 생성한다. |
| Roadmap Builder | prerequisite, priority, difficulty, duration에 따라 milestone을 정렬한다. |
| Prompt Context Assembler | Prompt Builder용 구조화 fact를 만든다. |
| Conflict Resolver | 커리어와 회사 우선순위 충돌을 결정적으로 해소한다. |

## 5. 워크플로

요청 검증 → Rule Engine 출력 로딩 → 커리어 profile 해석 → 회사 profile 해석 → eligibility 확인 → gap 분석 → 준비도 분류 → 추천 객체 생성 → 우선순위 정렬 → roadmap 생성 → Prompt Context 조립 → 저장 및 trace 순으로 처리한다.

## 6. 입력 모델

| 입력 | 설명 |
|---|---|
| User ID | 평가 소유자 |
| Rule Evaluation Result ID | 기술 평가 source reference |
| Selected Career | 목표 커리어 |
| Selected Company | 선택된 목표 회사 |
| Career/Profile Version | 커리어 profile version |
| Company/Profile Version | 회사 profile version |
| Output Scope | readiness, gap, roadmap, recommendation, prompt context 범위 |

## 7. 출력 모델

출력 package는 Career Evaluation Result ID, 사용자 ID, Rule Evaluation Result ID, 선택 커리어, 선택 회사, profile version, career readiness, company readiness, Skill Gap Report, Recommendation Objects, Learning Roadmap, Prompt Context, trace를 포함한다.

## 8. 커리어 규칙 선택

커리어 규칙 선택은 사용자 선택 커리어를 profile과 해석 정책에 매핑한다. 기술 점수 계산은 수행하지 않는다.

| SRS 커리어 | 활성 profile |
|---|---|
| Backend | Backend Engineer |
| Frontend | Frontend Engineer |
| AI Engineer | AI Engineer |
| DevOps | DevOps Engineer |
| Security | Security Engineer |
| Game | Game Developer parent profile |
| Embedded | Embedded Engineer |
| Mobile | Mobile Developer parent profile |
| Data Engineer | Data Engineer |

## 9. 회사 규칙 선택

회사 profile은 일반적인 엔지니어링 역량 강조만 표현한다. 비공개 채용 과정이나 내부 면접 rubric을 모델링하지 않는다.

| 회사 | 상태 | 강조 역량 |
|---|---|---|
| Google | 활성 | 아키텍처, 테스트, 언어 깊이, 복잡도 |
| Amazon | 활성 | backend reliability, DevOps, 운영 문서 |
| Naver | 활성 | 웹 서비스, 문서화, 데이터/search 인접 역량 |
| Kakao | 활성 | 제품 엔지니어링, 협업, 신뢰성 |
| Toss | 활성 | 테스트, 신뢰성, 영향 명확성 |
| Coupang | 활성 | 확장성, DevOps, 데이터 시스템 |
| Meta/Microsoft/Netflix/Line | 확장 후보 | SRS 개정 후 활성화 가능 |

## 10. 추천 전략

추천은 Skill Gap, weakness flag, 누락 필수 역량, 낮은 신뢰도, career priority, company override, prerequisite 순서에서 생성한다. 추천은 자연어가 아니라 type, priority, difficulty, duration, evidence, rationale code, completion criteria를 가진 구조화 객체이다.

## 11. 스킬 갭 분석

| 상태 | 의미 |
|---|---|
| Missing | 기대 역량에 측정 가능한 증거가 없다. |
| Weak | 증거는 있으나 threshold보다 낮다. |
| Partial | 일부 신호만 만족한다. |
| Sufficient | 기대 threshold를 만족한다. |
| Strong | 기대치를 초과하고 신뢰도가 충분하다. |

## 12. 학습 로드맵 생성

로드맵은 prerequisite, 필수 역량, 회사 강조, gap severity, difficulty, duration, portfolio value, evidence generation potential 순으로 정렬된 milestone 집합이다.

## 13. 커리어 준비도 모델

| 수준 | 측정 기준 |
|---|---|
| Beginner | 필수 역량 대부분이 누락되고 repository evidence가 제한적이다. |
| Junior Ready | 핵심 역량에 기본 증거가 있고 프로젝트, 문서, 활동 신호가 존재한다. |
| Mid-Level Ready | 다수 필수 역량이 충분하고 architecture/testing/maintainability 증거가 의미 있다. |
| Senior Potential | 핵심 범주 전반에 강한 증거와 성장, 소유권, 협업 신호가 있다. |
| Expert Potential | 복잡한 프로젝트 전반에 깊고 넓은 증거와 높은 신뢰도가 있다. |

## 14. 회사 준비도 모델

회사 준비도는 일반 커리어 준비도와 독립적으로 산출된다. 출력은 overall readiness, missing skills, missing experience, recommended projects, learning order, evidence confidence를 포함한다.

## 15. 추천 우선순위화

필수 역량 gap은 optional differentiator보다 우선한다. prerequisite은 advanced project보다 우선한다. 동일 impact에서는 낮은 effort를 우선할 수 있다. 충돌은 rationale code와 함께 trace에 기록한다.

## 16. 커리어 프로필

| 커리어 | 상태 | 핵심 기술 | 포트폴리오 기대 |
|---|---|---|---|
| Backend Engineer | 활성 | Java, Spring Boot, SQL, Redis, Docker | API 문서, architecture note, 배포 증거 |
| Frontend Engineer | 활성 | TypeScript, React, TailwindCSS, React Query | screenshot, live demo, UX flow |
| AI Engineer | 활성 | Python, FastAPI, Ollama/OpenAI optional, pgvector | 근거 기반 AI project README |
| Data Engineer | 활성 | SQL, Python, PostgreSQL, pipeline 도구 | schema 문서, pipeline 실행 지침 |
| DevOps Engineer | 활성 | Docker, GitHub Actions, Nginx, cloud deployment | 배포 README, workflow evidence |
| Security Engineer | 활성 | secure config, dependency hygiene, auth project | 보안 notes, test evidence |
| Game/Embedded/Mobile | 활성 parent | 각 도메인 framework와 구조 증거 | build instruction, demo, architecture |
| Full Stack/ML/Cloud/QA/System 등 | 확장 후보 | SRS 개정 후 정의 | SRS 개정 후 활성화 |

## 17. 회사 프로필

| 회사 | 상태 | 기술 초점 | 추천 우선순위 |
|---|---|---|---|
| Google | 활성 | scalable system, testing | architecture, interview prep |
| Amazon | 활성 | backend, database, DevOps | project, DevOps, architecture |
| Naver | 활성 | web service, data/search | portfolio, documentation |
| Kakao | 활성 | product service, collaboration | collaboration, project |
| Toss | 활성 | reliability, testing | testing, portfolio |
| Coupang | 활성 | commerce-scale backend, data | DevOps, data, architecture |
| Meta/Microsoft/Netflix/Line | 확장 후보 | SRS 개정 후 활성화 | SRS 개정 후 활성화 |

## 18. 기능 요구사항

### CR-001 — 커리어 선택 해석

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 커리어 선택 해석 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 커리어 선택 해석 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-002 — 커리어별 규칙 컨텍스트 선택

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 커리어별 규칙 컨텍스트 선택 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 커리어별 규칙 컨텍스트 선택 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-003 — 회사 선택 해석

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 회사 선택 해석 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 회사 선택 해석 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-004 — 회사별 규칙 컨텍스트 선택

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 회사별 규칙 컨텍스트 선택 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 회사별 규칙 컨텍스트 선택 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-005 — 스킬 갭 분석

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 스킬 갭 분석 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 스킬 갭 분석 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-006 — 학습 로드맵 생성

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 학습 로드맵 생성 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 학습 로드맵 생성 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-007 — 커리어 준비도 분류

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 커리어 준비도 분류 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 커리어 준비도 분류 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-008 — 회사 준비도 분류

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 회사 준비도 분류 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 회사 준비도 분류 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-009 — 추천 객체 생성

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 추천 객체 생성 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 추천 객체 생성 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-010 — 추천 우선순위 정렬

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 추천 우선순위 정렬 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 추천 우선순위 정렬 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-011 — 기술 우선순위 생성

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 기술 우선순위 생성 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 기술 우선순위 생성 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-012 — 프로젝트 추천 생성

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 프로젝트 추천 생성 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 프로젝트 추천 생성 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-013 — Prompt Context 조립

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 Prompt Context 조립 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | Prompt Context 조립 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-014 — 충돌 감지

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 충돌 감지 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 충돌 감지 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-015 — 데이터 부족 처리

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 데이터 부족 처리 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 데이터 부족 처리 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-016 — 출력 저장

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 출력 저장 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 출력 저장 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-017 — 재계산 트리거 처리

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 재계산 트리거 처리 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 재계산 트리거 처리 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-018 — 프로필 버전 검증

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 프로필 버전 검증 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 프로필 버전 검증 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-019 — 확장 후보 guardrail

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 확장 후보 guardrail 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 확장 후보 guardrail 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |

### CR-020 — 감사 trace 생성

| 항목 | 명세 |
|---|---|
| 설명 | Career Path Engine은 감사 trace 생성 기능을 구조화된 입력과 프로필 설정에 따라 결정적으로 수행해야 한다. |
| 입력 | Rule Engine 출력, Career Profile, Company Profile, 사용자 선택값 |
| 출력 | 구조화된 커리어 평가 결과, gap, roadmap, recommendation object, trace |
| 비즈니스 규칙 | 기술 점수는 계산하지 않으며 Rule Engine 출력을 그대로 사용해야 한다. |
| 검증 규칙 | 필수 ID, profile version, source reference, enum 값은 유효해야 한다. |
| 인수 기준 | 감사 trace 생성 요청 시 동일 입력과 동일 profile version은 동일 출력을 생성한다. |
| 의존성 | Rule Engine 출력, Career/Profile Store, Company/Profile Store |


## 19. 비기능 요구사항

| ID | 범주 | 요구사항 | 측정 기준 |
|---|---|---|---|
| CPE-NFR-001 | 성능 | 일반 career evaluation은 설정 SLA 내 완료되어야 한다. | 95 백분위 latency를 output size별로 측정한다. |
| CPE-NFR-002 | 신뢰성 | 회사 선택이 없어도 career readiness는 생성되어야 한다. | company profile 없이 career readiness가 성공한다. |
| CPE-NFR-003 | 확장성 | Rule Engine 업데이트 후 비동기 재계산을 지원해야 한다. | recalculation job queue가 동작한다. |
| CPE-NFR-004 | 유지보수성 | 커리어/회사 profile은 설정 기반으로 versioning되어야 한다. | profile 변경은 core logic 변경 없이 반영된다. |
| CPE-NFR-005 | 관측성 | readiness, gap, recommendation 지표가 발행되어야 한다. | career/company별 metric 조회가 가능하다. |

## 20. 로깅

평가 lifecycle, profile resolution, gap analysis, readiness classification, recommendation generation, conflict resolution, error handling을 기록한다. 로그는 secret과 raw private content를 포함하지 않는다.

## 21. 모니터링

career evaluation count, latency, career/company distribution, critical gap frequency, recommendation type distribution, unsupported selection count, low-confidence output rate를 모니터링한다.

## 22. 오류 처리

| 시나리오 | 처리 |
|---|---|
| 선택 커리어 없음 | validation error를 반환하고 커리어 선택을 요구한다. |
| 선택 회사 없음 | career readiness만 생성하고 company readiness는 생략한다. |
| 저장소 데이터 부족 | low-confidence output과 evidence-building recommendation을 생성한다. |
| Rule Engine 출력 누락 | 기술 source of truth가 없으므로 평가를 거부한다. |
| 미지원 기술 | unsupported fact를 보존하고 미지원 추천을 피한다. |
| 추천 충돌 | 결정적 conflict resolution을 적용하고 rationale을 기록한다. |

## 23. 향후 확장

Full Stack, Machine Learning, Cloud, Android, iOS, QA, System Engineer 및 Meta, Microsoft, Netflix, Line은 SRS 개정 후 활성화할 수 있다. 모든 확장은 기술 점수 계산 금지, LLM 호출 금지, 구조화 추천 객체만 생성한다는 제약을 유지해야 한다.
