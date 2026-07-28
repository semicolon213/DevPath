# DevPath Database Design

## 1. Purpose

### 1.1 Purpose

This document defines the logical and physical database architecture for DevPath. It explains how the conceptual system data model is persisted across relational storage, cache storage, vector storage, and object storage.

This document is the authoritative reference for backend persistence implementation. It is not an API specification, backend implementation guide, ORM model, migration script, or executable database definition.

### 1.2 Scope

This document covers:

- Physical storage architecture.
- Logical database design.
- Storage mapping for canonical data objects.
- Database boundary and schema ownership.
- Major table definitions at a conceptual level.
- Relationships, constraints, indexes, partitioning, versioning, audit, backup, recovery, performance, scalability, and traceability.

This document does not include:

- SQL statements.
- Executable table creation scripts.
- ORM entities.
- Migration scripts.
- API endpoints.
- Backend classes.
- Infrastructure provisioning.

### 1.3 Audience

| Audience | Usage |
|---|---|
| Backend Engineers | Implement persistence models, repositories, and transactional boundaries |
| Data Architects | Validate logical schemas, relationships, integrity, versioning, and storage responsibility |
| AI Engineers | Understand where prompt, AI response, knowledge, and vector metadata are persisted |
| Security Engineers | Validate privacy, audit, credential, and deletion design |
| QA Engineers | Design persistence, integrity, historical, and recovery tests |
| DevOps Engineers | Understand storage responsibilities for backup, monitoring, and scaling without treating this document as deployment specification |

### 1.4 References

| Reference | Usage |
|---|---|
| `00_Project_Context.md` | Product vision, modules, technical stack, and core philosophy |
| `01_SRS.md` | Functional requirements and non-functional requirements |
| `02_Rule_Engine.md` | Rule, score, evaluation, evidence, and Skill Matrix persistence needs |
| `03_Career_Path_Engine.md` | Career, company, recommendation, and roadmap persistence needs |
| `04_AI_Architecture.md` | AI task, response, validation, model execution, and generated artifact persistence needs |
| `05_Prompt_Engineering.md` | Prompt template, prompt context, prompt execution, and prompt version persistence needs |
| `06_Knowledge_Architecture.md` | Knowledge document, chunk, embedding, retrieval, and index persistence needs |
| `07_Domain_Model.md` | Domain ownership, aggregates, invariants, lifecycle, and events |
| `08_System_Data_Model.md` | Canonical data model, storage responsibility, lifecycle, snapshot, version, and classification |

## 2. Database Architecture

### 2.1 Overall Storage Architecture

DevPath uses multiple storage systems because different data types have different access patterns, consistency requirements, retention requirements, and query models.

| Storage | Primary Role | Consistency Expectation | Primary Workload |
|---|---|---|---|
| PostgreSQL | Primary relational store for canonical business data and transactional history | Strong consistency for authoritative data | User, repository metadata, snapshots metadata, evaluations, careers, prompts, artifacts, audit |
| Redis | Ephemeral cache, optional future session store, rate-limit coordination, temporary workflow state | Rebuildable and non-authoritative | Short-lived caches, locks, queue coordination, rate-limit counters; sessions only after a future scaling review |
| Vector Database | Semantic retrieval index for knowledge chunks | Eventually consistent derived index | Embedding similarity search, hybrid retrieval support |
| Object Storage | Large binary/text content and generated files | Referenced by canonical metadata | Repository archives, documents, exports, PDFs, images, generated files |

### 2.2 Why Multiple Databases Exist

| Reason | Explanation |
|---|---|
| Relational integrity | Authoritative business data requires identity, relationships, lifecycle state, transactions, and auditability. |
| Operational state | Rate limits and temporary workflow state require fast expiration; initial authenticated sessions use JDBC-backed PostgreSQL under ADR-026 and may move to Redis only after a scaling review. |
| Semantic search | Knowledge retrieval requires vector similarity and retrieval indexes, which are not the source of business truth. |
| Large object handling | Repository archives, document bodies, images, and generated exports are better persisted as objects referenced by metadata. |
| Cost and scalability | Large generated content and vector indexes scale differently from transactional business records. |

### 2.3 Storage Responsibility Rules

| Rule ID | Rule |
|---|---|
| DB-ARCH-001 | PostgreSQL is the authoritative store for canonical business records and lifecycle states. |
| DB-ARCH-002 | Redis must not be the sole storage for authoritative business data. |
| DB-ARCH-003 | Vector Database stores retrieval indexes and vector representations only; it does not own business entities. |
| DB-ARCH-004 | Object Storage stores large content and generated files; PostgreSQL stores the authoritative metadata and references. |
| DB-ARCH-005 | Credentials must be stored through secure secret management references, not as ordinary relational fields. |
| DB-ARCH-006 | Every derived data object must preserve references to its source canonical object, snapshot, or version. |

### 2.4 Accepted Persistence and Migration Baseline

| Area | Accepted Rule |
|---|---|
| Persistence technology | Spring Data JPA with Hibernate is the primary write-side approach under ADR-024. |
| Domain isolation | Domain types contain no persistence annotations; adapter-owned persistence models map explicitly to domain objects. |
| Schema ownership | Flyway versioned SQL migrations under ADR-025 are the only authoritative schema-evolution mechanism. |
| ORM schema behavior | Runtime schema creation/update is prohibited outside disposable tests; production uses validation only. |
| Transaction ownership | Application use cases own transactions; provider and long-running external calls remain outside database transactions. |
| Read queries | Module-owned projections may use JPQL or reviewed native SQL; persistence entities never become API models. |

## 3. Storage Mapping

### 3.1 Canonical Object Storage Mapping

