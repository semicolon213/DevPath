package com.devpath.career.adapter.out.persistence;

import com.devpath.career.application.CareerReadinessPersistencePort;
import com.devpath.career.domain.CareerReadiness;
import com.devpath.career.domain.CareerReadinessPolicy;
import com.devpath.career.domain.CareerReadinessStatus;
import com.devpath.career.domain.GapState;
import com.devpath.career.domain.SkillGap;
import com.devpath.rule.domain.RuleCategory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JpaCareerReadinessAdapter implements CareerReadinessPersistencePort {
    private final CareerReadinessPolicyJpaRepository policies;
    private final CareerReadinessWeightJpaRepository weights;
    private final CareerReadinessJpaRepository readiness;
    private final SkillGapJpaRepository gaps;
    private final SkillGapEvidenceJpaRepository evidenceLinks;
    private final CareerCatalogJpaRepository careers;

    public JpaCareerReadinessAdapter(
        CareerReadinessPolicyJpaRepository policies,
        CareerReadinessWeightJpaRepository weights,
        CareerReadinessJpaRepository readiness,
        SkillGapJpaRepository gaps,
        SkillGapEvidenceJpaRepository evidenceLinks,
        CareerCatalogJpaRepository careers
    ) {
        this.policies = policies; this.weights = weights; this.readiness = readiness;
        this.gaps = gaps; this.evidenceLinks = evidenceLinks; this.careers = careers;
    }

    @Override
    public CareerReadinessPolicy loadActivePolicy(UUID careerProfileVersionId) {
        var policy = policies.findByStatus("ACTIVE").orElseThrow(() -> new IllegalStateException("No active readiness policy"));
        Map<RuleCategory, java.math.BigDecimal> values = new EnumMap<>(RuleCategory.class);
        weights.findAllByPolicyIdAndCareerProfileVersionId(policy.id, careerProfileVersionId)
            .forEach(value -> values.put(RuleCategory.valueOf(value.category), value.weight));
        return new CareerReadinessPolicy(policy.id, policy.versionLabel, careerProfileVersionId,
            policy.expectedMinimum, policy.developingMinimum, policy.strongMinimum, values);
    }

    @Override
    public Optional<CareerReadiness> findByBasis(
        UUID userId, UUID skillMatrixId, UUID careerProfileVersionId, UUID policyId
    ) {
        return readiness.findByUserIdAndSkillMatrixIdAndCareerProfileVersionIdAndPolicyId(
            userId, skillMatrixId, careerProfileVersionId, policyId).map(this::map);
    }

    @Override
    public Optional<CareerReadiness> findByIdAndOwner(UUID readinessId, UUID userId) {
        return readiness.findByIdAndUserId(readinessId, userId).map(this::map);
    }

    @Override
    public Optional<CareerReadiness> findCurrentByOwner(UUID userId, UUID skillMatrixId, UUID careerProfileVersionId) {
        return readiness.findFirstByUserIdAndSkillMatrixIdAndCareerProfileVersionIdOrderByAssessedAtDescIdDesc(
            userId, skillMatrixId, careerProfileVersionId).map(this::map);
    }

    @Override
    public CareerReadiness save(CareerReadiness value) {
        CareerReadinessJpaEntity entity = new CareerReadinessJpaEntity();
        entity.id = value.readinessId(); entity.userId = value.userId(); entity.skillMatrixId = value.skillMatrixId();
        entity.careerProfileVersionId = value.careerProfileVersionId(); entity.policyId = value.policyId();
        entity.status = value.status().name(); entity.readinessScore = value.readinessScore();
        entity.readinessLevel = value.readinessLevel(); entity.confidence = value.confidence();
        entity.ruleSetVersion = value.ruleSetVersion(); entity.unavailableCategories = value.unavailableCategories();
        entity.assessedAt = value.assessedAt();
        readiness.save(entity);
        List<SkillGapJpaEntity> savedGaps = new ArrayList<>();
        List<SkillGapEvidenceJpaEntity> links = new ArrayList<>();
        for (SkillGap gap : value.skillGaps()) {
            SkillGapJpaEntity item = new SkillGapJpaEntity();
            item.id = gap.gapId(); item.readinessId = value.readinessId(); item.skillAssessmentId = gap.skillAssessmentId();
            item.skillId = gap.skillId(); item.skillKey = gap.skillKey();
            item.category = gap.category().name(); item.actualScore = gap.actualScore(); item.actualLevel = gap.actualLevel();
            item.expectedMinimum = gap.expectedMinimum(); item.gapState = gap.gapState().name();
            item.careerWeight = gap.careerWeight(); savedGaps.add(item);
            gap.evidenceIds().forEach(evidenceId -> links.add(new SkillGapEvidenceJpaEntity(gap.gapId(), evidenceId)));
        }
        gaps.saveAll(savedGaps);
        evidenceLinks.saveAll(links);
        return value;
    }

    private CareerReadiness map(CareerReadinessJpaEntity entity) {
        CareerProfileVersionJpaEntity profile = careers.findProfileById(entity.careerProfileVersionId)
            .orElseThrow(() -> new IllegalStateException("Career readiness profile is missing"));
        CareerReadinessPolicyJpaEntity policy = policies.findById(entity.policyId)
            .orElseThrow(() -> new IllegalStateException("Career readiness policy is missing"));
        List<SkillGapJpaEntity> gapEntities = gaps.findAllByReadinessId(entity.id);
        List<UUID> gapIds = gapEntities.stream().map(value -> value.id).toList();
        Map<UUID, List<UUID>> evidenceByGap = gapIds.isEmpty() ? Map.of() : evidenceLinks.findAllByGapIdIn(gapIds).stream()
            .collect(Collectors.groupingBy(value -> value.gapId,
                Collectors.mapping(value -> value.evidenceId, Collectors.toList())));
        List<SkillGap> mapped = gapEntities.stream().map(value -> new SkillGap(value.id, value.skillAssessmentId,
            value.skillId, value.skillKey, RuleCategory.valueOf(value.category),
            value.actualScore, value.actualLevel, value.expectedMinimum, GapState.valueOf(value.gapState),
            value.careerWeight, evidenceByGap.getOrDefault(value.id, List.of())))
            .sorted(Comparator.comparingInt((SkillGap value) -> value.gapState().ordinal())
                .thenComparing(SkillGap::careerWeight, Comparator.reverseOrder())
                .thenComparing(value -> value.category().name()))
            .toList();
        return new CareerReadiness(entity.id, entity.userId, entity.skillMatrixId, profile.careerId,
            profile.id, profile.versionLabel, policy.id, policy.versionLabel, entity.ruleSetVersion,
            CareerReadinessStatus.valueOf(entity.status), entity.readinessScore, entity.readinessLevel,
            entity.confidence, entity.unavailableCategories, mapped, entity.assessedAt);
    }

}
