package com.devpath.identity.adapter.out;

import com.devpath.career.application.CareerCatalogApplicationService;
import com.devpath.company.application.CompanyCatalogApplicationService;
import com.devpath.identity.application.TargetCatalogPort;
import com.devpath.identity.config.TargetCatalogProperties;
import com.devpath.identity.domain.PreferenceType;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredTargetCatalogAdapter implements TargetCatalogPort {
    private final TargetCatalogProperties properties;
    private final CompanyCatalogApplicationService companies;
    private final CareerCatalogApplicationService careers;

    public ConfiguredTargetCatalogAdapter(
        TargetCatalogProperties properties, CareerCatalogApplicationService careers,
        CompanyCatalogApplicationService companies
    ) {
        this.properties = properties;
        this.careers = careers;
        this.companies = companies;
    }

    public boolean supports(PreferenceType type, String targetId) {
        return targetId != null && (type == PreferenceType.CAREER
            ? careers.supports(targetId) : companies.supports(targetId));
    }

    public String version() { return properties.version(); }
}
