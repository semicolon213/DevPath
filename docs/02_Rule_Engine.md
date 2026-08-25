# DevPath Rule Engine Architecture

- **Document ID:** DevPath-ARCH-RE-001
- **Version:** 1.0
- **Status:** Draft
- **Related Documents:** `docs/00_Project_Context.md`, `docs/01_SRS.md`
- **Date:** 2026-07-20

## 1. Purpose

The purpose of this document is to define the production-ready architecture of the DevPath Rule Engine. The Rule Engine is the deterministic business evaluation core of DevPath. It evaluates developer activity, repository quality, technical skill evidence, growth signals, and career readiness inputs using normalized GitHub and Notion data.

The Rule Engine produces structured outputs consumed by the Career Path Engine, Prompt Builder, AI Engine, Dashboard, and future recommendation components. The Rule Engine does not generate natural-language coaching, does not call any LLM, does not contain prompt logic, and does not create recommendations directly.

The most important architectural constraint is:

> All scores are calculated only by the Rule Engine. The LLM must never calculate, infer, modify, or invent scores.

## 2. Scope

### 2.1 In Scope

The Rule Engine design covers:

- Rule Engine component architecture
- Deterministic evaluation workflow
- Input data expectations
- Normalization requirements
- Evaluation categories
- Score calculation model
- Weight management
- Rule inheritance and override strategy
- Rule priority and execution order
- Rule configuration concepts
- Rule versioning
- Career-specific rule selection
- Company-specific weight strategies
- Skill Matrix generation
- Output model design
- Error handling, logging, monitoring, and extensibility

### 2.2 Out of Scope

The Rule Engine design does not cover:

- Source code implementation
- API specification
- Database ERD
- UML diagrams
- Prompt engineering implementation
- LLM orchestration
- AI-generated recommendations
- Frontend dashboard implementation
- Career Path Engine implementation details beyond rule selection contracts

### 2.3 Authoritative Source Alignment

If this document conflicts with the SRS, the SRS takes precedence. This document assumes the SRS requirement IDs `FR-101` through `FR-180` and `RR-001` through `RR-010` are authoritative for Rule Engine scope.

## 3. Design Principles

| Principle | Description | Engineering Implication |
|---|---|---|
| Determinism | Identical normalized input, rule configuration, and rule version shall produce identical output. | No random logic, no LLM calls, no time-dependent scoring except explicit input timestamps. |
| Configuration-driven behavior | Business scoring logic shall not be hardcoded. | Rules, weights, thresholds, priorities, and overrides are externalized as versioned configuration. |
| Explainable scoring | Every score shall be traceable to rule IDs, evidence IDs, weights, and formula versions. | Outputs include calculation trace and evidence references. |
| Separation of concerns | Rule Engine calculates; AI explains; Career Path Engine selects context. | No prompt logic or recommendation text in Rule Engine. |
| Measurability | Every evaluation must produce measurable signals or explicit missing-data states. | Missing README is a measurable absence, not an implicit zero unless configured. |
| Version safety | Rule changes shall not silently alter historical outputs. | Rule version and input snapshot ID are persisted with every output. |
| Fail-safe evaluation | Partial failures shall not create false confidence. | Outputs include completeness, confidence, warnings, and skipped-rule metadata. |
| Extensibility | New categories, careers, companies, and rules shall be added through configuration and adapters. | Core execution remains stable while rule catalogs evolve. |

## 4. Rule Engine Architecture

### 4.1 Architectural Role

The Rule Engine is a backend domain component that receives normalized snapshots, applies versioned rule sets, calculates scores, generates skill matrices, and emits structured evaluation results. It is invoked after data collection and normalization have completed.

### 4.2 Logical Components

| Component | Responsibility |
|---|---|
| Rule Invocation Facade | Receives evaluation requests from backend services and validates requested scope. |
| Input Snapshot Loader | Loads immutable normalized GitHub and Notion data snapshots. |
| Rule Catalog Resolver | Resolves active rule configuration by rule version, career, company, and evaluation type. |
| Rule Dependency Resolver | Determines rule dependencies, prerequisites, and execution graph. |
| Rule Executor | Executes deterministic rule conditions and score formulas. |
| Evidence Resolver | Maps normalized records to evidence objects used by rules. |
| Weight Resolver | Applies base, career-specific, and company-specific weights. |
| Score Aggregator | Aggregates raw rule outputs into category scores, component scores, and overall score. |
| Confidence Calculator | Calculates confidence based on evidence completeness, freshness, and reliability. |
| Skill Matrix Builder | Converts evaluated signals into skill matrix entries. |
| Output Assembler | Produces structured outputs for downstream systems. |
| Trace Logger | Records rule execution trace, skipped rules, warnings, errors, and formula metadata. |

### 4.3 Dependency Boundaries

| Dependency | Direction | Rule Engine Usage |
|---|---|---|
| Normalized Data Store | Read | Load immutable input snapshots. |
| Rule Configuration Store | Read | Load versioned rule sets and weights. |
| Rule Output Store | Write | Persist evaluation output, traces, and score records. |
| Audit Logger | Write | Record evaluation lifecycle events. |
| Metrics System | Write | Publish latency, throughput, failure, and coverage metrics. |
| AI Engine | None | The Rule Engine shall not call AI services. |
| Prompt Builder | None | The Rule Engine shall not construct prompts. |

### 4.4 Execution Boundary

The Rule Engine may be implemented as an internal Spring Boot domain module, a separate backend service, or a worker component. Regardless of deployment style, the execution contract shall remain deterministic and side-effect controlled:

1. Read immutable input snapshot.
2. Read immutable rule version.
3. Execute deterministic rules.
4. Persist structured output and trace.
5. Return output identifier and summary status.

## 5. Evaluation Pipeline

### 5.1 Pipeline Stages

| Stage | Name | Description | Output |
|---:|---|---|---|
| 1 | Request Validation | Validate user, repository scope, career, company, and rule version. | Accepted or rejected evaluation request. |
| 2 | Snapshot Loading | Load normalized GitHub and Notion snapshots. | Immutable input bundle. |
| 3 | Completeness Assessment | Determine available and missing data sources. | Data completeness profile. |
| 4 | Rule Catalog Resolution | Select base, career, and company rule configuration. | Effective rule set. |
| 5 | Rule Ordering | Sort rules by dependencies, category, and priority. | Executable rule plan. |
| 6 | Evidence Resolution | Bind normalized records to rule evidence queries. | Evidence map. |
| 7 | Rule Execution | Evaluate conditions, thresholds, and formulas. | Raw rule results. |
| 8 | Score Aggregation | Calculate category, skill, component, and overall scores. | Aggregated score model. |
| 9 | Confidence Calculation | Calculate confidence and evidence strength. | Confidence model. |
| 10 | Skill Matrix Build | Produce skill-level entries from rules and evidence. | Skill Matrix. |
| 11 | Output Assembly | Assemble downstream-ready structured result. | Rule Engine output package. |
| 12 | Persistence and Trace | Store result, trace, warnings, and metadata. | Evaluation result ID. |

