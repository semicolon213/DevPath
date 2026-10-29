# DevPath 테스트 fixture

이 디렉터리는 결정론 엔진 회귀 테스트에 쓰는 고정 데이터만 담는다.

- `rule-engine/baseline-v1.json`: 저장소 기준 규칙 세트 `baseline-v1`의 카테고리·규칙 가중치, 공식 파라미터, 정규화된 사실(`facts`), 기대 점수(`expected`)
- `rule-engine/baseline-v2.json`: 승인된 활성 규칙 세트 `baseline-v2`(데이터베이스·아키텍처·DevOps 카테고리 포함)의 같은 구성

백엔드 테스트 `backend/src/test/java/com/devpath/rule/domain/RuleGoldenFixture.java`가 두 파일을 읽고 `DeterministicRuleEngineTest`가 Rule Engine 결과를 기대 점수와 비교하며 반복 실행해도 같은 결과가 나오는지 검증한다. 값이 바뀌면 기존 파일을 고치지 않고 새 규칙 세트 버전과 새 fixture를 추가한다(자세한 내용은 `rule-engine/README.md`).
