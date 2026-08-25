package com.devpath.rule.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.repository.domain.RepositoryBranch;
import com.devpath.repository.domain.RepositoryCommit;
import com.devpath.repository.domain.RepositoryDependency;
import com.devpath.repository.domain.RepositoryFile;
import com.devpath.repository.domain.RepositoryLanguage;
import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.rule.domain.MissingDataPolicy;
import com.devpath.rule.domain.RuleCategory;
import com.devpath.rule.domain.RuleDefinition;
import com.devpath.rule.domain.RuleFormula;
import com.devpath.rule.domain.RuleSetVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositoryRuleEvaluationApplicationServiceTest {
    @Test
    void resolvesActiveCatalogAndEvaluatesTheExactSnapshot() {
        var rule = new RuleDefinition("README_PRESENT", "1.0.0", RuleCategory.DOCUMENTATION, "README", 20,
            "README_PRESENT", RuleFormula.PRESENCE, BigDecimal.ZERO, BigDecimal.ONE, MissingDataPolicy.ZERO, true);
        var set = new RuleSetVersion("set", "version", "test-v2", "formula-v1",
            "engineering-evidence-extractor-v2", Map.of(RuleCategory.DOCUMENTATION, BigDecimal.ONE), List.of(rule));
        RuleCatalogPort catalog = scope -> {
            assertThat(scope).isEqualTo(RepositoryRuleEvaluationApplicationService.BASELINE_SCOPE);
            return set;
        };
        var service = new RepositoryRuleEvaluationApplicationService(catalog);
        String revision = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        RepositorySnapshot snapshot = RepositorySnapshot.ready(UUID.randomUUID(), UUID.randomUUID(), revision,
            Instant.parse("2026-08-11T00:00:00Z"), List.of(new RepositoryBranch("main", revision, true)),
            List.of(new RepositoryCommit(revision, "owner", Instant.parse("2026-08-10T00:00:00Z"), "docs")),
            List.of(RepositoryLanguage.normalize("Java", 100, 100)), List.<RepositoryDependency>of(),
            List.of(RepositoryFile.normalized("README.md", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", 100)));

        var result = service.evaluate(snapshot);

        assertThat(result.snapshotId()).isEqualTo(snapshot.id().toString());
        assertThat(result.ruleSetVersion()).isEqualTo("test-v2");
        assertThat(result.overallScore()).isEqualByComparingTo("100.00");
    }
}
