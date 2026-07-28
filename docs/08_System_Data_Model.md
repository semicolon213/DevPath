# DevPath System Data Model

## 1. Purpose

### 1.1 Purpose

This document defines the canonical system-wide data model of DevPath. It explains how DevPath business data exists, evolves, flows, is versioned, is synchronized, and is classified across the platform.

This document is a conceptual data architecture specification. It is not a database design, ERD, SQL specification, API specification, backend implementation guide, ORM model, DTO model, or infrastructure design.

### 1.2 Scope

This document covers:

- Canonical data objects used across DevPath.
- Ownership of data objects by bounded context.
- Data lifecycle and state evolution.
- Snapshot strategy for reproducible analysis and generated artifacts.
- Versioning strategy for rules, profiles, prompts, knowledge, and artifacts.
- System-wide data flow from GitHub and Notion to Rule Engine, Career Engine, Prompt, AI, and generated artifacts.
- Conceptual storage responsibility by storage type without defining tables or schemas.
- Synchronization and consistency expectations.
- Data classification and traceability.

This document excludes:

- Physical tables, columns, indexes, constraints, and database migrations.
- ERD, UML, implementation classes, DTOs, repositories, and API endpoints.
- Infrastructure provisioning, deployment topology, and vendor-specific physical tuning.

### 1.3 Audience

| Audience | Purpose |
|---|---|
| Data Architect | Use as the source for logical and physical data design |
| Backend Architect | Derive persistence boundaries, transaction boundaries, cache usage, and event-driven flows |
| API Architect | Derive resource identity, version references, command inputs, and data contract boundaries |
| AI Engineer | Understand which data may enter prompts and which data remains deterministic |
| Security Engineer | Validate ownership, isolation, privacy, retention, and credential boundaries |
| QA Engineer | Derive data lifecycle, consistency, traceability, and reproducibility tests |
| Product Owner | Validate that measurable career intelligence data is preserved and explainable |

### 1.4 References

| Reference | Usage |
|---|---|
| `00_Project_Context.md` | Product vision, philosophy, modules, supported careers, supported companies, and constraints |
| `01_SRS.md` | Functional and non-functional requirement source |
| `02_Rule_Engine.md` | Rule data, score data, evidence, Skill Matrix, and deterministic evaluation |
| `03_Career_Path_Engine.md` | Career profiles, company profiles, readiness, skill gaps, recommendations, and roadmaps |
| `04_AI_Architecture.md` | AI task inputs, outputs, model execution, validation, and generated artifacts |
| `05_Prompt_Engineering.md` | Prompt templates, prompt contexts, variables, versions, and prompt validation |
| `06_Knowledge_Architecture.md` | Knowledge documents, chunks, embeddings, retrieval, synchronization, freshness, and privacy |
| `07_Domain_Model.md` | Bounded contexts, entities, aggregates, invariants, lifecycles, and domain ownership |

### 1.5 Data Architecture Assumptions

The following accepted implementation facts refine this conceptual model without converting it into an ORM design:

- ADR-024 maps framework-independent domain objects to separate JPA/Hibernate persistence models inside owning outbound adapters.
- ADR-025 makes Flyway versioned SQL the owner of PostgreSQL schema evolution.
- ADR-026 separates internal `User`, external OAuth identity, encrypted provider credentials, and the opaque application session.

| Assumption ID | Assumption |
|---|---|
| SDM-ASM-001 | Canonical object identity is internal to DevPath; external provider identifiers are stored as references, not as primary conceptual identity. |
| SDM-ASM-002 | Historical reproducibility is more important than overwriting current state for analysis, evaluation, readiness, prompts, and artifacts. |
| SDM-ASM-003 | Storage technologies may vary by implementation, but the conceptual ownership and lifecycle defined here must remain stable. |
| SDM-ASM-004 | LLM outputs are generated data and must not become deterministic score data. |
| SDM-ASM-005 | Embedding vectors are derived retrieval data and must be traceable to source document version and embedding model version. |

## 2. Data Architecture Principles

### 2.1 Single Source of Truth

Each canonical data object has exactly one owning context. Other contexts may consume copies, projections, references, or events, but they must not become competing sources of truth.

| Data Area | Source of Truth |
|---|---|
| User identity and external account ownership | Identity Context |
| Repository current state and snapshots | Repository Context |
| Official scores and evaluation results | Rule Context |
| Skill Matrix | Rule Context |
| Career readiness and skill gaps | Career Context |
| Company readiness | Company Context |
| Recommendation priority | Recommendation Context |
| Learning roadmap progress | Learning Context |
| Knowledge documents, chunks, and embedding metadata | Knowledge Context |
| Prompt templates and prompt contexts | Prompt Context |
| AI responses and generated artifact envelopes | AI Context |
| Portfolio and resume publication state | Portfolio Context |
| Configuration activation and deprecation | Administration Context |

### 2.2 Canonical Data

Canonical data is normalized DevPath data with stable meaning independent from external provider structures. GitHub and Notion data must pass through anti-corruption translation before becoming canonical repository or knowledge data.

Canonical data must:

- Use DevPath-owned identity.
- Preserve source references.
- Preserve owner scope.
- Preserve relevant timestamps.
- Preserve version or snapshot references where reproducibility is required.
- Avoid provider-specific structures leaking into the core model.

### 2.3 Immutable Snapshot

Snapshots preserve exact analysis context at a point in time. Official evaluation, career readiness, company readiness, prompt execution, and generated artifact records must reference immutable snapshots or versioned data.

### 2.4 Append Only History

DevPath preserves history for:

- Repository snapshots.
- Evaluation results.
- Skill matrices.
- Career and company readiness assessments.
- Recommendation sets.
- Learning roadmap progress.
- Knowledge document versions.
- Prompt executions.
- AI responses.
- Generated artifact versions.

Append-only history prevents silent changes to previously generated results.

### 2.5 Versioning

Versioned data is used where definitions or generated artifacts evolve over time:

- RuleSetVersion.
- CareerProfileVersion.
- CompanyProfileVersion.
- KnowledgeDocumentVersion.
- PromptTemplateVersion.
- PortfolioVersion.
- ResumeVersion.
- EmbeddingModelVersion.