| Canonical Object | Primary Storage | Secondary Storage | Rationale |
|---|---|---|---|
| User | PostgreSQL | None required | User and external identity links are authoritative; application sessions are separate operational state. |
| GitHubAccount | PostgreSQL | Secure secret store reference outside ordinary data model | Connection metadata is relational; credentials are sensitive. |
| NotionWorkspace | PostgreSQL | Secure secret store reference outside ordinary data model | Workspace identity and scope are authoritative metadata. |
| Repository | PostgreSQL | Redis for sync cache | Repository metadata requires relational ownership and lifecycle. |
| RepositorySnapshot | PostgreSQL for metadata | Object Storage for large captured archive/content | Snapshot identity and lifecycle are relational; large content is object-based. |
| Project | PostgreSQL | Object Storage for attached large documents if needed | Project metadata and relationships are relational. |
| ProjectSnapshot | PostgreSQL for metadata | Object Storage for large frozen content | Snapshot references are authoritative relational records. |
| Evaluation | PostgreSQL | None | Official scores and evidence links require strong consistency. |
| SkillMatrix | PostgreSQL | None | Skill assessments are authoritative derived records. |
| CareerProfile | PostgreSQL | None | Versioned reference data. |
| CompanyProfile | PostgreSQL | None | Versioned reference data. |
| CareerReadiness | PostgreSQL | Redis for dashboard cache | Readiness is authoritative derived data. |
| CompanyReadiness | PostgreSQL | Redis for dashboard cache | Company readiness is authoritative derived data. |
| Recommendation | PostgreSQL | Redis for active recommendation view cache | Recommendation priority and status are authoritative. |
| LearningRoadmap | PostgreSQL | Redis for progress view cache | Roadmap progress is authoritative and user-owned. |
| KnowledgeDocument | PostgreSQL | Object Storage for source content | Metadata and lifecycle are authoritative; content may be large. |
| KnowledgeChunk | PostgreSQL for metadata | Vector Database for searchable vector representation | Chunk identity and provenance are relational; similarity search is vector-based. |
| EmbeddingMetadata | PostgreSQL for metadata | Vector Database for vector index | Model/version/source traceability is relational. |
| RetrievalResult | PostgreSQL for execution history where retained | Redis for short-lived retrieval cache | Retrieval result may be audited; cache is non-authoritative. |
| PromptTemplate | PostgreSQL | None | Versioned prompt definition metadata and content are authoritative configuration. |
| PromptContext | PostgreSQL for metadata and selected source references | Redis only for temporary pre-lock context candidate | Locked PromptContext is authoritative execution data. |
| PromptExecution | PostgreSQL | Redis for in-flight status cache | Execution status is authoritative in PostgreSQL. |
| AITask | PostgreSQL | Redis for queue/status cache | AI task lifecycle is authoritative. |
| AIResponse | PostgreSQL for metadata | Object Storage for large response body if needed | Validation and provenance are relational; large response content may be object-based. |
| GeneratedArtifact | PostgreSQL for metadata | Object Storage for content/export files | Artifact lifecycle and provenance are relational. |
| Portfolio | PostgreSQL for metadata and version state | Object Storage for exported files and rendered assets | Publication state is authoritative. |
| Resume | PostgreSQL for metadata and version state | Object Storage for exports | Publication state is authoritative. |
| InterviewQuestionSet | PostgreSQL | None or Object Storage for exports | Questions are structured generated artifact data. |
| ConfigurationChange | PostgreSQL | None | Auditability and governance require durable relational history. |
| ApplicationSession | PostgreSQL through JDBC-backed session storage for MVP | In-memory only for local single-instance development; Redis is a future option | Session state is opaque, expirable, revocable, and distinct from provider credentials. |
| ProviderCredential | PostgreSQL restricted encrypted record or approved secret mechanism | No browser or Redis token copy | Provider API authorization material is server-side, user-scoped, encrypted, and adapter-owned. |
| Rate Limit | Redis | Optional aggregated logs in PostgreSQL | Rate counters are operational. |
| Generated Portfolio PDF | Object Storage | PostgreSQL metadata reference | Binary export should not live inside relational records. |
| Generated Resume Export | Object Storage | PostgreSQL metadata reference | Binary export should be object-based. |

### 3.2 Storage Exclusion Rules

| Storage | Must Not Store |
|---|---|
| PostgreSQL | Raw embedding vectors when vector storage is used as the retrieval engine; large binary exports as ordinary relational values. |
| Redis | Official scores, completed evaluations, published artifact versions, audit records, long-term prompt contexts. |
| Vector Database | User identity truth, recommendation priority, rule definitions, career/company profiles, prompt templates. |
| Object Storage | Ownership truth, authorization state, active version pointers, official score values as the only copy. |

## 4. Logical Database Design

### 4.1 Logical Schema Overview

Logical schemas group tables by ownership and access patterns. Schema names are conceptual; the physical naming may be adjusted during implementation while preserving ownership.

| Logical Schema | Owner Context | Purpose |
|---|---|---|
| Identity | Identity Context | Users, external accounts, consent, settings, authorization-relevant metadata |
| Repository | Repository Context | Repositories, snapshots, source activity metadata, repository documents |
| Analysis | Rule Context | Rules, evaluations, scores, evidence, Skill Matrix |
| Career | Career Context and Company Context | Careers, career profiles, company profiles, readiness assessments, skill gaps |
| Recommendation | Recommendation and Learning Contexts | Recommendations, roadmap, roadmap progress, learning resources |
| Knowledge | Knowledge Context | Knowledge documents, versions, chunks, embedding metadata, retrieval history |
| Prompt | Prompt Context | Prompt templates, template versions, prompt contexts, prompt executions |
| Artifact | AI and Portfolio Contexts | AI tasks, responses, generated artifacts, portfolio, resume, interview artifacts |
| Administration | Administration Context | Configuration changes, administrative actions, system-managed version activation records |
| Audit | Audit capability | Append-only audit records, access records, deletion records |

### 4.2 Schema Ownership Rules

| Rule ID | Rule |
|---|---|
| DB-SCH-001 | Each logical schema has one owning context. |
| DB-SCH-002 | Cross-schema references must use stable identifiers, version identifiers, or snapshot identifiers. |
| DB-SCH-003 | Tables storing immutable records must not be updated except for retention or archival metadata explicitly allowed by policy. |
| DB-SCH-004 | Read-heavy projections may exist but must not become sources of truth. |
| DB-SCH-005 | Administrative configuration tables must preserve activation, deprecation, and historical references. |

## 5. Table Definitions

### 5.1 Identity Tables

| Table | Purpose | Primary Key | Foreign Keys | Important Columns | Relationships | Lifecycle |
|---|---|---|---|---|---|---|
| users | Stores DevPath account identity. | user_id | None | account status, role, registration time, deletion state | One user owns external accounts, settings, profiles, and user-owned records | Registered to deleted/anonymized |
| external_accounts | Stores provider account linkage metadata. | external_account_id | user_id | provider, provider subject/account reference, connection status, scope summary | Many external accounts may belong to one user; provider plus provider subject is unique under supported policy | Connected, revoked, disconnected |
| provider_credentials | Stores encrypted provider API authorization material and rotation metadata. | provider_credential_id | user_id, external_account_id or workspace reference | provider, encrypted credential reference/material, scopes, expiry, key version, revocation state | One active credential set belongs to one authorized provider connection | Active, expired, revoked, deleted |
| application_sessions | Stores JDBC-backed opaque application-session state for MVP. | session identifier | user_id through authenticated principal relationship | creation, last activity, idle/absolute expiry, revocation and security metadata | Many active sessions may belong to one user; sessions do not own identity | Active, expired, revoked, deleted |
| user_consents | Stores user consent decisions. | consent_id | user_id | consent type, consent version, granted time, revoked time | Many consent records belong to one user | Granted, revoked, expired |
| user_settings | Stores configurable user preferences. | setting_id | user_id | setting type, setting value, updated time | Many settings belong to one user | Active, updated, deleted |
| user_preferences | Stores target career and company selections. | preference_id | user_id, career_id, company_id where applicable | preference type, selected value, active flag, selected time | User has active career preference and optional company preference | Active, superseded |

### 5.2 Repository Tables

