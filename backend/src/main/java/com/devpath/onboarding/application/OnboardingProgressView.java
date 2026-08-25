package com.devpath.onboarding.application;

import java.time.Instant;
import java.util.List;

public record OnboardingProgressView(
    String status,
    int completedStepCount,
    int totalStepCount,
    String nextStep,
    List<Step> steps,
    Instant generatedAt
) {
    public OnboardingProgressView {
        steps = List.copyOf(steps);
    }

    public record Step(
        String step,
        String requirement,
        String status,
        String resourceId,
        String actionPath
    ) {}
}