Completed results must reference the exact version used at the time of creation.

### 2.6 Data Ownership

Ownership controls mutation rights, lifecycle decisions, retention policy, and traceability. Read access may be broader than write ownership, but write ownership must remain unambiguous.

### 2.7 Eventual Consistency

Eventual consistency is acceptable for projections, search results, notifications, knowledge indexes, dashboard cards, and AI artifact history. It is not acceptable for ownership checks, official score persistence, or immutable snapshot creation.

### 2.8 Synchronization

External source synchronization must be idempotent, resumable, permission-aware, and checkpointed. Synchronization produces canonical data changes only after provider data has been translated and validated.

### 2.9 Data Isolation

User-private data, private repository data, Notion content, knowledge chunks, embeddings, prompt contexts, and generated artifacts must be isolated by user ownership scope. Shared indexes or caches must not weaken user isolation.

## 3. Canonical Data Model

### 3.1 Canonical Object Catalog

| Canonical Object | Purpose | Owner | Lifetime | Mutable Fields | Immutable Fields | Relationships |
|---|---|---|---|---|---|---|
| User | Represents a DevPath account owner. | Identity Context | Registration until deletion or anonymized retention. | Account status, role assignment, consent state. | Internal user identity, registration timestamp. | Owns external accounts, repositories, knowledge, artifacts, preferences. |
| ExternalIdentity | Provider-namespaced identity used to authenticate or link a User. | Identity Context | Link creation until unlinking or account deletion, subject to audit retention. | Link status and last verification metadata. | Provider, provider subject/account reference, owning User. | Many supported external identities may link to one internal User; one provider identity cannot silently link to multiple Users. |
| ApplicationSession | Opaque server-managed authenticated session state. | Identity Context | Login until idle/absolute expiry, logout, revocation, suspension, or deletion. | Last activity, expiry, revocation state, security metadata. | Session identifier relationship to User after creation. | Establishes authentication context but does not replace User identity or authorization policy. |
| ProviderCredential | Encrypted server-side authorization material for a connected provider. | Integration Context | Connection until rotation, revocation, disconnection, or deletion. | Encrypted token material, expiry, scopes, rotation metadata. | Owner User, provider, external account/workspace relationship. | Used only by owning provider adapters; never exposed to frontend or ordinary domain models. |
| GitHubAccount | Represents connected GitHub identity and authorization. | Identity Context | Connection until disconnection, revocation, or deletion. | Connection status, permission scope summary. | Provider identity reference, connection owner. | Authorizes repository synchronization. |
| NotionWorkspace | Represents connected Notion workspace authorization. | Identity Context | Connection until disconnection, revocation, or deletion. | Connection status, permission scope summary. | Workspace source reference, owner. | Supplies knowledge documents and project documentation. |
| Repository | Canonical representation of a source repository. | Repository Context | Discovery until archive, deletion, or retention expiry. | Display metadata, default branch, archived state, sync summary. | Internal repository identity, owner scope, provider reference. | Has RepositorySnapshots; may be linked to Projects. |
| RepositorySnapshot | Immutable captured repository state. | Repository Context | Created for analysis and retained per policy. | Retention status only. | Snapshot identity, source revision, captured facts, captured timestamp. | Referenced by Evaluation, Evidence, KnowledgeDocument, GeneratedArtifact. |
| Project | Career-relevant body of work. | Portfolio Context | Created by user or inferred candidate until archived/deleted. | Title, description, role, linked references, review state. | Internal project identity, owner. | References repositories, documents, technologies, artifacts. |
| ProjectSnapshot | Immutable project fact snapshot. | Portfolio Context | Created for artifact generation or analysis support. | Retention status only. | Snapshot identity, project facts, source references, creation timestamp. | Referenced by PortfolioVersion, ResumeVersion, PromptContext. |
| Evaluation | Deterministic analysis execution and result. | Rule Context | Created per analysis request and retained historically. | Execution status before completion. | Input snapshot references, RuleSetVersion, completed scores, evidence links. | Produces SkillMatrix and dashboard projections. |
| SkillMatrix | Evidence-backed skill assessment. | Rule Context | Generated per completed evaluation and retained historically. | Publication state before finalization. | Skill assessments, evidence references, evaluation reference. | Consumed by Career, Company, Recommendation, Prompt, Dashboard. |
| CareerProfile | Versioned target-career expectation definition. | Career Context | Managed as configuration history. | Draft metadata before activation. | Activated profile version content and effective time. | Used by CareerReadiness. |
| CompanyProfile | Versioned target-company expectation definition. | Company Context | Managed as configuration history. | Draft metadata before activation. | Activated profile version content and effective time. | Used by CompanyReadiness and interview generation. |
| CareerReadiness | Deterministic career readiness assessment. | Career Context | Created per SkillMatrix and CareerProfileVersion. | Status before completion. | Readiness level, skill gaps, input references. | Consumed by Recommendation and Prompt. |
| CompanyReadiness | Deterministic company readiness assessment. | Company Context | Created per SkillMatrix and CompanyProfileVersion. | Status before completion. | Readiness level, input references, company profile version. | Consumed by Recommendation, Prompt, Interview generation. |
| Recommendation | Evidence-based suggested action. | Recommendation Context | Generated with recommendation set and retained historically. | User decision state such as accepted, dismissed, completed. | Original reason, priority, source gap/evidence references. | May produce roadmap steps and AI explanations. |
| LearningRoadmap | Ordered learning plan. | Learning Context | Created from recommendation set and retained until deletion/archive. | Progress state, step completion state. | Original recommendation basis, generated ordering. | Consumed by dashboard and AI learning planner. |
| KnowledgeDocument | Versioned long-term knowledge source. | Knowledge Context | Discovery until deletion, archive, or retention expiry. | Freshness state, metadata, indexing state. | Document identity, owner scope, source reference. | Has document versions and chunks. |
| KnowledgeChunk | Retrievable segment of document version. | Knowledge Context | Created by chunking and retained while source version is retained. | Index state and freshness state. | Chunk identity, document version reference, content hash, position. | Has embedding metadata; appears in retrieval results. |
| EmbeddingMetadata | Metadata for derived vector representation. | Knowledge Context | Exists while chunk and embedding model version remain valid. | Staleness and rebuild status. | Chunk reference, provider, model, model version, vector reference. | Used by retrieval. |
| RetrievalResult | Ranked knowledge retrieval output. | Knowledge Context | Execution/audit retention window. | None after completion. | Query intent, selected chunks, source references, ranking metadata. | Consumed by PromptContext. |
| PromptTemplate | Versioned reusable prompt structure. | Prompt Context | Managed as configuration history. | Draft content before activation. | Active template version content, variables, constraints. | Used by PromptContext and PromptExecution. |
| PromptContext | Immutable context package for one AI task. | Prompt Context | Retained with prompt execution history. | None after lock. | Template version, variable bindings, source references, token budget. | Consumed by AI task. |
| PromptExecution | Record of prompt execution request. | Prompt Context | Retained for audit and reproducibility. | Execution status. | PromptContext reference, template version, task type. | Produces AI task execution. |
| AITask | AI work request. | AI Context | Request until completion/failure and retention. | Task status and retry state. | Task type, owner, source PromptExecution. | Produces AIResponse. |
| AIResponse | LLM-generated response record. | AI Context | Retained according to AI logging and privacy policy. | Validation status until final. | Model execution reference, prompt context reference, response content reference. | Validated into GeneratedArtifact. |
| GeneratedArtifact | AI-assisted output envelope. | AI Context | Draft through archive/delete. | Review state, approval state. | Source PromptContext, AIResponse reference, artifact type, provenance. | May become Portfolio, Resume, READMEImprovement, InterviewQuestionSet. |
| Portfolio | User-owned career presentation artifact. | Portfolio Context | Draft until archive/delete; published versions retained. | Draft state, active version, publication state. | Published version content and source references. | References Projects, SkillMatrix, GeneratedArtifact. |
| Resume | User-owned resume artifact. | Portfolio Context | Draft until archive/delete; published versions retained. | Draft state, active version, publication state. | Published version content and source references. | References Projects, SkillMatrix, GeneratedArtifact. |
| InterviewQuestionSet | Generated interview preparation artifact. | Portfolio Context | Generated until archive/delete. | Review/practice state. | Original career/company context and source references. | Contains InterviewQuestions. |
| ConfigurationChange | Administrative configuration event record. | Administration Context | Permanent or compliance retention. | None after recording. | Actor reference, affected configuration, effective time. | Affects future rule/career/company/prompt versions. |

