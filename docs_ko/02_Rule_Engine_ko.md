<!--
한국어판 문서입니다. 원본 기준 문서: docs/02_Rule_Engine.md
요구사항 ID, 엔진명, 기술명, 스키마명은 추적성을 위해 필요한 경우 원문 표기를 유지합니다.
-->

# DevPath Rule Engine 아키텍처

- **문서 ID:** DevPath-ARCH-RE-001
- **버전:** 1.0
- **상태:** 초안
- **관련 문서:** `docs/00_Project_Context.md`, `docs/01_SRS.md`
- **작성일:** 2026-07-20

## 1. 목적

이 문서는 DevPath Rule Engine의 운영 가능한 아키텍처를 정의한다. Rule Engine은 DevPath의 결정적 비즈니스 평가 핵심으로, GitHub 및 Notion에서 정규화된 데이터를 사용하여 개발자 활동, 저장소 품질, 기술 스킬 증거, 성장 신호, 커리어 준비도 입력을 평가한다.

Rule Engine은 Career Path Engine, Prompt Builder, AI Engine, Dashboard, 향후 추천 구성요소가 소비하는 구조화된 출력을 생성한다. Rule Engine은 자연어 코칭을 생성하지 않고, LLM을 호출하지 않으며, 프롬프트 로직을 포함하지 않고, 추천을 직접 생성하지 않는다.

> 모든 점수는 오직 Rule Engine에서만 계산된다. LLM은 점수를 계산, 추론, 수정 또는 생성해서는 안 된다.

## 2. 범위

### 2.1 범위 내

- Rule Engine 구성요소 구조
- 결정적 평가 workflow
- 입력 데이터 기대사항
- 정규화 요구사항
- 평가 범주
- 점수 계산 모델
- 가중치 관리
- 규칙 상속, override, 우선순위
- 규칙 설정과 버전 관리
- 커리어별 및 회사별 규칙 적용 방식
- Skill Matrix와 출력 모델
- 오류 처리, 로깅, 모니터링, 확장 전략

### 2.2 범위 외

- 구현 코드
- API 명세
- 데이터베이스 ERD
- UML
- 프롬프트 구현
- LLM orchestration
- AI 추천 문구 생성

### 2.3 기준 문서 정렬

이 문서가 SRS와 충돌하는 경우 SRS가 우선한다. 이 문서는 SRS의 `FR-101`~`FR-180` 및 `RR-001`~`RR-010`을 Rule Engine 범위의 권위 있는 요구사항으로 간주한다.

## 3. 설계 원칙

| 원칙 | 설명 | 엔지니어링 영향 |
|---|---|---|
| 결정성 | 동일한 정규화 입력, 규칙 설정, 규칙 버전은 동일한 출력을 생성해야 한다. | 난수, LLM 호출, 암묵적 시간 의존 계산을 금지한다. |
| 설정 기반 동작 | 비즈니스 점수 산정 로직을 하드코딩하지 않는다. | 규칙, 가중치, 임계값, 우선순위, override는 버전 관리되는 설정으로 외부화한다. |
| 설명 가능한 점수 | 모든 점수는 규칙 ID, 증거 ID, 가중치, 공식 버전으로 추적 가능해야 한다. | 출력은 계산 trace와 증거 참조를 포함한다. |
| 관심사 분리 | Rule Engine은 계산하고 AI는 설명한다. | Prompt Builder와 AI Engine에는 점수 계산 로직을 두지 않는다. |
| 측정 가능성 | 모든 평가는 측정 가능한 신호 또는 명시적 누락 상태를 생성해야 한다. | README 누락도 측정 가능한 부재로 기록한다. |
| 버전 안전성 | 규칙 변경은 과거 출력을 조용히 변경해서는 안 된다. | 모든 출력은 규칙 버전과 입력 스냅샷 ID를 보존한다. |
| 실패 안전성 | 부분 실패가 거짓 신뢰도를 만들어서는 안 된다. | 출력은 완전성, 신뢰도, 경고, skip 정보를 포함한다. |
| 확장성 | 신규 범주, 커리어, 회사, 규칙은 설정과 adapter를 통해 추가한다. | 핵심 실행 계약은 안정적으로 유지한다. |

