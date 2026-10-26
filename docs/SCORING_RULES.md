# DevPath 점수 규칙 (Scoring Rules)

이 문서는 DevPath가 공식 결과를 계산하는 결정적 규칙을 정리한다. 대상은 분석 점수(Rule Engine), Skill Matrix, 커리어 준비도, 추천, 학습 로드맵이다.
모든 수치는 Flyway 마이그레이션, 도메인 코드, 골든 fixture와 대조해 확인했다. 문서와 코드가 다르면 코드와 마이그레이션이 기준이다.

관련 문서: [docs/SRS.md](SRS.md) · [docs/ARCHITECTURE.md](ARCHITECTURE.md) · [docs/ADR.md](ADR.md) · [docs/TEST_PLAN.md](TEST_PLAN.md) · [docs/STATUS.md](STATUS.md) · [contracts/openapi/devpath-openapi.yaml](../contracts/openapi/devpath-openapi.yaml)

---

## 1. 원칙

### 1.1 불변 규칙

| 규칙 | 내용 |
|---|---|
| 점수 계산 주체 | 공식 점수, 가중치, 준비도, 추천 우선순위는 결정적 엔진(Rule Engine, 준비도·추천·로드맵 엔진)만 계산한다. |
| AI 금지 | AI는 점수·가중치·준비도·추천 우선순위·순위를 계산, 수정, 추정하지 않는다. AI는 확정된 구조화 결과를 설명만 한다. |
| 재현성 | 같은 입력 스냅샷과 같은 정책 버전은 항상 같은 결과를 낸다. |
| 버전 고정 | 가중치·임계값·매핑이 바뀌면 기존 버전을 고치지 않고 새 정책 버전을 만든다. 이전 결과는 재계산하지 않는다. |
| 추적성 | 모든 결과는 정책 버전, 공식, 가중치, 근거(Evidence ID)를 참조한다. |
| 저장소 | 정책과 결과의 원본은 PostgreSQL이다. 정책 값은 Flyway 마이그레이션으로만 들어간다. |
| 결측 명시 | 근거가 없으면 숨기지 않고 명시적 상태(`FAILED`, `SKIPPED`, `NONE`, `INSUFFICIENT_EVIDENCE`)로 표현한다. |

### 1.2 비즈니스 규칙 ID

| ID | 규칙 |
|---|---|
| BR-RE-001 | Rule Engine만 점수를 계산한다. |
| BR-RE-002 | Rule Engine은 LLM을 호출하지 않는다. |
| BR-RE-003 | Rule Engine에는 프롬프트 로직이 없다. |
| BR-RE-004 | Rule Engine은 사용자용 코칭 문장을 만들지 않는다. |
| BR-RE-005 | 제공자 데이터가 없을 때 근거를 지어내지 않는다. |
| BR-RE-006 | 모든 점수는 규칙 버전, 공식, 가중치, 가능한 근거를 참조한다. |
| BR-RE-007 | 같은 입력 스냅샷과 규칙 버전은 같은 출력을 낸다. |
| BR-RE-008 | 결측 데이터는 명시적으로 표현한다. |
| BR-RE-009 | 커리어·기업 로직은 결정적 가중치·임계값 설정으로만 구현한다. |
| BR-RE-010 | 규칙 변경은 버전으로 관리하고 감사 가능해야 한다. |

### 1.3 계산 흐름

분석 작업 한 건은 다음 순서로 결정적 결과를 만든다. 각 단계는 입력 기준(basis)이 같으면 기존 결과를 재사용한다(멱등).

| 단계 | 결과 | 멱등 기준 |
|---|---|---|
| 1. 규칙 평가 | `evaluations` (총점, 카테고리 점수, 규칙 결과) | `(user_id, snapshot_id, rule_set_version_id, input_hash)` |
| 2. Skill Matrix 생성 | `skill_matrices`, `skill_assessments` | `evaluation_id` 1건당 1개 |
| 3. 커리어 준비도 | `career_readiness_assessments`, `skill_gaps` | `(user_id, skill_matrix_id, career_profile_version_id, career_readiness_policy_id)` |
| 4. 추천 | `recommendation_sets`, `recommendations` | `(user_id, career_readiness_id, recommendation_policy_id)` |
| 5. 로드맵 | `learning_roadmaps`, `roadmap_milestones`, `roadmap_steps` | `(user_id, recommendation_set_id, roadmap_policy_id)` |

- 3단계는 사용자의 목표 커리어가 `backend` 또는 `frontend`일 때만 실행한다. 그 외 커리어나 목표 미설정이면 준비도·추천·로드맵을 만들지 않는다.
- 4·5단계는 준비도 상태가 `COMPLETED`일 때만 실행한다. `INSUFFICIENT_EVIDENCE`이면 추천과 로드맵이 없다.
- 조회 API는 결과를 만들거나 바꾸지 않는다.

### 1.4 공통 반올림

| 항목 | 규칙 |
|---|---|
| 저장 정밀도 | 점수·신뢰도는 소수 둘째 자리 (`NUMERIC(5,2)`, 범위 0~100). |
| 반올림 방식 | `HALF_UP` (사사오입). |
| 중간 나눗셈 | 나눗셈은 소수 8자리 `HALF_UP`으로 계산한 뒤 최종 값을 2자리로 반올림한다. |
| 단계별 반올림 | 규칙 점수 → 카테고리 점수 → 총점 순으로 각 단계에서 2자리로 반올림한 값을 다음 단계 입력으로 쓴다. |

---

## 2. 분석 점수 (Rule Engine)

### 2.1 규칙 세트 버전