| Table | Purpose | Primary Key | Foreign Keys | Important Columns | Relationships | Lifecycle |
|---|---|---|---|---|---|---|
| repositories | Stores canonical repository metadata. | repository_id | user_id, external_account_id | provider repository reference, full name, visibility, default branch, archived flag, sync status | One repository has many snapshots | Discovered, active, archived, deleted externally |
| repository_snapshots | Stores immutable repository snapshot metadata. | snapshot_id | repository_id | source revision, captured time, snapshot status, content reference, content hash | One repository has many snapshots; one snapshot supports many evaluations | Capturing, ready, failed, superseded, deleted by policy |
| repository_branches | Stores branch facts captured in a snapshot. | branch_record_id | snapshot_id | branch name, default flag, head commit reference | Many branches belong to one snapshot | Snapshot-local immutable |
| repository_commits | Stores commit facts captured in a snapshot. | commit_record_id | snapshot_id | commit hash, author reference, committed time, message summary reference | Many commits belong to one snapshot | Snapshot-local immutable |
| repository_pull_requests | Stores PR facts captured in a snapshot. | pull_request_record_id | snapshot_id | provider PR reference, status, opened time, merged time, review count | Many PRs belong to one snapshot | Snapshot-local immutable |
| repository_issues | Stores issue facts captured in a snapshot. | issue_record_id | snapshot_id | provider issue reference, status, labels, opened time, closed time | Many issues belong to one snapshot | Snapshot-local immutable |
| repository_dependencies | Stores dependency declarations captured in a snapshot. | dependency_record_id | snapshot_id | ecosystem, package name, version, manifest path, source reference | Many dependencies belong to one snapshot | Snapshot-local immutable |
| repository_documents | Stores repository document metadata. | repository_document_id | snapshot_id | document type, path, content hash, object content reference | May become KnowledgeDocument source | Captured, excluded, deleted by policy |

### 5.3 Analysis and Rule Tables

| Table | Purpose | Primary Key | Foreign Keys | Important Columns | Relationships | Lifecycle |
|---|---|---|---|---|---|---|
| rule_sets | Stores rule-set stable identity. | rule_set_id | None | name, scope, active version reference, status | One rule set has many versions | Draft, active, deprecated |
| rule_set_versions | Stores immutable rule-set versions. | rule_set_version_id | rule_set_id | version label, effective time, status, weight model, validation status | Referenced by evaluations | Draft, active, superseded, deprecated |
| rules | Stores rule definitions within versioned rule sets. | rule_id | rule_set_version_id | rule category, condition definition reference, outcome definition, severity | Many rules belong to one rule-set version | Draft before activation; immutable after activation |
| evaluations | Stores evaluation execution records. | evaluation_id | user_id, snapshot_id, rule_set_version_id | status, started time, completed time, deterministic input hash | One evaluation has category results and skill matrix | Requested, running, completed, failed, superseded |
| category_evaluations | Stores category-level scores. | category_evaluation_id | evaluation_id | category, score, confidence, weight, evidence count | Many category evaluations belong to one evaluation | Immutable after evaluation completion |
| rule_execution_results | Stores per-rule execution outcomes. | rule_execution_result_id | evaluation_id, rule_id | outcome status, score contribution, evidence reference count | Many rule results belong to one evaluation | Immutable after evaluation completion |
| evidence_records | Stores accepted evidence metadata. | evidence_id | user_id, snapshot_id or knowledge_document_version_id | evidence type, source reference, observed fact summary, confidence, freshness | Evidence links to scores, skills, recommendations | Extracted, accepted, rejected, superseded, deleted by policy |
| score_evidence_links | Links scores and rule results to evidence. | score_evidence_link_id | evidence_id, evaluation_id | target score type, contribution role, rule reference | Many links per evaluation | Immutable after evaluation completion |
| skill_matrices | Stores Skill Matrix headers and generation basis. | skill_matrix_id | user_id, evaluation_id | generated time, status, overall summary reference | One matrix has many skill assessments | Generated, published, superseded, archived |
| skill_assessments | Stores per-skill assessment records. | skill_assessment_id | skill_matrix_id, skill_id | level, confidence, strength/weakness flag, evidence summary | Many assessments belong to one Skill Matrix | Immutable after matrix publication |
| skill_evidence_links | Links skill assessments to evidence. | skill_evidence_link_id | skill_assessment_id, evidence_id | evidence strength, source role | Many links per skill assessment | Immutable after matrix publication |
| technologies | Stores canonical technology reference data. | technology_id | None | name, category, aliases reference, status | Referenced by dependencies and assessments | Active, deprecated, merged |
| skills | Stores canonical skill reference data. | skill_id | None | name, category, level scale, status | Referenced by skill assessments and career profiles | Active, deprecated, merged |

### 5.4 Career and Company Tables

| Table | Purpose | Primary Key | Foreign Keys | Important Columns | Relationships | Lifecycle |
|---|---|---|---|---|---|---|
| careers | Stores supported career identities. | career_id | None | career name, supported status, active profile version reference | One career has many profile versions | Supported, deprecated, future candidate |
| career_profile_versions | Stores immutable career profile versions. | career_profile_version_id | career_id | version label, competency expectations, thresholds, weights, effective time | Referenced by career readiness assessments | Draft, active, superseded, deprecated |
| companies | Stores supported company identities. | company_id | None | company name, supported status, active profile version reference | One company has many profile versions | Supported, deprecated, future candidate |
| company_profile_versions | Stores immutable company profile versions. | company_profile_version_id | company_id | expectations, company weights, interview focus, effective time | Referenced by company readiness assessments | Draft, active, superseded, deprecated |
| career_readiness_assessments | Stores career readiness results. | career_readiness_id | user_id, skill_matrix_id, career_profile_version_id | readiness level, status, assessed time, summary reference | One assessment has many skill gaps | Requested, completed, failed, superseded |
| company_readiness_assessments | Stores company readiness results. | company_readiness_id | user_id, skill_matrix_id, company_profile_version_id | readiness level, status, assessed time, summary reference | May support recommendations and interview questions | Requested, completed, failed, superseded |
| skill_gaps | Stores identified gaps against career/company expectations. | skill_gap_id | career_readiness_id, skill_id | expected level, actual level, gap magnitude, priority basis | Many gaps belong to one readiness assessment | Identified, addressed, superseded |

### 5.5 Recommendation and Learning Tables

| Table | Purpose | Primary Key | Foreign Keys | Important Columns | Relationships | Lifecycle |
|---|---|---|---|---|---|---|
| recommendation_sets | Stores recommendation set generation basis. | recommendation_set_id | user_id, career_readiness_id, company_readiness_id optional | generated time, policy version, status | One set contains many recommendations | Draft, published, superseded |
| recommendations | Stores individual recommendations. | recommendation_id | recommendation_set_id | recommendation type, deterministic priority, reason code, status | May link to gaps/evidence and roadmap steps | Proposed, accepted, dismissed, completed, superseded |
| recommendation_evidence_links | Links recommendations to evidence or skill gaps. | recommendation_evidence_link_id | recommendation_id, evidence_id or skill_gap_id | reason role, strength, explanation reference | Many links per recommendation | Immutable after recommendation publication |
| learning_roadmaps | Stores learning roadmap header. | roadmap_id | user_id, recommendation_set_id | status, generated time, progress summary | One roadmap contains many steps | Created, in progress, completed, archived |
| roadmap_steps | Stores roadmap actions. | roadmap_step_id | roadmap_id | order, target skill, difficulty, expected duration, completion criteria, status | Many steps belong to one roadmap | Not started, in progress, completed, skipped |
| roadmap_milestones | Stores measurable roadmap milestones. | milestone_id | roadmap_id | milestone title, target date, completion state | Milestones group roadmap steps | Planned, achieved, skipped |
| learning_resources | Stores learning resource metadata. | learning_resource_id | roadmap_step_id optional | resource type, title, reference, difficulty | Resources support roadmap steps | Suggested, used, archived |