## 4. Rule Engine 아키텍처

| 구성요소 | 책임 |
|---|---|
| Rule Invocation Facade | 평가 요청을 수신하고 범위와 권한을 검증한다. |
| Input Snapshot Loader | 불변 정규화 스냅샷을 로드한다. |
| Rule Catalog Resolver | 규칙 버전, 커리어, 회사, 평가 유형에 맞는 규칙 catalog를 해석한다. |
| Rule Dependency Resolver | 규칙 의존성과 실행 graph를 결정한다. |
| Rule Executor | 조건, 임계값, 점수 공식을 결정적으로 실행한다. |
| Evidence Resolver | 정규화 레코드를 규칙 증거 객체에 매핑한다. |
| Weight Resolver | 기본, 커리어별, 회사별 가중치를 적용한다. |
| Score Aggregator | 규칙 점수를 범주, 구성요소, 전체 점수로 집계한다. |
| Confidence Calculator | 증거 완전성, 최신성, 신뢰성을 기반으로 신뢰도를 계산한다. |
| Skill Matrix Builder | 규칙 출력과 증거를 Skill Matrix 항목으로 변환한다. |
| Output Assembler | downstream 시스템용 구조화 결과를 조립한다. |
| Trace Logger | 실행 trace, skip, warning, error, 공식 메타데이터를 기록한다. |

## 5. 평가 파이프라인

| 단계 | 이름 | 설명 | 출력 |
|---:|---|---|---|
| 1 | 요청 검증 | 사용자, 저장소 범위, 커리어, 회사, 규칙 버전을 검증한다. | 수락 또는 거부된 평가 요청 |
| 2 | 스냅샷 로딩 | 정규화된 GitHub 및 Notion 스냅샷을 로드한다. | 불변 입력 bundle |
| 3 | 완전성 평가 | 사용 가능 데이터와 누락 데이터를 판단한다. | 데이터 완전성 profile |
| 4 | 규칙 catalog 해석 | 기본, 커리어별, 회사별 규칙 설정을 선택한다. | 유효 규칙 세트 |
| 5 | 실행 순서 결정 | 의존성, 범주, 우선순위에 따라 규칙을 정렬한다. | 실행 계획 |
| 6 | 증거 해석 | 정규화 레코드를 규칙 증거 query에 연결한다. | 증거 map |
| 7 | 규칙 실행 | 조건, 임계값, 공식을 평가한다. | 원시 규칙 결과 |
| 8 | 점수 집계 | 범주, 스킬, 구성요소, 전체 점수를 계산한다. | 집계 점수 모델 |
| 9 | 신뢰도 계산 | 증거 강도와 완전성을 반영한다. | 신뢰도 모델 |
| 10 | Skill Matrix 생성 | 스킬 수준 항목을 생성한다. | Skill Matrix |
| 11 | 출력 조립 | downstream-ready 결과를 만든다. | Rule Engine 출력 package |
| 12 | 저장 및 trace | 결과, trace, warning, metadata를 저장한다. | 평가 결과 ID |

## 6. 입력 데이터 모델

Rule Engine은 논리적 `EvaluationInputSnapshot`을 소비한다. 이 모델은 API나 DB 스키마가 아니라 구현 계약이다.

| 입력 영역 | 주요 내용 |
|---|---|
| Snapshot ID | 정규화 입력 데이터의 불변 식별자 |
| User ID | 평가 소유자 |
| Repository Set | 평가 대상 저장소 목록 |
| GitHub Data Bundle | 저장소, 커밋, 브랜치, PR, 이슈, README, 의존성, 언어, 디렉터리 데이터 |
| Notion Data Bundle | 워크스페이스, 페이지, 회고, 문서, 학습 노트, 프로젝트 노트 |
| Career Context | 커리어 가중 평가 요청 시 선택된 커리어 |
| Company Context | 회사 가중 평가 요청 시 선택된 회사 |
| Collection Metadata | 제공자 timestamp, sync 상태, rate limit 상태, 정규화 버전 |

