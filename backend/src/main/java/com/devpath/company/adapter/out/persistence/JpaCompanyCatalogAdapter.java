package com.devpath.company.adapter.out.persistence;
import com.devpath.company.application.CompanyCatalogPort;
import com.devpath.company.domain.CompanyProfile;
import java.util.*;
import org.springframework.stereotype.Component;
@Component public class JpaCompanyCatalogAdapter implements CompanyCatalogPort {
    private final CompanyCatalogJpaRepository repository; public JpaCompanyCatalogAdapter(CompanyCatalogJpaRepository r){repository=r;}
    public List<CompanyProfile> findSupported(){return repository.findByStatusOrderByLocalizedName("SUPPORTED").stream().map(this::map).toList();}
    public Optional<CompanyProfile> findSupportedById(String id){return repository.findByIdAndStatus(id,"SUPPORTED").map(this::map);}
    private CompanyProfile map(CompanyJpaEntity c){var p=c.activeProfile;if(p==null||!"ACTIVE".equals(p.status))throw new IllegalStateException("Supported company has no active profile");return new CompanyProfile(c.id,c.name,c.localizedName,p.id,p.versionLabel,p.engineeringCulture,p.technologyFocus,p.preferredCompetencies,p.recommendationPriorities,p.skillEmphasis,p.weightOverrides,p.effectiveAt);}
}
