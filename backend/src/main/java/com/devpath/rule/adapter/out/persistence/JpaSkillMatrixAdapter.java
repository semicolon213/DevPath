package com.devpath.rule.adapter.out.persistence;

import com.devpath.rule.application.SkillMatrixPersistencePort;
import com.devpath.rule.domain.RuleCategory;
import com.devpath.rule.domain.SkillAssessment;
import com.devpath.rule.domain.SkillDefinition;
import com.devpath.rule.domain.SkillLevel;
import com.devpath.rule.domain.SkillMatrix;
import com.devpath.rule.domain.SkillMatrixPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
class JpaSkillMatrixAdapter implements SkillMatrixPersistencePort {
    private final SkillJpaRepository skills;
    private final SkillMatrixPolicyJpaRepository policies;
    private final SkillPolicyMappingJpaRepository mappings;
    private final SkillMatrixJpaRepository matrices;
    private final SkillAssessmentJpaRepository assessments;
    private final SkillEvidenceLinkJpaRepository evidenceLinks;
    private final SkillRepositoryLinkJpaRepository repositoryLinks;
    private final SkillAssessmentFactJpaRepository facts;
    private final RuleEvaluationJpaRepository evaluations;

    JpaSkillMatrixAdapter(
        SkillJpaRepository skills, SkillMatrixPolicyJpaRepository policies,
        SkillPolicyMappingJpaRepository mappings, SkillMatrixJpaRepository matrices,
        SkillAssessmentJpaRepository assessments, SkillEvidenceLinkJpaRepository evidenceLinks,
        SkillRepositoryLinkJpaRepository repositoryLinks, SkillAssessmentFactJpaRepository facts,
        RuleEvaluationJpaRepository evaluations
    ) {
        this.skills = skills; this.policies = policies; this.mappings = mappings; this.matrices = matrices;
        this.assessments = assessments; this.evidenceLinks = evidenceLinks; this.repositoryLinks = repositoryLinks;
        this.facts = facts; this.evaluations = evaluations;
    }

    @Override
    public SkillMatrixPolicy loadActivePolicy(UUID ruleSetVersionId) {
        SkillMatrixPolicyJpaEntity policy = policies.findByRuleSetVersionIdAndStatus(ruleSetVersionId, "ACTIVE")
            .orElseThrow(() -> new IllegalStateException("No active Skill Matrix policy for rule-set version"));
        List<SkillPolicyMappingJpaEntity> mapped = mappings
            .findAllByPolicyIdAndEnabledTrueOrderBySourceCategoryAsc(policy.id());
        Map<UUID, SkillJpaEntity> skillById = skills.findAllById(mapped.stream().map(SkillPolicyMappingJpaEntity::skillId).toList())
            .stream().collect(Collectors.toMap(SkillJpaEntity::id, Function.identity()));
        List<SkillDefinition> definitions = mapped.stream().map(mapping -> {
            SkillJpaEntity skill = skillById.get(mapping.skillId());
            if (skill == null || !skill.category().equals(mapping.sourceCategory())) {
                throw new IllegalStateException("Skill policy mapping is invalid");
            }
            return new SkillDefinition(skill.id(), skill.stableKey(), skill.name(), RuleCategory.valueOf(mapping.sourceCategory()));
        }).toList();
        return new SkillMatrixPolicy(policy.id(), policy.ruleSetVersionId(), policy.versionLabel(), policy.beginnerMinimum(),
            policy.developingMinimum(), policy.competentMinimum(), policy.strongMinimum(), policy.weaknessMaximum(),
            policy.strengthMinimum(), definitions);
    }

