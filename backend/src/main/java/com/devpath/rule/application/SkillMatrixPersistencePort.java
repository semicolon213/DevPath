package com.devpath.rule.application;

import com.devpath.rule.domain.SkillMatrix;
import com.devpath.rule.domain.SkillMatrixPolicy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillMatrixPersistencePort {
    SkillMatrixPolicy loadActivePolicy(UUID ruleSetVersionId);
    Optional<SkillMatrix> findByEvaluationAndOwner(UUID evaluationId, UUID userId);
    Optional<SkillMatrix> findCurrentByOwner(UUID userId);
    Optional<SkillMatrix> findByIdAndOwner(UUID matrixId, UUID userId);
    List<SkillMatrix> findAllByIdsAndOwner(List<UUID> matrixIds, UUID userId);
    Optional<UUID> findRepositoryIdByEvaluationAndOwner(UUID evaluationId, UUID userId);
    SkillMatrix saveAsCurrent(SkillMatrix matrix);
}
