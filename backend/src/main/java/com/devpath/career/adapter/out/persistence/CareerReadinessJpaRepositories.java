package com.devpath.career.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CareerReadinessPolicyJpaRepository extends JpaRepository<CareerReadinessPolicyJpaEntity, UUID> {
    Optional<CareerReadinessPolicyJpaEntity> findByStatus(String status);
}

interface CareerReadinessWeightJpaRepository extends JpaRepository<CareerReadinessWeightJpaEntity, CareerReadinessWeightId> {
    List<CareerReadinessWeightJpaEntity> findAllByPolicyIdAndCareerProfileVersionId(UUID policyId, UUID profileId);
}

interface CareerReadinessJpaRepository extends JpaRepository<CareerReadinessJpaEntity, UUID> {
    Optional<CareerReadinessJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<CareerReadinessJpaEntity> findByUserIdAndSkillMatrixIdAndCareerProfileVersionIdAndPolicyId(
        UUID userId, UUID skillMatrixId, UUID careerProfileVersionId, UUID policyId
    );
    Optional<CareerReadinessJpaEntity> findFirstByUserIdAndSkillMatrixIdAndCareerProfileVersionIdOrderByAssessedAtDescIdDesc(
        UUID userId, UUID skillMatrixId, UUID careerProfileVersionId
    );
}

interface SkillGapJpaRepository extends JpaRepository<SkillGapJpaEntity, UUID> {
    List<SkillGapJpaEntity> findAllByReadinessId(UUID readinessId);
}

interface SkillGapEvidenceJpaRepository extends JpaRepository<SkillGapEvidenceJpaEntity, SkillGapEvidenceId> {
    List<SkillGapEvidenceJpaEntity> findAllByGapIdIn(List<UUID> gapIds);
}
