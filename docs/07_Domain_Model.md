# DevPath Domain Model

## 1. Purpose

### 1.1 Document Goal

This document defines the conceptual business domain model of DevPath, an AI-powered Developer Career Intelligence Platform. It is the authoritative domain specification for later documents covering system data models, database design, API contracts, backend architecture, frontend architecture, event design, and AI integration.

This document describes business meaning, ownership, boundaries, invariants, and lifecycle rules. It does not define implementation details.

### 1.2 Scope

The scope includes:

- Business concepts and ubiquitous language.
- Bounded contexts and context relationships.
- Core entities and their conceptual attributes.
- Value objects and business semantics.
- Aggregates and consistency boundaries.
- Domain services and business responsibilities.
- Domain events and business meanings.
- Domain invariants and entity lifecycles.
- Requirement traceability to the previous DevPath documents.
- Open domain issues and future extension points.

The scope excludes:

- Database schema, tables, columns, indexes, and ERD.
- API endpoints, DTOs, OpenAPI, GraphQL, and transport contracts.
- Backend classes, frontend models, source code, and infrastructure design.
- PostgreSQL, Redis, vector database, Docker, cloud, and deployment implementation.

### 1.3 Audience

| Audience | Purpose |
|---|---|
| Product Owner | Validate business concepts and domain scope |
| Software Architect | Derive later architecture documents from stable domain boundaries |
| Backend Engineer | Understand aggregate boundaries, domain services, and business invariants |
| AI Engineer | Understand where AI may explain or generate but must not calculate |
| Prompt Engineer | Understand prompt context ownership and prompt boundary rules |
| Data Architect | Derive logical data model without changing business ownership |
| QA Engineer | Derive acceptance tests from invariants, lifecycle rules, and traceability |
| Security Reviewer | Validate privacy, ownership, and authorization-related domain constraints |

### 1.4 References

| Document | Role |
|---|---|
| `00_Project_Context.md` | Defines DevPath vision, philosophy, modules, supported careers, supported companies, and major constraints |
| `01_SRS.md` | Primary source for functional and non-functional requirements |
| `02_Rule_Engine.md` | Source of truth for deterministic rule evaluation, score generation, and Skill Matrix creation |
| `03_Career_Path_Engine.md` | Source of truth for career readiness, company readiness, skill gaps, recommendations, and learning roadmaps |
| `04_AI_Architecture.md` | Source of truth for AI processing boundaries, LLM usage, response validation, and AI task types |
| `05_Prompt_Engineering.md` | Source of truth for prompt assembly, prompt validation, prompt versioning, and token strategy |
| `06_Knowledge_Architecture.md` | Source of truth for knowledge ingestion, chunking, embeddings, retrieval, and knowledge freshness |

### 1.5 Source Document Conflicts

| Conflict ID | Conflict | Resolution | Rationale |
|---|---|---|---|
| DM-CON-001 | `00_Project_Context.md` uses "Company Engine" while `03_Career_Path_Engine.md` treats company logic as part of career workflows. | This model defines a separate Company Context that collaborates with Career Context. | Company-specific rules and readiness have distinct ownership, but career workflows depend on them. |
| DM-CON-002 | AI requirements include skill analysis language, while core philosophy says LLM must never calculate scores. | AI Context may explain Skill Matrix results but must not calculate skill scores. | Rule Engine calculates; AI explains. |
| DM-CON-003 | Knowledge and Prompt documents both mention context assembly. | Knowledge Context retrieves relevant knowledge; Prompt Context assembles final prompt context. | Retrieval and prompt composition have different business responsibilities. |

### 1.6 Assumptions

| Assumption ID | Assumption |
|---|---|
| DM-ASM-001 | A User is the legal and privacy owner of connected GitHub, Notion, knowledge, prompt, AI, portfolio, and resume data. |
| DM-ASM-002 | Repository and Project are intentionally separate concepts. A Project may reference one or more repositories and documents. |
| DM-ASM-003 | Rule, career, company, prompt, and knowledge definitions are versioned to preserve historical reproducibility. |
| DM-ASM-004 | Recommendation priority is deterministic and is not assigned by an LLM. |
| DM-ASM-005 | Generated artifacts may be edited by the user, but user edits must remain distinguishable from AI-generated content. |

## 2. Domain Design Principles

### 2.1 DDD Philosophy

DevPath uses Domain-Driven Design to keep business meaning independent from technical implementation. Each domain concept is defined by its business responsibility, lifecycle, ownership, and invariants.

### 2.2 Ubiquitous Language

Terms must have one meaning within a bounded context. When the same word appears in multiple contexts, the owning context determines its meaning. For example, "context" in Knowledge Context means retrievable knowledge context, while "context" in Prompt Context means an immutable prompt execution package.

### 2.3 Aggregate Boundary

Aggregates are consistency boundaries, not database containers. A command may modify one aggregate directly. Cross-aggregate changes must be coordinated by domain services, domain events, or application orchestration.

### 2.4 Business Consistency

Immediate consistency is required for:

- User ownership and authorization.
- Repository snapshot immutability.
- Rule-set version validity before evaluation.
- Official score range validation.
- Skill Matrix evidence requirements.
- Prompt context immutability after creation.
- Generated artifact provenance.

Eventual consistency is acceptable for:

- Dashboard projections.
- Knowledge indexing.
- Recommendation views.
- Notification feeds.
- AI artifact history views.

### 2.5 Ownership

Every business concept has exactly one owning bounded context. Other contexts may reference the concept by identity, version, snapshot, source reference, or published event. No context may directly mutate another context's aggregate.

### 2.6 Immutability

The following concepts are immutable once completed or published:

- RepositorySnapshot.
- EvaluationResult.
- SkillMatrix version.
- CareerReadiness result.
- CompanyReadiness result.
- PromptContext.
- AIResponse validation record.
- Published Portfolio version.
- Published Resume version.
- KnowledgeDocumentVersion.

### 2.7 Separation of Responsibility

| Responsibility | Owning Domain |
|---|---|
| Authentication and ownership | Identity Context |
| Source synchronization | Repository Context and Knowledge Context |
| Technical score calculation | Rule Context only |
| Skill Matrix generation | Rule Context and Skill-related concepts |
| Career readiness | Career Context |
| Company readiness | Company Context |
| Recommendation priority | Recommendation Context |
| Learning roadmap construction | Learning Context |
| Long-term memory and retrieval | Knowledge Context |
| Prompt assembly | Prompt Context |
| Natural-language generation | AI Context |
| Portfolio and resume lifecycle | Portfolio Context |
| Rule, career, company, and prompt administration | Administration Context |

## 3. Ubiquitous Language

### 3.1 Business Glossary

| Term | Definition | Owner |
|---|---|---|
| User | A registered DevPath account holder who owns platform data and connected external accounts. | Identity Context |
| GitHubAccount | A GitHub external account connected by OAuth and authorized for repository access. | Identity Context |
| NotionWorkspace | A Notion workspace connected by OAuth and authorized for document access. | Knowledge Context |
| Repository | A source-code repository imported from GitHub and represented in DevPath. | Repository Context |
| RepositorySnapshot | Immutable captured state of a repository at a specific synchronization point. | Repository Context |
| Project | A career-relevant body of work that may reference repositories, documents, technologies, and generated artifacts. | Portfolio Context |
| Technology | A technical tool, language, framework, library, database, platform, or DevOps component. | Rule Context |
| Framework | A Technology that provides application structure or runtime conventions. | Rule Context |
| Skill | A human capability demonstrated by evidence, such as backend design, testing, DevOps, or documentation. | Rule Context |
| Evidence | A traceable source observation used to support a score, skill assessment, readiness result, or recommendation. | Rule Context |
| Score | A deterministic numeric evaluation result calculated only by the Rule Engine. | Rule Context |
| Skill Matrix | Structured representation of assessed developer skills and evidence-backed levels. | Rule Context |
| Rule | Deterministic evaluation definition used by the Rule Engine. | Rule Context |
| Rule Set | Versioned collection of rules and weights used for evaluation. | Rule Context |
| Career | A supported target career path such as Backend, Frontend, AI Engineer, DevOps, Security, Game, Embedded, Mobile, or Data Engineer. | Career Context |
| CareerProfile | Versioned expectations and competency requirements for a Career. | Career Context |
| Career Readiness | Deterministic assessment of Skill Matrix against a CareerProfile. | Career Context |
| Company | A supported target company such as Google, Amazon, Naver, Kakao, Toss, or Coupang. | Company Context |
| CompanyProfile | Versioned company-specific expectations, weights, recommendations, and interview focus. | Company Context |
| Company Readiness | Deterministic assessment of Skill Matrix against a CompanyProfile. | Company Context |
| Recommendation | Evidence-based suggested action with deterministic priority. | Recommendation Context |
| Learning Roadmap | Ordered learning plan derived from recommendations and skill gaps. | Learning Context |
| Knowledge | User-isolated long-term information stored for retrieval and AI grounding. | Knowledge Context |
| KnowledgeDocument | Versioned source document ingested from GitHub, Notion, generated reports, or future sources. | Knowledge Context |
| KnowledgeChunk | Retrievable segment of a KnowledgeDocumentVersion. | Knowledge Context |
| Prompt | Structured LLM instruction assembled from templates, variables, constraints, and context. | Prompt Context |
| PromptTemplate | Versioned reusable prompt structure. | Prompt Context |
| PromptContext | Immutable context package assembled for one AI task execution. | Prompt Context |
| AI Response | LLM-generated natural-language output subject to validation. | AI Context |
| Generated Artifact | AI-assisted output such as portfolio draft, resume draft, README draft, or interview question set. | AI Context |
| Portfolio | User-owned career presentation artifact generated or edited from project evidence. | Portfolio Context |
| Resume | User-owned resume artifact generated or edited from structured career evidence. | Portfolio Context |
| Interview Question | Generated question grounded in career, company, repository, skill, or roadmap context. | AI Context |

