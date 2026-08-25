package com.devpath.rule.application;

import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.rule.domain.CompletedRuleEvaluation;
import com.devpath.rule.domain.RuleCategoryScore;
import com.devpath.rule.domain.RuleExecutionResult;
import com.devpath.rule.domain.RuleEvaluationEvidenceLink;
import com.devpath.rule.domain.SkillMatrix;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompletedRuleEvaluationApplicationService {
    private final RepositoryRuleEvaluationApplicationService evaluator;
    private final RuleEvaluationPersistencePort persistence;
    private final SkillMatrixApplicationService skillMatrices;

    public CompletedRuleEvaluationApplicationService(
        RepositoryRuleEvaluationApplicationService evaluator,
        RuleEvaluationPersistencePort persistence,
        SkillMatrixApplicationService skillMatrices
    ) {
        this.evaluator = evaluator;
        this.persistence = persistence;
        this.skillMatrices = skillMatrices;
    }

    @Transactional
    public CompletedRuleEvaluation evaluateAndPersist(RepositorySnapshot snapshot, Instant now) {
        CompletedRuleEvaluation evaluation = persistEvaluation(snapshot, now);
        skillMatrices.generate(snapshot.userId(), evaluation.id(), now);
        return evaluation;
    }

    @Transactional
    public RuleAnalysisCompletion evaluateAndPersistWithMatrix(RepositorySnapshot snapshot, Instant now) {
        CompletedRuleEvaluation evaluation = persistEvaluation(snapshot, now);
        SkillMatrix matrix = skillMatrices.generate(snapshot.userId(), evaluation.id(), now);
        return new RuleAnalysisCompletion(evaluation, matrix);
    }

    private CompletedRuleEvaluation persistEvaluation(RepositorySnapshot snapshot, Instant now) {
        var result = evaluator.evaluate(snapshot);
        UUID ruleSetVersionId = UUID.fromString(result.ruleSetVersionId());
        String inputHash = hash(snapshot.contentHash(), result.ruleSetVersionId(), result.ruleSetVersion(),
            result.formulaLibraryVersion(), result.extractorVersion());
        CompletedRuleEvaluation evaluation = persistence.findCompletedByBasis(
            snapshot.userId(), snapshot.id(), ruleSetVersionId, inputHash
        )
            .orElseGet(() -> persistence.saveCompleted(new CompletedRuleEvaluation(
                UUID.randomUUID(), snapshot.userId(), snapshot.id(), ruleSetVersionId,
                inputHash, result, now, now
            )));
        return evaluation;
    }

    @Transactional(readOnly = true)
    public RuleEvaluationView getEvaluation(UUID userId, UUID evaluationId) {
        return toView(find(userId, evaluationId));
    }

    @Transactional(readOnly = true)
    public RuleScoreBreakdownView getScoreBreakdown(UUID userId, UUID evaluationId) {
        CompletedRuleEvaluation evaluation = find(userId, evaluationId);
        return new RuleScoreBreakdownView(evaluation.id(), evaluation.result().overallScore(),
            evaluation.result().confidence(), evaluation.result().categoryScores().stream().map(this::toCategory).toList());
    }

    @Transactional(readOnly = true)
    public RuleEvidenceListView getEvidence(UUID userId, UUID evaluationId) {
        find(userId, evaluationId);
        var evidence = persistence.findEvidenceByEvaluationAndOwner(evaluationId, userId).stream().map(link ->
            new RuleEvidenceView(link.evidence().evidenceId(), link.ruleId(), link.contributionRole(),
                link.evidence().evidenceType(), link.evidence().sourceReference(),
                link.evidence().observedFactSummary(), link.evidence().confidence())).toList();
        return new RuleEvidenceListView(evaluationId, evidence);
    }

    private CompletedRuleEvaluation find(UUID userId, UUID evaluationId) {
        return persistence.findByIdAndOwner(evaluationId, userId).orElseThrow(RuleEvaluationNotFoundException::new);
    }

    private RuleEvaluationView toView(CompletedRuleEvaluation evaluation) {
        var result = evaluation.result();
        int evidenceCount = Math.toIntExact(result.categoryScores().stream().flatMap(category -> category.ruleResults().stream())
            .flatMap(rule -> rule.evidenceReferences().stream()).distinct().count());
        int rulesWithEvidence = Math.toIntExact(result.categoryScores().stream().flatMap(category -> category.ruleResults().stream())
            .filter(rule -> !rule.evidenceReferences().isEmpty()).count());
        int missingEvidenceCount = result.categoryScores().stream().mapToInt(category -> category.missingEvidence().size()).sum();
        return new RuleEvaluationView(evaluation.id(), evaluation.snapshotId(), evaluation.ruleSetVersionId(),
            result.ruleSetVersion(), result.formulaLibraryVersion(), result.extractorVersion(), result.overallScore(),
            result.confidence(), new RuleEvidenceSummaryView(evidenceCount, rulesWithEvidence, missingEvidenceCount),
            result.categoryScores().stream().map(this::toCategory).toList(),
            result.warnings(), evaluation.completedAt());
    }

    private RuleCategoryScoreView toCategory(RuleCategoryScore category) {
        return new RuleCategoryScoreView(category.category().name(), category.score(), category.weight(), category.confidence(),
            category.ruleResults().stream().map(this::toRule).toList(), category.missingEvidence());
    }

    private RuleResultView toRule(RuleExecutionResult result) {
        return new RuleResultView(result.ruleId(), result.ruleVersion(), result.status().name(), result.rawValue(),
            result.score(), result.weight(), result.formulaId(), result.trace(), result.evidenceReferences());
    }

    private static String hash(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) digest.update((part + "\n").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
