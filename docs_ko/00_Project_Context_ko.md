<!--
한국어판 문서입니다. 원본 기준 문서: docs/00_Project_Context.md
요구사항 ID, 파일 경로, 기술명, 엔진명, 스키마명은 추적성을 위해 원문 표기를 일부 유지합니다.
-->

# DevPath 프로젝트 컨텍스트

## 1. 프로젝트명

DevPath

## 2. 프로젝트 비전

DevPath는 개발자의 GitHub 저장소와 Notion 워크스페이스를 분석하여 기술 역량, 개발 이력, 프로젝트 품질, 성장 추세, 커리어 준비도를 평가하는 AI 기반 개발자 커리어 인텔리전스 플랫폼이다.

기존 GitHub 분석 서비스가 통계 시각화에 머무르는 것과 달리, DevPath는 AI 처리 전에 규칙 기반 엔지니어링 분석을 수행하고 사용자의 목표 커리어와 목표 회사에 맞춘 개인화된 추천을 제공한다.

이 플랫폼은 개발자의 지속적인 커리어 성장을 지원하는 장기적인 Developer Operating System으로 발전하는 것을 목표로 한다.

## 3. 핵심 철학

1. Rule Engine은 계산한다.
2. AI는 설명한다.
3. 커리어가 바뀌면 평가도 바뀐다.
4. 회사가 바뀌면 추천도 바뀐다.
5. 모든 것은 측정 가능해야 한다.

LLM은 절대 점수를 계산해서는 안 된다. 모든 점수는 오직 Rule Engine에서만 산출된다.

## 4. 대상 사용자

- 컴퓨터공학 전공 학생
- 주니어 개발자
- 커리어 전환자
- 포트폴리오 준비자
- 면접을 준비하는 개발자
- 커리어 성장을 원하는 개발자

## 5. 주요 목표

시스템은 enable users to:

- Analyze GitHub repositories
- Analyze Notion documentation
- Evaluate technical skills
- Measure growth
- Identify weak areas
- Generate learning roadmap
- Generate portfolio
- Generate resume
- Generate README improvements
- Generate interview questions
- Receive AI career coaching

## 6. Functional Modules

### 6.1 사용자 Management

- GitHub OAuth Login
- 사용자 Profile
- 커리어 Selection
- 회사 Selection
- Settings

### 6.2 GitHub Integration

- OAuth
- 저장소 List
- 저장소 Synchronization
- Commit Analysis
- Branch Analysis
- Pull Request Analysis
- Issue Analysis
- README Analysis
- Dependency Analysis
- Directory Analysis

### 6.3 Notion Integration

- OAuth
- Workspace Connection
- Retrospective Analysis
- Documentation Analysis
- Learning Notes Analysis
- Project Notes Analysis

### 6.4 Data Collection

- GitHub Collector
- Notion Collector
- Data Normalizer
- Cache
- Synchronization

### 6.5 Rule Engine

Rule Engine은 DevPath에서 가장 중요한 핵심 구성요소이다.

Responsibilities:

- Language Analysis
- Framework Analysis
- Database Analysis
- 아키텍처 Analysis
- Testing Analysis
- DevOps Analysis
- Documentation Analysis
- Collaboration Analysis

출력:

- Skill Matrix
- Activity Score
- Growth Score
- 아키텍처 Score
- Documentation Score
- Overall Score

### 6.6 커리어 Path Engine

Supported careers:

- Backend
- Frontend
- AI Engineer
- DevOps
- 보안
- Game
- Embedded
- Mobile
- Data Engineer

Responsibilities:

- Select Rule Set
- Select Prompt
- Select Recommendation
- Generate 커리어 Roadmap

### 6.7 회사 Engine

Supported companies:

- Google
- Amazon
- Naver
- Kakao
- Toss
- Coupang

Responsibilities:

- 회사별 weights
- 회사별 recommendations
- 회사별 interview questions

### 6.8 AI Engine

Modules:

- Prompt Builder
- Skill Analyzer
- 저장소 Reviewer
- 커리어 Coach
- Portfolio Writer
- Resume Writer
- README Generator
- Interview Generator
- Learning Planner

### 6.9 Dashboard

- Skill Matrix
- Growth Graph
- Activity Graph
- 기술 Distribution
- AI Recommendation Cards
- 커리어 Progress
- 회사 Readiness

### 6.10 Administration

- Rule Management
- 커리어 Management
- 회사 Rule Management
- Prompt Management
- Logs
- Statistics

## 7. 요구사항 Ranges

- FR-001 ~ FR-020: 사용자 Management
- FR-021 ~ FR-050: GitHub Integration
- FR-051 ~ FR-070: Notion Integration
- FR-071 ~ FR-100: Data Collection
- FR-101 ~ FR-180: Rule Engine
- FR-181 ~ FR-220: 커리어 Path Engine
- FR-221 ~ FR-280: AI Engine
- FR-281 ~ FR-320: Dashboard
- FR-321 ~ FR-340: Search
- FR-341 ~ FR-360: Administration

## 8. Rule Requirements

- RR-001 Language Analysis
- RR-002 Framework Analysis
- RR-003 Database Analysis
- RR-004 아키텍처 Analysis
- RR-005 Testing Analysis
- RR-006 DevOps Analysis
- RR-007 Documentation Analysis
- RR-008 Collaboration Analysis
- RR-009 Skill Matrix Generation
- RR-010 Overall Score Calculation

## 9. 커리어 Requirements

- CR-001 커리어 Selection
- CR-002 커리어별 Rules
- CR-003 회사 Selection
- CR-004 회사별 Rules
- CR-005 Skill Gap Analysis
- CR-006 Learning Roadmap

## 10. AI Requirements

- AI-001 Prompt Builder
- AI-002 저장소 Summary
- AI-003 Skill Analysis
- AI-004 저장소 Review
- AI-005 Portfolio Generation
- AI-006 Resume Generation
- AI-007 Interview Generation
- AI-008 Learning Planner

## 11. Prompt Requirements

- PR-001 시스템 Prompt
- PR-002 커리어 Prompt
- PR-003 회사 Prompt
- PR-004 Rule Prompt
- PR-005 Output Format

## 12. 비기능 요구사항

- 보안
- Scalability
- Performance
- Maintainability
- Reliability
- 모니터링
- 로깅
- Backup
- Accessibility

## 13. Technical Stack

### 13.1 Frontend

- React
- TypeScript
- TailwindCSS
- React Query

### 13.2 Backend

- Spring Boot
- Spring 보안
- PostgreSQL
- Redis

### 13.3 AI

- FastAPI
- Ollama
- OpenAI API optional
- LangChain optional
- pgvector

### 13.4 DevOps

- Docker
- GitHub Actions
- Nginx
- Oracle Cloud Free

## 14. Important Rules

- AI는 프로젝트 기능을 임의로 만들어내서는 안 된다.
- AI는 절대 점수를 계산해서는 안 된다.
- 모든 요구사항은 측정 가능해야 한다.
- 모든 요구사항은 고유 ID를 가져야 한다.
- SRS는 IEEE 29148을 따라야 한다.
- 문서는 엔터프라이즈 소프트웨어 개발에 적합한 전문적인 Markdown 형식으로 작성되어야 한다.