### 5.6 Knowledge Tables

| Table | Purpose | Primary Key | Foreign Keys | Important Columns | Relationships | Lifecycle |
|---|---|---|---|---|---|---|
| knowledge_documents | Stores knowledge document identity and metadata. | knowledge_document_id | user_id | source type, source reference, privacy class, freshness state, active version reference | One document has many versions | Discovered, ingested, indexed, stale, deleted |
| knowledge_document_versions | Stores immutable knowledge content versions. | knowledge_document_version_id | knowledge_document_id | content hash, source update time, object content reference, version status | One version has many chunks | Created, indexed, superseded, deleted |
| knowledge_chunks | Stores chunk metadata. | knowledge_chunk_id | knowledge_document_version_id | chunk position, content hash, token estimate, metadata, index status | One chunk may have embedding metadata | Created, embedded, indexed, stale, deleted |
| embedding_records | Stores embedding metadata and vector references. | embedding_record_id | knowledge_chunk_id | provider, model, model version, vector reference, status | Vector Database stores actual search index | Pending, active, stale, deleted |
| retrieval_requests | Stores retained retrieval request metadata. | retrieval_request_id | user_id | request intent, filters summary, token budget, requested time | One request may have one result | Requested, completed, failed |
| retrieval_results | Stores retained retrieval result metadata. | retrieval_result_id | retrieval_request_id | selected chunk references, ranking metadata, completed time | Consumed by PromptContext | Completed, expired |

### 5.7 Prompt Tables

| Table | Purpose | Primary Key | Foreign Keys | Important Columns | Relationships | Lifecycle |
|---|---|---|---|---|---|---|
| prompt_templates | Stores prompt template identity. | prompt_template_id | None | category, owner context, active version reference, status | One template has many versions | Draft, active, deprecated |
| prompt_template_versions | Stores immutable prompt template versions. | prompt_template_version_id | prompt_template_id | version label, variable schema, constraints, output format, effective time | Referenced by PromptContext | Draft, active, superseded, deprecated |
| prompt_contexts | Stores immutable prompt context metadata. | prompt_context_id | user_id, prompt_template_version_id | task type, token budget, source reference summary, locked time | Referenced by PromptExecution and AIResponse | Created, validated, locked, rejected |
| prompt_context_sources | Stores source references included in PromptContext. | prompt_context_source_id | prompt_context_id | source object type, source object id, version/snapshot id, evidence role | Many sources per context | Immutable after context lock |
| prompt_executions | Stores prompt execution lifecycle. | prompt_execution_id | prompt_context_id | status, requested time, submitted time, completed time, failure reason | One execution may produce AI task/model execution | Requested, submitted, completed, failed |

### 5.8 Artifact and AI Tables

| Table | Purpose | Primary Key | Foreign Keys | Important Columns | Relationships | Lifecycle |
|---|---|---|---|---|---|---|
| ai_tasks | Stores AI task requests. | ai_task_id | user_id, prompt_execution_id | task type, status, provider policy, requested time | One task may have multiple model executions | Requested, running, completed, failed, canceled |
| model_executions | Stores LLM provider execution attempts. | model_execution_id | ai_task_id | provider, model identifier, attempt number, status, token usage summary | One execution may produce AIResponse | Submitted, completed, timed out, failed, retried |
| ai_responses | Stores AI response metadata. | ai_response_id | model_execution_id, prompt_context_id | validation status, confidence, response content reference, received time | Validated response may create artifact | Received, validated, rejected |
| response_validation_results | Stores response validation outcomes. | validation_result_id | ai_response_id | validation status, failure reason, grounding status, format status | One response has validation result history | Passed, failed, needs review |
| generated_artifacts | Stores generated artifact metadata. | generated_artifact_id | user_id, ai_response_id, prompt_context_id | artifact type, status, provenance summary, content reference | May become portfolio/resume/README/interview artifact | Draft, validated, reviewed, approved, published, superseded |
| portfolios | Stores portfolio identity and active state. | portfolio_id | user_id | title, status, active version reference | One portfolio has many versions | Draft, generated, reviewed, published, archived |
| portfolio_versions | Stores immutable portfolio versions. | portfolio_version_id | portfolio_id, generated_artifact_id optional | version label, publication state, source references, content reference | One version has sections | Draft, reviewed, published, superseded |
| portfolio_sections | Stores portfolio section metadata/content reference. | portfolio_section_id | portfolio_version_id | section type, order, content reference, provenance | Many sections per portfolio version | Immutable after publication |
| resumes | Stores resume identity and active state. | resume_id | user_id | title, status, active version reference | One resume has many versions | Draft, generated, reviewed, published, archived |
| resume_versions | Stores immutable resume versions. | resume_version_id | resume_id, generated_artifact_id optional | version label, publication state, source references, content reference | One version has sections | Draft, reviewed, published, superseded |
| resume_sections | Stores resume section metadata/content reference. | resume_section_id | resume_version_id | section type, order, content reference, provenance | Many sections per resume version | Immutable after publication |
| interview_question_sets | Stores generated interview question sets. | question_set_id | user_id, generated_artifact_id optional | career/company references, difficulty distribution, status | One set has many questions | Generated, reviewed, archived |
| interview_questions | Stores individual interview questions. | question_id | question_set_id | topic, difficulty, source reference, answer guidance reference | Many questions per set | Generated, reviewed, practiced, archived |
| artifact_exports | Stores generated export metadata. | artifact_export_id | generated_artifact_id or portfolio_version_id or resume_version_id | export type, object storage reference, generated time, status | References Object Storage content | Requested, generated, expired, deleted |

### 5.9 Administration and Audit Tables

| Table | Purpose | Primary Key | Foreign Keys | Important Columns | Relationships | Lifecycle |
|---|---|---|---|---|---|---|
| configuration_changes | Stores administrative configuration changes. | configuration_change_id | admin user reference | target configuration type, target version, action, effective time | Links to activated/deprecated rule/career/company/prompt versions | Recorded, applied, reverted by new change |
| audit_records | Stores append-only audit facts. | audit_record_id | actor user reference optional | action type, resource reference, privacy class, timestamp, outcome | References protected resources conceptually | Recorded, retained, purged by policy |
| deletion_requests | Stores user data deletion workflow metadata. | deletion_request_id | user_id | request time, status, scope, completion time | Coordinates deletion across schemas and stores | Requested, processing, completed, failed |
| system_statistics | Stores aggregated operational statistics. | statistic_id | None | metric type, period, aggregate value, generated time | Derived from operational data | Generated, expired |

## 6. Relationships

### 6.1 Relationship Types

