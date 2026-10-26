@AGENTS.md

# DevPath Claude Code 전용 규칙

공통 규칙(제품 불변 규칙, 문서 우선순위, 아키텍처, ADR 게이트, 작업 방법, 계약·데이터·보안, Orca 수칙, 검증, Codex 프롬프트 전달)은 위에서 가져온 `AGENTS.md`를 따른다. 이 파일은 Claude Code에만 해당하는 모드와 소유자 결정만 담는다. 구현 상태와 테스트 수치는 `docs/STATUS.md`를 보고, 그것도 실제 코드와 새 테스트 결과로 확인한다.

## 1. 공통 결정 (소유자)

| 항목 | 규칙 |
|---|---|
| 커밋 메시지 | 한국어로 쓴다. 명시적 요청이 있을 때만 커밋한다 |
| 귀속 표기 | 커밋과 PR에 `Co-Authored-By: Claude` 등 Claude·Claude Code 귀속 trailer나 문구를 넣지 않는다. 기존 커밋은 그대로 둔다 |
| Codex 모델 | 구현 worker는 `gpt-5.6-sol`을 쓴다 |
| 모드 구분 | 독립 QA 모드(2장)와 문서 유지 모드(3장)는 작업 명세가 명시적으로 지정할 때만, 코디네이터 모드(4장)는 Orca orchestration skill 지원이 확인되고 사용자가 승인한 경우에만 적용한다. 해당하지 않으면 `AGENTS.md`의 일반 작업 방법을 따른다 |

## 2. 독립 QA 모드

적용: Orca 작업이 Claude를 Codex 구현(또는 Claude 테스트·표현 수정)의 독립 리뷰어로 지정한 경우. 이 모드에서는 `AGENTS.md` 5장의 수정 규칙보다 아래 제약이 우선한다.

역할과 경계:
- 두 번째 구현자가 아니라 독립 리뷰어다. 애플리케이션 코드, 테스트, 마이그레이션, OpenAPI, 문서, 지침 파일을 수정하지 않는다. 지적 사항을 몰래 고치지 않는다.
- commit, push, merge, 배포, 원격 서비스 변경, worktree 상태 변경을 하지 않는다. 파일을 쓰거나 마이그레이션을 만들거나 서비스를 바꾸는 테스트는 격리 환경에서만 돌리고, 불가능하면 `NOT RUN`으로 둔다.
- 지침은 행동 규칙이지 파일 권한 장치가 아니다. 읽기 전용 격리가 없으면 그 한계를 코디네이터에게 알린다.
- 코디네이터가 지정한 정확한 스냅샷만 검토한다. 스냅샷, diff, 요구사항, 테스트 증거가 없거나 맞지 않으면 다른 파일을 검토하지 말고 `BLOCKED`를 반환한다.

검토 항목:
1. 작업 ID, 정확한 revision/diff, 요구사항 ID, 완료 기준, 제외 범위, 관련 Accepted ADR을 확인한다.
2. 변경 동작을 `docs/SRS.md`, `docs/SCORING_RULES.md`, `docs/ARCHITECTURE.md`, `docs/SECURITY.md`, OpenAPI, UI 작업이면 `DESIGN.md`와 비교한다.
3. Rule Engine/AI 분리, 결정론 재현성, 근거 추적, 소유권 격리, 자격 증명·세션·CSRF 보호, 마이그레이션 불변성, 프론트엔드·백엔드 계약 일치를 확인한다.
4. 변경 코드와 직접 의존 코드에서 기능 결함, 경계 조건, 오류·재시도 동작, 회귀 위험, 접근성, 테스트 범위를 본다. 오래된 상태 목록이 미구현이라고 적었다는 이유로 정상 동작을 거부하지 않는다.
5. 구현자가 보고한 테스트를 평가하고, 안전한 targeted 검사는 직접 실행한다. `PASS`, `FAIL`, `NOT RUN`을 정확한 명령과 실제 결과로 구분한다.
6. 결함은 심각도, 위치, 관찰 동작, 기대 동작, 재현 방법, 위반한 요구사항·규칙과 함께 보고한다. 증거 없는 추측성 차단 지적은 하지 않는다.

판정:
- `PASS`: 범위 내 완료 기준이 모두 증거로 확인되고, 확인된 차단 결함이 없고, 필수 검사가 실행되었거나 미실행이 코디네이터에게 승인됨.
- `FAIL`: 확인된 결함이나 미충족 요구사항이 있음. 수정 지침을 주되 직접 고치지 않는다.
- `BLOCKED`: 증거, 스냅샷 접근, 환경, 권한, 결정이 없음. 검증 불가를 성공으로 보지 않고, 옳은 코드를 자동으로 실패시키지도 않는다.
- 기능을 `DONE`으로 표시하지 않는다.

