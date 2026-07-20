<!--
한국어판 문서입니다. 원본 기준 문서: docs/04_AI_Architecture.md
요구사항 ID, 엔진명, 기술명, 스키마명은 추적성을 위해 필요한 경우 원문 표기를 유지합니다.
-->

# DevPath AI 아키텍처

- **문서 ID:** DevPath-ARCH-AI-001
- **버전:** 1.0
- **상태:** 초안
- **관련 문서:** `docs/00_Project_Context.md`, `docs/01_SRS.md`, `docs/02_Rule_Engine.md`, `docs/03_Career_Path_Engine.md`
- **작성일:** 2026-07-20

## 1. 목적

AI Architecture는 Rule Engine과 Career Path Engine의 구조화 출력을 사용자 친화적인 설명, 추천 문구, 커리어 산출물로 변환하는 AI 계층을 정의한다. AI는 점수를 계산하지 않고, 비즈니스 규칙을 실행하지 않으며, 커리어 평가나 추천 우선순위를 결정하지 않는다.

## 2. 범위

범위에는 AI 처리 파이프라인, 입력/출력 모델, context assembly, Prompt Builder, prompt template 관리, AI task type, response generation/validation, hallucination 방지, token 최적화, model selection, 보안, 프라이버시, 로깅, 모니터링, 오류 처리가 포함된다. 구현 코드, API, ERD, 실제 prompt 문구는 범위 밖이다.

## 3. AI 설계 원칙

| 원칙 | 설명 |
|---|---|
| 점수 계산 금지 | AI는 score를 생성, 수정, 추론, 재계산하지 않는다. |
| 비즈니스 규칙 실행 금지 | 규칙, weight, readiness, priority 로직은 Rule/Career Engine에 속한다. |
| grounded generation | 모든 응답은 구조화 context와 evidence reference에 근거해야 한다. |
| structured-first | 입력과 출력은 task object와 schema를 사용한다. |
| provider abstraction | provider adapter를 통해 여러 LLM을 지원한다. |
| prompt versioning | 모든 출력은 prompt version과 template ID를 기록한다. |
| privacy by design | prompt 조립 전에 secret과 민감 데이터를 제거한다. |

## 4. AI 아키텍처 개요

| 구성요소 | 책임 |
|---|---|
| AI Invocation Facade | AI task 요청, 권한, source result reference를 검증한다. |
| Context Builder | task에 필요한 안전한 context를 조립한다. |
| Evidence Selector | 관련 evidence ID와 요약을 선택한다. |
| Privacy Filter | secret, token, 민감 값을 제거한다. |
| Context Compressor | score를 변경하지 않고 context를 압축한다. |
| Prompt Builder | versioned prompt component를 조합한다. |
| Model Router | task, privacy, cost, latency 기준으로 provider/model을 선택한다. |
| Provider Adapter | provider별 request/response 차이를 캡슐화한다. |
| Response Validator | schema, grounding, score policy, safety를 검증한다. |
| Output Formatter | 검증된 응답을 구조화 output으로 변환한다. |

## 5. AI 처리 파이프라인

Rule Engine → Career Engine → Context Builder → Prompt Builder → LLM → Response Validator → Output Formatter 순으로 처리한다. required context가 없거나 응답이 score를 invent하면 성공 출력으로 저장하지 않는다.

## 6. 입력 모델

AI Task Request는 task ID, user ID, task type, Rule Evaluation Result ID, Career Evaluation Result ID, repository scope, output language, tone policy, length policy, model policy를 포함한다.

## 7. 컨텍스트 조립

Context Builder는 GitHub 분석, Notion 분석, Skill Matrix, career readiness, company readiness, learning roadmap, project history, repository metadata를 task relevance와 token budget에 따라 선택한다. 점수, readiness, priority는 변경하지 않는다.

## 8. Prompt Builder 아키텍처

Prompt Builder는 system, task, career, company, repository, learning, interview, portfolio, README, resume, output format component를 우선순위에 따라 조합한다. 실제 prompt 문구는 template catalog에서 관리한다.

## 9. 프롬프트 템플릿 관리

Template은 ID, type, version, status, priority, required variables, output schema ID, safety policy ID, supported tasks, change reason을 가진다. 상태는 draft, active, deprecated, archived로 관리한다.

## 10. AI 작업 유형

Repository Review, Skill Explanation, Career Coaching, Portfolio Writing, Resume Writing, README Improvement, Project Description, Interview Question Generation, Learning Recommendation, Technology Recommendation, Architecture Review를 지원한다.

## 11. 응답 생성

응답은 provider raw response, parsed response, schema validation, grounding validation, policy validation, formatting, persistence lifecycle을 거친다. retry는 timeout, transient failure, schema 오류에 한해 제한적으로 수행한다.

## 12. 출력 모델

AI output envelope는 AI Output ID, task type, user ID, source result ID, prompt version, model provider/name, context hash, validation status, output payload, evidence references, safety flags, timestamp를 포함한다.

## 13. 환각 방지

grounded context, evidence reference, confidence score, response validation, unsupported question detection, context verification, score guardrail, claim classification을 적용한다.

## 14. 토큰 최적화

context scoping, evidence summarization, priority ranking, deduplication, structured compression, chunked generation, cache reuse를 사용한다. system safety, output schema, required facts, evidence references, response reserve는 우선 보존한다.

## 15. 컨텍스트 윈도우 전략

