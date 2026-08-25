package com.devpath.repository.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DependencyTechnologyDetector {
    private static final Map<String, TechnologyDefinition> DEFINITIONS = definitions();

    private DependencyTechnologyDetector() {}

    public static List<RepositoryTechnology> detect(List<RepositoryDependency> dependencies) {
        Map<String, Detection> detected = new LinkedHashMap<>();
        dependencies.stream().sorted(java.util.Comparator.comparing(RepositoryDependency::packageName)
            .thenComparing(RepositoryDependency::manifestPath)).forEach(dependency -> {
                TechnologyDefinition definition = match(dependency.packageName());
                if (definition != null) {
                    String key = definition.category() + ":" + definition.name();
                    detected.computeIfAbsent(key, ignored -> new Detection(definition, new ArrayList<>()))
                        .paths().add(dependency.manifestPath());
                }
            });
        return detected.values().stream().map(value -> new RepositoryTechnology(
            value.definition().name(), value.definition().category(), value.definition().packageLabel(),
            value.paths().stream().distinct().sorted().toList()
        )).toList();
    }

    private static TechnologyDefinition match(String packageName) {
        TechnologyDefinition exact = DEFINITIONS.get(packageName);
        if (exact != null) return exact;
        if (packageName.startsWith("org.springframework.boot:spring-boot")) {
            return new TechnologyDefinition("Spring Boot", "FRAMEWORK", packageName);
        }
        return null;
    }

    private static Map<String, TechnologyDefinition> definitions() {
        Map<String, TechnologyDefinition> values = new LinkedHashMap<>();
        add(values, "react", "React", "FRAMEWORK"); add(values, "next", "Next.js", "FRAMEWORK");
        add(values, "vue", "Vue.js", "FRAMEWORK"); add(values, "@angular/core", "Angular", "FRAMEWORK");
        add(values, "pg", "PostgreSQL", "DATABASE"); add(values, "org.postgresql:postgresql", "PostgreSQL", "DATABASE");
        add(values, "mysql", "MySQL", "DATABASE"); add(values, "mysql2", "MySQL", "DATABASE");
        add(values, "mongodb", "MongoDB", "DATABASE"); add(values, "mongoose", "MongoDB", "DATABASE");
        add(values, "redis", "Redis", "DATABASE"); add(values, "ioredis", "Redis", "DATABASE");
        add(values, "org.springframework.boot:spring-boot-starter-data-redis", "Redis", "DATABASE");
        return Map.copyOf(values);
    }

    private static void add(Map<String, TechnologyDefinition> values, String dependency, String name, String category) {
        values.put(dependency, new TechnologyDefinition(name, category, dependency));
    }

    private record TechnologyDefinition(String name, String category, String packageLabel) {}
    private record Detection(TechnologyDefinition definition, List<String> paths) {}
}