### 3.2 Similar Concept Distinctions

| Concepts | Distinction |
|---|---|
| Repository ≠ Project | Repository is a source-code container. Project is a career story or portfolio unit that may use one or more repositories and documents. |
| Skill ≠ Technology | Technology is a tool or platform. Skill is demonstrated competency using tools, practices, and evidence. |
| Evidence ≠ Score | Evidence is a traceable observation. Score is a deterministic calculation based on rules and evidence. |
| Recommendation ≠ AI Response | Recommendation is deterministic and priority-ranked. AI Response is natural-language output that may explain the recommendation. |
| Career Readiness ≠ Overall Score | Career readiness compares Skill Matrix to career expectations. Overall score summarizes technical evaluation. |
| KnowledgeDocument ≠ PromptContext | KnowledgeDocument is long-term stored knowledge. PromptContext is a short-lived immutable package for one AI task. |
| Rule ≠ Prompt | Rule calculates deterministic outputs. Prompt instructs an LLM how to explain or generate text. |
| Generated Artifact ≠ Source Evidence | Generated artifacts are outputs and may cite evidence, but are not primary evidence themselves. |

## 4. Domain Landscape

### 4.1 Core Domains

| Domain | Why It Exists |
|---|---|
| Rule Domain | DevPath's core differentiation depends on deterministic engineering analysis before AI explanation. |
| Career Domain | The platform must evaluate career readiness and skill gaps for selected target careers. |
| Company Domain | The platform must apply company-specific expectations and recommendations. |
| Recommendation Domain | The platform must convert measurable gaps into prioritized actions. |
| Knowledge Domain | The platform must maintain long-term developer knowledge for grounded AI output. |

### 4.2 Supporting Domains

| Domain | Why It Exists |
|---|---|
| Repository Domain | Provides source-code history, repository snapshots, metadata, and development activity. |
| Prompt Domain | Converts structured outputs into validated LLM prompts without business rule execution. |
| AI Domain | Produces explanations, summaries, generated documents, and coaching narratives. |
| Learning Domain | Converts recommendations into measurable learning steps and progress. |
| Portfolio Domain | Manages generated and user-reviewed career artifacts. |

### 4.3 Generic Domains

| Domain | Why It Exists |
|---|---|
| Identity Domain | Provides authentication, user ownership, OAuth connection, consent, and access boundaries. |
| Administration Domain | Manages rules, careers, companies, prompts, logs, statistics, and configuration governance. |
| Notification Capability | Delivers user-facing updates based on domain events. |
| Audit Capability | Preserves traceability for security, privacy, and administrative actions. |

## 5. Bounded Contexts

### 5.1 Context Summary

| Context | Purpose | Owned Aggregates |
|---|---|---|
| Identity Context | Manage user identity, ownership, external accounts, and consent. | User Aggregate |
| Repository Context | Manage repositories, snapshots, and source development activity. | Repository Aggregate, RepositorySnapshot Aggregate |
| Rule Context | Execute deterministic evaluations and produce official scores and Skill Matrix. | RuleSet Aggregate, Evaluation Aggregate, SkillMatrix Aggregate |
| Career Context | Evaluate career readiness and skill gaps. | Career Aggregate, CareerAssessment Aggregate |
| Company Context | Evaluate company readiness and company-specific expectations. | Company Aggregate, CompanyAssessment Aggregate |
| Recommendation Context | Generate evidence-based, priority-ranked recommendations. | Recommendation Aggregate |
| Learning Context | Create and track learning roadmaps. | LearningRoadmap Aggregate |
| Knowledge Context | Ingest, version, chunk, embed, retrieve, and protect knowledge. | KnowledgeDocument Aggregate |
| Prompt Context | Assemble and validate prompt templates and prompt contexts. | PromptTemplate Aggregate, PromptExecution Aggregate |
| AI Context | Execute AI tasks, validate responses, and create generated artifacts. | AITask Aggregate, GeneratedArtifact Aggregate |
| Portfolio Context | Manage portfolio, resume, and career artifact lifecycle. | Portfolio Aggregate, Resume Aggregate |
| Administration Context | Manage versioned configuration and operational governance. | Configuration Aggregate |

### 5.2 Identity Context

| Aspect | Definition |
|---|---|
| Purpose | Establish authenticated users and data ownership boundaries. |
| Responsibilities | GitHub OAuth login, user registration, external account linkage, consent, user settings ownership, authorization-relevant identity. |
| Owned Concepts | User, GitHubAccount, ExternalAccount, ConsentRecord, UserSetting reference. |
| Owned Aggregates | User Aggregate. |
| Inputs | OAuth authorization result, user account actions, consent decisions. |
| Outputs | Authenticated user identity, external account connection state, consent state. |
| Dependencies | GitHub OAuth provider, Notion OAuth provider for identity linkage. |
| Business Rules | A private source must belong to one authorized user scope. Revoked permission must block new private data use. Credential values must not be modeled as ordinary domain data. |
| Excluded Responsibilities | Repository analysis, score calculation, AI generation, prompt construction. |

### 5.3 Repository Context

| Aspect | Definition |
|---|---|
| Purpose | Represent GitHub repositories and immutable repository snapshots. |
| Responsibilities | Repository discovery, synchronization state, branch/commit/PR/issue/release/dependency/document capture, repository snapshot creation. |
| Owned Concepts | Repository, RepositorySnapshot, Commit, Branch, PullRequest, Issue, Release, Contributor, Dependency, DirectoryEntry, RepositoryDocument. |
| Owned Aggregates | Repository Aggregate, RepositorySnapshot Aggregate. |
| Inputs | GitHub normalized source data, source permissions, synchronization completion. |
| Outputs | Repository metadata, RepositorySnapshot, repository source facts. |
| Dependencies | Identity Context, GitHub external provider, Knowledge Context for document ingestion. |
| Business Rules | RepositorySnapshot is immutable after readiness. Repository current state must not be used as a substitute for a snapshot in official evaluation. |
| Excluded Responsibilities | Rule scoring, career readiness, company readiness, prompt generation. |

### 5.4 Rule Context

| Aspect | Definition |
|---|---|
| Purpose | Calculate deterministic technical evaluation results and Skill Matrix. |
| Responsibilities | Language, framework, database, architecture, testing, DevOps, documentation, collaboration, score, evidence, and Skill Matrix analysis. |
| Owned Concepts | Rule, RuleSet, RuleSetVersion, Evaluation, EvaluationResult, CategoryScore, OverallScore, SkillMatrix, SkillAssessment, Evidence. |
| Owned Aggregates | RuleSet Aggregate, Evaluation Aggregate, SkillMatrix Aggregate. |
| Inputs | RepositorySnapshot, Notion/document facts when eligible, active RuleSetVersion. |
| Outputs | Category scores, OverallScore, SkillMatrix, SkillEvidence, evaluation evidence links. |
| Dependencies | Repository Context, Knowledge Context, Administration Context. |
| Business Rules | LLM must never calculate scores. Completed evaluations are immutable. Every official score must reference rule version and evidence. |
| Excluded Responsibilities | Career recommendation narrative, LLM prompt composition, company coaching text. |

### 5.5 Career Context

| Aspect | Definition |
|---|---|
| Purpose | Evaluate developer readiness for selected career paths. |
| Responsibilities | Target career interpretation, career profile versioning, career-specific expectations, skill gap analysis, career readiness classification. |
| Owned Concepts | Career, CareerProfile, CareerProfileVersion, CareerReadiness, SkillGap, CompetencyExpectation. |
| Owned Aggregates | Career Aggregate, CareerAssessment Aggregate. |
| Inputs | User target career, SkillMatrix, CareerProfileVersion. |
| Outputs | CareerReadinessAssessment, SkillGap list, career-specific context for recommendations and prompts. |
| Dependencies | Identity Context, Rule Context, Administration Context. |
| Business Rules | Career readiness requires a SkillMatrix and CareerProfileVersion. Career readiness is not the same as OverallScore. |
| Excluded Responsibilities | Official technical score calculation, AI-generated coaching prose. |

### 5.6 Company Context