### 3.2 Canonical Identity Rules

| Identity Type | Rule |
|---|---|
| Internal ID | DevPath-generated and stable within the system. |
| External ID | Provider-specific and always namespaced by provider. |
| Authentication Link | A unique provider plus provider-subject relationship resolves to one internal User; email alone is not an identity key. |
| Session ID | Opaque, revocable, expirable, and independent of provider access tokens. |
| Snapshot ID | Identifies immutable captured source state. |
| Version ID | Identifies immutable configuration or artifact version. |
| Source Reference | Identifies original provider source, location, timestamp, and owner scope. |
| Content Hash | Identifies content equality and supports deduplication and version detection. |
| Correlation ID | Connects related records across long-running workflows. |
| Idempotency Key | Prevents duplicate command effects during retries. |

## 4. Data Ownership

### 4.1 Ownership Matrix

| Data Object | Owning Context | Mutable By | Readable By | Must Be Versioned | Must Be Snapshotted | Contains Private Data |
|---|---|---|---|---|---|---|
| User | Identity | User/Admin policy | Authorized contexts | No | No | Yes |
| ExternalIdentity | Identity | Identity login/linking policy | Identity, Audit | Link history where required | No | Yes |
| ApplicationSession | Identity | Authentication/session service | Identity/Security only | No; audit events are separate | No | Yes |
| ProviderCredential | Integration | Owning provider adapter and credential service | Owning adapter only | Rotation metadata only | No | Restricted |
| GitHubAccount | Identity | User/provider sync | Repository, Audit | No | No | Yes |
| NotionWorkspace | Identity | User/provider sync | Knowledge, Audit | No | No | Yes |
| Repository | Repository | Repository synchronization | Rule, Knowledge, Dashboard | Current state version optional | Yes for analysis | Possible |
| RepositorySnapshot | Repository | Snapshot service before Ready | Rule, Knowledge, AI | Snapshot identity | Yes | Possible |
| Evaluation | Rule | Rule Engine only | Career, Company, Dashboard, AI | Yes by RuleSetVersion | References snapshots | Indirect |
| SkillMatrix | Rule | Rule Engine only | Career, Company, Recommendation, AI | Yes | References evaluation snapshot | Yes |
| CareerProfile | Career | Admin through governance | Career, Recommendation, Prompt | Yes | No | No |
| CompanyProfile | Company | Admin through governance | Company, Recommendation, Prompt | Yes | No | No |
| CareerReadiness | Career | Career Engine only | Recommendation, Learning, Prompt, AI | Yes by source versions | References SkillMatrix | Yes |
| CompanyReadiness | Company | Company Engine only | Recommendation, Prompt, AI | Yes by source versions | References SkillMatrix | Yes |
| Recommendation | Recommendation | Recommendation service; user status actions | Learning, Prompt, AI, Dashboard | Set version | References assessments | Yes |
| LearningRoadmap | Learning | Roadmap service; user progress actions | Dashboard, Prompt, AI | Generated basis version | References recommendations | Yes |
| KnowledgeDocument | Knowledge | Knowledge ingestion/sync | Prompt, Search | Yes | Versioned source | Yes |
| KnowledgeChunk | Knowledge | Chunking/indexing pipeline | Retrieval, Prompt | By document version | No separate snapshot | Yes |
| EmbeddingMetadata | Knowledge | Embedding pipeline | Retrieval | By model version | Derived from chunk | Yes if source private |
| PromptTemplate | Prompt | Admin through governance | Prompt service | Yes | No | No |
| PromptContext | Prompt | Prompt composition service before lock | AI, Audit | Execution version | Yes conceptually | Yes |
| AIResponse | AI | AI execution/validation | Artifact contexts, Audit | Execution record | References prompt snapshot | Yes |
| GeneratedArtifact | AI | AI validation; user review state | Portfolio, Dashboard | Artifact version | References prompt/context | Yes |
| Portfolio | Portfolio | User/artifact workflow | User, sharing/export contexts | Yes | Published versions | Yes or public if published |
| Resume | Portfolio | User/artifact workflow | User, export contexts | Yes | Published versions | Yes |
| ConfigurationChange | Administration | Admin action only | Admin/Audit | Yes | No | No |

