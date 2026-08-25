CREATE TABLE repository_pull_requests (
    pull_request_record_id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    provider_pull_request_id VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    merged_at TIMESTAMP WITH TIME ZONE,
    review_count INTEGER NOT NULL,
    CONSTRAINT repository_pull_requests_snapshot_fk
        FOREIGN KEY (snapshot_id) REFERENCES repository_snapshots (snapshot_id) ON DELETE CASCADE,
    CONSTRAINT repository_pull_requests_snapshot_provider_uk UNIQUE (snapshot_id, provider_pull_request_id),
    CONSTRAINT repository_pull_requests_status_check CHECK (status IN ('OPEN', 'CLOSED', 'MERGED')),
    CONSTRAINT repository_pull_requests_review_count_check CHECK (review_count >= 0),
    CONSTRAINT repository_pull_requests_merged_check CHECK (status <> 'MERGED' OR merged_at IS NOT NULL)
);

CREATE INDEX repository_pull_requests_snapshot_time_idx
    ON repository_pull_requests (snapshot_id, opened_at DESC);

CREATE TABLE repository_issues (
    issue_record_id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    provider_issue_id VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    labels_text TEXT NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT repository_issues_snapshot_fk
        FOREIGN KEY (snapshot_id) REFERENCES repository_snapshots (snapshot_id) ON DELETE CASCADE,
    CONSTRAINT repository_issues_snapshot_provider_uk UNIQUE (snapshot_id, provider_issue_id),
    CONSTRAINT repository_issues_status_check CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE INDEX repository_issues_snapshot_time_idx
    ON repository_issues (snapshot_id, opened_at DESC);

CREATE TABLE repository_documents (
    repository_document_id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    document_type VARCHAR(32) NOT NULL,
    normalized_path VARCHAR(1000) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    byte_size BIGINT NOT NULL,
    quality_signals VARCHAR(255) NOT NULL,
    object_content_reference VARCHAR(1000),
    CONSTRAINT repository_documents_snapshot_fk
        FOREIGN KEY (snapshot_id) REFERENCES repository_snapshots (snapshot_id) ON DELETE CASCADE,
    CONSTRAINT repository_documents_snapshot_type_path_uk UNIQUE (snapshot_id, document_type, normalized_path),
    CONSTRAINT repository_documents_type_check CHECK (document_type IN ('README')),
    CONSTRAINT repository_documents_hash_check CHECK (content_hash ~ '^[a-f0-9]{64}$'),
    CONSTRAINT repository_documents_size_check CHECK (byte_size >= 0)
);

CREATE INDEX repository_documents_snapshot_type_idx
    ON repository_documents (snapshot_id, document_type);
