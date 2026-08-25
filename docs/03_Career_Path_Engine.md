# DevPath Career Path Engine Architecture

- **Document ID:** DevPath-ARCH-CPE-001
- **Version:** 1.0
- **Status:** Draft
- **Related Documents:** `docs/00_Project_Context.md`, `docs/01_SRS.md`, `docs/02_Rule_Engine.md`
- **Date:** 2026-07-20

## 1. Purpose

The Career Path Engine transforms deterministic Rule Engine outputs into structured career-oriented decision objects. It determines career readiness, company readiness, skill gaps, learning priorities, technology priorities, project recommendations, and prompt context facts for downstream systems.

The Career Path Engine does not calculate technical scores, does not call LLMs, does not generate natural language, and does not contain prompt text. It uses Rule Engine outputs as the only source of technical evaluation truth.

The core purpose is to convert:

- Measured skill evidence into career fit signals
- Rule Engine score outputs into readiness classifications
- Weakness and strength facts into structured recommendation objects
- Career and company selections into deterministic weight and priority strategies
- Technical evidence into Prompt Builder context facts

## 2. Scope

### 2.1 In Scope

The Career Path Engine design covers:

- Career Engine architecture
- Career and company workflow
- Input and output models
- Career rule selection
- Company rule selection
- Skill gap analysis
- Learning roadmap generation
- Career readiness classification
- Company readiness classification
- Recommendation prioritization
- Career profiles
- Company profiles
- Functional and non-functional requirements
- Logging, monitoring, and error handling
- Future extension strategy

### 2.2 Out of Scope

The Career Path Engine design does not cover:

- Source code implementation
- API specifications
- Database ERD
- UML diagrams
- LLM orchestration
- Natural-language coaching generation
- Prompt template authoring
- Rule Engine technical score calculation
- Dashboard rendering

### 2.3 Source-of-Truth Constraint

The SRS defines the active v1 career scope as Backend, Frontend, AI Engineer, DevOps, Security, Game, Embedded, Mobile, and Data Engineer. The SRS defines the active v1 company scope as Google, Amazon, Naver, Kakao, Toss, and Coupang.

This document also includes requested additional careers and companies as **future extension candidate profiles**. They are not active production scope unless the SRS is formally updated.

## 3. Design Principles

| Principle | Description | Implication |
|---|---|---|
| Deterministic decisions | Identical Rule Engine output, selected career, selected company, and profile version shall produce identical Career Path Engine output. | No randomization, no LLM calls, no hidden state. |
| Rule Engine authority | Technical scores originate only from the Rule Engine. | Career Path Engine consumes scores but does not recalculate them. |
| Structured outputs only | The engine emits machine-readable recommendation objects. | Natural-language rendering belongs to AI Engine or UI. |
| Configuration-driven profiles | Career and company priorities shall be externalized as versioned profiles. | New or adjusted careers do not require core logic changes. |
| Traceable recommendations | Every recommendation shall reference skill gaps, evidence, career profile, company profile, and Rule Engine output IDs. | Engineers can audit why a recommendation exists. |
| Separation of readiness dimensions | Career readiness and company readiness are related but independent. | A user may be generally career-ready but weak for a selected company emphasis. |
| Measurable guidance | Roadmaps and recommendations shall include priority, difficulty, prerequisites, and completion criteria. | Every output can be tested and displayed. |
| Safe extension | Unsupported careers and companies shall not be silently treated as active. | Extension candidates require SRS and configuration updates. |

## 4. Career Engine Architecture

### 4.1 Architectural Role

The Career Path Engine sits between deterministic technical evaluation and downstream explanation or visualization systems. It receives Rule Engine results and user-selected career/company context, then produces career decision outputs.

### 4.2 Logical Components

| Component | Responsibility |
|---|---|
| Career Invocation Facade | Receives requests and validates selected career, company, and Rule Engine result references. |
| Rule Output Reader | Loads immutable Rule Engine output packages. |
| Career Profile Resolver | Resolves active career profile, required skills, optional skills, priority weights, and roadmap templates. |
| Company Profile Resolver | Resolves active company profile, competency emphasis, and company-specific overrides. |
| Eligibility Validator | Checks whether selected career/company is active, unsupported, or extension-only. |
| Gap Analyzer | Compares current Skill Matrix entries against expected career and company competencies. |
| Readiness Classifier | Converts Rule Engine scores and gap states into deterministic readiness levels. |
| Recommendation Object Builder | Produces structured technology, project, study, certification, architecture, open source, interview, course, book, and portfolio recommendation objects. |
| Roadmap Builder | Orders learning items by prerequisite, priority, difficulty, and estimated duration. |
| Prompt Context Assembler | Builds structured facts for Prompt Builder without prompt text. |
| Conflict Resolver | Detects and resolves conflicting priorities between career and company profiles. |
| Output Assembler | Emits career recommendation, skill gap report, roadmap, company readiness, and prompt context objects. |
| Trace Logger | Records profile versions, decisions, inputs, conflicts, and recommendation rationale IDs. |

### 4.3 Dependency Boundaries

| Dependency | Direction | Usage |
|---|---|---|
| Rule Engine Output Store | Read | Load scores, Skill Matrix, evidence, confidence, and traces. |
| Career Profile Store | Read | Load career requirements, priorities, templates, and profile versions. |
| Company Profile Store | Read | Load company emphasis, weight overrides, and profile versions. |
| User Profile Store | Read | Load selected career, selected company, and user preferences. |
| Career Output Store | Write | Persist structured career outputs and trace. |
| Prompt Builder | Downstream consumer | Receives prompt context facts only. |
| AI Engine | No direct dependency | The Career Path Engine shall not call LLMs. |

### 4.4 Engine Invariants

- The engine shall not calculate technical scores.
- The engine shall not alter Rule Engine scores.
- The engine shall not call the AI Engine.
- The engine shall not generate prose recommendations.
- The engine shall always include profile version metadata.
- The engine shall reject unsupported active selections unless explicitly configured.

## 5. Workflow

### 5.1 Standard Workflow