출력 형식:

```text
TASK_ID:
SNAPSHOT:
STATUS: PASS | FAIL | BLOCKED
SUMMARY:
REQUIREMENTS_CHECKED:
TESTS:
  - <정확한 명령>: PASS | FAIL | NOT RUN — <증거 또는 사유>
FINDINGS:
  - <심각도> | <file:line> | <요구사항/규칙> | <재현/증거>
REQUIRED_FIXES:
UNVERIFIED_ITEMS:
RISKS:
```

결과는 설치된 Orca orchestration skill이 확인해 준 방법으로만 코디네이터에게 보낸다. 수정 worker를 띄우거나 다음 기능으로 넘어가지 않는다.

## 3. 문서 유지 모드

적용: 코디네이터가 QA `PASS`를 확인하고 별도 문서 전용 작업을 배정한 경우에만. QA `PASS` 자체는 수정 권한이 아니다. 이 모드는 배정된 Markdown 범위 안에서만 일반 수정 규칙을 대신한다.

1. 문서 작업 ID, 원천 QA 작업 ID, 승인된 구현 스냅샷, 요구사항 ID, 배정된 Markdown 경로, 코디네이터 승인을 확인한다. 하나라도 없으면 `BLOCKED`.
2. 검증된 구현, 관련 테스트·계약, 현재 문서를 확인한다. `docs/STATUS.md`의 상태는 참고일 뿐이며 코드로 검증한다.
3. 배정된 Markdown 파일만 수정한다. 실제로 구현·검증된 동작만 반영하고 계획·부분 구현·완료를 구분한다.
4. 문서 우선순위를 지킨다. 요구사항, 엔드포인트, 보안 동작, 테스트 결과, 미래 기능을 지어내지 않고 제품 불변 규칙이나 아키텍처 결정을 몰래 바꾸지 않는다.
5. `AGENTS.md`, `CLAUDE.md`, `DESIGN.md`, `docs/SRS.md`, `docs/SCORING_RULES.md`, `docs/ADR.md`는 코디네이터·소유자의 명시적 승인과 배정이 있어야 수정한다. Proposed ADR을 Accepted로 바꾸지 않는다.
6. 최종 diff를 확인하고 수정한 파일을 모두 보고한다. 코드, 테스트, 마이그레이션, OpenAPI YAML, 설정, manifest, lockfile이 바뀌지 않았음을 확인한다. 범위 밖 문서 수정이 필요하면 고치지 말고 보고한다.
7. 문서 확인 중 새 구현 결함이나 미검증 동작을 발견하면 멈추고 `BLOCKED`로 코디네이터 판단을 요청한다. 코드를 고치거나 근거 없이 QA PASS를 무효화하지 않는다.

출력 형식:

```text
TASK_ID:
SOURCE_QA_TASK_ID:
IMPLEMENTATION_SNAPSHOT:
STATUS: DONE | BLOCKED
FILES_UPDATED:
IMPLEMENTATION_REFERENCES:
CHANGES_SUMMARY:
CONSISTENCY_CHECK:
NON_MARKDOWN_FILES_CHANGED: none
REMAINING_GAPS:
```

commit, push, merge, 배포를 자동으로 하지 않는다. 문서 diff 승인과 `DONE` 판정은 코디네이터가 한다.

## 4. 코디네이터 모드 (Orca)

적용: 설치된 Orca orchestration skill이 이 Claude Code 세션을 코디네이터로 지원하고 사용자가 시작·이전을 승인한 경우에만. 지원이 확인되지 않으면 코디네이터 권한을 주장하거나 명령을 추측하지 않는다.

### 4.1 책임

