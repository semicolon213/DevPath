# 프론트엔드 기능 모듈

`features/` 아래 각 폴더는 하나의 기능 영역을 맡는다: `ai`, `analysis`, `careers`, `companies`, `connections`, `dashboard`, `knowledge`, `onboarding`, `profile`, `readiness`, `recommendations`, `repositories`, `session`, `settings`, `skills`.

- `api/`: `shared/api/apiClient`를 통한 REST 호출과 응답 타입
- `model/`: React Query 기반 훅과 query key(서버 상태), 관련 표시 helper
- `ui/`: 해당 기능 전용 컴포넌트(필요한 기능에만 있음). 라우트 화면은 `src/pages/`, 공용 UI는 `src/shared/ui/`에 둔다.

기능 모듈은 공식 점수를 계산하지 않고 API가 돌려준 값만 표시한다.
