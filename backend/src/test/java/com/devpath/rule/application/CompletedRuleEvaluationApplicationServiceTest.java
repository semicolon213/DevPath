package com.devpath.rule.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.devpath.repository.domain.RepositoryBranch;
import com.devpath.repository.domain.RepositoryCommit;
import com.devpath.repository.domain.RepositoryFile;
import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.rule.domain.CompletedRuleEvaluation;
import com.devpath.rule.domain.MissingDataPolicy;
import com.devpath.rule.domain.RuleCategory;
import com.devpath.rule.domain.RuleDefinition;
import com.devpath.rule.domain.RuleFormula;
import com.devpath.rule.domain.RuleEvaluationEvidenceLink;
import com.devpath.rule.domain.RuleSetVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompletedRuleEvaluationApplicationServiceTest {
    @Test
    void persistsOneImmutableEvaluationPerDeterministicBasisAndEnforcesOwnerReads() {
        var persistence = new InMemoryPersistence();
        var evaluator = new RepositoryRuleEvaluationApplicationService(scope -> ruleSet());
        var skillMatrices = mock(SkillMatrixApplicationService.class);
        var service = new CompletedRuleEvaluationApplicationService(evaluator, persistence, skillMatrices);
        RepositorySnapshot snapshot = snapshot();
        Instant now = Instant.parse("2026-08-11T10:00:00Z");

        CompletedRuleEvaluation first = service.evaluateAndPersist(snapshot, now);
        CompletedRuleEvaluation repeated = service.evaluateAndPersist(snapshot, now.plusSeconds(10));

        assertThat(repeated.id()).isEqualTo(first.id());
        assertThat(persistence.saved).hasSize(1);
        RuleEvaluationView view = service.getEvaluation(snapshot.userId(), first.id());
        assertThat(view.overallScore()).isEqualByComparingTo("100.00");
        assertThat(view.ruleSetVersion()).isEqualTo("test-v2");
        assertThat(view.categoryScores()).hasSize(1);
        assertThat(service.getScoreBreakdown(snapshot.userId(), first.id()).categories()).hasSize(1);
        verify(skillMatrices).generate(snapshot.userId(), first.id(), now);
        verify(skillMatrices).generate(snapshot.userId(), first.id(), now.plusSeconds(10));
        assertThatThrownBy(() -> service.getEvaluation(UUID.randomUUID(), first.id()))
            .isInstanceOf(RuleEvaluationNotFoundException.class);
    }

    private static RuleSetVersion ruleSet() {
        var rule = new RuleDefinition("README_PRESENT", "1.0.0", RuleCategory.DOCUMENTATION, "README", 20,
            "README_PRESENT", RuleFormula.PRESENCE, BigDecimal.ZERO, BigDecimal.ONE, MissingDataPolicy.ZERO, true);
        return new RuleSetVersion("10000000-0000-0000-0000-000000000001",
            "11000000-0000-0000-0000-000000000002", "test-v2", "formula-v1",
            "engineering-evidence-extractor-v2", Map.of(RuleCategory.DOCUMENTATION, BigDecimal.ONE), List.of(rule));
    }

    private static RepositorySnapshot snapshot() {
        String revision = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        return RepositorySnapshot.ready(UUID.randomUUID(), UUID.randomUUID(), revision,
            Instant.parse("2026-08-11T00:00:00Z"), List.of(new RepositoryBranch("main", revision, true)),
            List.of(new RepositoryCommit(revision, "owner", Instant.parse("2026-08-10T00:00:00Z"), "docs")),
            List.of(), List.of(),
            List.of(RepositoryFile.normalized("README.md", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", 100)));
    }

    private static final class InMemoryPersistence implements RuleEvaluationPersistencePort {
        private final List<CompletedRuleEvaluation> saved = new ArrayList<>();
        @Override public Optional<CompletedRuleEvaluation> findCompletedByBasis(UUID userId, UUID snapshotId, UUID ruleSetVersionId, String inputHash) {
            return saved.stream().filter(value -> value.userId().equals(userId) && value.snapshotId().equals(snapshotId)
                && value.ruleSetVersionId().equals(ruleSetVersionId) && value.inputHash().equals(inputHash)).findFirst();
        }
        @Override public Optional<CompletedRuleEvaluation> findByIdAndOwner(UUID evaluationId, UUID userId) {
            return saved.stream().filter(value -> value.id().equals(evaluationId) && value.userId().equals(userId)).findFirst();
        }
        @Override public CompletedRuleEvaluation saveCompleted(CompletedRuleEvaluation evaluation) { saved.add(evaluation); return evaluation; }
        @Override public List<RuleEvaluationEvidenceLink> findEvidenceByEvaluationAndOwner(UUID evaluationId, UUID userId) { return List.of(); }
    }
}