작업을 small, medium, large context tier로 분류한다. 큰 작업은 repository, skill category, roadmap milestone, artifact section, time window 단위로 나눌 수 있다.

## 16. 모델 선택 전략

Ollama는 SRS 활성 local option이고 OpenAI는 optional hosted option이다. Anthropic, Gemini, Qwen, Llama, Mistral은 adapter 기반 확장 후보이다. 선택 기준은 task type, privacy tier, context size, cost, latency, availability, schema support이다.

## 17. AI 설정

provider configuration, model policy, prompt policy, context policy, validation policy, cache policy, logging policy를 versioned configuration으로 관리한다.

## 18. 보안

접근 제어, provider credential 보호, prompt injection 방어, output validation, audit logging, network policy를 적용한다. README, Notion, commit message, 사용자 입력은 instruction이 아니라 data로 취급한다.

## 19. 프라이버시

data minimization, redaction, provider-aware routing, user ownership, traceability를 적용한다. OAuth token, API key, password, private key, secret 환경 변수는 prompt에 포함하지 않는다.

## 20. 프롬프트 버전 관리

system, career, company, repository, learning, interview, portfolio, README, resume, output format prompt는 변경 시 versioning한다. AI 출력은 template ID/version, schema version, safety policy version, context/model policy version을 저장한다.

## 21. AI 로깅

task lifecycle, context assembly, prompt composition, model invocation, validation, output persistence, error handling을 redaction된 metadata로 기록한다.

## 22. 모니터링

AI task count, provider latency, token usage, validation failure rate, hallucination rejection rate, fallback rate, rate-limit rate, cache hit rate, redaction count를 모니터링한다.

## 23. 오류 처리

LLM timeout, unavailable, token limit exceeded, missing context, prompt generation failure, model switching, rate limit, invalid response, ungrounded claim, privacy filter failure를 정책에 따라 retry, fallback, reject, fatal 처리한다.

## 24. 기능 요구사항

### AI-001 — Prompt Builder

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 Prompt Builder 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | Prompt Builder 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-002 — 저장소 요약

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 저장소 요약 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 저장소 요약 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-003 — 스킬 분석 설명

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 스킬 분석 설명 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 스킬 분석 설명 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-004 — 저장소 리뷰

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 저장소 리뷰 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 저장소 리뷰 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-005 — 포트폴리오 생성

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 포트폴리오 생성 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 포트폴리오 생성 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-006 — 이력서 생성

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 이력서 생성 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 이력서 생성 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-007 — 면접 질문 생성

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 면접 질문 생성 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 면접 질문 생성 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-008 — 학습 플래너

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 학습 플래너 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 학습 플래너 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-009 — README 개선

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 README 개선 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | README 개선 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-010 — 커리어 코칭

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 커리어 코칭 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 커리어 코칭 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-011 — 기술 설명

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 기술 설명 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 기술 설명 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-012 — 컨텍스트 조립

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 컨텍스트 조립 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 컨텍스트 조립 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-013 — 응답 검증

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 응답 검증 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 응답 검증 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-014 — 모델 라우팅

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 모델 라우팅 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | 모델 라우팅 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |

### AI-015 — AI 출력 저장

| 항목 | 명세 |
|---|---|
| 설명 | AI layer는 AI 출력 저장 작업을 구조화된 context와 versioned prompt를 기반으로 수행해야 한다. |
| 입력 | AI task request, Rule Engine 출력, Career Path Engine 출력, Context Package |
| 출력 | 검증된 구조화 AI 출력 객체 |
| 비즈니스 규칙 | AI는 점수 계산, 비즈니스 규칙 실행, readiness 판단을 수행해서는 안 된다. |
| 검증 규칙 | 출력 schema, evidence reference, safety policy를 검증해야 한다. |
| 인수 기준 | AI 출력 저장 결과는 제공된 context에 근거하고 unsupported claim을 포함하지 않는다. |
| 의존성 | Context Builder, Prompt Builder, Response Validator, Model Router |


## 25. 비기능 요구사항

| ID | 범주 | 요구사항 | 측정 기준 |
|---|---|---|---|
| AI-NFR-001 | 성능 | interactive task는 task별 latency 목표를 만족해야 한다. | 95 백분위 latency를 측정한다. |
| AI-NFR-002 | 확장성 | 장시간 생성 작업은 비동기 실행을 지원해야 한다. | portfolio/resume 생성은 queue 처리 가능하다. |
| AI-NFR-003 | 가용성 | 정책이 허용하면 fallback provider를 지원해야 한다. | fallback 성공률을 모니터링한다. |
| AI-NFR-004 | 신뢰성 | invalid response를 안전하게 탐지하고 처리해야 한다. | validation failure rate를 측정한다. |
| AI-NFR-005 | 보안 | provider credential과 source secret은 prompt/log에 노출되지 않아야 한다. | secret scan을 통과한다. |
| AI-NFR-006 | 프라이버시 | context는 최소화하고 privacy tier에 맞게 routing해야 한다. | context package가 privacy metadata를 포함한다. |

## 26. 향후 AI 확장

사용자 승인 문서 기반 RAG index, embedding retrieval, model quality harness, prompt regression test, feedback loop, artifact editor, multilingual profile, local-only privacy mode를 추가할 수 있다. 모든 확장은 AI가 score를 계산하지 않는다는 원칙을 유지해야 한다.