### 5.2 Pipeline Invariants

- The selected rule version shall remain constant during a single evaluation run.
- The input snapshot shall remain immutable during execution.
- Category scores shall be calculated before overall score.
- Overall score shall never be calculated if required component score dependencies are invalid unless the rule version defines a partial-score policy.
- Every executed rule shall emit one of: `PASSED`, `FAILED`, `PARTIAL`, `SKIPPED`, or `ERROR`.
- Every score shall include its formula version and contributing rule IDs.

## 6. Input Data Model

### 6.1 Input Snapshot

The Rule Engine consumes a logical `EvaluationInputSnapshot`. This model is not an API or database schema; it is an architectural contract for implementation.

| Field | Description | Required |
|---|---|---|
| Snapshot ID | Immutable identifier for normalized input data. | Yes |
| User ID | Owner of the evaluation. | Yes |
| Repository Set | Repositories selected for evaluation. | Yes |
| GitHub Data Bundle | Repository, commit, branch, PR, issue, README, dependency, language, and directory data. | Conditional |
| Notion Data Bundle | Workspace, page, database, retrospective, documentation, learning note, and project note data. | Conditional |
| Career Context | Selected career when career-weighted evaluation is requested. | Conditional |
| Company Context | Selected company when company-weighted evaluation is requested. | Conditional |
| Collection Metadata | Provider timestamps, sync status, rate-limit status, and normalization version. | Yes |

### 6.2 GitHub Input Data

| Data Type | Required Signals |
|---|---|
| Repository Metadata | Name, visibility, default branch, created date, updated date, archived flag, fork flag, topics. |
| Commit History | Commit count, timestamps, authorship, message text, touched file paths. |
| Branch Data | Branch count, default branch, recent activity, naming signals. |
| Pull Requests | PR count, status, reviews, comments, merge status, timestamps. |
| Issues | Issue count, state, labels, comments, assignment, timestamps. |
| README | Presence, length, sections, badges, setup instructions, architecture notes, screenshots or links. |
| Dependencies | Manifest files, package names, versions when available, lockfiles. |
| Directory Tree | File paths, directory names, file extensions, configuration files. |
| Language Statistics | Provider language percentages and file-level supporting evidence. |

### 6.3 Notion Input Data

| Data Type | Required Signals |
|---|---|
| Workspace Metadata | Workspace ID, connection status, permissions, last sync timestamp. |
| Documentation Pages | Page titles, hierarchy, content blocks, timestamps, project references. |
| Retrospectives | Reflection entries, dates, linked projects, lessons learned. |
| Learning Notes | Topics, tags, dates, references, progression evidence. |
| Project Notes | Project descriptions, design decisions, implementation notes, TODOs, outcomes. |

### 6.4 Input Quality Dimensions

| Dimension | Meaning | Impact |
|---|---|---|
| Completeness | Required data is present. | Affects confidence and skipped rules. |
| Freshness | Data is recently synchronized. | Affects evidence freshness weight. |
| Consistency | Provider metadata and normalized records agree. | Affects validation warnings. |
| Reliability | Source is authoritative and uncorrupted. | Affects confidence. |
| Coverage | Evidence spans enough repositories or time periods. | Affects growth and activity confidence. |

## 7. Normalization Rules

### 7.1 Normalization Objectives

Normalization transforms provider-specific raw data into stable, comparable records. The Rule Engine shall not parse raw provider responses directly during scoring. It shall consume normalized records produced by the Data Collection and Normalizer modules.

### 7.2 Normalization Requirements

| Area | Rule |
|---|---|
| Provider IDs | Preserve source provider ID and normalized internal ID. |
| Timestamps | Convert timestamps to a canonical time zone and retain original provider value. |
| File Paths | Normalize path separators, casing policy, and repository-relative paths. |
| Languages | Map language names to a controlled taxonomy while preserving provider label. |
| Frameworks | Normalize framework names and aliases through technology taxonomy. |
| Dependencies | Normalize manifest type, dependency name, version, scope, and source file. |
| Text Content | Normalize markdown/plain text while preserving original content hash. |
| Empty Values | Represent missing, empty, and inaccessible data as distinct states. |
| Duplicates | Deduplicate by provider ID, repository ID, content hash, and timestamp where applicable. |

### 7.3 Missing Data Semantics

| State | Meaning | Example | Scoring Impact |
|---|---|---|---|
| Missing | Expected data does not exist. | Repository has no README. | Rule may score absence if configured. |
| Empty | Data exists but contains no meaningful content. | README file with whitespace only. | Rule may apply low documentation signal. |
| Inaccessible | Provider denied or omitted data. | Private repository PRs not authorized. | Rule shall reduce confidence, not assume failure. |
| Unsupported | Data type is recognized but not evaluable. | Unsupported language taxonomy entry. | Rule shall emit warning and skip dependent rule. |
| Corrupted | Data violates validation constraints. | Invalid timestamp or malformed dependency record. | Rule shall fail affected rule safely. |

## 8. Evaluation Categories

### 8.1 Category Overview

| Category | Purpose | Primary Evidence |
|---|---|---|
| Programming Language | Evaluate language usage and depth. | Language stats, file paths, dependency manifests. |
| Framework | Detect and evaluate framework experience. | Dependencies, config files, directory conventions. |
| Database | Detect data storage technologies. | Dependencies, config files, code paths, documentation. |
| Architecture | Evaluate structural engineering maturity. | Directory tree, module boundaries, docs, config. |
| Testing | Evaluate testing discipline and breadth. | Test files, test dependencies, CI workflows. |
| DevOps | Evaluate operational and deployment signals. | Docker, CI/CD, deployment config, environment files. |
| Documentation | Evaluate README and project documentation quality. | README, Notion docs, API docs, architecture notes. |
| Collaboration | Evaluate teamwork and project management signals. | PRs, reviews, issues, comments, commit messages. |
| Repository Quality | Evaluate repository completeness and maintainability. | Metadata, structure, docs, activity, dependencies. |
| Growth Trend | Evaluate progression over time. | Time-series commits, technologies, docs, project maturity. |
| Activity | Evaluate development consistency and recency. | Commits, PRs, issues, sync timestamps. |
| Project Complexity | Evaluate implementation and structural complexity. | Module count, technologies, architecture, integrations. |
| Technology Diversity | Evaluate breadth of technology exposure. | Languages, frameworks, databases, DevOps tools. |
| Security Practices | Evaluate basic secure engineering signals. | Dependency hygiene, secrets absence, security configs. |
| Maintainability | Evaluate ease of ongoing development. | Structure, tests, docs, dependency discipline. |
| Code Organization | Evaluate directory and naming organization. | Directory tree, module layout, config placement. |

