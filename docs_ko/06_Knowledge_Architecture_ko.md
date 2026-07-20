<!--
한국어판 문서입니다. 원본 기준 문서: docs/06_Knowledge_Architecture.md
요구사항 ID, 엔진명, 기술명, 스키마명은 추적성을 위해 필요한 경우 원문 표기를 유지합니다.
-->

# DevPath Knowledge Architecture

- **문서 ID:** DevPath-ARCH-KA-001
- **버전:** 1.0
- **상태:** 초안
- **관련 문서:** `docs/00_Project_Context.md`, `docs/01_SRS.md`, `docs/02_Rule_Engine.md`, `docs/03_Career_Path_Engine.md`, `docs/04_AI_Architecture.md`, `docs/05_Prompt_Engineering.md`
- **작성일:** 2026-07-20

## 1. 목적

Knowledge Architecture는 DevPath의 AI memory, semantic retrieval, knowledge management 구성요소를 정의한다. LLM은 사용자 이력을 장기 기억해서는 안 되며, 모든 장기 개발자 지식은 Knowledge Base에 저장되고 필요한 시점에 권한과 relevance에 따라 검색된다.

## 2. 범위

범위에는 data source, collection, normalization, metadata extraction, chunking, embedding, vector storage, indexing, retrieval, context assembly, freshness, synchronization, versioning, security, privacy, logging, monitoring, requirements, future extension이 포함된다. 응답 생성, score 계산, business rule 실행은 범위 밖이다.

## 3. Knowledge Architecture 원칙

| 원칙 | 설명 |
|---|---|
| 저장된 지식 | LLM memory가 아니라 관리되는 지식 저장소를 장기 기억으로 사용한다. |
| evidence-based retrieval | 모든 검색 결과는 source object와 metadata를 참조해야 한다. |
| user isolation | 사용자 지식은 사용자별 권한 경계로 격리한다. |
| source traceability | 모든 object와 chunk는 source, source ID, ingestion version을 보존한다. |
| freshness-aware retrieval | 최신성과 sync 상태를 retrieval ranking과 warning에 반영한다. |
| hybrid readiness | semantic, keyword, metadata, time-aware retrieval을 함께 지원한다. |

## 4. 전체 아키텍처

Knowledge Source Adapter → Ingestion Orchestrator → Knowledge Object Builder → Metadata Extractor → Chunk Builder → Embedding Service → Vector/Keyword/Metadata Index → Retrieval Orchestrator → Context Result Assembler 순으로 처리한다.

## 5. 데이터 소스

GitHub 저장소, commit, branch, issue, PR, release, README, source code metadata, directory structure, dependencies를 수집한다. Notion learning note, retrospective, project document, architecture document를 수집한다. Portfolio, resume, generated report도 지식 객체로 색인할 수 있다. Jira, Slack, Figma, Blog는 향후 확장 source이다.

## 6. 지식 수집

source event 감지, permission check, normalized record intake, knowledge object mapping, metadata extraction, chunking, embedding, indexing, freshness update, audit/metrics 순서로 수집한다.

## 7. 데이터 정규화

source ID, normalized object ID, user ID, timestamp, markdown/plain text, path, technology taxonomy, category, empty state, duplicate를 일관되게 정규화한다.

## 8. 메타데이터 추출

metadata는 knowledge object ID, chunk ID, user ID, source, source object ID, repository ID, technology, created/updated/synced/indexed date, confidence, evidence, tags, category, career, company, visibility, version을 포함한다.

## 9. 청킹 전략

README는 section 단위, Notion 문서는 heading hierarchy 단위, learning note는 topic/date 단위, architecture 문서는 component/decision 단위, commit은 time window/topic group 단위로 chunking한다. 일반 target size는 250~1,000 token이며 50~150 token overlap을 사용할 수 있다.

## 10. 임베딩 전략

embedding은 권한 있는 sanitized chunk에 대해서만 생성한다. local provider는 privacy-sensitive indexing에 사용하고 hosted provider는 정책이 허용할 때만 사용한다. embedding metadata는 embedding ID, chunk ID, provider, model, version, dimension, content hash, created date, refresh reason을 포함한다.

## 11. 벡터 저장소

SRS의 baseline은 PostgreSQL + pgvector이다. vector entry는 vector, chunk reference, user scope, source metadata, retrieval metadata, version metadata를 포함한다. 삭제 또는 접근 불가능 chunk는 검색 결과에 반환되어서는 안 된다.

## 12. 인덱스 전략

vector index, keyword index, metadata index, freshness index, permission index, artifact index를 운영한다. full build, incremental update, delete propagation, user/source/version 단위 rebuild를 지원한다.

## 13. 검색 전략

semantic search, metadata filtering, hybrid search, similarity search, career-aware retrieval, company-aware retrieval, time-aware retrieval을 지원한다. ranking은 semantic similarity, keyword match, metadata match, freshness, confidence, evidence strength, user scope를 사용한다.

## 14. 컨텍스트 조립

검색 결과는 top-ranked authorized chunk 선택, 중복 제거, source ID 보존, evidence-bearing chunk 우선, compression, task/career/company/token budget 우선순위 반영 후 Context Builder 또는 Search Service에 전달한다.

## 15. 지식 최신성

source freshness, sync freshness, index freshness, embedding freshness, retrieval freshness를 관리한다. 상태는 fresh, stale, partial, deleted, unknown으로 구분한다.