| Relationship Type | Definition | Example |
|---|---|---|
| One-to-One | One record relates to exactly one counterpart. | User to active UserSetting group where modeled as single settings document. |
| One-to-Many | One parent owns or references many child records. | Repository to RepositorySnapshots. |
| Many-to-Many | Many records relate to many records through a linking table. | Recommendations to Evidence records. |
| Ownership | Parent controls lifecycle of child. | KnowledgeDocument owns KnowledgeDocumentVersions. |
| Composition | Child has no independent lifecycle outside parent. | PortfolioVersion contains PortfolioSections. |
| Aggregation | Parent references related data that has independent lifecycle. | PortfolioVersion references ProjectSnapshot. |
| Reference | Relationship by stable ID, version ID, snapshot ID, or source reference. | PromptContext references SkillMatrix and RetrievalResult. |

### 6.2 Major Relationships

| Relationship | Cardinality | Type | Integrity Expectation |
|---|---|---|---|
| User to ExternalAccount | One-to-many | Ownership | External account cannot exist without User. |
| User to Repository | One-to-many | Ownership | Repository belongs to one user scope. |
| Repository to RepositorySnapshot | One-to-many | Composition by snapshot history | Snapshot references one repository and remains immutable. |
| RepositorySnapshot to Evaluation | One-to-many | Reference | Evaluation must reference exact snapshot. |
| RuleSet to RuleSetVersion | One-to-many | Ownership/versioning | Active version pointer selects current version. |
| RuleSetVersion to Evaluation | One-to-many | Reference | Completed evaluation retains original rule version. |
| Evaluation to CategoryEvaluation | One-to-many | Composition | Category results belong to one evaluation. |
| Evaluation to Evidence | Many-to-many through links | Reference | Evidence may support multiple scores. |
| Evaluation to SkillMatrix | One-to-one or one-to-many by regeneration policy | Derived reference | SkillMatrix records generation basis. |
| SkillMatrix to CareerReadiness | One-to-many | Reference | Multiple career assessments may use one matrix. |
| SkillMatrix to CompanyReadiness | One-to-many | Reference | Multiple company assessments may use one matrix. |
| CareerProfileVersion to CareerReadiness | One-to-many | Reference | Assessment retains profile version. |
| CompanyProfileVersion to CompanyReadiness | One-to-many | Reference | Assessment retains profile version. |
| CareerReadiness to SkillGap | One-to-many | Composition | Gaps belong to assessment. |
| Assessment to RecommendationSet | One-to-many | Reference | Recommendation set records source assessments. |
| RecommendationSet to Recommendation | One-to-many | Composition | Recommendations belong to set. |
| Recommendation to Evidence or SkillGap | Many-to-many | Reference | Recommendations must remain evidence/gap traceable. |
| RecommendationSet to LearningRoadmap | One-to-many | Reference | Roadmap generation basis retained. |
| LearningRoadmap to RoadmapStep | One-to-many | Composition | Steps belong to roadmap. |
| KnowledgeDocument to KnowledgeDocumentVersion | One-to-many | Ownership/versioning | Version belongs to one document. |
| KnowledgeDocumentVersion to KnowledgeChunk | One-to-many | Composition | Chunk belongs to one document version. |
| KnowledgeChunk to EmbeddingRecord | One-to-many by model version | Derived reference | Embedding records reference chunk and model. |
| RetrievalRequest to RetrievalResult | One-to-one or one-to-many by retry policy | Execution history | Result references request. |
| PromptTemplate to PromptTemplateVersion | One-to-many | Ownership/versioning | Execution references template version. |
| PromptContext to PromptContextSource | One-to-many | Composition | Sources are immutable after lock. |
| PromptContext to AIResponse | One-to-many | Reference | Multiple attempts may use same context. |
| AITask to ModelExecution | One-to-many | Execution attempts | Fallback attempts recorded separately. |
| AIResponse to GeneratedArtifact | One-to-many | Derived output | Artifact references validated response. |
| Portfolio to PortfolioVersion | One-to-many | Ownership/versioning | Published versions immutable. |
| Resume to ResumeVersion | One-to-many | Ownership/versioning | Published versions immutable. |

## 7. Index Strategy

### 7.1 Index Types

| Index Type | Purpose | Examples |
|---|---|---|
| Primary Index | Fast access by primary identity. | user_id, repository_id, snapshot_id, evaluation_id. |
| Unique Index | Enforce natural uniqueness within scope. | provider account per user, repository external ID per provider/user, active profile version per career. |
| Composite Index | Support common scoped queries. | user plus status, repository plus captured time, skill matrix plus generated time. |
| Search Index | Support text or metadata search. | repository names, project titles, artifact titles, knowledge document metadata. |
| Full-text Index | Support keyword search over retained text metadata. | README summaries, document titles, generated artifact summaries. |
| Vector Index | Support semantic similarity search. | KnowledgeChunk embeddings in vector storage. |
| Time Index | Support historical and audit queries. | created time, completed time, captured time, event time. |
| Status Index | Support workflow dashboards. | sync status, evaluation status, AI task status, roadmap step status. |

### 7.2 PostgreSQL Index Strategy

| Data Area | Index Strategy | Reason |
|---|---|---|
| Identity | User identity, external provider identity, account status. | Login, authorization, account lookup. |
| Repository | User-scoped repository lookup, provider external ID uniqueness, sync status, updated time. | Repository list, sync processing, duplicate prevention. |
| Repository Snapshot | Repository plus captured time, snapshot status, source revision. | Historical analysis and latest snapshot lookup. |
| Evaluation | User plus completed time, snapshot reference, rule-set version, status. | Dashboard, reproducibility, evaluation history. |
| Skill Matrix | User plus generated time, evaluation reference, current matrix marker. | Current skill view and growth timeline. |
| Career/Company | Active version lookup and profile version references. | Assessment execution and historical traceability. |
| Recommendation | User plus status, recommendation set, priority. | Active recommendations and dashboard cards. |
| Roadmap | User plus status, roadmap step status. | Progress tracking. |
| Knowledge | User plus source type, freshness, active version, metadata filters. | Retrieval eligibility and search filtering. |
| Prompt | Template category/version, user plus task type, prompt execution status. | Prompt selection and execution history. |
| AI/Artifact | User plus task type/status, artifact type/status, publication state. | Artifact history and generation monitoring. |
| Audit | Actor, resource reference, event time, privacy class. | Compliance investigation and retention. |

### 7.3 Vector Index Strategy

| Vector Index Area | Strategy |
|---|---|
| User Isolation | Every vector search must include user or tenant ownership filter. |
| Metadata Filtering | Index metadata must support source type, repository, document category, career, company, freshness, and privacy class filters. |
| Embedding Version | Retrieval must filter or route by compatible embedding model version. |
| Hybrid Search Readiness | Metadata and keyword search may be combined with vector similarity. |
| Rebuild Strategy | Index rebuild must be possible from PostgreSQL metadata and Object Storage content references. |

### 7.4 Index Governance Rules

| Rule ID | Rule |
|---|---|
| DB-IDX-001 | Indexes must support user-scoped access patterns first. |
| DB-IDX-002 | Historical queries require time-oriented indexes on snapshot, evaluation, and audit records. |
| DB-IDX-003 | Unique indexes must enforce external identity only within provider and owner scope. |
| DB-IDX-004 | Vector indexes must never bypass relational permission checks. |
| DB-IDX-005 | Indexes for generated artifacts must support artifact type, owner, status, and publication state. |