| Step | Name | Description | Output |
|---:|---|---|---|
| 1 | Request Validation | Validate user, Rule Engine result ID, selected career, selected company, and requested output type. | Accepted or rejected request. |
| 2 | Rule Output Loading | Load Skill Matrix, category scores, evidence references, confidence, and completeness data. | Technical evaluation input. |
| 3 | Career Resolution | Resolve selected career profile and version. | Career profile package. |
| 4 | Company Resolution | Resolve selected company profile and version when selected. | Company profile package. |
| 5 | Eligibility Check | Verify active support status or extension-only status. | Eligibility result. |
| 6 | Gap Analysis | Compare current skills with expected skills. | Skill Gap Report. |
| 7 | Readiness Classification | Classify career readiness and company readiness. | Readiness models. |
| 8 | Recommendation Generation | Build structured recommendation objects. | Recommendation list. |
| 9 | Prioritization | Sort recommendations by severity, impact, dependency, and effort. | Prioritized recommendations. |
| 10 | Roadmap Build | Create ordered milestones and learning sequence. | Learning Roadmap. |
| 11 | Prompt Context Assembly | Convert facts into safe structured prompt context. | Prompt Context object. |
| 12 | Persistence and Trace | Persist output, profile metadata, conflicts, and rationale. | Career evaluation result ID. |

### 5.2 Recalculation Workflow

Career outputs shall be recalculated when:

- A new Rule Engine result is produced.
- The user changes selected career.
- The user changes selected company.
- A career profile version is published.
- A company profile version is published.
- An administrator requests impact analysis.

## 6. Input Models

### 6.1 Career Evaluation Input

| Field | Description | Required |
|---|---|---|
| User ID | Owner of the evaluation. | Yes |
| Rule Evaluation Result ID | Immutable Rule Engine result reference. | Yes |
| Selected Career | User-selected target career. | Yes |
| Selected Company | User-selected target company. | Conditional |
| Career Profile Version | Requested or active career profile version. | Optional |
| Company Profile Version | Requested or active company profile version. | Optional |
| Output Scope | Requested outputs such as readiness, gaps, roadmap, recommendations, or prompt context. | Yes |

### 6.2 Consumed Rule Engine Data

| Rule Engine Data | Career Engine Usage |
|---|---|
| Skill Matrix | Primary input for skill gap analysis and readiness classification. |
| Category Scores | Used as measured facts for career and company readiness. |
| Overall Score | Referenced as a Rule Engine fact; not recalculated. |
| Evidence IDs | Used for traceability and prompt context. |
| Confidence | Used to qualify recommendations and readiness certainty. |
| Completeness Profile | Used to detect insufficient data and missing evidence. |
| Growth Trend | Used for readiness level and learning priority decisions. |
| Rule Version | Persisted for reproducibility. |

### 6.3 Career Profile Input

| Field | Description |
|---|---|
| Career ID | Stable identifier. |
| Career Name | Display name. |
| Status | Active, inactive, or extension candidate. |
| Parent Career | SRS-supported parent when profile is a specialization. |
| Required Competencies | Must-have skill groups. |
| Preferred Competencies | Optional differentiators. |
| Evaluation Categories | Relevant Rule Engine categories. |
| Priority Weights | Career emphasis over categories and competencies. |
| Roadmap Template | Ordered competency development structure. |
| Project Templates | Structured project recommendation patterns. |

### 6.4 Company Profile Input

| Field | Description |
|---|---|
| Company ID | Stable identifier. |
| Company Name | Display name. |
| Status | Active, inactive, or extension candidate. |
| Technology Focus | Generic technology emphasis. |
| Engineering Competencies | Generic competency emphasis. |
| Weight Overrides | Adjustments to career or category priorities. |
| Recommendation Priorities | Types of recommendations emphasized. |
| Profile Version | Version used for traceability. |

## 7. Output Models

### 7.1 Career Evaluation Result

| Field | Description |
|---|---|
| Career Evaluation Result ID | Stable output identifier. |
| User ID | Owner of the result. |
| Rule Evaluation Result ID | Source Rule Engine output. |
| Selected Career | Career used for evaluation. |
| Selected Company | Company used when applicable. |
| Career Profile Version | Career profile version. |
| Company Profile Version | Company profile version. |
| Career Readiness | Deterministic readiness model. |
| Company Readiness | Deterministic company readiness model. |
| Skill Gap Report | Structured gap entries. |
| Recommendation Objects | Prioritized structured recommendations. |
| Learning Roadmap | Ordered milestones and learning sequence. |
| Prompt Context | Structured facts for Prompt Builder. |
| Trace | Decision metadata and rationale references. |

### 7.2 Career Recommendation Object

| Field | Description |
|---|---|
| Recommendation ID | Stable identifier. |
| Type | Technology, project, study, certification, open source, architecture, book, course, interview, portfolio. |
| Target Competency | Competency addressed. |
| Priority | Critical, high, medium, low. |
| Difficulty | Beginner, intermediate, advanced. |
| Estimated Duration | Configured time estimate range. |
| Prerequisites | Required prior skills or milestones. |
| Evidence References | Rule Engine evidence and Skill Matrix IDs. |
| Rationale Code | Machine-readable rationale identifier. |
| Completion Criteria | Measurable condition for completion. |
| Downstream Prompt Facts | Safe facts for AI explanation. |

### 7.3 Skill Gap Report

| Field | Description |
|---|---|
| Current Skill | Skill Matrix entry or missing skill state. |
| Expected Skill | Career or company profile requirement. |
| Gap | Missing, weak, partial, sufficient, strong. |
| Priority | Critical, high, medium, low. |
| Difficulty | Beginner, intermediate, advanced. |
| Estimated Learning Time | Configured duration range. |
| Evidence | Evidence IDs and confidence. |
| Recommendation | Reference to structured recommendation objects. |

### 7.4 Learning Roadmap

| Field | Description |
|---|---|
| Roadmap ID | Stable identifier. |
| Target Career | Career profile. |
| Target Company | Optional company profile. |
| Technology Sequence | Ordered technology learning items. |
| Prerequisites | Required prior milestones. |
| Difficulty | Difficulty per milestone. |
| Estimated Duration | Duration estimate per milestone and total. |
| Milestones | Measurable learning or project checkpoints. |
| Completion Criteria | Deterministic criteria linked to evidence expectations. |

### 7.5 Prompt Context

Prompt Context is not a prompt. It is structured input for the Prompt Builder.

| Field | Description |
|---|---|
| Career Facts | Selected career, readiness level, gaps, strengths, profile version. |
| Company Facts | Selected company, readiness level, missing competencies, profile version. |
| Evidence Facts | Evidence IDs, repository IDs, skill entries, confidence. |
| Recommendation Facts | Recommendation IDs, priorities, completion criteria. |
| Safety Constraints | Flags that remind downstream systems not to calculate scores. |

## 8. Career Rule Selection

### 8.1 Selection Strategy

Career rule selection maps a user-selected career to a career profile and career-specific interpretation policy. It does not execute technical scoring. Technical scoring remains in the Rule Engine.

### 8.2 Active SRS Career Mapping