| Aspect | Definition |
|---|---|
| Purpose | Apply company-specific expectations to readiness and interview preparation. |
| Responsibilities | Company profile management, company-specific weights, company readiness, company-specific recommendation context, company interview focus. |
| Owned Concepts | Company, CompanyProfile, CompanyProfileVersion, CompanyReadiness, CompanyExpectation. |
| Owned Aggregates | Company Aggregate, CompanyAssessment Aggregate. |
| Inputs | Target company, SkillMatrix, CompanyProfileVersion, CareerReadinessAssessment. |
| Outputs | CompanyReadinessAssessment, company-specific gap context, interview focus context. |
| Dependencies | Career Context, Rule Context, Administration Context. |
| Business Rules | Missing target company must not block career readiness. Company-specific logic must not overwrite Rule Context scores. |
| Excluded Responsibilities | Base career selection, technical score generation, resume writing. |

### 5.7 Recommendation Context

| Aspect | Definition |
|---|---|
| Purpose | Convert assessed gaps and readiness into measurable recommendations. |
| Responsibilities | Recommendation candidate generation, priority calculation, conflict detection, recommendation status tracking. |
| Owned Concepts | Recommendation, RecommendationSet, RecommendationReason, RecommendationPriority, RecommendationEvidence. |
| Owned Aggregates | Recommendation Aggregate. |
| Inputs | SkillGap, CareerReadinessAssessment, CompanyReadinessAssessment, SkillMatrix evidence. |
| Outputs | Prioritized RecommendationSet. |
| Dependencies | Career Context, Company Context, Rule Context. |
| Business Rules | Recommendation priority is deterministic. Authoritative recommendations must reference evidence or skill gaps. |
| Excluded Responsibilities | Natural-language coaching text, roadmap progress tracking. |

### 5.8 Learning Context

| Aspect | Definition |
|---|---|
| Purpose | Convert recommendations into a measurable learning roadmap. |
| Responsibilities | Roadmap generation, roadmap step ordering, milestone definition, progress state tracking. |
| Owned Concepts | LearningRoadmap, RoadmapStep, Milestone, LearningResource, LearningProgress. |
| Owned Aggregates | LearningRoadmap Aggregate. |
| Inputs | RecommendationSet, accepted recommendations, user progress updates. |
| Outputs | LearningRoadmap, progress state, milestone completion. |
| Dependencies | Recommendation Context, Career Context. |
| Business Rules | Roadmap steps must be measurable and traceable to recommendations. Completed steps must remain historically traceable. |
| Excluded Responsibilities | Recommendation priority calculation, AI learning-plan prose generation. |

### 5.9 Knowledge Context

| Aspect | Definition |
|---|---|
| Purpose | Store, organize, index, and retrieve user-owned knowledge for grounded AI context. |
| Responsibilities | Knowledge ingestion, normalization, metadata extraction, versioning, chunking, embedding references, retrieval, freshness, deletion propagation. |
| Owned Concepts | KnowledgeDocument, KnowledgeDocumentVersion, KnowledgeChunk, EmbeddingRecord, RetrievalRequest, RetrievalResult, SourceReference. |
| Owned Aggregates | KnowledgeDocument Aggregate. |
| Inputs | GitHub documents, repository metadata, Notion notes, retrospectives, architecture documents, generated reports, future sources. |
| Outputs | RetrievalResult with evidence references and privacy scope. |
| Dependencies | Identity Context, Repository Context, Prompt Context. |
| Business Rules | LLM must not memorize user history. Long-term memory belongs to Knowledge Context. Private knowledge must remain user-isolated. |
| Excluded Responsibilities | AI generation, score calculation, prompt template management. |

### 5.10 Prompt Context

| Aspect | Definition |
|---|---|
| Purpose | Assemble validated prompts from structured context and reusable templates. |
| Responsibilities | Prompt template versioning, prompt variables, prompt context assembly, token budget validation, prompt validation, prompt logging. |
| Owned Concepts | PromptTemplate, PromptTemplateVersion, PromptContext, PromptExecution, PromptVariable. |
| Owned Aggregates | PromptTemplate Aggregate, PromptExecution Aggregate. |
| Inputs | AI task request, SkillMatrix, readiness results, recommendations, RetrievalResult, selected template version. |
| Outputs | Immutable PromptContext and PromptExecution request. |
| Dependencies | Knowledge Context, Rule Context, Career Context, Company Context, Recommendation Context. |
| Business Rules | Prompt Builder must never execute business logic or calculate scores. PromptContext is immutable after creation. |
| Excluded Responsibilities | LLM execution, response generation, deterministic evaluation. |

### 5.11 AI Context

| Aspect | Definition |
|---|---|
| Purpose | Generate validated natural-language responses and AI-assisted artifacts. |
| Responsibilities | AI task execution, model selection, fallback handling, response validation, hallucination prevention, generated artifact creation. |
| Owned Concepts | AITask, ModelExecution, AIResponse, ResponseValidationResult, GeneratedArtifact. |
| Owned Aggregates | AITask Aggregate, GeneratedArtifact Aggregate. |
| Inputs | PromptExecution, PromptContext, model configuration. |
| Outputs | Validated AIResponse, GeneratedArtifact. |
| Dependencies | Prompt Context, Knowledge Context, Portfolio Context. |
| Business Rules | AI must explain, summarize, rewrite, and generate text only. AI must never calculate scores, readiness values, rule weights, or recommendation priorities. |
| Excluded Responsibilities | Rule execution, recommendation prioritization, official evidence creation. |

### 5.12 Portfolio Context

| Aspect | Definition |
|---|---|
| Purpose | Manage user-owned career artifacts such as portfolio, resume, README improvement, and interview question outputs. |
| Responsibilities | Portfolio versions, resume versions, artifact review, user edits, publication state, artifact provenance. |
| Owned Concepts | Portfolio, PortfolioVersion, PortfolioSection, Resume, ResumeVersion, ResumeSection, READMEImprovement, InterviewQuestion. |
| Owned Aggregates | Portfolio Aggregate, Resume Aggregate, InterviewQuestionSet Aggregate. |
| Inputs | GeneratedArtifact, Project, SkillMatrix, CareerReadiness, user edits. |
| Outputs | Reviewed or published portfolio/resume/interview artifacts. |
| Dependencies | AI Context, Repository Context, Project concept, Rule Context. |
| Business Rules | Published artifact versions are immutable. Generated content must remain distinguishable from user-edited content. |
| Excluded Responsibilities | AI response generation, score calculation, recommendation priority calculation. |

### 5.13 Administration Context

| Aspect | Definition |
|---|---|
| Purpose | Govern versioned business configuration and operational oversight. |
| Responsibilities | Rule management, career management, company rule management, prompt management, logs, statistics, configuration activation and deprecation. |
| Owned Concepts | ConfigurationChange, AdminActor, ManagedRuleSetVersion, ManagedCareerProfileVersion, ManagedCompanyProfileVersion, ManagedPromptTemplateVersion, AuditRecord reference. |
| Owned Aggregates | Configuration Aggregate. |
| Inputs | Admin commands, validation results, operational metrics. |
| Outputs | Activated/deprecated configuration versions, audit events, administration views. |
| Dependencies | Rule Context, Career Context, Company Context, Prompt Context, Audit capability. |
| Business Rules | Active configuration changes affect future evaluations unless explicit recalculation is requested. Historical results retain original version references. |
| Excluded Responsibilities | User-owned private content editing, repository synchronization, AI response generation. |

## 6. Context Relationships

### 6.1 Primary Information Flow

| Step | Flow | Business Meaning |
|---|---|---|
| 1 | Identity Context → Repository Context | Authenticated user authorizes GitHub access. |
| 2 | Repository Context → Rule Context | Immutable repository snapshots become deterministic evaluation input. |
| 3 | Rule Context → Career Context | Skill Matrix and evidence-backed scores become career assessment input. |
| 4 | Career Context → Company Context | Career readiness and Skill Matrix help calculate company readiness when a target company exists. |
| 5 | Career and Company Contexts → Recommendation Context | Skill gaps and readiness become deterministic recommendation candidates. |
| 6 | Recommendation Context → Learning Context | Prioritized recommendations become roadmap steps. |
| 7 | Knowledge Context → Prompt Context | Retrieved knowledge chunks become prompt context candidates. |
| 8 | Rule/Career/Company/Recommendation/Learning Contexts → Prompt Context | Structured outputs become prompt variables and constraints. |
| 9 | Prompt Context → AI Context | Validated prompt execution becomes LLM input. |
| 10 | AI Context → Portfolio Context | Validated AI output becomes generated career artifacts. |

### 6.2 Context Relationship Matrix