    @Override public Optional<SkillMatrix> findByEvaluationAndOwner(UUID evaluationId, UUID userId) {
        return matrices.findByEvaluationIdAndUserId(evaluationId, userId).map(this::hydrate);
    }
    @Override public Optional<SkillMatrix> findCurrentByOwner(UUID userId) {
        return matrices.findByUserIdAndStatus(userId, "CURRENT").map(this::hydrate);
    }
    @Override public Optional<SkillMatrix> findByIdAndOwner(UUID matrixId, UUID userId) {
        return matrices.findByIdAndUserId(matrixId, userId).map(this::hydrate);
    }
    @Override public List<SkillMatrix> findAllByIdsAndOwner(List<UUID> matrixIds, UUID userId) {
        return matrices.findAllByIdInAndUserId(matrixIds, userId).stream().map(this::hydrate).toList();
    }
    @Override public Optional<UUID> findRepositoryIdByEvaluationAndOwner(UUID evaluationId, UUID userId) {
        return evaluations.findRepositoryIdByEvaluationIdAndUserId(evaluationId, userId);
    }

    @Override
    public SkillMatrix saveAsCurrent(SkillMatrix matrix) {
        Optional<SkillMatrixJpaEntity> existing = matrices.findByEvaluationIdAndUserId(matrix.evaluationId(), matrix.userId());
        if (existing.isPresent()) return hydrate(existing.get());
        matrices.findByUserIdAndStatus(matrix.userId(), "CURRENT").ifPresent(current -> {
            current.supersede(); matrices.saveAndFlush(current);
        });
        matrices.saveAndFlush(new SkillMatrixJpaEntity(matrix));
        for (SkillAssessment assessment : matrix.assessments()) {
            assessments.save(new SkillAssessmentJpaEntity(matrix.matrixId(), assessment));
            for (UUID evidenceId : assessment.evidenceIds()) {
                evidenceLinks.save(new SkillEvidenceLinkJpaEntity(assessment.assessmentId(), evidenceId, assessment.confidence()));
            }
            for (UUID repositoryId : assessment.repositoryIds()) {
                repositoryLinks.save(new SkillRepositoryLinkJpaEntity(assessment.assessmentId(), repositoryId));
            }
            for (int index = 0; index < assessment.recommendationInputFacts().size(); index++) {
                facts.save(new SkillAssessmentFactJpaEntity(assessment.assessmentId(), index,
                    assessment.recommendationInputFacts().get(index)));
            }
        }
        facts.flush();
        return matrix;
    }

    private SkillMatrix hydrate(SkillMatrixJpaEntity matrix) {
        List<SkillAssessmentJpaEntity> stored = assessments.findAllByMatrixIdOrderBySkillIdAsc(matrix.id());
        Map<UUID, SkillJpaEntity> skillById = skills.findAllById(stored.stream().map(SkillAssessmentJpaEntity::skillId).toList())
            .stream().collect(Collectors.toMap(SkillJpaEntity::id, Function.identity()));
        var hydrated = new ArrayList<SkillAssessment>();
        for (SkillAssessmentJpaEntity value : stored) {
            SkillJpaEntity skill = skillById.get(value.skillId());
            var definition = new SkillDefinition(skill.id(), skill.stableKey(), skill.name(), RuleCategory.valueOf(skill.category()));
            List<UUID> evidenceIds = evidenceLinks.findAllByAssessmentIdOrderByEvidenceIdAsc(value.id()).stream()
                .map(SkillEvidenceLinkJpaEntity::evidenceId).toList();
            List<UUID> repositoryIds = repositoryLinks.findAllByAssessmentIdOrderByRepositoryIdAsc(value.id()).stream()
                .map(SkillRepositoryLinkJpaEntity::repositoryId).toList();
            List<String> inputFacts = facts.findAllByAssessmentIdOrderByOrderIndexAsc(value.id()).stream()
                .map(SkillAssessmentFactJpaEntity::value).toList();
            hydrated.add(new SkillAssessment(value.id(), definition, value.score(), SkillLevel.valueOf(value.level()),
                value.confidence(), value.strength(), value.weakness(), value.growthTrend(), value.aggregateReference(),
                evidenceIds, repositoryIds, inputFacts, value.ruleSetVersion()));
        }
        return new SkillMatrix(matrix.id(), matrix.userId(), matrix.evaluationId(), matrix.policyId(), matrix.policyVersion(),
            matrix.ruleSetVersion(), matrix.status(), matrix.generatedAt(), hydrated);
    }
}
