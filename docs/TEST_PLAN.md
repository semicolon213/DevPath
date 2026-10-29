# DevPath 테스트 계획

이 문서는 DevPath의 테스트 수준, 테스트 위치, 실행 명령, 실행 환경, 변경 검증 방식을 정리한다.
여기 적은 테스트와 명령은 모두 저장소에 실제로 있는 것이다. 없는 것은 마지막 "계획(미구현)" 표에만 둔다.

관련 문서: 보안 규칙과 보안 테스트 [docs/SECURITY.md](SECURITY.md), 점수 규칙 [docs/SCORING_RULES.md](SCORING_RULES.md),
구조 [docs/ARCHITECTURE.md](ARCHITECTURE.md), 결정 [docs/ADR.md](ADR.md) (테스트 도구는 ADR-032), 상태 [docs/STATUS.md](STATUS.md).

## 1. 원칙

| 원칙 | 내용 |
|---|---|
| 결정론 검증 | 공식 점수는 같은 입력과 같은 규칙 버전에서 항상 같은 값이어야 한다. 기대 점수는 검토된 골든 데이터에서 오며 LLM이 만든 값을 기대값으로 쓰지 않는다. |
| 실제 PostgreSQL | PostgreSQL 동작이 필요한 테스트는 Testcontainers의 실제 PostgreSQL(pgvector 포함)로 돌린다. H2로 바꾸지 않는다. |
| AI 비결정성 | AI 생성 결과는 정확한 문자열로 비교하지 않는다. 스키마, 근거, 안전 검증 결과를 확인한다. 테스트는 실제 모델을 호출하지 않는다. |
| 보안 약화 금지 | 테스트를 통과시키려고 보안 설정이나 테스트를 약하게 만들지 않는다. |
| 테스트 데이터 | 테스트 데이터는 합성·익명 데이터만 사용한다. 실사용자·비공개 데이터와 운영 비밀값을 쓰지 않고 CI에서 실제 provider를 호출하지 않는다. |
| 결과 보고 | 각 검사는 `PASS`, `FAIL`, `NOT RUN`과 정확한 명령, 이유로 보고한다. 이전 실행 결과를 새 실행 결과처럼 보고하지 않는다. |

## 2. 테스트 수준

| 수준 | 대상 | 도구 | 실제 의존성 | 위치 |
|---|---|---|---|---|
| 도메인 단위 | 엔진, 도메인 불변식, 추출기 | JUnit 5 | 없음 | `backend/src/test/java/com/devpath/<module>/domain` |
| 애플리케이션 | 유스케이스, 트랜잭션 경계, 소유자 확인, worker | JUnit 5, 포트 대역(fake) | 없음 | `<module>/application` |
| 웹 어댑터 | 인증, CSRF, 요청 검증, 응답 envelope | Spring `@WebMvcTest` + Spring Security | 없음 | `<module>/adapter/in/web/*SecurityTest` |
| 영속성 통합 | JPA 매핑, Flyway 마이그레이션, 소유자 범위 질의, 잠금, pgvector 검색 | Spring Boot Test + Testcontainers | PostgreSQL 16 + pgvector (`pgvector/pgvector:0.8.0-pg16`) | `<module>/adapter/out/persistence/*PersistenceIntegrationTest` |
| 외부 어댑터 | GitHub, Notion, Ollama 응답 정규화와 오류 처리 | JUnit 5, 가짜 HTTP 응답 | 없음 | `<module>/adapter/out/<provider>` |
| 아키텍처 | 모듈·계층 의존 규칙 | ArchUnit 1.3.0 | 없음 | `backend/src/test/java/com/devpath/architecture` |
| 계약 lint | OpenAPI 문법과 규칙 | Redocly CLI 1.34.5 | 없음 | `contracts/openapi/devpath-openapi.yaml` |
| 저장소 보안 게이트 | 비밀·민감 파일·금지 패턴 | Node `node --test` + 검사 스크립트 | 없음 | `scripts/check-security-boundaries*.mjs` |
| 프론트엔드 단위·컴포넌트 | API 클라이언트, 훅, 화면 상태, 접근성 이름 | Vitest 4 + React Testing Library + jsdom | 없음 | `frontend/src/**/*.test.ts(x)` |
| 브라우저 e2e | 핵심 사용자 흐름, 반응형, 키보드, 접근성 | Playwright 1.62 (Chromium) + `@axe-core/playwright` | Vite 개발 서버. API는 계약 형태의 route mock | `frontend/e2e/repository-analysis-journey.spec.ts` |