### 8.2 Programming Language Evaluation

| Criterion | Measurement |
|---|---|
| Primary language | Highest weighted language by provider statistics and file evidence. |
| Language depth | Sustained usage across repositories and time windows. |
| Language diversity | Number and distribution of meaningful languages. |
| Career relevance | Match between detected languages and selected career rule set. |
| Growth | Increase in language usage maturity over time. |

### 8.3 Framework Evaluation

| Criterion | Measurement |
|---|---|
| Framework detection | Dependency and configuration evidence. |
| Framework relevance | Alignment with career and company weighting strategy. |
| Framework maturity | Repeated usage, project integration depth, and configuration completeness. |
| Framework breadth | Multiple relevant frameworks without excessive fragmentation. |

### 8.4 Database Evaluation

| Criterion | Measurement |
|---|---|
| Database presence | Detected relational, NoSQL, cache, or vector database evidence. |
| Integration depth | Configuration, migration, ORM, query, or documentation signals. |
| Career relevance | Backend, data, AI, and DevOps career weighting impact. |
| Operational maturity | Environment configuration and deployment-related database signals. |

### 8.5 Architecture Evaluation

| Criterion | Measurement |
|---|---|
| Structural separation | Presence of layered, modular, or domain-based organization. |
| Boundary clarity | Clear separation of UI, domain, infrastructure, tests, and configuration. |
| Architecture documentation | README or Notion explanation of major design decisions. |
| Complexity appropriateness | Architecture complexity matches project size and purpose. |

### 8.6 Testing Evaluation

| Criterion | Measurement |
|---|---|
| Test presence | Test files and test dependencies exist. |
| Test breadth | Unit, integration, and end-to-end test signals. |
| Test execution | CI workflow or documented test command evidence. |
| Test density | Ratio of test files to implementation files, configured by language/framework. |

### 8.7 DevOps Evaluation

| Criterion | Measurement |
|---|---|
| Containerization | Dockerfile and compose configuration evidence. |
| CI/CD | GitHub Actions or equivalent workflow evidence. |
| Deployment readiness | Nginx, environment, deployment scripts, or hosting configuration. |
| Operational clarity | README or Notion deployment instructions. |

### 8.8 Documentation Evaluation

| Criterion | Measurement |
|---|---|
| README completeness | Overview, stack, setup, usage, architecture, test, deployment, and screenshots/links. |
| Project documentation | Notion project notes and design records. |
| Learning documentation | Learning notes and retrospectives connected to projects. |
| Maintenance signal | Documentation updated near meaningful code changes. |

### 8.9 Collaboration Evaluation

| Criterion | Measurement |
|---|---|
| Pull request usage | PR count, merged PRs, discussion, and review signals. |
| Issue management | Issues, labels, comments, assignments, and resolution patterns. |
| Commit quality | Message clarity, consistency, and meaningful change grouping. |
| Team signal | Multiple contributors or review interactions where available. |

### 8.10 Repository Quality Evaluation

Repository quality combines documentation, architecture, testing, dependency hygiene, activity, and maintainability into a repository-level score. It shall not replace category scores; it is an aggregate view for downstream dashboards.

### 8.11 Growth Trend Evaluation

Growth trend shall be evaluated over configured time windows using only timestamped evidence. Growth calculations may compare:

- Project complexity over time
- New technology adoption
- Testing and documentation improvement
- Activity consistency
- Collaboration maturity

### 8.12 Activity Evaluation

Activity evaluates measurable development behavior, including commit frequency, PR activity, issue activity, recent updates, and sustained contribution windows. Activity shall distinguish recent inactivity from long-term historical strength.

### 8.13 Project Complexity Evaluation

Project complexity evaluates meaningful engineering scope. It shall avoid rewarding accidental complexity. Signals include module count, technology integration, deployment setup, architecture boundaries, database usage, and documentation of design decisions.

### 8.14 Technology Diversity Evaluation

Technology diversity evaluates breadth while preventing shallow keyword inflation. The Rule Engine shall require evidence strength before crediting technology diversity.

### 8.15 Security Practices Evaluation

Security practices evaluate basic secure engineering evidence, such as absence of detected secrets in committed files, dependency update signals, environment variable separation, authentication-related configuration, and security documentation when present.

### 8.16 Maintainability Evaluation

Maintainability evaluates structure, documentation, testability, dependency discipline, and consistency. It is intended to reflect whether another engineer could understand and safely modify the project.

### 8.17 Code Organization Evaluation

Code organization evaluates directory clarity, naming consistency, conventional placement of source and tests, separation of generated files, and avoidance of confusing repository clutter.

## 9. Score Calculation Strategy

### 9.1 Score Types

| Score Type | Description |
|---|---|
| Rule Score | Output of a single rule formula. |
| Category Score | Weighted aggregate of rule scores in a category. |
| Skill Score | Score mapped to a specific skill matrix entry. |
| Repository Score | Aggregate score for one repository. |
| Activity Score | Deterministic activity measure from timestamped contribution evidence. |
| Growth Score | Deterministic trend score from time-series evidence. |
| Architecture Score | Aggregate score for architecture-related rules. |
| Documentation Score | Aggregate score for documentation-related rules. |
| Overall Score | Weighted aggregate of configured component scores. |

### 9.2 Score Range

Unless a rule version specifies otherwise, scores shall use a normalized `0` to `100` range.

| Range | Interpretation |
|---:|---|
| 0 | No measurable evidence or configured hard failure. |
| 1–39 | Weak evidence or low maturity. |
| 40–59 | Basic measurable evidence. |
| 60–79 | Solid evidence with meaningful implementation depth. |
| 80–100 | Strong evidence with broad, recent, and relevant maturity. |

### 9.3 Formula Governance

Score formulas shall be versioned, deterministic, and auditable. A score formula may use:

- Counts
- Ratios
- Boolean indicators
- Threshold bands
- Time-window comparisons
- Weighted averages
- Caps and floors
- Missing-data policies
- Confidence modifiers

### 9.4 Aggregation Model

Category scores shall be calculated from rule scores using configured weights. Overall score shall be calculated from category or component scores using configured base, career, and company weights.

The general architecture is:

1. Evaluate rule-level conditions.
2. Calculate rule-level scores.
3. Apply rule-level caps, floors, and thresholds.
4. Aggregate rule scores into category scores.
5. Aggregate category scores into component scores.
6. Apply career and company weight overrides.
7. Calculate overall score.
8. Calculate confidence separately from score.

