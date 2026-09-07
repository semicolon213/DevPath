package com.devpath.ai.application;

import com.devpath.ai.domain.GenerationJob;
import java.util.UUID;

public record GenerationWorkItem(
    GenerationJob job, UUID skillMatrixId, UUID analysisId, UUID careerReadinessId, UUID learningRoadmapId, String prompt
) {}
