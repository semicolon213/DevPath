<!--
한국어판 문서입니다. 원본 기준 문서: docs/05_Prompt_Engineering.md
요구사항 ID, 엔진명, 기술명, 스키마명은 추적성을 위해 필요한 경우 원문 표기를 유지합니다.
-->

# DevPath Prompt Engineering 아키텍처

- **문서 ID:** DevPath-ARCH-PE-001
- **버전:** 1.0
- **상태:** 초안
- **관련 문서:** `docs/00_Project_Context.md`, `docs/01_SRS.md`, `docs/02_Rule_Engine.md`, `docs/03_Career_Path_Engine.md`, `docs/04_AI_Architecture.md`
- **작성일:** 2026-07-20

## 1. 목적

Prompt Engineering layer는 Rule Engine, Career Path Engine, Context Builder의 구조화 출력을 고품질 LLM prompt package로 변환하는 방식을 정의한다. 이 문서는 실제 prompt 문구를 작성하지 않고, prompt를 설계, 조합, versioning, 검증, 관리하는 공식 규격을 정의한다.

## 2. 범위

범위에는 원칙, prompt architecture, composition pipeline, context assembly strategy, template, variable, metadata, versioning, category, validation, optimization, token budget, security, logging, monitoring, error handling, requirements, future extension이 포함된다.

## 3. Prompt Engineering 원칙

Prompt Builder는 조립만 수행한다. 입력은 구조화되어야 하고, template은 versioning되어야 하며, provider-neutral package로 구성되어야 한다. system/safety constraint는 모든 content보다 우선한다. prompt는 LLM 호출 전에 반드시 검증되어야 한다.

## 4. 프롬프트 아키텍처

| 구성요소 | 책임 |
|---|---|
| Prompt Request Resolver | task type, schema, language, tone, provider policy를 해석한다. |
| Template Selector | task/category/career/company/language/schema에 맞는 template을 선택한다. |
| Variable Binder | 검증된 변수를 template slot에 주입한다. |
| Prompt Composer | component 우선순위에 따라 prompt package를 조립한다. |
| Prompt Validator | completeness, safety, token budget, compatibility를 검증한다. |
| Token Estimator | provider 호출 전 token 사용량을 추정한다. |
| Prompt Metadata Builder | template version, context hash, schema ID를 부여한다. |

## 5. 프롬프트 조합 파이프라인

Rule Engine → Career Engine → Context Builder → Prompt Builder → Prompt Validator → LLM → Response Validator 순으로 동작한다. Prompt Builder는 score 계산, rule 실행, career 평가를 요청하지 않는다.

## 6. 컨텍스트 조립 전략

GitHub, Notion, Skill Matrix, career readiness, company readiness, learning roadmap, repository metadata, project history, technology stack, evaluation results에서 task와 관련 있는 정보만 선택한다. 선택 기준은 task relevance, schema field, source priority, evidence strength, confidence, scope, career/company target, token budget, privacy constraint이다.

## 7. 프롬프트 템플릿

Template은 header, instruction block, constraint block, context block, evidence block, output format block, safety block, compatibility block으로 구성한다. system, task, role, constraint, output format, evidence, safety, optional few-shot component를 지원한다.

## 8. 프롬프트 변수

변수는 career, company, repository, technologies, weak skills, strong skills, learning goals, target output, evidence references, output language, tone policy, length policy 등을 포함한다. 모든 변수는 type, source provenance, sanitization, token estimate, template compatibility를 검증해야 한다.

## 9. 프롬프트 메타데이터

Prompt Package ID, task type, template IDs, template versions, context package ID, context hash, output schema ID, safety policy ID, token estimate, provider compatibility, created timestamp, validation result를 저장한다.

## 10. 프롬프트 버전 관리

Template, variable schema, output schema, safety policy, composition policy, token policy는 변경 시 versioning한다. deprecated template은 신규 prompt에 사용하지 않고 과거 traceability를 위해 보존한다.

## 11. 프롬프트 범주

Repository Review, Skill Analysis, Career Coaching, Portfolio Generation, Resume Generation, README Generation, Interview Question Generation, Learning Recommendation, Technology Recommendation, Architecture Review, Project Explanation template category를 지원한다.

## 12. 프롬프트 검증

prompt completeness, missing variables, unsupported context, token overflow, invalid template, unsafe instruction, injection risk, output schema를 검증한다. 실패한 prompt는 LLM provider로 전송하지 않는다.

## 13. 프롬프트 최적화

template modularity, variable compaction, evidence selection, schema-first prompting, task-specific constraint, provider-aware formatting, prompt regression testing을 사용한다. 최적화는 business fact를 변경하지 않는다.

## 14. 토큰 예산 전략