## 7. 정규화 규칙

Rule Engine은 원시 provider 응답을 직접 파싱하지 않고 Normalizer가 생성한 레코드를 사용한다.

| 영역 | 규칙 |
|---|---|
| Provider ID | 원천 provider ID와 내부 normalized ID를 모두 보존한다. |
| Timestamp | 표준 timezone으로 변환하고 원본 값을 보존한다. |
| File Path | 경로 구분자, casing 정책, 저장소 상대 경로를 정규화한다. |
| Language | 언어명을 통제된 taxonomy에 매핑하고 원 provider label을 보존한다. |
| Framework | framework명과 alias를 taxonomy로 정규화한다. |
| Dependency | manifest 유형, 의존성명, 버전, scope, 출처 파일을 정규화한다. |
| Empty State | missing, empty, inaccessible, unsupported, corrupted를 구분한다. |

## 8. 평가 범주

| 범주 | 목적 | 주요 증거 |
|---|---|---|
| Programming Language | 언어 사용 깊이와 다양성을 평가한다. | 언어 통계, 파일 경로, manifest |
| Framework | framework 경험을 탐지하고 평가한다. | 의존성, 설정 파일, 구조 관례 |
| Database | 데이터 저장 기술을 탐지한다. | 의존성, 설정, 코드 경로, 문서 |
| Architecture | 구조적 엔지니어링 성숙도를 평가한다. | 디렉터리, 모듈 경계, 설계 문서 |
| Testing | 테스트 규율과 범위를 평가한다. | 테스트 파일, 테스트 의존성, CI |
| DevOps | 운영 및 배포 신호를 평가한다. | Docker, CI/CD, 배포 설정 |
| Documentation | README와 프로젝트 문서를 평가한다. | README, Notion, API/Architecture 문서 |
| Collaboration | 협업과 프로젝트 관리 신호를 평가한다. | PR, 리뷰, 이슈, 댓글, 커밋 |
| Repository Quality | 저장소 완성도와 유지보수성을 종합한다. | 구조, 문서, 테스트, 활동 |
| Growth Trend | 시간에 따른 성장 신호를 평가한다. | commit timeline, 기술 도입, 문서 개선 |
| Activity | 활동 지속성과 최신성을 평가한다. | commit, PR, issue, sync timestamp |
| Project Complexity | 의미 있는 구현 복잡도를 평가한다. | 모듈, 기술 통합, 배포, 아키텍처 |
| Technology Diversity | 얕은 키워드 나열을 배제하고 기술 폭을 평가한다. | 언어, framework, DB, DevOps 도구 |
| Security Practices | 기본 보안 엔지니어링 신호를 평가한다. | secret 부재, 의존성 관리, 보안 설정 |
| Maintainability | 변경 용이성과 이해 가능성을 평가한다. | 구조, 테스트, 문서, 의존성 관리 |
| Code Organization | 디렉터리와 naming 구성을 평가한다. | 파일 경로, 구조, 설정 배치 |

## 9. 점수 계산 전략

점수는 기본적으로 `0`~`100` 범위를 사용한다. 규칙 점수는 범주 점수로 집계되고, 범주 또는 구성요소 점수는 전체 점수로 집계된다. 신뢰도는 점수와 별도이며, 증거 완전성·최신성·일관성·범위·접근 가능성을 반영한다.

| 점수 유형 | 설명 |
|---|---|
| Rule Score | 단일 규칙 공식의 결과 |
| Category Score | 범주 내 규칙 점수의 가중 집계 |
| Skill Score | Skill Matrix 항목에 매핑된 점수 |
| Repository Score | 단일 저장소 기준 집계 점수 |
| Activity Score | timestamp 기반 활동 점수 |
| Growth Score | time-series 증거 기반 성장 점수 |
| Overall Score | 설정된 구성요소 가중 집계 점수 |

## 10. 가중치 관리