| 버전 | 상태 | 마이그레이션 | 공식 라이브러리 | 필요 추출기 버전 | 적용 시점 |
|---|---|---|---|---|---|
| `baseline-v1` | `SUPERSEDED` | V12 (생성), V18 (SUPERSEDED 전환) | `formula-v1` | `engineering-evidence-extractor-v1` | 2026-08-11 |
| `baseline-v2` | `ACTIVE` | V18 | `formula-v1` | `engineering-evidence-extractor-v2` | 2026-08-24 |

- 규칙 세트 범위(scope)는 `REPOSITORY_BASELINE` 하나다. 활성 버전은 `rule_sets.active_rule_set_version_id`가 가리킨다.
- 엔진은 규칙 세트가 요구하는 추출기 버전과 실제 추출기 버전이 다르면 평가를 거부한다.
- 같은 신호 키(signal key)의 사실(fact)이 두 번 들어오면 평가를 거부한다.

### 2.2 공식 (`formula-v1`)

| 공식 | 계산 |
|---|---|
| `PRESENCE` | 근거가 있으면 `100`, 없으면 `0`. |
| `COUNT_CAP` | `min(관측값 / 파라미터 × 100, 100)`. 나눗셈은 소수 8자리. |
| `PERCENTAGE` | 관측 백분율을 `0..100`으로 자른 값. |

모든 규칙 점수는 `max(점수, 0)` 후 소수 둘째 자리 `HALF_UP`으로 반올림한다.

### 2.3 규칙 실행 결과 상태

| 상태 | 조건 |
|---|---|
| `PASSED` | 규칙 점수 = 100 |
| `PARTIAL` | 0 < 규칙 점수 < 100 |
| `FAILED` | 규칙 점수 = 0, 또는 근거가 없고 결측 정책이 `ZERO` |
| `SKIPPED` | 근거가 없고 결측 정책이 `SKIP` (점수 0, 신뢰도에서 제외) |
| `ERROR` | 상태 값으로 정의되어 있으나 현재 엔진은 만들지 않는다. |

결측 정책은 `ZERO`와 `SKIP` 두 가지다. **현재 두 baseline의 모든 규칙은 `ZERO`다.** 따라서 근거가 없으면 해당 규칙은 `FAILED` 0점이 되고, 신뢰도는 낮아지지 않는다.

### 2.4 집계 공식

```text
카테고리 점수   = round2( Σ (규칙 점수 × 규칙 가중치) )            # 카테고리 안의 활성 규칙
카테고리 신뢰도 = round2( Σ (SKIPPED가 아닌 규칙의 가중치) × 100 )
총점            = round2( Σ (카테고리 점수 × 카테고리 가중치) )
총 신뢰도       = round2( Σ (카테고리 신뢰도 × 카테고리 가중치) )
```

- 신뢰도는 점수와 별개로 계산한다. "필요한 근거가 얼마나 확보되었는가"를 뜻하며 점수의 높낮이와 무관하다.
- `SKIPPED` 규칙은 카테고리의 결측 근거(missing evidence) 목록과 경고(`Missing unavailable evidence: <ruleId>`)에 기록된다.
- 규칙은 `priority` 오름차순, 같으면 `rule_id` 오름차순으로 실행한다. 현재 모든 규칙의 `priority`는 `20`이다.
- 카테고리 결과는 카테고리 enum 이름의 알파벳 순서로 정렬한다.
- 비활성(`enabled = false`) 규칙은 평가에서 제외한다.

### 2.5 규칙 세트 검증

규칙 세트를 불러올 때 다음을 만족하지 않으면 거부한다.

- 활성 규칙이 1개 이상이다.
- 카테고리 가중치의 키 집합이 활성 규칙의 카테고리 집합과 같다.
- 카테고리 가중치 합계가 정확히 `1.000000`이고 모두 0보다 크다.
- 카테고리마다 규칙 가중치 합계가 정확히 `1.000000`이고 모두 0보다 크다.
- 규칙 ID가 중복되지 않는다.

### 2.6 `baseline-v1` 카테고리 가중치 (V12)

| 카테고리 | 가중치 |
|---|---:|
| `LANGUAGE` | 0.25 |
| `FRAMEWORK` | 0.15 |
| `TESTING` | 0.25 |
| `DOCUMENTATION` | 0.20 |
| `ACTIVITY` | 0.15 |

`baseline-v1`의 규칙은 아래 2.8의 `baseline-v2` 규칙 중 `LANGUAGE`, `FRAMEWORK`, `TESTING`, `DOCUMENTATION`, `ACTIVITY` 카테고리 규칙과 공식·파라미터·가중치가 같다. 규칙 버전만 `1.0.0`이다(`baseline-v2`는 `2.0.0`).

### 2.7 `baseline-v2` 카테고리 가중치 (V18, 활성)

| 카테고리 | 가중치 |
|---|---:|
| `LANGUAGE` | 0.15 |
| `FRAMEWORK` | 0.15 |
| `DATABASE` | 0.15 |
| `ARCHITECTURE` | 0.15 |
| `TESTING` | 0.15 |
| `DEVOPS` | 0.10 |
| `DOCUMENTATION` | 0.10 |
| `ACTIVITY` | 0.05 |

### 2.8 `baseline-v2` 규칙 (V18)

모든 규칙: 규칙 버전 `2.0.0`, priority `20`, 결측 정책 `ZERO`, 활성.

