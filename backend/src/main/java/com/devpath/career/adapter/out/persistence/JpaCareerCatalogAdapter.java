package com.devpath.career.adapter.out.persistence;

import com.devpath.career.application.CareerCatalogPort;
import com.devpath.career.domain.CareerProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaCareerCatalogAdapter implements CareerCatalogPort {
    private static final String SUPPORTED = "SUPPORTED";
    private final CareerCatalogJpaRepository repository;

    public JpaCareerCatalogAdapter(CareerCatalogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CareerProfile> findSupported() {
        return repository.findByStatusOrderByLocalizedName(SUPPORTED).stream().map(this::map).toList();
    }

    @Override
    public Optional<CareerProfile> findSupportedById(String careerId) {
        return repository.findByIdAndStatus(careerId, SUPPORTED).map(this::map);
    }

    private CareerProfile map(CareerJpaEntity career) {
        var profile = career.activeProfile;
        if (profile == null || !"ACTIVE".equals(profile.status)) {
            throw new IllegalStateException("Supported career has no active profile");
        }
        return new CareerProfile(career.id, career.name, career.localizedName,
            CareerProfile.CareerStatus.valueOf(career.status), profile.id, profile.versionLabel, profile.purpose,
            profile.coreTechnologies, profile.requiredCompetencies, profile.preferredCompetencies,
            profile.evaluationCategories, profile.priorityWeights, profile.roadmapTemplate, profile.effectiveAt);
    }
}
