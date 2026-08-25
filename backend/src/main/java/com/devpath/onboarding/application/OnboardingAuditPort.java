package com.devpath.onboarding.application;

import java.time.Instant;
import java.util.UUID;

public interface OnboardingAuditPort {
    void record(OnboardingAuditEvent event, UUID userId, Instant occurredAt);
}