### 4.2 Ownership Rules

| Rule ID | Ownership Rule |
|---|---|
| SDM-OWN-001 | No object may have more than one owning context. |
| SDM-OWN-002 | Read models and projections must not become sources of truth. |
| SDM-OWN-003 | User-owned private data must carry owner scope through every derived object. |
| SDM-OWN-004 | Rule Engine owns official score data; AI Context cannot create or mutate it. |
| SDM-OWN-005 | Recommendation Context owns official recommendation priority; AI Context cannot assign it. |
| SDM-OWN-006 | Knowledge Context owns long-term memory; LLM providers must not be treated as memory stores. |
| SDM-OWN-007 | Prompt Context owns PromptContext; AI Context owns model execution and response records. |
| SDM-OWN-008 | Identity Context owns internal User and application-session state; provider identities remain subordinate links, not User identifiers. |
| SDM-OWN-009 | Integration Context owns encrypted provider API credentials; authentication sessions never contain or expose provider access tokens. |
| SDM-OWN-010 | Persistence adapters own domain-to-persistence mapping; persistence records are not canonical domain concepts or API contracts. |

## 5. Data Lifecycle

### 5.1 Repository Data Lifecycle

| Step | State | Description | Owner |
|---|---|---|---|
| 1 | Collect | GitHub source data is collected under user permission. | Repository Context |
| 2 | Normalize | Provider data is translated into canonical repository data. | Repository Context |
| 3 | Snapshot | Immutable RepositorySnapshot is created. | Repository Context |
| 4 | Analyze | Snapshot is prepared for deterministic Rule Engine evaluation. | Rule Context |
| 5 | Archive | Repository or snapshot is archived or retained historically. | Repository Context |

### 5.2 Evaluation Data Lifecycle

| Step | State | Description | Owner |
|---|---|---|---|
| 1 | Requested | Evaluation request references immutable input and RuleSetVersion. | Rule Context |
| 2 | Running | Rule Engine executes deterministic rules. | Rule Context |
| 3 | Completed | Scores, evidence links, and category outputs become immutable. | Rule Context |
| 4 | Skill Matrix Generated | SkillMatrix is produced from Evaluation. | Rule Context |
| 5 | Superseded | New evaluation may become current, but old result remains historical. | Rule Context |

### 5.3 Recommendation Data Lifecycle

| Step | State | Description | Owner |
|---|---|---|---|
| 1 | Created | Recommendation is generated from readiness, gaps, and evidence. | Recommendation Context |
| 2 | Active | Recommendation is visible and actionable. | Recommendation Context |
| 3 | Accepted or Dismissed | User records decision. | Recommendation Context |
| 4 | Completed | User completes accepted recommendation. | Recommendation Context |
| 5 | Archived | Recommendation remains historically traceable. | Recommendation Context |

### 5.4 Knowledge Data Lifecycle

| Step | State | Description | Owner |
|---|---|---|---|
| 1 | Imported | GitHub, Notion, or artifact source becomes KnowledgeDocument candidate. | Knowledge Context |
| 2 | Versioned | Content hash and source reference create KnowledgeDocumentVersion. | Knowledge Context |
| 3 | Chunked | Document version is split into KnowledgeChunks. | Knowledge Context |
| 4 | Embedded | Embedding metadata is created for chunks. | Knowledge Context |
| 5 | Indexed | Chunks become eligible for retrieval. | Knowledge Context |
| 6 | Retrieved | RetrievalResult selects chunks for search or prompt grounding. | Knowledge Context |
| 7 | Archived or Deleted | Retention or permission changes remove eligibility and derived indexes. | Knowledge Context |

### 5.5 Prompt and AI Data Lifecycle

| Step | State | Description | Owner |
|---|---|---|---|
| 1 | Template Selected | Active PromptTemplateVersion is selected for task. | Prompt Context |
| 2 | Context Assembled | Structured outputs and retrieval results become PromptContext. | Prompt Context |
| 3 | Context Locked | PromptContext becomes immutable. | Prompt Context |
| 4 | AI Executed | Model execution produces AIResponse. | AI Context |
| 5 | Response Validated | AIResponse is accepted or rejected. | AI Context |
| 6 | Artifact Created | Validated response becomes GeneratedArtifact where applicable. | AI Context |

### 5.6 Portfolio and Resume Data Lifecycle

| Step | State | Description | Owner |
|---|---|---|---|
| 1 | Draft | User or AI creates draft artifact content. | Portfolio Context |
| 2 | Generated | AI-assisted artifact is created with provenance. | AI/Portfolio Context |
| 3 | Reviewed | User reviews and may edit content. | Portfolio Context |
| 4 | Published | Artifact version becomes immutable. | Portfolio Context |
| 5 | Archived | Artifact is retained historically or removed by policy. | Portfolio Context |

## 6. Snapshot Strategy

### 6.1 Why Snapshots Exist

Snapshots exist to guarantee that DevPath analysis is reproducible, explainable, and historically stable. Without snapshots, repository updates, document edits, profile changes, and prompt updates could silently change prior scores, recommendations, and generated artifacts.

### 6.2 Snapshot Types