### 9.5 Confidence Model

Confidence is not the same as score. A high score with low evidence completeness shall not be treated as equally reliable as a high score with complete evidence.

| Confidence Factor | Example |
|---|---|
| Evidence completeness | README, commits, PRs, dependencies, and directory tree are present. |
| Evidence freshness | Repository was synchronized recently. |
| Evidence consistency | Provider statistics match file-level signals. |
| Evidence breadth | Multiple repositories or time windows support the same conclusion. |
| Source accessibility | Private repository data was accessible with sufficient permissions. |

## 10. Weight Management

### 10.1 Weight Layers

| Layer | Purpose | Example |
|---|---|---|
| Base Weight | Default platform-wide category importance. | Testing has a default weight for all users. |
| Career Weight | Adjusts importance for selected career. | DevOps increases DevOps and CI/CD weights. |
| Company Weight | Adjusts importance for selected company. | Google strategy increases architecture and algorithmic depth indicators. |
| Rule Override | Adjusts a specific rule under specific scope. | Backend increases relational database evidence weight. |

### 10.2 Effective Weight Resolution

The effective weight shall be resolved in this order:

1. Base rule weight
2. Base category weight
3. Career-specific override
4. Company-specific override
5. Version-specific migration adjustment
6. Administrative enabled/disabled state

### 10.3 Weight Validation

The Rule Engine shall validate that:

- Required category weights are present.
- Weight totals are normalized or explicitly marked as non-normalized.
- Disabled rules are excluded from scoring.
- Overrides reference existing rules or categories.
- Company and career overrides do not create negative or invalid score ranges.

## 11. Rule Execution Order

### 11.1 Ordering Strategy

Rules shall execute according to:

1. Input validation rules
2. Taxonomy and evidence mapping rules
3. Atomic detection rules
4. Category scoring rules
5. Skill mapping rules
6. Aggregation rules
7. Confidence rules
8. Output validation rules

### 11.2 Priority

Priority controls execution order within the same dependency level.

| Priority | Meaning |
|---:|---|
| 0 | Critical validation rule. |
| 10 | Required evidence extraction rule. |
| 20 | Core category rule. |
| 30 | Aggregation rule. |
| 40 | Optional enrichment rule. |
| 50 | Output packaging rule. |

### 11.3 Dependency Handling

If a rule depends on a failed or skipped prerequisite:

- The dependent rule shall not execute blindly.
- The dependent rule shall emit `SKIPPED`.
- The output shall record the missing dependency.
- Aggregation shall apply the configured missing-data policy.

## 12. Rule Configuration

### 12.1 Configuration-driven Architecture

The Rule Engine shall not hardcode business scoring logic. Rule behavior shall be defined by versioned configuration that can later be represented using YAML or JSON.

This document intentionally does not generate configuration files. It defines the conceptual model backend engineers shall implement.

### 12.2 Rule Configuration Concepts

| Field | Purpose |
|---|---|
| Rule ID | Stable identifier used for traceability. |
| Category | Evaluation category such as Testing or Documentation. |
| Name | Human-readable rule name. |
| Description | Engineering meaning of the rule. |
| Enabled Flag | Determines whether the rule participates in execution. |
| Version | Rule definition version. |
| Priority | Ordering within dependency group. |
| Dependencies | Rule IDs or evidence types required before execution. |
| Conditions | Deterministic predicates over normalized input. |
| Threshold | Numeric or categorical boundary for scoring. |
| Weight | Contribution to parent category or aggregate. |
| Score Formula | Deterministic formula reference and parameters. |
| Evidence Query | Definition of required evidence from normalized data. |
| Missing Data Policy | Behavior when expected evidence is absent. |
| Inheritance | Parent rule or rule group inherited from base configuration. |
| Override | Career or company-specific changes to base rule behavior. |

### 12.3 Rule Inheritance

Inheritance allows common rule definitions to be reused across careers and companies.

| Inheritance Level | Description |
|---|---|
| Global Base Rule | Applies to all users unless disabled. |
| Category Rule | Applies to one evaluation category. |
| Career Rule | Inherits base rule and adjusts weight, threshold, or required evidence. |
| Company Rule | Inherits career or base rule and adjusts emphasis. |
| Repository-type Rule | Adjusts behavior for repository type when such metadata exists. |

### 12.4 Override Rules

Overrides shall be explicit and traceable. An override may change:

- Weight
- Threshold
- Enabled flag
- Priority
- Missing-data policy
- Confidence modifier
- Required evidence strength

An override shall not change the identity of the underlying score without creating a new rule version.

### 12.5 Implemented Repository Baselines

The first implemented catalog is the immutable `REPOSITORY_BASELINE` rule set version `baseline-v1`.
It requires extractor version `engineering-evidence-extractor-v1` and formula library `formula-v1`.
The catalog is stored in PostgreSQL and loaded through the Rule Engine application port; the executor does not
hardcode rule weights or thresholds.

`formula-v1` provides three deterministic formulas:

| Formula | Calculation |
|---|---|
| `PRESENCE` | `100` when normalized evidence is present, otherwise `0`. |
| `COUNT_CAP` | `min(observed count / configured target, 1) * 100`. |
| `PERCENTAGE` | The normalized input percentage, clamped to `0..100`. |

The initial category weights are Language `0.25`, Framework `0.15`, Testing `0.25`, Documentation `0.20`, and
Activity `0.15`. Rule-level weights and targets are defined by Flyway migration
`V12__create_versioned_rule_catalog_schema.sql` and mirrored by the immutable golden fixture
`fixtures/rule-engine/baseline-v1.json`. A value change requires a new rule-set version and regression fixture.

This baseline scores only evidence currently available from immutable repository snapshots. It does not claim code
coverage, README content completeness, collaboration maturity, career readiness, company readiness, or recommendation
priority. Confidence is calculated separately as the weighted availability of required normalized evidence; a known
absence may score zero without being treated as inaccessible evidence.

The repository evidence read model may evolve independently when it adds non-scoring facts. `baseline-v1` remains
immutable and reproducible with the exact `engineering-evidence-extractor-v1` fact set. The approved `baseline-v2`
policy consumes `engineering-evidence-extractor-v2`, adds Database, Architecture, and DevOps, and uses category weights
Language 15%, Framework 15%, Database 15%, Architecture 15%, Testing 15%, DevOps 10%, Documentation 10%, and Activity
5%. Database rules weight technology declaration 20%, data-access dependency 20%, migrations 35%, and persistence
configuration 25%. Architecture weights structured boundaries 50%, module layout 25%, and architecture documentation
25%; hexagonal and layered boundaries are alternatives for the structured-boundary signal. DevOps weights container
configuration 30%, CI 30%, infrastructure-as-code 20%, and deployment configuration 20%. Every v2 result records its
extractor, mapper, formula-library, rule-set, and Skill Matrix policy versions.

