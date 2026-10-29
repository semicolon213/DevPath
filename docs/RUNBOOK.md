# DevPath 로컬 문제 해결 Runbook

로컬 개발과 검증에서 재현 가능한 장애만 다룬다. 명령은 저장소 루트의 PowerShell 기준이다.

## Docker 미실행으로 Testcontainers 실패

**증상**

- 백엔드 PostgreSQL 통합 테스트가 Docker 연결 오류로 시작하지 못한다.

**확인**

```powershell
npm run docker:check
```

**조치**

- Docker Desktop을 시작하고 엔진이 준비될 때까지 기다린다.

**완료 확인**

- `npm run docker:check`가 PASS한 뒤 실패했던 백엔드 테스트를 다시 실행한다.
- Docker가 필요한 테스트의 SKIP은 PASS로 보고하지 않는다.

## Docker Desktop API 버전 호환 실패

**증상**

- Docker Desktop과 Testcontainers 1.20.3 조합에서 Docker API 호환 오류가 난다.

**확인**

- Docker는 실행 중인데 Testcontainers 연결 단계에서 API 버전 오류가 표시되는지 확인한다.

**조치**

```powershell
$env:DOCKER_API_VERSION = '1.44'
npm run backend:test
```

**완료 확인**

- 같은 셸에서 PostgreSQL Testcontainers 테스트가 시작되고 종료된다.

## Ollama 미실행 또는 모델 미설치

**증상**

- 지식 임베딩이나 AI 생성 작업이 provider 오류로 재시도되며 실행 중이던 작업은 lease 동안 `RUNNING`으로 보일 수 있다.

**확인**

```powershell
ollama list
```

- `backend/.env`의 `DEVPATH_EMBEDDING_BASE_URL`, `DEVPATH_AI_BASE_URL`, 모델 이름을 확인한다.

**조치**

- Ollama를 시작한다.
- 기본 생성 모델 `qwen2.5:7b`와 임베딩 모델 `nomic-embed-text`를 설치하거나 환경 변수에 설치된 모델을 지정한다.
- 작업 행을 직접 수정하지 않는다. lease가 만료되면 워커가 재클레임하고 시도 한도에 닿으면 `WORKER_LEASE_EXPIRED`로 실패한다.

**완료 확인**

- `ollama list`에 설정한 모델이 보이고 `http://localhost:11434`가 응답한다.
- 새 작업이 `SUCCEEDED`가 되거나 기존 작업이 lease 만료 후 재클레임되는지 작업 조회 API로 확인한다.

## Windows 예약 포트와 E2E 포트 충돌

**증상**

- Playwright 웹 서버가 기본 포트 `4173`에 bind하지 못한다.

**확인**

- `npm run frontend:e2e` 출력의 `[e2e] Using ...` 주소와 bind 오류를 확인한다.

**조치**

- 기본 포트가 바쁘면 스크립트가 빈 포트를 한 번 자동 선택하므로 다시 고정하지 않는다.
- 특정 포트가 필요하면 실행 전에 지정한다.

```powershell
$env:DEVPATH_E2E_PORT = '4180'
npm run frontend:e2e
```

**완료 확인**

- 출력에 선택된 주소가 표시되고 Playwright가 시작된다.
- 명시한 포트를 사용할 수 없으면 다른 포트로 대체되지 않고 명확한 오류로 종료되는 것이 정상이다.

## Flyway 검증 실패

**증상**

- 백엔드 시작 또는 빌드에서 적용된 마이그레이션의 checksum·순서 검증이 실패한다.

**확인**

- 오류에 표시된 마이그레이션 버전과 `backend/src/main/resources/db/migration`의 파일을 비교한다.
- 최신 마이그레이션은 V30이며 실행 설정은 `ddl-auto=validate`다.

**조치**

- 이미 적용된 마이그레이션을 수정하지 않는다.
- 우발적으로 바뀐 기존 파일은 원래 내용으로 복구한다.
- 필요한 스키마 변경은 다음 번호의 새 immutable Flyway 파일로만 추가한다.

**완료 확인**

```powershell
npm run backend:build
```

- Flyway 검증과 JPA 스키마 검증이 오류 없이 끝나는지 확인한다.

## 필수 환경 변수 누락

**증상**

- 백엔드 시작 시 DB, GitHub OAuth, provider 자격 증명 암호화 키 등의 설정 오류가 난다.

**확인**

- `backend/.env.example`과 현재 프로세스 환경 변수 또는 `backend/.env`를 비교한다.
- Spring Boot 자체는 `.env`를 읽지 않고 Gradle `bootRun`만 `backend/.env`를 읽는다.

**조치**

- `backend/.env.example`의 필수 값을 로컬 값으로 채운 `backend/.env`를 사용하거나 프로세스 환경 변수로 설정한다.
- 실제 secret을 저장소에 커밋하지 않는다. 프로세스 환경 변수가 `.env`보다 우선한다.

**완료 확인**

```powershell
cd backend
.\gradlew.bat bootRun
```

- 백엔드가 설정 오류 없이 시작되고 `/internal/health`가 응답하는지 확인한다.