| 계층 | 목적 |
|---|---|
| Base Weight | 플랫폼 기본 중요도 |
| Career Weight | 선택 커리어에 따른 중요도 조정 |
| Company Weight | 선택 회사에 따른 역량 강조 |
| Rule Override | 특정 규칙의 세부 조정 |

유효 가중치는 기본 규칙 가중치, 범주 가중치, 커리어 override, 회사 override, 버전 보정, enabled 상태 순으로 해석한다.

## 11. 규칙 실행 순서

규칙은 입력 검증, taxonomy 및 증거 매핑, 원자적 탐지, 범주 점수화, 스킬 매핑, 집계, 신뢰도 계산, 출력 검증 순서로 실행한다. 의존 규칙이 실패하면 dependent rule은 `SKIPPED`로 기록하고 누락 의존성을 trace에 남긴다.

## 12. 규칙 설정

Rule Engine은 비즈니스 점수 산정 로직을 하드코딩하지 않는다. 규칙은 향후 YAML 또는 JSON으로 표현 가능한 버전 관리 설정으로 정의한다.

| 설정 항목 | 설명 |
|---|---|
| Rule ID | 추적 가능한 안정 식별자 |
| Category | 평가 범주 |
| Weight | 부모 범주 또는 집계에 대한 기여도 |
| Priority | 동일 의존성 수준 내 실행 순서 |
| Threshold | 점수 산정을 위한 경계값 |
| Conditions | 정규화 입력에 대한 결정적 조건 |
| Score Formula | 공식 참조와 parameter |
| Enabled Flag | 규칙 활성 여부 |
| Version | 규칙 정의 버전 |
| Inheritance | 상위 규칙 또는 group 상속 |
| Override | 커리어/회사별 조정 |

## 13. 규칙 버전 관리

규칙 정의, 규칙 세트, 기술 taxonomy, 공식 library, 커리어 mapping, 회사 mapping은 변경 시 버전 관리되어야 한다. 이력 재계산은 입력 스냅샷 ID, 규칙 세트 버전, taxonomy 버전, 커리어/회사 mapping 버전을 명시해야 한다.

## 14. 커리어별 규칙

커리어별 규칙은 새 평가 범주를 만들지 않고 우선순위, 가중치, 임계값, 필수 증거 수준을 조정한다.

| 커리어 | 평가 우선순위 | 가중치 조정 |
|---|---|---|
| Backend | 언어 깊이, framework, database, architecture, testing, DevOps | backend framework, database, testing 가중치 증가 |
| Frontend | framework, UI 구조, testing, documentation | frontend framework, code organization 가중치 증가 |
| AI Engineer | Python/AI framework, vector DB, documentation | AI framework, data handling 가중치 증가 |
| DevOps | CI/CD, Docker, deployment, infrastructure | DevOps와 운영 문서 가중치 증가 |
| Security | security practice, dependency hygiene, testing | security, testing, maintainability 가중치 증가 |
| Game | game framework, language, complexity | game framework, complexity 가중치 증가 |
| Embedded | C/C++, low-level structure, documentation | language, organization, documentation 가중치 증가 |
| Mobile | mobile framework, app structure, testing | mobile framework, platform structure 가중치 증가 |
| Data Engineer | database, pipeline, DevOps, documentation | database, pipeline, DevOps 가중치 증가 |

## 15. 회사별 규칙

회사별 규칙은 일반적인 엔지니어링 역량 기반 가중치 전략만 표현한다. 비공개 채용 절차나 내부 면접 기준을 모델링한다고 주장해서는 안 된다.

| 회사 | 가중치 전략 |
|---|---|
| Google | 아키텍처, 테스트, 언어 깊이, 프로젝트 복잡도 강조 |
| Amazon | backend reliability, DevOps, 운영 문서, activity 강조 |
| Naver | 웹 서비스, 문서화, 데이터/search 인접 신호 강조 |
| Kakao | 제품 엔지니어링, 협업, 신뢰성 강조 |
| Toss | 테스트, 신뢰성, 영향 명확성 강조 |
| Coupang | 확장성, DevOps, 데이터 시스템, 운영 활동 강조 |

