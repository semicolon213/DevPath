package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.devpath.ai.domain.GenerationJob;
import com.devpath.shared.infrastructure.WorkerShutdownGate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AiGenerationWorkerTest {
    @Test
    void doesNotLogTrustedOrProviderContentWhileReportingOnlyFailureCodes() {
        String trusted = "TRUSTED_SENTINEL", context = "CONTEXT_SENTINEL", raw = "RAW_SENTINEL", nested = "NESTED_SENTINEL";
        var service = mock(AiGenerationApplicationService.class);
        var provider = mock(GenerationProviderPort.class);
        var owner = UUID.randomUUID();
        var first = work(owner, trusted, context);
        var second = work(owner, trusted, context);
        var firstExecution = UUID.randomUUID();
        var secondExecution = UUID.randomUUID();
        when(service.claim()).thenReturn(Optional.of(first), Optional.of(second));
        when(service.beginExecution(first)).thenReturn(firstExecution);
        when(service.beginExecution(second)).thenReturn(secondExecution);
        var response = new GenerationProviderResult(raw, 1, 1);
        when(provider.generate(first.prompt(), first.contextPayload())).thenReturn(response).thenThrow(
            new GenerationProviderException("UPSTREAM_FAILURE", new RuntimeException(nested)));
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        root.addAppender(logs);

        try {
            var worker = new AiGenerationWorker(service, provider, new WorkerShutdownGate());
            worker.processNext();
            worker.processNext();
        } finally {
            root.detachAppender(logs);
            logs.stop();
        }

        verify(service).providerSucceeded(eq(first), eq(firstExecution), eq(response), anyLong());
        verify(service).providerFailed(eq(second), eq(secondExecution), eq("UPSTREAM_FAILURE"), anyLong());
        String renderedLogs = logs.list.stream()
            .map(event -> event.getFormattedMessage() + throwableText(event.getThrowableProxy()))
            .reduce("", String::concat);
        assertThat(renderedLogs).doesNotContain(trusted, context, raw, nested);
    }

    @Test
    void logsUnexpectedFailureWithoutExceptionDetails() {
        var service = mock(AiGenerationApplicationService.class);
        var provider = mock(GenerationProviderPort.class);
        var item = work(UUID.randomUUID(), "trusted", "context");
        UUID execution = UUID.randomUUID();
        when(service.claim()).thenReturn(Optional.of(item));
        when(service.beginExecution(item)).thenReturn(execution);
        when(provider.generate(item.prompt(), item.contextPayload()))
            .thenThrow(new IllegalStateException("sensitive failure"));
        Logger logger = (Logger) LoggerFactory.getLogger(AiGenerationWorker.class);
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        logger.addAppender(logs);

        try {
            new AiGenerationWorker(service, provider, new WorkerShutdownGate()).processNext();
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }

        verify(service).providerFailed(eq(item), eq(execution), eq("AI_GENERATION_FAILED"), anyLong());
        assertThat(logs.list).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage()).isEqualTo(
                "AI generation attempt failed job_id=" + item.job().id() + " error_type=IllegalStateException");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    private static String throwableText(IThrowableProxy throwable) {
        return throwable == null ? "" : throwable.getMessage() + throwableText(throwable.getCause());
    }

    private static GenerationWorkItem work(UUID owner, String trusted, String context) {
        var job = GenerationJob.queue(owner, UUID.randomUUID(), UUID.randomUUID().toString(),
            AiGenerationApplicationService.SKILL_EXPLANATION_TASK, Instant.EPOCH);
        return new GenerationWorkItem(job, UUID.randomUUID(), null, null, null, trusted, context, trusted);
    }
}