| Upstream Context | Downstream Context | Relationship Type | Ownership | Dependency | Shared Concepts |
|---|---|---|---|---|---|
| Identity | Repository | Customer/Supplier | Identity owns User and permission scope. Repository owns Repository. | Repository requires authorized user scope. | UserId, PermissionScope |
| Repository | Rule | Published Language | Repository owns RepositorySnapshot. Rule owns EvaluationResult. | Rule requires immutable snapshot input. | RepositorySnapshot, EvidenceSource |
| Rule | Career | Customer/Supplier | Rule owns SkillMatrix. Career owns CareerReadiness. | Career requires SkillMatrix. | SkillMatrix, SkillAssessment |
| Rule | Company | Customer/Supplier | Rule owns SkillMatrix. Company owns CompanyReadiness. | Company requires SkillMatrix and profile version. | SkillMatrix, SkillEvidence |
| Career | Recommendation | Customer/Supplier | Career owns SkillGap. Recommendation owns RecommendationSet. | Recommendation requires gaps/readiness. | SkillGap, CareerReadiness |
| Company | Recommendation | Customer/Supplier | Company owns CompanyReadiness. Recommendation owns recommendations. | Company readiness modifies recommendation context without mutating scores. | CompanyReadiness, CompanyExpectation |
| Recommendation | Learning | Customer/Supplier | Recommendation owns priority. Learning owns roadmap progress. | Roadmap requires recommendation set. | Recommendation, Priority |
| Knowledge | Prompt | Open Host Service | Knowledge owns retrieval results. Prompt owns final context package. | Prompt may request relevant knowledge. | RetrievalResult, SourceReference |
| Prompt | AI | Customer/Supplier | Prompt owns PromptContext. AI owns AIResponse. | AI requires locked prompt execution. | PromptExecution |
| AI | Portfolio | Published Language | AI owns generated response. Portfolio owns reviewed artifact versions. | Portfolio consumes validated generated artifact. | GeneratedArtifact |
| Administration | Rule/Career/Company/Prompt | Customer/Supplier | Administration initiates configuration changes; target context owns active version semantics. | Each context validates its own configuration. | Version, ConfigurationChange |

### 6.3 Shared Concept Rules

| Shared Concept | Rule |
|---|---|
| UserId | May be referenced by all user-owned contexts but is owned by Identity Context. |
| Evidence | Must be created from traceable source observations and owned by Rule Context or Knowledge Context according to purpose. |
| Score | Owned only by Rule Context. No other context may create or modify official scores. |
| Readiness | Owned by Career or Company Context, not Rule Context and not AI Context. |
| Recommendation Priority | Owned by Recommendation Context. AI may explain but not assign it. |
| PromptContext | Owned by Prompt Context and immutable after creation. |
| GeneratedArtifact | Owned by AI Context until converted into reviewed Portfolio/Resume/Interview artifact lifecycle. |

## 7. Core Entities

### 7.1 Entity Catalog

| Entity | Purpose | Responsibilities | Conceptual Attributes | Relationships | Lifecycle | Business Constraints | Owner Aggregate | Related Domain Services | Related Events |
|---|---|---|---|---|---|---|---|---|---|
| User | Represents a DevPath account owner. | Own private data, connect accounts, select career/company, manage settings. | User identity, account status, role, consent state. | Owns GitHubAccount, NotionWorkspace connection, artifacts, analyses. | Registered, Active, Suspended, DeletionRequested, Deleted. | User cannot access another user's private data. | User Aggregate | Authorization Service | UserRegistered, UserDeleted |
| GitHubAccount | Represents connected GitHub identity. | Maintain provider identity and permission scope. | Provider identity, username, scopes, connection status. | Belongs to User; authorizes Repository sync. | Pending, Connected, PermissionChanged, Revoked, Disconnected. | Token values are not ordinary domain attributes. | User Aggregate | Integration Authorization Service | GitHubAccountConnected, IntegrationPermissionChanged |
| NotionWorkspace | Represents connected Notion workspace. | Authorize learning notes, retrospectives, and documents. | Workspace reference, permission scope, connection status. | Belongs to User; supplies KnowledgeDocument sources. | Pending, Connected, PermissionChanged, Revoked, Disconnected. | Revoked permission blocks future ingestion and retrieval eligibility. | User Aggregate | Integration Authorization Service | NotionWorkspaceConnected |
| Repository | Canonical source repository. | Track metadata and current synchronization state. | Repository identity, provider reference, full name, visibility, default branch, archive state. | Has RepositorySnapshots; may relate to Projects. | Discovered, Active, Synchronized, Archived, DeletedExternally. | Repository current state cannot replace immutable snapshot for evaluation. | Repository Aggregate | Repository Analysis Service | RepositorySynced |
| RepositorySnapshot | Immutable repository capture. | Preserve reproducible repository state for analysis. | Snapshot identity, repository reference, source revision, captured time, content references. | Contains branches, commits, PRs, issues, dependencies, documents. | Capturing, Ready, Failed, Superseded, DeletedByPolicy. | Immutable after Ready. | RepositorySnapshot Aggregate | Repository Analysis Service | RepositorySnapshotCreated |
| Project | Career-relevant body of work. | Represent user-visible project story, project role, technology usage, and evidence references. | Project identity, title, description, role, linked repositories, linked documents. | References repositories, documents, generated artifacts. | Draft, Active, Reviewed, Archived. | Project is not the same as repository. | Portfolio Aggregate or Project sub-aggregate | Portfolio Generation Service | ProjectIdentified |
| Technology | Canonical technology reference. | Classify languages, frameworks, libraries, databases, tools, and platforms. | Technology identity, name, category, aliases, lifecycle status. | Used by Dependency, SkillEvidence, Project. | Active, Deprecated, Merged. | Technology usage alone does not prove a Skill. | RuleSet Aggregate reference catalog | Skill Evaluation Service | TechnologyDetected |
| Framework | Specialized Technology. | Represent framework usage evidence. | Framework name, ecosystem, version reference, detected source. | Related to Repository dependencies and Skill evidence. | Detected, Confirmed, Deprecated. | Must be distinguished from language and library. | RuleSet Aggregate reference catalog | Skill Evaluation Service | FrameworkDetected |
| Skill | Developer capability. | Represent assessed competency. | Skill identity, category, name, description, level scale. | Assessed by SkillMatrix; compared by CareerProfile. | Active, Deprecated, Reclassified. | Skill must be supported by evidence for official assessment. | SkillMatrix Aggregate reference catalog | Skill Evaluation Service | SkillAssessed |
| Evidence | Traceable observed fact. | Support scores, skills, readiness, or recommendations. | Evidence identity, source reference, observed fact, confidence, timestamp, freshness. | Linked to Rule, Score, SkillMatrix, Recommendation. | Extracted, Accepted, Rejected, Superseded, DeletedByPolicy. | Evidence requires source and timestamp. | Evaluation Aggregate | Evidence Extraction Service | EvidenceAccepted |
| SkillMatrix | Structured skill assessment result. | Store evidence-backed skill levels and strengths/weaknesses. | Matrix identity, evaluation reference, skill assessments, confidence, generated time. | References EvaluationResult and Evidence. | Generated, Published, Superseded, Archived. | One SkillMatrix belongs to exactly one analysis/evaluation basis. | SkillMatrix Aggregate | Skill Evaluation Service | SkillMatrixGenerated |
| Rule | Deterministic evaluation definition. | Calculate measurable signals and score contributions. | Rule identity, category, condition, outcome, version. | Belongs to RuleSetVersion; produces RuleExecutionResult. | Draft, Active, Deprecated. | LLM must never execute or replace official rule logic. | RuleSet Aggregate | Skill Evaluation Service | RuleActivated |
| Career | Supported career path. | Identify target career for readiness and roadmap generation. | Career identity, name, supported status. | Has CareerProfile versions; selected by User. | Supported, Deprecated, FutureCandidate. | Official assessment requires supported Career. | Career Aggregate | Career Recommendation Service | CareerChanged |
| CareerProfile | Versioned career expectation set. | Define competencies, thresholds, and weights for a career. | Profile identity, career reference, version, competency expectations, effective state. | Used by CareerReadiness assessment. | Draft, Active, Superseded, Deprecated. | Historical assessments retain profile version. | Career Aggregate | Career Recommendation Service | CareerProfileActivated |
| Company | Supported target company. | Identify company-specific readiness context. | Company identity, name, supported status. | Has CompanyProfile versions; selected by User. | Supported, Deprecated, FutureCandidate. | Missing Company must not block Career assessment. | Company Aggregate | Career Recommendation Service | CompanyChanged |
| CompanyProfile | Versioned company expectation set. | Define company-specific weights, recommendations, and interview focus. | Profile identity, company reference, version, expectations, effective state. | Used by CompanyReadiness and InterviewQuestion generation. | Draft, Active, Superseded, Deprecated. | Must not mutate original Rule scores. | Company Aggregate | Career Recommendation Service | CompanyProfileActivated |
| Recommendation | Evidence-based suggested action. | Explain what user should improve and why. | Recommendation identity, type, reason, priority, status, evidence/gap references. | Belongs to RecommendationSet; may create RoadmapStep. | Proposed, Accepted, Dismissed, Completed, Superseded. | Must reference evidence or skill gap. | Recommendation Aggregate | Career Recommendation Service | RecommendationGenerated |
| LearningRoadmap | Ordered learning plan. | Organize measurable learning actions and progress. | Roadmap identity, source recommendation set, steps, milestones, progress state. | Contains RoadmapSteps; references recommendations. | Created, InProgress, Completed, Archived. | Roadmap steps must be measurable. | LearningRoadmap Aggregate | Learning Roadmap Service | LearningRoadmapCreated |
| KnowledgeDocument | Long-term knowledge source. | Preserve source document versions and retrieval eligibility. | Document identity, source type, owner, metadata, privacy class, freshness. | Contains KnowledgeChunks; has document versions. | Discovered, Ingested, Indexed, Stale, Deleted. | Private knowledge must remain user-isolated. | KnowledgeDocument Aggregate | Knowledge Retrieval Service | KnowledgeUpdated |
| KnowledgeChunk | Retrievable document segment. | Support semantic retrieval and prompt grounding. | Chunk identity, document version reference, position, content hash, metadata. | Belongs to one KnowledgeDocumentVersion; may have EmbeddingRecord. | Created, Embedded, Indexed, Stale, Deleted. | Each chunk belongs to exactly one KnowledgeDocument. | KnowledgeDocument Aggregate | Knowledge Retrieval Service | KnowledgeChunkIndexed |
| PromptTemplate | Reusable prompt structure. | Define task-specific prompt composition rules. | Template identity, category, version, variables, constraints, output format. | Used by PromptContext and PromptExecution. | Draft, Active, Superseded, Deprecated. | Active execution must reference a template version. | PromptTemplate Aggregate | Prompt Composition Service | PromptTemplateActivated |
| PromptContext | Immutable task context package. | Bind structured data, retrieved evidence, template variables, and token budget. | Context identity, task type, source references, variables, token budget, creation time. | References SkillMatrix, readiness, recommendations, RetrievalResult, PromptTemplateVersion. | Created, Validated, Locked, Rejected. | Immutable after creation/lock. | PromptExecution Aggregate | Prompt Composition Service | PromptGenerated |
| GeneratedArtifact | AI-assisted generated output. | Represent validated AI output before or during artifact lifecycle. | Artifact identity, type, source prompt, validation status, provenance, owner. | May become Portfolio, Resume, README draft, or InterviewQuestionSet. | Draft, Validated, Reviewed, Approved, Published, Superseded. | Must distinguish AI-generated content from source evidence and user edits. | GeneratedArtifact Aggregate | Portfolio Generation Service | GeneratedArtifactCreated |
| Portfolio | User-owned career presentation artifact. | Present projects, skills, evidence, and generated narrative. | Portfolio identity, owner, versions, sections, publication status. | References Projects, SkillMatrix, GeneratedArtifact. | Draft, Generated, Reviewed, Published, Archived. | Generated portfolio must reference Projects. | Portfolio Aggregate | Portfolio Generation Service | PortfolioGenerated |
| Resume | User-owned resume artifact. | Present career profile, skills, projects, and experience. | Resume identity, owner, versions, sections, export status. | References SkillMatrix, Projects, GeneratedArtifact. | Draft, Generated, Reviewed, Published, Archived. | Published resume version is immutable. | Resume Aggregate | Portfolio Generation Service | ResumeGenerated |
| InterviewQuestion | Generated preparation question. | Help user prepare for target career and company interviews. | Question identity, career/company context, difficulty, topic, evidence reference. | Belongs to InterviewQuestionSet; references SkillGap or CompanyProfile. | Generated, Reviewed, Practiced, Archived. | Must be grounded in supported context. | GeneratedArtifact or InterviewQuestionSet Aggregate | Portfolio Generation Service | InterviewQuestionsGenerated |

