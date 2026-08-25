package com.devpath.analysis.application;

import com.devpath.rule.application.CompletedRuleEvaluationApplicationService;
import com.devpath.career.application.CareerReadinessApplicationService;
import com.devpath.recommendation.application.RecommendationApplicationService;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "devpath.runtime.worker-enabled", havingValue = "true")
class AnalysisWorker {
    private final AnalysisApplicationService analyses;
    private final CompletedRuleEvaluationApplicationService ruleEvaluations;
    private final CareerReadinessApplicationService careerReadiness;
    private final RecommendationApplicationService recommendations;
    private final Clock clock;

    AnalysisWorker(
        AnalysisApplicationService analyses,
        CompletedRuleEvaluationApplicationService ruleEvaluations,
        CareerReadinessApplicationService careerReadiness,
        RecommendationApplicationService recommendations,
        Clock clock
    ) {
        this.analyses = analyses;
        this.ruleEvaluations = ruleEvaluations;
        this.careerReadiness = careerReadiness;
        this.recommendations = recommendations;
        this.clock = clock;
    }

    @Scheduled(
        fixedDelayString = "${devpath.jobs.analysis.poll-interval:1000}",
        initialDelayString = "${devpath.jobs.analysis.initial-delay:1500}"
    )
    void processNext() {
        analyses.claim(clock.instant()).ifPresent(item -> {
            try {
                var completion = ruleEvaluations.evaluateAndPersistWithMatrix(item.snapshot(), clock.instant());
                careerReadiness.generateForSelectedCareer(
                    item.job().userId(), completion.skillMatrix().matrixId(), clock.instant()
                ).ifPresent(result -> recommendations.generate(result, clock.instant()));
                analyses.complete(item, completion.evaluation().id(), completion.skillMatrix().matrixId(), clock.instant());
            } catch (RuntimeException exception) {
                analyses.fail(item, "ANALYSIS_FAILED", "Deterministic analysis failed safely.", clock.instant());
            }
        });
    }
}
