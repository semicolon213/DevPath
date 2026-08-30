ALTER TABLE audit_records
    ADD COLUMN details JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE TABLE retrieval_requests (
    retrieval_request_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    query_hash VARCHAR(64) NOT NULL,
    context_purpose VARCHAR(32) NOT NULL,
    source_types VARCHAR(128) NOT NULL,
    document_filter_count INTEGER NOT NULL,
    requested_limit INTEGER NOT NULL,
    policy_version VARCHAR(64) NOT NULL,
    request_status VARCHAR(16) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_ms BIGINT NOT NULL,
    CONSTRAINT retrieval_requests_id_user_uk UNIQUE (retrieval_request_id, user_id),
    CONSTRAINT retrieval_requests_query_hash_check CHECK (query_hash ~ '^[a-f0-9]{64}$'),
    CONSTRAINT retrieval_requests_purpose_check CHECK (context_purpose IN ('USER_SEARCH')),
    CONSTRAINT retrieval_requests_document_count_check CHECK (document_filter_count BETWEEN 0 AND 20),
    CONSTRAINT retrieval_requests_limit_check CHECK (requested_limit BETWEEN 1 AND 20),
    CONSTRAINT retrieval_requests_status_check CHECK (request_status IN ('COMPLETED')),
    CONSTRAINT retrieval_requests_duration_check CHECK (duration_ms >= 0)
);

CREATE INDEX retrieval_requests_owner_requested_idx
    ON retrieval_requests (user_id, requested_at DESC);

CREATE TABLE retrieval_results (
    retrieval_result_id UUID PRIMARY KEY,
    retrieval_request_id UUID NOT NULL,
    user_id UUID NOT NULL,
    result_count INTEGER NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT retrieval_results_id_user_uk UNIQUE (retrieval_result_id, user_id),
    CONSTRAINT retrieval_results_request_uk UNIQUE (retrieval_request_id),
    CONSTRAINT retrieval_results_request_owner_fk FOREIGN KEY (retrieval_request_id, user_id)
        REFERENCES retrieval_requests (retrieval_request_id, user_id) ON DELETE CASCADE,
    CONSTRAINT retrieval_results_count_check CHECK (result_count BETWEEN 0 AND 20)
);

CREATE TABLE retrieval_result_items (
    retrieval_result_item_id UUID PRIMARY KEY,
    retrieval_result_id UUID NOT NULL,
    user_id UUID NOT NULL,
    result_position INTEGER NOT NULL,
    knowledge_chunk_id UUID NOT NULL,
    relevance NUMERIC(8, 7) NOT NULL,
    CONSTRAINT retrieval_result_items_result_position_uk UNIQUE (retrieval_result_id, result_position),
    CONSTRAINT retrieval_result_items_result_owner_fk FOREIGN KEY (retrieval_result_id, user_id)
        REFERENCES retrieval_results (retrieval_result_id, user_id) ON DELETE CASCADE,
    CONSTRAINT retrieval_result_items_chunk_owner_fk FOREIGN KEY (knowledge_chunk_id, user_id)
        REFERENCES knowledge_chunks (knowledge_chunk_id, user_id),
    CONSTRAINT retrieval_result_items_position_check CHECK (result_position BETWEEN 0 AND 19),
    CONSTRAINT retrieval_result_items_relevance_check CHECK (relevance BETWEEN 0 AND 1)
);

CREATE INDEX retrieval_result_items_owner_result_idx
    ON retrieval_result_items (user_id, retrieval_result_id, result_position);
