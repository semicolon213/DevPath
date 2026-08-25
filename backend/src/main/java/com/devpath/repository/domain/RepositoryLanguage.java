package com.devpath.repository.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record RepositoryLanguage(
    String providerLabel,
    String canonicalName,
    long byteCount,
    BigDecimal percentage,
    String taxonomyStatus,
    String taxonomyVersion,
    String extractorVersion
) {
    public static final String TAXONOMY_VERSION = "technology-taxonomy-v1";
    public static final String EXTRACTOR_VERSION = "github-language-extractor-v1";
    private static final Map<String, String> ALIASES = Map.of(
        "typescript", "TypeScript", "javascript", "JavaScript", "java", "Java",
        "python", "Python", "kotlin", "Kotlin", "go", "Go", "rust", "Rust"
    );
    private static final Set<String> SUPPORTED = Set.of(
        "TypeScript", "JavaScript", "Java", "Python", "Kotlin", "Go", "Rust",
        "C", "C++", "C#", "PHP", "Ruby", "Swift", "Dart", "Shell", "HTML", "CSS", "SQL"
    );

    public RepositoryLanguage {
        if (providerLabel == null || providerLabel.isBlank() || canonicalName == null || canonicalName.isBlank()
            || byteCount < 0 || percentage == null || percentage.signum() < 0
            || percentage.compareTo(BigDecimal.valueOf(100)) > 0
            || !("SUPPORTED".equals(taxonomyStatus) || "UNSUPPORTED".equals(taxonomyStatus))
            || !TAXONOMY_VERSION.equals(taxonomyVersion) || !EXTRACTOR_VERSION.equals(extractorVersion)) {
            throw new IllegalArgumentException("Repository language evidence is invalid");
        }
    }

    public static RepositoryLanguage normalize(String providerLabel, long bytes, long totalBytes) {
        String canonical = ALIASES.getOrDefault(providerLabel.toLowerCase(Locale.ROOT), providerLabel.trim());
        BigDecimal percentage = totalBytes == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(bytes)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalBytes), 4, RoundingMode.HALF_UP);
        return new RepositoryLanguage(providerLabel.trim(), canonical, bytes, percentage,
            SUPPORTED.contains(canonical) ? "SUPPORTED" : "UNSUPPORTED", TAXONOMY_VERSION, EXTRACTOR_VERSION);
    }
}