| SRS Career | Career Path Engine Active Profile |
|---|---|
| Backend | Backend Engineer |
| Frontend | Frontend Engineer |
| AI Engineer | AI Engineer |
| DevOps | DevOps Engineer |
| Security | Security Engineer |
| Game | Game Developer parent profile |
| Embedded | Embedded Engineer |
| Mobile | Mobile Developer parent profile |
| Data Engineer | Data Engineer |

### 8.3 Extension Candidate Mapping

| Requested Career | Status | Relationship to SRS Scope |
|---|---|---|
| Full Stack Engineer | Extension candidate | Combines Backend and Frontend only after SRS update. |
| Machine Learning Engineer | Extension candidate | Specializes AI Engineer after SRS update. |
| Cloud Engineer | Extension candidate | Specializes DevOps after SRS update. |
| Game Client Developer | Extension candidate | Specializes Game after SRS update. |
| Game Server Developer | Extension candidate | Specializes Game and Backend after SRS update. |
| Android Developer | Extension candidate | Specializes Mobile after SRS update. |
| iOS Developer | Extension candidate | Specializes Mobile after SRS update. |
| QA Engineer | Extension candidate | Requires SRS update. |
| System Engineer | Extension candidate | Requires SRS update. |

### 8.4 Rule Selection Output

The engine shall emit:

- Selected career ID
- Active or extension-only status
- Profile version
- Parent career when applicable
- Required competency set
- Preferred competency set
- Category priority weights
- Roadmap template ID

## 9. Company Rule Selection

### 9.1 Selection Strategy

Company rule selection applies generic engineering competency emphasis. It shall not model confidential hiring practices and shall not claim company-specific private knowledge.

### 9.2 Active SRS Company Mapping

| SRS Company | Active Status |
|---|---|
| Google | Active |
| Amazon | Active |
| Naver | Active |
| Kakao | Active |
| Toss | Active |
| Coupang | Active |

### 9.3 Extension Candidate Company Mapping

| Requested Company | Status |
|---|---|
| Meta | Extension candidate pending SRS update. |
| Microsoft | Extension candidate pending SRS update. |
| Netflix | Extension candidate pending SRS update. |
| Line | Extension candidate pending SRS update. |

### 9.4 Company Selection Output

The engine shall emit:

- Selected company ID
- Active or extension-only status
- Profile version
- Competency emphasis
- Weight overrides
- Recommendation priority modifiers
- Unsupported selection warnings when applicable

## 10. Recommendation Strategy

### 10.1 Recommendation Categories

| Category | Purpose | Example Structured Target |
|---|---|---|
| Technology Recommendation | Prioritize missing or weak technologies. | Learn PostgreSQL for backend data persistence evidence. |
| Project Recommendation | Suggest measurable project evidence. | Build a CRUD API with tests and deployment configuration. |
| Study Recommendation | Address conceptual gaps. | Study HTTP, transactions, or CI basics. |
| Certification Recommendation | Suggest optional credential paths when relevant. | Cloud or security fundamentals certification. |
| Open Source Recommendation | Encourage collaboration evidence. | Submit issues, PRs, or documentation improvements. |
| Architecture Recommendation | Improve system structure and design evidence. | Add layered architecture and architecture notes. |
| Book Recommendation | Provide non-mandatory learning resources. | Recommended only as structured resource type, not prose. |
| Course Recommendation | Provide course-type learning resource. | Includes topic, difficulty, and expected completion signal. |
| Interview Preparation Recommendation | Prepare for role/company competency areas. | Generate topic tags for Interview Generator. |
| Portfolio Recommendation | Improve showcase readiness. | Add README, screenshots, deployment link, and impact facts. |

### 10.2 Recommendation Generation Rules

Recommendations shall be generated from:

- Skill gaps
- Weakness flags
- Missing required competencies
- Low-confidence evidence
- Career priority weights
- Company priority overrides
- Roadmap prerequisite ordering

Recommendations shall not be generated from:

- LLM inference
- Unverified user claims
- Unsupported company hiring assumptions
- Unavailable private data

## 11. Skill Gap Analysis

### 11.1 Gap Classification

| Gap State | Meaning |
|---|---|
| Missing | Expected competency has no measurable evidence. |
| Weak | Evidence exists but is below configured threshold. |
| Partial | Evidence satisfies some but not all expected signals. |
| Sufficient | Evidence meets expected threshold. |
| Strong | Evidence exceeds expected threshold and confidence is adequate. |

### 11.1.1 Approved MVP Readiness Policy

`readiness-v1` evaluates Backend and Frontend only. Company weights are not applied. The expected minimum for every
evaluated category is 60. The category bands are Missing = 0, Weak = 1-39.99, Partial = 40-59.99, Sufficient = 60-79.99,
and Strong = 80-100. Backend weights are Language 15%, Framework 20%, Database 20%, Architecture 15%, Testing 20%, and
DevOps 10%. Frontend weights are Language 30%, Framework 30%, Testing 20%, and Documentation 20%.

Readiness score and confidence are calculated independently as career-weighted averages and rounded to two decimal
places using half-up rounding. If a required category is unsupported or unavailable, the result status is
`INSUFFICIENT_EVIDENCE`, readiness score and level are absent, and the unavailable inputs are reported. Gap ordering is
Missing, Weak, Partial, Sufficient, Strong; ties use career weight descending and then category key. Recommendation
priority is not calculated by `readiness-v1`.

Completed assessments are immutable and identified by Skill Matrix, career profile version, and readiness policy
version. Analysis completion creates the assessment after Skill Matrix creation when the user has a supported target
career. The same version tuple is idempotently reused. Read APIs never create or mutate an assessment.

### 11.1.2 Approved MVP Recommendation and Roadmap Policy

`recommendation-v1` consumes immutable `readiness-v1` comparisons for Backend and Frontend only. It emits one
machine-readable recommendation for each Missing, Weak, or Partial category and none for Sufficient or Strong.
Missing is Critical, Weak is High, Partial is Medium, and Low is reserved for future optional competencies. Type
mapping is Language to Study; Framework, Database, Testing, and DevOps to Project; Architecture to Architecture; and
Documentation to Portfolio. Ordering is priority, configured prerequisite order, career weight descending, configured
effort ascending, then category key. Company modifiers and AI prose are not applied.

`roadmap-v1` creates one measurable milestone and step per recommendation. Backend order is Language, Framework,
Database, Architecture, Testing, DevOps; Frontend order is Language, Framework, Testing, Documentation. A step's
completion criteria require a later official category score of at least 60 and its configured evidence outputs.
Recommendation sets are immutable by CareerReadiness and policy version; roadmap structure is immutable, while user
accept/dismiss/complete and step progress states are explicit mutable lifecycle fields.
An `INSUFFICIENT_EVIDENCE` readiness result does not produce recommendations or a roadmap.

