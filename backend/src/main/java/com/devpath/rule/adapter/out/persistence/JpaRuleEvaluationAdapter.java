package com.devpath.rule.adapter.out.persistence;

import com.devpath.rule.application.RuleEvaluationPersistencePort;
import com.devpath.rule.domain.CompletedRuleEvaluation;
import com.devpath.rule.domain.RuleCategory;
import com.devpath.rule.domain.RuleCategoryScore;
import com.devpath.rule.domain.RuleEvaluationEvidence;
import com.devpath.rule.domain.RuleEvaluationEvidenceLink;
import com.devpath.rule.domain.RuleEvaluationResult;
import com.devpath.rule.domain.RuleExecutionResult;
import com.devpath.rule.domain.RuleOutcomeStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
class JpaRuleEvaluationAdapter implements RuleEvaluationPersistencePort {
    private final RuleEvaluationJpaRepository evaluations;
    private final EvaluationWarningJpaRepository warnings;
    private final RuleCategoryEvaluationJpaRepository categories;
    private final CategoryMissingEvidenceJpaRepository missingEvidence;
    private final RuleExecutionResultJpaRepository ruleResults;
    private final RuleEvidenceJpaRepository evidence;
    private final ScoreEvidenceLinkJpaRepository evidenceLinks;

    JpaRuleEvaluationAdapter(
        RuleEvaluationJpaRepository evaluations,
        EvaluationWarningJpaRepository warnings,
        RuleCategoryEvaluationJpaRepository categories,
        CategoryMissingEvidenceJpaRepository missingEvidence,
        RuleExecutionResultJpaRepository ruleResults,
        RuleEvidenceJpaRepository evidence,
        ScoreEvidenceLinkJpaRepository evidenceLinks
    ) {
        this.evaluations = evaluations; this.warnings = warnings; this.categories = categories;
        this.missingEvidence = missingEvidence; this.ruleResults = ruleResults;
        this.evidence = evidence; this.evidenceLinks = evidenceLinks;
    }

    @Override
    public Optional<CompletedRuleEvaluation> findCompletedByBasis(
        UUID userId, UUID snapshotId, UUID ruleSetVersionId, String inputHash
    ) {
        return evaluations.findByUserIdAndSnapshotIdAndRuleSetVersionIdAndInputHash(userId, snapshotId, ruleSetVersionId, inputHash)
            .map(this::hydrate);
    }

    @Override
    public Optional<CompletedRuleEvaluation> findByIdAndOwner(UUID evaluationId, UUID userId) {
        return evaluations.findByIdAndUserId(evaluationId, userId).map(this::hydrate);
    }

    @Override
    public CompletedRuleEvaluation saveCompleted(CompletedRuleEvaluation evaluation) {
        evaluations.saveAndFlush(new RuleEvaluationJpaEntity(evaluation));
        for (int index = 0; index < evaluation.result().warnings().size(); index++) {
            warnings.save(new EvaluationWarningJpaEntity(evaluation.id(), index, evaluation.result().warnings().get(index)));
        }
        for (RuleCategoryScore category : evaluation.result().categoryScores()) {
            RuleCategoryEvaluationJpaEntity categoryEntity = categories.save(new RuleCategoryEvaluationJpaEntity(evaluation.id(), category));
            for (int index = 0; index < category.missingEvidence().size(); index++) {
                missingEvidence.save(new CategoryMissingEvidenceJpaEntity(categoryEntity.id(), index, category.missingEvidence().get(index)));
            }
            for (RuleExecutionResult rule : category.ruleResults()) {
                RuleExecutionResultJpaEntity ruleEntity = ruleResults.save(
                    new RuleExecutionResultJpaEntity(evaluation.id(), evaluation.ruleSetVersionId(), rule));
                for (String reference : rule.evidenceReferences().stream().distinct().toList()) {
                    RuleEvidenceJpaEntity evidenceEntity = findOrCreateEvidence(evaluation, reference);
                    evidenceLinks.save(new ScoreEvidenceLinkJpaEntity(evidenceEntity.id(), evaluation.id(), ruleEntity.id()));
                }
            }
        }
        evidenceLinks.flush();
        return evaluation;
    }

    @Override
    public List<RuleEvaluationEvidenceLink> findEvidenceByEvaluationAndOwner(UUID evaluationId, UUID userId) {
        if (evaluations.findByIdAndUserId(evaluationId, userId).isEmpty()) return List.of();
        List<ScoreEvidenceLinkJpaEntity> links = evidenceLinks
            .findAllByEvaluationIdOrderByRuleExecutionResultIdAscEvidenceIdAsc(evaluationId);
        Map<UUID, RuleEvidenceJpaEntity> evidenceById = evidence.findAllById(
            links.stream().map(ScoreEvidenceLinkJpaEntity::evidenceId).distinct().toList()
        ).stream().collect(Collectors.toMap(RuleEvidenceJpaEntity::id, Function.identity()));
        Map<UUID, String> ruleIds = ruleResults.findAllByEvaluationIdOrderByCategoryAscRuleIdAsc(evaluationId).stream()
            .collect(Collectors.toMap(RuleExecutionResultJpaEntity::id, RuleExecutionResultJpaEntity::ruleId));
        return links.stream().map(link -> {
            RuleEvidenceJpaEntity value = evidenceById.get(link.evidenceId());
            return new RuleEvaluationEvidenceLink(ruleIds.get(link.ruleExecutionResultId()), link.contributionRole(),
                new RuleEvaluationEvidence(value.id(), value.userId(), value.snapshotId(), value.evidenceType(),
                    value.sourceReference(), value.observedFactSummary(), value.confidence()));
        }).toList();
    }