| Snapshot Type | Purpose | Immutable Content | Reference Rules | Historical Usage |
|---|---|---|---|---|
| Repository Snapshot | Preserve source repository state for analysis. | Repository metadata, branch/commit/PR/issue/dependency/document facts selected for analysis. | Evaluation must reference Snapshot ID. | Enables historical comparison and reproducible score generation. |
| Project Snapshot | Preserve career-relevant project facts for artifacts. | Project description, linked repository snapshots, documents, role, technologies, evidence references. | Portfolio and resume versions reference ProjectSnapshot where used. | Prevents portfolio claims from changing silently. |
| Analysis Snapshot | Preserve normalized input to Rule Engine. | Normalized facts, source references, input quality metadata. | Evaluation references analysis input identity and RuleSetVersion. | Supports deterministic regression and audit. |
| Prompt Snapshot | Preserve exact prompt context for one AI task. | PromptTemplateVersion, variables, source references, retrieval results, token budget. | AIResponse must reference PromptContext. | Enables AI output reproducibility and validation. |
| Portfolio Snapshot | Preserve published portfolio version. | Sections, project references, evidence references, generated/user-edited provenance. | Published portfolio identity references version. | Enables rollback and publication history. |
| Resume Snapshot | Preserve published resume version. | Sections, skill/project references, generated/user-edited provenance. | Published resume identity references version. | Enables export history and audit. |

### 6.3 Snapshot Immutability Rules

| Rule ID | Rule |
|---|---|
| SDM-SNP-001 | Snapshot content must not change after it reaches ready or published state. |
| SDM-SNP-002 | Corrections require a new snapshot or version, not mutation of prior snapshot content. |
| SDM-SNP-003 | Snapshot references must include owner scope and source reference. |
| SDM-SNP-004 | Derived results must reference the exact snapshot or version used. |
| SDM-SNP-005 | Retention deletion may remove or redact content, but historical records must preserve allowed audit references where permitted. |

## 7. Version Strategy

### 7.1 Versioned Data Types

| Versioned Data | Current Version | Historical Version | Migration | Compatibility |
|---|---|---|---|---|
| Rule Version | Active RuleSetVersion used for new evaluations. | Completed evaluations retain original RuleSetVersion. | Migration may map old rule categories to new categories for reporting only. | Evaluation requires compatible input model. |
| Career Profile Version | Active CareerProfileVersion used for new career assessments. | Historical readiness retains original version. | New profile may trigger optional reassessment. | Skill expectations must map to known skills. |
| Company Version | Active CompanyProfileVersion used for new company assessments. | Historical company readiness retains original version. | New profile may supersede future assessments. | Company-specific weights must not rewrite Rule scores. |
| Knowledge Version | Latest KnowledgeDocumentVersion eligible for retrieval. | Older versions retained or deleted by policy. | Reindexing may create new embedding records. | Retrieval must know document and embedding versions. |
| Prompt Version | Active PromptTemplateVersion used for new prompt contexts. | Prompt executions retain original template version. | Template migration may update variables for future tasks. | PromptContext must validate against selected version. |
| Portfolio Version | Active published or draft portfolio version. | Older portfolio versions retained for history. | User may create new version from prior one. | Published versions are immutable. |
| Resume Version | Active published or draft resume version. | Older resume versions retained for history. | User may create new version from prior one. | Published versions are immutable. |
| Embedding Model Version | Current embedding model used for new chunks. | Previous embedding records retained until reindex or deletion. | Reindex creates new embedding metadata. | Retrieval must not compare incompatible vectors without strategy. |

### 7.2 Version Reference Rules

| Rule ID | Rule |
|---|---|
| SDM-VER-001 | Completed results must never reference "latest" as their version basis. |
| SDM-VER-002 | Current version is a selection pointer, not a replacement for historical versions. |
| SDM-VER-003 | Version migration must not silently modify historical official results. |
| SDM-VER-004 | Compatibility must be validated before a version is activated. |
| SDM-VER-005 | Deprecated versions remain readable if referenced by historical results. |

## 8. Data Flow

### 8.1 System-Wide Flow

| Flow Step | Transition | Description | Data Control |
|---|---|---|---|
| 1 | GitHub → Collector | GitHub source metadata, repositories, commits, branches, PRs, issues, README, dependencies, and directory structure are collected. | Permission and rate-limit aware. |
| 2 | Notion → Collector | Notion workspace pages, retrospectives, learning notes, and project documents are collected. | Permission and workspace scope aware. |
| 3 | Collector → Normalizer | External provider structures are translated into canonical source facts. | Anti-corruption boundary. |
| 4 | Normalizer → Repository Snapshot | Repository facts become immutable snapshot data. | Snapshot identity and source reference required. |
| 5 | Normalizer → Knowledge | Document-like sources become versioned KnowledgeDocuments. | Content hash, metadata, owner scope, privacy class. |
| 6 | Repository Snapshot → Rule Engine | Snapshot facts become deterministic evaluation input. | RuleSetVersion required. |
| 7 | Rule Engine → Skill Matrix | Evaluation results become evidence-backed skill assessments. | AI not involved in score calculation. |
| 8 | Skill Matrix → Career Engine | SkillMatrix is compared against CareerProfileVersion. | Career readiness and gaps are deterministic. |
| 9 | Skill Matrix → Company Engine | SkillMatrix is compared against CompanyProfileVersion when target company exists. | Company readiness is deterministic and separate from Rule scores. |
| 10 | Career/Company → Recommendation | Readiness and skill gaps become recommendation candidates. | Priority calculated deterministically. |
| 11 | Recommendation → Learning Roadmap | Accepted or generated recommendation set becomes roadmap steps. | Steps are measurable. |
| 12 | Knowledge → Prompt | RetrievalResult supplies relevant chunks and evidence references. | User isolation and token budget respected. |
| 13 | Structured Outputs → Prompt | SkillMatrix, readiness, recommendations, roadmap, and project facts become prompt variables. | Prompt Builder does not execute business rules. |
| 14 | Prompt → LLM | Locked PromptContext becomes model execution input. | Provider selection and privacy controls apply. |
| 15 | LLM → AI Response | Model returns natural-language response. | Response requires validation. |
| 16 | AI Response → Generated Artifact | Valid response becomes artifact envelope. | Provenance retained. |
| 17 | Generated Artifact → Portfolio/Resume/Interview | User-reviewed artifacts become domain-specific versions. | Published versions immutable. |

