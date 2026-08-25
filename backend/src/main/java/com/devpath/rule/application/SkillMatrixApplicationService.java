package com.devpath.rule.application;

import com.devpath.rule.domain.DeterministicSkillMatrixBuilder;
import com.devpath.rule.domain.SkillAssessment;
import com.devpath.rule.domain.SkillMatrix;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillMatrixApplicationService {
    private final RuleEvaluationPersistencePort evaluations;
    private final SkillMatrixPersistencePort matrices;
    private final DeterministicSkillMatrixBuilder builder = new DeterministicSkillMatrixBuilder();

    public SkillMatrixApplicationService(RuleEvaluationPersistencePort evaluations, SkillMatrixPersistencePort matrices) {
        this.evaluations = evaluations; this.matrices = matrices;
    }

    @Transactional
    public SkillMatrix generate(UUID userId, UUID evaluationId, Instant now) {
        return matrices.findByEvaluationAndOwner(evaluationId, userId).orElseGet(() -> {
            var evaluation = evaluations.findByIdAndOwner(evaluationId, userId)
                .orElseThrow(RuleEvaluationNotFoundException::new);
            var policy = matrices.loadActivePolicy(evaluation.ruleSetVersionId());
            UUID repositoryId = matrices.findRepositoryIdByEvaluationAndOwner(evaluationId, userId)
                .orElseThrow(RuleEvaluationNotFoundException::new);
            var links = evaluations.findEvidenceByEvaluationAndOwner(evaluationId, userId);
            return matrices.saveAsCurrent(builder.build(UUID.randomUUID(), evaluation, repositoryId, policy, links, now));
        });
    }

    @Transactional(readOnly = true)
    public SkillMatrixView getCurrent(UUID userId) {
        return matrices.findCurrentByOwner(userId).map(this::toView).orElseThrow(SkillMatrixNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public SkillMatrixView get(UUID userId, UUID matrixId) {
        return matrices.findByIdAndOwner(matrixId, userId).map(this::toView).orElseThrow(SkillMatrixNotFoundException::new);
    }

    private SkillMatrixView toView(SkillMatrix matrix) {
        var skills = matrix.assessments().stream().map(this::toView).toList();
        var strengths = matrix.assessments().stream().filter(SkillAssessment::strength)
            .map(value -> value.skill().stableKey()).toList();
        var weaknesses = matrix.assessments().stream().filter(SkillAssessment::weakness)
            .map(value -> value.skill().stableKey()).toList();
        return new SkillMatrixView(matrix.matrixId(), matrix.evaluationId(), matrix.policyVersion(), matrix.ruleSetVersion(),
            matrix.status(), skills, strengths, weaknesses, matrix.generatedAt());
    }

    private SkillAssessmentView toView(SkillAssessment value) {
        return new SkillAssessmentView(value.assessmentId(), value.skill().skillId(), value.skill().stableKey(),
            value.skill().name(), value.skill().category().name(), value.score(), value.level().name(), value.confidence(),
            value.strength(), value.weakness(), value.growthTrend(), value.aggregateRuleResultReference(),
            value.evidenceIds(), value.repositoryIds(), value.recommendationInputFacts(), value.ruleSetVersion());
    }
}