`baseline-v2` is the active catalog. Flyway migration `V18__create_baseline_v2_and_career_readiness_schema.sql`
persists its exact weights and rules, while `fixtures/rule-engine/baseline-v2.json` provides the deterministic golden
regression fixture. `baseline-v1` remains immutable with `SUPERSEDED` status for historical result reproduction.

## 13. Rule Versioning

### 13.1 Versioning Goals

Rule versioning ensures historical reproducibility, safe evolution, auditability, and regression testing.

### 13.2 Versioned Artifacts

| Artifact | Versioning Requirement |
|---|---|
| Rule definition | Versioned when condition, formula, weight, or threshold changes. |
| Rule set | Versioned when included rules or effective weights change. |
| Technology taxonomy | Versioned when aliases or classifications change. |
| Formula library | Versioned when calculation behavior changes. |
| Career mapping | Versioned when career priorities or required skills change. |
| Company mapping | Versioned when company-specific weight strategy changes. |

### 13.3 Historical Recalculation

Historical recalculation shall require explicit selection of:

- Input snapshot ID
- Rule set version
- Career mapping version
- Company mapping version when applicable
- Taxonomy version

### 13.4 Rule Regression

Every new rule version shall support regression testing against fixture snapshots to verify:

- Deterministic output
- Expected category score changes
- No unintentional changes to unrelated categories
- Valid output schema
- Valid trace metadata

## 14. Career-specific Rules

### 14.1 Career Rule Strategy

Career-specific rules shall not invent new evaluation categories. They shall modify priorities, weights, thresholds, and required skills within the supported DevPath career list.

### 14.2 Career Priority Matrix

| Career | Evaluation Priorities | Weight Changes | Required Skills | Optional Skills |
|---|---|---|---|---|
| Backend | Language depth, framework, database, architecture, testing, DevOps. | Increase backend framework, database, testing, architecture. | Server-side language, API framework, database, testing, GitHub activity. | Messaging, caching, observability, deployment. |
| Frontend | Framework, UI project structure, testing, documentation, activity. | Increase frontend framework, code organization, documentation. | JavaScript/TypeScript, React or comparable framework, component structure. | Accessibility, design system, E2E testing, performance. |
| AI Engineer | Python or AI language evidence, AI framework, vector database, documentation. | Increase AI framework, data handling, experimentation notes. | Python, ML/AI framework, data processing evidence. | RAG, vector database, MLOps, notebooks. |
| DevOps | CI/CD, Docker, deployment, infrastructure, activity, documentation. | Increase DevOps, reliability, deployment documentation. | Containerization, CI workflow, deployment configuration. | Monitoring, infrastructure-as-code, orchestration. |
| Security | Security practice signals, dependency hygiene, architecture, documentation. | Increase security practices, maintainability, testing. | Secure configuration evidence, dependency discipline, documented risk awareness. | Static analysis, threat modeling notes, auth/security projects. |
| Game | Game framework, language relevance, project complexity, code organization. | Increase game framework, performance-adjacent organization, project complexity. | Game engine/framework evidence, gameplay project structure. | Graphics, physics, multiplayer, tooling. |
| Embedded | Language relevance, hardware-adjacent structure, maintainability, documentation. | Increase C/C++ or embedded language signals, code organization. | Embedded-relevant language evidence, low-level project structure. | RTOS, device docs, hardware notes. |
| Mobile | Mobile framework, app structure, testing, documentation, activity. | Increase mobile framework and platform-specific organization. | Android/iOS/mobile framework evidence, app project structure. | App deployment notes, UI testing, accessibility. |
| Data Engineer | Database, data processing, pipelines, DevOps, documentation. | Increase database, data pipeline, DevOps, maintainability. | SQL or data language, database, ETL/pipeline evidence. | Warehousing, streaming, orchestration. |

### 14.3 Career Rule Output

Career-weighted Rule Engine output shall include:

- Selected career
- Career mapping version
- Effective category weights
- Required skill evidence status
- Optional skill evidence status
- Career-specific category scores
- Rule trace for career overrides

## 15. Company-specific Rules

### 15.1 Company Rule Strategy

Company-specific rules shall remain generic and competency-based. They shall not claim to model private hiring processes, internal interview rubrics, or confidential company evaluation criteria.

The SRS-supported companies are Google, Amazon, Naver, Kakao, Toss, and Coupang. Meta and Microsoft may be represented only as future extension candidates unless the SRS is updated.

### 15.2 Company Weighting Matrix

| Company | Weight Overrides | Technology Priorities | Recommended Competencies |
|---|---|---|---|
| Google | Increase architecture, testing, language depth, project complexity. | General-purpose languages, scalable systems, algorithmic project evidence. | Technical depth, system design clarity, testing maturity. |
| Amazon | Increase backend reliability, DevOps, operational documentation, activity. | Backend frameworks, databases, deployment and CI/CD signals. | Ownership evidence, operational thinking, scalable service quality. |
| Naver | Increase web service engineering, documentation, data/search-adjacent signals. | Backend/frontend web stacks, databases, documentation practices. | Product-quality web engineering, maintainability, technical communication. |
| Kakao | Increase product engineering, collaboration, reliability, frontend/backend quality. | Service-oriented web/mobile stacks and collaboration evidence. | User-facing delivery, team workflow, service maintainability. |
| Toss | Increase testing, reliability, backend/frontend quality, impact clarity. | Fintech-adjacent backend/frontend, database, CI/CD, test evidence. | Reliability, correctness, iteration speed, measurable impact. |
| Coupang | Increase scalability, DevOps, data systems, operational activity. | Commerce-scale backend, data, deployment, infrastructure signals. | Large-scale systems, operational discipline, data-informed engineering. |
| Meta | Future extension candidate only unless added to SRS. | Not active in version 1.0. | Not active in version 1.0. |
| Microsoft | Future extension candidate only unless added to SRS. | Not active in version 1.0. | Not active in version 1.0. |

### 15.3 Company Override Constraints

Company overrides shall:

- Reference active SRS-supported companies only.
- Be versioned independently.
- Modify weights and thresholds, not create AI text.
- Preserve deterministic score calculation.
- Include trace entries for all changed weights.

## 16. Skill Matrix

### 16.1 Skill Matrix Purpose

The Skill Matrix is the Rule Engine output that translates measurable evidence into structured skill-level information. It is used by downstream engines to explain strengths, identify weaknesses, build roadmaps, generate portfolio content, and display dashboards.

### 16.2 Skill Matrix Structure

