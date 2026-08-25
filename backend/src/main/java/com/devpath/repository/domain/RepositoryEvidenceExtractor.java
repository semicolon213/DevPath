package com.devpath.repository.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class RepositoryEvidenceExtractor {
    public static final String EXTRACTOR_VERSION = "engineering-evidence-extractor-v3";

    private RepositoryEvidenceExtractor() {}

    public static List<EngineeringEvidenceCategory> extract(RepositorySnapshot snapshot) {
        return List.of(
            architecture(snapshot.files()), database(snapshot.files(), snapshot.dependencies()),
            testing(snapshot.files(), snapshot.dependencies()),
            devops(snapshot.files()), documentation(snapshot), collaboration(snapshot), activity(snapshot)
        );
    }

    private static EngineeringEvidenceCategory database(
        List<RepositoryFile> files, List<RepositoryDependency> dependencies
    ) {
        var technologies = DependencyTechnologyDetector.detect(dependencies).stream()
            .filter(value -> value.category().equals("DATABASE")).toList();
        var dataAccess = dependencies.stream().filter(value -> dataAccessDependency(value.packageName())).toList();
        List<String> migrations = paths(files, path -> path.startsWith("migrations/")
            || path.contains("/migrations/") || path.contains("/db/migration/")
            || fileName(path).equals("schema.sql") || fileName(path).equals("data.sql"));
        List<String> configuration = paths(files, path -> fileName(path).equals("persistence.xml")
            || fileName(path).equals("schema.prisma") || fileName(path).equals("database.yml")
            || fileName(path).equals("database.yaml") || fileName(path).startsWith("ormconfig.")
            || fileName(path).startsWith("knexfile.") || fileName(path).equals("alembic.ini")
            || fileName(path).equals("hibernate.cfg.xml"));
        List<String> technologyPaths = technologies.stream().flatMap(value -> value.evidencePaths().stream())
            .distinct().sorted().limit(20).toList();
        List<String> dataAccessPaths = dataAccess.stream().map(RepositoryDependency::manifestPath)
            .distinct().sorted().limit(20).toList();
        return category("DATABASE", "Database", List.of(
            signal("DATABASE_TECHNOLOGIES", "Database technologies", !technologies.isEmpty(), technologies.size(),
                joinLimited(technologies.stream().map(RepositoryTechnology::name).distinct().sorted().toList()), technologyPaths),
            signal("DATA_ACCESS_DEPENDENCIES", "Data access dependencies", !dataAccess.isEmpty(), dataAccess.size(),
                joinLimited(dataAccess.stream().map(RepositoryDependency::packageName).distinct().sorted().toList()), dataAccessPaths),
            signal("DATABASE_MIGRATIONS", "Database migrations", !migrations.isEmpty(), migrations.size(), null, migrations),
            signal("PERSISTENCE_CONFIGURATION", "Persistence configuration", !configuration.isEmpty(),
                configuration.size(), null, configuration)
        ));
    }

    private static EngineeringEvidenceCategory architecture(List<RepositoryFile> files) {
        List<String> domain = paths(files, path -> segment(path, "domain"));
        List<String> application = paths(files, path -> segment(path, "application"));
        List<String> adapter = paths(files, path -> segment(path, "adapter"));
        List<String> controller = paths(files, path -> path.contains("controller"));
        List<String> service = paths(files, path -> path.contains("service"));
        List<String> repository = paths(files, path -> path.contains("repository"));
        var topLevels = new LinkedHashSet<String>();
        files.forEach(file -> topLevels.add(file.path().contains("/") ? file.path().substring(0, file.path().indexOf('/')) : "(root)"));
        boolean hexagonal = !domain.isEmpty() && !application.isEmpty() && !adapter.isEmpty();
        boolean layered = !controller.isEmpty() && !service.isEmpty() && !repository.isEmpty();
        return category("ARCHITECTURE", "Architecture", List.of(
            signal("STRUCTURED_BOUNDARIES", "Structured boundaries", hexagonal || layered,
                hexagonal || layered ? 1 : 0, hexagonal ? "HEXAGONAL" : layered ? "LAYERED" : null,
                hexagonal ? combine(domain, application, adapter) : layered ? combine(controller, service, repository) : List.of()),
            signal("HEXAGONAL_BOUNDARIES", "Hexagonal boundaries", hexagonal,
                domain.size() + application.size() + adapter.size(), null, combine(domain, application, adapter)),
            signal("LAYERED_BOUNDARIES", "Layered boundaries", layered,
                controller.size() + service.size() + repository.size(), null, combine(controller, service, repository)),
            signal("MODULE_LAYOUT", "Module layout", topLevels.size() >= 2, topLevels.size(), String.join(", ", topLevels.stream().limit(10).toList()), List.of())
        ));
    }

    private static EngineeringEvidenceCategory testing(List<RepositoryFile> files, List<RepositoryDependency> dependencies) {
        List<String> tests = paths(files, path -> path.matches(".*(^|/)(test|tests|__tests__)(/|$).*")
            || path.matches(".*(\\.test|\\.spec)\\.[a-z0-9]+$") || path.matches(".*(^|/)test_[^/]+\\.py$")
            || path.endsWith("_test.go"));
        var frameworks = dependencies.stream().filter(value -> {
            String name = value.packageName();
            return name.contains("junit") || name.contains("mockito") || name.contains("testcontainers")
                || name.equals("vitest") || name.equals("jest") || name.contains("testing-library")
                || name.equals("playwright") || name.equals("cypress");
        }).toList();
        List<String> workflows = paths(files, path -> path.startsWith(".github/workflows/") && yaml(path));
        return category("TESTING", "Testing", List.of(
            signal("TEST_FILES", "Test files", !tests.isEmpty(), tests.size(), null, tests),
            signal("TEST_FRAMEWORKS", "Test frameworks", !frameworks.isEmpty(), frameworks.size(),
                joinLimited(frameworks.stream().map(RepositoryDependency::packageName).distinct().sorted().toList()),
                frameworks.stream().map(RepositoryDependency::manifestPath).distinct().sorted().limit(20).toList()),
            signal("CI_WORKFLOW_METADATA", "CI workflow metadata", !workflows.isEmpty(), workflows.size(), null, workflows)
        ));
    }

    private static EngineeringEvidenceCategory devops(List<RepositoryFile> files) {
        List<String> docker = paths(files, path -> fileName(path).equals("dockerfile")
            || fileName(path).startsWith("docker-compose.") || fileName(path).startsWith("compose."));
        List<String> workflows = paths(files, path -> path.startsWith(".github/workflows/") && yaml(path));
        List<String> infrastructure = paths(files, path -> path.endsWith(".tf") || path.contains("/k8s/")
            || path.contains("/kubernetes/") || fileName(path).equals("helmfile.yaml"));
        List<String> deployment = paths(files, path -> fileName(path).startsWith("nginx")
            || fileName(path).equals("procfile") || path.contains("deployment") && yaml(path));
        return category("DEVOPS", "DevOps", List.of(
            signal("CONTAINER_CONFIGURATION", "Container configuration", !docker.isEmpty(), docker.size(), null, docker),
            signal("CI_WORKFLOWS", "CI workflows", !workflows.isEmpty(), workflows.size(), null, workflows),
            signal("INFRASTRUCTURE_AS_CODE", "Infrastructure as code", !infrastructure.isEmpty(), infrastructure.size(), null, infrastructure),
            signal("DEPLOYMENT_CONFIGURATION", "Deployment configuration", !deployment.isEmpty(), deployment.size(), null, deployment)
        ));
    }

    private static EngineeringEvidenceCategory documentation(RepositorySnapshot snapshot) {
        List<RepositoryFile> files = snapshot.files();
        List<String> readme = paths(files, path -> fileName(path).startsWith("readme."));
        List<RepositoryDocument> readmeDocuments = snapshot.documents().stream()
            .filter(value -> value.documentType().equals("README")).toList();
        List<String> capturedReadmePaths = readmeDocuments.stream().map(RepositoryDocument::path).sorted().toList();
        int qualitySignalCount = readmeDocuments.stream().mapToInt(value -> value.qualitySignals().size()).sum();
        List<String> api = paths(files, path -> path.contains("openapi") || path.contains("swagger") || path.contains("/api-doc"));
        List<String> architecture = paths(files, path -> path.contains("architecture") || path.contains("/adr")
            || fileName(path).equals("design.md"));
        List<String> contributing = paths(files, path -> fileName(path).startsWith("contributing."));
        List<String> license = paths(files, path -> fileName(path).startsWith("license"));
        return category("DOCUMENTATION", "Documentation", List.of(
            signal("README_PRESENT", "README present", !readmeDocuments.isEmpty() || !readme.isEmpty(),
                !readmeDocuments.isEmpty() ? readmeDocuments.size() : readme.size(), null,
                !capturedReadmePaths.isEmpty() ? capturedReadmePaths : readme),
            signal("README_QUALITY_SECTIONS", "README quality sections", qualitySignalCount > 0,
                qualitySignalCount,
                joinLimited(readmeDocuments.stream().flatMap(value -> value.qualitySignals().stream()).distinct().sorted().toList()),
                capturedReadmePaths),
            signal("API_DOCUMENTATION", "API documentation", !api.isEmpty(), api.size(), null, api),
            signal("ARCHITECTURE_DOCUMENTATION", "Architecture documentation", !architecture.isEmpty(), architecture.size(), null, architecture),
            signal("CONTRIBUTING_GUIDE", "Contributing guide", !contributing.isEmpty(), contributing.size(), null, contributing),
            signal("LICENSE_PRESENT", "License present", !license.isEmpty(), license.size(), null, license)
        ));
    }

    private static EngineeringEvidenceCategory collaboration(RepositorySnapshot snapshot) {
        int merged = Math.toIntExact(snapshot.pullRequests().stream().filter(value -> value.status().equals("MERGED")).count());
        int reviews = snapshot.pullRequests().stream().mapToInt(RepositoryPullRequest::reviewCount).sum();
        int closedIssues = Math.toIntExact(snapshot.issues().stream().filter(value -> value.status().equals("CLOSED")).count());
        int labelledIssues = Math.toIntExact(snapshot.issues().stream().filter(value -> !value.labels().isEmpty()).count());
        return category("COLLABORATION", "Collaboration", List.of(
            signal("PULL_REQUEST_COUNT", "Captured pull requests", !snapshot.pullRequests().isEmpty(),
                snapshot.pullRequests().size(), Integer.toString(snapshot.pullRequests().size()), List.of()),
            signal("MERGED_PULL_REQUEST_COUNT", "Merged pull requests", merged > 0, merged, Integer.toString(merged), List.of()),
            signal("PULL_REQUEST_REVIEW_COUNT", "Pull request reviews", reviews > 0, reviews, Integer.toString(reviews), List.of()),
            signal("ISSUE_COUNT", "Captured issues", !snapshot.issues().isEmpty(),
                snapshot.issues().size(), Integer.toString(snapshot.issues().size()), List.of()),
            signal("CLOSED_ISSUE_COUNT", "Closed issues", closedIssues > 0, closedIssues, Integer.toString(closedIssues), List.of()),
            signal("LABELLED_ISSUE_COUNT", "Labelled issues", labelledIssues > 0, labelledIssues, Integer.toString(labelledIssues), List.of())
        ));
    }

    private static EngineeringEvidenceCategory activity(RepositorySnapshot snapshot) {
        long contributors = snapshot.commits().stream().map(RepositoryCommit::authorLogin)
            .filter(value -> value != null && !value.isBlank()).distinct().count();
        return category("ACTIVITY", "Activity", List.of(
            signal("COMMIT_COUNT", "Captured commits", !snapshot.commits().isEmpty(), snapshot.commits().size(), Integer.toString(snapshot.commits().size()), List.of()),
            signal("CONTRIBUTOR_COUNT", "Captured contributors", contributors > 0, Math.toIntExact(contributors), Long.toString(contributors), List.of()),
            signal("BRANCH_COUNT", "Captured branches", !snapshot.branches().isEmpty(), snapshot.branches().size(), Integer.toString(snapshot.branches().size()), List.of())
        ));
    }

    private static EngineeringEvidenceCategory category(String key, String label, List<EngineeringEvidenceSignal> signals) {
        return new EngineeringEvidenceCategory(key, label, signals);
    }

    private static EngineeringEvidenceSignal signal(String key, String label, boolean present, int count, String value, List<String> paths) {
        return new EngineeringEvidenceSignal(key, label, present, count, value, paths);
    }

    private static List<String> paths(List<RepositoryFile> files, Predicate<String> predicate) {
        return files.stream().map(file -> file.path().toLowerCase(Locale.ROOT)).filter(predicate).distinct().sorted().limit(20).toList();
    }

    @SafeVarargs
    private static List<String> combine(List<String>... groups) {
        var values = new ArrayList<String>();
        for (List<String> group : groups) values.addAll(group);
        return values.stream().distinct().sorted().limit(20).toList();
    }

    private static boolean segment(String path, String value) { return path.contains("/" + value + "/"); }
    private static boolean dataAccessDependency(String packageName) {
        String value = packageName.toLowerCase(Locale.ROOT);
        return value.startsWith("org.springframework.boot:spring-boot-starter-data-")
            || value.startsWith("org.hibernate:") || value.startsWith("org.jooq:")
            || value.equals("@prisma/client") || value.equals("prisma") || value.equals("sequelize")
            || value.equals("typeorm") || value.equals("knex") || value.equals("sqlalchemy")
            || value.equals("alembic") || value.equals("django") || value.startsWith("gorm.io/")
            || value.equals("github.com/jmoiron/sqlx");
    }
    private static boolean yaml(String path) { return path.endsWith(".yml") || path.endsWith(".yaml"); }
    private static String fileName(String path) { return path.substring(path.lastIndexOf('/') + 1); }
    private static String joinLimited(List<String> values) {
        String joined = String.join(", ", values);
        return joined.length() <= 255 ? joined : joined.substring(0, 255);
    }
}
