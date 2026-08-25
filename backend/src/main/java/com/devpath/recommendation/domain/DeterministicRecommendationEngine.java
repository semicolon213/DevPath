package com.devpath.recommendation.domain;

import com.devpath.career.domain.CareerReadiness;
import com.devpath.career.domain.GapState;
import com.devpath.career.domain.SkillGap;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class DeterministicRecommendationEngine {
    public RecommendationSet generate(UUID setId, CareerReadiness readiness, RecommendationPolicy policy, Instant now) {
        if (!readiness.careerProfileVersionId().equals(policy.careerProfileVersionId())) {
            throw new IllegalArgumentException("recommendation policy does not match readiness profile");
        }
        var candidates = new ArrayList<Candidate>();
        for (SkillGap gap : readiness.skillGaps()) {
            if (gap.gapState() == GapState.SUFFICIENT || gap.gapState() == GapState.STRONG) continue;
            RecommendationTemplate template = policy.templates().get(gap.category());
            if (template == null) throw new IllegalArgumentException("missing recommendation template " + gap.category());
            candidates.add(new Candidate(gap, template, priority(gap.gapState())));
        }
        candidates.sort(Comparator.comparingInt((Candidate value) -> value.priority.ordinal())
            .thenComparingInt(value -> value.template.prerequisiteOrder())
            .thenComparing(value -> value.gap.careerWeight(), Comparator.reverseOrder())
            .thenComparingInt(value -> value.template.effortHours())
            .thenComparing(value -> value.gap.category().name()));
        List<Recommendation> recommendations = new ArrayList<>();
        for (int position = 0; position < candidates.size(); position++) {
            Candidate value = candidates.get(position);
            UUID id = stableId(setId + ":" + value.gap.gapId());
            recommendations.add(new Recommendation(id, value.gap.gapId(), value.gap.category(), value.template.type(),
                value.priority, value.template.rationaleCode(), value.template.title(),
                value.template.completionCriteria(), value.template.expectedEvidence(), value.gap.evidenceIds(),
                value.template.effortHours(), position, "PROPOSED"));
        }
        return new RecommendationSet(setId, readiness.userId(), readiness.readinessId(), policy.policyId(),
            policy.versionLabel(), "PUBLISHED", recommendations, now);
    }

    private RecommendationPriority priority(GapState state) {
        return switch (state) {
            case MISSING -> RecommendationPriority.CRITICAL;
            case WEAK -> RecommendationPriority.HIGH;
            case PARTIAL -> RecommendationPriority.MEDIUM;
            case SUFFICIENT, STRONG -> throw new IllegalArgumentException("eligible gap required");
        };
    }

    static UUID stableId(String basis) { return UUID.nameUUIDFromBytes(basis.getBytes(StandardCharsets.UTF_8)); }
    private record Candidate(SkillGap gap, RecommendationTemplate template, RecommendationPriority priority) {}
}