### 7.2 Entity Ownership Notes

| Concept | Modeling Decision |
|---|---|
| Project | Modeled as a career artifact concept, not as a repository replacement. It may later become an independent aggregate if project curation becomes complex. |
| Framework | Modeled as a specialized Technology, not a separate root aggregate. |
| InterviewQuestion | Modeled as a generated artifact component unless future interview practice workflows require a dedicated aggregate. |
| Evidence | Owned by deterministic analysis and evaluation flows. AI output may cite evidence but does not create official evidence. |

## 8. Value Objects

| Value Object | Purpose | Meaning | Validation Rules | Equality | Immutability | Related Entities |
|---|---|---|---|---|---|---|
| Score | Represent official deterministic evaluation value. | Numeric result generated only by Rule Context. | Must be within configured scale; must reference score type and rule version. | Same value, scale, and score type. | Immutable. | EvaluationResult, SkillMatrix |
| Weight | Represent configured importance factor. | Determines contribution of category, skill, career, or company expectation. | Non-negative; must satisfy normalization policy in a versioned set. | Same value and scope. | Immutable once profile/rule version is active. | RuleSet, CareerProfile, CompanyProfile |
| Confidence | Represent evidence or response reliability. | Bounded confidence for evidence, skill assessment, retrieval, or AI validation. | Must be within defined range and have a basis. | Same value and basis. | Immutable in completed result. | Evidence, SkillMatrix, AIResponse |
| Priority | Represent deterministic recommendation ordering. | Business priority assigned by Recommendation Context. | Must be derived from deterministic policy; cannot be assigned by LLM. | Same rank and policy version. | Immutable after recommendation publication. | Recommendation, RoadmapStep |
| Difficulty | Represent learning or interview challenge level. | Indicates expected challenge of roadmap step or question. | Must use configured difficulty scale. | Same scale and level. | Immutable unless regenerated. | RoadmapStep, InterviewQuestion |
| TechnologyLevel | Represent depth of demonstrated technology usage. | Describes observed sophistication, not a human skill by itself. | Must reference evidence and technology category. | Same technology, level, and basis. | Immutable in assessment result. | Technology, SkillMatrix |
| LearningDuration | Represent estimated time to complete learning activity. | Duration for a roadmap step or milestone. | Must be positive and use supported unit. | Same amount and unit. | Immutable in generated roadmap version. | RoadmapStep |
| Similarity | Represent retrieval similarity. | Ranking measure for knowledge retrieval. | Must include metric and bounded value. | Same metric and value. | Immutable in RetrievalResult. | KnowledgeChunk, RetrievalResult |
| RoadmapStep | Represent an ordered learning action as value-like plan item when not independently managed. | Action, skill target, duration, difficulty, completion criteria. | Must be measurable and linked to recommendation. | Same roadmap, order, and action identity. | Immutable in generated roadmap version; progress may be entity state. | LearningRoadmap |
| Version | Represent immutable definition or artifact version. | Stable version reference for rules, profiles, prompts, documents, and artifacts. | Must be unique within owning definition. | Same owner identity and version. | Immutable. | RuleSet, CareerProfile, CompanyProfile, PromptTemplate, KnowledgeDocument |
| Reference | Represent cross-aggregate or source reference. | Safe pointer to another aggregate, snapshot, version, or source. | Must include referenced type and identifier; source references include provider and location. | Same reference target. | Immutable. | Evidence, PromptContext, GeneratedArtifact |
| TokenBudget | Represent prompt input/output budget. | Maximum context and response size for AI task. | Must be positive and within selected model constraints. | Same input, output, and reserved budget. | Immutable for PromptContext. | PromptContext |
| ReadinessLevel | Represent career or company readiness class. | Deterministic readiness label based on profile thresholds. | Must be one of configured levels and reference profile version. | Same level and profile version. | Immutable in completed assessment. | CareerReadiness, CompanyReadiness |
| SourceReference | Represent provenance reference. | Provider, source object, location, timestamp, privacy class. | Must include source type and owner scope. | Same source, location, and timestamp. | Immutable. | Evidence, KnowledgeDocument, PromptContext |

## 9. Aggregates