### 11.2 Gap Analysis Inputs

- Skill Matrix entries
- Category scores
- Evidence references
- Confidence values
- Career required competencies
- Career preferred competencies
- Company competency emphasis
- Completeness profile

### 11.3 Gap Priority Calculation

Gap priority shall be derived from deterministic factors:

| Factor | Impact |
|---|---|
| Required competency missing | Raises priority. |
| Company emphasis | Raises priority for selected company readiness. |
| Low effort and high impact | Raises short-term priority. |
| Prerequisite dependency | May raise ordering priority. |
| Low confidence | May create evidence-building recommendation. |
| Optional competency missing | Usually medium or low priority. |

### 11.4 Gap Output

Every gap entry shall include current skill, expected skill, gap state, priority, difficulty, estimated learning time, evidence, and recommendation references.

## 12. Learning Roadmap Generation

### 12.1 Roadmap Design

The roadmap is an ordered set of measurable milestones. It is generated from skill gaps and career profile templates, then adjusted by company priorities.

### 12.2 Roadmap Ordering

Roadmap ordering shall consider:

1. Prerequisites
2. Required competencies
3. Company emphasis
4. Skill gap severity
5. Learning difficulty
6. Estimated duration
7. Portfolio value
8. Evidence generation potential

### 12.3 Milestone Model

| Field | Description |
|---|---|
| Milestone ID | Stable milestone identifier. |
| Competency | Target skill or competency. |
| Technology Sequence | Ordered technologies to learn or apply. |
| Prerequisites | Required prior milestones. |
| Difficulty | Beginner, intermediate, advanced. |
| Estimated Duration | Time range from profile configuration. |
| Project Evidence Target | Repository evidence expected after completion. |
| Completion Criteria | Measurable evidence condition. |

## 13. Career Readiness Model

### 13.1 Readiness Levels

| Level | Measurable Criteria |
|---|---|
| Beginner | Required competencies mostly missing; limited repository evidence; low confidence. |
| Junior Ready | Core competencies show measurable evidence; at least basic project, documentation, and activity signals exist. |
| Mid-Level Ready | Multiple required competencies are sufficient; architecture, testing, and maintainability evidence are meaningful. |
| Senior Potential | Strong evidence across core categories; growth, architecture, ownership, and collaboration signals are high. |
| Expert Potential | Broad and deep evidence across complex projects, strong growth, high confidence, and sustained technical maturity. |

### 13.2 Classification Inputs

Career readiness shall use:

- Rule Engine category scores
- Skill Matrix levels
- Required competency gap state
- Preferred competency coverage
- Growth trend
- Activity evidence
- Confidence and completeness

It shall not calculate or alter technical scores.

## 14. Company Readiness Model

### 14.1 Independence from Career Readiness

Company readiness is calculated as a deterministic classification over career readiness, company competency emphasis, and missing company-specific evidence. It may be lower or higher than general career readiness.

### 14.2 Company Readiness Output

| Field | Description |
|---|---|
| Overall Readiness | Company-specific readiness level. |
| Missing Skills | Missing or weak competencies emphasized by the company profile. |
| Missing Experience | Missing project, collaboration, architecture, reliability, or documentation evidence. |
| Recommended Projects | Project recommendation objects relevant to company emphasis. |
| Learning Order | Company-adjusted roadmap ordering. |
| Evidence Confidence | Confidence and completeness indicators. |

### 14.3 Readiness States

| State | Meaning |
|---|---|
| Not Ready | Critical required competencies are missing. |
| Preparing | Foundation exists but major gaps remain. |
| Potentially Ready | Core competencies are sufficient but company-emphasis gaps remain. |
| Ready | Core and company-emphasis competencies are sufficient with adequate confidence. |
| Strong Fit Signal | Strong evidence across career and company-emphasis areas. |

## 15. Recommendation Prioritization

### 15.1 Priority Levels

| Priority | Meaning |
|---|---|
| Critical | Blocks basic readiness for selected career or company. |
| High | Strongly affects readiness or portfolio credibility. |
| Medium | Improves competitiveness or depth. |
| Low | Optional enhancement or long-term differentiation. |

### 15.2 Priority Rules

Recommendations shall be prioritized by:

1. Required competency gaps before optional gaps.
2. Missing evidence before weak evidence when the competency is required.
3. Company-emphasis gaps before general optional improvements.
4. Prerequisite technologies before advanced projects.
5. Portfolio-impacting improvements before low-visibility study items.
6. Low-confidence areas may receive evidence-building recommendations.

### 15.3 Conflict Resolution

When recommendations conflict:

- Required career competencies override optional company differentiators.
- Prerequisites override advanced target projects.
- Lower effort may be prioritized when impact is equal.
- Conflicts shall be logged with deterministic rationale codes.

## 16. Career Profiles

### 16.1 Active and Extension Profile Policy

The profiles below include active SRS-supported profiles and requested extension candidates. Extension candidates are design placeholders only until the SRS is updated.

### 16.2 Career Profile Matrix