system/safety constraint, output schema, task instruction, required context, evidence reference, response reserve는 필수 budget으로 보존한다. optional context와 few-shot은 budget 부족 시 제거할 수 있다.

## 15. 컨텍스트 우선순위화

P0는 safety, task type, schema, source ID이다. P1은 career/company fact, Skill Matrix, gap, recommendation이다. P2는 repository evidence, technology stack, roadmap, confidence이다. P3/P4는 budget에 따라 포함한다.

## 16. 프롬프트 보안

prompt injection, secret leakage, system prompt override, business logic injection, data overexposure, unsafe template activation을 방어한다. repository, Notion, user text는 instruction이 아니라 delimited data로 취급한다.

## 17. 프롬프트 로깅

prompt requested, template selected, variables bound, token estimated, prompt validated, prompt dispatched event를 redaction된 metadata로 기록한다.

## 18. 모니터링

prompt build count, validation failure rate, missing variable rate, token overflow rate, template usage distribution, generation latency, provider rejection, response validation failure를 모니터링한다.

## 19. 오류 처리

missing context, prompt generation failure, missing variables, unsupported template, context overflow, token overflow, LLM prompt rejection, invalid template version, unsafe instruction은 정책에 따라 reject, fallback, compression, fatal 처리한다.

## 20. 기능 요구사항

### PR-001 — 시스템 프롬프트 관리

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 시스템 프롬프트 관리 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 시스템 프롬프트 관리 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-002 — 커리어 프롬프트 관리

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 커리어 프롬프트 관리 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 커리어 프롬프트 관리 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-003 — 회사 프롬프트 관리

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 회사 프롬프트 관리 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 회사 프롬프트 관리 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-004 — 규칙 출력 컨텍스트

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 규칙 출력 컨텍스트 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 규칙 출력 컨텍스트 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-005 — 출력 형식 프롬프트 관리

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 출력 형식 프롬프트 관리 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 출력 형식 프롬프트 관리 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-006 — 프롬프트 조합

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 프롬프트 조합 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 프롬프트 조합 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-007 — 변수 바인딩

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 변수 바인딩 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 변수 바인딩 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-008 — 프롬프트 검증

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 프롬프트 검증 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 프롬프트 검증 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-009 — 프롬프트 버전 관리

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 프롬프트 버전 관리 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 프롬프트 버전 관리 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-010 — 토큰 예산 검증

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 토큰 예산 검증 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 토큰 예산 검증 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-011 — 프롬프트 보안 적용

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 프롬프트 보안 적용 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 프롬프트 보안 적용 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-012 — 프롬프트 로깅

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 프롬프트 로깅 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 프롬프트 로깅 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-013 — 템플릿 lifecycle 관리

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 템플릿 lifecycle 관리 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 템플릿 lifecycle 관리 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-014 — 프롬프트 최적화

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 프롬프트 최적화 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | 프롬프트 최적화 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |

### PR-015 — provider 호환성

| 항목 | 명세 |
|---|---|
| 설명 | Prompt Engineering layer는 provider 호환성 기능을 versioned template과 structured context 기반으로 수행해야 한다. |
| 입력 | Prompt request, Context Package, Template Catalog, Variable Schema |
| 출력 | 검증된 Prompt Package 또는 validation failure |
| 비즈니스 규칙 | Prompt Builder는 prompt 조립만 수행하고 score 계산과 business logic 실행을 하지 않는다. |
| 검증 규칙 | 필수 template, variable, safety policy, token budget, schema compatibility를 검증해야 한다. |
| 인수 기준 | provider 호환성 처리 결과는 prompt metadata와 trace로 재현 가능해야 한다. |
| 의존성 | Context Builder, Prompt Template Store, Prompt Validator |


## 21. 비기능 요구사항

| ID | 범주 | 요구사항 | 측정 기준 |
|---|---|---|---|
| PE-NFR-001 | 성능 | interactive prompt assembly는 latency 목표를 만족해야 한다. | 95 백분위 build latency를 측정한다. |
| PE-NFR-002 | 확장성 | Prompt Builder는 가능한 stateless composition으로 동시 요청을 처리해야 한다. | 분당 prompt package 처리량을 측정한다. |
| PE-NFR-003 | 유지보수성 | template, schema, policy는 외부화되고 versioning되어야 한다. | 일반 template 변경은 code 변경 없이 반영된다. |
| PE-NFR-004 | 보안 | prompt injection과 secret leakage를 방지해야 한다. | 보안 테스트가 redaction과 hierarchy를 검증한다. |

## 22. 향후 확장

prompt regression suite, RAG-specific component, multilingual template, few-shot catalog, prompt simulation, template diff review, provider-specific packaging profile, prompt injection classifier를 추가할 수 있다. 모든 확장은 Prompt Builder가 조립만 수행한다는 제약을 유지해야 한다.