| 카테고리 | 규칙 ID | 신호 키 | 공식 | 파라미터 | 규칙 가중치 | 의미 |
|---|---|---|---|---:|---:|---|
| LANGUAGE | `LANGUAGE_PRIMARY_SHARE` | `LANGUAGE_PRIMARY_SHARE` | `PERCENTAGE` | 0 | 0.70 | 주 언어 비율 |
| LANGUAGE | `LANGUAGE_DIVERSITY` | `LANGUAGE_DIVERSITY` | `COUNT_CAP` | 3 | 0.30 | 측정 언어 수, 3개에서 만점 |
| FRAMEWORK | `FRAMEWORK_COUNT` | `FRAMEWORK_COUNT` | `COUNT_CAP` | 2 | 1.00 | 정규화된 프레임워크 수, 2개에서 만점 |
| DATABASE | `DATABASE_TECHNOLOGIES` | `DATABASE_TECHNOLOGIES` | `PRESENCE` | 0 | 0.20 | DB 기술 선언 |
| DATABASE | `DATA_ACCESS_DEPENDENCIES` | `DATA_ACCESS_DEPENDENCIES` | `PRESENCE` | 0 | 0.20 | 데이터 접근 의존성 |
| DATABASE | `DATABASE_MIGRATIONS` | `DATABASE_MIGRATIONS` | `PRESENCE` | 0 | 0.35 | 마이그레이션 자산 |
| DATABASE | `PERSISTENCE_CONFIGURATION` | `PERSISTENCE_CONFIGURATION` | `PRESENCE` | 0 | 0.25 | 영속성 설정 |
| ARCHITECTURE | `STRUCTURED_BOUNDARIES` | `STRUCTURED_BOUNDARIES` | `PRESENCE` | 0 | 0.50 | 완전한 헥사고날 또는 계층형 경계 (둘 중 하나) |
| ARCHITECTURE | `MODULE_LAYOUT` | `MODULE_LAYOUT` | `PRESENCE` | 0 | 0.25 | 멀티 모듈 또는 멀티 루트 구조 |
| ARCHITECTURE | `ARCHITECTURE_DOCUMENTATION_STRUCTURE` | `ARCHITECTURE_DOCUMENTATION` | `PRESENCE` | 0 | 0.25 | 아키텍처·결정 문서 경로 |
| TESTING | `TEST_FILES` | `TEST_FILES` | `COUNT_CAP` | 10 | 0.50 | 테스트 파일 수, 10개에서 만점 |
| TESTING | `TEST_FRAMEWORKS` | `TEST_FRAMEWORKS` | `COUNT_CAP` | 2 | 0.30 | 테스트 프레임워크 수, 2개에서 만점 |
| TESTING | `CI_WORKFLOW_METADATA` | `CI_WORKFLOW_METADATA` | `PRESENCE` | 0 | 0.20 | CI 워크플로 메타데이터 |
| DEVOPS | `CONTAINER_CONFIGURATION` | `CONTAINER_CONFIGURATION` | `PRESENCE` | 0 | 0.30 | 컨테이너 빌드·구성 설정 |
| DEVOPS | `CI_WORKFLOWS` | `CI_WORKFLOWS` | `PRESENCE` | 0 | 0.30 | CI 워크플로 설정 |
| DEVOPS | `INFRASTRUCTURE_AS_CODE` | `INFRASTRUCTURE_AS_CODE` | `PRESENCE` | 0 | 0.20 | IaC 자산 |
| DEVOPS | `DEPLOYMENT_CONFIGURATION` | `DEPLOYMENT_CONFIGURATION` | `PRESENCE` | 0 | 0.20 | 배포 설정 |
| DOCUMENTATION | `README_PRESENT` | `README_PRESENT` | `PRESENCE` | 0 | 0.20 | README 경로 |
| DOCUMENTATION | `API_DOCUMENTATION` | `API_DOCUMENTATION` | `PRESENCE` | 0 | 0.20 | API 계약·문서 경로 |
| DOCUMENTATION | `ARCHITECTURE_DOCUMENTATION` | `ARCHITECTURE_DOCUMENTATION` | `PRESENCE` | 0 | 0.20 | 아키텍처·결정 문서 경로 |
| DOCUMENTATION | `CONTRIBUTING_GUIDE` | `CONTRIBUTING_GUIDE` | `PRESENCE` | 0 | 0.20 | 기여 가이드 경로 |
| DOCUMENTATION | `LICENSE_PRESENT` | `LICENSE_PRESENT` | `PRESENCE` | 0 | 0.20 | 라이선스 경로 |
| ACTIVITY | `COMMIT_COUNT` | `COMMIT_COUNT` | `COUNT_CAP` | 50 | 0.60 | 수집된 커밋 수, 50개에서 만점 |
| ACTIVITY | `CONTRIBUTOR_COUNT` | `CONTRIBUTOR_COUNT` | `COUNT_CAP` | 3 | 0.20 | 수집된 기여자 수, 3명에서 만점 |
| ACTIVITY | `BRANCH_COUNT` | `BRANCH_COUNT` | `COUNT_CAP` | 3 | 0.20 | 수집된 브랜치 수, 3개에서 만점 |

참고: `ARCHITECTURE_DOCUMENTATION` 신호는 ARCHITECTURE 카테고리(`ARCHITECTURE_DOCUMENTATION_STRUCTURE` 규칙)와 DOCUMENTATION 카테고리(`ARCHITECTURE_DOCUMENTATION` 규칙)에서 함께 쓰인다.

### 2.9 baseline이 주장하지 않는 것

baseline은 저장소 스냅샷에서 확인 가능한 근거만 점수로 만든다. 다음은 측정하지 않는다.

