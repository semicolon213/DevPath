CREATE TABLE repository_language_statistics (
    language_record_id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    provider_label VARCHAR(128) NOT NULL,
    canonical_name VARCHAR(128) NOT NULL,
    byte_count BIGINT NOT NULL,
    percentage NUMERIC(7,4) NOT NULL,
    taxonomy_status VARCHAR(16) NOT NULL,
    taxonomy_version VARCHAR(32) NOT NULL,
    extractor_version VARCHAR(32) NOT NULL,
    CONSTRAINT repository_language_statistics_snapshot_fk
        FOREIGN KEY (snapshot_id) REFERENCES repository_snapshots (snapshot_id) ON DELETE CASCADE,
    CONSTRAINT repository_language_statistics_snapshot_label_uk UNIQUE (snapshot_id, provider_label),
    CONSTRAINT repository_language_statistics_bytes_check CHECK (byte_count >= 0),
    CONSTRAINT repository_language_statistics_percentage_check CHECK (percentage BETWEEN 0 AND 100),
    CONSTRAINT repository_language_statistics_status_check CHECK (taxonomy_status IN ('SUPPORTED', 'UNSUPPORTED'))
);

CREATE INDEX repository_language_statistics_snapshot_percentage_idx
    ON repository_language_statistics (snapshot_id, percentage DESC);
