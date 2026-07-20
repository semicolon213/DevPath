# DevPath Project Context

## 1. Project Name

DevPath

## 2. Project Vision

DevPath is an AI-powered Developer Career Intelligence Platform that analyzes a developer's GitHub repositories and Notion workspace to evaluate technical skills, development history, project quality, growth trends, and career readiness.

Unlike existing GitHub analytics services that only visualize statistics, DevPath performs rule-based engineering analysis before AI processing and provides personalized recommendations based on the user's target career and company.

The platform is intended to become a long-term Developer Operating System that supports continuous career development.

## 3. Core Philosophy

1. Rule Engine calculates.
2. AI explains.
3. Career changes evaluation.
4. Company changes recommendation.
5. Everything must be measurable.

The LLM must never calculate scores. All scores are produced only by the Rule Engine.

## 4. Target Users

- Computer Science Students
- Junior Developers
- Career Changers
- Portfolio Builders
- Developers preparing for interviews
- Developers seeking career growth

## 5. Main Goals

The system shall enable users to:

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

### 6.1 User Management

- GitHub OAuth Login
- User Profile
- Career Selection
- Company Selection
- Settings

### 6.2 GitHub Integration

- OAuth
- Repository List
- Repository Synchronization
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

The Rule Engine is the most important component.

Responsibilities:

- Language Analysis
- Framework Analysis
- Database Analysis
- Architecture Analysis
- Testing Analysis
- DevOps Analysis
- Documentation Analysis
- Collaboration Analysis

Outputs:

- Skill Matrix
- Activity Score
- Growth Score
- Architecture Score
- Documentation Score
- Overall Score

### 6.6 Career Path Engine

Supported careers:

- Backend
- Frontend
- AI Engineer
- DevOps
- Security
- Game
- Embedded
- Mobile
- Data Engineer

Responsibilities:

- Select Rule Set
- Select Prompt
- Select Recommendation
- Generate Career Roadmap

### 6.7 Company Engine

Supported companies:

- Google
- Amazon
- Naver
- Kakao
- Toss
- Coupang

Responsibilities:

- Company-specific weights
- Company-specific recommendations
- Company-specific interview questions

### 6.8 AI Engine

Modules:

- Prompt Builder
- Skill Analyzer
- Repository Reviewer
- Career Coach
- Portfolio Writer
- Resume Writer
- README Generator
- Interview Generator
- Learning Planner

### 6.9 Dashboard

- Skill Matrix
- Growth Graph
- Activity Graph
- Technology Distribution
- AI Recommendation Cards
- Career Progress
- Company Readiness

### 6.10 Administration

- Rule Management
- Career Management
- Company Rule Management
- Prompt Management
- Logs
- Statistics

## 7. Requirement Ranges

- FR-001 ~ FR-020: User Management
- FR-021 ~ FR-050: GitHub Integration
- FR-051 ~ FR-070: Notion Integration
- FR-071 ~ FR-100: Data Collection
- FR-101 ~ FR-180: Rule Engine
- FR-181 ~ FR-220: Career Path Engine
- FR-221 ~ FR-280: AI Engine
- FR-281 ~ FR-320: Dashboard
- FR-321 ~ FR-340: Search
- FR-341 ~ FR-360: Administration

## 8. Rule Requirements

- RR-001 Language Analysis
- RR-002 Framework Analysis
- RR-003 Database Analysis
- RR-004 Architecture Analysis
- RR-005 Testing Analysis
- RR-006 DevOps Analysis
- RR-007 Documentation Analysis
- RR-008 Collaboration Analysis
- RR-009 Skill Matrix Generation
- RR-010 Overall Score Calculation

## 9. Career Requirements

- CR-001 Career Selection
- CR-002 Career-specific Rules
- CR-003 Company Selection
- CR-004 Company-specific Rules
- CR-005 Skill Gap Analysis
- CR-006 Learning Roadmap

## 10. AI Requirements

- AI-001 Prompt Builder
- AI-002 Repository Summary
- AI-003 Skill Analysis
- AI-004 Repository Review
- AI-005 Portfolio Generation
- AI-006 Resume Generation
- AI-007 Interview Generation
- AI-008 Learning Planner

## 11. Prompt Requirements

- PR-001 System Prompt
- PR-002 Career Prompt
- PR-003 Company Prompt
- PR-004 Rule Prompt
- PR-005 Output Format

## 12. Non-functional Requirements

- Security
- Scalability
- Performance
- Maintainability
- Reliability
- Monitoring
- Logging
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
- Spring Security
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

- The AI must never invent project functionality.
- The AI must never calculate scores.
- Every requirement must be measurable.
- Every requirement must have an ID.
- The SRS shall follow IEEE 29148.
- The document shall be written in professional Markdown suitable for enterprise software development.