## 16. Skill Matrix

Skill Matrix는 측정 가능한 증거를 구조화된 스킬 정보로 변환한 Rule Engine 출력이다.

| 항목 | 설명 |
|---|---|
| Skill ID | 안정 스킬 식별자 |
| Skill Name | 사용자 표시용 스킬명 |
| Skill Category | 언어, framework, DB, DevOps, testing 등 |
| Score | Rule Engine 결정 점수 |
| Skill Level | 설정된 threshold 기반 수준 |
| Confidence | 점수와 별개의 증거 신뢰도 |
| Evidence IDs | 관련 정규화 증거 참조 |
| Related Repository IDs | 스킬에 기여한 저장소 |
| Growth Trend | 결정적 성장 추세 |
| Weakness/Strength | threshold 기반 약점/강점 flag |

## 17. 출력 모델

Rule Engine은 `RuleEvaluationResult` package를 생성한다. 이 package는 평가 결과 ID, 사용자 ID, 스냅샷 ID, 저장소 범위, 규칙 세트 버전, 커리어/회사 컨텍스트, 범주 점수, 구성요소 점수, 전체 점수, Skill Matrix, 증거 참조, 신뢰도, 완전성, warning/error, 실행 trace를 포함한다.

## 18. 비즈니스 규칙

| ID | 규칙 |
|---|---|
| BR-RE-001 | Rule Engine은 점수를 계산할 수 있는 유일한 구성요소이다. |
| BR-RE-002 | Rule Engine은 LLM을 호출해서는 안 된다. |
| BR-RE-003 | Rule Engine은 프롬프트 로직을 포함해서는 안 된다. |
| BR-RE-004 | Rule Engine은 사용자 대상 코칭 문구를 생성해서는 안 된다. |
| BR-RE-005 | 누락된 provider 데이터를 근거 없이 보완해서는 안 된다. |
| BR-RE-006 | 모든 점수는 규칙 버전, 공식, 가중치, 증거를 추적할 수 있어야 한다. |
| BR-RE-007 | 동일 입력과 동일 규칙 버전은 동일 출력을 생성해야 한다. |
| BR-RE-008 | 누락 데이터는 명시적으로 표현해야 한다. |
| BR-RE-009 | 커리어/회사 로직은 결정적 가중치와 threshold 설정으로 구현한다. |
| BR-RE-010 | 규칙 변경은 버전 관리되고 감사 가능해야 한다. |

## 19. 기능 요구사항

### RR-001 — 언어 분석

| 항목 | 명세 |
|---|---|
| 설명 | Rule Engine은 저장소 언어 통계와 파일 증거를 기반으로 주요 언어, 보조 언어, 언어 다양성, 커리어 관련성을 결정적으로 분석해야 한다. |
| 입력 | 언어 통계, 파일 확장자, 의존성 manifest, 저장소 메타데이터 |
| 출력 | 언어 신호, 언어 점수, 증거 참조, 신뢰도 |
| 비즈니스 규칙 | 언어 점수는 Rule Engine에서만 산출되며 AI가 추론해서는 안 된다. |
| 검증 규칙 | 언어명은 기술 taxonomy에 매핑되거나 unsupported 상태로 표시되어야 한다. |
| 인수 기준 | 동일 입력과 규칙 버전은 동일한 주요 언어와 점수를 반환한다. |
| 의존성 | 정규화된 GitHub 데이터, 기술 taxonomy |

### RR-002 — 프레임워크 분석

| 항목 | 명세 |
|---|---|
| 설명 | Rule Engine은 의존성, 설정 파일, 프로젝트 구조, 프레임워크 관례를 기반으로 프레임워크 사용 증거를 탐지해야 한다. |
| 입력 | dependency manifest, lockfile, 설정 파일, 디렉터리 트리 |
| 출력 | 프레임워크 탐지 결과, 점수, 신뢰도, 증거 ID |
| 비즈니스 규칙 | 프레임워크 인정은 키워드가 아니라 측정 가능한 증거를 요구한다. |
| 검증 규칙 | alias는 canonical taxonomy 항목으로 해석되어야 한다. |
| 인수 기준 | 프레임워크 의존성이 있는 저장소는 증거 경로와 함께 탐지된다. |
| 의존성 | Dependency Normalizer, Directory Normalizer |

