package com.devpath.repository.domain;

public record RepositoryDependency(
    String ecosystem,
    String packageName,
    String versionConstraint,
    String scope,
    String manifestPath,
    String extractorVersion
) {
    public static final String EXTRACTOR_VERSION = "dependency-extractor-v1";

    public RepositoryDependency {
        if (ecosystem == null || ecosystem.isBlank() || ecosystem.length() > 32
            || packageName == null || packageName.isBlank() || packageName.length() > 255
            || versionConstraint != null && versionConstraint.length() > 255
            || !("RUNTIME".equals(scope) || "DEVELOPMENT".equals(scope) || "TEST".equals(scope)
                || "PLUGIN".equals(scope) || "UNKNOWN".equals(scope))
            || manifestPath == null || manifestPath.isBlank() || manifestPath.length() > 500
            || !EXTRACTOR_VERSION.equals(extractorVersion)) {
            throw new IllegalArgumentException("Repository dependency evidence is invalid");
        }
    }

    public static RepositoryDependency normalized(
        String ecosystem, String packageName, String versionConstraint, String scope, String manifestPath
    ) {
        return new RepositoryDependency(ecosystem.toLowerCase(), packageName.trim().toLowerCase(),
            versionConstraint == null || versionConstraint.isBlank() ? null : versionConstraint.trim(),
            scope, manifestPath.replace('\\', '/'), EXTRACTOR_VERSION);
    }
}