- 실제 코드 커버리지
- README 내용의 완성도(섹션 품질)
- 협업 성숙도(PR 리뷰, 이슈 관리)
- 성장 추세(Growth trend)
- 커리어 준비도, 기업 준비도, 추천 우선순위 (이들은 별도 엔진이 계산한다)

---

## 3. Skill Matrix

Skill Matrix는 Rule Engine의 카테고리 결과를 스킬 단위로 옮긴 것이다. 점수와 신뢰도를 다시 계산하지 않고 카테고리 값을 그대로 복사한다.

### 3.1 정책 버전

| 정책 | 상태 | 바인딩 규칙 세트 | 매핑 스킬 수 | 근거 상태 | 마이그레이션 |
|---|---|---|---:|---|---|
| `skill-matrix-v1` | `SUPERSEDED` | `baseline-v1` | 5 | 없음 | V14 (생성), V18 (SUPERSEDED 전환) |
| `skill-matrix-v2` | `SUPERSEDED` | `baseline-v2` | 8 | 없음 | V18 (생성), V30 (SUPERSEDED 전환) |
| `skill-matrix-v3` | `ACTIVE` | `baseline-v2` | 8 | 최소 근거 2, 신뢰도 60 | V30 |

- 규칙 세트 버전 하나당 `ACTIVE` 정책은 최대 1개다 (V30의 부분 유니크 인덱스 `uq_skill_matrix_policy_active_rule_set`).
- `skill-matrix-v3`는 `skill-matrix-v2`의 레벨 임계값, 강점/약점 임계값, 스킬 매핑을 그대로 복사하고 근거 상태 파라미터만 추가했다.
- 정책의 규칙 세트 버전과 평가의 규칙 세트 버전이 다르면 생성을 거부한다.

### 3.2 스킬 매핑 (카테고리 1:1)

| 스킬 stable key | 이름 | 원천 카테고리 | 도입 |
|---|---|---|---|
| `language-engineering` | Language Engineering | `LANGUAGE` | v1 |
| `framework-application` | Framework Application | `FRAMEWORK` | v1 |
| `testing-discipline` | Testing Discipline | `TESTING` | v1 |
| `technical-documentation` | Technical Documentation | `DOCUMENTATION` | v1 |
| `development-activity` | Development Activity | `ACTIVITY` | v1 |
| `database-engineering` | Database Engineering | `DATABASE` | v2 |
| `architecture-structure` | Architecture Structure | `ARCHITECTURE` | v2 |
| `delivery-operations` | Delivery Operations | `DEVOPS` | v2 |

정책 하나에서 스킬과 카테고리는 각각 한 번씩만 매핑된다.

### 3.3 스킬 레벨 (v1~v3 공통)

| 레벨 | 조건 (점수 s) |
|---|---|
| `NONE` | s = 0 |
| `BEGINNER` | 0 < s < 40 (`beginner_minimum` = 1) |
| `DEVELOPING` | 40 ≤ s < 60 |
| `COMPETENT` | 60 ≤ s < 80 |
| `STRONG` | 80 ≤ s ≤ 100 |

| 플래그 | 조건 |
|---|---|
| 강점(strength) | s ≥ 80 (`strength_minimum`) |
| 약점(weakness) | s ≤ 39.99 (`weakness_maximum`, 0점 포함) |

강점과 약점은 동시에 참일 수 없다. 정책 검증 조건: `beginner_minimum = 1`, 각 레벨 하한이 증가 순서, `strong_minimum ≤ 100`, `weakness_maximum < strength_minimum`.

### 3.4 스킬 평가 항목

| 항목 | 값 |
|---|---|
| 점수 | 카테고리 점수 그대로 |
| 신뢰도 | 카테고리 신뢰도 그대로 |
| 근거 ID | 해당 카테고리 규칙 결과에 연결된 Evidence ID, 중복 제거 후 정렬 |
| 저장소 ID | 평가된 저장소 |
| 성장 추세 | 항상 `UNAVAILABLE` (비교 가능한 이력 계산 미구현) |
| 집계 참조 | `evaluation:<evaluationId>:category:<CATEGORY>` |
| 추천 입력 사실 | `CATEGORY=`, `CATEGORY_SCORE=`, `CONFIDENCE=`, `MISSING_EVIDENCE_COUNT=` 4개 구조화 사실 (문장 아님) |
| 규칙 세트 버전 | 평가의 규칙 세트 버전 |

0점이 아닌 평가는 근거 ID 또는 집계 참조를 반드시 가진다.

### 3.5 근거 상태 (Evidence Status, `skill-matrix-v3`)

입력은 평가 자체의 저장 값이다.

- 근거 수: 평가의 `evidenceIds` 중 서로 다른 Evidence ID 수
- 신뢰도: 평가의 `confidence` (카테고리 신뢰도)

판정은 위에서부터 순서대로 적용한다.

| 순서 | 상태 | 조건 | 화면 의미 |
|---:|---|---|---|
| 1 | `NONE` | 근거 수 = 0 (신뢰도와 무관) | 점수 막대 없이 "근거 없음"과 안내 표시 |
| 2 | `INSUFFICIENT` | 근거 수 < 2, 또는 신뢰도 < 60 | 점수와 함께 "근거 부족" 표시 |
| 3 | `SUFFICIENT` | 그 외 (근거 수 ≥ 2 그리고 신뢰도 ≥ 60) | 일반 평가로 표시 |