| Career | Status | Purpose | Core Technologies | Required Competencies | Preferred Competencies | Evaluation Categories | Priority Weights | Typical Roadmap | Common Skill Gaps | Recommended Projects | Learning Priorities | Portfolio Expectations |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Backend Engineer | Active | Build server-side services and APIs. | Java, Spring Boot, SQL, Redis, Docker. | API design, database integration, testing, architecture, GitHub activity. | Caching, messaging, CI/CD, observability. | Language, Framework, Database, Architecture, Testing, DevOps. | High: backend framework/database/testing; Medium: documentation/collaboration. | Language → framework → database → testing → deployment. | Missing tests, weak database evidence, no deployment. | REST API with PostgreSQL, tests, Docker, CI. | API design, persistence, testing, deployment. | Clear README, API docs, architecture notes, deployment evidence. |
| Frontend Engineer | Active | Build user-facing web applications. | TypeScript, React, TailwindCSS, React Query. | Component structure, state management, accessibility basics, testing. | Performance, design systems, E2E testing. | Language, Framework, Code Organization, Testing, Documentation. | High: frontend framework/code organization; Medium: testing/docs. | TypeScript → React → state/data → testing → portfolio polish. | Weak testing, poor README, shallow component structure. | Dashboard UI with API integration and accessible charts. | React patterns, accessibility, testability. | Screenshots, live demo, UX flow, component documentation. |
| Full Stack Engineer | Extension candidate | Combine frontend and backend project delivery. | TypeScript, React, Java, Spring Boot, SQL. | Frontend, backend, API integration, database, deployment. | Auth, CI/CD, end-to-end testing. | Backend and Frontend parent categories. | Balanced backend/frontend; higher integration evidence. | Frontend → backend → database → auth → deployment. | One side shallow, missing integration tests. | Full-stack product with auth and dashboard. | End-to-end ownership and integration. | End-to-end demo, API docs, deployment, architecture. |
| AI Engineer | Active | Build AI-assisted software systems. | Python, FastAPI, Ollama/OpenAI optional, pgvector. | AI framework use, data handling, prompt context consumption, evaluation awareness. | RAG, vector search, MLOps, LangChain optional. | Language, Framework, Database, Documentation, Architecture. | High: AI framework/data/vector; Medium: docs/testing. | Python → AI framework → data/vector → evaluation → app integration. | No AI project evidence, no data pipeline, weak docs. | RAG prototype with evaluation notes and vector DB. | AI application design, data grounding, evaluation. | Evidence-based AI project README and limitations. |
| Machine Learning Engineer | Extension candidate | Specialize in ML model and data workflows. | Python, ML libraries, notebooks, data tools. | Data preparation, experimentation, model evaluation. | MLOps, monitoring, reproducibility. | AI parent categories plus data/testing. | High: data/evaluation/reproducibility. | Python → data → model → evaluation → deployment. | No experiment tracking, weak evaluation. | Reproducible ML project with metrics. | Data quality and evaluation discipline. | Dataset notes, metrics, reproducibility instructions. |
| Data Engineer | Active | Build data pipelines and storage workflows. | SQL, Python, PostgreSQL, data processing tools. | Data modeling, ETL, database, reliability, documentation. | Streaming, orchestration, warehousing. | Database, DevOps, Architecture, Documentation, Activity. | High: database/data pipeline/devops. | SQL → pipeline → orchestration → reliability → documentation. | No pipeline evidence, weak database design. | ETL pipeline with tests and documentation. | Data modeling, pipeline reliability. | Pipeline diagram, schema docs, run instructions. |
| DevOps Engineer | Active | Build delivery and operational automation. | Docker, GitHub Actions, Nginx, cloud deployment. | CI/CD, containerization, deployment, monitoring basics. | IaC, orchestration, reliability engineering. | DevOps, Testing, Documentation, Activity. | High: DevOps/deployment/docs. | Docker → CI → deployment → monitoring → reliability. | No CI, missing deployment docs. | Dockerized app with GitHub Actions and Nginx. | CI/CD, deployment, operational clarity. | Deployment README, workflow evidence, runbooks. |
| Cloud Engineer | Extension candidate | Specialize in cloud infrastructure and deployment. | Docker, CI/CD, cloud services, networking. | Deployment, environment management, reliability. | IaC, observability, cost awareness. | DevOps parent categories. | High: deployment/infrastructure. | DevOps basics → cloud deployment → monitoring. | Local-only projects, no infra evidence. | Cloud-deployed service with CI/CD. | Infrastructure fundamentals. | Architecture and deployment evidence. |
| Security Engineer | Active | Evaluate and improve secure engineering evidence. | Secure configs, dependency hygiene, auth-related projects. | Security practices, testing, maintainability, documentation. | Static analysis, threat modeling, secure auth. | Security Practices, Testing, Architecture, Documentation. | High: security/testing/maintainability. | Secure coding → dependency hygiene → auth → security docs. | Secrets in repo risk, no security docs. | Auth service with secure config and tests. | Secure defaults, risk awareness. | Security notes, dependency policy, test evidence. |
| Game Client Developer | Extension candidate | Build game client experiences. | C#, C++, Unity/Unreal or game frameworks. | Game loop, rendering/UI, client structure. | Physics, tooling, optimization. | Game parent categories, Language, Code Organization. | High: game framework/project complexity. | Language → engine → gameplay → polish. | Prototype-only evidence, weak organization. | Small playable game with structured code. | Engine fundamentals and client architecture. | Gameplay demo, architecture notes, build instructions. |
| Game Server Developer | Extension candidate | Build backend systems for games. | Java, C++, networking, backend frameworks. | Server logic, networking, database, reliability. | Matchmaking, realtime systems, scaling. | Game and Backend parent categories. | High: backend/database/activity. | Backend → networking → persistence → reliability. | No server-side game evidence. | Multiplayer lobby or game server prototype. | Networking and backend reliability. | API/protocol docs and deployment notes. |
| Embedded Engineer | Active | Build low-level and hardware-adjacent software. | C, C++, embedded toolchains. | Low-level language evidence, code organization, documentation. | RTOS, device documentation, hardware notes. | Language, Code Organization, Maintainability, Documentation. | High: language/code organization/docs. | C/C++ → device basics → structured modules → tests/docs. | No embedded-specific evidence, weak docs. | Sensor data parser or device-control simulation. | Low-level structure and reliability. | Build instructions, hardware assumptions, module docs. |
| Android Developer | Extension candidate | Build Android applications. | Kotlin/Java, Android SDK. | App structure, UI, API integration, testing. | Jetpack, accessibility, release workflow. | Mobile parent categories. | High: mobile framework/code organization. | Kotlin → Android components → API → testing. | No platform-specific structure. | Android app consuming backend API. | Android fundamentals and testability. | Screenshots, APK/build notes, architecture. |
| iOS Developer | Extension candidate | Build iOS applications. | Swift, iOS SDK. | App structure, UI, API integration, testing. | SwiftUI/UIKit depth, accessibility. | Mobile parent categories. | High: mobile framework/code organization. | Swift → iOS components → API → testing. | No platform-specific evidence. | iOS app consuming backend API. | iOS fundamentals and testability. | Screenshots, build notes, architecture. |
| QA Engineer | Extension candidate | Validate software quality through testing strategy. | Test frameworks, CI, automation tools. | Test planning, automation, defect tracking, CI evidence. | E2E testing, performance testing, accessibility testing. | Testing, Documentation, Collaboration. | High: testing/documentation/collaboration. | Test basics → automation → CI → reporting. | No automated tests, weak defect workflow. | Automated test suite for existing app. | Testing strategy and automation. | Test plan, reports, CI evidence. |
| System Engineer | Extension candidate | Work across systems, reliability, and infrastructure. | OS, networking, scripting, deployment tools. | System fundamentals, automation, reliability. | Monitoring, incident response, capacity planning. | DevOps, Architecture, Maintainability. | High: reliability/devops/architecture. | Systems basics → automation → monitoring → reliability. | No systems evidence, weak docs. | Monitoring-enabled service deployment. | Systems thinking and reliability. | Runbooks, diagrams, operational evidence. |