| Aggregate | Aggregate Root | Included Entities | Included Value Objects | Business Boundary | Consistency Rules | Lifecycle | Business Constraints |
|---|---|---|---|---|---|---|---|
| User Aggregate | User | GitHubAccount, NotionWorkspace reference, ConsentRecord | User identity, PermissionScope, Reference | Owns identity, consent, and external account connection state. | A private source belongs to one authorized user scope. | Registered → Active → Suspended/DeletionRequested → Deleted. | Credential values are stored only as secure references, not domain attributes. |
| Repository Aggregate | Repository | Repository metadata, synchronization summary | Repository identity, RepositoryFullName, SourceReference | Owns current repository record. | Repository identity is stable even if provider display name changes. | Discovered → Active/Synchronized → Archived/DeletedExternally. | Current repository state cannot be official evaluation input without snapshot. |
| RepositorySnapshot Aggregate | RepositorySnapshot | Branch, Commit, PullRequest, Issue, Release, Dependency, DirectoryEntry, RepositoryDocument | Snapshot identity, CommitHash, ContentHash, SourceReference | Owns immutable captured source state. | Snapshot is immutable after Ready. | Capturing → Ready/Failed → Superseded/DeletedByPolicy. | All official analysis must reference snapshot identity. |
| RuleSet Aggregate | RuleSet | Rule, RuleSetVersion, RuleOverride | RuleVersion, Weight, Threshold, Score scale | Owns deterministic evaluation definitions. | Active version must validate before evaluation. | Draft → Active → Superseded/Deprecated. | Rule changes affect future evaluations only unless explicit recalculation occurs. |
| Evaluation Aggregate | Evaluation | EvaluationResult, CategoryEvaluation, RuleExecutionResult, ScoreEvidenceLink, Evidence | Score, Confidence, Reference | Owns official deterministic evaluation result. | Completed EvaluationResult is immutable and evidence-linked. | Requested → Running → Completed/Failed → Superseded. | LLM output cannot alter evaluation result. |
| SkillMatrix Aggregate | SkillMatrix | SkillAssessment, SkillEvidence, GrowthAssessment | Skill level, Confidence, TechnologyLevel | Owns published skill assessment and history. | Every official SkillAssessment must reference evidence. | Generated → Published → Superseded/Archived. | Historical skill matrices must not be overwritten. |
| Career Aggregate | Career | CareerProfile, CareerProfileVersion, CompetencyExpectation | Version, Weight, Threshold | Owns target career definitions. | CareerProfileVersion is immutable after activation. | Draft → Active → Superseded/Deprecated. | Readiness references exact profile version. |
| CareerAssessment Aggregate | CareerReadiness | SkillGap | ReadinessLevel, GapMagnitude, Reference | Owns career readiness result for a SkillMatrix. | Career readiness requires SkillMatrix and CareerProfileVersion. | Requested → Completed/Failed → Superseded. | Career readiness is not OverallScore. |
| Company Aggregate | Company | CompanyProfile, CompanyProfileVersion, CompanyExpectation | Version, Weight, Threshold | Owns company-specific expectations. | CompanyProfileVersion is immutable after activation. | Draft → Active → Superseded/Deprecated. | Company rules must not mutate Rule scores. |
| CompanyAssessment Aggregate | CompanyReadiness | Company readiness detail | ReadinessLevel, Confidence, Reference | Owns company readiness result. | Requires target company and CompanyProfileVersion. | Requested → Completed/Failed → Superseded. | Missing company creates no company assessment, not an error for career assessment. |
| Recommendation Aggregate | RecommendationSet | Recommendation, RecommendationEvidence | Priority, RecommendationReason, Reference | Owns recommendation candidates, priority, and status. | Recommendation priority is deterministic. | Draft → Published → Accepted/Dismissed/Completed/Superseded. | Authoritative recommendations require evidence or skill gap. |
| LearningRoadmap Aggregate | LearningRoadmap | RoadmapStep, Milestone, LearningResource | Difficulty, LearningDuration, Priority | Owns roadmap plan and progress state. | Roadmap steps must be measurable and ordered. | Created → InProgress → Completed/Archived. | Progress changes must not rewrite recommendation history. |
| KnowledgeDocument Aggregate | KnowledgeDocument | KnowledgeDocumentVersion, KnowledgeChunk, EmbeddingRecord | ContentHash, Similarity, SourceReference, Version | Owns long-term knowledge and retrieval eligibility. | KnowledgeChunk belongs to one document version. | Discovered → Ingested → Indexed → Stale/Deleted. | Private knowledge must remain user-isolated. |
| PromptTemplate Aggregate | PromptTemplate | PromptTemplateVersion, PromptVariable definition | Version, TokenBudget | Owns reusable prompt definitions. | Active prompt template version must validate variables and constraints. | Draft → Active → Superseded/Deprecated. | Prompt templates must not encode score calculation logic. |
| PromptExecution Aggregate | PromptContext | PromptExecution | TokenBudget, Reference, Version | Owns immutable prompt context for one AI task. | PromptContext is immutable after creation. | Created → Validated → Locked → Submitted/Rejected. | Prompt Builder only assembles prompts. |
| AITask Aggregate | AITask | ModelExecution, AIResponse, ResponseValidationResult | Confidence, Reference, ModelIdentifier | Owns AI execution and response validation. | AI response cannot become official evidence without deterministic validation. | Requested → Running → Completed/Failed/Canceled. | AI cannot calculate scores, readiness, or priority. |
| GeneratedArtifact Aggregate | GeneratedArtifact | Artifact version/provenance records | ArtifactType, Version, Reference | Owns generated output before specialized artifact lifecycle. | Generated content must be distinguishable from source content. | Draft → Validated → Reviewed → Approved/Rejected. | Artifact must reference PromptContext and source references. |
| Portfolio Aggregate | Portfolio | PortfolioVersion, PortfolioSection | Version, Reference | Owns portfolio lifecycle and publication. | Published version is immutable. | Draft → Generated → Reviewed → Published → Archived. | Generated portfolio must reference projects. |
| Resume Aggregate | Resume | ResumeVersion, ResumeSection | Version, Reference | Owns resume lifecycle and publication. | Published version is immutable. | Draft → Generated → Reviewed → Published → Archived. | Resume claims must trace to SkillMatrix, Project, or Evidence. |

## 10. Domain Services

| Domain Service | Purpose | Inputs | Outputs | Business Responsibility | Dependencies |
|---|---|---|---|---|---|
| Repository Analysis Service | Determine whether repository data is ready for deterministic analysis. | RepositorySnapshot, repository metadata, source permissions. | RepositoryAnalysis readiness result. | Validate snapshot readiness and select source facts for Rule Context. | Repository Context, Identity Context |
| Skill Evaluation Service | Execute deterministic skill and technical evaluation. | RepositorySnapshot, normalized facts, RuleSetVersion. | EvaluationResult, SkillMatrix, Evidence links. | Calculate official scores and skill assessments. | Rule Context, Repository Context |
| Career Recommendation Service | Assess career and company readiness and produce deterministic recommendation inputs. | SkillMatrix, CareerProfileVersion, CompanyProfileVersion, user target selections. | CareerReadiness, CompanyReadiness, SkillGap, recommendation candidates. | Apply career/company expectations without modifying Rule scores. | Career Context, Company Context, Rule Context |
| Recommendation Prioritization Service | Assign deterministic priorities and resolve conflicts. | Skill gaps, readiness results, evidence, recommendation policies. | RecommendationSet. | Produce actionable, ranked, evidence-based recommendations. | Recommendation Context |
| Learning Roadmap Service | Convert recommendations into ordered learning plan. | RecommendationSet, learning constraints, accepted recommendations. | LearningRoadmap with measurable steps. | Create roadmap and track progress. | Learning Context, Recommendation Context |
| Knowledge Ingestion Service | Convert eligible source documents into versioned knowledge. | Source documents, metadata, permissions, content hash. | KnowledgeDocumentVersion, KnowledgeChunk, EmbeddingRecord reference. | Store long-term knowledge while preserving privacy and freshness. | Knowledge Context, Identity Context |
| Knowledge Retrieval Service | Retrieve relevant knowledge for search and AI grounding. | Retrieval intent, metadata filters, user scope, token budget. | RetrievalResult. | Return ranked, user-isolated knowledge with source references. | Knowledge Context |
| Prompt Composition Service | Assemble prompt context from structured outputs and retrieved knowledge. | AI task, template version, SkillMatrix, readiness, recommendations, RetrievalResult. | PromptContext, PromptExecution. | Validate prompt completeness, variables, and token budget. | Prompt Context, Knowledge Context |
| AI Response Validation Service | Validate generated AI output against context and output rules. | AIResponse, PromptContext, expected output model. | ResponseValidationResult. | Detect unsupported claims, invalid format, and grounding failures. | AI Context, Prompt Context |
| Portfolio Generation Service | Convert validated generated artifacts into user-reviewable career artifacts. | GeneratedArtifact, Project references, SkillMatrix, user edits. | PortfolioVersion, ResumeVersion, InterviewQuestionSet, READMEImprovement. | Preserve provenance and manage artifact lifecycle. | AI Context, Portfolio Context |

## 11. Domain Events

| Event | Purpose | Producer | Consumers | Business Meaning |
|---|---|---|---|---|
| UserRegistered | Record creation of a DevPath user. | Identity Context | Profile, Audit, Notification | A new account owner exists. |
| GitHubAccountConnected | Record successful GitHub OAuth connection. | Identity Context | Repository Context, Audit | User authorized GitHub source access. |
| NotionWorkspaceConnected | Record successful Notion workspace connection. | Identity Context | Knowledge Context, Audit | User authorized Notion knowledge access. |
| RepositorySynced | Record completion of repository synchronization. | Repository Context | Rule Context, Knowledge Context, Notification | Repository data is available for snapshot or analysis. |
| RepositorySnapshotCreated | Record creation of immutable repository snapshot. | Repository Context | Rule Context, Knowledge Context | A reproducible source state exists. |
| RepositoryAnalyzed | Record completion of repository analysis preparation. | Repository Context | Rule Context, Dashboard projections | Repository facts are ready for deterministic evaluation. |
| EvaluationCompleted | Record completion of deterministic rule evaluation. | Rule Context | SkillMatrix generation, Career Context, Dashboard | Official scores and evidence links exist. |
| SkillMatrixGenerated | Record generation of Skill Matrix. | Rule Context | Career Context, Recommendation Context, Prompt Context | Evidence-backed skill assessment is available. |
| CareerChanged | Record target career change. | Identity/Career Context | Career Context, Recommendation Context | Future readiness uses the selected career. |
| CompanyChanged | Record target company change. | Identity/Company Context | Company Context, Recommendation Context, AI Context | Future company readiness and interview context use the selected company. |
| CareerReadinessAssessed | Record completed career readiness assessment. | Career Context | Recommendation Context, Prompt Context, Dashboard | Career fit and gaps are known. |
| CompanyReadinessAssessed | Record completed company readiness assessment. | Company Context | Recommendation Context, Prompt Context, Interview generation | Company-specific readiness is known. |
| RecommendationGenerated | Record generated recommendation set. | Recommendation Context | Learning Context, Prompt Context, Dashboard | Deterministic actions are available. |
| LearningRoadmapCreated | Record creation of roadmap. | Learning Context | Dashboard, Prompt Context, Notification | User has measurable learning plan. |
| RoadmapStepCompleted | Record completion of roadmap step. | Learning Context | Dashboard, Recommendation Context | User made measurable learning progress. |
| KnowledgeUpdated | Record new, updated, stale, or deleted knowledge. | Knowledge Context | Prompt Context, Search projections | Retrieval source state changed. |
| KnowledgeIndexed | Record completed knowledge indexing. | Knowledge Context | Prompt Context | Knowledge is eligible for retrieval. |
| PromptGenerated | Record creation of PromptContext. | Prompt Context | AI Context, Audit | Immutable prompt execution context exists. |
| PromptExecutionFailed | Record prompt preparation or execution failure. | Prompt Context or AI Context | Notification, Audit | AI task cannot proceed without remediation. |
| AIResponseValidated | Record validated AI response. | AI Context | Portfolio Context, Dashboard | Generated response passed format and grounding validation. |
| AIResponseRejected | Record rejected AI response. | AI Context | Notification, Audit | Generated response must not become user-facing artifact. |
| GeneratedArtifactCreated | Record generated artifact creation. | AI Context | Portfolio Context | AI output became a managed generated artifact. |
| PortfolioGenerated | Record portfolio draft/version generation. | Portfolio Context | Dashboard, Notification | User has portfolio artifact to review. |
| ResumeGenerated | Record resume draft/version generation. | Portfolio Context | Dashboard, Notification | User has resume artifact to review. |
| InterviewQuestionsGenerated | Record interview question set generation. | Portfolio/AI Context | Dashboard, Notification | User has interview preparation material. |
| ArtifactPublished | Record publication of reviewed artifact. | Portfolio Context | Dashboard, Audit | Published artifact version is immutable. |
| ConfigurationChanged | Record administrative configuration change. | Administration Context | Rule, Career, Company, Prompt contexts | Future operations may use new active version. |