    private RuleEvidenceJpaEntity findOrCreateEvidence(
        CompletedRuleEvaluation evaluation, String reference
    ) {
        String referenceHash = hash(reference);
        return evidence.findByUserIdAndSnapshotIdAndSourceReferenceHash(evaluation.userId(), evaluation.snapshotId(), referenceHash)
            .orElseGet(() -> evidence.save(new RuleEvidenceJpaEntity(evaluation.userId(), evaluation.snapshotId(),
                evidenceType(reference), reference, referenceHash,
                summary(reference), new java.math.BigDecimal("100.00"), evaluation.completedAt())));
    }

    private CompletedRuleEvaluation hydrate(RuleEvaluationJpaEntity evaluation) {
        List<RuleExecutionResultJpaEntity> storedRules = ruleResults
            .findAllByEvaluationIdOrderByCategoryAscRuleIdAsc(evaluation.id());
        Map<UUID, List<String>> referencesByRule = referencesByRule(evaluation.id());
        Map<String, List<RuleExecutionResultJpaEntity>> rulesByCategory = storedRules.stream()
            .collect(Collectors.groupingBy(RuleExecutionResultJpaEntity::category, LinkedHashMap::new, Collectors.toList()));
        var categoryScores = new ArrayList<RuleCategoryScore>();
        for (RuleCategoryEvaluationJpaEntity category : categories.findAllByEvaluationIdOrderByCategoryAsc(evaluation.id())) {
            List<RuleExecutionResult> hydratedRules = rulesByCategory.getOrDefault(category.category(), List.of()).stream()
                .map(rule -> toDomain(rule, referencesByRule.getOrDefault(rule.id(), List.of()))).toList();
            List<String> missing = missingEvidence.findAllByCategoryEvaluationIdOrderByOrderIndexAsc(category.id()).stream()
                .map(CategoryMissingEvidenceJpaEntity::evidenceKey).toList();
            categoryScores.add(new RuleCategoryScore(RuleCategory.valueOf(category.category()), category.score(), category.weight(),
                category.confidence(), hydratedRules, missing));
        }
        List<String> warningMessages = warnings.findAllByEvaluationIdOrderByOrderIndexAsc(evaluation.id()).stream()
            .map(EvaluationWarningJpaEntity::message).toList();
        RuleEvaluationResult result = new RuleEvaluationResult(evaluation.snapshotId().toString(),
            evaluation.ruleSetVersionId().toString(), evaluation.ruleSetVersionLabel(), evaluation.formulaLibraryVersion(),
            evaluation.extractorVersion(), evaluation.overallScore(), evaluation.confidence(), categoryScores, warningMessages);
        return new CompletedRuleEvaluation(evaluation.id(), evaluation.userId(), evaluation.snapshotId(),
            evaluation.ruleSetVersionId(), evaluation.inputHash(), result, evaluation.startedAt(), evaluation.completedAt());
    }

    private Map<UUID, List<String>> referencesByRule(UUID evaluationId) {
        List<ScoreEvidenceLinkJpaEntity> links = evidenceLinks
            .findAllByEvaluationIdOrderByRuleExecutionResultIdAscEvidenceIdAsc(evaluationId);
        Map<UUID, RuleEvidenceJpaEntity> evidenceById = evidence.findAllById(
            links.stream().map(ScoreEvidenceLinkJpaEntity::evidenceId).distinct().toList()
        ).stream().collect(Collectors.toMap(RuleEvidenceJpaEntity::id, Function.identity()));
        return links.stream().collect(Collectors.groupingBy(ScoreEvidenceLinkJpaEntity::ruleExecutionResultId,
            LinkedHashMap::new, Collectors.mapping(link -> evidenceById.get(link.evidenceId()).sourceReference(), Collectors.toList())));
    }

    private RuleExecutionResult toDomain(RuleExecutionResultJpaEntity value, List<String> references) {
        return new RuleExecutionResult(value.ruleId(), value.ruleVersion(), RuleCategory.valueOf(value.category()),
            RuleOutcomeStatus.valueOf(value.outcomeStatus()), value.rawValue(), value.score(), value.weight(),
            value.formulaId(), value.calculationTrace(), references);
    }

    private static String evidenceType(String reference) {
        if (reference.contains(":path:")) return "REPOSITORY_PATH";
        if (reference.contains(":language:")) return "LANGUAGE_STATISTIC";
        return "SNAPSHOT_SIGNAL";
    }

    private static String summary(String reference) {
        if (reference.contains(":path:")) return "Normalized repository-path evidence captured in the immutable snapshot";
        if (reference.contains(":language:")) return "Normalized language-statistic evidence captured in the immutable snapshot";
        return "Normalized aggregate signal captured in the immutable snapshot";
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
