package com.devpath.test;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class PgVectorPostgreSQLContainer extends PostgreSQLContainer<PgVectorPostgreSQLContainer> {
    private static final DockerImageName IMAGE = DockerImageName.parse("pgvector/pgvector:0.8.0-pg16")
        .asCompatibleSubstituteFor("postgres");

    public PgVectorPostgreSQLContainer() {
        super(IMAGE);
    }
}
