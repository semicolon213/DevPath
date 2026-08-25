package com.devpath.repository.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositoryEvidenceExtractorTest {
    @Test
    void extractsTraceableEngineeringSignalsWithoutScores() {
        String revision = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        var files = List.of(
            file("backend/src/main/java/example/domain/User.java"),
            file("backend/src/main/java/example/application/UserService.java"),
            file("backend/src/main/java/example/adapter/UserController.java"),
            file("backend/src/test/java/example/UserTest.java"),
            file(".github/workflows/verify.yml"), file("Dockerfile"),
            file("README.md"), file("docs/architecture.md"), file("contracts/openapi.yaml"),
            file("backend/src/main/resources/db/migration/V1__schema.sql"), file("prisma/schema.prisma")
        );
        var dependencies = List.of(
            RepositoryDependency.normalized("gradle", "org.junit.jupiter:junit-jupiter", "5.11", "TEST", "backend/build.gradle"),
            RepositoryDependency.normalized("gradle", "org.postgresql:postgresql", "42.7", "RUNTIME", "backend/build.gradle"),
            RepositoryDependency.normalized("gradle", "org.springframework.boot:spring-boot-starter-data-jpa", "3.3", "RUNTIME", "backend/build.gradle")
        );
        RepositorySnapshot snapshot = RepositorySnapshot.ready(
            UUID.randomUUID(), UUID.randomUUID(), revision, Instant.parse("2026-08-11T00:00:00Z"),
            List.of(new RepositoryBranch("main", revision, true)),
            List.of(new RepositoryCommit(revision, "owner", Instant.parse("2026-08-10T00:00:00Z"), "test")),
            List.of(), dependencies, files
        );

        var categories = RepositoryEvidenceExtractor.extract(snapshot);

        assertThat(categories).extracting(EngineeringEvidenceCategory::category)
            .containsExactly("ARCHITECTURE", "DATABASE", "TESTING", "DEVOPS", "DOCUMENTATION", "ACTIVITY");
        assertSignal(categories, "HEXAGONAL_BOUNDARIES", true);
        assertSignal(categories, "DATABASE_TECHNOLOGIES", true);
        assertSignal(categories, "DATA_ACCESS_DEPENDENCIES", true);
        assertSignal(categories, "DATABASE_MIGRATIONS", true);
        assertSignal(categories, "PERSISTENCE_CONFIGURATION", true);
        assertSignal(categories, "TEST_FILES", true);
        assertSignal(categories, "CONTAINER_CONFIGURATION", true);
        assertSignal(categories, "README_PRESENT", true);
        assertSignal(categories, "COMMIT_COUNT", true);
    }

    @Test
    void reportsDatabaseAbsenceExplicitlyWithoutGuessingFromGenericFiles() {
        String revision = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        RepositorySnapshot snapshot = RepositorySnapshot.ready(
            UUID.randomUUID(), UUID.randomUUID(), revision, Instant.parse("2026-08-11T00:00:00Z"),
            List.of(new RepositoryBranch("main", revision, true)), List.of(), List.of(), List.of(),
            List.of(file("application.yml"), file("src/main/App.java"))
        );

        var categories = RepositoryEvidenceExtractor.extract(snapshot);

        assertSignal(categories, "DATABASE_TECHNOLOGIES", false);
        assertSignal(categories, "DATA_ACCESS_DEPENDENCIES", false);
        assertSignal(categories, "DATABASE_MIGRATIONS", false);
        assertSignal(categories, "PERSISTENCE_CONFIGURATION", false);
    }

    private static RepositoryFile file(String path) {
        return RepositoryFile.normalized(path, "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", 100);
    }

    private static void assertSignal(List<EngineeringEvidenceCategory> categories, String key, boolean expected) {
        var signal = categories.stream().flatMap(value -> value.signals().stream())
            .filter(value -> value.signalKey().equals(key)).findFirst().orElseThrow();
        assertThat(signal.present()).isEqualTo(expected);
        assertThat(signal).hasNoNullFieldsOrPropertiesExcept("observedValue");
    }
}
