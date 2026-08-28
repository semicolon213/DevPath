package com.devpath.repository.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class RepositoryWorkerConfiguration {
    @Bean("workerTaskScheduler")
    ThreadPoolTaskScheduler workerTaskScheduler(
        @Value("${devpath.jobs.shutdown-timeout:10m}") Duration shutdownTimeout
    ) {
        if (shutdownTimeout.isZero() || shutdownTimeout.isNegative()
            || shutdownTimeout.compareTo(Duration.ofMinutes(15)) > 0) {
            throw new IllegalArgumentException("Worker shutdown timeout must be positive and no longer than its lease");
        }
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("devpath-worker-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(Math.toIntExact(shutdownTimeout.toSeconds()));
        return scheduler;
    }
}
