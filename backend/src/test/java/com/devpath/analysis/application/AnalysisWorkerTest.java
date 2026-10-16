package com.devpath.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.devpath.analysis.domain.AnalysisJob;
import com.devpath.career.application.CareerReadinessApplicationService;
import com.devpath.recommendation.application.RecommendationApplicationService;
import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.rule.application.CompletedRuleEvaluationApplicationService;
import com.devpath.shared.infrastructure.WorkerShutdownGate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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

    @Test
    void logsUnexpectedFailureWithoutExceptionDetails() {
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        var analyses = mock(AnalysisApplicationService.class);
        var rules = mock(CompletedRuleEvaluationApplicationService.class);
        var readiness = mock(CareerReadinessApplicationService.class);
        var recommendations = mock(RecommendationApplicationService.class);
        var job = mock(AnalysisJob.class);
        var snapshot = mock(RepositorySnapshot.class);
        var item = new AnalysisWorkItem(job, snapshot);
        UUID jobId = UUID.randomUUID();
        when(job.id()).thenReturn(jobId);
        when(analyses.claim(now)).thenReturn(Optional.of(item));
        when(rules.evaluateAndPersistWithMatrix(snapshot, now))
            .thenThrow(new IllegalStateException("sensitive failure"));
        var logs = appender();

        try {
            new AnalysisWorker(analyses, rules, readiness, recommendations,
                Clock.fixed(now, ZoneOffset.UTC), new WorkerShutdownGate()).processNext();
        } finally {
            logger().detachAppender(logs);
            logs.stop();
        }

        verify(analyses).fail(item, "ANALYSIS_FAILED", "Deterministic analysis failed safely.", now);
        assertThat(logs.list).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage())
                .isEqualTo("Analysis attempt failed job_id=" + jobId + " error_type=IllegalStateException");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(AnalysisWorker.class);
    }

    private static ListAppender<ILoggingEvent> appender() {
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        logger().addAppender(logs);
        return logs;
    }
}
