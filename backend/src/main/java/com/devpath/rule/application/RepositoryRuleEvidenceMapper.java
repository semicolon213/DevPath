package com.devpath.rule.application;

import com.devpath.repository.domain.EngineeringEvidenceSignal;
import com.devpath.repository.domain.DependencyTechnologyDetector;
import com.devpath.repository.domain.RepositoryEvidenceExtractor;
import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.rule.domain.RuleEvidenceFact;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class RepositoryRuleEvidenceMapper {
    public static final String MAPPER_VERSION = "repository-rule-evidence-v2";
    public static final String EXTRACTOR_VERSION = RepositoryEvidenceExtractor.EXTRACTOR_VERSION;

    private RepositoryRuleEvidenceMapper() {}

    public static List<RuleEvidenceFact> map(RepositorySnapshot snapshot) {
        String prefix = "snapshot:" + snapshot.id() + ":";
        var facts = new ArrayList<RuleEvidenceFact>();
        BigDecimal languageBytes = snapshot.languages().stream().map(value -> BigDecimal.valueOf(value.byteCount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal primaryShare = snapshot.languages().stream().map(value -> BigDecimal.valueOf(value.byteCount()))
            .max(BigDecimal::compareTo).filter(value -> languageBytes.signum() > 0)
            .map(value -> value.multiply(new BigDecimal("100")).divide(languageBytes, 8, RoundingMode.HALF_UP))
            .orElse(BigDecimal.ZERO);
        facts.add(RuleEvidenceFact.measured("LANGUAGE_PRIMARY_SHARE", primaryShare,
            snapshot.languages().stream().map(value -> prefix + "language:" + value.canonicalName()).toList()));
        facts.add(RuleEvidenceFact.measured("LANGUAGE_DIVERSITY", BigDecimal.valueOf(snapshot.languages().size()),
            snapshot.languages().stream().map(value -> prefix + "language:" + value.canonicalName()).toList()));

        var frameworks = DependencyTechnologyDetector.detect(snapshot.dependencies()).stream()
            .filter(value -> value.category().equals("FRAMEWORK")).toList();
        facts.add(RuleEvidenceFact.measured("FRAMEWORK_COUNT", BigDecimal.valueOf(frameworks.size()),
            frameworks.stream().flatMap(value -> value.evidencePaths().stream()).distinct().sorted()
                .map(path -> prefix + "path:" + path).toList()));

        RepositoryEvidenceExtractor.extract(snapshot).stream()
            .flatMap(category -> category.signals().stream())
            .forEach(signal -> facts.add(mapSignal(prefix, signal)));
        return List.copyOf(facts);
    }

    private static RuleEvidenceFact mapSignal(String prefix, EngineeringEvidenceSignal signal) {
        List<String> references = signal.evidencePaths().stream().map(path -> prefix + "path:" + path).toList();
        if (references.isEmpty() && signal.present()) references = List.of(prefix + "signal:" + signal.signalKey());
        return new RuleEvidenceFact(signal.signalKey(), true, signal.present(),
            BigDecimal.valueOf(signal.count()), references);
    }
}
