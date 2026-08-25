package com.devpath.career.application;

import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareerCatalogApplicationService {
    private final CareerCatalogPort catalog;

    public CareerCatalogApplicationService(CareerCatalogPort catalog) {
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public CareerCatalogView list() {
        var careers = catalog.findSupported().stream()
            .sorted(Comparator.comparing(value -> value.localizedName()))
            .map(value -> new CareerSummaryView(value.careerId(), value.name(), value.localizedName(),
                value.careerStatus().name(), value.profileVersion(), value.purpose()))
            .toList();
        String version = careers.stream().map(CareerSummaryView::profileVersion).distinct().sorted()
            .reduce((left, right) -> left + "," + right).orElse("unavailable");
        return new CareerCatalogView(version, careers);
    }

    @Transactional(readOnly = true)
    public CareerProfileView get(String careerId) {
        return catalog.findSupportedById(careerId).map(CareerProfileView::from)
            .orElseThrow(CareerNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public boolean supports(String careerId) {
        return catalog.findSupportedById(careerId).isPresent();
    }
}