## 8. Partition Strategy

### 8.1 Partition Candidates

| Data Area | Partition Key Candidate | Reason | Notes |
|---|---|---|---|
| Repository Snapshots | User scope, repository, captured time | High volume and historical growth. | Time-based and repository-scoped access both matter. |
| Repository Activity Facts | Snapshot and captured time | Large append-only data. | Partitioning should align with snapshot retention. |
| Analysis History | User scope and completed time | Historical queries and retention. | Supports growth graphs and audit. |
| Evaluation Results | Completed time and user scope | Append-only evaluation history. | Keep source version references intact. |
| Audit Log | Event time and privacy class | High-volume compliance records. | Retention and legal hold may differ by class. |
| Knowledge Documents | User scope and source type | User isolation and source-specific lifecycle. | Document versions may grow independently. |
| Knowledge Chunks | Document version and user scope | Chunk volume and deletion propagation. | Align with vector index metadata. |
| AI Task History | User scope and requested time | User history and operational monitoring. | Supports retry and failure analysis. |
| Generated Artifacts | User scope, artifact type, publication time | Artifact history and export management. | Object content remains externally referenced. |

### 8.2 Partition Rules

| Rule ID | Rule |
|---|---|
| DB-PRT-001 | Partitioning must preserve user isolation and owner-scoped access. |
| DB-PRT-002 | Append-only historical records should be partitioned by time where volume justifies it. |
| DB-PRT-003 | Deletion and retention workflows must be able to locate all user-owned partitioned records. |
| DB-PRT-004 | Partition strategy must not change domain identity or version semantics. |
| DB-PRT-005 | Vector index partitioning or collection strategy must preserve privacy filters and embedding version compatibility. |

## 9. Versioning Strategy

### 9.1 Versioned Table Groups

| Versioned Area | Stable Table | Version Table | Version Rule |
|---|---|---|---|
| Rule Version | rule_sets | rule_set_versions | Evaluations reference exact rule-set version. |
| Career Profile Version | careers | career_profile_versions | Career readiness references exact profile version. |
| Company Version | companies | company_profile_versions | Company readiness references exact profile version. |
| Knowledge Version | knowledge_documents | knowledge_document_versions | Chunks reference exact document version. |
| Prompt Version | prompt_templates | prompt_template_versions | PromptContext references exact template version. |
| Repository Snapshot | repositories | repository_snapshots | Evaluation references exact snapshot. |
| Portfolio Version | portfolios | portfolio_versions | Published versions immutable. |
| Resume Version | resumes | resume_versions | Published versions immutable. |

### 9.2 Current and Historical Version Handling

| Concept | Current Version Handling | Historical Version Handling |
|---|---|---|
| RuleSet | Current active version pointer supports new evaluations. | Historical versions remain readable for completed evaluations. |
| CareerProfile | Active version pointer supports new career assessments. | Historical versions remain readable for readiness history. |
| CompanyProfile | Active version pointer supports new company assessments. | Historical versions remain readable for company readiness history. |
| KnowledgeDocument | Active version pointer supports latest retrieval. | Prior versions may be retained, archived, or deleted by policy. |
| PromptTemplate | Active version pointer supports new prompt contexts. | Historical prompt executions retain exact template version. |
| Portfolio | Active version pointer supports current portfolio. | Published versions remain immutable. |
| Resume | Active version pointer supports current resume. | Published versions remain immutable. |

### 9.3 Migration and Compatibility

| Rule ID | Rule |
|---|---|
| DB-VER-001 | Version migrations must create new versions or migration records, not rewrite historical records. |
| DB-VER-002 | A version cannot become active until compatibility validation passes. |
| DB-VER-003 | Completed results must reference exact source version, never a moving "latest" concept. |
| DB-VER-004 | Deprecated versions must remain available while referenced by historical records. |
| DB-VER-005 | Recalculation must create new evaluations and assessments rather than mutating previous outputs. |
| DB-VER-006 | Flyway versioned SQL migrations are immutable after application and use checksum validation. |
| DB-VER-007 | Applied migration correction requires a new migration; Flyway repair is exceptional, approved, and audited. |
| DB-VER-008 | Out-of-order execution and automatic production baseline are disabled by default. |
| DB-VER-009 | Destructive schema changes use expand-and-contract, compatibility evidence, and an explicit recovery plan. |
| DB-VER-010 | Production migrations execute as a privileged deployment step; application startup validates compatible schema without silently mutating it. |

Flyway migration files use `V<version>__<lower_snake_case_description>.sql`. Repeatable migrations are limited to approved replaceable database objects or reference views. Production rollback is forward-fix by default; application rollback is allowed only while schema compatibility is preserved.

## 10. Audit Strategy

### 10.1 Audit Tables

| Audit Table | Purpose | Typical Events |
|---|---|---|
| audit_records | Append-only business and security audit log. | Login, account connection, permission change, evaluation completed, artifact published. |
| configuration_changes | Administrative configuration history. | Rule activation, career profile activation, company profile activation, prompt template activation. |
| deletion_requests | User deletion and data removal workflow trace. | UserDeletionRequested, deletion completed, deletion failed. |
| access_audit_records | Access-sensitive read/write trace where required by policy. | Private repository access, Notion document retrieval, artifact export. |

### 10.2 History Strategy

| Data Type | History Strategy |
|---|---|
| Immutable snapshots | Retain as historical records until retention deletion. |
| Evaluations | Append-only; new evaluation supersedes but does not rewrite previous evaluation. |
| Skill matrices | Append-only; latest matrix may be marked current by reference. |
| Recommendations | Preserve generated priority and reason; status changes tracked. |
| Prompt executions | Preserve prompt context source references and execution metadata. |
| Generated artifacts | Preserve generated and user-edited version provenance. |
| Configuration | Preserve all activated/deprecated versions and administrative changes. |

### 10.3 Soft Delete

| Rule ID | Rule |
|---|---|
| DB-AUD-001 | User-visible deletion may use soft delete while retention workflow completes. |
| DB-AUD-002 | Soft-deleted private data must be excluded from normal reads, retrieval, prompts, and AI generation. |
| DB-AUD-003 | Deletion tombstones may remain only when required for audit, deduplication, or compliance. |
| DB-AUD-004 | Sensitive content must be physically removed or redacted according to privacy policy. |

### 10.4 Retention and Change Tracking

| Area | Retention Consideration |
|---|---|
| Credentials | Never retained as ordinary data; revocation and secure deletion required. |
| Provider credentials | Encrypted at application level with external key material; token values are adapter-restricted and removed or revoked on disconnect/deletion. |
| Application sessions | Expire by idle/absolute policy and are invalidated on logout, suspension, compromise, and account deletion; session security events are audited separately. |
| Private repository data | Retained only while user permission and retention policy allow. |
| Knowledge chunks and embeddings | Deleted or invalidated when source is deleted or permission revoked. |
| Audit records | Retained according to compliance policy with private content minimization. |
| Generated artifacts | Retained by user preference, publication state, and export history. |
| Historical evaluations | Retained for reproducibility unless privacy deletion requires removal or redaction. |

