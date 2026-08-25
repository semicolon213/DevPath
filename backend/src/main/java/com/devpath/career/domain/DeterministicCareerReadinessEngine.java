package com.devpath.career.domain;

import com.devpath.rule.domain.RuleCategory;
import com.devpath.rule.domain.SkillAssessment;
import com.devpath.rule.domain.SkillLevel;
import com.devpath.rule.domain.SkillMatrix;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

public final class DeterministicCareerReadinessEngine {
    private static final int SCALE = 2;

    public CareerReadiness evaluate(
        UUID readinessId,
        SkillMatrix matrix,
        CareerProfile profile,
        CareerReadinessPolicy policy,
        Instant now
    ) {
        if (!profile.profileVersionId().equals(policy.careerProfileVersionId())) {
            throw new IllegalArgumentException("readiness policy does not match the career profile");
        }
        var byCategory = new EnumMap<RuleCategory, SkillAssessment>(RuleCategory.class);
        matrix.assessments().forEach(value -> byCategory.put(value.skill().category(), value));
        List<String> unavailable = policy.categoryWeights().keySet().stream()
            .filter(category -> !byCategory.containsKey(category)).map(Enum::name).sorted().toList();

        List<SkillGap> gaps = new ArrayList<>();
        for (var entry : policy.categoryWeights().entrySet()) {
            SkillAssessment assessment = byCategory.get(entry.getKey());
            if (assessment == null) continue;
            UUID gapId = UUID.nameUUIDFromBytes((readinessId + ":" + assessment.assessmentId())
                .getBytes(StandardCharsets.UTF_8));
            gaps.add(new SkillGap(gapId, assessment.assessmentId(), assessment.skill().skillId(),
                assessment.skill().stableKey(), entry.getKey(), assessment.score(), assessment.level().name(),
                policy.expectedMinimum(), policy.gapState(assessment.score()), entry.getValue(), assessment.evidenceIds()));
        }
        gaps.sort(Comparator.comparingInt((SkillGap value) -> value.gapState().ordinal())
            .thenComparing(SkillGap::careerWeight, Comparator.reverseOrder())
            .thenComparing(value -> value.category().name()));

        BigDecimal confidence = gaps.stream()
            .map(gap -> byCategory.get(gap.category()).confidence().multiply(gap.careerWeight())
                .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP))
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
        if (!unavailable.isEmpty()) {
            return new CareerReadiness(readinessId, matrix.userId(), matrix.matrixId(), profile.careerId(),
                profile.profileVersionId(), profile.profileVersion(), policy.policyId(), policy.versionLabel(),
                matrix.ruleSetVersion(), CareerReadinessStatus.INSUFFICIENT_EVIDENCE, null, null, confidence,
                unavailable, gaps, now);
        }
        BigDecimal score = gaps.stream()
            .map(gap -> gap.actualScore().multiply(gap.careerWeight())
                .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP))
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
        SkillLevel level = level(score, policy);
        return new CareerReadiness(readinessId, matrix.userId(), matrix.matrixId(), profile.careerId(),
            profile.profileVersionId(), profile.profileVersion(), policy.policyId(), policy.versionLabel(),
            matrix.ruleSetVersion(), CareerReadinessStatus.COMPLETED, score, level.name(), confidence,
            List.of(), gaps, now);
    }

    private SkillLevel level(BigDecimal score, CareerReadinessPolicy policy) {
        if (score.signum() == 0) return SkillLevel.NONE;
        if (score.compareTo(policy.strongMinimum()) >= 0) return SkillLevel.STRONG;
        if (score.compareTo(policy.expectedMinimum()) >= 0) return SkillLevel.COMPETENT;
        if (score.compareTo(policy.developingMinimum()) >= 0) return SkillLevel.DEVELOPING;
        return SkillLevel.BEGINNER;
    }
}