아키텍처 테스트가 확인하는 규칙:

| 테스트 | 규칙 |
|---|---|
| `ArchitectureBoundaryTest.domainPackagesDoNotDependOnSpringOrProviderSdkPackages` | `domain`은 Spring, JPA, 공급자 SDK에 의존하지 않는다 |
| `ArchitectureBoundaryTest.domainPackagesDoNotDependOnAdapters` | `domain`은 어댑터에 의존하지 않는다 |
| `ArchitectureBoundaryTest.inboundAdaptersDoNotDependOnOutboundAdapters` | `adapter.in`은 `adapter.out`에 의존하지 않는다 |
| `ArchitectureBoundaryTest.moduleAdaptersDoNotImportOtherModuleAdapters` | 모듈 어댑터는 다른 모듈의 어댑터를 가져오지 않는다 |
| `ArchitectureBoundaryTest.webAdaptersDoNotDependOnJpa` | 컨트롤러는 JPA에 직접 접근하지 않는다 |
| `RepositoryBoundaryTest` | 백엔드와 프론트엔드 소스가 서로 섞이지 않는다 |

## 3. 결정론 골든 테스트

| 항목 | 내용 |
|---|---|
| 데이터 | `fixtures/rule-engine/baseline-v1.json`, `fixtures/rule-engine/baseline-v2.json` |
| 내용 | 카테고리·규칙 가중치, 공식 파라미터, 정규화된 사실(facts), 기대 점수 |
| 로더 | `backend/src/test/java/com/devpath/rule/domain/RuleGoldenFixture.java` |
| 테스트 | `DeterministicRuleEngineTest`: v1·v2 골든 점수 일치와 반복 실행 동일성, 추출기 불일치·잘못된 가중치 거부, 누락 근거는 점수를 만들지 않고 신뢰도만 낮춤 |
| 변경 규칙 | 가중치·공식·기대 점수가 바뀌면 새 규칙 세트 버전과 새 fixture를 만든다. 기존 fixture는 수정하지 않는다. |

다른 결정론 엔진은 도메인 단위 테스트로 같은 입력 → 같은 출력을 확인한다.

| 엔진 | 테스트 |
|---|---|
| Skill Matrix | `rule/domain/DeterministicSkillMatrixBuilderTest` |
| 커리어 준비도 | `career/domain/DeterministicCareerReadinessEngineTest` |
| 추천 | `recommendation/domain/DeterministicRecommendationEngineTest` |
| 학습 로드맵 | `learning/domain/DeterministicRoadmapEngineTest` |
| 저장소 근거 추출 | `repository/domain/RepositoryEvidenceExtractorTest`, `DependencyTechnologyDetectorTest`, `RepositoryActivityTimelineExtractorTest` |

점수 공식과 버전은 [docs/SCORING_RULES.md](SCORING_RULES.md)를 따른다.

## 4. 기능 영역별 테스트 위치

백엔드 경로는 `backend/src/test/java/com/devpath/` 기준, 프론트엔드 경로는 `frontend/src/` 기준이다.