## 16. 동기화

full sync, incremental sync, artifact sync, metadata-only sync, reindex sync를 지원한다. repository 변경은 metadata refresh, chunk update, embedding refresh, delete propagation, freshness update를 유발한다.

## 17. 버전 관리

knowledge object, chunk, embedding, metadata extraction, index, retrieval policy는 변경 시 versioning한다. retrieval result는 source object version, chunk version, embedding version, index version, retrieval policy version으로 추적 가능해야 한다.

## 18. 보안

user isolation, repository permission, private repository protection, encryption, access control, deletion enforcement, audit logging을 적용한다. permission filtering은 candidate retrieval 전후에 모두 수행한다.

## 19. 프라이버시

data minimization, sensitive content filtering, provider-aware embedding, user consent, right to deletion을 적용한다. LLM은 memory store가 아니며 관련 이력은 Knowledge Base에서 임시 context로 검색된다.

## 20. 로깅

ingestion, normalization, chunking, embedding, indexing, retrieval, security, freshness event를 기록한다. 로그에는 secret, credential, full private content를 저장하지 않는다.

## 21. 모니터링

ingestion throughput, failure rate, chunk count, embedding latency/failure, vector index size, retrieval latency, zero-result rate, stale knowledge count, permission rejection, rebuild duration을 모니터링한다.

## 22. 기능 요구사항

### KR-001 — 지식 소스 수집

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 지식 소스 수집 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | 지식 소스 수집 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-002 — Knowledge Object 생성

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Knowledge Object 생성 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Knowledge Object 생성 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-003 — 메타데이터 추출

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 메타데이터 추출 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | 메타데이터 추출 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-004 — Chunk 생성

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Chunk 생성 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Chunk 생성 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-005 — Embedding 생성

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Embedding 생성 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Embedding 생성 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-006 — Vector 저장

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Vector 저장 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Vector 저장 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-007 — Keyword/Metadata indexing

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Keyword/Metadata indexing 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Keyword/Metadata indexing 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-008 — Semantic retrieval

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Semantic retrieval 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Semantic retrieval 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-009 — Hybrid retrieval

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Hybrid retrieval 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Hybrid retrieval 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-010 — Career-aware retrieval

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Career-aware retrieval 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Career-aware retrieval 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-011 — Company-aware retrieval

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Company-aware retrieval 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Company-aware retrieval 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-012 — Time-aware retrieval

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Time-aware retrieval 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Time-aware retrieval 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-013 — Context result assembly

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Context result assembly 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Context result assembly 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-014 — Knowledge freshness tracking

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Knowledge freshness tracking 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Knowledge freshness tracking 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-015 — Incremental synchronization

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Incremental synchronization 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Incremental synchronization 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-016 — Deleted data handling

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Deleted data handling 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Deleted data handling 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-017 — Permission enforcement

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Permission enforcement 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Permission enforcement 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-018 — Index rebuild

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Index rebuild 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Index rebuild 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-019 — Embedding provider management

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Embedding provider management 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Embedding provider management 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |

### KR-020 — Retrieval audit logging

| 항목 | 명세 |
|---|---|
| 설명 | Knowledge System은 Retrieval audit logging 기능을 권한과 metadata 정책에 따라 수행해야 한다. |
| 입력 | 정규화 source record, user scope, metadata, policy |
| 출력 | knowledge object, chunk, embedding, index entry, retrieval result 또는 audit event |
| 비즈니스 규칙 | Knowledge System은 응답 생성, score 계산, business logic 실행을 하지 않는다. |
| 검증 규칙 | source ID, user ID, permission, version, metadata 필드는 유효해야 한다. |
| 인수 기준 | Retrieval audit logging 작업은 권한 있는 범위에서 재현 가능하고 trace 가능하게 처리된다. |
| 의존성 | Data Collection, Normalizer, Vector Store, Index Manager, Security Gate |


## 23. 비기능 요구사항

| ID | 범주 | 요구사항 | 측정 기준 |
|---|---|---|---|
| KA-NFR-001 | 성능 | 검색은 일반 search/RAG context task의 latency 목표를 만족해야 한다. | retrieval method별 95 백분위 latency를 측정한다. |
| KA-NFR-002 | 가용성 | optional embedding provider 장애 시에도 기존 index 기반 검색은 가능해야 한다. | degradation mode를 테스트한다. |
| KA-NFR-003 | 확장성 | repository, chunk, embedding, artifact 증가를 지원해야 한다. | storage와 index growth metric을 모니터링한다. |
| KA-NFR-004 | 신뢰성 | embedding/indexing 실패는 retry 가능하고 trace 가능해야 한다. | retry 상태와 실패 사유가 저장된다. |
| KA-NFR-005 | 보안 | user isolation과 private repository protection을 storage/retrieval layer에서 적용해야 한다. | cross-user retrieval 방지 테스트를 통과한다. |
| KA-NFR-006 | 프라이버시 | secret과 민감 데이터는 embedding되거나 retrieval로 노출되어서는 안 된다. | redaction test와 embedding payload audit을 통과한다. |

## 24. 향후 확장

Jira, Slack, Figma, Blog ingestion, knowledge graph, advanced hybrid ranking, artifact lineage, learning progress retrieval, enterprise knowledge space, embedding migration tooling, retrieval quality benchmark를 추가할 수 있다. 모든 확장은 Knowledge System이 검색만 수행한다는 제약을 유지해야 한다.