- 최소 근거 수 `2`와 신뢰도 임계값 `60`은 정책 컬럼(`evidence_min_count`, `evidence_confidence_threshold`)이며 클라이언트 상수가 아니다. 값을 바꾸려면 새 정책 버전이 필요하다.
- 두 파라미터는 함께 NULL이거나 함께 값이 있어야 한다. 값이 있으면 최소 근거 수 > 0, 임계값 0~100.
- `skill-matrix-v1`, `v2`로 만든 기존 매트릭스는 재계산하지 않는다. 이들의 근거 상태는 `null`이며, 소비자는 "알 수 없음"으로 보고 별도 표시를 하지 않는다.
- AI, 프롬프트 조립, 브라우저는 근거 상태를 계산하거나 덮어쓰지 않는다.
- 현재 baseline의 규칙은 모두 결측 정책 `ZERO`라서 카테고리 신뢰도는 항상 100이다. 따라서 지금은 근거 수만 상태를 가른다. 신뢰도 조건은 `SKIP` 규칙이 도입되면 효력이 생긴다.

### 3.6 매트릭스 수명

- 사용자당 `CURRENT` 매트릭스는 1개다. 새 매트릭스가 생기면 이전 것은 `SUPERSEDED`가 된다.
- 매트릭스와 평가는 생성 후 바뀌지 않는다.

---

## 4. 커리어 준비도 (`readiness-v1`)

### 4.1 정책과 범위

| 항목 | 값 |
|---|---|
| 정책 버전 | `readiness-v1` (`ACTIVE`, V18, 적용 2026-08-24) |
| 대상 커리어 | `backend`, `frontend`만 (애플리케이션 서비스의 MVP 커리어 집합) |
| 커리어 프로필 | `career-v2` (V18, `ACTIVE`). 두 커리어의 `career-v1`(V16)은 `SUPERSEDED` |
| 기업 가중치 | 적용하지 않음 |
| 기대 최소 점수 | 60 (모든 평가 카테고리 공통, DB CHECK로 고정) |
| 입력 | 현재 Skill Matrix, 커리어 프로필 버전, 준비도 정책 버전 |

### 4.2 커리어 가중치 (V18, `career_readiness_weights`)

가중치는 백분율이며 커리어마다 합계가 정확히 100이어야 한다.

| 카테고리 | Backend | Frontend |
|---|---:|---:|
| `LANGUAGE` | 15 | 30 |
| `FRAMEWORK` | 20 | 30 |
| `DATABASE` | 20 | - |
| `ARCHITECTURE` | 15 | - |
| `TESTING` | 20 | 20 |
| `DEVOPS` | 10 | - |
| `DOCUMENTATION` | - | 20 |
| 합계 | 100 | 100 |

`ACTIVITY`는 어느 커리어의 준비도에도 쓰이지 않는다.

### 4.3 갭 상태 (카테고리별)

| 갭 상태 | 조건 (실제 점수 s) |
|---|---|
| `MISSING` | s = 0 |
| `WEAK` | 0 < s < 40 |
| `PARTIAL` | 40 ≤ s < 60 |
| `SUFFICIENT` | 60 ≤ s < 80 |
| `STRONG` | 80 ≤ s ≤ 100 |

정책 임계값: `developing_minimum = 40`, `expected_minimum = 60`, `strong_minimum = 80` (DB CHECK로 고정).

### 4.4 계산 공식

```text
준비도 점수   = round2( Σ round8(카테고리 점수 × 커리어 가중치 / 100) )
준비도 신뢰도 = round2( Σ round8(카테고리 신뢰도 × 커리어 가중치 / 100) )
```

- 카테고리 점수와 신뢰도는 Skill Matrix 평가 값을 그대로 쓴다.
- 점수와 신뢰도는 서로 독립적으로 계산한다.

준비도 레벨:

| 레벨 | 조건 (준비도 점수 r) |
|---|---|
| `NONE` | r = 0 |
| `BEGINNER` | 0 < r < 40 |
| `DEVELOPING` | 40 ≤ r < 60 |
| `COMPETENT` | 60 ≤ r < 80 |
| `STRONG` | 80 ≤ r ≤ 100 |

### 4.5 근거 부족 처리 (`INSUFFICIENT_EVIDENCE`)

- 커리어 가중치에 있는 카테고리가 Skill Matrix에 없으면 상태는 `INSUFFICIENT_EVIDENCE`다. 예: `skill-matrix-v1` 매트릭스(5개 카테고리)로 Backend를 평가하면 `DATABASE`, `ARCHITECTURE`, `DEVOPS`가 없다.
- 이때 준비도 점수와 레벨은 비어 있고(DB CHECK로 강제), `unavailable_categories`에 빠진 카테고리를 이름순으로 기록한다.
- 신뢰도와 확인 가능한 카테고리의 갭은 그대로 계산·저장한다.
- `INSUFFICIENT_EVIDENCE` 결과는 추천과 로드맵을 만들지 않는다.

### 4.6 갭 정렬

1. 갭 상태 심각도: `MISSING` → `WEAK` → `PARTIAL` → `SUFFICIENT` → `STRONG`
2. 커리어 가중치 내림차순
3. 카테고리 키 오름차순

각 갭은 실제 점수, 실제 스킬 레벨, 기대 최소 점수(60), 커리어 가중치, 평가의 Evidence ID를 함께 저장한다. 갭 ID는 `준비도 ID:스킬 평가 ID`의 이름 기반 UUID다.

### 4.7 계산 예시 (산술 확인용)

Frontend, 카테고리 점수 Language 69, Framework 100, Testing 75, Documentation 80인 경우:

```text
69×0.30 + 100×0.30 + 75×0.20 + 80×0.20 = 20.70 + 30.00 + 15.00 + 16.00 = 81.70 → STRONG
갭(정렬 순서): LANGUAGE SUFFICIENT, TESTING SUFFICIENT, FRAMEWORK STRONG, DOCUMENTATION STRONG → 추천 0건
```

