package com.devpath.rule.application;

import com.devpath.rule.domain.DeterministicSkillMatrixBuilder;
import com.devpath.rule.domain.SkillAssessment;
import com.devpath.rule.domain.SkillMatrix;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillMatrixApplicationService {
    private final RuleEvaluationPersistencePort evaluations;
    private final SkillMatrixPersistencePort matrices;
    private final SkillMatrixAuditPort audit;
    private final Clock clock;
    private final DeterministicSkillMatrixBuilder builder = new DeterministicSkillMatrixBuilder();

    public SkillMatrixApplicationService(
        RuleEvaluationPersistencePort evaluations, SkillMatrixPersistencePort matrices, SkillMatrixAuditPort audit,
        Clock clock
    ) {
        this.evaluations = evaluations; this.matrices = matrices; this.audit = audit; this.clock = clock;
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

    @Transactional
    public SkillMatrixComparisonView compare(UUID userId, List<UUID> matrixIds) {
        if (matrixIds == null) {
            throw new IllegalArgumentException("exactly two distinct skill matrix IDs are required");
        }
        List<UUID> requested = List.copyOf(matrixIds);
        if (requested.size() != 2 || requested.stream().distinct().count() != 2) {
            throw new IllegalArgumentException("exactly two distinct skill matrix IDs are required");
        }
        Map<UUID, SkillMatrix> found = matrices.findAllByIdsAndOwner(requested, userId).stream()
            .collect(Collectors.toMap(SkillMatrix::matrixId, Function.identity()));
        if (found.size() != 2) {
            throw new SkillMatrixNotFoundException();
        }
        var comparison = new SkillMatrixComparisonView(requested.stream().map(found::get).map(this::toView).toList());
        audit.record(SkillMatrixAuditEvent.SKILL_MATRICES_COMPARED, userId, requested, clock.instant());
        return comparison;
    }

    @Transactional
    public SkillDetailView getSkillDetail(UUID userId, UUID skillId) {
        SkillMatrix matrix = currentMatrix(userId);
        SkillAssessment assessment = currentAssessment(matrix, skillId);
        audit.record(SkillMatrixAuditEvent.SKILL_DETAIL_VIEWED, userId, skillId, clock.instant());
        return new SkillDetailView(matrix.matrixId(), matrix.evaluationId(), matrix.policyVersion(),
            matrix.ruleSetVersion(), matrix.status(), matrix.generatedAt(), toView(assessment));
    }

    @Transactional
    public SkillEvidenceListView getSkillEvidence(UUID userId, UUID skillId) {
        SkillMatrix matrix = currentMatrix(userId);
        SkillAssessment assessment = currentAssessment(matrix, skillId);
        var evidence = evaluations.findEvidenceByIdsAndOwner(assessment.evidenceIds(), userId).stream()
            .map(value -> new SkillEvidenceView(value.evidenceId(), value.snapshotId(), value.evidenceType(),
                value.sourceReference(), value.observedFactSummary(), value.confidence()))
            .toList();
        audit.record(SkillMatrixAuditEvent.SKILL_EVIDENCE_VIEWED, userId, skillId, clock.instant());
        return new SkillEvidenceListView(skillId, assessment.assessmentId(), matrix.matrixId(), evidence);
    }

    private SkillMatrix currentMatrix(UUID userId) {
        return matrices.findCurrentByOwner(userId).orElseThrow(SkillNotFoundException::new);
    }

    private SkillAssessment currentAssessment(SkillMatrix matrix, UUID skillId) {
        return matrix.assessments().stream().filter(value -> value.skill().skillId().equals(skillId)).findFirst()
            .orElseThrow(SkillNotFoundException::new);
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