## 17. Company Profiles

### 17.1 Company Profile Policy

Company profiles describe generic engineering competency emphasis only. They do not describe confidential hiring processes, private interview rubrics, or guaranteed success factors.

### 17.2 Company Profile Matrix

| Company | Status | Technology Focus | Engineering Culture | Preferred Competencies | Recommendation Priorities | Skill Emphasis | Company-specific Weight Overrides |
|---|---|---|---|---|---|---|
| Google | Active | General-purpose languages, scalable systems, testing. | Technical depth and engineering rigor. | Architecture, algorithms-adjacent projects, testing, clarity. | Architecture, interview prep, project complexity. | Language depth, architecture, testing. | Increase architecture, testing, project complexity. |
| Amazon | Active | Backend services, databases, DevOps, reliability. | Ownership and operational excellence. | Service reliability, deployment, documentation, backend depth. | Project, DevOps, architecture, portfolio. | Backend, DevOps, activity. | Increase DevOps, database, reliability documentation. |
| Meta | Extension candidate | Large-scale product systems and frontend/backend depth. | Product impact and engineering velocity. | Product engineering, scalability, experimentation evidence. | Project, portfolio, architecture. | Frontend/backend depth and project complexity. | Not active until SRS update. |
| Microsoft | Extension candidate | Cloud, enterprise software, developer platforms. | Maintainability and broad engineering quality. | Cloud, testing, documentation, reliability. | Cloud, project, documentation. | DevOps, maintainability, documentation. | Not active until SRS update. |
| Netflix | Extension candidate | Distributed systems, reliability, data-informed engineering. | Reliability and high-quality service operation. | Observability, backend depth, scalability. | Architecture, DevOps, project. | Reliability, DevOps, architecture. | Not active until SRS update. |
| Naver | Active | Web services, backend/frontend, data/search-adjacent systems. | Product-quality web engineering. | Documentation, maintainability, web service depth. | Portfolio, documentation, project. | Web engineering, documentation, data. | Increase documentation, backend/frontend, data-adjacent signals. |
| Kakao | Active | Product services, web/mobile systems, collaboration. | User-facing product delivery. | Collaboration, reliability, frontend/backend quality. | Portfolio, collaboration, project. | Product engineering, collaboration. | Increase collaboration and service maintainability. |
| Toss | Active | Fintech-adjacent reliability, testing, backend/frontend quality. | Fast iteration with correctness. | Testing, reliability, impact clarity, maintainability. | Testing, portfolio, architecture. | Testing, reliability, impact evidence. | Increase testing, maintainability, reliability. |
| Coupang | Active | Commerce-scale backend, data, DevOps, operations. | Scale and operational discipline. | Scalability, data systems, deployment, activity. | DevOps, data, architecture, project. | Scalability, data, DevOps. | Increase DevOps, data, architecture, activity. |
| Line | Extension candidate | Messaging-scale product systems and mobile/web services. | Product reliability and cross-platform engineering. | Mobile/web, backend reliability, collaboration. | Project, portfolio, DevOps. | Mobile/web, backend, reliability. | Not active until SRS update. |

## 18. Functional Requirements

### CR-001 — Career Selection Resolution

| Field | Specification |
|---|---|
| Description | The engine shall resolve the user's selected career to an active career profile or extension-only status. |
| Inputs | User ID, selected career, career profile catalog. |
| Outputs | Career profile resolution result. |
| Business Rules | Unsupported careers shall not be silently mapped to active profiles. |
| Validation Rules | Career ID shall exist in active or extension catalog. |
| Acceptance Criteria | A valid Backend selection resolves to Backend Engineer active profile with profile version. |
| Dependencies | User Profile Store, Career Profile Store. |

### CR-002 — Career-specific Rule Context Selection

| Field | Specification |
|---|---|
| Description | The engine shall select career-specific competency expectations and priority weights. |
| Inputs | Selected career profile, Rule Engine output. |
| Outputs | Career evaluation context. |
| Business Rules | Career context shall not recalculate technical scores. |
| Validation Rules | Required competencies and weights shall be present for active careers. |
| Acceptance Criteria | Backend profile selects backend-required competencies and priority weights. |
| Dependencies | Career Profile Store, Rule Engine output. |

### CR-003 — Company Selection Resolution

| Field | Specification |
|---|---|
| Description | The engine shall resolve the selected company to an active company profile or extension-only status. |
| Inputs | Selected company, company profile catalog. |
| Outputs | Company profile resolution result. |
| Business Rules | Company profiles shall express generic competencies only. |
| Validation Rules | Active company must be one of SRS-supported companies. |
| Acceptance Criteria | Toss resolves to active profile; Meta resolves to extension candidate unless SRS is updated. |
| Dependencies | Company Profile Store. |

### CR-004 — Company-specific Rule Context Selection

| Field | Specification |
|---|---|
| Description | The engine shall select company-specific competency emphasis and weight overrides. |
| Inputs | Company profile, career context. |
| Outputs | Company evaluation context. |
| Business Rules | Company context shall not claim confidential hiring practices. |
| Validation Rules | Overrides shall reference valid competencies and categories. |
| Acceptance Criteria | Coupang increases data, DevOps, and scalability emphasis in company readiness context. |
| Dependencies | Company Profile Store, Career Profile Store. |

### CR-005 — Skill Gap Analysis

| Field | Specification |
|---|---|
| Description | The engine shall compare current Skill Matrix entries with career and company expectations. |
| Inputs | Skill Matrix, career profile, company profile, confidence model. |
| Outputs | Skill Gap Report. |
| Business Rules | Gaps shall be based only on Rule Engine evidence and configured expectations. |
| Validation Rules | Every non-missing current skill shall reference a Skill Matrix entry. |
| Acceptance Criteria | Missing required database evidence for Backend produces a high-priority gap. |
| Dependencies | Rule Engine output, Career Profile Store, Company Profile Store. |

### CR-006 — Learning Roadmap Generation

| Field | Specification |
|---|---|
| Description | The engine shall generate a structured roadmap from gaps, prerequisites, priorities, and profile templates. |
| Inputs | Skill gaps, career roadmap template, company emphasis. |
| Outputs | Learning Roadmap. |
| Business Rules | Roadmap entries shall be structured objects, not natural-language coaching. |
| Validation Rules | Roadmap milestones shall include difficulty, duration, prerequisites, and completion criteria. |
| Acceptance Criteria | A Backend roadmap orders language/framework basics before deployment and advanced architecture. |
| Dependencies | Skill Gap Report, Career Profile Store. |

### CR-007 — Career Readiness Classification

