package com.devpath.rule.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SkillJpaRepository extends JpaRepository<SkillJpaEntity, UUID> {}
interface SkillMatrixPolicyJpaRepository extends JpaRepository<SkillMatrixPolicyJpaEntity, UUID> {
    Optional<SkillMatrixPolicyJpaEntity> findByRuleSetVersionIdAndStatus(UUID ruleSetVersionId, String status);
}
interface SkillPolicyMappingJpaRepository extends JpaRepository<SkillPolicyMappingJpaEntity, UUID> {
    List<SkillPolicyMappingJpaEntity> findAllByPolicyIdAndEnabledTrueOrderBySourceCategoryAsc(UUID policyId);
}
interface SkillMatrixJpaRepository extends JpaRepository<SkillMatrixJpaEntity, UUID> {
    Optional<SkillMatrixJpaEntity> findByEvaluationIdAndUserId(UUID evaluationId, UUID userId);
    Optional<SkillMatrixJpaEntity> findByUserIdAndStatus(UUID userId, String status);
    Optional<SkillMatrixJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    List<SkillMatrixJpaEntity> findAllByIdInAndUserId(List<UUID> ids, UUID userId);
}
interface SkillAssessmentJpaRepository extends JpaRepository<SkillAssessmentJpaEntity, UUID> {
    List<SkillAssessmentJpaEntity> findAllByMatrixIdOrderBySkillIdAsc(UUID matrixId);
}
interface SkillEvidenceLinkJpaRepository extends JpaRepository<SkillEvidenceLinkJpaEntity, UUID> {
    List<SkillEvidenceLinkView> findAllByAssessmentIdInOrderByAssessmentIdAscEvidenceIdAsc(List<UUID> assessmentIds);
}
interface SkillRepositoryLinkJpaRepository extends JpaRepository<SkillRepositoryLinkJpaEntity, UUID> {
    List<SkillRepositoryLinkView> findAllByAssessmentIdInOrderByAssessmentIdAscRepositoryIdAsc(List<UUID> assessmentIds);
}
interface SkillAssessmentFactJpaRepository extends JpaRepository<SkillAssessmentFactJpaEntity, UUID> {
    List<SkillAssessmentFactView> findAllByAssessmentIdInOrderByAssessmentIdAscOrderIndexAsc(List<UUID> assessmentIds);
}

interface SkillEvidenceLinkView {
    UUID getAssessmentId();
    UUID getEvidenceId();
}

interface SkillRepositoryLinkView {
    UUID getAssessmentId();
    UUID getRepositoryId();
}

interface SkillAssessmentFactView {
    UUID getAssessmentId();
    String getValue();
}