| Field | Description |
|---|---|
| Skill ID | Stable identifier for the skill. |
| Skill Name | Human-readable skill name. |
| Skill Category | Language, framework, database, DevOps, testing, documentation, etc. |
| Score | Deterministic score from Rule Engine. |
| Skill Level | Derived level from configured score thresholds. |
| Confidence | Evidence confidence independent from score. |
| Evidence IDs | References to normalized evidence records. |
| Related Repository IDs | Repositories contributing to the skill. |
| Growth Trend | Deterministic trend value or state. |
| Weakness Flag | Deterministic flag based on configured thresholds. |
| Strength Flag | Deterministic flag based on configured thresholds. |
| Recommendation Inputs | Structured facts downstream recommendation systems may consume. |
| Rule Version | Rule version used to calculate the entry. |

### 16.3 Skill Levels

| Level | Default Score Range | Meaning |
|---|---:|---|
| None | 0 | No measurable evidence. |
| Beginner | 1–39 | Basic or sparse evidence. |
| Developing | 40–59 | Repeated but limited evidence. |
| Competent | 60–79 | Solid evidence across meaningful work. |
| Strong | 80–100 | Broad, recent, and high-quality evidence. |

### 16.4 Strength and Weakness Flags

Strengths and weaknesses shall be derived from rule-configured thresholds. The Rule Engine shall not produce natural-language recommendations; it may produce structured flags and inputs.

### 16.5 Implemented Skill Matrix Policies

The first implemented policy is `skill-matrix-v1`, bound immutably to RuleSetVersion `baseline-v1`. It maps the
Language, Framework, Testing, Documentation, and Activity category scores one-to-one to stable skill definitions.
It copies the authoritative category score and confidence without recalculation. Level thresholds are persisted as
`NONE = 0`, `BEGINNER = 1..39.99`, `DEVELOPING = 40..59.99`, `COMPETENT = 60..79.99`, and
`STRONG = 80..100`. The configured strength threshold is `80` and weakness maximum is `39.99`.

Every assessment references its aggregate evaluation/category result and includes available normalized Evidence IDs
and contributing Repository IDs. Until multiple comparable historical matrices exist, growth trend is explicitly
`UNAVAILABLE`. Recommendation inputs are bounded structured facts only; this policy does not calculate recommendation
priority or produce coaching text. Any mapping or threshold change requires a new Skill Matrix policy version.

After a completed RuleEvaluation is persisted, the application orchestration invokes Skill Matrix generation in the
same deterministic workflow. Both Evaluation persistence and Skill Matrix generation are idempotent by their immutable
input basis, so repeating the same evaluation reuses the existing result and matrix. This connection does not introduce
an analysis job runtime or imply that repository synchronization itself is an analysis request.

The active `skill-matrix-v2` policy is bound to `baseline-v2` and adds one-to-one Database, Architecture, and DevOps
skills without changing the approved level thresholds. Historical `skill-matrix-v1` matrices remain reproducible.

## 17. Output Models

### 17.1 Output Package

The Rule Engine shall produce a `RuleEvaluationResult` package containing:

- Evaluation result ID
- User ID
- Snapshot ID
- Repository scope
- Rule set version
- Career context when applicable
- Company context when applicable
- Category scores
- Component scores
- Overall score
- Skill Matrix
- Evidence references
- Confidence model
- Completeness model
- Warnings and errors
- Rule execution trace
- Created timestamp

### 17.2 Category Score Output

| Field | Description |
|---|---|
| Category | Evaluation category. |
| Score | Deterministic category score. |
| Weight | Effective category weight. |
| Confidence | Confidence for this category. |
| Rule Results | Rules that contributed to the category. |
| Missing Evidence | Expected evidence that was missing. |

### 17.3 Rule Result Output

| Field | Description |
|---|---|
| Rule ID | Executed rule identifier. |
| Rule Version | Version of the rule definition. |
| Status | `PASSED`, `FAILED`, `PARTIAL`, `SKIPPED`, or `ERROR`. |
| Raw Value | Measured raw value before scoring. |
| Score | Rule-level score. |
| Weight | Effective rule weight. |
| Evidence IDs | Evidence used by the rule. |
| Formula ID | Deterministic formula reference. |
| Trace | Calculation metadata sufficient for audit. |

### 17.4 Downstream Consumers

| Consumer | Usage |
|---|---|
| Career Path Engine | Uses scores, skill gaps, career-weighted outputs. |
| Prompt Builder | Uses structured facts and evidence references. |
| AI Engine | Explains deterministic outputs without calculating scores. |
| Dashboard | Displays scores, charts, trends, skill matrix, and evidence. |
| Recommendation Engine | Consumes structured weakness, strength, and roadmap inputs. |

## 18. Business Rules

| ID | Business Rule |
|---|---|
| BR-RE-001 | The Rule Engine shall be the only component allowed to calculate scores. |
| BR-RE-002 | The Rule Engine shall never call an LLM. |
| BR-RE-003 | The Rule Engine shall not contain prompt logic. |
| BR-RE-004 | The Rule Engine shall not generate user-facing career coaching text. |
| BR-RE-005 | The Rule Engine shall not invent evidence when provider data is missing. |
| BR-RE-006 | Every score shall reference rule version, formula, weight, and evidence where available. |
| BR-RE-007 | Identical input snapshot and rule version shall produce identical output. |
| BR-RE-008 | Missing data shall be represented explicitly. |
| BR-RE-009 | Career and company logic shall be implemented through deterministic weight and threshold configuration. |
| BR-RE-010 | Rule changes shall be versioned and auditable. |

## 19. Functional Requirements

### RR-001 — Language Analysis

| Field | Specification |
|---|---|
| Purpose | Identify and measure programming language evidence. |
| Description | The Rule Engine shall analyze normalized language statistics and file evidence to determine primary language, secondary languages, language diversity, and language relevance. |
| Inputs | Repository language statistics, file extensions, dependency manifests, repository metadata, career context. |
| Outputs | Language signals, language scores, evidence references, confidence values. |
| Business Rules | Language scores shall be deterministic and shall not be inferred by AI. |
| Validation Rules | Language percentages shall be normalized; unsupported labels shall be mapped to taxonomy or marked unsupported. |
| Acceptance Criteria | Given identical language evidence and rule version, the engine returns identical primary language, score, and evidence references. |
| Dependencies | Normalized GitHub language data, technology taxonomy, rule configuration. |

### RR-002 — Framework Analysis