## 11. Performance Considerations

### 11.1 Query Optimization

| Concern | Design Approach |
|---|---|
| User dashboard loading | Use user-scoped projections and cached summary views derived from authoritative tables. |
| Repository history | Query by repository, user, and captured time. |
| Evaluation history | Query by user, evaluation status, completed time, and snapshot reference. |
| Skill growth | Precompute or project timeline from SkillMatrix history. |
| Recommendation cards | Query active recommendation status and priority by user. |
| Artifact history | Query by user, artifact type, status, and publication time. |

### 11.2 Caching Strategy

| Cache Type | Storage | Rule |
|---|---|---|
| Application session | PostgreSQL JDBC session store for MVP | Rebuildable through authentication flow; local memory is allowed only for single-instance development. |
| Dashboard cache | Redis | Invalidated by evaluation, recommendation, roadmap, and artifact events. |
| Sync progress cache | Redis | Mirrors authoritative sync job state for responsiveness. |
| Prompt candidate cache | Redis | Temporary only; locked PromptContext is persisted authoritatively. |
| Retrieval cache | Redis | User-scoped, short-lived, invalidated by knowledge freshness changes. |
| Rate-limit cache | Redis | Provider-scoped and user-scoped operational counters. |

### 11.3 Read-heavy Workloads

Read-heavy workloads include dashboards, repository lists, artifact history, knowledge source summaries, and active recommendations. These should use projections, scoped indexes, and cache while preserving authoritative relational writes.

### 11.4 Write-heavy Workloads

Write-heavy workloads include repository synchronization, snapshot creation, knowledge chunking, embedding metadata generation, audit records, and AI execution logs. These should use append-only writes, idempotency keys, batch-friendly processing, and partitioning where needed.

### 11.5 Historical Queries

Historical queries must preserve:

- Snapshot identity.
- RuleSetVersion.
- CareerProfileVersion.
- CompanyProfileVersion.
- PromptTemplateVersion.
- KnowledgeDocumentVersion.
- Artifact version.

Historical query optimization should prioritize time, user scope, and source reference.

### 11.6 Vector Search Performance

| Concern | Design Approach |
|---|---|
| Permission filtering | Apply user/tenant filter as mandatory retrieval metadata. |
| Freshness | Exclude deleted or stale chunks unless task explicitly allows stale context. |
| Embedding version | Route or filter by compatible embedding model version. |
| Hybrid retrieval | Combine semantic similarity with source type, career, company, repository, and freshness filters. |
| Rebuild | Rebuild vector indexes from KnowledgeDocumentVersion and KnowledgeChunk metadata. |

## 12. Data Integrity

### 12.1 Foreign Key Strategy

| Strategy | Application |
|---|---|
| Strong foreign keys | Use for same-schema ownership relationships such as user to external accounts, repository to snapshots, knowledge document to versions. |
| Cross-schema references | Use stable IDs with integrity enforced by application/domain services where strict database coupling would harm bounded context independence. |
| Historical references | Preserve referenced version or snapshot IDs even when current state changes. |
| Optional references | Company readiness, generated artifact links, and external source links may be optional where business workflow allows. |

### 12.2 Unique Constraints

| Constraint Area | Rule |
|---|---|
| User identity | Login identity must be unique under supported authentication policy. |
| External account | Provider external account reference must be unique within provider/user policy. |
| Repository external identity | Provider repository identity must be unique within user and provider scope. |
| Active versions | Only one active version should exist per rule set, career, company, or prompt template scope. |
| Snapshot source revision | Duplicate snapshots for same repository and source revision should be prevented by idempotency policy. |
| Knowledge document version | Same document and content hash should not create duplicate active versions. |

### 12.3 Check Constraints

| Constraint Area | Rule |
|---|---|
| Score range | Score values must remain within configured scale. |
| Confidence range | Confidence values must remain within configured range. |
| Weight range | Weights must be non-negative and validated as a set. |
| Status values | Lifecycle status must be one of allowed states for each table. |
| Token budget | Token budgets must be positive and within selected model constraints. |
| Priority | Recommendation priority must use supported deterministic priority scale. |

### 12.4 Business Constraints

| Constraint ID | Constraint |
|---|---|
| DB-INT-001 | Evaluation cannot complete unless it references RepositorySnapshot and RuleSetVersion. |
| DB-INT-002 | SkillMatrix cannot publish unless linked to completed Evaluation. |
| DB-INT-003 | CareerReadiness cannot complete unless linked to SkillMatrix and CareerProfileVersion. |
| DB-INT-004 | CompanyReadiness cannot complete unless linked to SkillMatrix and CompanyProfileVersion. |
| DB-INT-005 | Recommendation cannot publish without deterministic reason and evidence or skill gap reference. |
| DB-INT-006 | PromptContext cannot lock unless template version and required source references are valid. |
| DB-INT-007 | AIResponse cannot create GeneratedArtifact unless validation passes. |
| DB-INT-008 | Published PortfolioVersion and ResumeVersion cannot be mutated. |

### 12.5 Referential Integrity

Referential integrity must protect owner scope, source provenance, version reproducibility, and deletion propagation. Where physical foreign keys are not used because of scale, partitioning, or bounded context separation, equivalent integrity must be enforced by domain services, validation jobs, and audit checks.

## 13. Backup & Recovery

### 13.1 Backup Strategy

| Storage | Backup Requirement |
|---|---|
| PostgreSQL | Regular full and incremental backups for authoritative business data and history. |
| Redis | No long-term backup requirement for ordinary cache; initial authentication does not depend on Redis. |
| Vector Database | Rebuildable from KnowledgeChunk and EmbeddingMetadata where possible; snapshots may be used for faster recovery. |
| Object Storage | Versioned or retained backups for source documents, archives, generated exports, and artifact content. |

### 13.2 Restore Strategy

| Restore Scenario | Strategy |
|---|---|
| Single user data recovery | Restore user-owned canonical records and referenced object content consistently. |
| Evaluation history recovery | Restore Evaluation, RuleSetVersion references, Evidence, SkillMatrix, and related snapshots. |
| Knowledge index recovery | Rebuild vector index from KnowledgeDocumentVersion and KnowledgeChunk metadata when vector backup is unavailable. |
| Artifact recovery | Restore metadata from PostgreSQL and content from Object Storage. |
| Audit recovery | Restore append-only audit records with ordering and integrity preserved. |

### 13.3 Disaster Recovery

Disaster recovery must prioritize:

1. Identity and ownership data.
2. Authoritative canonical data in PostgreSQL.
3. Object Storage content referenced by canonical metadata.
4. Vector indexes, rebuilt if necessary.
5. Redis ephemeral state, recreated where possible.

### 13.4 Point-in-Time Recovery

Point-in-time recovery must preserve consistency between:

- RepositorySnapshot and Evaluation.
- Evaluation and SkillMatrix.
- SkillMatrix and CareerReadiness.
- Recommendation and LearningRoadmap.
- PromptContext and AIResponse.
- GeneratedArtifact and Portfolio/Resume versions.

### 13.5 Retention Policy