## 12. Domain Invariants

| Invariant ID | Invariant |
|---|---|
| INV-001 | A User owns all private GitHub, Notion, knowledge, prompt, AI, portfolio, and resume data created under that account. |
| INV-002 | A User cannot access another user's private Repository, KnowledgeDocument, PromptContext, AIResponse, Portfolio, or Resume. |
| INV-003 | Revoked external permissions must block new synchronization, retrieval, prompt inclusion, and AI use of private source data. |
| INV-004 | RepositorySnapshot is immutable after it reaches Ready state. |
| INV-005 | Official analysis must reference RepositorySnapshot or another immutable source snapshot. |
| INV-006 | Rule Context is the only context allowed to calculate official technical scores. |
| INV-007 | LLM output must never calculate or modify Score, Weight, OverallScore, CareerReadiness, CompanyReadiness, or RecommendationPriority. |
| INV-008 | Completed EvaluationResult is immutable. |
| INV-009 | Every official Score must reference RuleSetVersion and supporting Evidence. |
| INV-010 | Every Score must remain within the configured score range. |
| INV-011 | Active RuleSetVersion must pass weight and rule validation before evaluation. |
| INV-012 | SkillMatrix belongs to exactly one completed evaluation basis. |
| INV-013 | Every official SkillAssessment must reference Evidence. |
| INV-014 | Historical SkillMatrix records must not be overwritten. |
| INV-015 | CareerReadiness cannot exist without SkillMatrix and CareerProfileVersion. |
| INV-016 | CareerReadiness is not equivalent to OverallScore. |
| INV-017 | CompanyReadiness cannot exist without selected Company and CompanyProfileVersion. |
| INV-018 | Missing target Company must not prevent CareerReadiness assessment. |
| INV-019 | Company-specific weights must not mutate original Rule Context scores. |
| INV-020 | Recommendation must reference Evidence, SkillGap, readiness result, or deterministic reason. |
| INV-021 | RecommendationPriority must be deterministic and policy-based. |
| INV-022 | Dismissed Recommendation must remain historically traceable. |
| INV-023 | LearningRoadmap must reference the RecommendationSet from which it was created. |
| INV-024 | RoadmapStep must have measurable completion criteria. |
| INV-025 | KnowledgeChunk belongs to exactly one KnowledgeDocumentVersion. |
| INV-026 | EmbeddingRecord must reference embedding model and version. |
| INV-027 | Private knowledge must remain user-isolated during indexing and retrieval. |
| INV-028 | Deleted knowledge must propagate to derived chunks, embeddings, and retrieval indexes. |
| INV-029 | PromptContext is immutable after creation or lock. |
| INV-030 | PromptExecution must reference PromptTemplateVersion. |
| INV-031 | Prompt Builder must not execute business rules. |
| INV-032 | AIResponse cannot become official Evidence without deterministic validation against source data. |
| INV-033 | GeneratedArtifact must retain source PromptContext and provenance. |
| INV-034 | AI-generated content must be distinguishable from user-edited content. |
| INV-035 | Generated Portfolio must reference Projects or project evidence. |
| INV-036 | Resume claims must be traceable to SkillMatrix, Project, Evidence, or user-provided profile data. |
| INV-037 | Published Portfolio and Resume versions are immutable. |
| INV-038 | Configuration changes affect future operations and must not silently rewrite historical results. |

## 13. Entity Lifecycle

### 13.1 Repository Lifecycle

| State | Meaning | Valid Next States |
|---|---|---|
| Discovered | Repository was found through GitHub synchronization. | Active, Archived |
| Active | Repository is available for synchronization and snapshot creation. | Synchronized, Archived, DeletedExternally |
| Synchronized | Latest allowed source metadata has been collected. | Active, Analyzed, Archived |
| Analyzed | Repository snapshot has been used for analysis. | Synchronized, Archived |
| Archived | Repository is inactive but retained. | Active, DeletedExternally |
| DeletedExternally | External source no longer exists or is unavailable. | Archived |

### 13.2 RepositorySnapshot Lifecycle

| State | Meaning | Valid Next States |
|---|---|---|
| Capturing | Snapshot is being assembled. | Ready, Failed |
| Ready | Snapshot is immutable and eligible for analysis. | Superseded, DeletedByPolicy |
| Failed | Snapshot capture failed. | DeletedByPolicy |
| Superseded | Newer snapshot exists. | DeletedByPolicy |
| DeletedByPolicy | Snapshot removed or tombstoned under retention policy. | None |

### 13.3 Evaluation and SkillMatrix Lifecycle

| Entity | Lifecycle |
|---|---|
| Evaluation | Requested → Running → Completed or Failed → Superseded when a newer evaluation is selected as current |
| EvaluationResult | Created only on completed evaluation → Immutable → Archived by policy |
| SkillMatrix | Generated → Published → Superseded by newer matrix → Archived |

### 13.4 Career and Company Assessment Lifecycle

| Entity | Lifecycle |
|---|---|
| CareerReadiness | Requested → Completed or Failed → Superseded |
| CompanyReadiness | Requested → Completed or Failed → Superseded |
| SkillGap | Identified → AddressedByRoadmap or SupersededByNewAssessment |

### 13.5 Recommendation Lifecycle

| State | Meaning | Valid Next States |
|---|---|---|
| Proposed | Recommendation was generated but user has not acted. | Accepted, Dismissed, Superseded |
| Accepted | User chose to follow the recommendation. | Completed, Superseded |
| Dismissed | User dismissed the recommendation. | Superseded |
| Completed | User completed the recommendation. | Superseded |
| Superseded | New assessment or recommendation set replaced it. | None |

### 13.6 Learning Roadmap Lifecycle

| State | Meaning | Valid Next States |
|---|---|---|
| Created | Roadmap exists but progress has not started. | InProgress, Archived |
| InProgress | At least one roadmap step is active or completed. | Completed, Archived |
| Completed | All required roadmap steps are complete. | Archived |
| Archived | Roadmap is retained for history but no longer active. | None |

### 13.7 Knowledge Lifecycle

| Entity | Lifecycle |
|---|---|
| KnowledgeDocument | Discovered → Ingested → Indexed → Stale or Deleted |
| KnowledgeDocumentVersion | Created → Indexed → Superseded or Deleted |
| KnowledgeChunk | Created → Embedded → Indexed → Stale or Deleted |

### 13.8 Prompt and AI Lifecycle

| Entity | Lifecycle |
|---|---|
| PromptTemplate | Draft → Active → Superseded or Deprecated |
| PromptContext | Created → Validated → Locked or Rejected |
| AITask | Requested → Running → Completed, Failed, or Canceled |
| AIResponse | Received → Validated or Rejected |
| GeneratedArtifact | Draft → Validated → Reviewed → Approved or Rejected → Published or Archived |

### 13.9 Portfolio and Resume Lifecycle

| Entity | Lifecycle |
|---|---|
| Portfolio | Draft → Generated → Reviewed → Published → Archived |
| Resume | Draft → Generated → Reviewed → Published → Archived |
| InterviewQuestion | Generated → Reviewed → Practiced → Archived |

## 14. Requirement Traceability

### 14.1 SRS Traceability