### 8.2 Transition Control Rules

| Rule ID | Rule |
|---|---|
| SDM-FLW-001 | External data must be normalized before becoming canonical data. |
| SDM-FLW-002 | Snapshot data must be ready before deterministic evaluation starts. |
| SDM-FLW-003 | Rule Engine output must be completed before SkillMatrix publication. |
| SDM-FLW-004 | Career readiness must not be generated without SkillMatrix and CareerProfileVersion. |
| SDM-FLW-005 | PromptContext must not be assembled from stale or unauthorized private knowledge. |
| SDM-FLW-006 | AIResponse must not become user-facing artifact without response validation. |

## 9. Storage Responsibility

### 9.1 Conceptual Storage Classification

This chapter describes conceptual storage responsibility only. It does not define physical tables, columns, indexes, schema names, migrations, or vendor-specific deployment.

| Storage Type | Conceptual Responsibility | Data That Belongs There | Data That Does Not Belong There |
|---|---|---|---|
| PostgreSQL | Durable canonical records, version metadata, ownership, lifecycle state, audit references, configuration records, transactional history, and the accepted JDBC session store for MVP. | User, external identity links, encrypted provider-credential records, JDBC session records, Repository metadata, Snapshot metadata, Evaluation result metadata, SkillMatrix, Readiness, Recommendation, Roadmap, Prompt execution records, Artifact metadata. | Raw large files when object storage is more appropriate; ordinary temporary cache entries; raw vectors. |
| Redis | Temporary cache, short-lived synchronization state, rate-limit coordination, deduplication locks, and ephemeral workflow status. | Temporary sync status, short-lived idempotency markers, dashboard cache, provider rate-limit state. | Official scores, immutable snapshots, historical evaluations, published artifacts. |
| Vector Database | Retrieval-optimized representation of KnowledgeChunks. | Embedding vector references, chunk retrieval indexes, similarity search metadata. | Official scores, user profile truth, recommendation priority, prompt templates. |
| Object Storage | Large content blobs and generated file-like artifacts. | Repository document content, Notion document content copies if retained, generated artifact content, exports, rendered files. | Canonical ownership metadata, score values, authorization truth. |
| Temporary Cache | Non-authoritative transient computation support. | Partial collection buffers, token-budget estimation cache, recent retrieval cache, validation scratch data. | Any source of truth or compliance record. |

### 9.2 Storage Responsibility Rules

| Rule ID | Rule |
|---|---|
| SDM-STO-001 | Durable canonical metadata must be stored in a transactional source of truth. |
| SDM-STO-002 | Cache entries must be reconstructable from canonical data or external source synchronization. |
| SDM-STO-003 | Vector storage must not be the only source of knowledge truth. |
| SDM-STO-004 | Object storage content must be referenced by secure content references, not treated as ownership metadata. |
| SDM-STO-005 | Credential values must be stored only through secure secret handling and must not appear as ordinary data objects. |
| SDM-STO-006 | Application sessions are operational authenticated state: local development may use memory, while MVP uses PostgreSQL-backed JDBC sessions under ADR-026; Redis is not required initially. |
| SDM-STO-007 | Provider credentials are encrypted server-side and are never equivalent to an application session. |
| SDM-STO-008 | Flyway owns physical schema evolution; ORM runtime behavior may validate compatibility but must not mutate authoritative schema. |

## 10. Synchronization

### 10.1 GitHub Synchronization

| Aspect | Rule |
|---|---|
| Scope | Repository list, metadata, branches, commits, PRs, issues, README, dependencies, directory structure, releases where supported. |
| Permission | Sync may operate only within current GitHubAccount permission scope. |
| Incremental Behavior | Synchronization uses provider cursor, last source update time, or equivalent checkpoint. |
| Snapshot Creation | Snapshot is created only after enough canonical source facts are collected and validated. |
| Failure Handling | Partial sync may preserve successful canonical updates while marking incomplete areas. |
| Revocation | Permission revocation blocks future sync and may mark derived private data as ineligible. |

### 10.2 Notion Synchronization

| Aspect | Rule |
|---|---|
| Scope | Retrospectives, learning notes, project documents, architecture documents, and other authorized workspace content. |
| Permission | Sync may operate only within authorized workspace or page scope. |
| Version Detection | Content hash and source update time identify KnowledgeDocumentVersion changes. |
| Unsupported Content | Unsupported block types are excluded or represented as unsupported metadata. |
| Failure Handling | Failed pages do not block successfully ingested documents unless consistency policy requires all-or-nothing sync. |
| Revocation | Revocation marks affected private knowledge unavailable for new retrieval and AI prompts. |

### 10.3 Knowledge Synchronization

| Aspect | Rule |
|---|---|
| Freshness | KnowledgeDocument may be Current, Stale, Superseded, Deleted, or ReindexRequired. |
| Incremental Update | New content version creates new KnowledgeDocumentVersion and new chunks. |
| Embedding Refresh | Embedding model changes may require re-embedding affected chunks. |
| Deletion Propagation | Source deletion or permission revocation propagates to chunks, embeddings, retrieval eligibility, and caches. |
| Conflict Handling | Conflicts are resolved by source timestamp, content hash, explicit user action, or source priority policy. |

### 10.4 Snapshot Synchronization

| Aspect | Rule |
|---|---|
| Repository Snapshot | Created from synchronized repository source facts. |
| Analysis Snapshot | Created from normalized input and references RepositorySnapshot. |
| Prompt Snapshot | Created from selected structured outputs and retrieval result. |
| Artifact Snapshot | Created when portfolio or resume version is published. |
| Historical Isolation | New snapshots do not mutate prior snapshots. |

### 10.5 Version Updates

| Version Type | Synchronization Rule |
|---|---|
| RuleSetVersion | Admin activation affects only future evaluations. |
| CareerProfileVersion | Admin activation affects only future career assessments. |
| CompanyProfileVersion | Admin activation affects only future company assessments. |
| PromptTemplateVersion | Admin activation affects only future prompt contexts. |
| KnowledgeDocumentVersion | Source update creates a new version and may mark prior version superseded. |
| Portfolio/Resume Version | User publication creates a new immutable version. |

