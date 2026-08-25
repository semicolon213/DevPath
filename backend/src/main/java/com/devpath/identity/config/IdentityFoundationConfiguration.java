package com.devpath.identity.config;

import com.devpath.identity.adapter.in.security.AuditedLogoutSuccessHandler;
import com.devpath.identity.application.AuthenticationAuditPort;
import com.devpath.shared.api.RequestCorrelationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class IdentityFoundationConfiguration {
    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    RequestCorrelationFilter requestCorrelationFilter() {
        return new RequestCorrelationFilter();
    }

    @Bean
    AuditedLogoutSuccessHandler auditedLogoutSuccessHandler(AuthenticationAuditPort auditPort, Clock clock) {
        return new AuditedLogoutSuccessHandler(auditPort, clock);
    }
}
