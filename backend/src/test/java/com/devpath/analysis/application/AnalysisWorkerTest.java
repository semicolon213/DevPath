package com.devpath.analysis.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.devpath.career.application.CareerReadinessApplicationService;
import com.devpath.recommendation.application.RecommendationApplicationService;
import com.devpath.rule.application.CompletedRuleEvaluationApplicationService;
import com.devpath.shared.infrastructure.WorkerShutdownGate;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextClosedEvent;

class AnalysisWorkerTest {
    @Test
    void doesNotClaimNewWorkAfterShutdownBegins() {
        var analyses = mock(AnalysisApplicationService.class);
        var rules = mock(CompletedRuleEvaluationApplicationService.class);
        var readiness = mock(CareerReadinessApplicationService.class);
        var recommendations = mock(RecommendationApplicationService.class);
        var gate = new WorkerShutdownGate();
        gate.stopAcceptingClaims(mock(ContextClosedEvent.class));

        new AnalysisWorker(analyses, rules, readiness, recommendations, Clock.systemUTC(), gate).processNext();

        verifyNoInteractions(analyses, rules, readiness, recommendations);
    }
}
