package com.devpath.rule.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.repository.domain.RepositoryBranch;
import com.devpath.repository.domain.RepositoryCommit;
import com.devpath.repository.domain.RepositoryDependency;
import com.devpath.repository.domain.RepositoryFile;
import com.devpath.repository.domain.RepositoryLanguage;
import com.devpath.repository.domain.RepositorySnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositoryRuleEvidenceMapperTest {
    @Test
    void mapsNormalizedSnapshotFactsWithoutCalculatingScores() {
        String revision = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        RepositorySnapshot snapshot = RepositorySnapshot.ready(
            UUID.randomUUID(), UUID.randomUUID(), revision, Instant.parse("2026-08-11T00:00:00Z"),
            List.of(new RepositoryBranch("main", revision, true)),
            List.of(new RepositoryCommit(revision, "owner", Instant.parse("2026-08-10T00:00:00Z"), "test")),
            List.of(
                RepositoryLanguage.normalize("Java", 700, 1000),
                RepositoryLanguage.normalize("TypeScript", 300, 1000)
            ),
            List.of(
                RepositoryDependency.normalized("gradle", "org.springframework.boot:spring-boot-starter-web", null,
                    "RUNTIME", "backend/build.gradle"),
                RepositoryDependency.normalized("gradle", "org.postgresql:postgresql", null,
                    "RUNTIME", "backend/build.gradle")
            ),
            List.of(file("README.md"), file("src/test/java/example/AppTest.java"), file(".github/workflows/test.yml"),
                file("src/main/domain/Example.java"), file("src/main/resources/db/migration/V1__schema.sql"))
        );

        var facts = RepositoryRuleEvidenceMapper.map(snapshot);

        assertFact(facts, "LANGUAGE_PRIMARY_SHARE", "70.00000000");
        assertFact(facts, "LANGUAGE_DIVERSITY", "2");
        assertFact(facts, "FRAMEWORK_COUNT", "1");
        assertFact(facts, "TEST_FILES", "1");
        var incompleteHexagonalSignal = facts.stream()
            .filter(fact -> fact.signalKey().equals("HEXAGONAL_BOUNDARIES"))
            .findFirst().orElseThrow();
        assertThat(incompleteHexagonalSignal.present()).isFalse();
        assertThat(incompleteHexagonalSignal.numericValue()).isEqualByComparingTo("1");
        assertFact(facts, "DATABASE_TECHNOLOGIES", "1");
        assertFact(facts, "DATA_ACCESS_DEPENDENCIES", "0");
        assertFact(facts, "DATABASE_MIGRATIONS", "1");
        assertThat(RepositoryRuleEvidenceMapper.EXTRACTOR_VERSION).isEqualTo("engineering-evidence-extractor-v2");
        assertThat(facts).allSatisfy(fact -> assertThat(fact.getClass().getRecordComponents())
            .extracting(component -> component.getName()).doesNotContain("score"));
    }

    private static RepositoryFile file(String path) {
        return RepositoryFile.normalized(path, "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", 100);
    }

    private static void assertFact(List<com.devpath.rule.domain.RuleEvidenceFact> facts, String key, String value) {
        BigDecimal actual = facts.stream().filter(fact -> fact.signalKey().equals(key)).findFirst().orElseThrow().numericValue();
        assertThat(actual).isEqualByComparingTo(value);
    }
}