| Field | Specification |
|---|---|
| Purpose | Detect and evaluate framework usage. |
| Description | The Rule Engine shall detect frameworks from dependency files, configuration files, project structure, and known framework conventions. |
| Inputs | Dependency manifests, lockfiles, configuration files, directory tree, technology taxonomy. |
| Outputs | Framework detections, framework scores, confidence, evidence IDs. |
| Business Rules | Framework credit requires measurable evidence, not keyword-only assumptions. |
| Validation Rules | Framework aliases shall resolve to canonical taxonomy entries. |
| Acceptance Criteria | A repository with known framework dependencies produces canonical framework detection with evidence path references. |
| Dependencies | Dependency normalizer, directory normalizer, technology taxonomy. |

### RR-003 — Database Analysis

| Field | Specification |
|---|---|
| Purpose | Detect and evaluate database technology evidence. |
| Description | The Rule Engine shall identify relational, NoSQL, cache, and vector database usage using dependencies, configuration, documentation, and project structure. |
| Inputs | Dependency manifests, environment configuration metadata, README, Notion project notes, directory tree. |
| Outputs | Database technology signals, score components, evidence references. |
| Business Rules | Database scoring shall distinguish declared dependency from meaningful integration evidence. |
| Validation Rules | Database technologies shall map to controlled taxonomy categories. |
| Acceptance Criteria | A repository with PostgreSQL dependency and database configuration receives database evidence with source references. |
| Dependencies | Technology taxonomy, documentation normalizer, dependency normalizer. |

### RR-004 — Architecture Analysis

| Field | Specification |
|---|---|
| Purpose | Evaluate architecture maturity from measurable structure. |
| Description | The Rule Engine shall evaluate layered, modular, clean architecture, microservice, monolith, and organization signals from normalized project structure and documentation. |
| Inputs | Directory tree, file paths, module layout, README architecture sections, Notion design notes. |
| Outputs | Architecture category score, architecture pattern signals, evidence IDs, confidence. |
| Business Rules | Architecture labels shall require structural evidence and shall not be assigned by AI. |
| Validation Rules | Directory evidence shall be repository-relative and normalized. |
| Acceptance Criteria | A repository with clear layered structure and architecture documentation receives traceable architecture signals. |
| Dependencies | Directory normalizer, documentation analyzer, rule configuration. |

### RR-005 — Testing Analysis

| Field | Specification |
|---|---|
| Purpose | Evaluate testing discipline and measurable test coverage signals. |
| Description | The Rule Engine shall detect unit, integration, and end-to-end testing signals from test files, dependencies, configuration, and CI workflows. |
| Inputs | File tree, dependency manifests, CI workflow metadata, README test instructions. |
| Outputs | Testing category score, test type signals, test framework evidence, confidence. |
| Business Rules | The engine shall not claim actual code coverage unless coverage data is explicitly available. |
| Validation Rules | Test files shall be identified by language-aware conventions and configured patterns. |
| Acceptance Criteria | A repository with test files and CI test execution receives higher testing evidence than a repository with only a test dependency. |
| Dependencies | Directory normalizer, dependency normalizer, DevOps workflow data. |

### RR-006 — DevOps Analysis

| Field | Specification |
|---|---|
| Purpose | Evaluate operational and deployment readiness signals. |
| Description | The Rule Engine shall evaluate Docker, CI/CD, deployment configuration, infrastructure signals, and operational documentation. |
| Inputs | Dockerfile, compose files, GitHub Actions workflows, Nginx config, deployment docs, environment metadata. |
| Outputs | DevOps category score, deployment readiness signals, evidence IDs. |
| Business Rules | DevOps scoring shall distinguish configuration presence from documented operational usability. |
| Validation Rules | Workflow files shall be parsed as metadata and invalid workflows shall produce warnings. |
| Acceptance Criteria | A repository with Docker and CI workflow evidence produces traceable DevOps signals. |
| Dependencies | GitHub workflow collector, directory normalizer, documentation analyzer. |

### RR-007 — Documentation Analysis

| Field | Specification |
|---|---|
| Purpose | Evaluate repository and Notion documentation quality. |
| Description | The Rule Engine shall evaluate README completeness, API documentation, architecture documentation, learning notes, project notes, and retrospectives. |
| Inputs | README content, Notion pages, project notes, learning notes, repository metadata. |
| Outputs | Documentation score, completeness checklist, evidence references, missing documentation states. |
| Business Rules | Missing README shall be explicit evidence absence and shall not be silently ignored. |
| Validation Rules | Documentation sections shall be detected using configured headings and semantic patterns. |
| Acceptance Criteria | A README with overview, setup, usage, architecture, testing, and deployment sections receives higher completeness than a README with only a title. |
| Dependencies | README collector, Notion collector, text normalizer. |

### RR-008 — Collaboration Analysis

| Field | Specification |
|---|---|
| Purpose | Evaluate collaboration and project management signals. |
| Description | The Rule Engine shall evaluate pull request usage, review activity, issue management, comments, commit message quality, and contributor signals. |
| Inputs | Pull requests, reviews, comments, issues, labels, commits, contributor metadata. |
| Outputs | Collaboration score, issue/PR metrics, commit quality signals, confidence. |
| Business Rules | Private or inaccessible collaboration data shall reduce confidence rather than create negative assumptions. |
| Validation Rules | Contribution timestamps and provider IDs shall be valid. |
| Acceptance Criteria | Repositories with PR discussions, reviews, and issue resolution produce stronger collaboration signals than commit-only repositories. |
| Dependencies | GitHub PR collector, issue collector, commit normalizer. |

### RR-009 — Skill Matrix Generation

| Field | Specification |
|---|---|
| Purpose | Convert rule outputs into structured skill evidence. |
| Description | The Rule Engine shall generate a Skill Matrix containing skill scores, levels, confidence, evidence, repositories, trends, strengths, and weaknesses. |
| Inputs | Category scores, rule results, evidence map, career context, company context. |
| Outputs | Skill Matrix entries and recommendation input facts. |
| Business Rules | The Skill Matrix shall contain deterministic facts only and shall not contain generated coaching text. |
| Validation Rules | Every skill entry with a non-zero score shall reference at least one evidence ID or aggregate rule result. |
| Acceptance Criteria | Generated skill entries include score, level, confidence, evidence IDs, related repositories, and rule version. |
| Dependencies | Rule results, technology taxonomy, skill taxonomy. |

### RR-010 — Overall Score Calculation

| Field | Specification |
|---|---|
| Purpose | Calculate deterministic overall score from configured components. |
| Description | The Rule Engine shall calculate the overall score using configured component scores, weights, career overrides, and company overrides. |
| Inputs | Category scores, component scores, base weights, career weights, company weights, rule version. |
| Outputs | Overall score, effective weights, calculation trace, confidence. |
| Business Rules | Overall score shall never be calculated by an LLM or downstream AI component. |
| Validation Rules | Effective weights shall be valid, enabled components shall be present, and score range shall remain within configured boundaries. |
| Acceptance Criteria | Given fixed component scores and weight configuration, the overall score equals the configured weighted calculation exactly. |
| Dependencies | Category score outputs, weight resolver, rule configuration. |

