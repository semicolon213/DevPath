package com.devpath.career.application;

import com.devpath.career.domain.CareerProfile;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CareerProfileView(
    String careerId,
    String name,
    String localizedName,
    String status,
    UUID careerProfileVersionId,
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
    static CareerProfileView from(CareerProfile value) {
        return new CareerProfileView(value.careerId(), value.name(), value.localizedName(), value.careerStatus().name(),
            value.profileVersionId(), value.profileVersion(), value.purpose(), value.coreTechnologies(),
            value.requiredCompetencies(), value.preferredCompetencies(), value.evaluationCategories(),
            value.priorityWeights(), value.roadmapTemplate(), value.effectiveAt());
    }
}