| Requirement Range | Domain Concepts | Owning Contexts |
|---|---|---|
| FR-001~FR-020 User Management | User, GitHubAccount, ExternalAccount, ConsentRecord, UserSetting, Career selection, Company selection | Identity, Career, Company |
| FR-021~FR-050 GitHub Integration | GitHubAccount, Repository, RepositorySnapshot, Commit, Branch, PullRequest, Issue, README, Dependency | Identity, Repository, Knowledge |
| FR-051~FR-070 Notion Integration | NotionWorkspace, KnowledgeDocument, LearningNote, Retrospective, ProjectDocument | Identity, Knowledge, Portfolio |
| FR-071~FR-100 Data Collection | RepositorySnapshot, KnowledgeDocument, Synchronization state, SourceReference | Repository, Knowledge |
| FR-101~FR-180 Rule Engine | Rule, RuleSet, Evaluation, Evidence, Score, SkillMatrix | Rule |
| FR-181~FR-220 Career Path Engine | Career, CareerProfile, Company, CompanyProfile, SkillGap, Recommendation, LearningRoadmap | Career, Company, Recommendation, Learning |
| FR-221~FR-280 AI Engine | PromptTemplate, PromptContext, AITask, AIResponse, GeneratedArtifact, Portfolio, Resume, InterviewQuestion | Prompt, AI, Portfolio |
| FR-281~FR-320 Dashboard | SkillMatrix view, growth graph, activity graph, recommendation cards, career progress, company readiness | Rule, Career, Company, Recommendation, Learning |
| FR-321~FR-340 Search | KnowledgeDocument, KnowledgeChunk, RetrievalResult | Knowledge |
| FR-341~FR-360 Administration | RuleSetVersion, CareerProfileVersion, CompanyProfileVersion, PromptTemplateVersion, ConfigurationChange | Administration, Rule, Career, Company, Prompt |

### 14.2 Rule Engine Traceability

| Requirement | Domain Concepts |
|---|---|
| RR-001 Language Analysis | Technology, Skill, Evidence, Rule, Evaluation |
| RR-002 Framework Analysis | Framework, Technology, Dependency, Evidence |
| RR-003 Database Analysis | Technology, Dependency, Rule, Score |
| RR-004 Architecture Analysis | Architecture evidence, DirectoryEntry, Rule, SkillMatrix |
| RR-005 Testing Analysis | RepositorySnapshot, DirectoryEntry, Evidence, Score |
| RR-006 DevOps Analysis | Dependency, configuration evidence, Rule, Score |
| RR-007 Documentation Analysis | README, RepositoryDocument, KnowledgeDocument, Evidence |
| RR-008 Collaboration Analysis | PullRequest, Review, Issue, Contributor, Evidence |
| RR-009 Skill Matrix Generation | SkillMatrix, SkillAssessment, SkillEvidence |
| RR-010 Overall Score Calculation | Score, Weight, RuleSetVersion, EvaluationResult |

### 14.3 Career Engine Traceability

| Requirement | Domain Concepts |
|---|---|
| CR-001 Career Selection | Career, CareerProfile, User target career |
| CR-002 Career-specific Rules | CareerProfileVersion, Rule override reference |
| CR-003 Company Selection | Company, CompanyProfile, User target company |
| CR-004 Company-specific Rules | CompanyProfileVersion, company-specific weights |
| CR-005 Skill Gap Analysis | SkillGap, SkillMatrix, CompetencyExpectation |
| CR-006 Learning Roadmap | LearningRoadmap, RoadmapStep, Milestone |
| CR-007~CR-020 Career/Company readiness and recommendations | CareerReadiness, CompanyReadiness, Recommendation, Priority |

### 14.4 AI and Prompt Traceability

| Requirement | Domain Concepts |
|---|---|
| AI-001 Prompt Builder | PromptTemplate, PromptContext, PromptExecution |
| AI-002 Repository Summary | RepositorySnapshot, PromptContext, AIResponse |
| AI-003 Skill Analysis | SkillMatrix, AIResponse explanation |
| AI-004 Repository Review | RepositorySnapshot, AIResponse, GeneratedArtifact |
| AI-005 Portfolio Generation | GeneratedArtifact, Portfolio |
| AI-006 Resume Generation | GeneratedArtifact, Resume |
| AI-007 Interview Generation | InterviewQuestion, GeneratedArtifact |
| AI-008 Learning Planner | LearningRoadmap, AIResponse |
| PR-001~PR-015 Prompt Requirements | PromptTemplate, PromptTemplateVersion, PromptVariable, PromptContext |

### 14.5 Knowledge Traceability

| Requirement | Domain Concepts |
|---|---|
| KR-001~KR-003 Knowledge Collection | KnowledgeDocument, SourceReference |
| KR-004~KR-006 Metadata and Normalization | KnowledgeDocumentVersion, metadata, ContentHash |
| KR-007~KR-009 Chunking | KnowledgeChunk, chunk position, document version |
| KR-010~KR-012 Embedding | EmbeddingRecord, embedding model version |
| KR-013~KR-015 Retrieval | RetrievalRequest, RetrievalResult, Similarity |
| KR-016~KR-020 Freshness, Security, Privacy, Monitoring | Knowledge freshness, user isolation, deletion propagation |

## 15. Open Issues

| Issue ID | Open Issue | Impact | Recommended Owner |
|---|---|---|---|
| DM-OPEN-001 | Whether Project should become a standalone aggregate in the first implementation. | Affects portfolio, resume, and project curation workflows. | Future System Data Model |
| DM-OPEN-002 | Exact retention periods for repository snapshots, prompt contexts, AI responses, and generated artifacts. | Affects privacy, storage, and compliance. | Database Design and Security Review |
| DM-OPEN-003 | Whether knowledge indexes are physically per user, logically isolated, or hybrid. | Affects privacy and retrieval architecture. | Knowledge and Database Design |
| DM-OPEN-004 | How user-edited generated artifacts should be versioned relative to AI-generated versions. | Affects Portfolio and Resume lifecycle. | Portfolio and Frontend Architecture |
| DM-OPEN-005 | Whether mentoring, recruiter sharing, and organization accounts become separate domains. | Affects future bounded context expansion. | Product and Architecture Governance |
| DM-OPEN-006 | Whether coding-practice platforms such as Baekjoon, Programmers, and LeetCode can produce official evidence. | Affects Rule Context and Evidence acceptance policy. | Rule Engine Design |
| DM-OPEN-007 | Whether generated README improvements are managed only as artifacts or linked to repository change workflows. | Affects Repository and Portfolio contexts. | API and Backend Architecture |
| DM-OPEN-008 | How company profiles are governed to avoid unsupported speculation. | Affects Company Context and Administration Context. | Administration and Product Governance |

## 16. Future Extension

### 16.1 External Source Extensions

| Future Source | Integration Approach | Core Domain Impact |
|---|---|---|
| GitLab | Add provider-specific anti-corruption mapping into Repository Context. | No change to Repository or RepositorySnapshot concepts. |
| Bitbucket | Add provider identity and repository synchronization mapping. | No change to Rule Context. |
| Jira | Add project/work item documents as KnowledgeDocument and possible future collaboration evidence. | Rule evidence policy must approve before scoring. |
| Slack | Add communication records as private KnowledgeDocuments only if user or workspace permission allows. | No score impact unless future rules define collaboration evidence. |
| Figma | Add design documents or portfolio assets as KnowledgeDocuments and Project references. | Portfolio Context may reference design artifacts. |
| Blog platforms | Add public writing as KnowledgeDocument and portfolio evidence candidate. | Documentation evidence policy may be extended. |

### 16.2 Coding Platform Extensions

| Platform | Integration Approach | Constraint |
|---|---|---|
| Baekjoon | Model as future external evidence source. | Must not affect official scores until Rule Context defines deterministic rules. |
| Programmers | Model as future coding-practice source. | Must include identity matching and evidence acceptance policy. |
| LeetCode | Model as future coding-practice source. | Must distinguish problem-solving activity from repository engineering quality. |

### 16.3 Human and Enterprise Extensions

| Extension | Integration Approach | Constraint |
|---|---|---|
| Mentoring | Add Mentor Review as future evidence or recommendation feedback domain. | Mentor feedback must not overwrite Rule scores. |
| Recruiter Sharing | Extend Portfolio publication permission and visibility rules. | Private repository data must remain protected. |
| Enterprise Organization | Add Organization ownership and team-level authorization context. | User isolation rules must evolve into tenant isolation rules. |
| Team Analytics | Add aggregate views over multiple users only with explicit organization consent. | Individual private data must not leak through aggregate analytics. |

### 16.4 AI and Knowledge Extensions

| Extension | Integration Approach | Constraint |
|---|---|---|
| New LLM providers | Add ProviderIdentifier and model selection policy configuration. | AI boundary remains unchanged. |
| New embedding providers | Add embedding model/version references. | Knowledge deletion and user isolation remain mandatory. |
| New generated artifact types | Add ArtifactType and PromptTemplate category. | Generated artifacts remain distinct from evidence. |
| Advanced RAG | Extend Knowledge retrieval strategy. | Knowledge Context retrieves; Prompt Context assembles; AI Context generates. |

### 16.5 Extension Principle

Future functionality must extend DevPath through provider mappings, versioned configuration, new artifact types, or additional bounded contexts. Future functionality must not weaken the core principles:

- Rule Engine calculates.
- AI explains.
- Career and company readiness are deterministic.
- Recommendations are measurable and evidence-based.
- Private user knowledge is isolated.
- Historical results are reproducible.

