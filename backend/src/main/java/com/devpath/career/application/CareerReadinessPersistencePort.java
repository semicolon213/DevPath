package com.devpath.career.application;

import com.devpath.career.domain.CareerReadiness;
import com.devpath.career.domain.CareerReadinessPolicy;
import java.util.Optional;
import java.util.UUID;

public interface CareerReadinessPersistencePort {
    CareerReadinessPolicy loadActivePolicy(UUID careerProfileVersionId);
    Optional<CareerReadiness> findByBasis(
        UUID userId, UUID skillMatrixId, UUID careerProfileVersionId, UUID policyId
    );
    Optional<CareerReadiness> findByIdAndOwner(UUID readinessId, UUID userId);
    Optional<CareerReadiness> findCurrentByOwner(UUID userId, UUID skillMatrixId, UUID careerProfileVersionId);
    CareerReadiness save(CareerReadiness readiness);
}