### RR-003 — 데이터베이스 분석

| 항목 | 명세 |
|---|---|
| 설명 | Rule Engine은 관계형, NoSQL, 캐시, 벡터 데이터베이스 사용 증거를 탐지하고 통합 깊이를 평가해야 한다. |
| 입력 | 의존성, 설정 메타데이터, README, Notion 문서, 디렉터리 트리 |
| 출력 | 데이터베이스 기술 신호, 점수 구성요소, 증거 참조 |
| 비즈니스 규칙 | 선언된 의존성과 실제 통합 증거를 구분해야 한다. |
| 검증 규칙 | 데이터베이스 기술은 통제된 taxonomy 범주로 매핑되어야 한다. |
| 인수 기준 | PostgreSQL 의존성과 설정이 있으면 데이터베이스 증거가 생성된다. |
| 의존성 | 기술 taxonomy, 문서 분석기 |

### RR-004 — 아키텍처 분석

| 항목 | 명세 |
|---|---|
| 설명 | Rule Engine은 디렉터리 구조, 모듈 경계, 명명 규칙, 설정, 문서를 기반으로 아키텍처 성숙도를 평가해야 한다. |
| 입력 | 디렉터리 트리, 파일 경로, 모듈 구조, README, Notion 설계 문서 |
| 출력 | 아키텍처 점수, 패턴 신호, 증거 ID, 신뢰도 |
| 비즈니스 규칙 | 아키텍처 라벨은 구조적 증거 없이 부여되어서는 안 된다. |
| 검증 규칙 | 디렉터리 증거는 저장소 상대 경로로 정규화되어야 한다. |
| 인수 기준 | 계층 구조와 아키텍처 문서가 있는 저장소는 추적 가능한 아키텍처 신호를 가진다. |
| 의존성 | Directory Normalizer, Documentation Analyzer |

### RR-005 — 테스트 분석

| 항목 | 명세 |
|---|---|
| 설명 | Rule Engine은 단위, 통합, E2E 테스트 신호와 CI 테스트 실행 증거를 평가해야 한다. |
| 입력 | 테스트 파일, 테스트 의존성, CI workflow, README 테스트 지침 |
| 출력 | 테스트 점수, 테스트 유형 신호, 프레임워크 증거, 신뢰도 |
| 비즈니스 규칙 | 명시적 coverage 데이터가 없으면 실제 coverage를 주장해서는 안 된다. |
| 검증 규칙 | 테스트 파일은 언어별 관례와 설정된 패턴으로 식별되어야 한다. |
| 인수 기준 | 테스트 파일과 CI 실행 증거가 있는 저장소는 단순 의존성만 있는 저장소보다 높은 테스트 신호를 가진다. |
| 의존성 | Directory Normalizer, CI workflow 데이터 |

### RR-006 — DevOps 분석

| 항목 | 명세 |
|---|---|
| 설명 | Rule Engine은 Docker, CI/CD, 배포 설정, 인프라 신호, 운영 문서를 평가해야 한다. |
| 입력 | Dockerfile, compose 파일, GitHub Actions, Nginx 설정, 배포 문서 |
| 출력 | DevOps 점수, 배포 준비 신호, 증거 ID |
| 비즈니스 규칙 | 설정 파일 존재와 문서화된 운영 가능성을 구분해야 한다. |
| 검증 규칙 | workflow 파일은 메타데이터로 파싱되고 유효하지 않으면 경고를 생성해야 한다. |
| 인수 기준 | Docker와 CI workflow가 있으면 추적 가능한 DevOps 신호가 생성된다. |
| 의존성 | GitHub workflow collector, Documentation Analyzer |