## 11. Data Consistency

### 11.1 Consistency Categories

| Category | Definition | Examples | Rules |
|---|---|---|---|
| Strong Consistency | Data must be immediately correct within command boundary. | User ownership, permission checks, active rule version validation, completed score persistence. | Required for authoritative writes. |
| Eventual Consistency | Data may update asynchronously from events. | Dashboard projections, notification feed, search index, retrieval index, artifact history. | Must expose freshness where user confusion is possible. |
| Immutable Data | Data cannot change after finalization. | RepositorySnapshot, EvaluationResult, PromptContext, published artifact version. | Corrections create new versions. |
| Derived Data | Data computed from canonical sources. | SkillMatrix, readiness, recommendations, embeddings, projections. | Must retain source references. |
| Cached Data | Temporary copy used for performance. | Dashboard cache, sync progress cache, token estimate cache. | Must be invalidatable and non-authoritative. |
| Temporary Data | Short-lived workflow data. | Collection buffers, retry state, validation scratch data. | Must not be required for historical reproducibility. |

### 11.2 Consistency Rules

| Rule ID | Rule |
|---|---|
| SDM-CON-001 | Official scores require strong consistency inside Rule Context. |
| SDM-CON-002 | Read models may be eventually consistent but must not accept authoritative writes. |
| SDM-CON-003 | Derived data must reference source data and version basis. |
| SDM-CON-004 | Cached data must be safely rebuildable. |
| SDM-CON-005 | Deleted private data must be removed from caches, retrieval indexes, and prompt eligibility. |
| SDM-CON-006 | Cross-context workflows must use IDs, versions, snapshots, source references, and events rather than direct mutation. |

## 12. Data Classification

### 12.1 Classification by Data Function

| Classification | Definition | Examples | Ownership | Retention Behavior |
|---|---|---|---|---|
| Master Data | Stable business identity data. | User, Repository, Career, Company, Technology, Skill. | Owning context. | Retained while active or referenced. |
| Reference Data | Managed definitions used by engines. | RuleSetVersion, CareerProfileVersion, CompanyProfileVersion, PromptTemplateVersion. | Administration with owning domain. | Retained historically if referenced. |
| Transactional Data | Data created by user or system operations. | SynchronizationJob, Evaluation, PromptExecution, AITask. | Owning context. | Retained according to operation/audit policy. |
| Historical Data | Append-only data preserving past state. | RepositorySnapshot, EvaluationResult, SkillMatrix, readiness assessments, artifact versions. | Owning context. | Retained for reproducibility unless privacy deletion applies. |
| Derived Data | Data computed from source or canonical data. | SkillMatrix, Recommendation, EmbeddingMetadata, RetrievalResult, DashboardProjection. | Computing context. | Rebuildable only if source retained. |
| Generated Data | AI-created or AI-assisted content. | AIResponse, GeneratedArtifact, Portfolio draft, Resume draft, README improvement. | AI or Portfolio Context. | Retained by artifact and privacy policy. |
| Temporary Data | Short-lived operational data. | Cache entries, sync buffers, rate-limit state, idempotency markers. | Operating context. | Expired automatically. |

### 12.2 Classification by Privacy

| Privacy Class | Examples | AI Context Eligibility | Logging Rule |
|---|---|---|---|
| Public | Public repository metadata, user-published portfolio. | Eligible if relevant. | May log metadata; avoid full content unless needed. |
| Internal | Rules, career profiles, company profiles, prompt templates. | Eligible as constraints. | Log version metadata. |
| User Private | User profile, SkillMatrix, recommendations, roadmaps. | Eligible with owner scope. | Log metadata only where possible. |
| Repository Private | Private repository metadata, README content, dependency facts, source structure. | Eligible only with permission and minimization. | Do not log raw content. |
| Notion Private | Retrospectives, learning notes, project documents. | Eligible only with permission and minimization. | Do not log raw content. |
| Generated Personal Content | Resume, portfolio, coaching output, interview prep. | Eligible as prior artifact context only by policy. | Log provenance and status, not unnecessary full content. |
| Sensitive Credential | OAuth tokens, refresh tokens, secret values. | Never eligible. | Never log values. |
| Audit Restricted | Audit records and compliance data. | Not eligible for LLM prompts. | Restricted access only. |

## 13. Traceability

### 13.1 Data Object to SRS Mapping

| Data Object | SRS Mapping | Related Architecture |
|---|---|---|
| User, GitHubAccount, NotionWorkspace | FR-001~FR-020, FR-021~FR-023, FR-051~FR-053 | `07_Domain_Model.md` Identity Context |
| Repository, RepositorySnapshot | FR-024~FR-050, FR-071~FR-100 | `02_Rule_Engine.md`, `07_Domain_Model.md` Repository Context |
| Evaluation, Score, Evidence | FR-101~FR-180, RR-001~RR-010 | `02_Rule_Engine.md` |
| SkillMatrix | FR-101~FR-180, RR-009 | `02_Rule_Engine.md`, `07_Domain_Model.md` |
| CareerProfile, CareerReadiness, SkillGap | FR-181~FR-220, CR-001~CR-020 | `03_Career_Path_Engine.md` |
| CompanyProfile, CompanyReadiness | FR-181~FR-220, CR-003~CR-004 | `03_Career_Path_Engine.md` |
| Recommendation, LearningRoadmap | FR-181~FR-220, CR-005~CR-006, CR-009~CR-014 | `03_Career_Path_Engine.md` |
| KnowledgeDocument, KnowledgeChunk, EmbeddingMetadata | FR-051~FR-070, FR-321~FR-340, KR-001~KR-020 | `06_Knowledge_Architecture.md` |
| PromptTemplate, PromptContext, PromptExecution | FR-221~FR-280, PR-001~PR-015 | `05_Prompt_Engineering.md` |
| AITask, AIResponse, GeneratedArtifact | FR-221~FR-280, AI-001~AI-015 | `04_AI_Architecture.md` |
| Portfolio, Resume, InterviewQuestionSet | FR-221~FR-280, AI-005~AI-007 | `04_AI_Architecture.md`, `07_Domain_Model.md` |
| ConfigurationChange | FR-341~FR-360 | `07_Domain_Model.md` Administration Context |

