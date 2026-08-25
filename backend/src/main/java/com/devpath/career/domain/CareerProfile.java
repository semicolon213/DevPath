package com.devpath.career.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record CareerProfile(
    String careerId,
    String name,
    String localizedName,
    CareerStatus careerStatus,
    UUID profileVersionId,
    String profileVersion,
    String purpose,
    List<String> coreTechnologies,
    List<String> requiredCompetencies,
    List<String> preferredCompetencies,
    List<String> evaluationCategories,
    Map<String, String> priorityWeights,
    List<String> roadmapTemplate,
    Instant effectiveAt
) {
    public CareerProfile {
        Objects.requireNonNull(careerId);
        Objects.requireNonNull(profileVersionId);
        Objects.requireNonNull(careerStatus);
        coreTechnologies = List.copyOf(coreTechnologies);
        requiredCompetencies = List.copyOf(requiredCompetencies);
        preferredCompetencies = List.copyOf(preferredCompetencies);
        evaluationCategories = List.copyOf(evaluationCategories);
        priorityWeights = Map.copyOf(priorityWeights);
        roadmapTemplate = List.copyOf(roadmapTemplate);
    }

    public enum CareerStatus { SUPPORTED, DEPRECATED, FUTURE_CANDIDATE }
}
