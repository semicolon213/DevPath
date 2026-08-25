package com.devpath.rule.application;

import com.devpath.rule.domain.CompletedRuleEvaluation;
import com.devpath.rule.domain.RuleEvaluationEvidenceLink;
import com.devpath.rule.domain.RuleEvaluationEvidence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuleEvaluationPersistencePort {
    Optional<CompletedRuleEvaluation> findCompletedByBasis(UUID userId, UUID snapshotId, UUID ruleSetVersionId, String inputHash);
    Optional<CompletedRuleEvaluation> findByIdAndOwner(UUID evaluationId, UUID userId);
    CompletedRuleEvaluation saveCompleted(CompletedRuleEvaluation evaluation);
    List<RuleEvaluationEvidenceLink> findEvidenceByEvaluationAndOwner(UUID evaluationId, UUID userId);
    List<RuleEvaluationEvidence> findEvidenceByIdsAndOwner(List<UUID> evidenceIds, UUID userId);
}