## 20. Non-functional Requirements

| ID | Category | Requirement | Measurement |
|---|---|---|---|
| RE-NFR-001 | Performance | The Rule Engine shall complete evaluation for a typical user repository set within the configured backend SLA. | 95th percentile execution time is measured per evaluation size tier. |
| RE-NFR-002 | Scalability | The Rule Engine shall support asynchronous execution for large repository sets. | Evaluation jobs can be queued and processed without blocking user requests. |
| RE-NFR-003 | Maintainability | Rules shall be externally configurable and versioned. | Business scoring changes do not require core executor changes unless a new formula type is introduced. |
| RE-NFR-004 | Reliability | Partial provider data shall not crash the entire evaluation. | Affected rules emit `SKIPPED`, `PARTIAL`, or `ERROR` with trace. |
| RE-NFR-005 | Availability | Evaluation requests shall degrade gracefully when optional data sources are unavailable. | Outputs contain completeness and confidence indicators. |
| RE-NFR-006 | Configurability | Weights, thresholds, priorities, and enabled flags shall be configuration-controlled. | Admin-published rule versions determine effective behavior. |
| RE-NFR-007 | Extensibility | New categories and rules shall be introduced through catalog extensions. | Existing rule execution contract remains stable. |
| RE-NFR-008 | Observability | Execution metrics shall be emitted for latency, rule failures, skipped rules, and coverage. | Metrics are queryable by operators. |
| RE-NFR-009 | Logging | Rule execution trace shall support audit and debugging. | Every evaluation stores rule status and calculation metadata. |
| RE-NFR-010 | Monitoring | Alerts shall detect abnormal failure rates, latency spikes, and rule coverage drops. | Monitoring thresholds are configurable. |
| RE-NFR-011 | Caching | Safe caches may be used for immutable snapshots and resolved rule catalogs. | Cache keys include snapshot ID and rule version. |

## 21. Error Handling

### 21.1 Error Classification

| Error Type | Handling |
|---|---|
| GitHub API failure | Use last successful normalized snapshot if available; otherwise mark GitHub data unavailable and skip dependent rules. |
| Notion API failure | Mark Notion evidence unavailable; continue GitHub-based rules. |
| Empty repository | Produce explicit empty-repository signals with low confidence and applicable zero scores. |
| Private repository | Evaluate only authorized data; inaccessible data reduces confidence. |
| Missing README | Emit missing documentation evidence; apply documentation rule policy. |
| Missing commits | Mark activity and growth rules incomplete; do not invent activity. |
| Corrupted data | Reject affected records, emit validation error, and skip dependent rules. |
| Unsupported languages | Preserve raw label, mark unsupported taxonomy state, and skip dependent language-specific rules. |
| Invalid metadata | Emit validation warning or error depending on severity. |

### 21.2 Failure Severity

| Severity | Meaning | Evaluation Behavior |
|---|---|---|
| Info | Non-blocking observation. | Continue. |
| Warning | Data is incomplete or suspicious. | Continue with reduced confidence. |
| Recoverable Error | A rule or category cannot be evaluated. | Skip affected rules and continue. |
| Fatal Error | Snapshot or rule configuration is invalid. | Stop evaluation and persist failure state. |

### 21.3 Safe Failure Rules

- The engine shall never replace missing evidence with AI-generated inference.
- The engine shall never silently drop failed rules from trace output.
- The engine shall never calculate overall score from invalid required dependencies unless configured partial scoring allows it.
- The engine shall always preserve enough metadata for debugging.

## 22. Logging

### 22.1 Log Categories

| Log Category | Contents |
|---|---|
| Evaluation Lifecycle | Requested, started, completed, failed, cancelled. |
| Rule Execution | Rule ID, status, duration, score, evidence count. |
| Configuration Resolution | Rule set version, career mapping, company mapping, overrides. |
| Data Completeness | Missing, empty, inaccessible, unsupported, corrupted evidence. |
| Score Aggregation | Component scores, weights, formula references. |
| Security and Audit | Actor, scope, result, timestamp, operation. |

### 22.2 Logging Constraints

- Logs shall not contain OAuth tokens.
- Logs shall not contain secrets found in repository content.
- Logs shall prefer evidence IDs over raw large content.
- Logs shall preserve enough traceability for audit and regression.

## 23. Monitoring

### 23.1 Metrics

| Metric | Purpose |
|---|---|
| Evaluation count | Track throughput. |
| Evaluation latency | Track performance by repository count and data size. |
| Rule failure rate | Detect broken rules or data regressions. |
| Rule skipped rate | Detect missing data or misconfigured dependencies. |
| Category coverage | Detect categories with insufficient evidence. |
| Rule catalog load failures | Detect configuration publication issues. |
| Score distribution | Detect abnormal scoring shifts after rule changes. |
| Confidence distribution | Detect provider or normalization quality issues. |

### 23.2 Alerts

Alerts should be configured for:

- Fatal evaluation failure spike
- Rule catalog resolution failure
- Significant score distribution drift after rule publication
- Unexpected zero evidence coverage for major categories
- Latency exceeding SLA thresholds
- Repeated corrupted input snapshots

## 24. Extensibility

### 24.1 Extension Points

| Extension Point | Strategy |
|---|---|
| New evaluation category | Add category configuration, rules, weights, output mapping, and tests. |
| New technology taxonomy | Add taxonomy entries and aliases through versioned configuration. |
| New career | Add career mapping, required/optional skills, and weight overrides after SRS update. |
| New company | Add company mapping and competency-based weight strategy after SRS update. |
| New formula type | Add formula implementation with deterministic test fixtures. |
| New evidence source | Normalize source data before Rule Engine consumption. |

### 24.2 Extension Guardrails

- Extensions shall not bypass versioning.
- Extensions shall not introduce LLM dependency.
- Extensions shall include regression fixtures.
- Extensions shall preserve output model compatibility or explicitly version the output schema.
- Extensions shall update traceability documentation.

## 25. Future Improvements

Future improvements may include:

- Rule simulation tools for administrators.
- Rule impact reports before publishing new rule versions.
- Advanced evidence freshness weighting.
- More granular repository-type classification.
- Configurable organization-level benchmarking.
- Expanded technology taxonomy governance.
- Rule coverage reports for QA and product teams.
- Safe historical recalculation workflows.
- Company support expansion after formal SRS updates.
- Additional confidence modeling based on provider completeness.

These improvements shall not change the core principle: the Rule Engine calculates deterministic structured evaluation results, and AI explains those results without calculating scores.