`readiness-v1`은 추천 우선순위를 계산하지 않는다. 우선순위는 `recommendation-v1`이 정한다.

---

## 5. 추천 (`recommendation-v1`)

### 5.1 정책과 범위

| 항목 | 값 |
|---|---|
| 정책 버전 | `recommendation-v1` (`ACTIVE`, V19, 적용 2026-08-25) |
| 입력 | `COMPLETED` 상태의 `readiness-v1` 결과와 그 갭 |
| 대상 커리어 | Backend, Frontend (`career-v2` 프로필 템플릿만 존재) |
| 기업 보정 | 적용하지 않음 |
| AI 문장 | 포함하지 않음. 제목·완료 기준·기대 근거는 정책 템플릿 문자열 |

### 5.2 생성 규칙

- `MISSING`, `WEAK`, `PARTIAL` 갭마다 추천 1건을 만든다. `SUFFICIENT`, `STRONG` 갭은 추천을 만들지 않는다.
- 갭 카테고리에 맞는 템플릿이 없으면 생성을 거부한다.
- 모든 템플릿의 근거 코드(rationale code)는 `CAREER_REQUIRED_GAP`이다.
- 추천은 갭의 Evidence ID를 그대로 연결한다.
- 추천 ID는 `추천 세트 ID:갭 ID`의 이름 기반 UUID다.

| 갭 상태 | 우선순위 |
|---|---|
| `MISSING` | `CRITICAL` |
| `WEAK` | `HIGH` |
| `PARTIAL` | `MEDIUM` |
| (없음) | `LOW` — 향후 선택 역량용으로 예약, 현재 생성되지 않음 |

### 5.3 정렬 (`position`)

1. 우선순위: `CRITICAL` → `HIGH` → `MEDIUM` → `LOW`
2. 템플릿 `prerequisite_order` 오름차순
3. 커리어 가중치 내림차순
4. 템플릿 `effort_hours` 오름차순
5. 카테고리 키 오름차순

정렬 결과 순서가 `position` 0부터 저장된다.

### 5.4 템플릿 (V19, `recommendation_policy_templates`)

Backend (`career-v2`):

| 카테고리 | 유형 | 선행 순서 | 노력(시간) | 제목 | 기대 근거 |
|---|---|---:|---:|---|---|
| `LANGUAGE` | `STUDY` | 0 | 12 | Strengthen backend language evidence | source files, language statistics |
| `FRAMEWORK` | `PROJECT` | 1 | 20 | Build framework-backed service evidence | framework dependency declaration, framework source structure |
| `DATABASE` | `PROJECT` | 2 | 20 | Add measurable database persistence | database dependency, data-access dependency, migration, persistence configuration |
| `ARCHITECTURE` | `ARCHITECTURE` | 3 | 16 | Establish traceable architecture boundaries | structured boundaries, module layout, architecture documentation |
| `TESTING` | `PROJECT` | 4 | 16 | Add repeatable automated testing | test files, test framework, CI test workflow |
| `DEVOPS` | `PROJECT` | 5 | 16 | Create delivery and deployment evidence | container configuration, CI workflow, deployment configuration |

Frontend (`career-v2`):

| 카테고리 | 유형 | 선행 순서 | 노력(시간) | 제목 | 기대 근거 |
|---|---|---:|---:|---|---|
| `LANGUAGE` | `STUDY` | 0 | 12 | Strengthen frontend language evidence | TypeScript source files, language statistics |
| `FRAMEWORK` | `PROJECT` | 1 | 20 | Build measurable React application evidence | React dependency declaration, component source structure |
| `TESTING` | `PROJECT` | 2 | 16 | Add repeatable frontend testing | component tests, test framework, CI test workflow |
| `DOCUMENTATION` | `PORTFOLIO` | 3 | 8 | Document the frontend project evidence | README, API or architecture documentation, usage evidence |

카테고리 → 유형 매핑 요약: Language → `STUDY`; Framework·Database·Testing·DevOps → `PROJECT`; Architecture → `ARCHITECTURE`; Documentation → `PORTFOLIO`.

모든 템플릿의 완료 기준 문장은 `Reach an official <Category> score of at least 60 in a later analysis.` 형식이다.

### 5.5 세트 수명

- 추천 세트는 `(사용자, 준비도, 정책 버전)`으로 식별되며 생성 후 내용이 바뀌지 않는다.
- 새 세트를 만들 때 사용자의 이전 `PUBLISHED` 세트는 `SUPERSEDED`가 된다.
- 개별 추천의 상태 컬럼(`PROPOSED`, `ACCEPTED`, `DISMISSED`, `COMPLETED`)은 스키마에 있으나, 현재 생성 시 `PROPOSED`만 쓰고 이를 바꾸는 API는 없다.

---

## 6. 로드맵 (`roadmap-v1`)

### 6.1 정책과 범위

| 항목 | 값 |
|---|---|
| 정책 버전 | `roadmap-v1` (`ACTIVE`, V19, 적용 2026-08-25) |
| 연결 추천 정책 | `recommendation-v1` (추천 정책당 로드맵 정책 1개) |
| 입력 | 추천 세트 1개 |

### 6.2 생성 규칙

- 추천 1건마다 마일스톤 1개와 단계(step) 1개를 만든다. 추천이 0건이면 마일스톤과 단계도 0개다.
- 순서 키는 추천 템플릿의 `prerequisite_order`다. 같으면 카테고리 키 오름차순.
  - Backend: Language → Framework → Database → Architecture → Testing → DevOps
  - Frontend: Language → Framework → Testing → Documentation
