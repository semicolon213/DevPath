package com.devpath.repository.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DependencyTechnologyDetectorTest {
    @Test
    void detectsFrameworksAndDatabasesWithManifestEvidenceWithoutCalculatingScores() {
        var dependencies = List.of(
            RepositoryDependency.normalized("npm", "react", "18.3.1", "RUNTIME", "frontend/package.json"),
            RepositoryDependency.normalized("gradle", "org.springframework.boot:spring-boot-starter-web", null,
                "RUNTIME", "backend/build.gradle"),
            RepositoryDependency.normalized("gradle", "org.postgresql:postgresql", "42.7.4",
                "RUNTIME", "backend/build.gradle")
        );

        assertThat(DependencyTechnologyDetector.detect(dependencies))
            .extracting(RepositoryTechnology::name)
            .containsExactlyInAnyOrder("React", "PostgreSQL", "Spring Boot");
        assertThat(DependencyTechnologyDetector.detect(dependencies).stream()
            .filter(value -> value.name().equals("React")).findFirst().orElseThrow().evidencePaths())
            .containsExactly("frontend/package.json");
    }
}