### 13.2 Rule Engine Traceability

| Rule Engine Data | Traceability |
|---|---|
| RuleSetVersion | Required by RR-001~RR-010 and referenced by every Evaluation. |
| Evaluation | Created from RepositorySnapshot and normalized facts; produces official scores. |
| Evidence | Links source observations to rules, scores, skill assessments, and recommendations. |
| Score | Deterministic value calculated only by Rule Engine. |
| SkillMatrix | Structured output consumed by Career, Company, Recommendation, Prompt, and Dashboard. |

### 13.3 Career Engine Traceability

| Career Engine Data | Traceability |
|---|---|
| CareerProfileVersion | Supports CR-001 and CR-002. |
| CompanyProfileVersion | Supports CR-003 and CR-004. |
| CareerReadiness | Supports CR-005 and CR-007. |
| CompanyReadiness | Supports CR-008 and company-specific recommendations. |
| SkillGap | Supports CR-005, recommendation generation, and roadmap creation. |
| Recommendation | Supports CR-009~CR-014. |
| LearningRoadmap | Supports CR-006. |

### 13.4 Knowledge, Prompt, and AI Traceability

| Data Area | Data Objects | Requirement Mapping |
|---|---|---|
| Knowledge | KnowledgeDocument, KnowledgeDocumentVersion, KnowledgeChunk, EmbeddingMetadata, RetrievalResult | KR-001~KR-020 |
| Prompt | PromptTemplate, PromptTemplateVersion, PromptContext, PromptExecution | PR-001~PR-015 |
| AI | AITask, ModelExecution, AIResponse, ResponseValidationResult, GeneratedArtifact | AI-001~AI-015 |

### 13.5 Domain Model Traceability

| Domain Model Chapter | System Data Model Mapping |
|---|---|
| Bounded Contexts | Data ownership, source of truth, storage responsibility |
| Core Entities | Canonical object catalog |
| Value Objects | Identity, version, snapshot, confidence, priority, score semantics |
| Aggregates | Consistency and lifecycle boundaries |
| Domain Events | Data flow and synchronization triggers |
| Domain Invariants | Data consistency rules and validation expectations |

## 14. Open Issues

| Issue ID | Open Issue | Impact | Recommended Owner |
|---|---|---|---|
| SDM-OPEN-001 | Exact retention periods for snapshots, evaluations, prompt contexts, AI responses, and artifacts are not finalized. | Affects database design, storage cost, privacy compliance. | `09_Database_Design.md` |
| SDM-OPEN-002 | Physical isolation model for vector retrieval is undecided. | Affects privacy, performance, and future enterprise tenancy. | `09_Database_Design.md` |
| SDM-OPEN-003 | Whether ProjectSnapshot is required in the first release or can be represented by artifact source references. | Affects portfolio/resume reproducibility. | `09_Database_Design.md`, `11_Backend_Architecture.md` |
| SDM-OPEN-004 | How much raw repository document content should be retained versus metadata-only snapshots. | Affects storage, privacy, and AI grounding quality. | `09_Database_Design.md` |
| SDM-OPEN-005 | Exact cache invalidation policy for dashboard, retrieval, and prompt-context candidates is not finalized. | Affects user freshness expectations. | `11_Backend_Architecture.md` |
| SDM-OPEN-006 | How future organization or team ownership changes user isolation rules. | Affects multi-tenant data model. | Future enterprise architecture |
| SDM-OPEN-007 | Whether coding-practice platform data can become official Rule Engine evidence. | Affects evidence acceptance and rule-set design. | `02_Rule_Engine.md` future revision |
| SDM-OPEN-008 | How generated artifact export history should be retained across formats. | Affects object storage and artifact metadata design. | `09_Database_Design.md` |

## 15. Future Extension

### 15.1 Future Integration Support

| Future Capability | Data Model Extension | Constraint |
|---|---|---|
| GitLab | Add external provider mapping to Repository and RepositorySnapshot canonical objects. | Provider model must not leak into canonical repository model. |
| Jira | Add issue/project work items as KnowledgeDocument sources or future collaboration evidence. | Must not affect official scores until deterministic rules support it. |
| Slack | Add conversation or workspace knowledge as permission-scoped KnowledgeDocuments. | Strong privacy filtering is required. |
| Figma | Add design artifacts as KnowledgeDocuments or Project references. | Design data must remain distinct from source-code repository evidence. |
| Organization | Add organization ownership scope above User. | Must preserve individual privacy and consent. |
| Enterprise | Add tenant-level governance, admin roles, and organization-wide configuration. | Must not weaken user isolation by default. |
| Team Workspace | Add shared project and team analytics concepts. | Requires explicit membership and permission modeling. |

### 15.2 Extension Rules

| Rule ID | Rule |
|---|---|
| SDM-EXT-001 | New external sources must enter through anti-corruption normalization. |
| SDM-EXT-002 | New score-producing data must be approved by Rule Context and deterministic rules. |
| SDM-EXT-003 | New AI providers must not change AI boundary constraints. |
| SDM-EXT-004 | New generated artifact types must carry PromptContext, source references, and provenance. |
| SDM-EXT-005 | New enterprise ownership models must explicitly define tenant ownership, user ownership, and sharing boundaries. |
| SDM-EXT-006 | New retrieval sources must support metadata, freshness, privacy classification, and deletion propagation. |

### 15.3 Future Document Readiness

| Future Document | How This Document Supports It |
|---|---|
| `09_Database_Design.md` | Provides canonical objects, ownership, lifecycle, snapshot strategy, version strategy, storage responsibility, and classification. |
| `10_API_Specification.md` | Provides identity, lifecycle, version, snapshot, and flow semantics for API resources and commands. |
| `11_Backend_Architecture.md` | Provides consistency rules, synchronization responsibilities, storage responsibility, and context data ownership. |