| Data Class | Retention Direction |
|---|---|
| User private data | Retained until deletion request or policy expiry. |
| Repository private data | Retained while permission and user policy allow. |
| Historical evaluations | Retained for reproducibility unless deletion/redaction is required. |
| Generated artifacts | Retained by user preference, publication state, and export history. |
| Audit data | Retained according to compliance requirements with content minimization. |
| Temporary cache | Expired automatically. |

## 14. Scalability

### 14.1 Horizontal Scaling

| Area | Scaling Approach |
|---|---|
| Application reads | Use read replicas and projections for dashboards and history. |
| Repository synchronization | Use queue-based processing with idempotent writes. |
| Knowledge ingestion | Use batch processing for chunking and embedding metadata creation. |
| AI execution history | Append execution records and offload large content to Object Storage. |
| Audit records | Append-only writes with partitioning by time. |

### 14.2 Read Replicas

Read replicas are suitable for:

- Dashboard queries.
- Repository browsing.
- Historical evaluation views.
- Artifact history.
- Administration read views.
- Search metadata queries.

Read replicas must not be used for operations requiring immediate authorization decisions unless replication lag is explicitly handled.

### 14.3 Partition Growth

Partitioning should be introduced for high-growth append-only areas:

- Repository snapshots.
- Repository activity facts.
- Evaluations.
- Knowledge chunks.
- AI model executions.
- Audit records.
- Generated artifacts.

### 14.4 Storage Expansion

| Storage | Expansion Concern |
|---|---|
| PostgreSQL | Historical records, audit volume, evaluation history, repository metadata volume. |
| Redis | Cache size, rate-limit counters, temporary workflow state, and optional future session volume only after review. |
| Vector Database | Chunk count, embedding dimensions, embedding model versions, metadata filters. |
| Object Storage | Repository archives, Notion content copies, generated PDFs, resume exports, images. |

### 14.5 Future Multi-tenancy

Future enterprise and organization features require explicit tenant identity, tenant ownership, organization membership, and cross-user sharing rules. User isolation remains the default. Multi-tenancy must not allow organization-level analytics to expose individual private repository data without explicit permission.

## 15. Traceability

### 15.1 Table to Domain Model Mapping

| Table Group | Domain Model Mapping |
|---|---|
| users, external_accounts, user_consents, user_settings | User Aggregate, Identity Context |
| repositories, repository_snapshots, repository activity tables | Repository Aggregate, RepositorySnapshot Aggregate |
| rule_sets, rule_set_versions, rules, evaluations, evidence records | RuleSet Aggregate, Evaluation Aggregate |
| skill_matrices, skill_assessments, skill_evidence_links | SkillMatrix Aggregate |
| careers, career_profile_versions, career_readiness_assessments, skill_gaps | Career Aggregate, CareerAssessment Aggregate |
| companies, company_profile_versions, company_readiness_assessments | Company Aggregate, CompanyAssessment Aggregate |
| recommendation_sets, recommendations, recommendation_evidence_links | Recommendation Aggregate |
| learning_roadmaps, roadmap_steps, roadmap_milestones, learning_resources | LearningRoadmap Aggregate |
| knowledge_documents, knowledge_document_versions, knowledge_chunks, embedding_records | KnowledgeDocument Aggregate |
| prompt_templates, prompt_template_versions, prompt_contexts, prompt_executions | PromptTemplate Aggregate, PromptExecution Aggregate |
| ai_tasks, model_executions, ai_responses, response_validation_results, generated_artifacts | AITask Aggregate, GeneratedArtifact Aggregate |
| portfolios, portfolio_versions, resumes, resume_versions, interview_question_sets | Portfolio Aggregate, Resume Aggregate, InterviewQuestionSet Aggregate |
| configuration_changes, audit_records, deletion_requests | Administration Context and Audit capability |

### 15.2 Table to System Data Model Mapping

| System Data Model Area | Database Design Mapping |
|---|---|
| Canonical Data Model | Major table definitions in chapter 5 |
| Data Ownership | Logical schemas and ownership rules in chapter 4 |
| Data Lifecycle | Table lifecycle and audit/history strategy |
| Snapshot Strategy | repository_snapshots, project snapshots, prompt_contexts, portfolio/resume versions |
| Version Strategy | rule_set_versions, career_profile_versions, company_profile_versions, prompt_template_versions, knowledge_document_versions, artifact versions |
| Synchronization | Repository sync, Notion/knowledge sync, checkpointed workflow metadata |
| Storage Responsibility | PostgreSQL, Redis, Vector Database, Object Storage mapping |
| Data Consistency | Integrity, constraints, partitioning, and version references |

### 15.3 Table to SRS Mapping

| SRS Range | Table Groups |
|---|---|
| FR-001~FR-020 | Identity tables, user preference tables, audit records |
| FR-021~FR-050 | GitHub account, repository, repository snapshot, repository activity, repository document tables |
| FR-051~FR-070 | Notion workspace metadata, knowledge document, project document-related tables |
| FR-071~FR-100 | Synchronization metadata, repository snapshots, normalized analysis/evaluation preparation records |
| FR-101~FR-180 | Rule, evaluation, evidence, score, Skill Matrix tables |
| FR-181~FR-220 | Career, company, readiness, skill gap, recommendation, roadmap tables |
| FR-221~FR-280 | Prompt, AI task, AI response, generated artifact, portfolio, resume, interview tables |
| FR-281~FR-320 | Projection-supporting indexes and dashboard read models derived from authoritative tables |
| FR-321~FR-340 | Knowledge, retrieval, vector index metadata tables |
| FR-341~FR-360 | Administration, configuration, audit, statistics tables |

### 15.4 Final Consistency Statement

This database design preserves the DevPath architectural boundary:

- Rule Engine owns score persistence.
- Career and Company contexts own readiness persistence.
- Recommendation Context owns deterministic priority persistence.
- Knowledge Context owns long-term memory metadata and retrieval indexes.
- Prompt Context owns prompt template and prompt execution persistence.
- AI Context owns generated responses and generated artifact metadata.
- Portfolio Context owns reviewed and published career artifacts.

No storage component grants the LLM authority to calculate scores, execute business rules, mutate official results, or become the source of long-term user memory.

## 16. Identity Foundation Implementation Evidence

| Item | Implemented Evidence | Verification Status |
|---|---|---|
| Migration | `backend/src/main/resources/db/migration/V1__create_identity_and_session_schema.sql` | Created; execution not run because Java 21 is unavailable |
| Identity tables | `users`, `external_identities` | Defined by Flyway migration |
| Session tables | `spring_session`, `spring_session_attributes` | Defined by Flyway migration; startup auto-creation disabled |
| Identity uniqueness | `UNIQUE (provider, provider_subject)` | Enforced in migration and represented in JPA metadata |
| Persistence model | `identity/adapter/out/persistence` | Separate JPA entities and explicit domain mappings |
| Schema ownership | Flyway migration with `ddl-auto=validate` and OSIV disabled | Configuration created; runtime validation not run |

Provider credentials are not part of this migration. Initial authentication does not require durable provider-token storage, and the OAuth authorized-client repository is intentionally non-persisting.