| Field | Specification |
|---|---|
| Description | The engine shall classify career readiness using Rule Engine scores, skill levels, gaps, confidence, and completeness. |
| Inputs | Rule Engine output, Skill Gap Report, career profile. |
| Outputs | Career Readiness model. |
| Business Rules | The engine shall classify readiness without recalculating technical scores. |
| Validation Rules | Readiness classification shall reference required competency coverage. |
| Acceptance Criteria | Missing multiple required competencies prevents Junior Ready classification. |
| Dependencies | Rule Engine output, Career Profile Store. |

### CR-008 — Company Readiness Classification

| Field | Specification |
|---|---|
| Description | The engine shall classify company readiness independently from general career readiness. |
| Inputs | Career readiness, company profile, skill gaps, evidence confidence. |
| Outputs | Company Readiness model. |
| Business Rules | Company readiness shall use generic competency emphasis only. |
| Validation Rules | Active company profile shall be required for active company readiness output. |
| Acceptance Criteria | A user may be Junior Ready for Backend but Preparing for Google if architecture/testing emphasis gaps remain. |
| Dependencies | Company Profile Store, Skill Gap Report. |

### CR-009 — Recommendation Object Generation

| Field | Specification |
|---|---|
| Description | The engine shall generate structured recommendation objects for supported recommendation categories. |
| Inputs | Skill gaps, readiness models, career profile, company profile. |
| Outputs | Recommendation objects. |
| Business Rules | Recommendations shall be machine-readable and shall not contain LLM-generated prose. |
| Validation Rules | Every recommendation shall include type, priority, rationale code, evidence, and completion criteria. |
| Acceptance Criteria | A testing gap produces a testing-related project or study recommendation object. |
| Dependencies | Skill Gap Report, profile catalogs. |

### CR-010 — Recommendation Prioritization

| Field | Specification |
|---|---|
| Description | The engine shall prioritize recommendations by required competency, severity, company emphasis, dependency, effort, and impact. |
| Inputs | Recommendation objects, gap severity, profile priorities. |
| Outputs | Ordered recommendation list. |
| Business Rules | Critical required gaps shall outrank optional differentiators. |
| Validation Rules | Priority values shall use the configured enumeration. |
| Acceptance Criteria | Missing Backend database competency outranks optional open-source recommendation. |
| Dependencies | Recommendation Object Builder. |

### CR-011 — Technology Priority Generation

| Field | Specification |
|---|---|
| Description | The engine shall identify technology priorities from missing or weak competencies. |
| Inputs | Skill Matrix, career profile technologies, company emphasis. |
| Outputs | Technology priority objects. |
| Business Rules | Technology priorities shall require configured career relevance. |
| Validation Rules | Technology IDs shall map to taxonomy entries. |
| Acceptance Criteria | Backend selection prioritizes SQL/database evidence when missing. |
| Dependencies | Skill taxonomy, technology taxonomy. |

### CR-012 — Project Recommendation Generation

| Field | Specification |
|---|---|
| Description | The engine shall generate structured project recommendations that can create measurable evidence. |
| Inputs | Skill gaps, portfolio expectations, career profile. |
| Outputs | Project recommendation objects. |
| Business Rules | Project recommendations shall include expected evidence outputs. |
| Validation Rules | Each project recommendation shall include target competencies and completion criteria. |
| Acceptance Criteria | A DevOps gap produces a Docker/CI/deployment project recommendation object. |
| Dependencies | Career Profile Store, Skill Gap Report. |

### CR-013 — Prompt Context Assembly

| Field | Specification |
|---|---|
| Description | The engine shall assemble structured facts for the Prompt Builder. |
| Inputs | Career evaluation result, gaps, recommendations, evidence references. |
| Outputs | Prompt Context object. |
| Business Rules | Prompt Context shall not include prompt text or AI instructions beyond safety facts. |
| Validation Rules | Prompt Context shall reference source IDs and score provenance. |
| Acceptance Criteria | Prompt Context includes career, company, gaps, strengths, evidence IDs, and no calculated new scores. |
| Dependencies | Career output models, Rule Engine output. |

### CR-014 — Conflict Detection

| Field | Specification |
|---|---|
| Description | The engine shall detect conflicting recommendation priorities between career and company profiles. |
| Inputs | Career priorities, company priorities, recommendation candidates. |
| Outputs | Conflict trace and resolved priorities. |
| Business Rules | Required career competencies override optional company differentiators. |
| Validation Rules | Conflicts shall include deterministic rationale codes. |
| Acceptance Criteria | Conflicting optional priorities are resolved and logged without nondeterminism. |
| Dependencies | Career Profile Store, Company Profile Store. |

### CR-015 — Insufficient Data Handling

| Field | Specification |
|---|---|
| Description | The engine shall detect insufficient Rule Engine evidence and produce low-confidence outputs. |
| Inputs | Completeness profile, confidence model, Skill Matrix. |
| Outputs | Data sufficiency status and evidence-building recommendations. |
| Business Rules | Missing evidence shall not be treated as user weakness when data is inaccessible. |
| Validation Rules | Inaccessible and missing data states shall remain distinct. |
| Acceptance Criteria | Private repository access limitations reduce confidence and create evidence-collection guidance. |
| Dependencies | Rule Engine completeness model. |

### CR-016 — Output Persistence

| Field | Specification |
|---|---|
| Description | The engine shall persist career evaluation output with profile versions and source references. |
| Inputs | Assembled career output package. |
| Outputs | Career evaluation result ID. |
| Business Rules | Historical outputs shall be reproducible from referenced versions. |
| Validation Rules | Required metadata shall be present before persistence. |
| Acceptance Criteria | Persisted output includes Rule Engine result ID, career profile version, and company profile version when applicable. |
| Dependencies | Career Output Store. |

### CR-017 — Recalculation Trigger Handling

| Field | Specification |
|---|---|
| Description | The engine shall support recalculation when Rule Engine outputs or selections change. |
| Inputs | Trigger event, user profile, latest Rule Engine result. |
| Outputs | New career evaluation result. |
| Business Rules | Recalculation shall not mutate prior historical outputs. |
| Validation Rules | Trigger source shall be recorded. |
| Acceptance Criteria | Changing target company creates a new company readiness result. |
| Dependencies | User Profile Store, Rule Engine Output Store. |

### CR-018 — Profile Version Validation

| Field | Specification |
|---|---|
| Description | The engine shall validate career and company profile versions before evaluation. |
| Inputs | Requested profile version, active profile catalog. |
| Outputs | Validated profile package or error. |
| Business Rules | Deprecated profile versions shall require explicit historical mode. |
| Validation Rules | Profile schema and required competencies shall be valid. |
| Acceptance Criteria | Invalid profile configuration blocks evaluation with a fatal validation error. |
| Dependencies | Profile stores, configuration validator. |

