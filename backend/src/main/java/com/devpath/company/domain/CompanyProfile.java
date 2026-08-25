package com.devpath.company.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CompanyProfile(String companyId, String name, String localizedName, UUID profileVersionId,
    String profileVersion, String engineeringCulture, List<String> technologyFocus,
    List<String> preferredCompetencies, List<String> recommendationPriorities, List<String> skillEmphasis,
    Map<String, String> weightOverrides, Instant effectiveAt) {
    public CompanyProfile { technologyFocus = List.copyOf(technologyFocus); preferredCompetencies = List.copyOf(preferredCompetencies); recommendationPriorities = List.copyOf(recommendationPriorities); skillEmphasis = List.copyOf(skillEmphasis); weightOverrides = Map.copyOf(weightOverrides); }
}
