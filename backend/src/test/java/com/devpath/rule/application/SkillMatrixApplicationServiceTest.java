package com.devpath.rule.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devpath.rule.domain.CompletedRuleEvaluation;
import com.devpath.rule.domain.RuleCategory;
import com.devpath.rule.domain.RuleCategoryScore;
import com.devpath.rule.domain.RuleEvaluationEvidence;
import com.devpath.rule.domain.RuleEvaluationEvidenceLink;
import com.devpath.rule.domain.RuleEvaluationResult;
import com.devpath.rule.domain.RuleExecutionResult;
import com.devpath.rule.domain.RuleOutcomeStatus;
import com.devpath.rule.domain.SkillDefinition;
import com.devpath.rule.domain.SkillMatrix;
import com.devpath.rule.domain.SkillMatrixPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkillMatrixApplicationServiceTest {
    @Test
    void generatesOneCurrentMatrixPerEvaluationAndEnforcesOwnerReads() {
        UUID userId = UUID.randomUUID(); UUID repositoryId = UUID.randomUUID();
        CompletedRuleEvaluation evaluation = evaluation(userId);
        RuleEvaluationEvidenceLink link = evidenceLink(evaluation);
        var evaluationPort = new EvaluationPort(evaluation, link);
        var matrixPort = new MatrixPort(userId, repositoryId, policy(evaluation.ruleSetVersionId()));
        var audit = new AuditPort();
        var service = new SkillMatrixApplicationService(evaluationPort, matrixPort, audit,
            Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC));

        SkillMatrix first = service.generate(userId, evaluation.id(), Instant.parse("2026-08-11T10:00:00Z"));
        SkillMatrix repeated = service.generate(userId, evaluation.id(), Instant.parse("2026-08-11T11:00:00Z"));

        assertThat(repeated.matrixId()).isEqualTo(first.matrixId());
        assertThat(matrixPort.saveCount).isEqualTo(1);
        SkillMatrixView current = service.getCurrent(userId);
        assertThat(current.skills()).singleElement().satisfies(skill -> {
            assertThat(skill.level()).isEqualTo("STRONG");
            assertThat(skill.evidenceIds()).containsExactly(link.evidence().evidenceId());
            assertThat(skill.repositoryIds()).containsExactly(repositoryId);
        });
        assertThatThrownBy(() -> service.getCurrent(UUID.randomUUID())).isInstanceOf(SkillMatrixNotFoundException.class);

        SkillMatrix historical = new SkillMatrix(UUID.randomUUID(), userId, UUID.randomUUID(), first.policyId(),
            first.policyVersion(), first.ruleSetVersion(), "SUPERSEDED", first.generatedAt().minusSeconds(60),
            first.assessments());
        matrixPort.stored.add(historical);
        var comparison = service.compare(userId, List.of(historical.matrixId(), first.matrixId()));
        assertThat(comparison.matrices()).extracting(SkillMatrixView::skillMatrixId)
            .containsExactly(historical.matrixId(), first.matrixId());
        assertThat(audit.event).isEqualTo(SkillMatrixAuditEvent.SKILL_MATRICES_COMPARED);
        assertThat(audit.matrixIds).containsExactly(historical.matrixId(), first.matrixId());
        assertThatThrownBy(() -> service.compare(userId, List.of(first.matrixId(), first.matrixId())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.compare(UUID.randomUUID(), List.of(historical.matrixId(), first.matrixId())))
            .isInstanceOf(SkillMatrixNotFoundException.class);

        UUID skillId = first.assessments().get(0).skill().skillId();
        SkillDetailView detail = service.getSkillDetail(userId, skillId);
        assertThat(detail.skillMatrixId()).isEqualTo(first.matrixId());
        assertThat(detail.skill().skillId()).isEqualTo(skillId);
        assertThat(audit.event).isEqualTo(SkillMatrixAuditEvent.SKILL_DETAIL_VIEWED);
        SkillEvidenceListView skillEvidence = service.getSkillEvidence(userId, skillId);
        assertThat(skillEvidence.evidence()).singleElement().satisfies(value ->
            assertThat(value.evidenceId()).isEqualTo(link.evidence().evidenceId()));
        assertThat(audit.event).isEqualTo(SkillMatrixAuditEvent.SKILL_EVIDENCE_VIEWED);
        assertThat(audit.skillId).isEqualTo(skillId);
        assertThatThrownBy(() -> service.getSkillDetail(UUID.randomUUID(), skillId))
            .isInstanceOf(SkillNotFoundException.class);
    }

    private static CompletedRuleEvaluation evaluation(UUID userId) {
        UUID evaluationId = UUID.randomUUID(); UUID snapshotId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
        var rule = new RuleExecutionResult("README_PRESENT", "1", RuleCategory.DOCUMENTATION, RuleOutcomeStatus.PASSED,
            BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ONE, "PRESENCE@formula-v1",
            "formula=PRESENCE; raw=1; score=100", List.of("snapshot:" + snapshotId + ":path:README.md"));
        var category = new RuleCategoryScore(RuleCategory.DOCUMENTATION, new BigDecimal("100"), BigDecimal.ONE,
            new BigDecimal("100"), List.of(rule), List.of());
        var result = new RuleEvaluationResult(snapshotId.toString(), versionId.toString(), "baseline-v1", "formula-v1",
            "engineering-evidence-extractor-v1", new BigDecimal("100"), new BigDecimal("100"), List.of(category), List.of());
        return new CompletedRuleEvaluation(evaluationId, userId, snapshotId, versionId, "d".repeat(64), result,
            Instant.parse("2026-08-11T09:00:00Z"), Instant.parse("2026-08-11T09:00:00Z"));
    }

    private static RuleEvaluationEvidenceLink evidenceLink(CompletedRuleEvaluation evaluation) {
        var evidence = new RuleEvaluationEvidence(UUID.randomUUID(), evaluation.userId(), evaluation.snapshotId(),
            "REPOSITORY_PATH", "snapshot:" + evaluation.snapshotId() + ":path:README.md",
            "Normalized repository-path evidence", new BigDecimal("100"));
        return new RuleEvaluationEvidenceLink("README_PRESENT", "DIRECT", evidence);
    }

    private static SkillMatrixPolicy policy(UUID versionId) {
        var skill = new SkillDefinition(UUID.randomUUID(), "technical-documentation", "Technical Documentation",
            RuleCategory.DOCUMENTATION);
        return new SkillMatrixPolicy(UUID.randomUUID(), versionId, "skill-matrix-v1", BigDecimal.ONE,
            new BigDecimal("40"), new BigDecimal("60"), new BigDecimal("80"), new BigDecimal("39.99"),
            new BigDecimal("80"), List.of(skill));
    }

    private record EvaluationPort(CompletedRuleEvaluation evaluation, RuleEvaluationEvidenceLink link)
        implements RuleEvaluationPersistencePort {
        @Override public Optional<CompletedRuleEvaluation> findCompletedByBasis(UUID userId, UUID snapshotId, UUID ruleSetVersionId, String inputHash) { return Optional.empty(); }
        @Override public Optional<CompletedRuleEvaluation> findByIdAndOwner(UUID evaluationId, UUID userId) {
            return evaluation.id().equals(evaluationId) && evaluation.userId().equals(userId) ? Optional.of(evaluation) : Optional.empty();
        }
        @Override public CompletedRuleEvaluation saveCompleted(CompletedRuleEvaluation value) { throw new UnsupportedOperationException(); }
        @Override public List<RuleEvaluationEvidenceLink> findEvidenceByEvaluationAndOwner(UUID evaluationId, UUID userId) {
            return findByIdAndOwner(evaluationId, userId).isPresent() ? List.of(link) : List.of();
        }
        @Override public List<com.devpath.rule.domain.RuleEvaluationEvidence> findEvidenceByIdsAndOwner(
            List<UUID> evidenceIds, UUID userId
        ) {
            return evaluation.userId().equals(userId) && evidenceIds.contains(link.evidence().evidenceId())
                ? List.of(link.evidence()) : List.of();
        }
    }

    private static final class MatrixPort implements SkillMatrixPersistencePort {
        private final UUID userId; private final UUID repositoryId; private final SkillMatrixPolicy policy;
        private final List<SkillMatrix> stored = new ArrayList<>(); private int saveCount;
        private MatrixPort(UUID userId, UUID repositoryId, SkillMatrixPolicy policy) {
            this.userId = userId; this.repositoryId = repositoryId; this.policy = policy;
        }
        @Override public SkillMatrixPolicy loadActivePolicy(UUID ruleSetVersionId) { return policy; }
        @Override public Optional<SkillMatrix> findByEvaluationAndOwner(UUID evaluationId, UUID ownerId) {
            return userId.equals(ownerId) ? stored.stream().filter(value -> value.evaluationId().equals(evaluationId)).findFirst() : Optional.empty();
        }
        @Override public Optional<SkillMatrix> findCurrentByOwner(UUID ownerId) {
            return userId.equals(ownerId) ? stored.stream().filter(value -> "CURRENT".equals(value.status())).findFirst() : Optional.empty();
        }
        @Override public Optional<SkillMatrix> findByIdAndOwner(UUID matrixId, UUID ownerId) {
            return userId.equals(ownerId) ? stored.stream().filter(value -> value.matrixId().equals(matrixId)).findFirst() : Optional.empty();
        }
        @Override public List<SkillMatrix> findAllByIdsAndOwner(List<UUID> matrixIds, UUID ownerId) {
            return userId.equals(ownerId) ? stored.stream().filter(value -> matrixIds.contains(value.matrixId())).toList() : List.of();
        }
        @Override public Optional<UUID> findRepositoryIdByEvaluationAndOwner(UUID evaluationId, UUID ownerId) {
            return userId.equals(ownerId) ? Optional.of(repositoryId) : Optional.empty();
        }
        @Override public SkillMatrix saveAsCurrent(SkillMatrix matrix) { stored.add(matrix); saveCount++; return matrix; }
    }

    private static final class AuditPort implements SkillMatrixAuditPort {
        private SkillMatrixAuditEvent event;
        private List<UUID> matrixIds = List.of();
        private UUID skillId;
        @Override public void record(SkillMatrixAuditEvent event, UUID userId, List<UUID> matrixIds, Instant occurredAt) {
            this.event = event; this.matrixIds = List.copyOf(matrixIds);
        }
        @Override public void record(SkillMatrixAuditEvent event, UUID userId, UUID skillId, Instant occurredAt) {
            this.event = event; this.skillId = skillId;
        }
    }
}
