package com.devpath.ai.application;

import com.devpath.shared.infrastructure.WorkerShutdownGate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "devpath.runtime.worker-enabled", havingValue = "true")
class AiGenerationWorker {
    private static final Logger log = LoggerFactory.getLogger(AiGenerationWorker.class);
    private final AiGenerationApplicationService service;
    private final GenerationProviderPort provider;
    private final WorkerShutdownGate shutdownGate;

    AiGenerationWorker(
        AiGenerationApplicationService service, GenerationProviderPort provider, WorkerShutdownGate shutdownGate
    ) {
        this.service = service; this.provider = provider; this.shutdownGate = shutdownGate;
    }

    @Scheduled(
        fixedDelayString = "${devpath.jobs.ai-generation.poll-interval:1000}",
        initialDelayString = "${devpath.jobs.ai-generation.initial-delay:2500}",
        scheduler = "workerTaskScheduler"
    )
    void processNext() {
        if (!shutdownGate.acceptingClaims()) return;
        service.claim().ifPresent(item -> {
            UUID executionId = service.beginExecution(item);
            long started = System.nanoTime();
            try {
                GenerationProviderResult result = provider.generate(item.prompt(), item.contextPayload());
                service.providerSucceeded(item, executionId, result, elapsedMillis(started));
            } catch (GenerationProviderException exception) {
                service.providerFailed(item, executionId, exception.code(), elapsedMillis(started));
            } catch (RuntimeException exception) {
                service.providerFailed(item, executionId, "AI_GENERATION_FAILED", elapsedMillis(started));
                log.warn("AI generation attempt failed job_id={} error_type={}",
                    item.job().id(), exception.getClass().getSimpleName());
            }
        });
    }

    private static long elapsedMillis(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }
}