- 각 단계의 선행 단계는 바로 앞 단계 1개다(첫 단계는 없음). 즉 단계는 일렬로 이어진다.
- 단계는 추천의 제목, 노력 시간, 완료 기준, 기대 근거를 그대로 복사한다.
- 마일스톤과 단계 ID는 `로드맵 ID:milestone|step:추천 ID`의 이름 기반 UUID다.

### 6.3 난이도

| 노력 시간 h | 난이도 |
|---|---|
| h ≤ 12 | `BEGINNER` |
| 12 < h ≤ 24 | `INTERMEDIATE` |
| h > 24 | `ADVANCED` |

현재 템플릿 노력 시간(8, 12, 16, 20)으로는 `BEGINNER`와 `INTERMEDIATE`만 나온다.

### 6.4 초기 상태와 수명

| 항목 | 생성 시 값 |
|---|---|
| 로드맵 상태 | `CREATED` |
| 진행률 | `0.00` |
| 마일스톤 상태 | `PLANNED` |
| 단계 상태 | `NOT_STARTED` |

- 사용자당 활성(`CREATED`, `IN_PROGRESS`) 로드맵은 1개다.
- 로드맵 구조(마일스톤·단계·순서)는 생성 후 바뀌지 않는다.
- 현재 구현된 사용자 상태 변경은 로드맵 보관(`POST /api/v1/learning-roadmaps/{roadmapId}/archive`, `ARCHIVED`)뿐이다.
- 단계 완료 기준("이후 분석에서 공식 카테고리 점수 60 이상")은 저장된 기준 문장이다. 이후 분석 결과로 단계를 자동 완료 처리하거나 진행률을 갱신하는 로직은 없다.

---

## 7. 정책 버전 관리와 재현성

### 7.1 전체 정책 버전

| 종류 | 버전 | 상태 | 마이그레이션 | 비고 |
|---|---|---|---|---|
| 규칙 세트 | `baseline-v1` | `SUPERSEDED` | V12 → V18 | 5개 카테고리, 14개 규칙 |
| 규칙 세트 | `baseline-v2` | `ACTIVE` | V18 | 8개 카테고리, 25개 규칙 |
| 공식 라이브러리 | `formula-v1` | 사용 중 | V12, V18 | `PRESENCE`, `COUNT_CAP`, `PERCENTAGE` |
| 근거 추출기 | `engineering-evidence-extractor-v1` | `baseline-v1` 전용 | V12 | |
| 근거 추출기 | `engineering-evidence-extractor-v2` | `baseline-v2` 전용 | V18 | |
| Skill Matrix | `skill-matrix-v1` | `SUPERSEDED` | V14 → V18 | |
| Skill Matrix | `skill-matrix-v2` | `SUPERSEDED` | V18 → V30 | |
| Skill Matrix | `skill-matrix-v3` | `ACTIVE` | V30 | 근거 상태 도입 |
| 커리어 프로필 | `career-v1` (backend, frontend) | `SUPERSEDED` | V16 → V18 | |
| 커리어 프로필 | `career-v2` (backend, frontend) | `ACTIVE` | V18 | 준비도 가중치 보유 |
| 준비도 | `readiness-v1` | `ACTIVE` | V18 | |
| 추천 | `recommendation-v1` | `ACTIVE` | V19 | |
| 로드맵 | `roadmap-v1` | `ACTIVE` | V19 | |

### 7.2 버전 변경 규칙

- 가중치, 임계값, 공식 파라미터, 매핑, 템플릿을 바꾸려면 새 Flyway 마이그레이션으로 새 버전을 추가하고 이전 버전을 `SUPERSEDED`로 바꾼다. 적용된 마이그레이션은 수정하지 않는다.
- 이전 버전으로 만든 결과는 재계산하지 않고 그대로 보존한다.
- 결과에는 사용한 버전이 함께 저장된다: 평가(규칙 세트·공식 라이브러리·추출기 버전), Skill Matrix(정책 버전·규칙 세트 버전), 준비도(프로필 버전·정책 버전·규칙 세트 버전), 추천 세트(정책 버전), 로드맵(정책 버전).

### 7.3 골든 fixture

| 파일 | 대상 | 검증하는 기대값 |
|---|---|---|
| `fixtures/rule-engine/baseline-v1.json` | `baseline-v1` | 총점 75.50, 신뢰도 100, LANGUAGE 69, FRAMEWORK 100, TESTING 75, DOCUMENTATION 80, ACTIVITY 56.67 |
| `fixtures/rule-engine/baseline-v2.json` | `baseline-v2` | 모든 사실 만점, 총점 100, 신뢰도 100, 8개 카테고리 모두 100 |

- fixture는 카테고리·규칙 가중치, 공식 파라미터, 정규화된 사실(fact), 기대 점수를 담는다. 마이그레이션 값과 같아야 한다.
- 백엔드 테스트(`DeterministicRuleEngineTest`, `RuleGoldenFixture`)가 두 fixture를 읽어 엔진 결과를 비교한다.
- 값을 바꾸려면 새 규칙 세트 버전과 새 fixture를 추가한다. 기존 fixture는 수정하지 않는다.

`baseline-v1` fixture 계산 확인:

```text
LANGUAGE      = 70×0.70 + (2/3×100=66.67)×0.30           = 69.00
FRAMEWORK     = (2/2×100)×1.00                           = 100.00
TESTING       = (8/10×100)×0.50 + (1/2×100)×0.30 + 100×0.20 = 75.00
DOCUMENTATION = 100×0.20×4 (CONTRIBUTING_GUIDE만 0)      = 80.00
ACTIVITY      = 50×0.60 + 66.67×0.20 + 66.67×0.20        = 56.67
총점          = 69×0.25 + 100×0.15 + 75×0.25 + 80×0.20 + 56.67×0.15 = 75.50
```

