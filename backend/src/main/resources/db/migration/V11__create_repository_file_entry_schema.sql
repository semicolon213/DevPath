CREATE TABLE repository_file_entries (
    file_entry_id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    normalized_path VARCHAR(1000) NOT NULL,
    blob_sha VARCHAR(64) NOT NULL,
    byte_size BIGINT NOT NULL,
    extractor_version VARCHAR(32) NOT NULL,
    CONSTRAINT repository_file_entries_snapshot_fk
        FOREIGN KEY (snapshot_id) REFERENCES repository_snapshots (snapshot_id) ON DELETE CASCADE,
    CONSTRAINT repository_file_entries_snapshot_path_uk UNIQUE (snapshot_id, normalized_path),
    CONSTRAINT repository_file_entries_size_check CHECK (byte_size >= 0)
);

CREATE INDEX repository_file_entries_snapshot_path_idx
    ON repository_file_entries (snapshot_id, normalized_path);