| 기능 영역 | 백엔드 테스트 | 프론트엔드 테스트 |
|---|---|---|
| 인증·세션·프로필 | `identity/`: `IdentitySecurityIntegrationTest`, `AbsoluteSessionTimeoutFilterTest`, `OAuthLoginApplicationServiceTest`, `UserProfileApplicationServiceTest`, `DevPathOAuth2UserTest`, `UserTest`, `ExternalIdentityTest`, `IdentityPersistenceIntegrationTest` | `features/session/api/sessionApi.test.ts`, `features/session/ui/SessionPanel.test.tsx`, `features/profile/**`, `shared/api/apiClient.test.ts` |
| GitHub·Notion 연동 | `integration/`: `GitHubIntegrationSecurityTest`, `NotionIntegrationSecurityTest`, `GitHubApiAdapterTest`, `NotionApiAdapterTest`, `AesGcmCredentialCipherTest`, `EncryptedNotionCredentialStoreTest`, `ProviderCredentialJpaEntityTest`, `NotionWorkspaceConnectionJpaEntityTest`, `NotionConnectionPersistenceIntegrationTest`, `GitHubIntegrationApplicationServiceTest`, `NotionIntegrationApplicationServiceTest`, `ProviderConnectionApplicationServiceTest`, `GitHubIntegrationConfigurationTest` | `features/connections/**`, `pages/SettingsPages.test.tsx`, `pages/SettingsHeadingOrder.test.tsx` |
| 저장소·동기화·스냅샷 | `repository/`: 18개 (`RepositorySecurityTest`, `RepositorySynchronizationSecurityTest`, `RepositorySyncWorkerTest`, `RepositorySnapshot*PersistenceIntegrationTest`, `RepositoryJobReliabilityPersistenceIntegrationTest`, 추출기·도메인 테스트 등) | `features/repositories/api/repositoryApi.test.ts`, `pages/RepositoriesPage.test.tsx`, `pages/RepositoryDetailPage.test.tsx`, `pages/RepositorySnapshotDetailPage.test.tsx` |
| 분석·Rule Engine | `analysis/`: `AnalysisSecurityTest`, `AnalysisApplicationServiceTest`, `AnalysisWorkerTest`, `AnalysisJobTest`, `AnalysisHistoryPersistenceIntegrationTest`; `rule/`: `RuleEvaluationSecurityTest`, `DeterministicRuleEngineTest`, `RuleCatalogPersistenceIntegrationTest`, `CompletedRuleEvaluationApplicationServiceTest`, `RepositoryRuleEvaluationApplicationServiceTest`, `RepositoryRuleEvidenceMapperTest` | `features/analysis/api/analysisApi.test.ts`, `pages/AnalysesPage.test.tsx`, `pages/AnalysisDetailPage.test.tsx`, `pages/AnalysisComparisonPage.test.tsx` |
| Skill Matrix | `rule/`: `SkillMatrixSecurityTest`, `DeterministicSkillMatrixBuilderTest`, `SkillMatrixApplicationServiceTest`, `SkillMatrixPersistenceIntegrationTest` | `features/skills/api/skillMatrixApi.test.ts`, `pages/SkillsPage.test.tsx`, `pages/SkillDetailPage.test.tsx`, `pages/SkillMatrixComparisonPage.test.tsx` |
| 직무·회사·준비도 | `career/`: 6개 (`CareerCatalogSecurityTest`, `CareerReadinessSecurityTest`, `DeterministicCareerReadinessEngineTest`, 카탈로그·정책 영속성 테스트 등); `company/`: `CompanyCatalogApplicationServiceTest`, `CompanyCatalogPersistenceIntegrationTest` | `features/careers/**`, `features/companies/**`, `features/readiness/**`, `pages/Career*.test.tsx`, `pages/Compan*.test.tsx` |
| 추천·학습 로드맵 | `recommendation/`: `RecommendationSecurityTest`, `DeterministicRecommendationEngineTest`, `RecommendationApplicationServiceTest`, `RecommendationPolicyPersistenceIntegrationTest`; `learning/`: `DeterministicRoadmapEngineTest` | `features/recommendations/api/recommendationApi.test.ts`, `pages/RecommendationsPage.test.tsx`, `pages/LearningRoadmapPage.test.tsx` |
| 지식(Notion) 수집·검색 | `knowledge/`: `KnowledgeSecurityTest`, `KnowledgeChunkerTest`, `KnowledgeIngestionWorkerTest`, `KnowledgeApplicationServiceTest`, `KnowledgeSearchApplicationServiceTest`, `FilesystemObjectContentAdapterTest`, `KnowledgePersistenceIntegrationTest` | `features/knowledge/api/knowledgeApi.test.ts`, `pages/KnowledgePage.test.tsx`, `pages/KnowledgeDetailPage.test.tsx` |
| 프롬프트 | `prompt/`: `PromptSecretDetectorTest`, `PromptPackageTest`, `RepositoryReviewPromptBuilderTest`, `RepositorySummaryPromptBuilderTest`, `SkillExplanationPromptBuilderTest` | - |
| AI 생성·검증 | `ai/`: `AiGenerationSecurityTest`, `AiGenerationApplicationServiceTest`, `AiGenerationWorkerTest`, `GeneratedOutputPolicyTest`, 작업별 `*ValidatorTest` 5개, `OllamaGenerationAdapterTest`, `JpaGenerationPersistenceAdapterTest`, `AiGenerationPersistenceIntegrationTest` | `features/ai/api/generationApi.test.ts`, `features/ai/model/useGeneration.test.tsx`, `features/ai/ui/*Panel.test.tsx` 6개 |
| 대시보드·온보딩 | `dashboard/`: `DashboardSecurityTest`, `DashboardApplicationServiceTest`; `onboarding/`: `OnboardingSecurityTest`, `OnboardingApplicationServiceTest` | `pages/DashboardPage.test.tsx`, `pages/OnboardingPage.test.tsx` |
| 감사·공통 API·운영 | `audit/DurableAuditRecordAdapterTest`, `shared/api/RequestCorrelationFilterTest`, `platform/health/InternalHealthControllerTest`, `reliability/JobRequestConcurrencyIntegrationTest` | - |
| 앱 셸·라우팅·공통 UI | - | `app/App.test.tsx`, `app/AppLayout.test.tsx`, `app/styles.test.ts`, `routes/AppRoutes.test.tsx`, `shared/ui/ConfirmDialog.test.tsx`, `shared/ui/labels.test.ts`, `shared/ui/PageState.test.tsx`, `shared/ui/RouteAccessibility.test.tsx` |
| 아키텍처 | `architecture/ArchitectureBoundaryTest`, `architecture/RepositoryBoundaryTest` | - |