---

## 8. 요구사항 ID 목록

ID는 다른 문서에서 참조되므로 그대로 유지한다. "구현"은 위 절에 서술된 범위만 뜻한다.

### 8.1 Rule Engine (RR)

| ID | 제목 | 상태 |
|---|---|---|
| RR-001 | 언어 분석 | 구현 (`LANGUAGE` 규칙) |
| RR-002 | 프레임워크 분석 | 구현 (`FRAMEWORK_COUNT`) |
| RR-003 | 데이터베이스 분석 | 구현 (`baseline-v2` `DATABASE` 규칙) |
| RR-004 | 아키텍처 분석 | 구현 (`baseline-v2` `ARCHITECTURE` 규칙) |
| RR-005 | 테스트 분석 | 구현 (파일·프레임워크·CI 신호, 커버리지 제외) |
| RR-006 | DevOps 분석 | 구현 (`baseline-v2` `DEVOPS` 규칙) |
| RR-007 | 문서 분석 | 부분 구현 (경로 존재만, README 섹션 품질·Notion 문서 미반영) |
| RR-008 | 협업 분석 | 미구현 (커밋·기여자·브랜치 수만 `ACTIVITY`로 측정) |
| RR-009 | Skill Matrix 생성 | 구현 (`skill-matrix-v3`, 성장 추세는 `UNAVAILABLE`) |
| RR-010 | 총점 계산 | 구현 (기본 카테고리 가중치만, 커리어·기업 보정 없음) |

### 8.2 Career Path Engine (CR)

CR-001~CR-006은 [docs/SRS.md](SRS.md)에도 같은 번호로 정의되어 있다.

| ID | 제목 | 상태 |
|---|---|---|
| CR-001 | 커리어 선택 해석 | 구현 (사용자 목표 커리어 선호) |
| CR-002 | 커리어별 규칙 컨텍스트 선택 | 부분 구현 (Backend·Frontend 준비도 가중치만) |
| CR-003 | 기업 선택 해석 | 구현 (기업 선택 저장), 점수 반영 없음 |
| CR-004 | 기업별 규칙 컨텍스트 선택 | 미구현 |
| CR-005 | 스킬 갭 분석 | 구현 (`readiness-v1` 갭) |
| CR-006 | 학습 로드맵 생성 | 구현 (`roadmap-v1`) |
| CR-007 | 커리어 준비도 분류 | 구현 (`readiness-v1` 레벨) |
| CR-008 | 기업 준비도 분류 | 미구현 |
| CR-009 | 추천 객체 생성 | 구현 (`recommendation-v1`) |
| CR-010 | 추천 우선순위 | 구현 (갭 상태 기반 우선순위와 정렬) |
| CR-011 | 기술 우선순위 생성 | 미구현 |
| CR-012 | 프로젝트 추천 생성 | 부분 구현 (`PROJECT` 유형 템플릿만) |
| CR-013 | 프롬프트 컨텍스트 조립 | 별도 AI 모듈에서 구조화 결과를 사용 ([docs/ARCHITECTURE.md](ARCHITECTURE.md) 참조) |
| CR-014 | 충돌 감지 | 미구현 |
| CR-015 | 근거 부족 처리 | 구현 (`INSUFFICIENT_EVIDENCE`, 근거 상태) |
| CR-016 | 결과 저장 | 구현 (불변 결과, 버전 기준 멱등 저장) |
| CR-017 | 재계산 트리거 처리 | 구현 (분석 완료 시 순차 생성), 수동 재계산 없음 |
| CR-018 | 프로필 버전 검증 | 구현 (정책·프로필 버전 불일치 시 거부) |
| CR-019 | 확장 후보 가드레일 | 구현 (MVP 커리어 외에는 준비도 미생성) |
| CR-020 | 감사 추적 생성 | 부분 구현 (버전·근거 참조 저장, 로드맵 보관 감사 이벤트) |

---

## 9. 계획(미구현)

| 항목 | 상태 |
|---|---|
| 기업별 가중치·추천 보정 (Google, Amazon, Naver, Kakao, Toss, Coupang) | 미구현. 기업 선택은 저장되나 점수·준비도·추천에 반영하지 않음 |
| 기업 준비도 (CR-008) | 미구현 |
| Backend·Frontend 외 커리어 준비도 (AI Engineer, DevOps, Security, Game, Embedded, Mobile, Data Engineer) | 미구현. 카탈로그에 `career-v1` 프로필만 있고 준비도 가중치·추천 템플릿 없음 |
| 추가 평가 카테고리 (Collaboration, Repository Quality, Growth Trend, Project Complexity, Technology Diversity, Security Practices, Maintainability, Code Organization) | 미구현. baseline에 없음 |
| 성장 추세 계산 | 미구현. 항상 `UNAVAILABLE` |
| 결측 정책 `SKIP` 사용 규칙 | 미구현. 엔진은 지원하나 현재 규칙 없음 |
| 커리어·기업 가중치로 Rule Engine 총점 보정 | 미구현 |
| 추천 수락·거절·완료 상태 변경 | 미구현. 스키마 컬럼만 존재 |
| 로드맵 단계 진행·자동 완료·진행률 갱신 | 미구현. 보관만 구현 |
| `LOW` 우선순위(선택 역량) 추천 | 미구현. 값만 예약 |
| 외부 강의·도서·자격증 추천 | 미구현 |
