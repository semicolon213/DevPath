package com.devpath.ai.application;

import java.util.List;
import java.util.UUID;

public record CareerAdviceContent(
    String readinessSummary, String strengthExplanation, String gapExplanation,
    List<NextStep> nextSteps, String confidenceNotice
) {
    public CareerAdviceContent { nextSteps = nextSteps == null ? null : List.copyOf(nextSteps); }

    public record NextStep(UUID recommendationId, String advice) {}
}
