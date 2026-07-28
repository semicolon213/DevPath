package com.devpath.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class IdentityFoundationConfiguration {
    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