### RR-007 — 문서화 분석

| 항목 | 명세 |
|---|---|
| 설명 | Rule Engine은 README 완성도, API 문서, 아키텍처 문서, 학습 노트, 프로젝트 노트, 회고를 평가해야 한다. |
| 입력 | README, Notion 페이지, 프로젝트 노트, 학습 노트, 저장소 메타데이터 |
| 출력 | 문서화 점수, 완성도 체크리스트, 증거 참조, 누락 상태 |
| 비즈니스 규칙 | README 누락은 명시적 증거 부재로 표현되어야 한다. |
| 검증 규칙 | 문서 섹션은 설정된 heading과 의미 패턴으로 탐지되어야 한다. |
| 인수 기준 | 개요, 설치, 사용법, 아키텍처, 테스트, 배포 섹션이 있는 README는 제목만 있는 README보다 높은 완성도를 가진다. |
| 의존성 | README collector, Notion collector |

### RR-008 — 협업 분석

| 항목 | 명세 |
|---|---|
| 설명 | Rule Engine은 PR 사용, 리뷰 활동, 이슈 관리, 댓글, 커밋 메시지 품질, 기여자 신호를 평가해야 한다. |
| 입력 | PR, 리뷰, 댓글, 이슈, 라벨, 커밋, 기여자 메타데이터 |
| 출력 | 협업 점수, PR/이슈 지표, 커밋 품질 신호, 신뢰도 |
| 비즈니스 규칙 | 비공개 또는 접근 불가능한 협업 데이터는 부정 추정이 아니라 신뢰도 저하로 처리해야 한다. |
| 검증 규칙 | 기여 타임스탬프와 제공자 ID는 유효해야 한다. |
| 인수 기준 | PR 토론, 리뷰, 이슈 해결 증거가 있는 저장소는 commit-only 저장소보다 강한 협업 신호를 가진다. |
| 의존성 | GitHub PR collector, Issue collector |

### RR-009 — Skill Matrix 생성

| 항목 | 명세 |
|---|---|
| 설명 | Rule Engine은 규칙 출력과 증거를 Skill Matrix 항목으로 변환해야 한다. |
| 입력 | 범주 점수, 규칙 결과, 증거 맵, 커리어/회사 컨텍스트 |
| 출력 | Skill Matrix 항목, 추천 입력 fact |
| 비즈니스 규칙 | Skill Matrix는 결정적 사실만 포함하고 코칭 문구를 포함해서는 안 된다. |
| 검증 규칙 | 0보다 큰 점수를 가진 스킬 항목은 최소 하나의 증거 ID 또는 집계 규칙 결과를 참조해야 한다. |
| 인수 기준 | 생성된 스킬 항목은 점수, 수준, 신뢰도, 증거 ID, 관련 저장소, 규칙 버전을 포함한다. |
| 의존성 | 규칙 결과, Skill taxonomy |

### RR-010 — 전체 점수 계산

| 항목 | 명세 |
|---|---|
| 설명 | Rule Engine은 구성요소 점수, 가중치, 커리어 override, 회사 override를 사용해 전체 점수를 계산해야 한다. |
| 입력 | 범주 점수, 구성요소 점수, 기본/커리어/회사 가중치, 규칙 버전 |
| 출력 | 전체 점수, 유효 가중치, 계산 trace, 신뢰도 |
| 비즈니스 규칙 | 전체 점수는 LLM 또는 downstream AI 구성요소가 계산해서는 안 된다. |
| 검증 규칙 | 유효 가중치와 활성 구성요소가 존재해야 하며 점수 범위는 설정 경계를 유지해야 한다. |
| 인수 기준 | 고정된 구성요소 점수와 가중치 설정에서 전체 점수는 공식 결과와 정확히 일치한다. |
| 의존성 | Score Aggregator, Weight Resolver |


## 20. 비기능 요구사항