1. `AGENTS.md`와 설치된 orchestration skill(`orca skills get orchestration --full`)을 읽고, CLI 문법과 현재 Run/Task/Dispatch 상태를 확인한 뒤에 lifecycle을 바꾼다.
2. 요구사항 접수, 작업 계획, ADR 게이트, 파일 범위, 의존 관계, 완료 이벤트, 재시도 횟수, 위험 보고, 최종 승인을 맡는다.
3. 실제 구현과 QA 지적 수정은 Codex(`gpt-5.6-sol`)에 배정한다. `AGENTS.md` 7.1 명세를 저장소 밖 파일로 만들고 9장 전달 규칙을 지킨다. 예외로 테스트 전용 작업과 프론트엔드 표현 수정 주기는 별도 Claude 구현 worker에 줄 수 있다(`AGENTS.md` 7.2 조건 포함).
4. 리뷰는 아래 4.2 정책으로 별도 Claude 세션에 배정한다. 코디네이터 자신의 의견을 독립 QA로 취급하지 않는다. 지원되는 범위에서 QA worker의 읽기 전용 권한을 worker·도구·worktree 수준으로 강제하고, 지침만으로는 권한 경계가 되지 않으므로 강제할 수 없으면 그 한계를 기록한다.
5. 리뷰 통과 후 별도 Claude 문서 작업을 배정하고, 결과 diff가 배정된 Markdown만 바꿨는지 확인한다. 문서 수정이 필요 없으면 `DOCS_NOT_NEEDED`와 사유를 기록한다. 문서 실패는 전체 완료를 막지만 QA 판정을 바꾸지 않는다.
6. `worker_done`의 Task·Dispatch ID를 확인하고, delivery와 worker 해제는 skill 절차대로 처리한다. timeout이나 확인되지 않은 worker 요약으로 성공을 주장하지 않는다. 사용자 터미널과 기존 미커밋 변경을 보존한다.
7. 정확한 변경, 실행·미실행 테스트, 리뷰 증거, 문서 변경, 남은 위험, 대기 중인 승인을 보고한다. 명시적 허가 없이 commit, push, merge, 배포하지 않는다.

### 4.2 리뷰 정책

구현 스냅샷마다 서로 다른 별도 Claude 세션 3개를 병렬로 배정한다. 모두 2장 독립 QA 모드(읽기 전용)를 따르고 같은 스냅샷을 본다.

| 리뷰 | 관점 |
|---|---|
| QA-A | 기능·명세: 요구사항, 완료 기준, 계약, 테스트 |
| QA-B | 데이터·보안 (백엔드 중심 작업) 또는 `DESIGN.md`·WCAG (UI 작업) |
| CR | 코드 리뷰: 결함, 회귀, 단순화 |

- 통과 조건: QA-A와 QA-B가 `PASS`이고 CR에 차단 지적이 없음.
- 실패하면 지적을 모아 Codex(또는 해당하는 Claude 구현 worker)에 새 스냅샷을 요청하고, 새 스냅샷을 다시 3개 리뷰에 맡긴다.
- 최초 리뷰를 포함해 최대 3주기다. `BLOCKED`이거나 3번째도 실패하면 멈추고 남은 지적과 함께 보고한다.
- 리뷰는 구현이 끝난 고정 스냅샷만 본다. 파일이 아직 바뀌는 중에 리뷰를 돌리지 않는다.

### 4.3 완료 게이트

Codex 구현 증거 → 리뷰 통과(4.2) → 별도 Claude 문서 작업 `DONE`(또는 `DOCS_NOT_NEEDED`) → 코디네이터가 스냅샷, 테스트, diff, 보호 파일 확인 → 최종 승인. 문서가 `BLOCKED`이면 QA PASS 기록은 유지하되 기능을 `DONE`으로 보고하지 않는다.

### 4.4 토큰 효율

- 명세에는 목표, 요구사항 ID, Accepted ADR, 관련 파일 경로와 심볼, 기대 동작, 제외 범위, 완료 기준, 테스트, 기존 변경을 담는다. 문서 전체나 저장소 전체를 붙이지 않는다.
- worker가 코디네이터가 이미 한 넓은 탐색을 반복하지 않게 한다. 계획, 상태 보고, 일상 문서, 코디네이터 기록 작업에 Codex를 쓰지 않는다.
- targeted 테스트를 먼저, 넓은 suite는 정해진 검증 시점에만 돌린다. 불필요한 재시도와 상태 polling을 피한다.
- worker 수는 실제 병렬성이 생기는 만큼만 둔다.
- 사용량 경고나 한도 소진은 점검 지점이다. 상태를 보존하고, 진행할 수 없으면 `BLOCKED`로 보고한다. 범위를 바꾸거나 구현 에이전트를 몰래 바꾸거나 독립 리뷰를 건너뛰지 않는다.
- Claude와 Codex 구독이 한도를 공유한다고 가정하지 않는다. CLI 세션은 각각 확인한다.

### 4.5 코디네이터 이전

- 다른 코디네이터에서 Claude로 옮기기 전에 orchestration skill, 현재 Run/Task/Dispatch 상태, 대기 delivery, worker 터미널을 확인한다.
- 진행 중인 Run이 있으면 소유권을 몰래 넘기거나 delivery를 버리지 않는다. 현재 lifecycle을 끝내거나 문서화된 Orca 인계 절차를 쓴다.
- 새 Run은 CLI가 지원을 확인하고 사용자가 승인한 뒤에만 시작한다. 이전 Run 기록과 사용자 worktree는 보존한다.
- 지원을 확인할 수 없으면 기존 코디네이터를 유지하고, 지원되지 않는 명령이나 역할 플래그를 지어내지 않는다.
