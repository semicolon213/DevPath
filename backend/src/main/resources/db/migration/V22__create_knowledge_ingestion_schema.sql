CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE notion_workspace_connections
    ADD CONSTRAINT notion_workspace_connections_id_user_uk UNIQUE (notion_connection_id, user_id);

CREATE TABLE knowledge_documents (
    knowledge_document_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    source_type VARCHAR(32) NOT NULL,
    source_connection_id UUID NOT NULL,
    source_object_id VARCHAR(255) NOT NULL,
    title VARCHAR(512) NOT NULL,
    lifecycle_status VARCHAR(32) NOT NULL,
    current_version_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT knowledge_documents_id_user_uk UNIQUE (knowledge_document_id, user_id),
    CONSTRAINT knowledge_documents_source_uk UNIQUE (user_id, source_type, source_object_id),
    CONSTRAINT knowledge_documents_connection_owner_fk FOREIGN KEY (source_connection_id, user_id)
        REFERENCES notion_workspace_connections (notion_connection_id, user_id),
    CONSTRAINT knowledge_documents_source_type_check CHECK (source_type IN ('NOTION')),
    CONSTRAINT knowledge_documents_status_check CHECK (lifecycle_status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX knowledge_documents_owner_updated_idx
    ON knowledge_documents (user_id, updated_at DESC);

CREATE TABLE knowledge_document_versions (
    knowledge_document_version_id UUID PRIMARY KEY,
    knowledge_document_id UUID NOT NULL,
    user_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    object_reference VARCHAR(512) NOT NULL,
    source_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT knowledge_document_versions_id_user_uk
        UNIQUE (knowledge_document_version_id, user_id),
    CONSTRAINT knowledge_document_versions_id_document_user_uk
        UNIQUE (knowledge_document_version_id, knowledge_document_id, user_id),
    CONSTRAINT knowledge_document_versions_number_uk UNIQUE (knowledge_document_id, version_number),
    CONSTRAINT knowledge_document_versions_document_owner_fk FOREIGN KEY (knowledge_document_id, user_id)
        REFERENCES knowledge_documents (knowledge_document_id, user_id) ON DELETE CASCADE,
    CONSTRAINT knowledge_document_versions_status_check CHECK (version_status IN ('INDEXED', 'SUPERSEDED', 'DELETED'))
);

ALTER TABLE knowledge_documents
    ADD CONSTRAINT knowledge_documents_current_version_fk
    FOREIGN KEY (current_version_id, knowledge_document_id, user_id)
    REFERENCES knowledge_document_versions (knowledge_document_version_id, knowledge_document_id, user_id);

CREATE TABLE knowledge_chunks (
    knowledge_chunk_id UUID PRIMARY KEY,
    knowledge_document_version_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    chunk_position INTEGER NOT NULL,
    heading VARCHAR(512),
    object_reference VARCHAR(512) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    token_estimate INTEGER NOT NULL,
    index_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT knowledge_chunks_id_user_uk UNIQUE (knowledge_chunk_id, user_id),
    CONSTRAINT knowledge_chunks_position_uk UNIQUE (knowledge_document_version_id, chunk_position),
    CONSTRAINT knowledge_chunks_version_owner_fk FOREIGN KEY (knowledge_document_version_id, user_id)
        REFERENCES knowledge_document_versions (knowledge_document_version_id, user_id) ON DELETE CASCADE,
    CONSTRAINT knowledge_chunks_status_check CHECK (index_status IN ('INDEXED', 'STALE', 'DELETED'))
);

CREATE INDEX knowledge_chunks_owner_version_idx
    ON knowledge_chunks (user_id, knowledge_document_version_id, chunk_position);

CREATE TABLE embedding_records (
    embedding_record_id UUID PRIMARY KEY,
    knowledge_chunk_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(128) NOT NULL,
    model_version VARCHAR(128) NOT NULL,
    dimension INTEGER NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding vector(768) NOT NULL,
    embedding_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT embedding_records_chunk_owner_fk FOREIGN KEY (knowledge_chunk_id, user_id)
        REFERENCES knowledge_chunks (knowledge_chunk_id, user_id) ON DELETE CASCADE,
    CONSTRAINT embedding_records_chunk_model_uk UNIQUE (knowledge_chunk_id, provider, model_version),
    CONSTRAINT embedding_records_dimension_check CHECK (dimension = 768),
    CONSTRAINT embedding_records_status_check CHECK (embedding_status IN ('ACTIVE', 'STALE', 'DELETED'))
);

CREATE INDEX embedding_records_owner_model_idx
    ON embedding_records (user_id, model_version, embedding_status);

CREATE INDEX embedding_records_vector_hnsw_idx
    ON embedding_records USING hnsw (embedding vector_cosine_ops);

CREATE TABLE knowledge_ingestion_jobs (
    knowledge_ingestion_job_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    source_connection_id UUID NOT NULL,
    source_object_id VARCHAR(255) NOT NULL,
    knowledge_document_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    phase VARCHAR(32) NOT NULL,
    progress_percent INTEGER NOT NULL,
    attempt_count INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT knowledge_ingestion_jobs_connection_owner_fk FOREIGN KEY (source_connection_id, user_id)
        REFERENCES notion_workspace_connections (notion_connection_id, user_id),
    CONSTRAINT knowledge_ingestion_jobs_owner_key_uk UNIQUE (user_id, idempotency_key),
    CONSTRAINT knowledge_ingestion_jobs_status_check CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT knowledge_ingestion_jobs_progress_check CHECK (progress_percent BETWEEN 0 AND 100),
    CONSTRAINT knowledge_ingestion_jobs_attempt_check CHECK (attempt_count BETWEEN 0 AND max_attempts AND max_attempts > 0)
);

CREATE INDEX knowledge_ingestion_jobs_claim_idx
    ON knowledge_ingestion_jobs (status, next_attempt_at, submitted_at);
CREATE UNIQUE INDEX knowledge_ingestion_jobs_active_source_uk
    ON knowledge_ingestion_jobs (user_id, source_object_id)
    WHERE status IN ('QUEUED', 'RUNNING');