| ID | 범주 | 요구사항 | 측정 기준 |
|---|---|---|---|
| RE-NFR-001 | 성능 | 일반적인 사용자 저장소 세트 평가는 설정된 SLA 안에 완료되어야 한다. | 95 백분위 실행 시간이 크기 tier별로 측정된다. |
| RE-NFR-002 | 확장성 | 대형 저장소 세트는 비동기 실행을 지원해야 한다. | 평가 job은 사용자 요청을 차단하지 않고 queue 처리된다. |
| RE-NFR-003 | 유지보수성 | 규칙은 외부 설정과 버전 관리로 운영되어야 한다. | 새 공식 유형이 아닌 비즈니스 변경은 executor 수정 없이 반영된다. |
| RE-NFR-004 | 신뢰성 | 부분 provider 데이터가 전체 평가를 중단시켜서는 안 된다. | 영향받은 규칙은 trace와 함께 `SKIPPED`, `PARTIAL`, `ERROR`로 기록된다. |
| RE-NFR-005 | 가용성 | 선택 데이터 소스가 없어도 가능한 범위에서 graceful degradation을 제공해야 한다. | 출력에 완전성과 신뢰도 indicator가 포함된다. |
| RE-NFR-006 | 설정 가능성 | 가중치, threshold, priority, enabled flag는 설정으로 관리해야 한다. | 관리자 published rule version이 동작을 결정한다. |
| RE-NFR-007 | 확장성 | 신규 범주와 규칙은 catalog extension으로 추가되어야 한다. | 기존 실행 계약은 안정적으로 유지된다. |
| RE-NFR-008 | 관측성 | latency, 실패, skip, coverage 지표가 발행되어야 한다. | 운영자가 지표를 조회할 수 있다. |
| RE-NFR-009 | 로깅 | 규칙 실행 trace는 감사와 debugging을 지원해야 한다. | 모든 평가는 규칙 상태와 계산 메타데이터를 저장한다. |
| RE-NFR-010 | 모니터링 | 실패율, latency spike, coverage drop을 감지해야 한다. | alert threshold는 설정 가능하다. |

## 21. 오류 처리

| 시나리오 | 처리 |
|---|---|
| GitHub API 실패 | 가능한 경우 마지막 정상 스냅샷을 사용하고, 없으면 GitHub 의존 규칙을 skip한다. |
| Notion API 실패 | Notion 증거를 unavailable로 표시하고 GitHub 기반 규칙은 계속 실행한다. |
| 빈 저장소 | empty repository 신호와 낮은 신뢰도를 생성한다. |
| 비공개 저장소 | 권한 있는 데이터만 평가하고 접근 불가능 데이터는 신뢰도에 반영한다. |
| README 누락 | 문서화 증거 부재로 기록한다. |
| 커밋 누락 | 활동과 성장 규칙을 incomplete로 처리한다. |
| 손상 데이터 | 영향받은 레코드를 거부하고 dependent rule을 skip한다. |
| 미지원 언어 | 원본 label을 보존하고 unsupported taxonomy 상태로 기록한다. |

## 22. 로깅

평가 lifecycle, 규칙 실행, 설정 해석, 데이터 완전성, 점수 집계, 보안/감사 이벤트를 기록한다. 로그에는 OAuth token, secret, 대용량 원문 콘텐츠를 저장하지 않는다.

## 23. 모니터링

평가 수, latency, 규칙 실패율, skip 비율, 범주 coverage, catalog load failure, 점수 분포, 신뢰도 분포를 모니터링한다.

## 24. 확장성

신규 평가 범주, 기술 taxonomy, 커리어, 회사, 공식 유형, 증거 소스는 버전 관리된 catalog와 adapter를 통해 확장한다. 확장은 LLM 의존성을 도입하거나 버전 관리를 우회해서는 안 된다.

## 25. 향후 개선사항

향후 rule simulation, publish 전 영향 분석, 증거 최신성 가중치, repository type 분류, benchmark reporting, rule coverage reporting, 안전한 이력 재계산을 추가할 수 있다. 모든 개선은 Rule Engine이 결정적 구조화 평가 결과만 계산한다는 원칙을 유지해야 한다.