### CR-019 — Extension Candidate Guardrail

| Field | Specification |
|---|---|
| Description | The engine shall prevent extension candidate careers or companies from being used as active production selections unless enabled by SRS-aligned configuration. |
| Inputs | Selected career, selected company, profile status. |
| Outputs | Eligibility result and warning or rejection. |
| Business Rules | SRS-supported scope controls active production availability. |
| Validation Rules | Extension candidate status shall be explicit. |
| Acceptance Criteria | Meta company selection is rejected or marked extension-only in v1 active mode. |
| Dependencies | Career and Company Profile Stores. |

### CR-020 — Audit Trace Generation

| Field | Specification |
|---|---|
| Description | The engine shall generate audit trace metadata for readiness, gap, roadmap, and recommendation decisions. |
| Inputs | All evaluation inputs and outputs. |
| Outputs | Audit trace. |
| Business Rules | Every decision shall be traceable to Rule Engine output and profile versions. |
| Validation Rules | Trace shall include source IDs, rationale codes, and timestamp. |
| Acceptance Criteria | A recommendation can be traced back to gap entry, Skill Matrix entry, profile version, and Rule Engine result ID. |
| Dependencies | Trace Logger, Career Output Store. |

## 19. Non-functional Requirements

| ID | Category | Requirement | Measurement |
|---|---|---|---|
| CPE-NFR-001 | Performance | Career evaluation shall complete within configured SLA for typical Rule Engine outputs. | 95th percentile latency is tracked by output size. |
| CPE-NFR-002 | Reliability | Missing optional company selection shall not prevent career readiness output. | Career readiness succeeds without company profile. |
| CPE-NFR-003 | Scalability | The engine shall support asynchronous recalculation after Rule Engine updates. | Recalculation jobs can be queued. |
| CPE-NFR-004 | Maintainability | Career and company profiles shall be configuration-driven and versioned. | Profile changes do not require core logic changes. |
| CPE-NFR-005 | Configurability | Weights, priorities, roadmap templates, and recommendation mappings shall be externalized. | Active profile version determines behavior. |
| CPE-NFR-006 | Logging | Evaluation lifecycle, profile resolution, gap generation, and conflicts shall be logged. | Logs include correlation IDs and no secrets. |
| CPE-NFR-007 | Monitoring | Metrics shall expose readiness distribution, gap frequency, and recommendation volume. | Operators can query metrics by career/company. |
| CPE-NFR-008 | Observability | Outputs shall include trace metadata sufficient for debugging. | Every recommendation includes rationale references. |
| CPE-NFR-009 | Extensibility | New careers and companies shall be added through profile catalogs after SRS approval. | Extension candidates can be promoted through versioned configuration. |
| CPE-NFR-010 | Determinism | Identical inputs and profile versions shall produce identical outputs. | Regression fixtures compare full output packages. |

## 20. Logging

### 20.1 Log Categories

| Category | Logged Fields |
|---|---|
| Evaluation Lifecycle | Request ID, user ID, status, start time, end time. |
| Profile Resolution | Career ID, company ID, profile versions, active/extension status. |
| Gap Analysis | Gap count, critical gap count, missing required competencies. |
| Readiness Classification | Career readiness, company readiness, confidence state. |
| Recommendation Generation | Recommendation count by type and priority. |
| Conflict Resolution | Conflict ID, selected resolution rule, affected recommendations. |
| Error Handling | Error code, severity, recoverability, affected output. |

### 20.2 Logging Constraints

- Logs shall not include OAuth tokens.
- Logs shall not include raw private repository content.
- Logs shall use identifiers and summaries where possible.
- Logs shall preserve enough trace data for audit and regression.

## 21. Monitoring

### 21.1 Metrics

| Metric | Purpose |
|---|---|
| Career evaluation count | Track usage volume. |
| Evaluation latency | Monitor performance. |
| Career distribution | Understand selected career demand. |
| Company distribution | Understand selected company demand. |
| Critical gap frequency | Identify common readiness blockers. |
| Recommendation type distribution | Monitor recommendation strategy output. |
| Unsupported selection count | Track demand for extension candidates. |
| Low-confidence output rate | Detect insufficient data patterns. |
| Conflict resolution count | Detect profile tuning issues. |

### 21.2 Alerts

Alerts should be configured for:

- Profile resolution failure spike
- Missing Rule Engine output spike
- Unsupported selection spike
- Abnormal recommendation volume changes
- High low-confidence output rate
- Evaluation latency SLA breach

## 22. Error Handling

### 22.1 Error Scenarios

| Scenario | Handling |
|---|---|
| No selected career | Return validation error and require career selection before readiness evaluation. |
| No selected company | Produce career readiness and omit company readiness or mark it not requested. |
| Insufficient repository data | Produce low-confidence output and evidence-building recommendation objects. |
| Missing Rule Engine outputs | Reject evaluation because technical source of truth is unavailable. |
| Unsupported technologies | Preserve unsupported technology facts and avoid unsupported recommendations. |
| Conflicting recommendations | Apply deterministic conflict resolution and log rationale. |
| Extension candidate selected | Reject in active mode or emit extension-only status depending on caller policy. |
| Invalid profile configuration | Stop evaluation and mark fatal configuration error. |

### 22.2 Severity Model

| Severity | Meaning | Behavior |
|---|---|---|
| Info | Non-blocking condition. | Continue and log. |
| Warning | Output can be produced with caveats. | Continue with warning and confidence impact. |
| Recoverable Error | One output section cannot be produced. | Produce partial output if valid. |
| Fatal Error | Evaluation cannot be trusted. | Stop and persist failure state. |

## 23. Future Extension

Future extension shall follow SRS change control and profile version governance.

Potential extensions include:

- Promoting Full Stack Engineer after SRS update.
- Splitting AI Engineer into AI Engineer and Machine Learning Engineer after SRS update.
- Splitting Mobile into Android and iOS after SRS update.
- Splitting Game into Game Client and Game Server after SRS update.
- Adding Cloud Engineer, QA Engineer, and System Engineer after SRS update.
- Adding Meta, Microsoft, Netflix, and Line company profiles after SRS update.
- Adding richer roadmap templates by project type.
- Adding organization-level benchmark profiles.
- Adding career transition comparison outputs.
- Adding profile simulation and impact analysis for administrators.

All future extensions shall preserve the core constraints: the Career Path Engine shall remain deterministic, shall use Rule Engine outputs only, shall not calculate technical scores, shall not call LLMs, and shall emit structured recommendation objects only.
