package com.devpath.recommendation.application;

import com.devpath.career.domain.CareerReadiness;
import com.devpath.career.domain.CareerReadinessStatus;
import com.devpath.learning.application.LearningRoadmapView;
import com.devpath.learning.application.LearningRoadmapListView;
import com.devpath.learning.application.LearningAuditEvent;
import com.devpath.learning.application.LearningAuditPort;
import com.devpath.learning.application.RoadmapMilestoneView;
import com.devpath.learning.application.RoadmapStepView;
import com.devpath.learning.domain.DeterministicRoadmapEngine;
import com.devpath.learning.domain.LearningRoadmap;
import com.devpath.recommendation.domain.DeterministicRecommendationEngine;
import com.devpath.recommendation.domain.Recommendation;
import com.devpath.recommendation.domain.RecommendationSet;
import java.time.Instant;
import java.time.Clock;
import java.util.UUID;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationApplicationService {
    private final RecommendationPersistencePort persistence;
    private final LearningAuditPort audit;
    private final Clock clock;
    private final DeterministicRecommendationEngine recommendationEngine = new DeterministicRecommendationEngine();
    private final DeterministicRoadmapEngine roadmapEngine = new DeterministicRoadmapEngine();

    public RecommendationApplicationService(RecommendationPersistencePort persistence, LearningAuditPort audit, Clock clock) { this.persistence = persistence; this.audit = audit; this.clock = clock; }

    @Transactional
    public Optional<RecommendationSet> generate(CareerReadiness readiness, Instant now) {
        if (readiness.status() != CareerReadinessStatus.COMPLETED) return Optional.empty();
        var policy = persistence.loadActivePolicy(readiness.careerProfileVersionId());
        RecommendationSet set = persistence.findSetByBasis(readiness.userId(), readiness.readinessId(), policy.policyId())
            .orElseGet(() -> {
                persistence.supersedeCurrent(readiness.userId(), now);
                return persistence.saveSet(recommendationEngine.generate(UUID.randomUUID(), readiness, policy, now));
            });
        var roadmapPolicy = persistence.loadActiveRoadmapPolicy(policy.policyId(), readiness.careerProfileVersionId());
        persistence.findRoadmapByBasis(readiness.userId(), set.recommendationSetId(), roadmapPolicy.policyId())
            .orElseGet(() -> persistence.saveRoadmap(
                roadmapEngine.generate(UUID.randomUUID(), set, roadmapPolicy, now)));
        return Optional.of(set);
    }

    @Transactional(readOnly = true)
    public RecommendationSetView getCurrent(UUID userId) {
        return persistence.findCurrentSet(userId).map(this::view).orElseThrow(RecommendationNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public RecommendationSetView get(UUID userId, UUID setId) {
        return persistence.findSetByIdAndOwner(setId, userId).map(this::view)
            .orElseThrow(RecommendationNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public RecommendationSetListView listRecommendationSets(UUID userId) {
        return new RecommendationSetListView(persistence.findSetsByOwner(userId).stream().map(this::view).toList());
    }

    @Transactional(readOnly = true)
    public RecommendationView getRecommendation(UUID userId, UUID recommendationId) {
        return persistence.findSetByRecommendationIdAndOwner(recommendationId, userId)
            .flatMap(set -> set.recommendations().stream()
                .filter(item -> item.recommendationId().equals(recommendationId)).findFirst())
            .map(this::view).orElseThrow(RecommendationNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public RecommendationEvidenceListView getRecommendationEvidence(UUID userId, UUID recommendationId) {
        getRecommendation(userId, recommendationId);
        return new RecommendationEvidenceListView(recommendationId,
            persistence.findEvidenceByRecommendationAndOwner(recommendationId, userId).stream()
                .map(item -> new RecommendationEvidenceView(item.evidenceId(), item.evidenceType(),
                    item.sourceReference(), item.observedFactSummary(), item.confidence(), item.createdAt())).toList());
    }

    @Transactional(readOnly = true)
    public LearningRoadmapView getActiveRoadmap(UUID userId) {
        return persistence.findActiveRoadmap(userId).map(this::view).orElseThrow(RecommendationNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public LearningRoadmapView getRoadmap(UUID userId, UUID roadmapId) {
        return persistence.findRoadmapByIdAndOwner(roadmapId, userId).map(this::view)
            .orElseThrow(RecommendationNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public LearningRoadmapListView listRoadmaps(UUID userId) {
        return new LearningRoadmapListView(persistence.findRoadmapsByOwner(userId).stream().map(this::view).toList());
    }

    @Transactional
    public LearningRoadmapView archiveRoadmap(UUID userId, UUID roadmapId) {
        LearningRoadmap roadmap = persistence.findRoadmapByIdAndOwner(roadmapId, userId)
            .orElseThrow(RecommendationNotFoundException::new);
        if ("ARCHIVED".equals(roadmap.status())) return view(roadmap);
        Instant now = clock.instant();
        LearningRoadmap archived = persistence.updateRoadmap(roadmap.archive(now));
        audit.record(LearningAuditEvent.ROADMAP_ARCHIVED, userId, roadmapId, now);
        return view(archived);
    }

    private RecommendationSetView view(RecommendationSet set) {
        return new RecommendationSetView(set.recommendationSetId(), set.careerReadinessId(), set.policyVersion(),
            set.status(), set.recommendations().stream().map(this::view).toList(), set.generatedAt());
    }

    private RecommendationView view(Recommendation value) {
        return new RecommendationView(value.recommendationId(), value.skillGapId(), value.category().name(),
            value.type().name(), value.priority().name(), value.rationaleCode(), value.title(),
            value.completionCriteria(), value.expectedEvidence(), value.evidenceIds(), value.effortHours(),
            value.position(), value.status());
    }

    private LearningRoadmapView view(LearningRoadmap value) {
        return new LearningRoadmapView(value.roadmapId(), value.recommendationSetId(), value.policyVersion(),
            value.status(), value.progressPercent(), value.milestones().stream().map(item -> new RoadmapMilestoneView(
                item.milestoneId(), item.position(), item.category().name(), item.title(), item.status())).toList(),
            value.steps().stream().map(item -> new RoadmapStepView(item.stepId(), item.milestoneId(),
                item.recommendationId(), item.position(), item.category().name(), item.title(), item.difficulty(),
                item.effortHours(), item.prerequisiteStepIds(), item.completionCriteria(), item.expectedEvidence(),
                item.status())).toList(), value.generatedAt(), value.updatedAt());
    }
}
