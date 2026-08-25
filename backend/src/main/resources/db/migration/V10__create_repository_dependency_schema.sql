CREATE TABLE repository_dependencies (
    dependency_record_id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    ecosystem VARCHAR(32) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    version_constraint VARCHAR(255),
    dependency_scope VARCHAR(32) NOT NULL,
    manifest_path VARCHAR(500) NOT NULL,
    extractor_version VARCHAR(32) NOT NULL,
    CONSTRAINT repository_dependencies_snapshot_fk
        FOREIGN KEY (snapshot_id) REFERENCES repository_snapshots (snapshot_id) ON DELETE CASCADE,
    CONSTRAINT repository_dependencies_snapshot_fact_uk
        UNIQUE (snapshot_id, ecosystem, package_name, dependency_scope, manifest_path),
    CONSTRAINT repository_dependencies_scope_check
        CHECK (dependency_scope IN ('RUNTIME', 'DEVELOPMENT', 'TEST', 'PLUGIN', 'UNKNOWN'))
);

CREATE INDEX repository_dependencies_snapshot_package_idx
    ON repository_dependencies (snapshot_id, ecosystem, package_name);
