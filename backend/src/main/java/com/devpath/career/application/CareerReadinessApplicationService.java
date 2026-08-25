package com.devpath.career.application;

import com.devpath.career.domain.CareerReadiness;
import com.devpath.career.domain.DeterministicCareerReadinessEngine;
import com.devpath.career.domain.SkillGap;
import com.devpath.identity.application.UserPreferenceRepositoryPort;
import com.devpath.identity.domain.PreferenceType;
import com.devpath.identity.domain.UserId;
import com.devpath.rule.application.SkillMatrixPersistencePort;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareerReadinessApplicationService {
    private static final Set<String> MVP_CAREERS = Set.of("backend", "frontend");
    private final UserPreferenceRepositoryPort preferences;
    private final CareerCatalogPort careers;
    private final SkillMatrixPersistencePort matrices;
    private final CareerReadinessPersistencePort readiness;
    private final DeterministicCareerReadinessEngine engine = new DeterministicCareerReadinessEngine();

    public CareerReadinessApplicationService(
        UserPreferenceRepositoryPort preferences,
        CareerCatalogPort careers,
        SkillMatrixPersistencePort matrices,
        CareerReadinessPersistencePort readiness
    ) {
        this.preferences = preferences;
        this.careers = careers;
        this.matrices = matrices;
        this.readiness = readiness;
    }

    @Transactional
    public Optional<CareerReadiness> generateForSelectedCareer(UUID userId, UUID skillMatrixId, Instant now) {
        var selected = preferences.findActive(new UserId(userId), PreferenceType.CAREER);
        if (selected.isEmpty() || !MVP_CAREERS.contains(selected.get().selectedValue())) return Optional.empty();
        var profile = careers.findSupportedById(selected.get().selectedValue())
            .orElseThrow(CareerNotFoundException::new);
        var matrix = matrices.findByIdAndOwner(skillMatrixId, userId)
            .orElseThrow(CareerReadinessNotFoundException::new);
        var policy = readiness.loadActivePolicy(profile.profileVersionId());
        return Optional.of(readiness.findByBasis(userId, skillMatrixId, profile.profileVersionId(), policy.policyId())
            .orElseGet(() -> readiness.save(engine.evaluate(UUID.randomUUID(), matrix, profile, policy, now))));
    }

    @Transactional(readOnly = true)
    public CareerReadinessView getCurrent(UUID userId) {
        var selected = preferences.findActive(new UserId(userId), PreferenceType.CAREER)
            .orElseThrow(CareerReadinessNotFoundException::new);
        if (!MVP_CAREERS.contains(selected.selectedValue())) throw new CareerReadinessNotFoundException();
        var profile = careers.findSupportedById(selected.selectedValue()).orElseThrow(CareerNotFoundException::new);
        var matrix = matrices.findCurrentByOwner(userId).orElseThrow(CareerReadinessNotFoundException::new);
        return readiness.findCurrentByOwner(userId, matrix.matrixId(), profile.profileVersionId())
            .map(this::toView).orElseThrow(CareerReadinessNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public CareerReadinessView get(UUID userId, UUID readinessId) {
        return readiness.findByIdAndOwner(readinessId, userId).map(this::toView)
            .orElseThrow(CareerReadinessNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public SkillGapListView getGaps(UUID userId, UUID readinessId) {
        CareerReadiness result = readiness.findByIdAndOwner(readinessId, userId)
            .orElseThrow(CareerReadinessNotFoundException::new);
        return new SkillGapListView(readinessId, result.skillGaps().stream().map(this::toView).toList());
    }

    private CareerReadinessView toView(CareerReadiness value) {
        return new CareerReadinessView(value.readinessId(), value.skillMatrixId(), value.careerId(),
            value.careerProfileVersionId(), value.careerProfileVersion(), value.policyVersion(), value.ruleSetVersion(),
            value.status().name(), value.readinessScore(), value.readinessLevel(), value.confidence(),
            value.unavailableCategories(), value.skillGaps().stream().map(this::toView).toList(), value.assessedAt());
    }

    private SkillGapView toView(SkillGap value) {
        return new SkillGapView(value.gapId(), value.skillId(), value.skillKey(), value.category().name(),
            value.actualScore(), value.actualLevel(), value.expectedMinimum(), value.gapState().name(),
            value.careerWeight(), value.evidenceIds());
    }
}