Testcontainers PostgreSQL을 쓰는 테스트는 `test/PgVectorPostgreSQLContainer.java`를 공유한다.
`CareerCatalogPersistenceIntegrationTest`, `CompanyCatalogPersistenceIntegrationTest`는 `DEVPATH_DB_URL`이 설정된 경우에만 실행되고 없으면 SKIP된다.

## 5. 통합 테스트 시나리오 (e2e)

`frontend/e2e/repository-analysis-journey.spec.ts`의 시나리오다. 백엔드 대신 계약 형태의 응답을 Playwright route로 돌려준다.
따라서 실제 백엔드 인가, PostgreSQL, 실제 GitHub·Notion·Ollama 연동은 이 테스트로 검증되지 않는다. 그 부분은 2절의 백엔드 테스트가 맡는다.

| 시나리오 | 확인 내용 |
|---|---|
| 저장소 분석 결과가 Skill Matrix, 준비도, 로드맵으로 이어짐 | 저장소 근거, CSRF·`Idempotency-Key`가 붙은 분석 요청, 분석 이력·상세, Skill Matrix, 준비도, 추천, 학습 로드맵 |
| 온보딩 완료 후 첫 분석 흐름 | 온보딩 완료가 결과 화면으로 연결됨 |
| 공유된 Notion 페이지가 복구 가능한 지식 수집 작업이 됨 | 가져오기 요청의 CSRF·멱등 키, 작업 상태 |
| 대시보드 요약 | 공식 결과 표시, 부분 상태의 접근성 표현 |
| 프로필과 GitHub 연동 설정 분리 | 설정 화면 구분과 접근성 |
| 익명 사용자의 준비도 접근 실패 | 실패 안내와 복구 동작, 접근성 |
| 모바일 내비게이션 | 키보드 조작, 접근성 |
| 감소된 움직임(reduced motion) 사용자 | 불필요한 전환 없이 키보드 흐름 유지 |
| 가로 넘침 없음 | 대시보드·결과 화면, 375px 프로필 선택, 스크롤바 표시 상태 |
| 레이아웃 회귀 | 지식 상세 breadcrumb 클릭 가능, 보관·삭제된 저장소 행의 동작 열, 저장소 hero 보조 동작 가독성, 오류 패널 스타일 |

시나리오는 모두 15개이며 주요 화면에서 axe 검사(WCAG 2.0 A/AA, WCAG 2.1 AA)를 함께 실행한다.

