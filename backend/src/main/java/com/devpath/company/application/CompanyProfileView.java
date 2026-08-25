package com.devpath.company.application;
import com.devpath.company.domain.CompanyProfile;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public record CompanyProfileView(String companyId, String name, String localizedName, String status, UUID companyProfileVersionId, String profileVersion, String engineeringCulture, List<String> technologyFocus, List<String> preferredCompetencies, List<String> recommendationPriorities, List<String> skillEmphasis, Map<String,String> weightOverrides, Instant effectiveAt) {
    static CompanyProfileView from(CompanyProfile v) { return new CompanyProfileView(v.companyId(),v.name(),v.localizedName(),"SUPPORTED",v.profileVersionId(),v.profileVersion(),v.engineeringCulture(),v.technologyFocus(),v.preferredCompetencies(),v.recommendationPriorities(),v.skillEmphasis(),v.weightOverrides(),v.effectiveAt()); }
}
