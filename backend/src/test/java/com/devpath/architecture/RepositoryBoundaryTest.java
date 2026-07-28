package com.devpath.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RepositoryBoundaryTest {
    private final Path repositoryRoot = Path.of("..").toAbsolutePath().normalize();

    @Test
    void backendSourceDoesNotContainFrontendSourceImports() throws IOException {
        Path backendSource = repositoryRoot.resolve("backend/src/main/java");
        if (!Files.exists(backendSource)) {
            return;
        }

        boolean hasFrontendImports;
        try (var paths = Files.walk(backendSource)) {
            hasFrontendImports = paths
                .filter(path -> path.toString().endsWith(".java"))
                .map(this::readSafely)
                .anyMatch(content -> content.contains("frontend/") || content.contains("from \"@/"));
        }

        assertThat(hasFrontendImports).isFalse();
    }

    @Test
    void frontendDoesNotContainBackendJavaSources() throws IOException {
        Path frontendSource = repositoryRoot.resolve("frontend/src");
        if (!Files.exists(frontendSource)) {
            return;
        }

        boolean hasJavaSources;
        try (var paths = Files.walk(frontendSource)) {
            hasJavaSources = paths.anyMatch(path -> path.toString().endsWith(".java"));
        }

        assertThat(hasJavaSources).isFalse();
    }

    private String readSafely(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            return "";
        }
    }
}