## 6. 실행 명령

저장소 루트에서 실행한다. 정의는 루트 `package.json`과 `frontend/package.json`에 있다.

| 명령 | 실행 내용 |
|---|---|
| `npm run docker:check` | Docker 엔진이 응답하는지 확인 (`scripts/check-docker.mjs`) |
| `npm run security:check` | `node --test scripts/check-security-boundaries.test.mjs` 후 `node scripts/check-security-boundaries.mjs` |
| `npm run backend:test` | `gradlew test` (`scripts/run-gradle.mjs`) |
| `npm run backend:build` | `gradlew build` (테스트 포함) |
| `npm run frontend:install` | `frontend`에서 `npm ci` |
| `npm run frontend:test` | `vitest run` |
| `npm run frontend:e2e` | bind 가능한 포트를 선택한 뒤 `playwright test` |
| `npm run frontend:build` | `tsc --noEmit && vite build` |
| `npm run frontend:quality` | `npm run test && npm run test:e2e && npm run build && npm audit` (frontend 내부) |
| `npm run contract:lint` | `npx --yes @redocly/cli@1.34.5 lint contracts/openapi/devpath-openapi.yaml --skip-rule=no-server-example.com` |
| `npm run test` | 백엔드 `test` → 프론트엔드 `test` (e2e 제외) |
| `npm run verify` | 백엔드 `clean build` → 프론트엔드 `ci` → `test` → `build` (e2e, 보안 검사, 계약 lint 제외) |
| `npm run verify:mvp` | `docker:check` → `security:check` → 백엔드 `clean build` → 프론트엔드 `ci` → `quality` → `contract:lint` |

권장 순서: 변경한 영역의 대상 테스트를 먼저 돌리고 그다음 `npm run test`, 마지막으로 `npm run verify:mvp`로 전체 게이트를 확인한다.

`scripts/run-gradle.mjs`는 Gradle wrapper가 실패하면 로컬 캐시의 Gradle 8.11.1 배포본으로 한 번 다시 시도한다.

## 7. 실행 환경

| 항목 | 내용 |
|---|---|
| Java | JDK 21. Gradle toolchain이 Java 21을 요구한다. 여러 JDK가 있으면 `JAVA_HOME`을 JDK 21로 맞춘다. |
| Node | `frontend/package.json` engines: `^20.19.0 \|\| >=22.12.0` |
| Docker | 영속성 통합 테스트(Testcontainers)에 실행 중인 Docker 호환 엔진이 필요하다. Docker가 없으면 `verify:mvp`는 첫 단계에서 멈춘다. |
| `DOCKER_API_VERSION` | Docker Engine 29는 Testcontainers 1.20.3의 기본 API 버전을 거부한다. `DOCKER_API_VERSION=1.44`를 설정하면 Gradle test 작업이 테스트 JVM에 `api.version`으로 넘긴다. PowerShell: `$env:DOCKER_API_VERSION='1.44'` |
| 백엔드 환경변수 | Spring Boot는 `backend/.env.example`을 자동으로 읽지 않는다. 필요한 값은 프로세스 환경변수로 넣는다. Testcontainers 테스트는 컨테이너 접속 정보를 스스로 주입한다. |
| 조건부 SKIP | `DEVPATH_DB_URL`이 없으면 카탈로그 통합 테스트 2개가 SKIP된다. 결과 보고 시 SKIP을 따로 적는다. |
| 프론트엔드 API 주소 | `frontend/vitest.setup.ts`가 테스트용 API 주소를 `http://localhost:8080`으로 고정한다. 로컬 `.env`의 값은 단위 테스트에 영향을 주지 않는다. |
| Playwright 브라우저 | 처음 한 번 `cd frontend` 후 `npx playwright install chromium` |
| e2e 포트 | `npm run test:e2e`가 먼저 `127.0.0.1:4173`을 bind 검사하고 사용할 수 없으면 OS가 배정한 빈 포트를 한 번 선택해 Playwright 개발 서버와 테스트에 함께 전달한다 (`reuseExistingServer: false`). |
| e2e 포트 지정 | `DEVPATH_E2E_PORT`를 설정하면 해당 포트만 사용하며 bind할 수 없으면 다른 포트로 대체하지 않고 명확한 오류로 종료한다. |

## 8. 리뷰 검증 방식

변경은 구현한 세션과 다른 세션이 검토한다. 상세 규칙은 [AGENTS.md](../AGENTS.md)와 [CLAUDE.md](../CLAUDE.md)에 있다.

| 역할 | 수행자 | 확인 내용 | 하지 않는 것 |
|---|---|---|---|
| 구현 | Codex | 프로덕션 코드와 테스트, 리뷰 지적 수정 | 자기 변경 리뷰 |
| 테스트 전용 작업 | 별도 Claude 세션 | 테스트만 추가·보강 | 프로덕션 코드·설정·마이그레이션 변경, 발견한 결함 수정 |
| QA-A | 별도 Claude 세션 (읽기 전용) | 기능·스펙 충족, 회귀, 테스트가 실제로 문제를 잡는지. 테스트·빌드·e2e를 직접 실행 | 파일 수정 |
| QA-B | 별도 Claude 세션 (읽기 전용) | 프론트엔드: DESIGN.md와 WCAG 접근성. 백엔드: 데이터 일관성, 동시성, 보안 | 파일 수정 |
| CR | 별도 Claude 세션 (읽기 전용) | 정확성 버그, 가독성, 중복, 코딩 표준 | 파일 수정, 테스트 실행 |

| 규칙 | 내용 |
|---|---|
| 스냅샷 고정 | 리뷰 대상은 diff 해시로 고정한다. 스냅샷이 다르면 리뷰는 `BLOCKED`. main에 적용한 diff의 해시가 같을 때만 커밋한다. |
| 통과 조건 | QA-A와 QA-B가 `PASS`이고 CR에 막는 지적이 없어야 한다. |
| 최대 3회 | 첫 리뷰를 포함해 최대 3회. 지적은 모아서 한 번에 구현자에게 돌려준다. 3회 안에 통과하지 못하면 멈추고 보고한다. |
| 뮤테이션 증거 | 테스트 전용 작업은 새 테스트마다 대상 동작을 일부러 망가뜨려 테스트가 실패하는 것을 보이고 원상 복구 후 최종 diff에 테스트만 남았음을 확인한다. |
| 담당 범위 밖 변경 검사 | 작업자는 배정된 경로만 수정한다. 리뷰와 코디네이터는 최종 diff에 범위 밖 파일(OpenAPI, 마이그레이션, lockfile, 루트 빌드 파일, 에이전트 가이드 등)이 섞이지 않았는지 확인한다. 문서 작업은 Markdown 외 파일이 바뀌지 않았음을 확인한다. |
| 결과 형식 | 각 검사를 `PASS`/`FAIL`/`NOT RUN`과 정확한 명령, 근거로 보고한다. 검증하지 못한 것은 성공으로 보고하지 않는다. |

## 9. 계획(미구현)

| 항목 | 상태 |
|---|---|
| CI 파이프라인 | `main` push·PR에서 백엔드(Testcontainers Docker 포함), 프론트엔드, e2e, 보안·계약 검사를 실행한다. 백엔드 skip guard는 Testcontainers 테스트가 건너뛰면 실패하며 `DEVPATH_DB_URL`로 게이트된 카탈로그 통합 테스트 2개는 의도적으로 SKIP을 허용한다. 첫 실제 실행은 push 후 확인한다. |
| 실제 백엔드·PostgreSQL을 붙인 e2e | 미구현 (현재 route mock) |
| 두 사용자 격리 e2e 시나리오 | 미구현 (격리는 백엔드 테스트로만 확인) |
| 실제 GitHub·Notion·Ollama 대상 검증 | 미구현 |
| AI 품질 평가(실제 모델 대상 근거·안전 평가 세트) | 미구현 |
| 두 트랜잭션이 실제로 경합하는 취소 잠금 테스트 | 미구현 (잠금 조회와 단위 테스트만 있음) |
| 백엔드 의존성 취약점 검사 | 미구현 |
| 성능·부하·복원력 테스트, 커버리지 측정 도구 | 미구현 |
| 보조기술(스크린 리더) 수동 점검 기록 | 미구현 |
